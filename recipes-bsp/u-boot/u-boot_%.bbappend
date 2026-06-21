# U-Boot bbappend — RAUC A/B boot support for Raspberry Pi 5
#
# TODO: The moto-timo/meta-raspberrypi fork (branch scarthgap/raspberrypi5_u-boot)
#       may already provide a machine-specific U-Boot bbappend.  Check for conflicts
#       with FILESEXTRAPATHS and boot.scr before enabling.  If the fork provides its
#       own boot script, remove the boot.cmd.in/boot.scr lines below and verify that
#       the BOOT_ORDER / BOOT_x_LEFT environment variables are wired up there instead.

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append = " \
    file://boot.cmd.in \
"

# Compile boot.cmd.in → boot.scr (FIT script image) using mkimage from the build
do_compile:append() {
    ${UBOOT_MKIMAGE} -A arm64 -T script -O linux -C none \
        -n "RAUC A/B boot script" \
        -d "${WORKDIR}/boot.cmd.in" \
        "${WORKDIR}/boot.scr"
}

do_deploy:append() {
    install -m 0644 "${WORKDIR}/boot.scr" "${DEPLOYDIR}/boot.scr"
}

# Ensure boot.scr lands in the FAT boot partition via the bootimg-partition source plugin
KERNEL_IMAGETYPES:append = " boot.scr"
