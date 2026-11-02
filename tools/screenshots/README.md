# Store Screenshots

Tooling for Google Play listing screenshots composed with
[MockupKit](https://github.com/grantbrooksgoodman/mockup-kit): a status bar
override for captures, and a Pixel 8a device frame for the mockups.

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
saves a PNG through `screencap` – 1080 x 2400 on a Pixel 8a or the Pixel 8a
AVD.

## Device frame

`pixel_8a.png` is a MockupKit frame built from the Pixel 8a emulator skin
with `make_device_frame.py`. The screen opening is transparent and the
camera cutout is opaque, so MockupKit's throwing `DeviceFrame` initializer
detects the screen itself (insets 57 top, 57 left, 82 bottom, 61 right;
corner radius 77) and draws the cutout over the screenshot. The opening is
exactly 1080 x 2400, so Pixel 8a captures fill it without cropping.

Regenerate it, or build a frame for another device, from the skins the
Android SDK ships under `$ANDROID_HOME/skins/<device>/`:

```
python3 tools/screenshots/make_device_frame.py $ANDROID_HOME/skins/pixel_8a tools/screenshots/pixel_8a.png
```

The script needs Pillow with WebP support and prints the insets MockupKit
will detect.

## Play Store sizes

Google Play accepts screenshots as JPEG or 24-bit PNG without an alpha channel,
each side 320–3,840 px, with the longest side at most twice the shortest.
Phone listings take 2–8 screenshots; featuring on phones needs at least four
at 9:16 (or 16:9) with the short side at least 1,080 px. A raw 1080 x 2400
capture is 20:9, which exceeds the 2:1 limit, so the mockups are rendered on
a 9:16 canvas (`OutputSize.custom(width: 1080, height: 1920)`, or `.small`,
1242 x 2208) and uploaded without an alpha channel.
