//
//  NavigatorStateProtocol.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.subsystem.modules.navigation.interfaces

/**
 * The navigation state of one flow: the full-screen modal it
 * presents, the sheet it presents, and its navigation stack.
 *
 * Declare a `data class` per flow whose nested path types implement
 * [Paths]; a channel the flow never uses declares an empty path type.
 *
 * @param ModalPaths The destinations the flow presents as full-screen
 *   modals.
 * @param SeguePaths The destinations the flow pushes onto its stack.
 * @param SheetPaths The destinations the flow presents as sheets.
 */
interface NavigatorState<ModalPaths : Paths, SeguePaths : Paths, SheetPaths : Paths> {
    /** The presented full-screen modal, or `null` if none is presented. */
    val modal: ModalPaths?

    /** The presented sheet, or `null` if none is presented. */
    val sheet: SheetPaths?

    /** The ordered list of destinations in the navigation stack. */
    val stack: List<SeguePaths>
}

/**
 * A marker for a navigation destination.
 *
 * Each navigator declares nested modal, segue, and sheet path types
 * that implement [Paths].
 */
interface Paths
