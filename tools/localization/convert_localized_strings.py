#!/usr/bin/env python3
#
#  convert_localized_strings.py
#
#  Created by Grant Brooks Goodman.
#  Copyright © NEOTechnica Corporation. All rights reserved.
#
#  Converts the iOS LocalizedStrings.plist files (keyed
#  snake_case_key -> {language_code: value}) into the JSON assets the
#  Android LocalizedStringResolver reads. The app's runtime resolves
#  strings for the user's chosen language, not the device locale, so a
#  single aggregated JSON per source is the faithful port of the plist —
#  not per-locale res/values strings.xml.
#
#  Usage: python3 convert_localized_strings.py

import json
import os
import plistlib

REPOS = os.path.expanduser("~/Documents/Development/Repositories")
OUT_DIR = os.path.join(
    REPOS, "panther-android/app/src/main/assets/localization"
)

SOURCES = {
    "localized_strings_app.json": os.path.join(
        REPOS, "panther/Sources/Resources/Property Lists/LocalizedStrings.plist"
    ),
    "localized_strings_subsystem.json": os.path.join(
        REPOS, "app-subsystem/Sources/Resources/LocalizedStrings.plist"
    ),
}


def main() -> None:
    os.makedirs(OUT_DIR, exist_ok=True)
    for out_name, plist_path in SOURCES.items():
        with open(plist_path, "rb") as handle:
            data = plistlib.load(handle)

        out_path = os.path.join(OUT_DIR, out_name)
        with open(out_path, "w", encoding="utf-8") as handle:
            json.dump(data, handle, ensure_ascii=False, sort_keys=True, indent=0)

        print(f"Wrote {out_name}: {len(data)} keys")


if __name__ == "__main__":
    main()
