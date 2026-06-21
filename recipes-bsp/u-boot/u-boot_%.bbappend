# U-Boot bbappend — meta-john/raspberrypi5
#
# We do NOT use U-Boot on Pi 5 (no BCM2712 PCIe driver — slot selection is done
# by the RPi firmware tryboot mechanism; see recipes-core/rauc/files/system.conf).
# This bbappend exists for ONE reason: the moto-timo/meta-raspberrypi scarthgap
# u-boot bbappend does `SRC_URI:append:raspberrypi5 = " file://autoboot_no_delay.cfg"`
# but does not ship that file. bitbake resolves SRC_URI checksums for every recipe
# at parse time (even ones never built with RPI_USE_U_BOOT="0"), so the file must
# be findable or parsing fails. FILESEXTRAPATHS makes our copy resolvable.
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
