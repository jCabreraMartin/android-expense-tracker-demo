package com.josem.misgastos

import android.app.Application
import com.josem.misgastos.data.GastosDatabase
import com.josem.misgastos.data.GastosRepository
import com.josem.misgastos.sync.DropboxCredentialStore
import com.josem.misgastos.sync.DropboxSyncProvider
import com.josem.misgastos.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GastosApplication : Application() {
    val repository by lazy { GastosRepository(GastosDatabase.get(this).gastosDao()) }
    private val credentialStore by lazy { DropboxCredentialStore(this) }
    val syncManager by lazy {
        SyncManager(this, repository, credentialStore, DropboxSyncProvider(credentialStore)).also {
            repository.onDataChanged = it::markDirty
        }
    }

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            repository.importInitialData(this@GastosApplication)
            syncManager.syncOnOpen()
        }
    }
}
