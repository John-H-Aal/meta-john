SUMMARY = "RAUC update bundle for Raspberry Pi 5 (slot A or B)"
LICENSE = "MIT"

inherit bundle

# Must match [system] compatible= in system.conf
RAUC_BUNDLE_COMPATIBLE = "RaspberryPi5"

# verity format: bundle contains a dm-verity root hash; requires RAUC ≥ 1.8
# and an image built with ext4 (matching RAUC_SLOT_rootfs[fstype] below).
# Switch to "plain" temporarily if you hit verity setup issues during dev.
RAUC_BUNDLE_FORMAT = "verity"

RAUC_BUNDLE_SLOTS = "rootfs"

RAUC_SLOT_rootfs[type]   = "image"
RAUC_SLOT_rootfs[fstype] = "ext4"
# Filename must match what bitbake produces in DEPLOY_DIR_IMAGE.
# "rootfs" suffix is added by Yocto's image class.
RAUC_SLOT_rootfs[file]   = "rpi5-base-image-${MACHINE}.rootfs.ext4"

# ── Signing keys ─────────────────────────────────────────────────────────────
# Generated once by create-example-keys.sh (from meta-rauc-community), then
# stored in meta-john/files/rauc-keys/ (gitignored — never commit private keys).
# Override in local.conf if your keys live elsewhere:
#   RAUC_KEY_FILE  = "/path/to/development-1.key.pem"
#   RAUC_CERT_FILE = "/path/to/development-1.cert.pem"
#
# ${THISDIR} = meta-john/recipes-core/rauc-bundle/
# ../../files/rauc-keys/ = meta-john/files/rauc-keys/
RAUC_KEY_FILE  ?= "${THISDIR}/../../files/rauc-keys/development-1.key.pem"
RAUC_CERT_FILE ?= "${THISDIR}/../../files/rauc-keys/development-1.cert.pem"

# Bundle output filename (lands in DEPLOY_DIR_IMAGE as rpi5-rauc-bundle.raucb)
PN = "rpi5-rauc-bundle"

# The bundle needs a built ext4 image before it can be assembled.
# Ensure rpi5-base-image is built with IMAGE_FSTYPES including "ext4".
do_bundle[depends] += "rpi5-base-image:do_image_ext4"
