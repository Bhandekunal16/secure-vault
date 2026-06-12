package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.presentation.VaultMainScreen
import com.example.presentation.VaultViewModel
import com.example.presentation.VaultViewModelFactory

class MainActivity : FragmentActivity() {

    // Retrieve our ViewModel with Hilt-free, clean Constructor Injection via Factory
    private val viewModel: VaultViewModel by viewModels {
        VaultViewModelFactory((application as VaultApplication).container.vaultRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // SEC COMPLIANCE: Enable screen capture protection to block external recording/screenshots
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        
        enableEdgeToEdge()
        
        setContent {
            VaultMainScreen(viewModel = viewModel)
        }
    }

    override fun onStop() {
        super.onStop()
        // SEC COMPLIANCE: Lock vault containers immediately when the application moves to background
        viewModel.lockApp()
    }
}
