package com.example

import android.app.Application
import android.content.Context
import com.example.database.AppDatabase
import com.example.repository.VaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VaultApplication : Application() {

    // Dependency injection container
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        
        container = AppContainer(this)
        
        // Asynchronously prefill data on initial run
        CoroutineScope(SupervisorJob()).launch {
            container.vaultRepository.prefillInitialDataIfEmpty()
        }
    }
}

class AppContainer(private val context: Context) {
    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    val vaultRepository: VaultRepository by lazy {
        VaultRepository(
            context = context,
            noteDao = database.noteDao(),
            passwordDao = database.passwordDao(),
            vaultFileDao = database.vaultFileDao(),
            intruderLogDao = database.intruderLogDao(),
            settingDao = database.settingDao()
        )
    }
}
