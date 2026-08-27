package com.josem.misgastos

import android.app.Application
import com.josem.misgastos.data.GastosDatabase
import com.josem.misgastos.data.GastosRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GastosApplication : Application() {
    val repository by lazy { GastosRepository(GastosDatabase.get(this).gastosDao()) }

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            repository.importInitialData(this@GastosApplication)
        }
    }
}
