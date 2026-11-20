package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.shopSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "snehal_shop_settings"
)

data class ShopSettings(
    val shopName: String = "Snehal Super Market",
    val shopAddress: String = "Main Market Road, Pune, Maharashtra",
    val shopMobile: String = "",
    val shopWhatsApp: String = "",
    val shopLogoUri: String = "",
    val currencySymbol: String = "₹",
    val defaultMilkPrice: Double = 30.0,
    val defaultBillingMonth: Int = DateUtils.currentMonth(),
    val defaultBillingYear: Int = DateUtils.currentYear(),
    val languageCode: String = "en", // "en" or "mr"
    val dailyEntryReminderEnabled: Boolean = false,
    val dailyReminderHour: Int = 9,
    val dailyReminderMinute: Int = 0,
    val monthlyBillReminderEnabled: Boolean = false,
    val pendingPaymentReminderEnabled: Boolean = false
)

class ShopSettingsRepository(private val context: Context) {

    private object Keys {
        val SHOP_NAME = stringPreferencesKey("shop_name")
        val SHOP_ADDRESS = stringPreferencesKey("shop_address")
        val SHOP_MOBILE = stringPreferencesKey("shop_mobile")
        val SHOP_WHATSAPP = stringPreferencesKey("shop_whatsapp")
        val SHOP_LOGO_URI = stringPreferencesKey("shop_logo_uri")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val DEFAULT_MILK_PRICE = doublePreferencesKey("default_milk_price")
        val DEFAULT_BILLING_MONTH = intPreferencesKey("default_billing_month")
        val DEFAULT_BILLING_YEAR = intPreferencesKey("default_billing_year")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_REMINDER_HOUR = intPreferencesKey("daily_reminder_hour")
        val DAILY_REMINDER_MINUTE = intPreferencesKey("daily_reminder_minute")
        val MONTHLY_BILL_REMINDER = booleanPreferencesKey("monthly_bill_reminder")
        val PENDING_PAYMENT_REMINDER = booleanPreferencesKey("pending_payment_reminder")
    }

    val settingsFlow: Flow<ShopSettings> = context.shopSettingsDataStore.data.map { prefs ->
        ShopSettings(
            shopName = prefs[Keys.SHOP_NAME] ?: "Snehal Super Market",
            shopAddress = prefs[Keys.SHOP_ADDRESS] ?: "Main Market Road, Pune, Maharashtra",
            shopMobile = prefs[Keys.SHOP_MOBILE] ?: "",
            shopWhatsApp = prefs[Keys.SHOP_WHATSAPP] ?: "",
            shopLogoUri = prefs[Keys.SHOP_LOGO_URI] ?: "",
            currencySymbol = prefs[Keys.CURRENCY_SYMBOL] ?: "₹",
            defaultMilkPrice = prefs[Keys.DEFAULT_MILK_PRICE] ?: 30.0,
            defaultBillingMonth = prefs[Keys.DEFAULT_BILLING_MONTH] ?: DateUtils.currentMonth(),
            defaultBillingYear = prefs[Keys.DEFAULT_BILLING_YEAR] ?: DateUtils.currentYear(),
            languageCode = prefs[Keys.LANGUAGE_CODE] ?: "en",
            dailyEntryReminderEnabled = prefs[Keys.DAILY_REMINDER_ENABLED] ?: false,
            dailyReminderHour = prefs[Keys.DAILY_REMINDER_HOUR] ?: 9,
            dailyReminderMinute = prefs[Keys.DAILY_REMINDER_MINUTE] ?: 0,
            monthlyBillReminderEnabled = prefs[Keys.MONTHLY_BILL_REMINDER] ?: false,
            pendingPaymentReminderEnabled = prefs[Keys.PENDING_PAYMENT_REMINDER] ?: false
        )
    }

    suspend fun getSnapshot(): ShopSettings = settingsFlow.first()

    suspend fun updateSettings(settings: ShopSettings) {
        context.shopSettingsDataStore.edit { prefs ->
            prefs[Keys.SHOP_NAME] = settings.shopName.trim().ifBlank { "Snehal Super Market" }
            prefs[Keys.SHOP_ADDRESS] = settings.shopAddress.trim()
            prefs[Keys.SHOP_MOBILE] = settings.shopMobile.trim()
            prefs[Keys.SHOP_WHATSAPP] = settings.shopWhatsApp.trim()
            prefs[Keys.SHOP_LOGO_URI] = settings.shopLogoUri
            prefs[Keys.CURRENCY_SYMBOL] = settings.currencySymbol.trim().ifBlank { "₹" }
            prefs[Keys.DEFAULT_MILK_PRICE] = settings.defaultMilkPrice.coerceAtLeast(0.0)
            prefs[Keys.DEFAULT_BILLING_MONTH] = settings.defaultBillingMonth.coerceIn(1, 12)
            prefs[Keys.DEFAULT_BILLING_YEAR] = settings.defaultBillingYear.coerceIn(2020, 2100)
            prefs[Keys.LANGUAGE_CODE] = if (settings.languageCode == "mr") "mr" else "en"
            prefs[Keys.DAILY_REMINDER_ENABLED] = settings.dailyEntryReminderEnabled
            prefs[Keys.DAILY_REMINDER_HOUR] = settings.dailyReminderHour.coerceIn(0, 23)
            prefs[Keys.DAILY_REMINDER_MINUTE] = settings.dailyReminderMinute.coerceIn(0, 59)
            prefs[Keys.MONTHLY_BILL_REMINDER] = settings.monthlyBillReminderEnabled
            prefs[Keys.PENDING_PAYMENT_REMINDER] = settings.pendingPaymentReminderEnabled
        }
    }

    suspend fun updateLanguage(languageCode: String) {
        context.shopSettingsDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_CODE] = if (languageCode == "mr") "mr" else "en"
        }
    }

    suspend fun updateLogoUri(uri: String) {
        context.shopSettingsDataStore.edit { prefs ->
            prefs[Keys.SHOP_LOGO_URI] = uri
        }
    }
}
