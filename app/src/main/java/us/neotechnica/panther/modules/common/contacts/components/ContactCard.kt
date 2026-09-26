//
//  ContactCard.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 20/09/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.AvatarImageView
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.modules.common.models.PhoneNumber

/**
 * A contact identified for display, standing in for the iOS
 * `CNContactContainer`.
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
 * Returns a presenter that shows a contact card, standing in for the iOS
 * `CNContactView`.
 *
 * When the number matches a saved device contact, the presenter opens the
 * system contact detail through a `ContactsContract` view intent (the
 * iOS `CNContactViewController(for:)`); otherwise it presents an in-app
 * detail sheet offering to add the number to contacts (the iOS
 * `CNContactViewController(forUnknownContact:)`).
 *
 * Call the returned function with a phone number and an optional display
 * name to present the card.
 */
@Composable
fun rememberContactCardPresenter(): (PhoneNumber?, String?) -> Unit {
    var card by remember { mutableStateOf<ContactCardInfo?>(null) }
    val context = LocalContext.current

    card?.let { info ->
        ContactDetailSheet(info = info, onDismiss = { card = null })
    }

    return { phoneNumber, displayName ->
        val lookupUri = phoneNumber?.compiledNumberString?.let { ContactService.deviceContactLookupUri(it) }
        if (lookupUri != null) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, lookupUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        } else {
            card = ContactCardInfo(displayName = displayName, phoneNumber = phoneNumber)
        }
    }
}

// MARK: - Contact Detail Sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactDetailSheet(
    info: ContactCardInfo,
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
                modifier = Modifier.size(AVATAR_SIZE),
                initials = info.initials,
                glyphSize = AVATAR_GLYPH_SIZE,
                initialsFont = Font.systemSemibold(FontScale.Large),
            )
            val title = info.displayName ?: number
            title?.let { Components.Text(it, color = colors.titleText, font = Font.systemSemibold(FontScale.Large)) }
            if (number != null && number != title) {
                Components.Text(number, color = colors.subtitleText, font = Font.system)
            }
            Button(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                onClick = {
                    addToContacts(context, info.displayName, number)
                    onDismiss()
                },
            ) {
                Components.Text(ADD_TO_CONTACTS_TITLE, color = Color.White, font = Font.systemSemibold())
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
private val AVATAR_GLYPH_SIZE = 36.dp
private const val ADD_TO_CONTACTS_TITLE = "Add to Contacts"
