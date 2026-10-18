package com.josem.misgastos.sync

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.DownloadErrorException
import com.dropbox.core.v2.files.WriteMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DropboxSyncProvider(
    private val credentialStore: DropboxCredentialStore,
) : CloudSyncProvider {
    private val client: DbxClientV2?
        get() = credentialStore.read()?.let { credential ->
            DbxClientV2(DbxRequestConfig.newBuilder("MisGastosAndroid/1.0").build(), credential)
        }

    override val isConnected: Boolean get() = credentialStore.read() != null

    override suspend fun download(): String? = withContext(Dispatchers.IO) {
        val dropbox = client ?: return@withContext null
        try {
            ByteArrayOutputStream().use { output ->
                dropbox.files().download(BACKUP_PATH).download(output)
                output.toString(StandardCharsets.UTF_8.name())
            }
        } catch (error: DownloadErrorException) {
            if (error.errorValue.isPath && error.errorValue.pathValue.isNotFound) null else throw error
        }
    }

    override suspend fun upload(content: String) = withContext(Dispatchers.IO) {
        val dropbox = requireNotNull(client) { "Dropbox no está conectado" }
        ByteArrayInputStream(content.toByteArray(StandardCharsets.UTF_8)).use { input ->
            dropbox.files().uploadBuilder(BACKUP_PATH)
                .withMode(WriteMode.OVERWRITE)
                .uploadAndFinish(input)
        }
        Unit
    }

    private companion object {
        const val BACKUP_PATH = "/mis-gastos-backup.json"
    }
}
