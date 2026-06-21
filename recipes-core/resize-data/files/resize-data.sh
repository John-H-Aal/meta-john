#!/bin/sh
set -e

DISK=/dev/nvme0n1
PART=4
FLAGFILE=/var/lib/resize-data-done

if [ -f "${FLAGFILE}" ]; then
    exit 0
fi

# Extend the GPT partition to fill the drive, then expand the filesystem.
# parted -s is non-interactive; udevadm settle lets the kernel see the new size
# before resize2fs opens the block device.
parted -s "${DISK}" resizepart "${PART}" 100%
udevadm settle
resize2fs "${DISK}p${PART}"

touch "${FLAGFILE}"
