package com.josem.misgastos.sync

interface CloudSyncProvider {
    val isConnected: Boolean
    suspend fun download(): String?
    suspend fun upload(content: String)
}
