package com.niagara.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.niagara.launcher.core.theme.NiagaraLauncherTheme
import com.niagara.launcher.ui.home.HomeScreen
import com.niagara.launcher.ui.home.HomeViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels {
        val app = application as NiagaraApplication
        HomeViewModel.Factory(app.appRepository, app.preferencesRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NiagaraLauncherTheme {
                HomeScreen(viewModel = viewModel)
            }
        }
    }
}
