#!/usr/bin/env python3
#
#  make_device_frame.py
#
#  Created by Grant Brooks Goodman.
#  Copyright © NEOTechnica Corporation. All rights reserved.
#
#  Builds a MockupKit device frame PNG from an emulator skin: a
#  folder such as `pixel_8a` holding `layout`, `back.webp` (the
#  device body with a transparent screen opening), and `mask.webp`
#  (opaque where the screen is hidden – the rounded corners and the
#  camera cutout). The Android SDK ships skins under
#  `$ANDROID_HOME/skins/<device>/`.
#
#  MockupKit locates the screen by scanning the frame's alpha channel
#  inward from each edge and draws the frame over the screenshot, so
#  the output keeps the body art, leaves the screen opening fully
#  transparent, and paints the camera cutout opaque so it covers the
#  screenshot beneath it. The script prints the screen insets
#  MockupKit will detect, measured with the same scans.
#
#  Usage: python3 make_device_frame.py <skin-directory> <output-png>
#
#  Requires Pillow with WebP support (pip install pillow).

# Native
import math
import os
import sys
from collections.abc import Iterable
from dataclasses import dataclass

# 3rd-party
from PIL import Image, ImageChops

CUTOUT_FILL = (0, 0, 0, 255)


@dataclass(frozen=True)
class ScreenInsets:
    """The insets defining the screen region within a device frame image.

    Values are in the frame image's pixel coordinate space and are
    the distance from each edge of the frame image to the
    corresponding edge of the screen area.
    """

    bottom: int
    corner_radius: float
    left: int
    right: int
    top: int


def build_frame(skin_directory: str) -> Image.Image:
    """Builds the device frame image from the skin's layout and art."""
    with open(
        os.path.join(skin_directory, "layout"),
        encoding="utf-8",
    ) as handle:
        layout = parse_layout(handle.read())

    display = layout["parts"]["device"]["display"]
    display_size = (int(display["width"]), int(display["height"]))
    origin = device_part_origin(layout)
    portrait = layout["parts"]["portrait"]

    frame = Image.open(
        os.path.join(skin_directory, portrait["background"]["image"])
    ).convert("RGBA")

    mask = Image.open(
        os.path.join(skin_directory, portrait["foreground"]["mask"])
    ).convert("RGBA")

    if mask.size != display_size:
        raise ValueError(
            f"Mask is {mask.size}, but the layout's display is {display_size}."
        )

    # Pixels the mask hides, but the body art left open, are the
    # camera cutout. Paint them opaque so the frame covers the
    # screenshot there. The corners are already opaque bezel in the
    # body art and are left alone.
    box = (
        origin[0],
        origin[1],
        origin[0] + display_size[0],
        origin[1] + display_size[1],
    )

    frame.paste(
        Image.new("RGBA", display_size, CUTOUT_FILL),
        box,
        ImageChops.multiply(
            mask.getchannel("A"),
            ImageChops.invert(opaque_mask(frame.crop(box))),
        ),
    )

    # Fully transparent pixels carry stray color in the source art.
    # Zero them so the opening is clean in every channel.
    visible = opaque_mask(frame)
    red, green, blue, alpha = frame.split()
    return Image.merge(
        "RGBA",
        (
            ImageChops.multiply(red, visible),
            ImageChops.multiply(green, visible),
            ImageChops.multiply(blue, visible),
            alpha,
        ),
    )


def detect_screen_insets(image: Image.Image) -> ScreenInsets:
    """Detects the screen region and corner radius from the alpha channel.

    Each scan runs inward from an edge along the center row or column
    until it has crossed the bezel and reached the first fully
    transparent pixel, which marks the screen boundary. The scans
    match those of MockupKit's `ScreenInsets.detect(from:)`, so the
    result is what MockupKit will detect from the output frame.
    """
    alpha = image.getchannel("A").load()
    width, height = image.size
    center_x, center_y = width // 2, height // 2

    def opaque(
        x: int,
        y: int,
    ) -> bool:
        return alpha[x, y] > 0

    def scan(coordinates: Iterable[tuple[int, int, int]]) -> int:
        found_bezel = False
        for position, x, y in coordinates:
            if opaque(x, y):
                found_bezel = True
            elif found_bezel:
                return position
        return 0

    if opaque(center_x, center_y):
        raise ValueError(
            "The center pixel is opaque; "
            "the screen opening must be transparent."
        )

    top = scan((y, center_x, y) for y in range(height))
    bottom = scan(
        (height - 1 - y, center_x, y) for y in range(height - 1, -1, -1)
    )

    left = scan((x, x, center_y) for x in range(width))
    right = scan(
        (width - 1 - x, x, center_y) for x in range(width - 1, -1, -1)
    )

    # The corner radius comes from two scans that start at the screen's
    # top-left bounding-box corner and run along its top row and its
    # left column. Each passes any transparency outside the body and
    # the bezel, then stops at the first screen pixel; the distance
    # covered is the corner's extent along that edge. The geometric
    # mean of the two extents approximates the visual corner radius,
    # and a square corner, which starts both scans on screen pixels,
    # measures zero.
    vertical_extent = scan(
        (y - top, left, y) for y in range(top, center_y)
    )

    horizontal_extent = scan(
        (x - left, x, top) for x in range(left, center_x)
    )

    if vertical_extent > 0 and horizontal_extent > 0:
        corner_radius = math.sqrt(vertical_extent * horizontal_extent)
    else:
        corner_radius = max(vertical_extent, horizontal_extent)

    return ScreenInsets(
        bottom=bottom,
        corner_radius=round(corner_radius, 1),
        left=left,
        right=right,
        top=top,
    )


def device_part_origin(layout: dict) -> tuple[int, int]:
    """Returns where the display part sits inside the portrait layout."""
    for value in layout["layouts"]["portrait"].values():
        if isinstance(value, dict) and value.get("name") == "device":
            return int(value["x"]), int(value["y"])
    raise ValueError("The layout has no portrait part named 'device'.")


def main() -> None:
    if len(sys.argv) != 3:
        print(
            f"Usage: {os.path.basename(sys.argv[0])} "
            "<skin-directory> <output-png>"
        )
        sys.exit(1)

    frame = build_frame(sys.argv[1])
    frame.save(sys.argv[2], format="PNG", optimize=True)

    insets = detect_screen_insets(frame)
    screen_width = frame.size[0] - insets.left - insets.right
    screen_height = frame.size[1] - insets.top - insets.bottom
    print(f"Wrote {sys.argv[2]} ({frame.size[0]}x{frame.size[1]})")
    print(
        "MockupKit will detect screen insets "
        f"top={insets.top} left={insets.left} "
        f"bottom={insets.bottom} right={insets.right} "
        f"cornerRadius={insets.corner_radius} "
        f"(screen {screen_width}x{screen_height})"
    )


def opaque_mask(image: Image.Image) -> Image.Image:
    """Returns a mask that is white where the image has any alpha."""
    return image.getchannel("A").point(lambda v: 255 if v > 0 else 0)


def parse_layout(text: str) -> dict:
    """Parses the emulator skin `layout` format into nested dictionaries.

    Blocks are `name { ... }`; leaves are `name value`. Repeated names
    keep the last value.
    """
    tokens = text.replace("{", " { ").replace("}", " } ").split()
    stack = [{}]
    index = 0
    while index < len(tokens):
        token = tokens[index]
        if token == "}":
            stack.pop()
            index += 1
            continue

        value = tokens[index + 1]
        if value == "{":
            block = {}
            stack[-1][token] = block
            stack.append(block)
        else:
            stack[-1][token] = value
        index += 2
    return stack[0]


if __name__ == "__main__":
    main()
