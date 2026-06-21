SUMMARY = "Base image for Raspberry Pi 5 — RAUC A/B OTA target"
DESCRIPTION = "SD-card RAUC A/B image with SSH, networking, OTA, and persistent /data"

inherit core-image

IMAGE_FEATURES += "ssh-server-openssh"

# RAUC A/B partition layout (p1 boot, p2 slot-A, p3 slot-B, p4 data)
WKS_FILE = "rauc-raspberrypi.wks"

# ext4 output is required by the rauc-bundle recipe for the rootfs slot.
# wic.bz2 + wic.bmap are used for initial SD flashing.
IMAGE_FSTYPES:append = " ext4"

# Allow key-based root login without debug-tweaks (which would also allow password login)
ROOTFS_POSTPROCESS_COMMAND:append = " permit_root_key_login;"
permit_root_key_login() {
    if grep -q 'PermitRootLogin' ${IMAGE_ROOTFS}/etc/ssh/sshd_config; then
        sed -i 's/^#*PermitRootLogin.*/PermitRootLogin prohibit-password/' \
            ${IMAGE_ROOTFS}/etc/ssh/sshd_config
    else
        echo 'PermitRootLogin prohibit-password' >> ${IMAGE_ROOTFS}/etc/ssh/sshd_config
    fi
}

IMAGE_INSTALL:append = " \
    bluez5 \
    pi-ble-status \
    eth0-networkd-config \
    wlan0-config \
    ssh-keys \
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
    rauc \
    u-boot-fw-utils \
    rauc-mark-good \
    data-mount \
    resize-data \
"
