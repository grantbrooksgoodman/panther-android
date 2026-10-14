//
//  SFSymbolDrawables.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.designsystem.modules.componentkit.models

import androidx.compose.ui.text.font.FontWeight
import us.neotechnica.panther.designsystem.R

/**
 * The drawable resources of the bundled SF Symbols, in the regular
 * weight and medium scale, keyed by symbol name.
 *
 * [SFSymbol] uses these drawables while [SFSymbol.usesSFSymbols] is
 * `true`. Add an entry for each SF Symbol drawable added to
 * `res/drawable`, and add other weights and scales of a symbol to
 * [SFSymbolVariantDrawables].
 */
internal val SFSymbolDrawables: Map<String, Int> =
    mapOf(
        "arrow.counterclockwise" to R.drawable.sf_symbol_arrow_counterclockwise,
        "arrow.up.circle.fill" to R.drawable.sf_symbol_arrow_up_circle_fill,
        "checkmark" to R.drawable.sf_symbol_checkmark,
        "checkmark.circle.fill" to R.drawable.sf_symbol_checkmark_circle_fill,
        "chevron.backward" to R.drawable.sf_symbol_chevron_backward,
        "chevron.compact.right" to R.drawable.sf_symbol_chevron_compact_right,
        "chevron.down.circle" to R.drawable.sf_symbol_chevron_down_circle,
        "chevron.forward" to R.drawable.sf_symbol_chevron_forward,
        "chevron.right.circle" to R.drawable.sf_symbol_chevron_right_circle,
        "command" to R.drawable.sf_symbol_command,
        "doc.circle.fill" to R.drawable.sf_symbol_doc_circle_fill,
        "doc.on.doc" to R.drawable.sf_symbol_doc_on_doc,
        "exclamationmark.bubble" to R.drawable.sf_symbol_exclamationmark_bubble,
        "exclamationmark.circle" to R.drawable.sf_symbol_exclamationmark_circle,
        "exclamationmark.octagon.fill" to R.drawable.sf_symbol_exclamationmark_octagon_fill,
        "exclamationmark.triangle" to R.drawable.sf_symbol_exclamationmark_triangle,
        "exclamationmark.triangle.fill" to R.drawable.sf_symbol_exclamationmark_triangle_fill,
        "film.fill" to R.drawable.sf_symbol_film_fill,
        "flag" to R.drawable.sf_symbol_flag,
        "flag.fill" to R.drawable.sf_symbol_flag_fill,
        "gearshape" to R.drawable.sf_symbol_gearshape,
        "globe" to R.drawable.sf_symbol_globe,
        "hand.raised.fill" to R.drawable.sf_symbol_hand_raised_fill,
        "info" to R.drawable.sf_symbol_info,
        "info.circle" to R.drawable.sf_symbol_info_circle,
        "info.circle.fill" to R.drawable.sf_symbol_info_circle_fill,
        "location.fill" to R.drawable.sf_symbol_location_fill,
        "magnifyingglass" to R.drawable.sf_symbol_magnifyingglass,
        "person.2.circle.fill" to R.drawable.sf_symbol_person_2_circle_fill,
        "person.crop.circle.fill" to R.drawable.sf_symbol_person_crop_circle_fill,
        "photo.fill" to R.drawable.sf_symbol_photo_fill,
        "play.fill" to R.drawable.sf_symbol_play_fill,
        "plus" to R.drawable.sf_symbol_plus,
        "questionmark" to R.drawable.sf_symbol_questionmark,
        "questionmark.circle.fill" to R.drawable.sf_symbol_questionmark_circle_fill,
        "questionmark.square.dashed" to R.drawable.sf_symbol_questionmark_square_dashed,
        "sparkles.2" to R.drawable.sf_symbol_sparkles_2,
        "speaker.slash.circle" to R.drawable.sf_symbol_speaker_slash_circle,
        "speaker.wave.2.bubble" to R.drawable.sf_symbol_speaker_wave_2_bubble,
        "speaker.wave.2.circle" to R.drawable.sf_symbol_speaker_wave_2_circle,
        "square.and.arrow.down" to R.drawable.sf_symbol_square_and_arrow_down,
        "square.and.pencil" to R.drawable.sf_symbol_square_and_pencil,
        "square.text.square.fill" to R.drawable.sf_symbol_square_text_square_fill,
        "star.fill" to R.drawable.sf_symbol_star_fill,
        "text.bubble" to R.drawable.sf_symbol_text_bubble,
        "trash" to R.drawable.sf_symbol_trash,
        "trash.fill" to R.drawable.sf_symbol_trash_fill,
        "wifi.slash" to R.drawable.sf_symbol_wifi_slash,
        "x.circle.fill" to R.drawable.sf_symbol_x_circle_fill,
        "xmark" to R.drawable.sf_symbol_xmark,
        "xmark.circle.fill" to R.drawable.sf_symbol_xmark_circle_fill,
    )

/**
 * A weight and scale of an SF Symbol other than its regular weight and
 * medium scale.
 *
 * @property systemName The SF Symbol name.
 * @property weight The weight of the symbol.
 * @property scale The scale of the symbol.
 */
internal data class SFSymbolVariant(
    val systemName: String,
    val weight: FontWeight?,
    val scale: SFSymbol.Scale,
)

/**
 * The drawable resources of the bundled SF Symbol variants, keyed by
 * variant.
 *
 * [SFSymbol] uses a variant's drawable in place of the symbol's regular
 * drawable when the variant is requested.
 */
internal val SFSymbolVariantDrawables: Map<SFSymbolVariant, Int> =
    mapOf(
        SFSymbolVariant("chevron.forward", FontWeight.SemiBold, SFSymbol.Scale.MEDIUM) to
            R.drawable.sf_symbol_chevron_forward_semibold,
        SFSymbolVariant("chevron.forward", FontWeight.SemiBold, SFSymbol.Scale.SMALL) to
            R.drawable.sf_symbol_chevron_forward_semibold_small,
    )
