package com.example.photoroulette.model

enum class SwipeAction(
    val storageValue: String,
) {
    Skip("skip"),
    Delete("delete"),
    Previous("previous"),
    Next("next"),
    ;

    companion object {
        fun fromStorageValue(value: String?): SwipeAction? = entries.firstOrNull {
            it.storageValue == value
        }
    }
}
