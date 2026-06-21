SUMMARY = "RAUC custom bootloader backend for Raspberry Pi 5 firmware tryboot A/B"
DESCRIPTION = "Implements the RAUC bootloader=custom interface (get/set-primary, \
get/set-state) by editing autoboot.txt on the selector partition (/dev/nvme0n1p1)."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://tryboot-backend.sh"

# mountpoint(1) is from util-linux; the script also uses mount/umount/awk.
RDEPENDS:${PN} = "util-linux"

do_install() {
    install -d ${D}${nonarch_libdir}/rauc
    install -m 0755 ${WORKDIR}/tryboot-backend.sh ${D}${nonarch_libdir}/rauc/tryboot-backend.sh
}

# system.conf references /usr/lib/rauc/tryboot-backend.sh
FILES:${PN} = "${nonarch_libdir}/rauc/tryboot-backend.sh"
