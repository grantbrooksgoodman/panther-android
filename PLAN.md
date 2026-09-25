# Panther Android — Parity II Plan (Session Module & Splash, 1:1)

**Audience:** the Claude Code instance implementing this plan. It has full local access to `panther-android`, the iOS `panther` repo, and the iOS package repos (`app-subsystem`, `networking`, `alert-kit`, `component-kit`, `translator`), plus the local `~/Documents/ANDROID` directory that holds `ANDROID_PARITY_PLAN.md`, `PARITY_REPORT.md`, `DEVIATIONS.md`, `STYLE_RULES_ANDROID.md`, and `DOC_RULES.md`.

**Posture:** the iOS app is the specification, down to member names. This plan finishes what the first R-phase pass deferred, re-validates every recorded deviation, and brings two domains to as close to 1:1 business-logic emulation as Android allows: (1) `SessionStore` and the wider iOS `Modules/Session` module (State, Sync, Entity, `ClientSession`), and (2) `SplashPageViewService` with its reducer and view. Everything else in this plan exists to make those two domains faithful, or to close gaps the first pass left.

**No Git.** Do not run any git command (no status, diff, add, commit, stash, branch, or push). Assume the repositories are already at the right revision. Deliver working-tree changes plus the two tracking files described in §2.

---

## 1. Decisions Already Made (do not re-open)

Grant answered these before the plan was written. Treat them as fixed.

| # | Decision | Consequence for you |
|---|---|---|
| D-II-1 | **Relocate the Session module and the app-level Networking layer into `:app`**, mirroring the iOS module layout. | Phase 0. `:networking` becomes the analog of the iOS `networking` *package* only (delegates, engine, hosted translation, health, cache). Session services may then call AlertKit, HUD, Toast, `ContactService`, `Navigation`, and localization directly, exactly as iOS does. |
| D-II-2 | **`IntegrityService`, `IntegrityServiceSession`, `IntegrityService+CommonNetworkingExtensions` (`repairDatabase`), and `SchemaMigrationService` are deferred to a separate plan.** | `SplashPageViewService.performRetryHandler()` is ported with the repair call absent (see Phase 6). `AccountDeletionService` keeps its no-repair note. Record one ledger entry naming the deferred files. Never write a stand-in "repair". |
| D-II-3 | **`Application.reset(preserveCurrentUserID:onCompletion:)` is ported faithfully, including `.exitGracefully` process exit.** | Phase 1. Clear Caches and Change Language become "Apply & Exit" flows exactly as on iOS; sign-out on `deviceID` change and the splash cache-invalid path use the same reset. |
| D-II-4 | **Android reads its own forced-update metadata from new hosted keys**: `shared/playStoreBuildNumber` (Int), `shared/shouldForceUpdateAndroid` (Bool), `shared/playStoreShareLink` (String). Grant adds them server-side in every environment. | Phase 5. Until the keys exist, their absence resolves to "not resolved" rather than throwing (a temporary, ledgered divergence from iOS's strict `assignValues`). |
| D-II-5 | **Keep the media staging model** (attach → preview → send). | The ledger entry is re-labelled as an intentional product decision, not a constraint. Do not switch to iOS's immediate send. |
| D-II-6 | **Keep the separate `NewChatPageView`** (decision D1 stands). | Only member names and the `ConversationStagingService`-style semantics are aligned. No merge into `ChatPageView`. |
| D-II-7 | **New dependencies are allowed**: Media3 Transformer (video transcoding), ZXing core (QR), Play In-App Review, and a pure-Kotlin LZFSE codec port (no library). | Phase 9. Pin versions in `gradle/libs.versions.toml`; prove each resolves against AGP 9.3.2 / compileSdk 37 before building on it. |
| D-II-8 | **Prevarication mode is cut with UI themes.** | `MetadataService.isPrevaricationModeEnabled` round-trips the hosted value; `checkPrevaricationMode`, `Application.isInPrevaricationMode`, and every call-site branch are omitted. One ledger entry. |

Carry-over from `ANDROID_PARITY_PLAN.md` §3 (still cut): AI-enhanced translations, audio message **sending**, data usage monitoring, message recipient consent, PenPals, typing indicator (including the launch-time `resetTypingIndicatorStatusForCurrentUser` write), UI themes. Also still cut by Grant's earlier directive: Retry Translation. Deferred developer tooling (not cut, but out of this plan unless a phase below names it): `UserTestingService`, `ConversationStagingService`, `RollbackService`, `DevModeActions*`, `BreadcrumbsCaptureService`.

---

## 2. Working Agreement

### 2.1 Rules files are authoritative

Before writing any code, locate and read **`STYLE_RULES_ANDROID.md`** and **`DOC_RULES.md`** in full (`~/Documents/ANDROID`; if they are not there, search the repo root and stop to ask Grant for the path rather than guessing). Every file you touch must conform to both. `detekt`/`ktlint` are the mechanical floor; where the rules files are stricter, the rules files win. Port the iOS header doc comments (the behavior contracts) as KDoc per `DOC_RULES.md`, adapting only Swift-specific references.

### 2.2 iOS naming is verbatim

- Type, property, method, parameter, enum-case, constant, and action names are copied from iOS **verbatim**, transformed only by Kotlin's casing conventions: `UPPER_SNAKE` for `const val`/enum entries, `PascalCase` for sealed `Action` classes and types, `camelCase` for everything else.
- Swift argument labels: use the iOS *internal* parameter name (the one the body uses) as the Kotlin parameter name; e.g. `entries(forConversationIDKey conversationIDKey:)` → `entries(conversationIDKey: String)`. Where iOS has only a label (`update(\.pushTokens, to: value)`), keep the label as the parameter name (`to`).
- Reducer actions keep the iOS suffixes exactly: `viewAppeared` → `ViewAppeared`, `signOutButtonTapped` → `SignOutButtonTapped` (the first pass dropped "Button"; this plan restores it — see Phase 10), `initializedBundle(Exception?)` → `InitializedBundle(val exception: Exception?)`.
- Named tuples (`(value: Conversation?, isPenPalsConversation: Bool)`) become a `data class` whose property names match the tuple labels, named after how iOS refers to it in prose/code (`ConversationTuple`).
- Files mirror iOS file names, including the `Type+Concern.kt` extension-file convention required by `STYLE_RULES_ANDROID.md` §2 (`MessageOutboxServiceRetry.kt` → `MessageOutboxService+Retry.kt`).
- Where iOS disables `file_length`/`type_body_length` for a file, split the Kotlin `object` across `+Concern.kt` files using `internal` visibility for cross-file helpers (the pattern `MessageOutboxService+Retry.kt` already uses). Suppress `LargeClass` only with a comment citing the iOS `swiftlint:disable`.
- Access paths mirror iOS dependency paths: session state is reached through `DependencyValues.current.clientSession.entity.user.currentUser`, never through a bare `UserSessionService.currentUser`, in every file this plan touches (Phase 10 sweeps the rest).

### 2.3 Verification loop

After every numbered item: `./gradlew :app:compileDevelopmentDebugKotlin detekt test`. Do not proceed on red. Where an item is UI-visible and the `Pixel_8` emulator is available, launch and exercise it; otherwise say so in the progress file. Extend the parity fixtures (`*/src/test/resources/parity/`) for every wire surface you touch; hand-derived fixtures carry a `// hand-derived` comment.

### 2.4 Check-in protocol (mandatory)

At the end of **every phase** (0 through 10):

1. Run the full verification loop and record the result.
2. Update `~/Documents/ANDROID/PARITY_II_PROGRESS.md` (template §2.5).
3. Append every new deviation to `~/Documents/ANDROID/PARITY_II_DEVIATIONS.md` (template §2.6).
4. **Stop.** Post a check-in to Grant containing: phase name; what shipped (per item: ✅ ported / 🟡 simplified / 🔀 mapped / ⏸ deferred / ✂️ cut); verification evidence (command output summary, emulator walkthroughs); deviations added; open questions. Then **wait for Grant's explicit go-ahead before starting the next phase.** Do not batch phases. If context runs long mid-phase, finish the current numbered item and its tests, update the progress file, and check in early rather than leaving a half-wired item.

### 2.5 `PARITY_II_PROGRESS.md` template

```markdown
# Android Parity II — Progress

Last updated: <date> · Current phase: <n> · Verification: `./gradlew :app:compileDevelopmentDebugKotlin detekt test` <green/red> at <time>

## Phase table
| Phase | Status | Started | Checked in | Notes |
|---|---|---|---|---|

## Baseline (Phase 0.1)
<PARITY_REPORT gaps re-verified against HEAD; items already closed by post-report commits>

## Phase <n> — <name>
| Item | Status | Evidence | Deviation IDs |
|---|---|---|---|
<one row per numbered item>

## Open questions for Grant
```

### 2.6 `PARITY_II_DEVIATIONS.md` template (same style as `DEVIATIONS.md`)

```markdown
# Android Parity II — Deviation Ledger

Every place Android diverges from iOS after Parity II, with the forcing constraint and the chosen
mapping. Entries retired from `DEVIATIONS.md` are listed under "Retired" with the phase that fixed them.

## <Area> (Phase <n>)
| ID | iOS | Android | Forcing constraint | Supersedes (DEVIATIONS.md) |
|---|---|---|---|---|
| II-<n>.<k> | … | … | … | <row or "new"> |

## Retired
| DEVIATIONS.md entry | Retired by | How |
```

IDs are `II-<phase>.<sequence>`. Every entry names a *forcing* constraint; a preference is not a constraint.

### 2.7 Reading list

Read the iOS file for an item **before** writing its Kotlin, end-to-end, header contract included. Package-level types (`SingleSlotCoalescer`, `Task.debounced`, `Task.delayed`, `LockIsolated`, `CacheDomain`, `Persistent`, `NetworkHealth*`, `StorageMetadata`, `RemotelyUpdatable`, `WriteAction`, `AKErrorAlert.TranslationOptionKey`, `AlertKit.ReportDelegate`) live in the package repos; read them there.

---
## 3. Phase 0 — Re-baseline and Module Relocation

*Goal: an honest starting point and an iOS-shaped module layout, with zero logic change.*

### 0.1 Re-baseline the gap list against HEAD

`PARITY_REPORT.md` predates these Android commits: delivery progress bar + `DeliveryProgressIndicatorService` (6e55510), Send spinner + `isSendingMessage` (ae95d4d), New Chat sends through `MessageDeliveryService` + `Conversation.empty/mock` (ae9ab15), translated alert prompts + success HUD + `present(translating:)` (8341e02), conversation-cell long-press Delete/Block/Report (12726fb), context-menu dismissal on speech end (c4c177b), contact selector on the chat sheet + add participant (fd93a0d, 3108047). Re-verify every ⏸/🟡 row in `PARITY_REPORT.md` and every "Remaining gaps" entry by **reading the code**, not the commit messages, and write the corrected list into the progress file's *Baseline* section. Anything already closed is dropped from Phases 8–9.

### 0.2 Create the tracking files

Create `PARITY_II_PROGRESS.md` and `PARITY_II_DEVIATIONS.md` from §2.5/§2.6.

### 0.3 Relocate the app-level layers into `:app`

Pure moves and package renames: **no behavior or naming changes** beyond package declarations and imports. Target packages mirror iOS directories.

| From (`networking/src/main/java/us/neotechnica/panther/networking/modules/…`) | To (`app/src/main/java/us/neotechnica/panther/modules/…`) | iOS home |
|---|---|---|
| `schema/conversation/models/*` | `networking/conversation/models/*` | `Modules/Networking/Sources/Conversation/Models` |
| `schema/message/models/*` | `networking/message/models/*` | `…/Message/Models` |
| `schema/user/models/*` | `networking/user/models/*` | `…/User/Models` |
| `schema/common/models/PhoneNumber.kt` | `common/models/PhoneNumber.kt` | `Modules/Common/Models/PhoneNumber.swift` |
| `conversation/services/ConversationService.kt` | `networking/conversation/services/` | `…/Conversation/Services` |
| `message/services/{Message,MediaMessage,AudioMessage}Service.kt` | `networking/message/services/` | `…/Message/Services` |
| `user/services/UserService.kt`, `user/models/DeviceID.kt` | `networking/user/{services,models}/` | `…/User/{Services,Models}` |
| `common/models/{MediaFileExtension,CommonConstants}.kt` | `common/models/MediaFileExtension.kt`, `common/constants/CommonConstants.kt` | `Modules/Common/{Models,Constants}` |
| `common/services/{AnalyticsService,ConnectionStatusService,MetadataService}.kt` | `common/services/` | `Modules/Common/Services/Sources` |
| `session/services/{SessionStore,MessageOutboxService,MessageOutboxServiceRetry,PendingTranslationArchive,SelfWriteRegistry}.kt`, `session/models/{OutboxEntry,SelfWriteRecord,SessionStoreChange}.kt` | `session/state/{services,models}/` (rename `MessageOutboxServiceRetry.kt` → `MessageOutboxService+Retry.kt`) | `Modules/Session/State` |
| `session/services/ConversationObserverService.kt` | `session/sync/services/` | `Modules/Session/Sync/Services` |
| `session/services/{Activity,Conversation,Message,Moderation,Reaction,User}SessionService.kt`, `session/interfaces/DeliveryProgressIndicatorProtocol.kt`, `session/constants/*`, `session/extensions/*` | `session/entity/{services,protocols,constants,extensions}/` | `Modules/Session/Entity` |
| `session/services/MessageDeliveryService.kt` | `content/user/services/` | `Content/User/Services/MessageDeliveryService.swift` |
| `session/services/{AccountDeletionService,NotificationSessionService}.kt` | `common/services/` | `Common/Services/Sources/{AccountDeletion,Notification}Service.swift` (renamed in Phase 5) |
| `session/services/UserMutationService.kt` | `networking/user/services/` | none — absorbed in Phases 3/5 |
| `session/services/{SignOutService,CacheClearingService,LanguageChangeService}.kt` | `content/user/services/` | none — absorbed by `SettingsPageViewService`/`ChangeLanguagePageViewService` in Phase 9 |
| `session/services/MessageTranslationCache.kt` | `networking/message/services/` | none — retired in Phase 9 |
| `networking/src/test/**` (parity tests + `resources/parity/*`) | `app/src/test/**` | — |

Keep in `:networking` (they are the framework package's types): `Networking.kt`, `auth/*`, `database/*`, `storage/*`, `translation/*`, `common/interfaces/*`, `common/extensions/*`, `common/models/{CacheStrategy,DataSample,NetworkEnvironment,NetworkPath}.kt`.

Mechanics:
- `DeviceID` used `Networking.requireContext()` (internal to `:networking`). Give it an app-level `initialize(context)` like `ContactService`, called from `PantherApplication`.
- Add to `app/build.gradle.kts`: `testImplementation(libs.kotlinx.coroutines.test)` and whatever `FixtureJson` needs (check its imports). Move `FixtureJson.kt` with the tests.
- Update `keepRules`/ProGuard only if a moved class was named there (none today; confirm).
- Delete now-empty `:networking` directories.

**AC:** `./gradlew :app:compileDevelopmentDebugKotlin :app:compileStagingDebugKotlin :app:compileProductionDebugKotlin detekt test` green; every moved test passes from `app/src/test`; app launches and reaches the conversations list on the emulator. Check in.

---

## 4. Phase 1 — Kernel Prerequisites and `Application`

*Goal: the subsystem primitives the iOS Session/Splash code assumes, and the app bootstrap/reset the rest of the plan calls into.*

### 1.1 `Persistent` archives and reset
- Add typed archive support mirroring `@Persistent(.conversationArchive) Set<Conversation>?`: `Persistent.archive(key, decode)` / `Persistent.setArchive(key, encoded)` backed by files under `FileStore` (`persistent/<rawValue>.json`, atomic temp-then-rename), storing `encoded` maps from the existing codecs. Ledger the file-vs-`UserDefaults` backing as an implementation mapping (same semantics).
- Port `PersistentStorageKeys.swift` + `PersistentStorageKey+CommonExtensions.swift` + `PersistentStorageKey+EntitySessionExtensions.swift`: scoped key factories (`PersistentStorageKey.sessionStore(SessionStoreStorageKey.CONVERSATION_ARCHIVE)`, `.userSessionService(.CURRENT_USER_ID)`, `.metadataService(…)`, `.updateService(…)`, `.reviewService(…)`, `.application(…)`, `.breadcrumbsCaptureService(…)`, `.contactPairArchiveService(…)`), and the `PermanentKeyDelegate` list. Existing raw keys map onto the scoped ones with the same `rawValue` (no data migration).
- Add `Persistent.reset(preserving: List<PersistentStorageKey>)` (SharedPreferences and the archive directory) and `Persistent.permanentAndSubsystemKeys(plus:)`.
- Testability: `Persistent` and `FileStore` get an in-memory/temp-dir backend installable from JVM tests (`initializeForTesting(...)`), so Phase 2's store tests run without Robolectric.

### 1.2 Task helpers
Port `Task.debounced(id, delay) { }` (keyed cancel-previous, runs after `delay`) and `Task.delayed(by:) { }` from `app-subsystem` into `subsystem/modules/foundation/services/`. Debounce keys mirror iOS: `"<sender context>/<TaskID.rawValue>"`. Make the scope injectable for tests.

### 1.3 `SingleSlotCoalescer`
Port from `app-subsystem` with the same modes (`lastCallerWins` at minimum), alongside `KeyedCoalescer`.

### 1.4 Logger domains and reporting flag
Port `LoggerDomains.swift`: app domains (`analytics`, `bugPrevention`, `chatPageState`, `clientSession`, `contacts`, `conversation`, `conversationObserver`, `conversationStore`, `conversationSync`, `dataIntegrity`, `dataUsage`, `messageStore`, `notifications`, `outbox`, `penPals`, `schemaMigration`, `sessionStore`, `uiCacheInvalidation`, `userSession`, `userStore`, plus `Networking.{auth,health,hostedTranslation,database}`), the `SubscriptionDelegate` (`subscribedDomains`, `domainsExcludedFromSessionRecord`) honored by `Logger`, and `Logger.setReportsErrorsAutomatically(_:)` / `reportsErrorsAutomatically` (auto-file reportable exceptions through the report delegate when true).

### 1.5 Runtime storage keys
Port `StoredItemKeys.swift`: `StoredItemKey.populatedTemporaryCaches`, `.shouldNotifyOfConversationAvailability`, and the `RuntimeStorage` accessors.

### 1.6 Shared events/states
Add the iOS `SharedEvents`/`SharedStates` declarations that ported code consumes (`networkActivityOccurred`, `traitCollectionChanged`, `updatedContactPairArchive`, `firstMessageSentInNewChat`, `currentConversationActivityChanged`, `currentConversationMetadataChanged`, `conversationsPageReappeared`, `chatInfoPageLoadingStateUpdated`, `reloadingConversationIDKeys`, `conversationsSearchQuery`; `networkHealth` arrives with Phase 5.1). Remove the Android-only `currentConversationDidBecomeUnavailable` in Phase 4 when its iOS replacement lands.

### 1.7 Process lifecycle hooks (`AppDelegate`/`SceneDelegate` parity)
Add `androidx.lifecycle:lifecycle-process` (same version as `lifecycle-runtime-ktx`) and, in `PantherApplication`, observe `ProcessLifecycleOwner`: `onStart` ≙ `sceneDidBecomeActive` (`traitCollectionChanged.send()`, `outbox.retryAllEligible()`); `onStop` ≙ `sceneDidEnterBackground` (`store.flushNow()`, `uiCacheInvalidationService.refreshNotificationExtensionNameMap()`, badge update via `notification.setBadgeNumber(currentUser.calculateBadgeNumber())`). `applicationWillTerminate` has no reliable Android analog: map to `onStop` flush + `MainActivity.onDestroy` when `isFinishing` (`flushNow`, `analytics.logEvent(TERMINATE_APP)`); ledger it.

### 1.8 `CacheDomain` registry
Port the subsystem `CacheDomain(name, clear)` type and `CacheDomains.swift` as `bundle/CacheDomains.kt` (`CacheDomain.List.appCacheDomains`), plus `CoreUtilities.clearCaches(domains?)` (all registered domains when `null`). Register every Android cache that exists or lands in this plan: `conversationArchive`/`messageArchive`/`userArchive` (Phase 2), `Networking.database` (`CoreDatabaseStore.clearStore`), `Networking.storage`, `contactService`, `contactPairArchive`, `conversationCellViewData`, `readReceipt`, `userDisplayName`, `userService`, `regionDetailService`, `commonPropertyLists`, `textToSpeechService`, `settingsPageViewService`, `chatInfoPageViewService`. Domains whose iOS cache has no Android counterpart are listed in the progress file, not stubbed.

### 1.9 `Application` (`Sources/Application.swift` + `Application+CommonExtensions.swift`)
Create `app/.../Application.kt` (an `object`):
- `loadStartDate`, `isInStagingMode` (port only if staging assets exist on Android; otherwise omit with a ledger line), `initialize()` mirroring the iOS sequence that has an Android counterpart: delegate registration (cache domain list, exception metadata, forced-update modal ≙ `UpdateService`, logger domain subscription, permanent persistent keys), `Build.initialize` (already in `PantherApplication.configureBuild`; move the call here), `Networking.initialize` + `registerActivityIndicatorDelegate(NetworkActivityIndicatorService)` (Phase 5.11), `setNetworkHealthConfiguration` (Phase 5.1), `database.prewarm()` + `storage.prewarm()` (add storage prewarm), first-run environment selection (emulator → development on first run; general release → production), TTS prewarm (`TextToSpeechService.initialize`). **Do not port** the temporary "Production Footgun Mitigation" `exit(0)` guard (iOS FIXME) or MessageKit swizzling; note both in the progress file.
- `dismissSheets()`: clear every navigator's sheet (`chat`, `settings`, `userContent` — add `sheet` to the navigators that lack one only if a consumer exists; otherwise dismiss what exists).
- `reset(preserveCurrentUserID = false, onCompletion: ResetCompletionProcedure? = null)` **verbatim**: `outbox.removeAll()`, `store.advanceEpoch()`, `sync.conversationObserver.stopObserving()`, `user.stopObservingCurrentUserChanges()` unless preserving, `CoreUtilities.clearCaches()`, erase documents (`filesDir` contents), application-support (`noBackupFilesDir`) and temporary (`cacheDir`) directories, remove the persisted notification-extension contact archive and conversation-name map (Phase 5.8 keys), `Persistent.reset(preserving = permanentAndSubsystemKeys(plus = if (preserveCurrentUserID) [userSessionService(.currentUserID)] else null))`, `RuntimeStorage.remove(.populatedTemporaryCaches)`, `auth.signOut()` (log failure), then the procedure after `dismissSheets()`: `EXIT_GRACEFULLY` → `Overlay.show()` (large white activity indicator), navigate `Root(SetModal(Splash))`, after 1 s `exitGracefully()`; `NAVIGATE_TO_SPLASH` → `UserContent(Stack([]))` + `Root(SetModal(Splash))`.
- `exitGracefully()` (private, the `CoreKit.Utilities.exitGracefully` analog): `currentActivity?.finishAffinity()` then `exitProcess(0)`. Ledger: Android discourages process exit; chosen per D-II-3.

**AC:** unit tests for archives/reset/debounce/coalescer; `Application.reset(preserveCurrentUserID = true)` on the emulator returns the app to a signed-in splash with an empty store and outbox; `test detekt` green. Check in.

---
## 5. Phase 2 — `SessionStore` and the State Sub-module (1:1)

*Goal: `Modules/Session/State` ported clause-for-clause. This is the foundation the splash's cached-user path stands on.*

Read first: `SessionStore.swift`, `AppConstants+SessionStore.swift`, `SessionStoreChange.swift`, `OutboxEntry.swift`, `SelfWriteRecord.swift`, `SelfWriteRegistry.swift`, `PendingTranslationArchive.swift`, `MessageOutboxService.swift`, `MessageOutboxService+Retry.swift`, `OutboxEntry+UserContentExtensions.swift`, `ClientSession.swift`.

### 2.1 `SessionStore.kt` — replace the memory-only store
Mirror `SessionStore.swift` member-for-member:
- Types `ArchiveState` (three dirty flags) and `StoreState`; properties `archiveState`, `currentEpoch: LockIsolated<ULong>`, `storeState` (one `LockIsolated<StoreState>` replacing the three separate maps), the three `@Persistent` archives (`persistedConversationArchive: Set<Conversation>?`, `persistedMessageArchive`, `persistedUserArchive` via Phase 1.1), `sessionStoreDidChange`.
- `init`: load each archive with the iOS filters (conversations: `!isEmpty && !isMock && hash/key not blank`; messages: `filteringSystemMessages`; users: `!id.isBlank`), the three "Loaded N … into memory." logs under `.conversationStore`/`.messageStore`/`.userStore`, then `sweepOrphanedMessages()`.
- Conversation methods: `clearConversationArchive()`, `getConversation(id:)`, `getConversation(idKey:)`, `removeConversation(idKey:)` (orphaned-message removal, persists both archives, emits messages change then conversations change, non-reportable log with `ConversationIDKey` userInfo), `upsertConversation(_:)` (`didChange` = typing-status inequality **or** `encodedHash` inequality; `shouldPersist` = full inequality; the "Added conversation to persisted archive." log with key+hash userInfo), `upsertConversations(_:)`.
- Message methods: `clearMessageArchive()`, `removeMessages(ids:)`, `upsertMessages(_:)` (filters system messages).
- User methods: `clearUserArchive()`, `removeUser(id:)` (port even though iOS marks it `NIT: Unused`), `upsertUser(_:)`, `upsertUsers(_:)`.
- `advanceEpoch()`, `flushNow()` (drain dirty flags, persist synchronously, `nil` when empty, single "Flushed dirty archives synchronously." log under `.sessionStore`).
- Private: `TaskID` enum, `cappedMessageSnapshot` (`messageArchiveCapPerConversation = 500` in `AppConstants+SessionStore.kt` as `SessionStoreFloats.MESSAGE_ARCHIVE_CAP_PER_CONVERSATION`; newest by `sentDate` per conversation; only IDs referenced by stored conversations), `emitChange`, `persistConversationArchive/persistMessageArchive/persistUserArchive` (mark dirty, capture epoch, `Task.debounced(250 ms)` guarded by epoch, clear dirty, persist; then `scheduleDeadlineFlush()`), `scheduleDeadlineFlush()` (`Task.debounced(1 s)` → `flushNow()`), `sweepOrphanedMessages()`, and the private `Conversation.isTypingStatusEqual(to:)`.
- Delete Android's `clear()`; sign-out and reset call the three `clear*Archive()` methods as iOS does.
- `SessionStoreChange`: add `Kind` and the `kind` property.

### 2.2 `OutboxEntry.kt`
Replace the flat `text`/`mediaRelativePath` shape with the iOS one: `Payload` sealed class (`Audio(inputFileName)`, `Media(fileName, fileExtension)`, `Text(value)`), `State` (`failed`/`sending` raw values), `autoRetryCap = 3` companion, properties in iOS order (`conversationIDKey`, `createdDate`, `fromAccountID`, `id`, `isPenPalsConversation`, `payload`, `recipientUserIDs`, `attemptCount`, `lastAttemptDate`, `reservedRemoteID`, `state`, `transcription`). The JSON codec writes the new shape and reads the legacy shape once (text → `Text`, `mediaRelativePath` → `Media`), so existing installs keep their queued entries. Keep the Android `ID_PREFIX = "outbox-"` (iOS builds the ID the same way in `MessageDeliveryService`).

### 2.3 `MessageOutboxService.kt` + `MessageOutboxService+Retry.kt`
- Add the payload directory (`FileStore` `outbox/`): `storePayloadFile(from:)` (`"<UUID>_<lastPathComponent>"`, copies the thumbnail sibling when present via a `File.thumbnailPath` extension mirroring `URL.thumbnailPath`), `payloadFileURL(forFileName:)`, private `garbageCollectPayloadFiles()` at load (references audio input names, media names + thumbnail siblings), `removePayloadFile(for:)` on `remove`/`removeAll`.
- `entries` becomes `LockIsolated<Map<String, OutboxEntry>>`; keep `allEntries`, `entries(forConversationIDKey:)`, `entry(forID:)`, `claimForRetry(id:candidateRemoteID:)`, `enqueue`, `markFailed`, `remove`, `removeAll` with the iOS log lines under `.outbox`.
- `MessageDeliveryService.sendMediaMessage` stages through `storePayloadFile(from: mediaFile.localPathURL)` and enqueues `Payload.Media(fileName, fileExtension)` exactly like iOS (the staged copy in `media/` is no longer the outbox's source of truth).
- `retry(entryID:)`: claim → missing conversation removes → resolve recipients (store, else `userService.getUser(id:)`, logging failures) → none marks failed → sending UI on (`toggleSendingUI(on: true, clearInputTextViewText: false)` on the Android input-bar service; add the method if absent) and `deliveryProgressIndicator?.startAnimatingDeliveryProgress()` when the entry's conversation is current → `sendPayload(_:presetID:transcription:toUsers:inConversation:)` switching on payload (`Audio` → throw "Failed to reconstruct AudioFile from payload." as iOS does when the file is missing; sending is cut so no entry is ever enqueued, but decode/retry must tolerate one) → remove/markFailed with the iOS logs → sending UI off + stop indicator. `retryAllEligible()` unchanged.
- `OutboxEntry.asDisplayMessage` (`content/user/extensions/OutboxEntry+UserContentExtensions.kt`): three branches per iOS, media branch reading from `outbox/<fileName>` and `mediaFile.encodedHash.shortened`.

### 2.4 `ClientSession.kt`, `EntitySession.kt`, `SyncSession.kt`
- `modules/session/ClientSession.kt`: `entity: EntitySession`, `outbox: MessageOutboxService`, `store: SessionStore`, `sync: SyncSession` (Phase 3 fills `conversationSync`), private `_deliveryProgressIndicator: LockIsolated<DeliveryProgressIndicator?>`, `deliveryProgressIndicator`, `registerDeliveryProgressIndicator(_:)`, `resolveAndSetLanguageCode()` (reads `users/<currentUserID>/languageCode` with `.adaptive`, logs "Setting language code to …" under `.clientSession`, sets `RuntimeStorage.languageCode`). `ClientSessionDependency` + `DependencyValues.clientSession`.
- Move `deliveryProgressIndicator`/`registerDeliveryProgressIndicator` off `MessageSessionService` onto `ClientSession`; update `DeliveryProgressIndicatorService`, `MessageSessionService`, `MessageDeliveryService`, and the retry to read it there.
- `EntitySession` holds `activity`, `conversation`, `message`, `moderation`, `reaction`, `user` (the Kotlin `object`s). `DeliveryProgressIndicator` protocol keeps the two iOS members; `stopAnimatingDeliveryProgress` stays on the concrete `DeliveryProgressIndicatorService`.

### 2.5 Tests (JVM)
Archive load filters; orphan sweep; 500-cap snapshot ordering; debounce + deadline flush with a test dispatcher; epoch invalidation of in-flight persists; `removeConversation` orphan removal and emitted changes; `upsertConversation` `didChange` vs `shouldPersist` matrix (typing-only change persists but does not emit); outbox payload garbage collection; legacy outbox JSON migration.

**AC:** kill-and-relaunch on the emulator shows the conversations list from the archives before any network read; `flushNow()` runs on background; `test detekt` green. Check in.

---

## 6. Phase 3 — `RemotelyUpdatable` Emulation and the Sync Sub-module

*Goal: the write primitives every Entity service uses (`update(_:to:)`, `update(_:applyingRaw:)`, `updateValues(with:)`, `willWrite`/`didWrite`) and the iOS conversation synchronizer.*

Read first: the `networking` package's `RemotelyUpdatable` macro sources (`update(_:to:)`, `update(_:applyingRaw:)`, `WriteAction`), `Conversation+RemotelyUpdatable.swift`, `User+RemotelyUpdatable.swift`, `Message+RemotelyUpdatable.swift`, `Conversation.swift` (`resolveMessages`, `resolveUsers`, `updateLastModifiedDate`, `updateReadDate`), `ConversationSyncService.swift`, `ConversationSyncData.swift`, `SynchronizationRecord.swift`, `SyncSession.swift`, `ConversationObserverService.swift`.

### 3.1 `RemotelyUpdatable` emulation (`networking/{conversation,user,message}/remotelyupdatable/`)
Kotlin has no key paths or macros; emulate the generated API with a sealed `UpdatableKey` per type whose members are the iOS `SerializableKey` names, and keep the iOS method names:
- `Conversation.update(key, to)`, `Conversation.update(key, applyingRaw: (Any?) -> Any?)` (a `database.runTransaction` on `conversations/<key>/<serializableKey>` whose result is decoded back through `modifyKey`), `Conversation.updateValues(with: Map<UpdatableKey, Any>)`, `modifyKey(_:withValue:)` (including the `.messages` branch that upserts messages and strips system messages), `willWrite(_:forKey:updating:)` (the `.messages` atomic fan-out currently inlined in `ConversationSessionService.addMessages`/`buildMessageFanOut` moves here verbatim, including the `PendingTranslationArchive` drain and `SelfWriteRegistry.record`), `didWrite(_:forKey:)` (hash + participant-token fan-out unless `.messages`; store upsert), private `buildParticipantUpdates(for:)`, `updateIDHash(_:)`. `WriteAction` sealed class: `Proceed`, `Handled(updated)`, `Encoded(value)`.
- `User.update(key, to)` with `willWrite` (blocked-user/push-token map diffs via `merge(updates:with:at:)`, date encoding) and `didWrite` (store upsert). `UserMutationService`'s map-diff writes are replaced by these; its scalar writers (`updateLanguageForCurrentUser`, `setMessageRecipientConsentRequiredForCurrentUser`, `setAIEnhancedTranslationsEnabledForCurrentUser`, `clearConversationIDsForCurrentUser`) become `currentUser.update(.languageCode, …)` etc. at their call sites; delete `UserMutationService` when empty (push-token members move to Phase 5.6's `PushTokenService`).
- `Message.update(key, to)` with `didWrite` store upsert.
- Retire `Conversation.commitFieldUpdates` in favor of `updateValues(with:)`.
- Add the `Exception.Networking` catalog helpers iOS uses here (`decodingFailed(data:)`, `typeMismatch(key:type:)`, `notRemotelyUpdatable(key:)`) with iOS-identical descriptors so error codes match.

### 3.2 `Conversation` resolution and read receipts
`Conversation.resolveMessages(ids:)` / `resolveUsers(forceUpdate:)` with the per-type `KeyedCoalescer`s keyed `"<id.encoded>/<sorted ids or all>"` and `"<id.encoded>/<forceUpdate>"`, `submitUnlessCancelled` semantics; `fetchAndCommitMessages` reconciles unfetchable IDs (`removeMessages` + upsert stripped conversation); `fetchAndCommitUsers` throws "Mismatched ratio returned." Rename `updateReadDate(messages)` → `updateReadDate(for:)` with the "No messages provided." throw and the "Updated read date for N message(s)." log; add `updateLastModifiedDate(to:)`.

### 3.3 `ConversationSyncService.kt` (+ `ConversationSyncService+Synchronization.kt` if needed)
Port verbatim: static `coalescer: KeyedCoalescer<String, Conversation>`, `recentlyFailedSyncRecords: LockIsolated<Set<SynchronizationRecord>>`, `_syncData`; `synchronizeConversation(_:)` (coalesced; cooldown guard returns the input unchanged with the non-reportable log; failure records `attempt + 1`); private `synchronizeActivities`, `synchronizeData`, `synchronizeHash` (server hash from the current user's `conversationIDs`), `synchronizeMessages(_:lastTenOnly:)` (newest ten by sorted push-ID order; server map is the source of truth for IDs), `synchronizeMetadata`, `synchronizeParticipants` (sorted by `userID`), `synchronizeReactionMetadata`, `getConversationData` (`.disregardCache`), `resolveConversation(_:)` (upserts conversation and messages), `_synchronizeConversation(_:hasResolvedMessages:)` with every branch in iOS order: not-participating → data only; `messages == nil` → `resolveMessages()` then reconcile from the store and recurse once (throw after the second pass); `filteredMessageIDs` with the fallback to `conversation.messageIDs`; message update path; hash comparison; last-ten reload; full reload with a failure record.
`ConversationSyncData` (filters system messages on init), `SynchronizationRecord` (cooldowns `[3, 15, 60]` s, equality/hash by key), `SyncSession.conversationSync` (a fresh instance per access, as iOS).

### 3.4 `ConversationObserverService.kt`
Align with iOS: logs carry `ConversationIDKey` userInfo; the non-decodable snapshot log uses `AlertType.toastInPrerelease` (add the case if missing); `handleSnapshot` backfills participant users with the iOS log; the retry path logs "Retrying conversation observation after stream termination." with userInfo.

### 3.5 Tests
`SynchronizationRecord` cooldown ladder; every `_synchronizeConversation` branch against a fake `DatabaseDelegate` registered through `Networking.config.registerDatabaseDelegate`; `applyingRaw` transaction round-trip for `reactionMetadata` (strip sentinel → remove own → add → empty sentinel); `updateValues` fan-out path set; `User.update(.pushTokens)` map diff.

**AC:** a conversation whose hash token changes on the user node is synchronized (not refetched) and lands in the store; `test detekt` green. Check in.

---
## 7. Phase 4 — Entity Services (1:1)

*Goal: `Modules/Session/Entity` ported clause-for-clause, including the moderation UI flow that iOS keeps inside the session service.*

Read first: `EntitySession.swift`, `ModerationType.swift`, `ReactionSessionServiceEffectID.swift`, `DeliveryProgressIndicatorProtocol.swift`, `Array+EntitySessionExtensions.swift`, `User+EntitySessionExtensions.swift`, `AppConstants+ConversationSessionService.swift`, `AppConstants+MessageSessionService.swift`, all six `*SessionService.swift`, `ChatPageStateService.swift`, `ReadReceiptService.swift`, `User+UserContentExtensions.swift`, `SessionStore+UserContentExtensions.swift`, `Conversation+UserContentExtensions.swift`, `Array+UserContentExtensions.swift`, and the Contacts trio (`Contact.swift`, `ContactPair.swift`, `NumberPair.swift`, `ContactService.swift`, `ContactPairArchiveService.swift`, `ContactNameService.swift`).

### 4.0 Contacts model prerequisite (`common/models`, `common/services/contacts`)
`ModerationSessionService`, `User.displayName`, and the RecipientBar all consume iOS's `ContactPair`. Port `Contact`, `ContactPair` (`contact`, `users`, `userIDs`, `.withUser(_:name:)`), `NumberPair`, `ContactPairArchiveService` (persisted archive keyed by phone number, `getValue(phoneNumber:)`, `clearArchive()`, `updatedContactPairArchive` event, `lastContactSyncDate`), and the `ContactService` API surface (`syncIfNeeded()`, `sync()`, `clearCache()`, `contactPairArchive`) over the existing `ContactsContract` reader. `ContactMatch` is retired in favor of `ContactPair`; the persisted archive migrates once. Keep `deviceContactLookupUri`/`nameForNumberHash` (Android-only, already ledgered). Port `User.contactPair`, `User.displayName` (with `UserDisplayNameCache`, PenPals branch cut) and `[User].uniquedByID`, `[ContactPair].uniquedByPhoneNumber/userIDs/users`, `[Conversation].filteredAndSorted`, `[Message].hydrated/offsetFromCurrentUserAdditionDate` (consent branch cut), `SessionStore.ignoredConversationIDKeys`, and `Conversation.isVisibleForCurrentUser` including the **blocked-participant rule** (Android currently ignores blocked users here).

### 4.1 `UserSessionService.kt` (+ `UserSessionService+Observation.kt`)
- Types `TaskID`, `UpdateState { idle, running, runningWithPending }`; static `conversationCoalescer/messageCoalescer/userCoalescer: SingleSlotCoalescer<Unit>`; `observationTask`, `updateState`; `currentUserID` via `Persistent(.userSessionService(.currentUserID))`; `currentUser`.
- `User.DataType` (`conversations`, `messages`, `users`) nested per `User+EntitySessionExtensions.swift`, plus `Set<User.DataType>.allDataTypes`.
- `resolveCurrentUser(and:)`: resolve user, then each requested data type through its coalescer with `lastCallerWins`.
- `startObservingCurrentUserChanges()`: on each snapshot, `blockedUserIDsDidChange || conversationsDidChange` → `updateCurrentUser()`; else `deviceIDDidChange` → `signOutToPreserveSingleActiveUser()`; else the "Skipping current user update…" log. `stopObservingCurrentUserChanges()`.
- Private, verbatim: `blockedUserIDsDidChange`, `commitConversationsToMemory`, `conversationsDidChange` (removals via `store.removeConversation`, known-version skip with the userInfo map, the "Detected N unrecognized conversation version(s)…" log), `deviceIDDidChange`, `isKnownVersion`, `resolveCurrentUser()`, `resolveCurrentUserConversations()` (the three-way triage; `ignoredConversationIDKeys`; early return when nothing to do and the sets already match; parallel `conversationSync.synchronizeConversation`; fetch + user-record hash reconciliation; the four-line count log), `resolveMessagesOnCurrentUserConversations()` (`conversationsNeedingMessages` filter), `resolveUsersOnCurrentUserConversations()`, `signOutToPreserveSingleActiveUser()` (info banner "You have been signed out." / "A sign-in was detected from another device." translating all keys, then `Application.reset(onCompletion = NAVIGATE_TO_SPLASH)`), `updateCurrentUser()` (idle/running/pending loop; the data-usage debounce is cut).
- `User.currentUserID` = session value ?? persisted (iOS), not persisted only.

### 4.2 `ConversationSessionService.kt`
- `CurrentConversationReference { draft, none, stored }`, `displayedMessages`, `currentConversationReference`, `messageOffset`, event subscriptions in `init` (outbox → `updateDisplayedMessages`; store changes of kind conversations/messages → `handleStoreChange`), `currentConversation`, private `hydratedMessages`.
- `addMessages(_:to:)` → `conversation.update(.messages, to: appended)` (fan-out now lives in `willWrite`).
- `setCurrentConversation(_:)`: draft vs stored; start observing **only on the draft→stored transition** as iOS; the chat page's appear/disappear own the normal start/stop (mirror the call sites you find in `ChatPageViewService.swift`/`ChatPageReducer.swift` in `ChatPageReducer.kt`). Keep the Android-only `persistOpenConversationIDKey` (already ledgered).
- `incrementMessageOffset()`, **`incrementMessageOffset(to:)`** (new), `resetMessageOffset()`, `updateDisplayedMessages()`, `deleteConversation(_:forced:)` in the iOS sequence (`removeConversationFromUsers` → `messageService.deleteMessages(ids:in:updateConversationHash: false)` → `setValue(null)` → clear pointer if current; the previous single fan-out is retired and ledgered as such), `clearPointer()`, `handleStoreChange(_:)` (removed current conversation → navigate `UserContent(Stack([]))` and show the info banner "This conversation is no longer available." translating all keys, gated on `RuntimeStorage.shouldNotifyOfConversationAvailability`, else remove the flag; delete the Android-only `currentConversationDidBecomeUnavailable` event and its `ChatPageReducer` consumer), `hideConversation(_:forUser:)`, `withMessagesOffset(_:)`.
- Move `markCurrentConversationAsRead` to `content/user/services/ReadReceiptService.kt` as `updateReadDateForUnreadMessages()`, which also calls `notification.setBadgeNumber(currentUser.calculateBadgeNumber())`.
- Constants: `AppConstants+ConversationSessionService.kt` (`DEFAULT_MESSAGE_OFFSET = 20`, `MESSAGE_OFFSET_INCREMENT = 10`).

### 4.3 `MessageSessionService.kt`
- `AppConstants+MessageSessionService.kt` carries all eight iOS floats (add `LANGUAGE_RECOGNITION_SERVICE_MATCH_CONFIDENCE_THRESHOLD`, `READ_TO_FILE_…`, `TRANSLATION_…`, `UPDATE_VALUE_…`); the private `MATCH_CONFIDENCE_THRESHOLD` goes away.
- Signatures: `sendTextMessage(text, presetID = null, toUsers, inConversation: ConversationTuple)`, `sendMediaMessage(mediaFile, presetID = null, toUsers, inConversation)`; `sendAudioMessage` stays cut (record once). `ConversationTuple(value: Conversation?, isPenPalsConversation: Boolean)`.
- Port the developer-mode auto-translate branch behind a new `Build.isDeveloperModeEnabled` (false until Phase 9.4), `resolveSourceLanguageCode`, `recordPendingArchiveEntry`, `shouldAnimateDeliveryProgress`, `incrementDeliveryProgress` (via `clientSession.deliveryProgressIndicator`), and `createMessageAndAddToConversation` including the nested `addMessage`/`notifyUsers` (the blocked-user filter lives **here**, as on iOS, not in the notification service) and the `.createNewConversation` analytics event. `getEnhancementConfiguration`/`messageReadout` are cut (AI).
- `MessageService.buildMessage(fromAccountID:presetID:inputUploadTask:outputUploadTasks:richContent:translations:)` replaces `buildTextMessage`/`buildMediaMessage`; the media upload moves into it exactly where iOS performs it (read `MessageService.swift` lines 64–182).

### 4.4 `ReactionSessionService.kt`
Port `isReactingToMessage` with `didSetIsReactingToMessage` (drives `ContextMenuInteraction.setCanBegin` — map to the existing Android long-press gating flag), the effect registry (`addEffectUponIsReactingToMessage(changedTo:id:_:)`, `drainEffects`, `runEffects`, `ReactionSessionServiceEffectID.reloadCollectionView/scrollToLastItem`), `react(_:to:)` (guards, already-applied → dismiss menu + `removeReaction`, background `notifyUsers(ofReaction:to:)` through Phase 5.7's `NotificationService.notify(_:ofReaction:…)`, dismiss menu, `updateConversation`), `removeReaction(from:)`, `updateConversation(_:messageData:newReaction:)` via `conversation.update(.reactionMetadata, applyingRaw:)` with the raw-map algorithm verbatim, then `resolveMessages(ids: [messageID])`, and — when `chatPageState.isPresented` and the conversation is current — `updateDisplayedMessages()`, the Compose equivalent of `reloadItemsWhenSafe` (bump the row's change token), and `audioMessagePlayback.updateDurationLabelIfNeeded(forMessage:)` for audio. Port `ChatPageStateService` (+ `ChatPageStateServiceEffectID`) 1:1 into `content/user/services` and set `isPresented` from the chat page's appear/disappear. This retires DEVIATIONS "reaction write atomicity" and "`isReactingToMessage` omitted".

### 4.5 `ModerationSessionService.kt`
Now app-level, port the whole flow: `blockUsers(inConversation:)`, `reportUsers(inConversation:)`, `unblockUsers()`; private `alertData(_:contactPairs:)` (per-contact actions + "<Type> All Users" destructive action; translation option keys), `blockUsers(ids:)` / `unblockUsers(ids:)` via `currentUser.update(.blockedUserIDs, to:)` with the bang-qualified-empty rule (`unblock` also sends `traitCollectionChanged`), `confirmModeration(_:title:)` (`ConfirmationAlert` with `destructivePreferred`, translating confirm/message/title; `⌘name⌘` emphasis — see Phase 7 for AlertKit's ⌘ rendering), `getBlockedUsers()` (`.adaptive` read of the map then `userService.getUsers(ids:)`), `moderate(_:dataSource:)` (`ContactService.syncIfNeeded()` first; PenPals branch cut; sort by full name, unique; single-pair shortcut; `ActionSheetAlert(title: "<Type> Users", …)`), `performModeration`, `reportUsers(ids:)` transaction. `ModerationType` moves to `session/entity/models` with both confirmation messages. Repoint `ModerationSessionService+UserContentExtensions.kt`, `SettingsPageReducer.unblockUsersEffect`, and the conversation-cell/ChatInfo callers at these methods and delete the duplicated Android flows.

### 4.6 `ActivitySessionService.kt`
`addToConversation(_:conversation:)` (returns `Unit`; builds consent/PenPals metadata as iOS still does at the data level; `updateValues(with:)`; private `addUserToConversation(userID:conversationID:)`), `removeFromConversation(_:conversation:removeFromUser:)` (`copyWith(name: nil…, nilImageData:, nilRequiresConsentFromInitiator:)` semantics; `conversationService.removeConversationFromUsers`). Move the Android-only `updateMetadata` to `content/user/services/ChatInfoPageViewService+ChangeMetadata.kt`.

### 4.7 Remaining Entity files
`EntitySession.kt` (final shape), `[ReactionMetadata].filteringCurrentUserReactions(to:)`, `Persistent(.userSessionService(...))` convenience, `DataUsageCalculation` (cut; note).

### 4.8 Tests
Triage matrix of `resolveCurrentUserConversations` (exact match / key match + self-write / key match + observed / key match stale → sync / missing → fetch / ignored); `conversationsDidChange` known-version skip and removal; `updateCurrentUser` pending-coalescing; reaction `applyingRaw` algorithm; `withMessagesOffset` window (`amountToGet + 1`); `hideConversation` fan-out paths.

**AC:** two devices reacting to the same message concurrently never clobber each other; a `deviceID` change on the user node signs the device out with the banner; blocking a participant hides the conversation; `test detekt` green. Check in.

---
## 8. Phase 5 — Common Services the Splash Depends On

*Goal: every service `SplashPageViewService` and `SplashPageReducer` reach exists on Android with the iOS name, contract, and behavior.*

Read first: `Application.swift`, `CommonServices.swift`, `CommonServicesDependency.swift`, `NetworkServices+CommonNetworkingExtensions.swift`, `RemoteCacheService.swift`, `RemoteCacheStatus.swift`, `MetadataService.swift`, `UpdateService.swift`, `ReviewService.swift`, `PushTokenService.swift`, `NotificationService.swift`, `UICacheInvalidationService.swift`, `ErrorReportingService.swift`, `ConnectionStatusService.swift` + `ConnectionStatusServiceEffectID.swift`, `NetworkActivityIndicatorService.swift`, `DatabaseDelegate+CommonNetworkingExtensions.swift`, `UserService.swift`, `ConversationService.swift`, `MessageService.swift`, and in the `networking` package every `NetworkHealth*` source plus `StorageMetadata`.

### 5.1 Network health (`:networking`, framework parity)
Port the package's health module: the estimator and its tiers (including `.poor`), `networking.health.health.tier`, the `SharedStates.networkHealth` stream, `NetworkHealthProbeConfiguration(url:)` and `Networking.config.setNetworkHealthConfiguration(_:)` (probe URL `https://www.apple.com`, as iOS; ledger if you change it), sample feeding from database/storage operation timings as the package does, and the `Networking.health` logger domain. Make `CacheStrategy.ADAPTIVE` resolve to `RETURN_CACHE_FIRST` under poor health and `RETURN_CACHE_ON_FAILURE` otherwise (remove the "deferred" note).

### 5.2 `RemoteCacheService` + `RemoteCacheStatus`
`cacheStatus(userID:)` (`.adaptive` read of `invalidatedCaches`), `setCacheStatus(_:userID:)` (transaction; `.invalid` appends, `.valid` removes, unique). Expose through a `CommonServices` container (`services.remoteCache`) — see 5.13.

### 5.3 `MetadataService` (full)
All seven iOS keys (`appShareLink`, `appStoreBuildNumber`, `geminiApiKey` decoded but unused, `isPrevaricationModeEnabled`, `redirectionKey`, `shouldForceUpdate`, `storageReferenceURL`) plus the three Android keys from D-II-4 (`playStoreBuildNumber`, `shouldForceUpdateAndroid`, `playStoreShareLink`), each a `Persistent(.metadataService(...))` value; `SingleSlotCoalescer`; `canRevalidate` over every key; `resolveValues()` coalesced and once-per-session. iOS throws when any hosted value is missing; for the three Android keys only, absence leaves the value unresolved (ledger `II-5.x`, to be retired once Grant confirms the keys exist in dev/staging/prod). `MetadataService.shared` accessor mirrors iOS.

### 5.4 `UpdateService` (re-enabled)
Read the Android keys: `installButtonRedirectURL` = `playStoreShareLink`; `checkForUpdates` compares `playStoreBuildNumber` and `shouldForceUpdateAndroid`; `startObservingForcedUpdateChanges` reads the same keys from the `shared` snapshot. Port the calendar-day logic with `comparator` (start-of-day) dates, `presentUpdateCTA` through `Alert(...).present(translating = [.actions([updateAction]), .message, .title])` with the iOS message (store name "Google Play" instead of "App Store"; ledger the wording), the postponement counters, and `triggerForcedUpdateModal`. `ForcedUpdateView` opens `installButtonRedirectURL`. Remove the commented-out splash lines (the splash now calls the service in Phase 6). Set `Build.appStoreBuildNumber` from the Play-released build number when Grant supplies it (progress-file question).

### 5.5 `ReviewService`
`incrementAppOpenCount()`, `canPromptToReview` (10 / 50 / every 100 opens, once per build), `lastRequestedReviewForBuildNumber` seeding, `promptToReview()` through Play In-App Review (`com.google.android.play:review-ktx`, pinned in the catalog); call it wherever iOS calls `services.review.promptToReview()` (grep the iOS repo). Settings "Leave Review" keeps the store deep link guarded on a resolved `playStoreShareLink` (the `appShareLink` analog).

### 5.6 `PushTokenService`
Replace the push-token half of `UserMutationService`: `currentToken`, `setCurrentToken(_:)`, `eraseStalePushToken(_:)`, `updatePushTokensForCurrentUser()` (throws the two non-reportable exceptions iOS throws, including "Push tokens already up to date."), `prunePushTokensForCurrentUser()`; writes go through `currentUser.update(.pushTokens, to:)`. Add `User.removeCurrentPushToken()`.

### 5.7 `NotificationService` (rename from `NotificationSessionService`)
`setBadgeNumber(_:updateHostedValue:)` (hosted transaction via `updateHostedBadgeNumber`; on Android the launcher badge is `Notification.Builder.setNumber` on posted notifications — keep the existing ledger entry), `notify(_:ofReaction:message:conversationIDKey:isPenPalsConversation:)` with both branches (reaction title `"<title> <Localized(.reacted, languageCode:)> <emoji>"`, `reactionMessageID`/`reactionSuffix` data fields, `notRegisteredForPushNotifications` swallow), `respondToInAppNotification` semantics mapped onto `onMessageReceived` (read the iOS body for the on-screen suppression rules), `notificationBody(for:user:)` with the localized per-type labels now reachable (add a `languageCode` parameter to the Android `localized()` resolver if it lacks one) — this retires the "notification body labels" deviation, `generateAccessToken`, `sendNotification` payload verbatim, `penPalsName` cut. The blocked-user filter leaves this service (it moved to `MessageSessionService` in 4.3). `PantherMessagingService` reads titles/subtitles from the persisted maps of 5.8.

### 5.8 `UICacheInvalidationService`
Port into `content/user/services`: `startObserving()`, `refreshNotificationExtensionNameMap()`, the debounced handlers (`conversationInvalidation` 250 ms → `ConversationCellViewDataCache.removeValues(forConversationIDKey:)`; `messageInvalidation` → clear `.conversationCellViewData` + `.readReceipt`; `userInvalidation` → clear `.conversationCellViewData` + `UserDisplayNameCache.removeValues(forUserIDs:)`; `notificationExtensionNameMap` 500 ms → `persistValuesForNotificationExtension()`), and the two caches (`ConversationCellViewDataCache`, `ReadReceiptCache`) if `ConversationCellViewData`/read-receipt display does not already cache. The app-group defaults become `Persistent` keys with the iOS `NotificationExtensionConstants` names; `PantherMessagingService` resolves the group-name subtitle from that map (cold-start capable), partially retiring the R6.1 subtitle deviation.

### 5.9 `ErrorReportingService` + storage metadata
Extend `StorageDelegate` with `upload(bytes, metadata: StorageMetadata(filePath, contentType, customValues))` mapped onto Firebase `StorageMetadata` (content type + custom metadata) and use it from `MediaMessageService` uploads (content type per extension) and the audio download side where iOS sets it. Port `ErrorReportingService` verbatim: `reportedErrorCodes`, `fileReport(_:)`/`fileReport(_:showsToastOnSuccess:)`, the path recipe (`reports/<bundleVersion>/<parentDirectoryName>/<yyMMdd>_<milestone><build><revision>_<shortDateHash>.txt` with `HostedOverrideErrorCode`, `Descriptor` → `shorthandErrorDescriptor`), the userInfo filter, `Logger.reportsErrorsAutomatically` gating, the success toast (`Localized(.errorReportedSuccessfully)`, persistent in developer mode with the `storageReferenceURL` tap). Add `AlertKit.ReportDelegate` (`fileReport(_:)`) + `AlertKitConfig.registerReportDelegate`, make `ErrorAlert`'s send-report route through it, and make `ExceptionMetadataService.isReportable` read `reportDelegate as? ErrorReportingService`. Retires the "metadata as text header" and "content type omitted" deviations.

### 5.10 `AnalyticsService`
Verify against `AnalyticsService.swift`: event list, `shouldEnableDataCollection`, `logEvent(_:additionalUserInfo:)`, `terminateApp`/`openApp` call sites (1.7). Fix drift only.

### 5.11 Connection status and network activity
`ConnectionStatusServiceEffectID` model (`.retryMessageOutbox`, and the IDs `PantherApplication` registers) replaces string IDs; `isAwaitingConnectionRestoration` semantics; `NetworkActivityIndicatorService` (forwards to the default delegate and sends `networkActivityOccurred`) registered in `Application.initialize`.

### 5.12 Database delegate extensions
`DatabaseDelegate+CommonNetworkingExtensions.kt`: `clearTemporaryCaches()`, `populateTemporaryCaches()` (bulk-read `conversations` and `users`, 300 s samples keyed `<env>/<path>/<key>`, prerelease toast "Established database snapshot.", `RuntimeStorage.populatedTemporaryCaches`), `withGlobalCacheStrategy(_:perform:)`.

### 5.13 `CommonServices` container and `NetworkServices` accessors
`CommonServices` (`services.accountDeletion`, `.analytics`, `.connectionStatus`, `.contact`, `.invite`, `.metadata`, `.networkActivityIndicator`, `.notification`, `.phoneNumber`, `.propertyLists`, `.pushToken`, `.regionDetail`, `.remoteCache`, `.review`, `.update`; cut members omitted and listed in the progress file) with `DependencyValues.commonServices`; `NetworkServices` accessors `networking.conversationService/messageService/userService` (integrity/schema-migration accessors omitted per D-II-2). Ported code reaches services through these paths.

### 5.14 App-level networking services drift
- `UserService`: 500 ms `cachedUserDataSnapshots` (`CacheDomain.userService`), `getUser(id:bypassSnapshotCache:cacheStrategy:)`, `getUsers(...)` **fails the batch on any failure** (Android currently swallows per-user failures), `getAllUsers` decodes from the snapshot, `getUser(phoneNumber:)` non-reportable exception, `clearCache()`.
- `ConversationService`: `getConversation(idKey:)` reads with `.disregardCache` (Android uses the cache-first default — a real freshness drift), `getConversations(idKeys:)` fails the batch on any failure, `removeConversationFromUsers(userIDs:conversationIDKey:failureStrategy:)`, `createConversation` metadata via `ConversationMetadata.empty(userIDs:isPenPalsConversation:)` exactly as iOS (drop the consent-derived fields unless iOS's `empty` sets them).
- `MessageService`: `getMessages(ids:)` failure semantics, `deleteMessage(id:in:updateConversationHash:)` (audio + media component deletion, compiled exception, fan-out), `deleteMessages(ids:in:updateConversationHash:failureStrategy:)`.
- `DeviceID`: `current` per 0.3.

### 5.15 `AccountDeletionService`
Move to the iOS shape: progress alert (`ProgressAlert` with `Localized(.deletingData)`/`.pleaseWait`), overlay, parallel deleted-users + conversation resolution, group leave / 1:1 forced delete with progress, `currentUser.update(.conversationIDs, to: [])`, the two `repairDatabase` calls **absent** (D-II-2; keep the ledger note), persisted ID clear, user node removal, `compiledException`.

**AC:** forced-update prompt appears when `playStoreBuildNumber` exceeds the build (dev environment), a reportable error alert's "Send Error Report" uploads a `text/plain` object with custom metadata, `CacheStrategy.ADAPTIVE` observably switches under a throttled emulator network; `test detekt` green. Check in.

---
## 9. Phase 6 — Splash (1:1)

*Goal: `SplashPageViewService`, `SplashPageReducer`, and `SplashPageView` replace `SplashView`, clause-for-clause.*

Read first: `SplashPageViewService.swift`, `SplashPageReducer.swift`, `SplashPageView.swift`, `AppConstants+SplashPageView.swift`, `SplashPageViewServiceDependency.swift`, `RootView.swift`, and the `AKErrorAlert.TranslationOptionKey` source in `alert-kit`.

### 6.1 Files
- `modules/content/shared/services/SplashPageViewService.kt` (+ `SplashPageViewService+Initialization.kt` if the file-length rule requires; iOS disables `file_length`/`type_body_length`/`function_body_length` here — cite that when suppressing `LongMethod`/`LargeClass`).
- `modules/content/shared/views/splashpageview/SplashPageReducer.kt`, `SplashPageView.kt`.
- `modules/content/shared/constants/AppConstants+SplashPageView.kt`: `SplashPageViewFloats` (`ACTIVITY_INDICATOR_SCALE_EFFECT = 0.8f`, `IMAGE_FRAME_HEIGHT = 70`, `IMAGE_FRAME_WIDTH = 150`, `PADDING = 5`, `PROGRESS_BAR_FADE_IN_DELAY_MILLISECONDS = 1750`, `PROGRESS_BAR_HORIZONTAL_PADDING = 130`, `PROGRESS_BAR_TOP_PADDING = 10`), `SplashPageViewColors` (`imageDarkForeground = 0xF8F8F8`, `progressBarTint = titleText`), `SplashPageViewStrings` (`GIF_IMAGE_NAME = "animated_logotype"`).
- `modules/content/shared/dependencies/SplashPageViewServiceDependency.kt` (`DependencyValues.splashPageViewService`, a single main-thread instance).
- Delete `SplashView.kt` and `AppConstants+SplashView.kt`; `RootView` renders `SplashPageView(ViewModel(SplashPageReducer.State(), SplashPageReducer()))`.

### 6.2 `SplashPageViewService`
- `LoadingIndicatorStyle { BAR, HIDDEN, SPINNER }`; observable `initializationProgress: Float` (StateFlow; when it reaches 1, a 2 s `Task.delayed` resets it to 0 unless a new initialization began) and `loadingIndicatorStyle`; `deferredResolutionRetryInterval = 3 s`, `maximumDeferredResolutionAttempts = 15`; `didAttemptDatabaseRepair`, `didSurpassQuickLoadTimeoutDuration`, `initializationStartDate`, `networkHealth` (`SharedState`).
- `initializeBundle(fromRetry:)` — port every step in iOS order; the table maps each iOS line to Android:

| iOS step | Android |
|---|---|
| `Toast.hide()` | `Toast.hide()` |
| non-retry reset of timeout flag, progress, start date, style; 2.5 s quick-load timeout task (`progress <= 0.6` → flag); 1 s style task (`progress < 1` → `resolveLoadingIndicatorStyle()`) | same, as child coroutines of the initialization job so cancellation propagates |
| `registerReportDelegate(ErrorReportingService())`, `registerTranslationDelegate(networking.hostedTranslation)` | `AlertKitConfig.registerReportDelegate(ErrorReportingService)`, `registerTranslationDelegate(AlertKitTranslationService)` (move the call here from `PantherApplication`) |
| `BreadcrumbsCaptureService.shared.setCaptureGranularity(.narrow)` | absent — `BreadcrumbsCaptureService` is deferred (ledger `II-6.x`; developer diagnostics, no user-visible behavior) |
| `uiCacheInvalidationService.startObserving()` | same |
| offline guard (`build.isOnline`) | add `Build.isOnline` (reads `ConnectionStatusService.isOnline`); no persisted user → non-reportable log and return; else progress = 1 and set language |
| `setIsEnhancedDialogTranslationEnabled`, `setEnhancedTranslationStatusVerbosity` | cut (AI) |
| `Logger.setReportsErrorsAutomatically(!isSimulator && milestone == generalRelease)` | same with `Build.isEmulator` (add) |
| `services.review.incrementAppOpenCount()` | same |
| anonymous sign-in (`try?`) | `runCatching { networking.auth.signInAnonymously() }` (add to `AuthDelegate` if absent) |
| parallel trio (`resolveCurrentUser()`, `resolveAndSetLanguageCode()`, `resolveCacheStatus(userID:)`) + background `metadata.resolveValues()` | `coroutineScope { async … }` in the same shape; background task on the app scope |
| await language code when `currentUserID != nil` (rethrow), `progress += 0.02` | same |
| `incrementRelaunchCountIfNeeded()`, `promptToUpdateIfNeeded()` (throws), `startObservingForcedUpdateChanges()`, `progress += 0.01` | same, against the Android keys |
| cache setup: `.invalid` → `setCacheStatus(.valid)`, `Application.reset(preserveCurrentUserID: true)`, `return initializeBundle(fromRetry: true)`; non-`noValueExists` errors logged | same; add `AppException.Networking.Database.noValueExists` to the catalog with the Android descriptor's code |
| await `resolveCurrentUser`, `progress += 0.2`, guard current user else throw "Failed to resolve current user." | same |
| AI config, `checkPrevaricationMode` | cut (D-II-8) |
| `currentUser.updateDeviceIDIfNeeded()` (before observers start) | same — port `User.updateDeviceIDIfNeeded()` via `update(.deviceID, to:)` |
| detached `ContactService.syncIfNeeded()`; detached `populateTemporaryCaches()` when `conversationIDs.count > 20 && store.conversations.isEmpty` | same on `Dispatchers.Default` |
| `setCurrentConversation(nil)`, `resolveCurrentUser(and: .allDataTypes)`, `progress = 1` | same |
| post-launch maintenance: prune push tokens unless staging; typing reset; badge; PenPals sharing | prune (errors → `AlertType.toastInPrerelease`); typing reset **cut** (write to typing state); `setBadgeNumber(calculateBadgeNumber())`; PenPals cut |
| catch: `.currentUserIDNotSet` → `progress = 1`, return; else rethrow | same |

- `performRetryHandler()`: structure verbatim; the first call sets `didAttemptDatabaseRepair = true` and — per D-II-2 — performs no repair (KDoc and ledger `II-6.x` name `IntegrityService.repairDatabase()` and the `updateRequired` → `isForcedUpdateRequired` branch as the deferred behavior); the second call `Application.reset()` and clears the flag.
- `presentErrorAlert(_:)`: the translation-option decision verbatim (generic / timed-out descriptor comparison via mock exceptions, `hasUserFacingDescriptor`), `ErrorAlert(exception, dismissButtonTitle = Localized(.tryAgain)).present(translating = keys)` — add `ErrorAlert.TranslationOptionKey { errorDescription, sendErrorReportButtonTitle, dismissButtonTitle }` and `present(translating:)` to AlertKit mirroring `AKErrorAlert`.
- `resolveCachedUserIfPoorNetwork()`: guard current user with every conversation's `messages` and `users` present; if tier is not poor, race `networkHealth.changes` (first `.poor`) against a 5 s deadline (`select`), cancelled → `false`; `progress = 0.9`; set language; background `resolveCurrentUserDataWhenNetworkRecovers()`; `true`.
- `resolveCacheStatus(userID:)`, `resolveLoadingIndicatorStyle()` (no user → `SPINNER`; poor health or empty store → `BAR`; else `HIDDEN`), `resolveCurrentUserDataWhenNetworkRecovers()` (15 attempts, 3 s apart, success/exhaustion logs).

### 6.3 `SplashPageReducer`
Actions `ViewAppeared`, `BundleInitializationProgressOccurred`, `ErrorAlertDismissed`, `InitializedBundle(exception)`, `PerformRetryHandlerReturned(exception)`; State `didAttemptAutomaticErrorRecovery`, `exception`; `reduce` verbatim (progress nudge `+0.0005` while `< 0.8`; dismissed alert → direct retry for `failedToGenerateMediaFile`/`timedOut`, else the retry handler; first failure → automatic recovery with the "Attempting automatic error recovery." log; subsequent → `presentErrorAlert` then `ErrorAlertDismissed`; success → `Root(SetModal(UserContent))` when `User.currentUserID != null && userSession.currentUser != null`, else `Onboarding(Stack([]))` + `Root(SetModal(Onboarding))`). `initializeBundleTask(fromRetry:)` = `Effect.run` racing `initializeBundle` against `resolveCachedUserIfPoorNetwork` with `select`; first action wins, the other is cancelled; the cached-user win logs "Loading from cached user; network is poor or initialization stalled." `performRetryHandlerTask` as a static effect. The view subscribes the view model to `networkActivityOccurred` → `BundleInitializationProgressOccurred`. Keep the Android-only `PendingChatNavigation.consume()` after the user-content navigation (already ledgered).

### 6.4 `SplashPageView`
`Application.loadStartDate = now` on appear; `ViewAppeared` on first appear; the animated logotype (bring `animated_logotype` from the iOS bundle; render with `ImageDecoder`/`AnimatedImageDrawable` on API 28+, static wordmark below — ledger) visible only for `BAR`; the `hello` wordmark tinted `imageDarkForeground` in dark mode; the determinate bar (`LinearProgressIndicator`, tint `titleText`, horizontal padding 130, eased animation, fade-in after 1750 ms, opacity by style); the spinner scaled 0.8 with top padding for `SPINNER`; status-bar styling not applicable (note).

### 6.5 Tests and walkthroughs
Reducer tests for every branch (mock the service through `DependencyValues`); service tests for `resolveLoadingIndicatorStyle`, the progress reset, and the deferred-resolution loop with a fake user session. Emulator: cold start online (hidden → spinner or bar per state); offline with archives (loads from cache, progress 1); throttled network (`-netdelay`/`-netspeed`) → cached-user path then deferred resolution succeeding on recovery; `invalidatedCaches` containing the user → reset and retry; network cut mid-init → automatic recovery, then the error alert with Try Again; sign-in → splash → content; sign-up → splash → content.

**AC:** the walkthroughs above pass; every `SplashPageViewService`/`SplashPageReducer` member exists on Android under its iOS name; `test detekt` green. Check in.

---
## 10. Phase 7 — Deviation Ledger Re-validation

*Goal: every row of `DEVIATIONS.md` gets a verdict, and every "Retire"/"Fix" verdict is implemented in this phase unless a later phase is named.*

Verdicts: **Keep** (the constraint stands; copy into `PARITY_II_DEVIATIONS.md` with a fresh ID), **Retire** (fixed by the named phase; list under *Retired*), **Fix here** (small change made in this phase, then retired), **Re-label** (constraint was a preference; keep the behavior, correct the rationale), **→ Phase n** (handled later). Investigate each row against the code before recording the verdict; the pre-assessment below is a starting point, not the answer.

| `DEVIATIONS.md` row | Pre-assessment | Action |
|---|---|---|
| Cross-layer delegates (`LoggerPresentationDelegate`, `ExceptionMetadataDelegate`) | The `:subsystem` seam stands (Kotlin module boundary); the *session-level* seams are gone after Phase 0 | Keep for `:subsystem`; note that session code now calls AlertKit directly |
| `UPPER_SNAKE` enum entries | Kotlin convention | Keep |
| Back gesture / Done checkmark | Platform navigation | Keep |
| Failures surface through Toast via the delegate seam | Session/common services now present `ErrorAlert`/`Toast` directly with the iOS `isReportable`/translation options; the Logger seam stays for `:subsystem` callers | Fix here: sweep the ported services for `Logger.log(exception, with = AlertType.toast)` where iOS presents an alert, and match iOS per call site |
| `ErrorReportingService` metadata as a text header | Firebase `StorageMetadata` supports custom metadata | Retire (5.9) |
| ChatInfo participants resolved synchronously via `ContactService.match` | `ContactPair` archive now exists (4.0) | Re-validate; retire if iOS's async resolution is now mirrored |
| `QuickViewer` → `MediaPreviewOverlay` | No `QLPreviewController` | Keep |
| Shared-media `.missing` placeholder dropped | A placeholder tile is trivial in Compose | Fix here: render the `.missing` placeholder for uncached media; retire |
| Change-name alert live-disables Done on reserved chars | `TextInputAlert` can take a validation hook | Fix here: add `TextInputAlert(isConfirmEnabled: (String) -> Boolean)` honored live by `AlertHost`; retire |
| Rename goes straight to a text-input alert | `ActionSheetAlert` is multi-action now | Fix here: port the "Change name and photo" sheet (photo actions land in Phase 8.6); retire |
| Block/Report/Delete consolidated on ChatInfo | Cell long-press moderation landed post-report | Re-validate against iOS ChatInfo; remove the Android-only card if iOS has none |
| Contextual menu scrim instead of blur | `Modifier.blur` is API 31+ and costly on large trees | Keep (optionally blur on API 31+; ledger either way) |
| Report Mistranslation decoupled from Retry | Retry Translation stays cut | Keep |
| `AttempedPlatforms` omitted from the report payload | Same root cause | Keep |
| Reaction write not atomic | `update(.reactionMetadata, applyingRaw:)` | Retire (4.4) |
| `isReactingToMessage` registry omitted | Ported | Retire (4.4) |
| Media bubbles gestures (already ✅ resolved) | — | Drop from the ledger |
| TTS voice quality | Platform | Keep |
| No audio focus for Speak | `AudioFocusRequest` exists for playback | Fix here: request transient focus before speaking, as iOS activates the session; retire |
| Word-by-word highlight (deferred) | Feasible on API 26+ | → Phase 8.9 |
| LZFSE plain-text documents uncompressed | Pure-Kotlin codec allowed (D-II-7) | → Phase 9.6 |
| Upload content type omitted | Storage metadata channel | Retire (5.9) |
| Thumbnail delete path extension-qualified | Faithful replication | Keep |
| `multipleMessagesReference` full-`messages` scan | `IntegrityServiceSession` deferred (D-II-2) | Keep |
| Streaming content hash | Byte-identical | Keep |
| System pickers vs `PHPicker` | Faithful mapping | Keep |
| Video not transcoded | Media3 Transformer allowed (D-II-7) | → Phase 9.5 |
| Non-PDF document thumbnails absent | No QuickLook | Keep |
| No `UTType` plain-text conformance check | Extension-based classification | Keep |
| JPEG quality loop | Equivalent | Keep |
| Media staged before send | Product decision (D-II-5) | Re-label |
| Attach-media sheet (already ✅ resolved) | — | Drop |
| Binary delivery-progress state | Granular indicator landed post-report (6e55510) | Re-validate; retire if `DeliveryProgressIndicatorService` now matches |
| No manual retry affordance for failed sends | iOS: check how a failed outbox cell is retried (grep `retry(entryID:` call sites in `Content/User`) | → Phase 8.4 |
| Post-move process-death edge | Faithful to iOS | Keep |
| `AudioFile` duration resolved lazily | Consistent model | Keep |
| `MediaPlayer` vs Media3 | Media3 now allowed; playback is single-file voice | Keep unless focus/progress issues surface (progress-file note) |
| Audio focus vs `activateAudioSession` | Mapping | Keep |
| 50 ms progress poll | Idiom | Keep |
| Auto-advance re-resolution | Faithful | Keep |
| `resetVisibleCells` unnecessary | Compose derives state | Keep |
| Reaction Details prepended to the audio menu | Page ported in Phase 8.1 | Retire (8.1) |
| Transcription toggle via reducer state | Compose | Keep |
| Transcription renders through the italic bubble | Same text path | Keep |
| Audio Speak icon asymmetry | Faithful | Keep |
| `MessageTranslationCache` instead of the whole-tree archive snapshot | Package snapshot approach is portable | → Phase 9.7 (retire) |
| Synchronous seeding of resolved content on `MessagesUpdated` | Compose | Keep |
| `ACTION_VIEW`/`ContactDetailSheet` vs `CNContactViewController` | Platform | Keep |
| `PhoneLookup` matching | Platform | Keep |
| Consent gate on cards | Cut | Keep |
| Contact-selector detail not wired | Selector kept (D-II-6) | Fix here: present the contact card from the selector's detail affordance; retire |
| Clear Caches without `Application.reset` | Reset ported (1.9) | Retire (Phase 9.1 rewires the row) |
| Invite Friends without QR | ZXing allowed | → Phase 9.2 |
| Leave Review opens the listing | Play review + `playStoreShareLink` guard | Retire (5.5, 9.1) |
| Send Feedback via mail intent | `AKReportDelegate.sendFeedback/reportBug` are AlertKit-package tooling | Keep, but route through the new `ReportDelegate` (5.9) and copy the iOS "File a Report" sheet shape |
| Blocked Users empty-state alert | Faithful shape | Keep |
| Build-info copy haptic | Platform | Keep |
| Theme / developer-mode / feature-switch / data-usage rows | Themes and features cut; developer-mode rows → Phase 9.4 | Split: Keep (cut rows), → Phase 9.4 (developer mode) |
| Change Language "Apply" instead of "Apply & Exit" | Reset with exit ported | Retire (9.1) |
| ⌘…⌘ emphasis not rendered in alerts | AlertKit can parse the markers | Fix here: render `⌘…⌘` spans bold in `AlertHost` for every alert type (as `AlertKit` does); retire |
| `previousLanguageCodes` superset rule | The message scan is a bounded, local computation | Fix here: port `changeLanguage(to:)` from `ChangeLanguagePageViewService.swift` verbatim; retire |
| `isMainPagePresented` / `traitCollectionChanged` on return | Compose | Keep |
| Change Language placement in the content stack | Settings sub-navigator unrendered | Keep |
| New chat merge (D1) | D-II-6 | Keep |
| Typed text not added as a `.mock` recipient | `InviteService` parity lands in 9.2 | → Phase 9.2 (re-validate) |
| Recipients are single `User`s, not `ContactPair`s | `ContactPair` model lands in 4.0 | Retire after 4.0 (rewire the RecipientBar selection to `ContactPair`) |
| `+` opens the selector directly | `selectContactButtonTapped` flow is small | Fix here: port the permission check → CTA → sync → empty-address-book prompt sequence; retire |
| No `UNNotificationServiceExtension` | Platform | Keep |
| Subtitle only while warm | Persisted name map (5.8) | Retire |
| No numeric launcher badge | Platform | Keep |
| Contact hash matching | Faithful | Keep |
| Partial navigation-stack restoration | Platform | Keep |
| Draft text not persisted | Faithful | Keep |
| Client `sentDate` ordering | Faithful | Keep |
| Notification body labels dropped | Layering fixed | Retire (5.7) |

**AC:** `PARITY_II_DEVIATIONS.md` has a verdict row for every `DEVIATIONS.md` entry; every "Fix here" is implemented and verified; `test detekt` green. Check in.

---
## 11. Phase 8 — Chat and Conversation Gaps

*Goal: close the chat-side gaps `PARITY_REPORT.md` ranked (as re-baselined in 0.1), each ported from its iOS contract.*

1. **`ReactionDetailsPageView`** (`ReactionDetailsPageReducer.swift`, `ReactionDetailsPageView.swift`): page, navigation route, the "Reaction Details" context-menu action for text and media, and the audio-menu prepend in `getAudioMessageActions`.
2. **Reaction push end-to-end**: verify 4.4 + 5.7 deliver a reaction notification an iOS recipient renders correctly (payload fields verbatim).
3. **In-chat search** (`SearchInteractionService.swift` + its `ChatPageViewService` hooks): entry, query, highlight, navigation between matches, `incrementMessageOffset(to:)` to reveal older matches.
4. **Failed-send retry affordance**: mirror exactly how iOS retries a failed outbox entry from the chat page (find the `retry(entryID:)` call sites); port the cell state and tap behavior.
5. **Media Save** (`MediaActionHandlerService` save path → `MediaStore`).
6. **Conversation photo change/remove** (`ChatInfoPageViewService+ChangeMetadata.swift`): camera/library actions on the "Change name and photo" sheet, image compression parameters, `changedGroupPhoto`/`removedGroupPhoto` activities, `imageHash` metadata.
7. **Participant removal + user-info alert** on ChatInfo (`ChatInfoPageViewService.swift`), including the removal confirmation.
8. **`ChatPageViewService+SessionStoreChange.swift`** rules mirrored in `ChatPageReducer`'s store-change handling (what reloads, what scrolls, what is ignored).
9. **Word-by-word speak highlight** via `UtteranceProgressListener.onRangeStart` (API 26+; ledger the API 24–25 fallback), plus the per-cell `speakingMessage` indicator.
10. **Delete-conversations toolbar** (`deleteConversationsToolbarButtonTapped` → `ConversationsPageViewService`), gated on developer mode exactly as iOS (lands enabled with 9.4).

**AC:** each item's iOS contract clauses listed in the progress file as ported/deviated/cut; emulator walkthrough per item; `test detekt` green. Check in.

---

## 12. Phase 9 — Settings, Invite, and Media-Pipeline Gaps

*Goal: the remaining Settings and media items, now unblocked by D-II-3 and D-II-7.*

1. **`SettingsPageViewService`** (`SettingsPageViewService.swift`): move the Settings flows out of the reducer/`SignOutService`/`CacheClearingService`/`LanguageChangeService` into an app-level service with the iOS members: `clearCachesButtonTapped` (confirmation → `Application.reset(preserveCurrentUserID: true)` → "Caches have been cleared…" alert with **Exit** (`exitGracefully`) and, in developer mode, **Reload**), `deleteAccountButtonTapped` (`clearCachesAndExit`), `signOutButtonTapped` (read the iOS body; graceful exit semantics), `blockedUsersButtonTapped` → `moderation.unblockUsers()`, `inviteFriendsButtonTapped`, `leaveReviewButtonTapped` (guarded on `playStoreShareLink`), `sendFeedbackButtonTapped` ("File a Report" sheet routed through the `ReportDelegate`), `setClipboardWithHapticFeedback`, `promptToEnterPrereleaseMode`, `developerModeListItems()`, `fetchCNContactForCurrentUser` → device-contact lookup, `clearCache()`, private `exitGracefully()`. `ChangeLanguagePageViewService.changeLanguage(to:)` ends with `Application.reset(preserveCurrentUserID: true, onCompletion: EXIT_GRACEFULLY)` and the sheet's confirm title becomes "Apply & Exit".
2. **`InviteService`** (`InviteService.swift`, 221 lines) full parity + **`InviteQRCodePageView`** (`InviteQRCodePageReducer/View/ViewStrings.swift`, `InviteQRCodePageViewService.swift`) with ZXing core (pin in the catalog); QR content identical to iOS. Re-validate the RecipientBar `.mock(withName:)` recipient path against the now-ported invite layer.
3. **`ReviewService.promptToReview`** wired at the iOS call sites (5.5).
4. **Developer mode**: `Build.isDeveloperModeEnabled` toggle persisted per iOS (`app-subsystem` `Build`), `promptToEnterPrereleaseMode`, `developerModeListItems` (toggle developer mode, override language code), and the developer-mode gates ported earlier (4.3 auto-translate, 8.10 delete toolbar, 5.9 report toast, 9.1 Reload action). `DevModeActions*` menus stay deferred (progress-file note).
5. **Video transcoding** with Media3 Transformer (pin `androidx.media3:media3-transformer` + `media3-effect`; prove resolution against the toolchain first): match `AVAssetExportPresetMediumQuality` → mp4 (H.264, medium bitrate/resolution ladder; document the chosen parameters in the ledger as the mapping).
6. **LZFSE**: pure-Kotlin port of Apple's BSD-licensed reference implementation (encoder + decoder, all four block types) under `subsystem/modules/foundation/services/lzfse/` with the license header preserved; JVM tests against vectors produced on a Mac with `compression_encode_buffer` (ask Grant to generate them if none exist). Then compress plain-text documents on upload (`application/octet-stream`) and decompress on download exactly as `MediaMessageService.swift` does; retire the ledger entry.
7. **Translation archive snapshot**: port the `networking` package's whole-tree hosted-archive snapshot (read its `HostedTranslationService`/archiver sources) into `:networking`, then delete `MessageTranslationCache` and re-point `resolvedTranslation`/`cachedTranslation`; retire the ledger entry.
8. **Contact selector detail** card presentation (7's "Fix here") and any RecipientBar `ContactPair` rewiring left from 4.0.

**AC:** Settings is row-for-row iOS minus cut rows with iOS flows; invite share + QR work; a plain-text document sent from Android opens on iOS and vice versa; a transcoded video's size and dimensions match an iOS-sent one within the documented parameters; `test detekt` green. Check in.

---

## 13. Phase 10 — Naming Sweep, Final Sweep, Deliverables

1. **Naming sweep** (§2.2): every reducer action, service member, constant, and file name across `app/` and `:networking` is re-checked against its iOS source; restore dropped words (`…ButtonTapped`, `doneToolbarButtonTapped`), rename extension files to `Type+Concern.kt`, replace bare `object` access with `clientSession`/`commonServices`/`networking` paths. Record the few Kotlin-forced exceptions (casing, `Companion`, no key paths) once in the ledger.
2. **Contract sweep of the two focus domains**: re-read every `Modules/Session/**` and `SplashPageView*`/`SplashPageViewService.swift` file against its Kotlin counterpart clause-by-clause; fix drift on the spot; list anything left as a ranked gap.
3. **Wire-format regression**: full parity suite green; add fixtures for surfaces touched (reaction transaction payload, outbox archive, session archives are local-only and need round-trip tests, not iOS hashes).
4. **Deliverables** (both in `~/Documents/ANDROID`, uncommitted): final `PARITY_II_PROGRESS.md` (per-phase status, verification evidence, remaining gaps ranked by user impact, open questions) and final `PARITY_II_DEVIATIONS.md` (every live deviation with a forcing constraint, the *Retired* table, and the *Supersedes* column filled). Leave the original `PARITY_REPORT.md`/`DEVIATIONS.md` untouched.

**AC:** a developer reading `SessionStore.swift` or `SplashPageViewService.swift` finds the Kotlin file recognizable member-by-member; `./gradlew :app:compileDevelopmentDebugKotlin detekt test` green; final check-in.

---

## 14. Order, Sizing, and Escalation

Phase 0 → 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10, one check-in each. Phases 0 and 5 are the largest by file count (mechanical); Phases 2–4 and 6 carry the business-logic risk and deserve the most tests. If a phase item turns out to need a decision not covered by §1, do the independent parts, write the question in the progress file, and raise it at the check-in rather than inventing a policy. Never widen a phase with work from a later one to "save a check-in".
