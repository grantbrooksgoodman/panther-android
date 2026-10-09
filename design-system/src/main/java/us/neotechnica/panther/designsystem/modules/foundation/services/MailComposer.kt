//
//  MailComposer.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.foundation.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger
import us.neotechnica.panther.translator.Translator
import java.io.File

internal object MailComposer {
    // MARK: - Types

    class AttachmentData(
        val data: ByteArray,
        val fileName: String,
        val mimeType: String,
    )

    // MARK: - Computed Properties

    val canSendMail: Boolean
        get() {
            val activity = Translator.config.currentActivityProvider?.invoke() ?: return false
            return Intent(Intent.ACTION_SENDTO, Uri.parse(MAILTO_URI_STRING))
                .resolveActivity(activity.packageManager) != null
        }

    // MARK: - Methods

    fun compose(
        subject: String,
        body: Pair<String, Boolean>?,
        recipients: List<String>,
        attachments: List<AttachmentData> = emptyList(),
    ) {
        val activity = Translator.config.currentActivityProvider?.invoke() ?: return
        val attachmentURIs = ArrayList(attachments.mapNotNull { attachmentURI(activity, it) })

        val intent =
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = MESSAGE_MIME_TYPE
                putExtra(Intent.EXTRA_EMAIL, recipients.toTypedArray())
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachmentURIs)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                body?.let { (string, isHTML) ->
                    if (isHTML) {
                        putExtra(Intent.EXTRA_HTML_TEXT, string)
                        putExtra(
                            Intent.EXTRA_TEXT,
                            HtmlCompat.fromHtml(string, HtmlCompat.FROM_HTML_MODE_LEGACY),
                        )
                    } else {
                        putExtra(Intent.EXTRA_TEXT, string)
                    }
                }
            }

        // Restrict the message to the apps that can send e-mail.
        val mailIntents =
            mailPackageNames(activity).map { packageName ->
                Intent(intent).setPackage(packageName)
            }

        val composeIntent = mailIntents.firstOrNull() ?: return
        runCatching {
            if (mailIntents.size == 1) {
                activity.startActivity(composeIntent)
            } else {
                activity.startActivity(
                    Intent.createChooser(composeIntent, null).apply {
                        putExtra(Intent.EXTRA_INITIAL_INTENTS, mailIntents.drop(1).toTypedArray())
                    },
                )
            }
        }.onFailure {
            Logger.log(Exception.from(it, ExceptionMetadata(this)))
        }
    }

    // MARK: - Auxiliary

    private fun mailPackageNames(context: Context): List<String> =
        context
            .packageManager
            .queryIntentActivities(
                Intent(Intent.ACTION_SENDTO, Uri.parse(MAILTO_URI_STRING)),
                0,
            ).map { it.activityInfo.packageName }
            .distinct()

    private fun attachmentURI(
        context: Context,
        attachment: AttachmentData,
    ): Uri? =
        runCatching {
            val directory = File(context.cacheDir, ATTACHMENTS_DIRECTORY_NAME).apply { mkdirs() }
            val file = File(directory, attachment.fileName).apply { writeBytes(attachment.data) }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}$FILE_PROVIDER_AUTHORITY_SUFFIX",
                file,
            )
        }.getOrNull()
}

private const val ATTACHMENTS_DIRECTORY_NAME = "reports"
private const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".fileprovider"
private const val MAILTO_URI_STRING = "mailto:"
private const val MESSAGE_MIME_TYPE = "message/rfc822"
