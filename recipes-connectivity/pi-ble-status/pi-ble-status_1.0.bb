SUMMARY = "BLE GATT server exposing Pi status (IP, CPU temp, uptime)"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://pi-ble-status.py \
           file://pi-ble-status.service"

S = "${WORKDIR}"

inherit systemd

SYSTEMD_SERVICE:${PN} = "pi-ble-status.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

RDEPENDS:${PN} = "python3-core python3-dbus python3-pygobject bluez5"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/pi-ble-status.py ${D}${bindir}/pi-ble-status

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/pi-ble-status.service \
        ${D}${systemd_system_unitdir}/pi-ble-status.service
}

FILES:${PN} += "${systemd_system_unitdir}/pi-ble-status.service"
