// Golden fixture generator for the Panther Android port.
//
// Reproduces the exact primitives the iOS app uses for wire-format
// identity so the Kotlin port can verify byte parity:
//
// 1. EncodedHashable: SHA-256 over JSONEncoder().encode([String]),
//    lowercase hex via %02x.
//    (app-subsystem/.../EncodedHashable.swift; the encoder is an
//    unconfigured JSONEncoder per JSONDependencies.swift.)
// 2. TimestampDateFormatterDependency: DateFormatter with
//    "yyyy-MM-dd HH:mm:ss zzz", en_US_POSIX, UTC.
//
// Run: swift generate_fixtures.swift <output-directory>

import CryptoKit
import Foundation

// MARK: - Primitives (verbatim recipes from the iOS sources)

let jsonEncoder = JSONEncoder()

func encodedJSON(_ factors: [String]) -> String {
    let data = try! jsonEncoder.encode(factors)
    return String(data: data, encoding: .utf8)!
}

func sha256Hex(_ data: Data) -> String {
    SHA256.hash(data: data).compactMap { String(format: "%02x", $0) }.joined()
}

func encodedHash(_ factors: [String]) -> String {
    sha256Hex(try! jsonEncoder.encode(factors))
}

let timestampFormatter: DateFormatter = {
    let formatter = DateFormatter()
    formatter.dateFormat = "yyyy-MM-dd HH:mm:ss zzz"
    formatter.locale = .init(identifier: "en_US_POSIX")
    formatter.timeZone = .init(identifier: "UTC")
    return formatter
}()

// MARK: - Fixture: encoded-hash vectors

struct HashVector: Codable {
    let factors: [String]
    let json: String
    let jsonBytesBase64: String
    let sha256: String
}

let hashFactorCases: [[String]] = [
    [],
    [""],
    ["!"],
    ["abc"],
    ["a", "b", "c"],
    ["ab", "c"],
    ["a", "bc"],
    ["a/b"],
    ["a\"b"],
    ["a\\b"],
    ["a\nb"],
    ["a\tb"],
    ["a\rb"],
    ["a\u{01}b"],
    ["a\u{08}b"],
    ["a\u{0B}b"],
    ["a\u{0C}b"],
    ["a\u{1F}b"],
    ["a\u{7F}b"],
    ["café"],
    ["ñandú"],
    ["日本語"],
    ["🐆"],
    ["a – b"],
    ["key | hash"],
    ["userID: !"],
    ["true", "false"],
    ["2026-08-19 21:30:00 GMT"],
    // Realistic PhoneNumber factor set (sorted, per PhoneNumber.hashFactors).
    ["", "", "5551234567", "1", "US"].sorted(),
    // Realistic Activity factor set (sorted, per Activity.hashFactors).
    ["LEFT", "1970-01-01 00:00:00 GMT", "!"].sorted(),
]

let hashVectors = hashFactorCases.map { factors in
    HashVector(
        factors: factors,
        json: encodedJSON(factors),
        jsonBytesBase64: (try! jsonEncoder.encode(factors)).base64EncodedString(),
        sha256: encodedHash(factors)
    )
}

// MARK: - Fixture: timestamp vectors

struct TimestampVector: Codable {
    let epochMillis: Int64
    let formatted: String
    let reparsedEpochMillis: Int64
}

let timestampEpochs: [Double] = [
    0,
    1,
    -1,
    86_399,
    951_827_696, // 2000-02-29 (leap day)
    1_234_567_890,
    1_755_642_600, // 2026-era value
    4_102_444_799, // 2099-12-31 23:59:59
    1_755_642_600.999, // fractional seconds truncate on format
]

let timestampVectors = timestampEpochs.map { epoch -> TimestampVector in
    let date = Date(timeIntervalSince1970: epoch)
    let formatted = timestampFormatter.string(from: date)
    let reparsed = timestampFormatter.date(from: formatted)!
    return TimestampVector(
        epochMillis: Int64((epoch * 1000).rounded()),
        formatted: formatted,
        reparsedEpochMillis: Int64((reparsed.timeIntervalSince1970 * 1000).rounded())
    )
}

// Parse-tolerance vectors: zone tokens the formatter accepts.
struct ParseVector: Codable {
    let string: String
    let epochMillis: Int64?
}

let parseCases = [
    "2026-08-19 21:30:00 GMT",
    "2026-08-19 21:30:00 UTC",
    "2026-08-19 21:30:00 GMT+2",
    "2026-08-19 21:30:00 PST",
    "2026-08-19 21:30:00",
    "not a date",
]

let parseVectors = parseCases.map { string -> ParseVector in
    guard let date = timestampFormatter.date(from: string) else {
        return ParseVector(string: string, epochMillis: nil)
    }
    return ParseVector(
        string: string,
        epochMillis: Int64((date.timeIntervalSince1970 * 1000).rounded())
    )
}

// MARK: - Fixture: full-type structural vectors

// Fixed inputs chosen to exercise every encoding rule in SCHEMA.md.
// Hashes are computed with the real recipe so the fixtures are
// internally consistent.

let fixedDate = Date(timeIntervalSince1970: 1_755_642_600)
let fixedDateString = timestampFormatter.string(from: fixedDate)
let epochZeroString = timestampFormatter.string(from: Date(timeIntervalSince1970: 0))

// PhoneNumber (label/internalFormattedString nil per decode path).
let phoneNumberEncoded: [String: Any] = [
    "callingCode": "1",
    "nationalNumberString": "5551234567",
    "regionCode": "US",
]
let phoneNumberHashFactors = ["", "", "5551234567", "1", "US"].sorted()
let phoneNumberHash = encodedHash(phoneNumberHashFactors)

// User.
let userID = "androidFixtureUser0001"
let conversationKey = "-FixtureConversation01"
let peerUserID = "androidFixtureUser0002"

// Conversation content, from which the conversation hash derives.
let activityFactors = ["LEFT", epochZeroString, "!"].sorted()
let emptyActivityHash = encodedHash(activityFactors)
let emptyReactionMetadataFactors = ["!", "!", "LOVE"].sorted()
let emptyReactionMetadataHash = encodedHash(emptyReactionMetadataFactors)

let messageID = "-FixtureMessage000001"

var conversationHashFactors: [String] = [conversationKey]
conversationHashFactors.append(emptyActivityHash)
conversationHashFactors.append(messageID)
conversationHashFactors.append("!") // metadata.name is "!" in this fixture
conversationHashFactors.append("!") // imageHash nil
conversationHashFactors.append("false") // isPenPalsConversation
conversationHashFactors.append(fixedDateString)
conversationHashFactors.append("\(peerUserID): false")
conversationHashFactors.append("\(userID): !")
conversationHashFactors.append("\(peerUserID): !")
conversationHashFactors.append("\(userID): !")
conversationHashFactors.append("!") // requiresConsentFromInitiator nil
conversationHashFactors.append("\(userID) | false")
conversationHashFactors.append("\(peerUserID) | false")
// reactionMetadata decodes [empty] back to nil, so a round-tripped
// conversation excludes reaction hashes from its factors. The empty
// activity placeholder, by contrast, decodes to [emptyActivity] and
// DOES contribute emptyActivityHash (see Conversation decode).
let conversationHash = encodedHash(conversationHashFactors.sorted())

let userHashFactors: [String] = [
    "false", // aiEnhancedTranslationsEnabled
    "\(conversationKey) | \(conversationHash)",
    "fixture-device-id-0001", // deviceID
    "false", // isPenPalsParticipant
    "en", // languageCode
    "false", // messageRecipientConsentRequired
    phoneNumberHash,
].sorted()
let userHash = encodedHash(userHashFactors)

let userEncoded: [String: Any] = [
    "id": userID,
    "aiEnhancedTranslationsEnabled": false,
    "blockedUserIDs": [String: Bool](),
    "openConversations": [conversationKey: conversationHash],
    "deviceID": "fixture-device-id-0001",
    "isPenPalsParticipant": false,
    "languageCode": "en",
    "messageRecipientConsentRequired": false,
    "phoneNumber": phoneNumberEncoded,
    "previousLanguageCodes": ["!"],
    "pushTokens": [String: Bool](),
]

let conversationEncoded: [String: Any] = [
    "id": "\(conversationKey) | \(conversationHash)",
    "activities": [[
        "action": "LEFT",
        "date": epochZeroString,
        "userID": "!",
    ]],
    "hash": conversationHash,
    "messages": [messageID: true],
    "metadata": [
        "imageData": "!",
        "isPenPalsConversation": false,
        "lastModified": fixedDateString,
        "messageRecipientConsentAcknowledgementData": [
            "\(peerUserID): false",
            "\(userID): !",
        ].sorted(),
        "name": "!",
        "penPalsSharingData": [
            "\(peerUserID): !",
            "\(userID): !",
        ].sorted(),
        "requiresConsentFromInitiator": "!",
    ] as [String: Any],
    "participants": [
        userID: [
            "hasDeletedConversation": false,
            "isTyping": false,
        ],
        peerUserID: [
            "hasDeletedConversation": false,
            "isTyping": true,
        ],
    ],
    "reactionMetadata": [[
        "messageID": "!",
        "reactions": [[
            "style": "LOVE",
            "userID": "!",
        ]],
    ] as [String: Any]],
]

let messageHashFactors: [String] = [
    messageID,
    userID,
    "text",
    fixedDateString,
    "\(peerUserID) | \(fixedDateString)",
].sorted()
let messageHash = encodedHash(messageHashFactors)

let messageEncoded: [String: Any] = [
    "id": messageID,
    "fromAccount": userID,
    "contentType": "text",
    "translations": ["en-es | -FixtureTranslation01"],
    "readReceipts": ["\(peerUserID) | \(fixedDateString)"],
    "sentDate": fixedDateString,
]

// MARK: - Output

struct Manifest: Codable {
    let generatedAt: String
    let generator: String
    let swiftVersion: String
    let notes: [String]
}

let outputDirectory = URL(
    fileURLWithPath: CommandLine.arguments.count > 1
        ? CommandLine.arguments[1]
        : "."
)

try! FileManager.default.createDirectory(
    at: outputDirectory,
    withIntermediateDirectories: true
)

func writeJSON(_ object: Any, to fileName: String) {
    let data = try! JSONSerialization.data(
        withJSONObject: object,
        options: [.prettyPrinted, .sortedKeys]
    )
    try! data.write(to: outputDirectory.appendingPathComponent(fileName))
    print("Wrote \(fileName)")
}

func writeEncodable(_ value: some Encodable, to fileName: String) {
    let encoder = JSONEncoder()
    encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
    let data = try! encoder.encode(value)
    try! data.write(to: outputDirectory.appendingPathComponent(fileName))
    print("Wrote \(fileName)")
}

writeEncodable(hashVectors, to: "encoded_hash_vectors.json")
writeEncodable(timestampVectors, to: "timestamp_vectors.json")
writeEncodable(parseVectors, to: "timestamp_parse_vectors.json")
writeJSON(userEncoded, to: "user.json")
writeJSON(conversationEncoded, to: "conversation.json")
writeJSON(messageEncoded, to: "message.json")
writeJSON(
    [
        "conversationHash": conversationHash,
        "conversationHashFactorsSorted": conversationHashFactors.sorted(),
        "emptyActivityHash": emptyActivityHash,
        "emptyReactionMetadataHash": emptyReactionMetadataHash,
        "messageHash": messageHash,
        "messageHashFactorsSorted": messageHashFactors,
        "phoneNumberHash": phoneNumberHash,
        "phoneNumberHashFactorsSorted": phoneNumberHashFactors,
        "userHash": userHash,
        "userHashFactorsSorted": userHashFactors,
    ] as [String: Any],
    to: "type_hashes.json"
)

writeEncodable(
    Manifest(
        generatedAt: ISO8601DateFormatter().string(from: Date()),
        generator: "generate_fixtures.swift (Panther Android port, Phase 1)",
        swiftVersion: "run `swift --version` on the generating machine",
        notes: [
            "encoded_hash_vectors: byte parity required – Kotlin must reproduce json/sha256 exactly.",
            "timestamp_vectors: byte parity required for `formatted`; reparse must match epochMillis.",
            "timestamp_parse_vectors: parse tolerance – null epochMillis means the iOS formatter rejects the string.",
            "user/conversation/message: structural parity – decode → re-encode must be structurally equal (RTDB carries structure, not bytes).",
            "type_hashes: identity-hash parity for the structural fixtures, computed with the real recipe.",
        ]
    ),
    to: "manifest.json"
)

print("Done.")
