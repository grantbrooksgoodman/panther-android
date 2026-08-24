//
//  ContactSelectorPageView.kt
//  Panther
//
//  Created by Grant Brooks Goodman on 23/08/2026.
//  Copyright © 2013-2026 NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.contactselectorpageview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.componentkit.Components
import us.neotechnica.panther.designsystem.modules.componentkit.components.CircleChipButton
import us.neotechnica.panther.designsystem.modules.componentkit.components.SearchBar
import us.neotechnica.panther.designsystem.modules.componentkit.models.Font
import us.neotechnica.panther.designsystem.modules.componentkit.models.FontScale
import us.neotechnica.panther.designsystem.modules.theming.views.LocalPantherColors
import us.neotechnica.panther.modules.common.contacts.models.ContactMatch
import us.neotechnica.panther.modules.common.contacts.services.ContactService
import us.neotechnica.panther.modules.common.extensions.formattedString
import us.neotechnica.panther.modules.common.services.PhoneNumberService
import us.neotechnica.panther.modules.common.services.RegionDetailService
import us.neotechnica.panther.modules.content.user.components.ContactRow
import us.neotechnica.panther.modules.content.user.constants.ContactSelectorPageViewFloats
import us.neotechnica.panther.modules.content.user.constants.ContactSelectorPageViewStrings
import us.neotechnica.panther.networking.modules.common.extensions.digits
import us.neotechnica.panther.networking.modules.schema.common.models.PhoneNumber
import us.neotechnica.panther.networking.modules.user.services.UserService
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.services.Logger

// MARK: - Constants Accessors

private typealias Floats = ContactSelectorPageViewFloats
private typealias Strings = ContactSelectorPageViewStrings

/**
 * A contact picker presented over the new-chat page: search the device's
 * matched contacts, or enter a phone number to look up its registered
 * user. Selecting a result reports it through [onSelect]. Mirrors the iOS
 * `ContactSelectorPageView`.
 *
 * @param onSelect Invoked with the chosen user's id and display name.
 * @param onDismiss Invoked when the picker is dismissed.
 * @param modifier The modifier for this view.
 */
@Composable
fun ContactSelectorPageView(
    onSelect: (userID: String, displayName: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPantherColors.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    val contacts = remember { ContactService.matches() }
    val filtered = filterContacts(contacts, query)
    val queryIsPhoneNumber =
        query.digits.isNotEmpty() &&
            PhoneNumberService.numberIsValidLength(query.digits.length, PhoneNumberService.deviceCallingCode)

    Box(modifier = modifier.fillMaxSize().background(colors.groupedContentBackground)) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            Header(onDismiss = onDismiss)
            SearchBar(
                value = query,
                placeholder = Strings.SEARCH_PLACEHOLDER,
                onValueChange = { query = it },
                modifier = Modifier.padding(horizontal = Floats.searchHorizontalPadding, vertical = Floats.searchVerticalPadding),
                containerColor = colors.background,
            )

            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(filtered, key = { it.userID }) { contact ->
                    ContactRow(
                        name = contact.fullName,
                        initials = contact.initials,
                        onClick = { onSelect(contact.userID, contact.fullName) },
                    )
                    HorizontalDivider(color = colors.groupedContentBackground)
                }

                if (filtered.isEmpty()) {
                    item {
                        NoResultsRow(
                            isPhoneNumber = queryIsPhoneNumber,
                            onFindByPhone = { scope.launch { resolveByPhone(query, onSelect) } },
                        )
                    }
                }
            }
        }
    }
}

// MARK: - Header

@Composable
private fun Header(onDismiss: () -> Unit) {
    val colors = LocalPantherColors.current
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = Floats.headerHorizontalPadding, vertical = Floats.headerVerticalPadding)) {
        Components.Text(
            Strings.TITLE,
            color = colors.titleText,
            font = Font.systemBold(FontScale.Large),
            modifier = Modifier.align(Alignment.Center),
        )
        CircleChipButton(
            systemName = "xmark",
            contentDescription = Strings.CLOSE,
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.CenterEnd),
            tint = colors.titleText,
            glyphSize = Floats.doneButtonGlyphSize,
        )
    }
}

// MARK: - Empty State

@Composable
private fun NoResultsRow(
    isPhoneNumber: Boolean,
    onFindByPhone: () -> Unit,
) {
    val colors = LocalPantherColors.current
    if (isPhoneNumber) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onFindByPhone)
                    .padding(horizontal = Floats.emptyStateHorizontalPadding, vertical = Floats.emptyStateVerticalPadding),
        ) {
            Components.Text(
                Strings.NO_CONTACTS_FOUND,
                color = colors.accent,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Floats.emptyStateHorizontalPadding, vertical = Floats.emptyStateVerticalPadding),
        ) {
            Components.Text(Strings.NO_RESULTS, color = colors.subtitleText, textAlign = TextAlign.Center)
        }
    }
}

// MARK: - Auxiliary

private fun filterContacts(
    contacts: List<ContactMatch>,
    query: String,
): List<ContactMatch> {
    if (query.isBlank()) return contacts
    val normalized = query.trim().lowercase()
    return contacts.filter {
        it.fullName.lowercase().contains(normalized) || it.compiledNumberString.contains(query.digits)
    }
}

private suspend fun resolveByPhone(
    query: String,
    onSelect: (userID: String, displayName: String) -> Unit,
) {
    val regionCode = RegionDetailService.deviceRegionCode
    val phoneNumber =
        PhoneNumber(
            callingCode = RegionDetailService.callingCode(regionCode) ?: PhoneNumberService.deviceCallingCode,
            nationalNumberString = query.digits,
            regionCode = regionCode,
            label = null,
            internalFormattedString = null,
        )
    try {
        if (UserService.accountExists(phoneNumber)) {
            val user = UserService.getUser(phoneNumber)
            val name = ContactService.match(user.id)?.fullName ?: user.phoneNumber.formattedString()
            onSelect(user.id, name)
        }
    } catch (exception: Exception) {
        Logger.log(exception)
    }
}
