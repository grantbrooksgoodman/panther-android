#!/usr/bin/env bash
#
#  status_bar.sh
#
#  Created by Grant Brooks Goodman.
#  Copyright © NEOTechnica Corporation. All rights reserved.
#
#  Puts the status bar of a connected device or emulator into System UI
#  demo mode for store screenshots: the clock reads 9:41, the battery
#  is full and not charging, Wi-Fi and cellular show full signal, and
#  notification and status icons are hidden. Runs over adb against the
#  emulator or a physical device with USB debugging.
#
#  Usage:
#    status_bar.sh on [--time HHMM] [--data-type TYPE] [--serial SERIAL]
#    status_bar.sh off [--serial SERIAL]
#    status_bar.sh capture <output.png> [--serial SERIAL]
#
#  --time       Clock to show, as four digits. Defaults to 0941. Shown as
#               9:41 or 09:41 according to the device's 12/24-hour setting.
#  --data-type  Label next to the cellular bars: lte, 4g, 5g, 3g, h, e, g,
#               1x, roam. Defaults to none, which shows bars only.
#  --serial     The adb serial to target when several devices are attached.

set -euo pipefail

adb_shell() {
    adb ${adb_args[@]+"${adb_args[@]}"} shell "$@" > /dev/null
}

demo() {
    # Each argument after the command is a `key value` pair for the
    # broadcast.
    local args=(-e command "$1")
    shift
    while [ $# -gt 0 ]; do
        args+=(-e "$1" "$2")
        shift 2
    done
    adb_shell am broadcast -a com.android.systemui.demo "${args[@]}"
}

usage() {
    sed -n '/^#  Usage:/,/^$/p' "$0" | sed -E 's/^# {0,2}//'
    exit 1
}

[ $# -ge 1 ] || usage
command_name=$1
shift

time=0941
data_type=none
serial=""
output=""

if [ "$command_name" = "capture" ]; then
    [ $# -ge 1 ] || usage
    output=$1
    shift
fi

while [ $# -gt 0 ]; do
    case $1 in
        --time) time=$2; shift 2 ;;
        --data-type) data_type=$2; shift 2 ;;
        --serial) serial=$2; shift 2 ;;
        *) usage ;;
    esac
done

adb_args=()
if [ -n "$serial" ]; then
    adb_args=(-s "$serial")
fi

case $command_name in
    on)
        case $time in
            [0-9][0-9][0-9][0-9]) ;;
            *) echo "--time takes four digits, such as 0941." >&2; exit 1 ;;
        esac

        adb_shell settings put global sysui_demo_allowed 1
        demo enter
        demo clock hhmm "$time"
        demo battery level 100 plugged false powersave false
        demo network wifi show level 4 fully true
        demo network mobile \
            show \
            datatype "$data_type" \
            level 4 \
            fully true \
            sims 1 \
            slot 0

        demo network airplane hide nosim hide carriernetworkchange hide
        demo notifications visible false
        demo status \
            volume hide \
            bluetooth hide \
            location hide \
            alarm hide \
            sync hide \
            tty hide \
            eri hide \
            mute hide \
            speakerphone hide

        echo "Status bar in demo mode: clock $time, battery full, signal full."
        echo "Run '$0 off' to restore it."
        ;;

    off)
        demo exit
        echo "Status bar restored."
        ;;

    capture)
        adb ${adb_args[@]+"${adb_args[@]}"} exec-out screencap -p > "$output"
        echo "Saved screenshot to $output."
        ;;

    *)
        usage
        ;;
esac
