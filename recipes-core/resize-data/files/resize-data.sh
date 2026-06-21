#!/bin/sh
set -e

DISK=/dev/nvme0n1
DISKNAME=nvme0n1
PART=6
PARTNAME=nvme0n1p6
FLAGFILE=/var/lib/resize-data-done

if [ -f "${FLAGFILE}" ]; then
    exit 0
fi

# A/B safety: /data (the last partition) is SHARED across the rootfs slots, but
# this first-boot flag lives in the per-slot rootfs. When the *second* slot boots
# for the first time (e.g. after a RAUC OTA + swap), /data has already been grown
# by the first slot — re-running the resize would fail (nothing left to grow, and
# offline resize2fs on the now-used fs demands an e2fsck). So skip when the
# partition already reaches the end of the disk. Geometry from sysfs, in sectors.
disk_sectors=$(cat "/sys/class/block/${DISKNAME}/size")
part_start=$(cat "/sys/class/block/${PARTNAME}/start")
part_sectors=$(cat "/sys/class/block/${PARTNAME}/size")
free_sectors=$(( disk_sectors - (part_start + part_sectors) ))
# 16 MiB slack: an unresized image leaves tens of GiB free, while a grown one
# leaves only the GPT backup (~33 sectors). Anything under 16 MiB = already full.
if [ "${free_sectors}" -lt 32768 ]; then
    touch "${FLAGFILE}"
    exit 0
fi

# The wic image is dd'd onto a much larger disk, so the GPT *backup* header sits
# at the old (image) end and the GPT's "last usable LBA" is wrong. parted then
# refuses to grow the last partition ("Unable to satisfy all constraints").
# Feed "Fix" to parted's "use all of the space" prompt to relocate the backup
# header to the true end of the disk. (GPT only — the old MBR layout did not need
# this. ---pretend-input-tty lets parted read the answer from stdin non-interactively.)
printf "Fix\n" | parted ---pretend-input-tty "${DISK}" print >/dev/null 2>&1 || true

# Grow the data partition (the last partition) to fill the disk, then expand the
# filesystem. This unit is ordered Before=data.mount, so p6 is NOT mounted here.
# root is on the same disk (p4), which blocks a full BLKRRPART re-read, so use
# `partx -u` to update just the resized last partition in the kernel via BLKPG.
parted -s "${DISK}" resizepart "${PART}" 100%
partx -u "${DISK}"
udevadm settle
# p6 is unmounted here (ordered Before=data.mount); make sure it is clean so
# resize2fs won't refuse with "Please run e2fsck first".
e2fsck -fp "${DISK}p${PART}" || true
resize2fs "${DISK}p${PART}"

touch "${FLAGFILE}"
