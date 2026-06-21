SUMMARY = "Base image for Raspberry Pi 5 — RAUC A/B OTA target"
DESCRIPTION = "NVMe RAUC A/B image with SSH, networking, OTA, and persistent /data"

inherit core-image

IMAGE_FEATURES += "ssh-server-openssh"

# RAUC A/B via RPi firmware tryboot — GPT layout:
# p1 bootsel(autoboot.txt) p2 boot-A p3 boot-B p4 rootfs-A p5 rootfs-B p6 data
WKS_FILE = "rauc-raspberrypi-tryboot.wks"

# ext4 output is required by the rauc-bundle recipe for the rootfs slot.
# wic.bz2 + wic.bmap are used for initial NVMe flashing.
IMAGE_FSTYPES:append = " ext4"

# meta-rauc-community's base-files bbappend ships a demo SD-card fstab
# (/dev/mmcblk0p1 /boot, mmcblk0p5 /data, mmcblk0p6 /home growfs) that does not
# exist on our NVMe layout — those entries fail to mount and drop systemd into
# emergency mode. Overwrite fstab with the correct layout.
#
# Note: there is intentionally NO /boot entry. Each RAUC slot has its own vfat
# boot partition (p2 or p3); the running rootfs is slot-agnostic (RAUC installs
# the same image to either slot), so it must not hardcode a boot device. RAUC
# writes boot slots via their block device, and the tryboot backend mounts the
# selector (p1) on demand. /data is mounted by the data.mount unit (nvme0n1p6).
# Baked into the rootfs so the standalone .ext4 used for RAUC OTA is correct too.
ROOTFS_POSTPROCESS_COMMAND:append = " fixup_fstab;"
fixup_fstab() {
    cat > ${IMAGE_ROOTFS}/etc/fstab <<'EOF'
/dev/root            /                    auto       defaults              1  1
proc                 /proc                proc       defaults              0  0
devpts               /dev/pts             devpts     mode=0620,ptmxmode=0666,gid=5      0  0
tmpfs                /run                 tmpfs      mode=0755,nodev,nosuid,strictatime 0  0
tmpfs                /var/volatile        tmpfs      defaults              0  0
EOF
}

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

# Visible build/version marker so an OTA update is observable on the target slot.
# Bump DEMO_VERSION (e.g. `bitbake -R 'DEMO_VERSION="3"' ...` or edit here) to
# produce a distinguishable bundle; `cat /etc/build-version` shows which build a
# slot is running.
DEMO_VERSION ?= "2"
ROOTFS_POSTPROCESS_COMMAND:append = " write_build_version;"
write_build_version() {
    echo "rpi5-base-image v${DEMO_VERSION}" > ${IMAGE_ROOTFS}/etc/build-version
}

# wic populates the boot partition (p2) via bootimg-partition, but it knows nothing
# about the tryboot selector (p1) or the per-slot cmdline files. Inject those into
# the compressed wic image after creation, computing FAT offsets from the GPT rather
# than hardcoding them. (wic also can't lay down our autoboot.txt or the config.txt
# [boot_partition=N] conditionals.)
IMAGE_POSTPROCESS_COMMAND += "setup_tryboot_image; "
setup_tryboot_image() {
    local wic_bz2="${IMGDEPLOYDIR}/${IMAGE_LINK_NAME}.wic.bz2"
    local tmp_wic="${WORKDIR}/tmp-tryboot.wic"

    pbzip2 -d -c "${wic_bz2}" > "${tmp_wic}"

    # Byte offsets of p1 (bootsel, index 0), p2 (boot-a, index 1), p3 (boot-b,
    # index 2), plus p2's size — used to clone boot-a onto boot-b below.
    local sel_off boot_off bootb_off boot_bytes
    sel_off=$(sfdisk -J "${tmp_wic}" | python3 -c \
        "import json,sys; p=json.load(sys.stdin)['partitiontable']['partitions']; print(p[0]['start']*512)")
    boot_off=$(sfdisk -J "${tmp_wic}" | python3 -c \
        "import json,sys; p=json.load(sys.stdin)['partitiontable']['partitions']; print(p[1]['start']*512)")
    bootb_off=$(sfdisk -J "${tmp_wic}" | python3 -c \
        "import json,sys; p=json.load(sys.stdin)['partitiontable']['partitions']; print(p[2]['start']*512)")
    boot_bytes=$(sfdisk -J "${tmp_wic}" | python3 -c \
        "import json,sys; p=json.load(sys.stdin)['partitiontable']['partitions']; print(p[1]['size']*512)")

    # p1: selector — slot A (boot_partition=2) is the committed default at flash time.
    cat > "${WORKDIR}/autoboot.txt" <<'EOF'
[all]
boot_partition=2

[tryboot]
boot_partition=3
EOF
    mcopy -o -i "${tmp_wic}@@${sel_off}" "${WORKDIR}/autoboot.txt" ::autoboot.txt

    # p2: per-slot cmdline files. Only root= differs; args mirror the proven
    # firmware-direct boot. rauc.slot lets RAUC confirm the booted slot.
    local args="rootfstype=ext4 fsck.repair=yes rootwait net.ifnames=0"
    echo "console=tty1 root=/dev/nvme0n1p4 ${args} rauc.slot=A" > "${WORKDIR}/cmdline-rootfs-A.txt"
    echo "console=tty1 root=/dev/nvme0n1p5 ${args} rauc.slot=B" > "${WORKDIR}/cmdline-rootfs-B.txt"
    mcopy -o -i "${tmp_wic}@@${boot_off}" "${WORKDIR}/cmdline-rootfs-A.txt" ::cmdline-rootfs-A.txt
    mcopy -o -i "${tmp_wic}@@${boot_off}" "${WORKDIR}/cmdline-rootfs-B.txt" ::cmdline-rootfs-B.txt

    # p2: prepend the firmware per-partition cmdline selector to config.txt so the
    # same boot image picks the matching rootfs in whichever slot it runs from.
    mcopy -i "${tmp_wic}@@${boot_off}" ::config.txt "${WORKDIR}/config-orig.txt"
    cat > "${WORKDIR}/config-tryboot.txt" <<'EOF'
[boot_partition=2]
cmdline=cmdline-rootfs-A.txt
[boot_partition=3]
cmdline=cmdline-rootfs-B.txt
[all]
EOF
    cat "${WORKDIR}/config-orig.txt" >> "${WORKDIR}/config-tryboot.txt"
    mcopy -o -i "${tmp_wic}@@${boot_off}" "${WORKDIR}/config-tryboot.txt" ::config.txt

    # Clone the now fully-populated boot-A (p2) onto boot-B (p3) so slot B is
    # bootable straight after the initial flash. config.txt is identical in both
    # slots (the [boot_partition=N] conditional selects the right cmdline), so a
    # byte-for-byte copy of the partition is correct. Without this, a rootfs-only
    # RAUC OTA leaves p3 empty and slot B fails to boot. Extract p2 to a temp file
    # first to avoid dd reading and writing the same file in place.
    # NB: byte offsets fed straight to dd via skip_bytes/seek_bytes/count_bytes —
    # bitbake's pysh shell parser does not support $((...)) arithmetic.
    dd if="${tmp_wic}" of="${WORKDIR}/boot-a.img" bs=1M iflag=skip_bytes,count_bytes \
        skip="${boot_off}" count="${boot_bytes}" status=none
    dd if="${WORKDIR}/boot-a.img" of="${tmp_wic}" bs=1M oflag=seek_bytes \
        seek="${bootb_off}" conv=notrunc status=none
    rm -f "${WORKDIR}/boot-a.img"

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
    rauc-tryboot-backend \
    rauc-mark-good \
    data-mount \
    resize-data \
"
