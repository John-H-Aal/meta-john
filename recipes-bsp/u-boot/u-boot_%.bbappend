# U-Boot bbappend — meta-john/raspberrypi5
#
# Purpose: provide autoboot_no_delay.cfg which the moto-timo/meta-raspberrypi
# scarthgap bbappend references (SRC_URI:append:raspberrypi5) but does not ship.
#
# Boot script (boot.cmd.in) and RAUC U-Boot environment integration are handled
# by meta-rauc-community/meta-rauc-raspberrypi — do not duplicate them here.
# boot.cmd.in is kept in files/ for reference only.

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
