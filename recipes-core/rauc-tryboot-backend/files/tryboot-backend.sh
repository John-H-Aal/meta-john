#!/bin/sh
# RAUC custom bootloader backend for Raspberry Pi 5 firmware tryboot A/B.
#
# RAUC (bootloader=custom) invokes this script with:
#   get-primary                  -> print bootname of the default/committed slot
#   set-primary <bootname>       -> make <bootname> the committed default slot
#   get-state   <bootname>       -> print "good" or "bad"
#   set-state   <bootname> good  -> (re)affirm <bootname> as the default slot
#   set-state   <bootname> bad   -> (no persistent attempt-counter on RPi; no-op)
#
# The selector partition /dev/nvme0n1p1 holds autoboot.txt:
#   [all]      boot_partition=<committed slot>   (normal boot uses this)
#   [tryboot]  boot_partition=<other slot>       (recovery / future one-shot tryboot)
#
# Update flow (operator):
#   rauc install bundle.raucb     # -> set-primary <target>: writes [all]=<target>
#   reboot                        # normal reboot now boots <target>
#   # on boot the system runs `rauc status mark-good` -> set-state good (idempotent)
#
# NOTE: this is the PERMANENT-SWAP model. The RPi firmware's one-shot tryboot
# (which would give automatic rollback if a new slot fails to boot) requires
# `vcmailbox` (raspberrypi-utils) to set the firmware tryboot flag — not installed
# here, and `systemctl reboot --reboot-argument` does not set it. Until that is
# added, set-primary switches [all] directly and rollback is a manual set-primary
# back to the previous slot. The firmware does not count boot attempts either, so
# get-state always reports "good" (booted == good model, per Bootlin).

set -eu

BOOTSEL_DEV="/dev/nvme0n1p1"
MNT="/run/rauc-bootsel"
AUTOBOOT="${MNT}/autoboot.txt"

bootname_to_part() {
    case "$1" in
        A) echo 2 ;;
        B) echo 3 ;;
        *) echo "tryboot-backend: unknown bootname '$1'" >&2; exit 1 ;;
    esac
}

part_to_bootname() {
    case "$1" in
        2) echo A ;;
        3) echo B ;;
        *) echo "tryboot-backend: unknown boot_partition '$1'" >&2; exit 1 ;;
    esac
}

mount_bootsel() {
    mkdir -p "${MNT}"
    mountpoint -q "${MNT}" || mount "${BOOTSEL_DEV}" "${MNT}"
}

umount_bootsel() {
    sync
    mountpoint -q "${MNT}" && umount "${MNT}" || true
}

# Read boot_partition from a given section ([all] or [tryboot]) of autoboot.txt.
read_section_part() {
    section="$1"
    awk -v want="${section}" '
        /^[[:space:]]*\[/ { sec = $0; gsub(/[][[:space:]]/, "", sec); next }
        sec == want && /boot_partition=/ {
            sub(/.*boot_partition=/, ""); gsub(/[[:space:]]/, ""); print; exit
        }
    ' "${AUTOBOOT}"
}

write_autoboot() {
    # $1 = [all] partition, $2 = [tryboot] partition
    cat > "${AUTOBOOT}" <<EOF
[all]
boot_partition=$1

[tryboot]
boot_partition=$2
EOF
    sync
}

other_part() {
    [ "$1" = "2" ] && echo 3 || echo 2
}

cmd="${1:-}"
case "${cmd}" in
    get-primary)
        mount_bootsel
        part="$(read_section_part all)"
        umount_bootsel
        [ -n "${part}" ] || { echo "tryboot-backend: no [all] boot_partition" >&2; exit 1; }
        part_to_bootname "${part}"
        ;;

    set-primary)
        # Permanent-swap: make the requested slot the committed default ([all]).
        # The other slot is kept as the [tryboot] target for manual recovery and
        # for future one-shot tryboot support (see header note).
        bootname="${2:?set-primary needs a bootname}"
        newpart="$(bootname_to_part "${bootname}")"
        mount_bootsel
        write_autoboot "${newpart}" "$(other_part "${newpart}")"
        umount_bootsel
        ;;

    get-state)
        # No persistent attempt counter on the RPi firmware.
        echo "good"
        ;;

    set-state)
        bootname="${2:?set-state needs a bootname}"
        state="${3:?set-state needs good|bad}"
        if [ "${state}" = "good" ]; then
            # Commit the booted slot as the default ([all]); other slot becomes tryboot.
            newpart="$(bootname_to_part "${bootname}")"
            mount_bootsel
            write_autoboot "${newpart}" "$(other_part "${newpart}")"
            umount_bootsel
        fi
        # "bad" has no persistent store here; nothing to do.
        ;;

    *)
        echo "tryboot-backend: unknown command '${cmd}'" >&2
        exit 1
        ;;
esac
