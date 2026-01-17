package com.ramble.app

import android.app.Application
import com.ramble.app.auth.AuthManager

class RambleApp : Application() {
    
    lateinit var authManager: AuthManager
        private set
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        authManager = AuthManager(this)
    }
    
    companion object {
        lateinit var instance: RambleApp
            private set
    }
}
