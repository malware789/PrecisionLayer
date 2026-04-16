package com.example.precisionlayertesting.core.utils

import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.TextView

object BugStyleUtils {

    fun applySeverityStyle(view: TextView, severity: String) {
        val (bgColor, textColor) = when (severity.lowercase()) {
            "critical" -> Pair("#fef2f2", "#dc2626") // Red
            "high" -> Pair("#fff7ed", "#ea580c")    // Orange
            "medium" -> Pair("#fefce8", "#ca8a04")  // Yellow
            "low" -> Pair("#f0fdf4", "#16a34a")     // Green
            else -> Pair("#f1f5f9", "#475569")      // Gray
        }
        view.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bgColor))
        view.setTextColor(Color.parseColor(textColor))
        view.text = severity.uppercase()
    }

    fun applyStatusStyle(view: TextView, status: String) {
        val (bgColor, textColor) = when (status.lowercase()) {
            "open" -> Pair("#eff6ff", "#1d4ed8")       // Blue
            "in progress" -> Pair("#f5f3ff", "#6d28d9") // Purple
            "closed" -> Pair("#f8fafc", "#0f172a")     // Dark/Black
            else -> Pair("#f1f5f9", "#475569")          // Gray
        }
        view.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bgColor))
        view.setTextColor(Color.parseColor(textColor))
        view.text = status.uppercase()
    }
}
