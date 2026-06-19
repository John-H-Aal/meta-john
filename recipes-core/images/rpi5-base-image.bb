SUMMARY = "Base image for Raspberry Pi 5 NVMe target"
DESCRIPTION = "Custom image with SSH, networking, and NVMe flash tooling"

inherit core-image

IMAGE_FEATURES += "ssh-server-openssh"

IMAGE_INSTALL:append = " \
    networkmanager \
    nm-eth0-config \
    e2fsprogs \
    e2fsprogs-mke2fs \
    e2fsprogs-e2fsck \
    e2fsprogs-resize2fs \
    resize-rootfs \
    bmaptool \
    util-linux \
    util-linux-lsblk \
    util-linux-blkid \
    parted \
    curl \
    nano \
"
