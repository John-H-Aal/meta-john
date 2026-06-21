SUMMARY = "Expand /data partition to fill NVMe drive on first boot"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://resize-data.sh file://resize-data.service"

RDEPENDS:${PN} = "parted e2fsprogs-resize2fs udev util-linux-partx"

inherit systemd

SYSTEMD_SERVICE:${PN} = "resize-data.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${sbindir}
    install -m 0755 ${WORKDIR}/resize-data.sh ${D}${sbindir}/resize-data.sh

    install -d ${D}${systemd_unitdir}/system
    install -m 0644 ${WORKDIR}/resize-data.service ${D}${systemd_unitdir}/system/

    install -d ${D}${localstatedir}/lib
}

FILES:${PN} = "${sbindir}/resize-data.sh ${systemd_unitdir}/system/resize-data.service ${localstatedir}/lib"
