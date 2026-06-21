# meta-john override for rauc-conf.
#
# system.conf uses literal "RaspberryPi5" for [system] compatible — must match
# RAUC_BUNDLE_COMPATIBLE = "RaspberryPi5" in rauc-bundle.bb.
# The @@MACHINE@@ template from meta-rauc-community resolves to the lowercase
# MACHINE name ("raspberrypi5"), which would NOT match, so we provide our own.
#
# FILESEXTRAPATHS:prepend ensures our files are searched first, overriding the
# placeholder system.conf and test ca.cert.pem from meta-rauc and the community
# bbappend.  meta-rauc-community's do_install:prepend sed is a no-op on our file.

FILESEXTRAPATHS:prepend := "${THISDIR}/files:${THISDIR}/../../files/rauc-keys:"
