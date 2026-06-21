SUMMARY = "Systemd service to mark the current RAUC slot as good after successful boot"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

# NOTE: Newer versions of meta-rauc may already ship a rauc-mark-good service.
# If bitbake warns about a file-owning conflict on rauc-mark-good.service, remove
# this recipe and add meta-rauc's rauc-mark-good package to IMAGE_INSTALL instead.

inherit systemd

SYSTEMD_SERVICE:${PN} = "rauc-mark-good.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

SRC_URI = "file://rauc-mark-good.service"

S = "${WORKDIR}"

do_install() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/rauc-mark-good.service \
        ${D}${systemd_system_unitdir}/rauc-mark-good.service
}

FILES:${PN} = "${systemd_system_unitdir}/rauc-mark-good.service"

RDEPENDS:${PN} = "rauc"
