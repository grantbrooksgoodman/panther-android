#!/usr/bin/env python3
#
#  convert_localized_strings.py
#
#  Created by Grant Brooks Goodman.
#  Copyright © NEOTechnica Corporation. All rights reserved.
#
#  Converts localized-strings property lists (keyed
#  snake_case_key -> {language_code: value}) into the JSON assets
#  LocalizedStringResolver reads. The app resolves strings for the
#  user's chosen language, not the device locale, so each source
#  becomes a single aggregated JSON asset rather than per-locale
#  res/values strings.
#
#  Usage: python3 convert_localized_strings.py <app-plist> <subsystem-plist>

import json
import os
import plistlib
import sys

OUT_DIR = os.path.normpath(
    os.path.join(
        os.path.dirname(os.path.abspath(__file__)),
        "..", "..", "app", "src", "main", "assets", "localization",
    )
)

OUTPUT_NAMES = [
    "localized_strings_app.json",
    "localized_strings_subsystem.json",
]


def main() -> None:
    if len(sys.argv) != len(OUTPUT_NAMES) + 1:
        print(f"Usage: {os.path.basename(sys.argv[0])} <app-plist> <subsystem-plist>")
        sys.exit(1)

    os.makedirs(OUT_DIR, exist_ok=True)
    for out_name, plist_path in zip(OUTPUT_NAMES, sys.argv[1:]):
        with open(plist_path, "rb") as handle:
            data = plistlib.load(handle)

        out_path = os.path.join(OUT_DIR, out_name)
        with open(out_path, "w", encoding="utf-8") as handle:
            json.dump(data, handle, ensure_ascii=False, sort_keys=True, indent=0)

        print(f"Wrote {out_name}: {len(data)} keys")


if __name__ == "__main__":
    main()
