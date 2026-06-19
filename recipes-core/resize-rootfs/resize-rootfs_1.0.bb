SUMMARY = "Expand root filesystem to fill disk on first boot"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://resize-rootfs"

RDEPENDS:${PN} = "parted e2fsprogs-resize2fs"

inherit update-rc.d

INITSCRIPT_NAME = "resize-rootfs"
INITSCRIPT_PARAMS = "start 05 2 3 4 5 ."

do_install() {
    install -d ${D}${INIT_D_DIR}
    install -m 0755 ${WORKDIR}/resize-rootfs ${D}${INIT_D_DIR}/resize-rootfs
    install -d ${D}${localstatedir}/lib
}

FILES:${PN} = "${INIT_D_DIR}/resize-rootfs ${localstatedir}/lib"
