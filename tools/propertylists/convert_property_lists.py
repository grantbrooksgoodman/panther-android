#!/usr/bin/env python3
#
#  convert_property_lists.py
#
#  Created by Grant Brooks Goodman.
#  Copyright © NEOTechnica Corporation. All rights reserved.
#
#  Converts CallingCodes.plist ({regionCode: callingCode}) and
#  LookupTables.plist ({numberLength: [callingCode]}) into the JSON
#  assets the phone and region services read.
#
#  Usage: python3 convert_property_lists.py <property-lists-directory>

import json
import os
import plistlib
import sys

OUT_DIR = os.path.normpath(
    os.path.join(
        os.path.dirname(os.path.abspath(__file__)),
        "..", "..", "app", "src", "main", "assets", "propertylists",
    )
)

SOURCES = {
    "calling_codes.json": "CallingCodes.plist",
    "lookup_tables.json": "LookupTables.plist",
}


def main() -> None:
    if len(sys.argv) != 2:
        print(f"Usage: {os.path.basename(sys.argv[0])} <property-lists-directory>")
        sys.exit(1)

    plist_dir = sys.argv[1]
    os.makedirs(OUT_DIR, exist_ok=True)
    for out_name, plist_name in SOURCES.items():
        with open(os.path.join(plist_dir, plist_name), "rb") as handle:
            data = plistlib.load(handle)

        out_path = os.path.join(OUT_DIR, out_name)
        with open(out_path, "w", encoding="utf-8") as handle:
            json.dump(data, handle, ensure_ascii=False, sort_keys=True, indent=0)

        print(f"Wrote {out_name}: {len(data)} keys")


if __name__ == "__main__":
    main()
