# Phase 8 — Endpoint / Write Parity Checklist

Every backend mutation the iOS app performs, mapped to its Android status.
Status legend: **✅ Ported** (wire-equivalent), **🟡 Simplified** (ported with a
documented deviation), **⏸ Deferred** (post-MVP), **N/A** (out of MVP scope).

Verify the ✅/🟡 rows by RTDB-export diffing after scripted parallel iOS/Android
runs (creating/mutating equivalent records and comparing the exported subtrees).

## Conversations

| Write | RTDB effect | Android | Notes |
|---|---|---|---|
| Create conversation | `conversations/<key>` node + `users/*/openConversations/<key>` + first `messages/<id>` + pending translations, atomic | ✅ | `ConversationService.createConversation`; hash via P1 primitive; metadata `empty()` (name `!`, epoch `lastModified`, consent/penpals prepopulation) matches iOS |
| Append message | message node + index + participant un-delete + typing reset + hash + `lastModified` + tokens + pending translations, atomic | ✅ | `ConversationSessionService.addMessages` (P7) |
| Hide conversation (1:1, others not all deleted) | `participants/<uid>/hasDeletedConversation=true` + hash + tokens | ✅ | `ConversationSessionService.deleteConversation` (unforced) |
| Delete conversation (forced / all deleted) | null `conversations/<key>` + `messages/<id>` + `users/*/openConversations/<key>` | ✅ | `ConversationSessionService.deleteConversation` (forced) |
| Rename conversation | `metadata.name` + activity + hash + tokens | ✅ | `ActivitySessionService.renameConversation` |
| Add participant | `participants` + `activities` + consent/penpals metadata + hash + tokens + new user token | ✅ | `ActivitySessionService.addToConversation` |
| Remove participant / leave | `participants` − user + `activities` + metadata cleanup + hash + tokens (+ user token null) | ✅ | `ActivitySessionService.removeFromConversation` |
| Read receipts | `messages/<id>/readReceipts` (array) + (1:1) hash + `lastModified` + tokens | ✅ | `Conversation.updateReadDate` (P7) |
| Typing indicator | `participants/<uid>/isTyping` | ⏸ | Deferred (P7 decision) |
| Reaction authoring | `reactionMetadata` + message reactions | ⏸ | Post-MVP |
| Group photo | `metadata.imageData/imageHash` | ⏸ | Post-MVP (media) |

## Users

| Write | RTDB effect | Android | Notes |
|---|---|---|---|
| Create user | `users/<id>` (+ `badgeNumber:0`) | ✅ | `UserService.createUser` (P5) |
| Block / unblock | `users/<uid>/blockedUserIDs/<id>` = true/null (incremental map diff) | ✅ | `ModerationSessionService` + `UserMutationService.setBlockedUserIDsForCurrentUser` |
| Report user | `reportedUsers/<id>` count += 1 (transaction) | ✅ | `ModerationSessionService.reportUsers` |
| Add / remove push token | `users/<uid>/pushTokens/<token>` = true/null (incremental) | ✅ | `UserMutationService.updatePushTokensForCurrentUser` / `prune` / `eraseStale` |
| Change language | `users/<uid>/{languageCode, previousLanguageCodes}`, atomic | 🟡 | `LanguageChangeService`; always records outgoing language (iOS scans messages first) |
| Consent required toggle | `users/<uid>/messageRecipientConsentRequired` | ✅ | `UserMutationService.setMessageRecipientConsentRequiredForCurrentUser` |
| AI-enhanced translations toggle | `users/<uid>/aiEnhancedTranslationsEnabled` | ✅ | `UserMutationService.setAIEnhancedTranslationsEnabledForCurrentUser` |
| PenPals participant toggle | `users/<uid>/isPenPalsParticipant` | ⏸ | PenPals deferred |
| Badge number | `users/<uid>/badgeNumber` transaction (recipient) | 🟡 | Recipient-side increment done in push send; local badge display approximated |
| Delete account | `deletedUsers` append + leave/delete conversations + clear `openConversations` + null `users/<uid>` | 🟡 | `AccountDeletionService`; skips `IntegrityService.repairDatabase` (decision) |
| Consent acknowledgement message | `metadata.messageRecipientConsentAcknowledgementData` RMW | ⏸ | Consent enforcement deferred; consent messages render as plain text (P7) |

## Push Notifications

| Concern | Android | Notes |
|---|---|---|
| Token lifecycle | ✅ | `PantherMessagingService.onNewToken` + boot fetch → `UserMutationService` |
| Send push (FCM v1) | ✅ | `NotificationSessionService`: OAuth token via Cloud Function, `apns`+`android`+`data`+`notification` payload, atomic badge, stale-token erase |
| Receive (foreground) | ✅ | `onMessageReceived` → channel notification, suppressed for on-screen conversation |
| Receive (backgrounded) + tap → conversation | ✅ | System notification (payload `data`); tap → `MainActivity` extras → `PendingChatNavigation` → splash pushes Chat |
| NotificationExtension (contact-name enrichment) | 🟡 | iOS enriches title via app-group archive; Android enriches in-service (foreground) |

## Contacts

| Concern | Android | Notes |
|---|---|---|
| Contact ↔ user matching | 🟡 | `ContactService` (ContactsContract) → `ContactMatch` archive; simpler than iOS `ContactPair` (no photos/number-pairs) |
| Conversation titles use contact names | ✅ | `ConversationCellViewData` consults the archive (fixes the P6 phone-only titles) |

## MVP-cut (N/A this phase)

New-chat invites/QR, PenPals, audio/media messages, reactions authoring, data-usage,
feedback/review/update/remote-cache/breadcrumbs, schema migration / integrity / rollback,
dev-mode & staging conversation seeding, full settings polish (Phase 9).
