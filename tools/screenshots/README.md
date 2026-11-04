# Store Screenshots

Tooling for Google Play listing screenshots composed with
[MockupKit](https://github.com/grantbrooksgoodman/mockup-kit): a status bar
override for captures, and a generator for the device frames the mockups
are composed in.

## Status bar

`status_bar.sh` puts the status bar of the connected device or emulator into
System UI demo mode. The clock reads 9:41, the battery is full and not
charging, Wi-Fi and cellular show full signal, and notification and status
icons are hidden.

```
tools/screenshots/status_bar.sh on
tools/screenshots/status_bar.sh capture ~/Desktop/chat.png
tools/screenshots/status_bar.sh off
```

`on` takes `--time HHMM` (default `0941`), `--data-type lte|4g|5g|…` (default
`none`, bars only), and `--serial` when several devices are attached. The
clock follows the device's 12/24-hour setting. The script runs over adb
against the emulator or a physical device with USB debugging. `capture`
saves a PNG through `screencap`.

## Device frame

`make_device_frame.py` builds a MockupKit frame from an emulator skin, which
the Android SDK ships under `$ANDROID_HOME/skins/<device>/`:

```
python3 tools/screenshots/make_device_frame.py $ANDROID_HOME/skins/pixel_10_pro_xl ~/Desktop/pixel_10_pro_xl.png
```

The frame's screen opening is transparent and the camera cutout is opaque,
so MockupKit's throwing `DeviceFrame` initializer detects the screen itself
and draws the cutout over the screenshot. The script prints the insets and
corner radius MockupKit will detect, along with the opening's size. A capture
from the matching AVD fills the opening exactly; MockupKit scales and crops a
capture of another size to fill it. On skins whose body is rounded, the
screen's bounding-box corners lie outside the body, so the corner-radius scans
pass that transparency before reaching the bezel.

Frames are not kept in the repository; generate one for each device as
needed. The script needs Pillow with WebP support.

## Play Store sizes

Google Play accepts screenshots as JPEG or 24-bit PNG without an alpha channel,
each side 320–3,840 px, with the longest side at most twice the shortest.
Phone listings take 2–8 screenshots; featuring on phones needs at least four
at 9:16 (or 16:9) with the short side at least 1,080 px. A raw 1080 x 2400
capture is 20:9, which exceeds the 2:1 limit, so the mockups are rendered on
a 9:16 canvas (`OutputSize.custom(width: 1080, height: 1920)`, or `.small`,
1242 x 2208) and uploaded without an alpha channel.
