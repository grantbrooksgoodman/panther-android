//
//  LocalizedStringKey.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 19/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.localization.models

import us.neotechnica.panther.subsystem.modules.localization.interfaces.LocalizedStringKeyRepresentable

/**
 * The app's localization key type.
 *
 * Each entry corresponds to a top-level key in the app's localized
 * strings, exposing its snake-case [referent] – the key used to look
 * the value up. Resolve a value with
 * [localized][us.neotechnica.panther.modules.localization.models.localized].
 */
enum class LocalizedStringKey(
    override val referent: String,
) : LocalizedStringKeyRepresentable {
    AcknowledgeConsent("acknowledge_consent"),
    AddedToConversation("added_to_conversation"),
    AiEnhanced("ai_enhanced"),
    Attachment("attachment"),
    AudioMessage("audio_message"),
    AwaitingConsent("awaiting_consent"),
    Blocked("blocked"),
    BlockUser("block_user"),
    CannotDisplayMessage("cannot_display_message"),
    ChangedGroupPhoto("changed_group_photo"),
    Contacts("contacts"),
    Copy("copy"),
    Delete("delete"),
    DeletingData("deleting_data"),
    Delivered("delivered"),
    Document("document"),
    Enable("enable"),
    ErrorReportedSuccessfully("error_reported_successfully"),
    File("file"),
    FinishingUp("finishing_up"),
    FromUser("from_user"),
    FromYou("from_you"),
    HoldDownToRecord("hold_down_to_record"),
    Image("image"),
    Invite("invite"),
    Language("language"),
    LeftConversation("left_conversation"),
    LoadingData("loading_data"),
    MessageRecipientConsentAcknowledgementMessage("message_recipient_consent_acknowledgement_message"),
    MessageRecipientConsentRequestMessage("message_recipient_consent_request_message"),
    Multiple("multiple"),
    MyAccount("my_account"),
    NewMessage("new_message"),
    NoResults("no_results"),
    NoSpeechDetected("no_speech_detected"),
    NotDelivered("not_delivered"),
    NotNow("not_now"),
    OfflineMode("offline_mode"),
    OriginalInLanguage("original_in_language"),
    People("people"),
    PleaseWait("please_wait"),
    Reacted("reacted"),
    ReactionDetails("reaction_details"),
    Read("read"),
    Region("region"),
    RemovedConversationName("removed_conversation_name"),
    RemovedFromConversation("removed_from_conversation"),
    RemovedGroupPhoto("removed_group_photo"),
    RenamedConversation("renamed_conversation"),
    RepairingData("repairing_data"),
    ReportMistranslation("report_mistranslation"),
    ReportUser("report_user"),
    RequestConsent("request_consent"),
    RetryTranslation("retry_translation"),
    SaveFile("save_file"),
    Search("search"),
    SelectCallingCode("select_calling_code"),
    SelectLanguage("select_language"),
    SettingLanguage("setting_language"),
    SlideToCancel("slide_to_cancel"),
    Someone("someone"),
    Speak("speak"),
    StopSpeaking("stop_speaking"),
    To("to"),
    Today("today"),
    TranslationInLanguage("translation_in_language"),
    Version("version"),
    Video("video"),
    ViewAsAudio("view_as_audio"),
    ViewOriginal("view_original"),
    ViewTranscription("view_transcription"),
    ViewTranslation("view_translation"),
    WelcomeToHello("welcome_to_hello"),
    You("you"),
    Cancel("cancel"),
    Dismiss("dismiss"),
    Done("done"),
    SendFeedback("send_feedback"),
    Settings("settings"),
    TryAgain("try_again"),
    Yesterday("yesterday"),
}
