package com.josem.misgastos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.josem.misgastos.ui.GastosApp
import com.josem.misgastos.ui.theme.MisGastosTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val gastosApplication get() = application as GastosApplication
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(gastosApplication.repository, gastosApplication.syncManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MisGastosTheme { GastosApp(viewModel) } }
    }

    override fun onResume() {
        super.onResume()
        if (gastosApplication.syncManager.completeAuthorization()) {
            lifecycleScope.launch(Dispatchers.IO) { gastosApplication.syncManager.syncNow() }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch(Dispatchers.IO) { gastosApplication.syncManager.syncOnOpen() }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch(Dispatchers.IO) { gastosApplication.syncManager.syncOnBackground() }
    }
}
