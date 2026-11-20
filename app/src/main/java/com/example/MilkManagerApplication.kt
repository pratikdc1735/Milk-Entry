package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.preferences.ShopSettingsRepository
import com.example.data.repository.MilkManagerRepository
import com.example.util.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MilkManagerApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val settingsRepository: ShopSettingsRepository by lazy { ShopSettingsRepository(this) }
    val repository: MilkManagerRepository by lazy { MilkManagerRepository(database) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createNotificationChannel(this)
        appScope.launch {
            repository.ensureDefaultProductsSeeded()
        }
    }
}
