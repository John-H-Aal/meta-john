# Override the community boot.cmd.in (which targets mmc 0:X / mmcblk0pX)
# with our NVMe-specific version (nvme 0:1, nvme0n1p2/p3).
# FILESEXTRAPATHS:prepend gives our files/ priority over meta-rauc-community's.

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
