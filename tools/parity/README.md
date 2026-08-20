# Parity Fixtures

Golden fixtures pinning the iOS wire-format primitives, consumed by the tests in
`subsystem/src/test/resources/parity/`.

`generate_fixtures.swift` reproduces the exact recipes from the iOS sources
(`EncodedHashable.swift`, `TimestampDateFormatterDependency.swift`,
`JSONDependencies.swift`) and executes them with real Foundation/CryptoKit, so the
fixture bytes are ground truth rather than transcription. Regenerate on a Mac with:

```
swift generate_fixtures.swift <output-directory>
```

Fixture contract (see `manifest.json`):

- `encoded_hash_vectors.json` – byte parity required. Kotlin must reproduce `json`
  (Swift `JSONEncoder` string-array encoding, including `\/` escaping) and `sha256`
  exactly.
- `timestamp_vectors.json` – byte parity required for `formatted`
  (`yyyy-MM-dd HH:mm:ss zzz`, `en_US_POSIX`, UTC – renders `GMT`); reparsing must
  yield `reparsedEpochMillis`.
- `timestamp_parse_vectors.json` – parse tolerance. A null `epochMillis` means the
  iOS formatter rejects the string; the port must reject it too.
- `user.json` / `conversation.json` / `message.json` – structural parity. Decode →
  re-encode must be structurally equal (RTDB carries structure, not bytes).
- `type_hashes.json` – identity hashes for the structural fixtures, with their
  sorted factor lists, computed with the real recipe.

The wire-format rules these fixtures verify are documented in the iOS repo's
`SCHEMA.md`.
