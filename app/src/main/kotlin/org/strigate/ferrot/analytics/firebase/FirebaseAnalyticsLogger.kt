package org.strigate.ferrot.analytics.firebase

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import org.strigate.ferrot.analytics.AnalyticsLogger

class FirebaseAnalyticsLogger(
    private val firebaseAnalytics: FirebaseAnalytics?,
) : AnalyticsLogger {
    private var collectionEnabled: Boolean = false

    override fun setConsent(enabled: Boolean) {
        collectionEnabled = enabled
        firebaseAnalytics?.setAnalyticsCollectionEnabled(enabled)
    }

    override fun setUserId(id: String?) {
        firebaseAnalytics?.setUserId(id)
    }

    override fun setUserProperty(name: String, value: String?) {
        firebaseAnalytics?.setUserProperty(name, value)
    }

    override fun logScreen(
        screenName: String,
        params: Map<String, Any?>,
    ) {
        if (!collectionEnabled) return
        val analytics = firebaseAnalytics ?: return
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            params.forEach { (k, v) -> putValue(this, k, v) }
        }
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    override fun logEvent(
        eventName: String,
        params: Map<String, Any?>,
    ) {
        if (!collectionEnabled) return
        val analytics = firebaseAnalytics ?: return
        val bundle = Bundle().apply {
            params.forEach { (k, v) -> putValue(this, k, v) }
        }
        analytics.logEvent(eventName, bundle)
    }

    override fun logEvent(eventName: String) {
        logEvent(eventName, emptyMap())
    }

    private fun putValue(
        bundle: Bundle,
        key: String,
        value: Any?,
    ) {
        when (value) {
            null -> Unit
            is String -> bundle.putString(key, value)
            is Int -> bundle.putInt(key, value)
            is Long -> bundle.putLong(key, value)
            is Double -> bundle.putDouble(key, value)
            is Float -> bundle.putFloat(key, value)
            is Boolean -> bundle.putString(key, value.toString())
            else -> bundle.putString(key, value.toString())
        }
    }
}