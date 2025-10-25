package org.strigate.ferrot.analytics

interface AnalyticsLogger {
    fun setConsent(enabled: Boolean)
    fun setUserId(id: String?)
    fun setUserProperty(name: String, value: String?)
    fun logScreen(screenName: String, params: Map<String, Any?> = emptyMap())
    fun logEvent(eventName: String, params: Map<String, Any?> = emptyMap())
    fun logEvent(eventName: String)
}
