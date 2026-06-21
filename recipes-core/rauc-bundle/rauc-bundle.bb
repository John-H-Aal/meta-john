SUMMARY = "RAUC update bundle for Raspberry Pi 5"
LICENSE = "MIT"

inherit bundle

RAUC_BUNDLE_COMPATIBLE = "RaspberryPi5"
RAUC_BUNDLE_FORMAT = "verity"
RAUC_BUNDLE_SLOTS = "rootfs"

# RAUC_SLOT_<name> must be set to the image recipe name — the bundle class
# derives the filename from the recipe's deploy output and IMAGE_FSTYPES.
RAUC_SLOT_rootfs = "rpi5-base-image"
RAUC_SLOT_rootfs[fstype] = "ext4"

# Signing keys — generated once by meta-john/files/rauc-keys/gen-keys.sh.
# ${THISDIR} = meta-john/recipes-core/rauc-bundle/
# Override in local.conf if your keys live elsewhere.
RAUC_KEY_FILE  ?= "${THISDIR}/../../files/rauc-keys/development-1.key.pem"
RAUC_CERT_FILE ?= "${THISDIR}/../../files/rauc-keys/development-1.cert.pem"
RAUC_KEYRING_FILE ?= "${THISDIR}/../../files/rauc-keys/ca.cert.pem"

# Bundle output name
PN = "rpi5-rauc-bundle"
