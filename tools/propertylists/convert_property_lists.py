#!/usr/bin/env python3
#
#  convert_property_lists.py
#
#  Created by Grant Brooks Goodman.
#  Copyright © NEOTechnica Corporation. All rights reserved.
#
#  Converts the iOS CallingCodes.plist ({regionCode: callingCode}) and
#  LookupTables.plist ({numberLength: [callingCode]}) into the JSON
#  assets the Android phone/region services read.
#
#  Usage: python3 convert_property_lists.py

import json
import os
import plistlib

REPOS = os.path.expanduser("~/Documents/Development/Repositories")
OUT_DIR = os.path.join(
    REPOS, "panther-android/app/src/main/assets/propertylists"
)
PLIST_DIR = os.path.join(REPOS, "panther/Sources/Resources/Property Lists")

SOURCES = {
    "calling_codes.json": "CallingCodes.plist",
    "lookup_tables.json": "LookupTables.plist",
}


def main() -> None:
    os.makedirs(OUT_DIR, exist_ok=True)
    for out_name, plist_name in SOURCES.items():
        with open(os.path.join(PLIST_DIR, plist_name), "rb") as handle:
            data = plistlib.load(handle)

        out_path = os.path.join(OUT_DIR, out_name)
        with open(out_path, "w", encoding="utf-8") as handle:
            json.dump(data, handle, ensure_ascii=False, sort_keys=True, indent=0)

        print(f"Wrote {out_name}: {len(data)} keys")


if __name__ == "__main__":
    main()
