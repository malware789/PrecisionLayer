package com.example.precisionlayertesting.core.utils

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("precision_layer_prefs", Context.MODE_PRIVATE)

    fun getContext(): Context = context

    fun saveWorkspaceId(id: String?) {
        prefs.edit().putString("workspace_id", id).apply()
    }

    fun getWorkspaceId(): String? {
        return prefs.getString("workspace_id", null)
    }

    fun setWorkspaceName(name: String?) = prefs.edit().putString("workspace_name", name).apply()
    fun getWorkspaceName(): String? = prefs.getString("workspace_name", null)

    fun saveUserRole(role: String?) = prefs.edit().putString("user_role", role).apply()
    fun getUserRole(): String? = prefs.getString("user_role", null)

    fun saveUserId(id: String) {
        prefs.edit().putString("user_id", id).apply()
    }

    fun getUserId(): String? {
        return prefs.getString("user_id", null)
    }

    fun saveUserEmail(email: String) {
        prefs.edit().putString("user_email", email).apply()
    }

    fun getUserEmail(): String? {
        return prefs.getString("user_email", null)
    }

    fun saveAccessToken(token: String) {
        prefs.edit().putString("access_token", token).apply()
    }

    fun getAccessToken(): String? {
        return prefs.getString("access_token", null)
    }

    fun saveRefreshToken(token: String) {
        prefs.edit().putString("refresh_token", token).apply()
    }

    fun getRefreshToken(): String? {
        return prefs.getString("refresh_token", null)
    }
    
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
