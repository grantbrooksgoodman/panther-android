//
//  DeliveryProgressIndicatorService.kt
//  Panther Android
//
//  Created by Grant Brooks Goodman.
//  Copyright © NEOTechnica Corporation. All rights reserved.
//

package us.neotechnica.panther.modules.content.user.services.chatpageview.sources

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import us.neotechnica.panther.designsystem.modules.foundation.models.SystemAnimation
import us.neotechnica.panther.modules.content.user.constants.ChatPageViewServiceFloats
import us.neotechnica.panther.modules.content.user.services.MessageDeliveryService
import us.neotechnica.panther.modules.session.clientSession
import us.neotechnica.panther.modules.session.entity.interfaces.DeliveryProgressIndicator
import us.neotechnica.panther.modules.session.entity.services.ConversationSessionService
import us.neotechnica.panther.modules.session.state.models.OutboxEntry
import us.neotechnica.panther.modules.session.state.services.MessageOutboxService
import us.neotechnica.panther.subsystem.modules.dependencyinjection.models.Dependency
import kotlin.time.Duration.Companion.seconds

// MARK: - Constants Accessors

private typealias IndicatorFloats = ChatPageViewServiceFloats.DeliveryProgressIndicator

/**
 * The service that manages the message delivery progress bar.
 *
 * Use [DeliveryProgressIndicatorService] to drive the thin progress
 * bar shown while a message is being sent. It reveals the bar after
 * a short delay, advances it toward – but not past – a threshold on
 * a repeating job, and completes and hides it once delivery
 * finishes.
 *
 * **Important:** create and use this service on the main thread. Its
 * animations run on [scope], which must carry a frame clock – supply
 * a scope obtained from the composition.
 *
 * @param scope The scope on which the progress and appearance
 *   animations run.
 */
class DeliveryProgressIndicatorService(
    private val scope: CoroutineScope,
) : DeliveryProgressIndicator {
    // MARK: - Dependencies

    private val conversationSession: ConversationSessionService by Dependency { it.clientSession.entity.conversation }

    // MARK: - Properties

    private val alphaAnimatable = Animatable(0f)
    private val progressAnimatable = Animatable(0f)

    private var appearanceJob: Job? = null
    private var deliveryProgressJob: Job? = null

    // MARK: - Computed Properties

    /** The bar's current opacity, from `0` (hidden) to `1` (shown). */
    val alpha: Float get() = alphaAnimatable.value

    /** The bar's current progress, from `0` to `1`. */
    val progress: Float get() = progressAnimatable.value

    // Covers outbox retries, which send without engaging
    // MessageDeliveryService.
    private val isSendingOutboxEntry: Boolean
        get() {
            val conversationIDKey =
                conversationSession.currentConversation?.id?.key ?: return false
            return MessageOutboxService
                .entries(conversationIDKey)
                .any { it.state == OutboxEntry.State.SENDING }
        }

    // MARK: - DeliveryProgressIndicator Conformance

    override fun incrementDeliveryProgress(by: Float) {
        scope.launch {
            progressAnimatable.animateTo(
                targetValue = progressAnimatable.targetValue + by,
                animationSpec = animationSpec(),
            )
        }
    }

    override fun startAnimatingDeliveryProgress() {
        instantiateDeliveryProgressJob(IndicatorFloats.HIDDEN_TIMER_TIME_INTERVAL)
        instantiateAppearanceJob()
    }

    override fun stopAnimatingDeliveryProgress() {
        deliveryProgressJob?.cancel()
        deliveryProgressJob = null

        scope.launch {
            progressAnimatable.animateTo(1f, animationSpec())
        }
        scope.launch {
            delay((IndicatorFloats.ANIMATION_DELAY * MILLIS_PER_SECOND).toLong())
            alphaAnimatable.animateTo(0f, animationSpec())
            progressAnimatable.snapTo(0f)
        }
    }

    // MARK: - Object Lifecycle

    /**
     * Cancels the bar's animation jobs.
     *
     * Call this method when the hosting page leaves the screen.
     * Calls made afterward are harmless.
     */
    fun teardown() {
        appearanceJob?.cancel()
        appearanceJob = null

        deliveryProgressJob?.cancel()
        deliveryProgressJob = null
    }

    // MARK: - Auxiliary

    // An ease-in-out curve over the indicator's animation duration.
    private fun animationSpec(): FiniteAnimationSpec<Float> =
        SystemAnimation.easeInOut(IndicatorFloats.ANIMATION_DURATION.toDouble().seconds)

    private fun instantiateAppearanceJob() {
        appearanceJob?.cancel()
        appearanceJob =
            scope.launch {
                delay((IndicatorFloats.APPEARANCE_TIMER_TIME_INTERVAL * MILLIS_PER_SECOND).toLong())
                if (!(MessageDeliveryService.isSendingMessage.value || isSendingOutboxEntry)) return@launch
                alphaAnimatable.animateTo(1f, animationSpec())
                instantiateDeliveryProgressJob(IndicatorFloats.VISIBLE_TIMER_TIME_INTERVAL)
            }
    }

    private fun instantiateDeliveryProgressJob(timeInterval: Float) {
        deliveryProgressJob?.cancel()
        deliveryProgressJob =
            scope.launch {
                val increment = IndicatorFloats.TIMER_PROGRESS_INCREMENT
                val threshold = IndicatorFloats.TIMER_PROGRESS_INCREMENT_THRESHOLD
                while (isActive) {
                    delay((timeInterval * MILLIS_PER_SECOND).toLong())
                    if (progressAnimatable.targetValue + increment < threshold) {
                        incrementDeliveryProgress(increment)
                    }
                }
            }
    }

    // MARK: - Companion

    companion object {
        private const val MILLIS_PER_SECOND = 1000f
    }
}
