//
//  ContactCard.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.common.contacts.components

import android.content.Intent
import android.provider.ContactsContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.modules.common.models.PhoneNumber
import us.neotechnica.panther.modules.content.user.components.AvatarImageView

/**
 * A contact identified for display.
 *
 * @property displayName The contact's name, or `null` when only a number
 *   is known.
 * @property phoneNumber The contact's phone number, or `null`.
 */
data class ContactCardInfo(
    val displayName: String?,
    val phoneNumber: PhoneNumber?,
) {
    /** The contact's initials, derived from [displayName]. */
    val initials: String
        get() {
            val words = displayName?.split(" ")?.filter { it.firstOrNull()?.isLetter() == true }.orEmpty()
            return when {
                words.isEmpty() -> ""
                words.size == 1 -> words[0].take(1).uppercase()
                else -> (words[0].take(1) + words.last().take(1)).uppercase()
            }
        }
}

/**
 * Presents a contact card in response to a tap or a long press on a
 * contact.
 *
 * Obtain an instance with [rememberContactCardPresenter].
 */
class ContactCardPresenter internal constructor(
    /**
     * Presents the card for a tapped contact: opens the saved device
     * contact, or an in-app sheet offering to add an unknown number to
     * contacts – and, when a non-`null` remove action is passed, to
     * remove them from the conversation.
     */
    val onTap: (PhoneNumber?, String?, (() -> Unit)?) -> Unit,
    /**
     * Presents the card for a long-pressed contact: for a saved device
     * contact, an in-app sheet offering only to remove them from the
     * conversation; a no-op for an unknown number.
     */
    val onLongPress: (PhoneNumber?, String?, (() -> Unit)?) -> Unit,
)

/**
 * Returns a [ContactCardPresenter] that shows a contact card.
 *
 * A tap on a saved device contact opens the system contact detail
 * through a `ContactsContract` view intent; a tap on an unknown number
 * presents an in-app detail sheet offering to add it to contacts. A long
 * press inverts this: a saved device contact gets the in-app sheet –
 * offering only removal – while an unknown number does nothing.
 *
 * Pass a non-`null` remove-from-conversation action to add a remove
 * button to the in-app sheet.
 */
@Composable
fun rememberContactCardPresenter(): ContactCardPresenter {
    var card by remember { mutableStateOf<ContactCardInfo?>(null) }
    var showsAddToContacts by remember { mutableStateOf(true) }
    var removeFromConversation by remember { mutableStateOf<(() -> Unit)?>(null) }
    val context = LocalContext.current

    card?.let { info ->
        ContactDetailSheet(
            info = info,
            showsAddToContacts = showsAddToContacts,
            onRemoveFromConversation = removeFromConversation,
            onDismiss = {
                card = null
                removeFromConversation = null
            },
        )
    }

    fun lookupUri(phoneNumber: PhoneNumber?) = phoneNumber?.compiledNumberString?.let { ContactService.deviceContactLookupUri(it) }

    return ContactCardPresenter(
        onTap = { phoneNumber, displayName, onRemoveFromConversation ->
            val uri = lookupUri(phoneNumber)
            if (uri != null) {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            } else {
                card = ContactCardInfo(displayName = displayName, phoneNumber = phoneNumber)
                showsAddToContacts = true
                removeFromConversation = onRemoveFromConversation
            }
        },
        onLongPress = { phoneNumber, displayName, onRemoveFromConversation ->
            if (lookupUri(phoneNumber) != null) {
                card = ContactCardInfo(displayName = displayName, phoneNumber = phoneNumber)
                showsAddToContacts = false
                removeFromConversation = onRemoveFromConversation
            }
            // An unknown number has no saved contact to show: do nothing.
        },
    )
}

// MARK: - Contact Detail Sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactDetailSheet(
    info: ContactCardInfo,
    showsAddToContacts: Boolean,
    onRemoveFromConversation: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val colors = LocalPantherColors.current
    val context = LocalContext.current
    val number = info.phoneNumber?.formattedString()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AvatarImageView(
                null,
                size = DpSize(AVATAR_SIZE, AVATAR_SIZE),
            )
            val title = info.displayName ?: number
            title?.let { Components.Text(it, foregroundColor = colors.titleText, font = Font.systemSemibold(FontScale.Large)) }
            if (number != null && number != title) {
                Components.Text(number, foregroundColor = colors.subtitleText, font = Font.system)
            }
            if (showsAddToContacts) {
                Button(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    onClick = {
                        addToContacts(context, info.displayName, number)
                        onDismiss()
                    },
                ) {
                    Components.Text(ADD_TO_CONTACTS_TITLE, foregroundColor = Color.White, font = Font.systemSemibold())
                }
            }
            onRemoveFromConversation?.let { remove ->
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().padding(top = if (showsAddToContacts) 0.dp else 12.dp),
                    onClick = {
                        onDismiss()
                        remove()
                    },
                ) {
                    Components.Text(REMOVE_FROM_CONVERSATION_TITLE, foregroundColor = Color.White, font = Font.systemSemibold())
                }
            }
        }
    }
}

// MARK: - Auxiliary

private fun addToContacts(
    context: android.content.Context,
    name: String?,
    number: String?,
) {
    val intent =
        Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.Contacts.CONTENT_TYPE
            name?.let { putExtra(ContactsContract.Intents.Insert.NAME, it) }
            number?.let { putExtra(ContactsContract.Intents.Insert.PHONE, it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    runCatching { context.startActivity(intent) }
}

private val AVATAR_SIZE = 72.dp
private const val ADD_TO_CONTACTS_TITLE = "Add to Contacts"
private const val REMOVE_FROM_CONVERSATION_TITLE = "Remove from Conversation"
