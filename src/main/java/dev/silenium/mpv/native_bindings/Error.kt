package dev.silenium.mpv.native_bindings

import dev.silenium.mpv.native_bindings.api.NativeEnum

enum class Error(override val value: Int): NativeEnum<Error> {
    SUCCESS(0),
    EVENT_QUEUE_FULL(-1),
    NOMEM(-2),
    UNINITIALIZED(-3),
    INVALID_PARAMETER(-4),
    OPTION_NOT_FOUND(-5),
    OPTION_FORMAT(-6),
    OPTION_ERROR(-7),
    PROPERTY_NOT_FOUND(-8),
    PROPERTY_FORMAT(-9),
    PROPERTY_UNAVAILABLE(-10),
    PROPERTY_ERROR(-11),
    COMMAND(-12),
    LOADING_FAILED(-13),
    AO_INIT_FAILED(-14),
    VO_INIT_FAILED(-15),
    NOTHING_TO_PLAY(-16),
    UNKNOWN_FORMAT(-17),
    UNSUPPORTED(-18),
    NOT_IMPLEMENTED(-19),
    GENERIC(-20);

    companion object {
        private val valueMap = entries.associateBy(Error::value)

        fun fromValue(value: Int): Error {
            if (value >= 0) return SUCCESS
            return valueMap[value] ?: error("Unknown error value: $value")
        }
    }
}
