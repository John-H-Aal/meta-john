# systemd bbappend — install watchdog drop-in (does not patch systemd.service directly)
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append = " file://10-watchdog.conf"

do_install:append() {
    install -d ${D}${sysconfdir}/systemd/system.conf.d
    install -m 0644 ${WORKDIR}/10-watchdog.conf \
        ${D}${sysconfdir}/systemd/system.conf.d/10-watchdog.conf
}

FILES:${PN}:append = " ${sysconfdir}/systemd/system.conf.d/10-watchdog.conf"
