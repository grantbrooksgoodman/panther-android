//
//  ChatInfoPageView+SheetView.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.views.chatinfopageview

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import us.neotechnica.panther.designsystem.modules.foundation.components.PageSheetView
import us.neotechnica.panther.modules.content.user.models.ContentPickerResult
import us.neotechnica.panther.modules.content.user.services.contentpicker.ContentPickerService
import us.neotechnica.panther.modules.content.user.services.contentpicker.ContentPickerSource
import us.neotechnica.panther.modules.content.user.views.contactselectorpageview.ContactSelectorPageView
import us.neotechnica.panther.navigation.ChatNavigatorState
import us.neotechnica.panther.navigation.ChatRoute
import us.neotechnica.panther.navigation.Route
import us.neotechnica.panther.navigation.navigation
import us.neotechnica.panther.subsystem.modules.dependencyinjection.services.DependencyValues
import us.neotechnica.panther.subsystem.modules.foundation.models.Exception
import us.neotechnica.panther.subsystem.modules.foundation.models.ExceptionMetadata
import us.neotechnica.panther.subsystem.modules.reducer.models.ViewModel

// MARK: - Sheet View

// The chat flow's sheet destinations over the page: the contact
// selector as a page sheet, and the camera and photo pickers as the
// system's picker activities. A picker's selection is delivered before
// its dismissal.
@Composable
internal fun SheetView(viewModel: ViewModel<ChatInfoPageReducer.State, ChatInfoPageReducer.Action>) {
    val navigation = remember { DependencyValues.current.navigation }
    val navigatorStates by navigation.state.collectAsState()
    val path = navigatorStates.chat.sheet

    PageSheetView(
        isPresented = path == ChatNavigatorState.SheetPaths.ContactSelector,
        onDismissRequest = { navigation.navigate(Route.Chat(ChatRoute.Sheet(null))) },
        ignoresBottomSafeArea = true,
    ) {
        ContactSelectorPageView(entryPoint = ContactSelectorPageView.EntryPoint.CHAT_INFO_PAGE_VIEW)
    }

    LaunchedEffect(path) {
        when (path) {
            ChatNavigatorState.SheetPaths.CameraPicker ->
                presentContentPicker(
                    ContentPickerService.camera,
                    onSelection = { viewModel.send(ChatInfoPageReducer.Action.SelectedImageChanged(it)) },
                    onDismiss = { viewModel.send(ChatInfoPageReducer.Action.CameraPickerDismissed(it)) },
                )

            ChatNavigatorState.SheetPaths.PhotoPicker ->
                presentContentPicker(
                    ContentPickerService.media,
                    onSelection = { viewModel.send(ChatInfoPageReducer.Action.SelectedImageChanged(it)) },
                    onDismiss = { viewModel.send(ChatInfoPageReducer.Action.PhotoPickerDismissed(it)) },
                )

            ChatNavigatorState.SheetPaths.ContactSelector,
            null,
            -> Unit
        }
    }
}

// MARK: - Auxiliary

// Presents the given picker, delivering a picked image to the selection
// handler followed by `null` to the dismissal handler. A cancellation,
// or a selection that is not an image, delivers only `null` to the
// dismissal handler; a failure delivers its exception instead.
private fun presentContentPicker(
    picker: ContentPickerSource,
    onSelection: (Uri) -> Unit,
    onDismiss: (Exception?) -> Unit,
) {
    picker.onDismiss { result ->
        if (result == null) return@onDismiss onDismiss(null)

        result
            .onSuccess { content ->
                val image = content as? ContentPickerResult.Image ?: return@onSuccess onDismiss(null)
                onSelection(image.uri)
                onDismiss(null)
            }.onFailure { throwable ->
                onDismiss(throwable as? Exception ?: Exception("Failed to get image data.", metadata = ExceptionMetadata(picker)))
            }
    }

    picker.present()
}
