# Wire-Format Fixtures

Golden fixtures pinning the wire-format primitives, consumed by the tests
under `subsystem/src/test/resources/parity/`.

`generate_fixtures.swift` computes each fixture with Foundation and CryptoKit,
so the fixture bytes are the reference output of the recipes the Kotlin
implementations reproduce. Regenerate on a Mac with:

```
swift generate_fixtures.swift <output-directory>
```

Fixture contract (see `manifest.json`):

- `encoded_hash_vectors.json` – byte-exact. The Kotlin encoder reproduces
  `json` (`JSONEncoder` string-array encoding, including `\/` escaping) and
  `sha256` exactly.
- `timestamp_vectors.json` – byte-exact for `formatted`
  (`yyyy-MM-dd HH:mm:ss zzz`, `en_US_POSIX`, UTC – renders `GMT`); reparsing
  yields `reparsedEpochMillis`.
- `timestamp_parse_vectors.json` – parse tolerance. A null `epochMillis` means
  the string is rejected.
- `user.json` / `conversation.json` / `message.json` – structural. Decoding and
  re-encoding yields a structurally equal value (the database carries
  structure, not bytes).
- `type_hashes.json` – identity hashes for the structural fixtures, with their
  sorted factor lists, computed with the hashing recipe.
