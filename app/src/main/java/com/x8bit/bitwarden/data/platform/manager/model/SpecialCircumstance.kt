package com.x8bit.bitwarden.data.platform.manager.model

import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager

/**
 * Represents a special circumstance the app may be in. These circumstances could require some kind
 * of navigation that is counter to what otherwise may happen based on the state of the app.
 */
sealed class SpecialCircumstance {
    /**
     * The app was launched in order to create/share a new Send using the given [data].
     */
    data class ShareNewSend(
        val data: IntentManager.ShareData,
        val shouldFinishWhenComplete: Boolean,
    ) : SpecialCircumstance()
}