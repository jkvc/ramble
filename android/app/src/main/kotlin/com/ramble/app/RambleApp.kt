package com.ramble.app

import android.app.Application
import com.ramble.app.auth.ApiKeyManager
import com.ramble.app.settings.SettingsManager

class RambleApp : Application() {
    
    lateinit var apiKeyManager: ApiKeyManager
        private set
    
    lateinit var settingsManager: SettingsManager
        private set
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        apiKeyManager = ApiKeyManager(this)
        settingsManager = SettingsManager(this)
    }
    
    companion object {
        lateinit var instance: RambleApp
            private set
    }
}
