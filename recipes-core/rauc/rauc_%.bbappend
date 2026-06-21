# RAUC bbappend — deploy system.conf and fw_env.config for Raspberry Pi 5 RAUC A/B
#
# system.conf is installed to /etc/rauc/system.conf (shadows any default from meta-rauc).
# fw_env.config is installed to /etc/fw_env.config for use by u-boot-fw-utils
# (fw_setenv / fw_getenv) which the RAUC U-Boot backend calls at install time.

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append = " \
    file://system.conf \
    file://fw_env.config \
"

do_install:append() {
    install -d ${D}${sysconfdir}/rauc
    install -m 0644 ${WORKDIR}/system.conf ${D}${sysconfdir}/rauc/system.conf

    install -m 0644 ${WORKDIR}/fw_env.config ${D}${sysconfdir}/fw_env.config
}

FILES:${PN}:append = " \
    ${sysconfdir}/rauc/system.conf \
    ${sysconfdir}/fw_env.config \
"
