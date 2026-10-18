package com.josem.misgastos.sync

import android.content.Context
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.josem.misgastos.BuildConfig
import com.josem.misgastos.data.GastosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SyncStatus { DISCONNECTED, IDLE, SYNCING, SUCCESS, ERROR }

class SyncManager(
    private val context: Context,
    private val repository: GastosRepository,
    private val credentialStore: DropboxCredentialStore,
    private val provider: CloudSyncProvider,
) {
    private var awaitingAuthorization = false
    private val preferences = context.getSharedPreferences("cloud_sync", Context.MODE_PRIVATE)
    private val _status = MutableStateFlow(if (provider.isConnected) SyncStatus.IDLE else SyncStatus.DISCONNECTED)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    val isConnected: Boolean get() = provider.isConnected

    fun connect(activityContext: Context) {
        if (BuildConfig.DROPBOX_APP_KEY.isBlank()) return
        Auth.startOAuth2PKCE(
            activityContext,
            BuildConfig.DROPBOX_APP_KEY,
            DbxRequestConfig.newBuilder("MisGastosAndroid/1.0").build(),
            listOf("files.content.read", "files.content.write"),
        )
        awaitingAuthorization = true
    }

    fun completeAuthorization(): Boolean {
        if (!awaitingAuthorization) return false
        awaitingAuthorization = false
        val credential = Auth.getDbxCredential() ?: return false
        credentialStore.write(credential)
        _status.value = SyncStatus.IDLE
        markDirty()
        return true
    }

    fun markDirty() {
        preferences.edit().putBoolean("dirty", true).apply()
    }

    suspend fun syncOnOpen() = sync(downloadWhenClean = true)

    suspend fun syncOnBackground() {
        if (preferences.getBoolean("dirty", false)) sync(downloadWhenClean = false)
    }

    suspend fun syncNow() = sync(downloadWhenClean = true)

    private suspend fun sync(downloadWhenClean: Boolean) {
        if (!provider.isConnected || _status.value == SyncStatus.SYNCING) return
        _status.value = SyncStatus.SYNCING
        runCatching {
            val dirty = preferences.getBoolean("dirty", false)
            if (dirty || !downloadWhenClean) {
                provider.upload(repository.createBackup())
            } else {
                val remote = provider.download()
                if (remote == null) provider.upload(repository.createBackup())
                else repository.restoreBackup(remote, markAsChanged = false)
            }
            preferences.edit().putBoolean("dirty", false).apply()
        }.onSuccess {
            _status.value = SyncStatus.SUCCESS
        }.onFailure {
            _status.value = SyncStatus.ERROR
        }
    }
}
