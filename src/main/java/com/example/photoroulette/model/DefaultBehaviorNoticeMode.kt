package com.example.photoroulette.model

enum class DefaultBehaviorNoticeMode(
    val storageValue: String,
) {
    Visible("visible"),
    AutoHidden("auto_hidden"),
    UserHidden("user_hidden"),
    ;

    companion object {
        fun fromStorageValue(value: String?): DefaultBehaviorNoticeMode? {
            return entries.firstOrNull { mode -> mode.storageValue == value }
        }
    }
}
