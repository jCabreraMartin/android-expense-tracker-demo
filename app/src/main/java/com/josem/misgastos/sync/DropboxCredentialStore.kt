package com.josem.misgastos.sync

import android.content.Context
import com.dropbox.core.oauth.DbxCredential

class DropboxCredentialStore(context: Context) {
    private val preferences = context.getSharedPreferences("dropbox_auth", Context.MODE_PRIVATE)

    fun read(): DbxCredential? = runCatching {
        preferences.getString("credential", null)?.let(DbxCredential.Reader::readFully)
    }.getOrNull()

    fun write(credential: DbxCredential) {
        preferences.edit().putString("credential", DbxCredential.Writer.writeToString(credential)).apply()
    }

    fun clear() {
        preferences.edit().remove("credential").apply()
    }
}
