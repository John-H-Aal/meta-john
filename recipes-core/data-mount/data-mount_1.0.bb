SUMMARY = "Systemd mount unit for persistent /data partition (mmcblk0p4)"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

inherit systemd

# Unit file name must exactly match Where= path with / → - substitution
SYSTEMD_SERVICE:${PN} = "data.mount"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

SRC_URI = "file://data.mount"

S = "${WORKDIR}"

do_install() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/data.mount \
        ${D}${systemd_system_unitdir}/data.mount
    # Create the mount point in the rootfs
    install -d ${D}/data
}

FILES:${PN} = "\
    ${systemd_system_unitdir}/data.mount \
    /data \
"
