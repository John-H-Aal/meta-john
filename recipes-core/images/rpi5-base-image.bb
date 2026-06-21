SUMMARY = "Base image for Raspberry Pi 5 — RAUC A/B OTA target"
DESCRIPTION = "NVMe RAUC A/B image with SSH, networking, OTA, and persistent /data"

inherit core-image

IMAGE_FEATURES += "ssh-server-openssh"

# RAUC A/B partition layout (p1 boot, p2 slot-A, p3 slot-B, p4 data)
WKS_FILE = "rauc-raspberrypi.wks"

# ext4 output is required by the rauc-bundle recipe for the rootfs slot.
# wic.bz2 + wic.bmap are used for initial NVMe flashing.
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

# Wic's direct.py only adds the 'p' partition separator for mmcblk devices, not nvme.
# With --ondisk nvme0n1, wic writes /dev/nvme0n11 in fstab and root=/dev/mmcblk0p2 in
# cmdline.txt. Patch both inside the compressed wic image after creation.
#
# Boot FAT at --align 4096 (sectors) = 4 MiB = byte offset 4194304.
# Rootfs-A is partition index 1 (0-based) in the partition table.
IMAGE_POSTPROCESS_COMMAND += "patch_nvme_image; "
patch_nvme_image() {
    local wic_bz2="${IMGDEPLOYDIR}/${IMAGE_LINK_NAME}.wic.bz2"
    local tmp_wic="${WORKDIR}/tmp-nvme-patch.wic"

    pbzip2 -d -c "${wic_bz2}" > "${tmp_wic}"

    # Fix cmdline.txt in FAT boot partition (offset 4 MiB)
    mcopy -i "${tmp_wic}@@4194304" ::cmdline.txt "${WORKDIR}/cmdline-nvme.txt"
    sed -i 's|/dev/mmcblk0p2|/dev/nvme0n1p2|g' "${WORKDIR}/cmdline-nvme.txt"
    sed -i 's|$| reboot=cold|' "${WORKDIR}/cmdline-nvme.txt"
    mcopy -o -i "${tmp_wic}@@4194304" "${WORKDIR}/cmdline-nvme.txt" ::cmdline.txt

    # Fix fstab in rootfs-A (partition index 1, 0-based); wic writes /dev/nvme0n11
    # instead of /dev/nvme0n1p1 for the /boot entry.
    local root_start
    root_start=$(sfdisk -J "${tmp_wic}" | python3 -c \
        "import json,sys; p=json.load(sys.stdin)['partitiontable']['partitions']; print(p[1]['start'])")
    dd if="${tmp_wic}" of="${WORKDIR}/root.ext4" bs=512 skip="${root_start}" status=none
    debugfs "${WORKDIR}/root.ext4" -R "cat /etc/fstab" > "${WORKDIR}/fstab-orig" 2>/dev/null
    sed 's|/dev/nvme0n11|/dev/nvme0n1p1|g' "${WORKDIR}/fstab-orig" > "${WORKDIR}/fstab-fixed"
    printf 'rm /etc/fstab\nwrite %s /etc/fstab\nchmod 644 /etc/fstab\nq\n' "${WORKDIR}/fstab-fixed" \
        | debugfs -w "${WORKDIR}/root.ext4" 2>/dev/null
    dd if="${WORKDIR}/root.ext4" of="${tmp_wic}" bs=512 seek="${root_start}" conv=notrunc status=none
    rm -f "${WORKDIR}/root.ext4"

    pbzip2 -f "${tmp_wic}"
    mv "${tmp_wic}.bz2" "${wic_bz2}"
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
