package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.MonthlyBillEntity
import com.example.data.preferences.ShopSettings
import java.io.File
import java.net.URLEncoder

object WhatsAppShareHelper {

    fun buildBillMessage(
        bill: MonthlyBillEntity,
        settings: ShopSettings,
        useMarathi: Boolean = settings.languageCode == "mr"
    ): String {
        val currency = settings.currencySymbol
        val monthName = DateUtils.monthName(bill.billingMonth, marathi = useMarathi)
        val monthYear = "$monthName ${bill.billingYear}"

        return if (useMarathi) {
            buildString {
                appendLine("*${settings.shopName}*")
                appendLine("*मासिक दूध बिल – $monthYear*")
                appendLine()
                appendLine("प्रिय ${bill.customerNameSnapshot},")
                appendLine()
                appendLine("आपले या महिन्याचे दूध बिल खालीलप्रमाणे आहे:")
                appendLine()
                appendLine("मागील बाकी: ${CurrencyFormatter.format(bill.previousOutstanding, currency)}")
                appendLine("$monthName दूध रक्कम: ${CurrencyFormatter.format(bill.totalMilkAmount, currency)}")
                appendLine("एकूण पॅकेट: ${bill.totalPackets}")
                appendLine("मिळालेले पेमेंट: ${CurrencyFormatter.format(bill.paymentsReceived, currency)}")
                appendLine()
                appendLine("*अंतिम देय रक्कम: ${CurrencyFormatter.format(bill.finalPayableAmount, currency)}*")
                appendLine()
                appendLine("${settings.shopName} मधून खरेदी केल्याबद्दल धन्यवाद!")
            }.trim()
        } else {
            buildString {
                appendLine("*${settings.shopName}*")
                appendLine("*Monthly Milk Bill – $monthYear*")
                appendLine()
                appendLine("Dear ${bill.customerNameSnapshot},")
                appendLine()
                appendLine("Please find your monthly milk statement below.")
                appendLine()
                appendLine("Previous Outstanding: ${CurrencyFormatter.format(bill.previousOutstanding, currency)}")
                appendLine("$monthName Milk Charges: ${CurrencyFormatter.format(bill.totalMilkAmount, currency)}")
                appendLine("Total Packets: ${bill.totalPackets}")
                appendLine("Payments Received: ${CurrencyFormatter.format(bill.paymentsReceived, currency)}")
                appendLine()
                appendLine("*Final Outstanding Amount: ${CurrencyFormatter.format(bill.finalPayableAmount, currency)}*")
                appendLine()
                appendLine("Thank you for shopping with ${settings.shopName}.")
                appendLine()
                appendLine("We appreciate your business!")
            }.trim()
        }
    }

    fun normalizeWhatsAppPhone(rawNumber: String): String {
        val digits = rawNumber.filter { it.isDigit() }
        return when {
            digits.isBlank() -> ""
            digits.length == 10 -> "91$digits" // Default India country code for 10-digit kirana numbers
            digits.startsWith("0") && digits.length == 11 -> "91${digits.drop(1)}"
            else -> digits
        }
    }

    /**
     * Opens WhatsApp directly to the customer's number if available, or opens WhatsApp / Share Sheet.
     * Returns a user-facing status note without ever falsely claiming the message was auto-sent.
     */
    fun openWhatsAppTextShare(
        context: Context,
        phoneNumber: String,
        message: String
    ): String {
        val normalized = normalizeWhatsAppPhone(phoneNumber)
        val encoded = URLEncoder.encode(message, "UTF-8")

        if (normalized.isNotBlank()) {
            val uri = Uri.parse("https://wa.me/$normalized?text=$encoded")
            val waIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pm = context.packageManager
            val waPackage = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
                try {
                    pm.getPackageInfo(pkg, 0)
                    true
                } catch (_: Exception) {
                    false
                }
            }
            if (waPackage != null) {
                waIntent.setPackage(waPackage)
            }
            return try {
                context.startActivity(waIntent)
                "Opened WhatsApp with bill message ready to review & send"
            } catch (_: Exception) {
                openGenericTextShare(context, message, "Monthly Milk Bill")
            }
        }

        // No WhatsApp number saved — try WhatsApp app or fallback to Share Sheet
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pm = context.packageManager
        val waPkg = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (_: Exception) {
                false
            }
        }
        return try {
            if (waPkg != null) {
                sendIntent.setPackage(waPkg)
                context.startActivity(sendIntent)
                "No WhatsApp number saved — select contact in WhatsApp to send"
            } else {
                openGenericTextShare(context, message, "Monthly Milk Bill")
            }
        } catch (_: Exception) {
            openGenericTextShare(context, message, "Monthly Milk Bill")
        }
    }

    fun openGenericTextShare(context: Context, message: String, title: String): String {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra(Intent.EXTRA_SUBJECT, title)
        }
        val chooser = Intent.createChooser(shareIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        return "Opened Android Share Menu — confirm sending in your chosen app"
    }

    fun shareFileAttachment(
        context: Context,
        file: File,
        mimeType: String,
        captionText: String = "",
        chooserTitle: String = "Share Statement",
        preferWhatsApp: Boolean = false
    ): String {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            if (captionText.isNotBlank()) {
                putExtra(Intent.EXTRA_TEXT, captionText)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (preferWhatsApp) {
            val pm = context.packageManager
            val waPkg = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
                try {
                    pm.getPackageInfo(pkg, 0)
                    true
                } catch (_: Exception) {
                    false
                }
            }
            if (waPkg != null) {
                intent.setPackage(waPkg)
                return try {
                    context.startActivity(intent)
                    "Opened WhatsApp with PDF attached — review and tap Send"
                } catch (_: Exception) {
                    intent.setPackage(null)
                    val chooser = Intent.createChooser(intent, chooserTitle).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                    "Opened Share Sheet with PDF attached"
                }
            }
        }

        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        return "Opened Share Sheet with file attached"
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}
