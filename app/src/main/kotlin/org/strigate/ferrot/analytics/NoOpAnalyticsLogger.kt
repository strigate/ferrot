package org.strigate.ferrot.analytics

class NoOpAnalyticsLogger : AnalyticsLogger {
    override fun setConsent(enabled: Boolean) = Unit
    override fun setUserId(id: String?) = Unit
    override fun setUserProperty(name: String, value: String?) = Unit
    override fun logScreen(screenName: String, params: Map<String, Any?>) = Unit
    override fun logEvent(eventName: String, params: Map<String, Any?>) = Unit
    override fun logEvent(eventName: String) = Unit
}
