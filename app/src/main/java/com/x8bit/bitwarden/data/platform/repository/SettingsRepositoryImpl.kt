package com.x8bit.bitwarden.data.platform.repository

import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Primary implementation of [SettingsRepository].
 */
class SettingsRepositoryImpl(
    private val settingsDiskSource: SettingsDiskSource,
    private val dispatcherManager: DispatcherManager,
) : SettingsRepository {
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    override fun getVaultTimeoutStateFlow(userId: String): StateFlow<VaultTimeout> =
        settingsDiskSource
            .getVaultTimeoutInMinutesFlow(userId = userId)
            .map { it.toVaultTimeout() }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = settingsDiskSource
                    .getVaultTimeoutInMinutes(userId = userId)
                    .toVaultTimeout(),
            )

    override fun storeVaultTimeout(userId: String, vaultTimeout: VaultTimeout) {
        settingsDiskSource.storeVaultTimeoutInMinutes(
            userId = userId,
            vaultTimeoutInMinutes = vaultTimeout.vaultTimeoutInMinutes,
        )
    }

    override fun getVaultTimeoutActionStateFlow(
        userId: String,
    ): StateFlow<VaultTimeoutAction> =
        settingsDiskSource
            .getVaultTimeoutActionFlow(userId = userId)
            .map { it.orDefault() }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = settingsDiskSource
                    .getVaultTimeoutAction(userId = userId)
                    .orDefault(),
            )

    override fun isVaultTimeoutActionSet(
        userId: String,
    ): Boolean = settingsDiskSource.getVaultTimeoutAction(userId = userId) != null

    override fun storeVaultTimeoutAction(
        userId: String,
        vaultTimeoutAction: VaultTimeoutAction?,
    ) {
        settingsDiskSource.storeVaultTimeoutAction(
            userId = userId,
            vaultTimeoutAction = vaultTimeoutAction,
        )
    }
}

/**
 * Converts a stored [Int] representing a vault timeout in minutes to a [VaultTimeout].
 */
private fun Int?.toVaultTimeout(): VaultTimeout =
    when (this) {
        VaultTimeout.Immediately.vaultTimeoutInMinutes -> VaultTimeout.Immediately
        VaultTimeout.OneMinute.vaultTimeoutInMinutes -> VaultTimeout.OneMinute
        VaultTimeout.FiveMinutes.vaultTimeoutInMinutes -> VaultTimeout.FiveMinutes
        VaultTimeout.ThirtyMinutes.vaultTimeoutInMinutes -> VaultTimeout.ThirtyMinutes
        VaultTimeout.OneHour.vaultTimeoutInMinutes -> VaultTimeout.OneHour
        VaultTimeout.FourHours.vaultTimeoutInMinutes -> VaultTimeout.FourHours
        VaultTimeout.OnAppRestart.vaultTimeoutInMinutes -> VaultTimeout.OnAppRestart
        null -> VaultTimeout.Never
        else -> VaultTimeout.Custom(vaultTimeoutInMinutes = this)
    }

/**
 * Returns the given [VaultTimeoutAction] or a default value if `null`.
 */
private fun VaultTimeoutAction?.orDefault(): VaultTimeoutAction =
    this ?: VaultTimeoutAction.LOCK