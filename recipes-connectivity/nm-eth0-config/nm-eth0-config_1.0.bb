SUMMARY = "Static IP NetworkManager profile for eth0"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://eth0-static.nmconnection"

do_install() {
    install -d ${D}${sysconfdir}/NetworkManager/system-connections
    install -m 0600 ${WORKDIR}/eth0-static.nmconnection \
        ${D}${sysconfdir}/NetworkManager/system-connections/eth0-static.nmconnection
}

FILES:${PN} = "${sysconfdir}/NetworkManager/system-connections/eth0-static.nmconnection"
