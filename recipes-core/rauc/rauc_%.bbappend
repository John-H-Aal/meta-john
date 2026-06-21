# meta-john rauc bbappend — intentionally empty.
#
# system.conf and ca.cert.pem are provided by rauc-conf_%.bbappend (the correct
# recipe — meta-rauc's rauc-conf.bb, RRECOMMENDED by rauc via virtual-rauc-conf).
#
# No fw_env.config / u-boot-env here: U-Boot is unused on Pi 5 (no BCM2712 PCIe
# driver), so slot selection goes through the RPi firmware tryboot mechanism. See
# system.conf (bootloader=custom) and rauc-tryboot-backend (tryboot-backend.sh).
