#!/bin/sh
set -e

DISK=/dev/nvme0n1
PART=6
FLAGFILE=/var/lib/resize-data-done

if [ -f "${FLAGFILE}" ]; then
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
resize2fs "${DISK}p${PART}"

touch "${FLAGFILE}"
