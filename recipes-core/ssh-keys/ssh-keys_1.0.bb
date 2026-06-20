SUMMARY = "Install authorized SSH keys for root"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://authorized_keys"

do_install() {
    install -d ${D}/root/.ssh
    chmod 700 ${D}/root/.ssh
    install -m 0600 ${WORKDIR}/authorized_keys ${D}/root/.ssh/authorized_keys
}

FILES:${PN} = "/root/.ssh/authorized_keys"
