SUMMARY = "systemd-networkd DHCP profile for wlan0 (credentials provisioned via BLE)"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://20-wlan0.network"

RDEPENDS:${PN} = "wpa-supplicant"

do_install() {
    install -d ${D}${sysconfdir}/systemd/network
    install -m 0644 ${WORKDIR}/20-wlan0.network \
        ${D}${sysconfdir}/systemd/network/20-wlan0.network
}

FILES:${PN} = "${sysconfdir}/systemd/network/20-wlan0.network"
