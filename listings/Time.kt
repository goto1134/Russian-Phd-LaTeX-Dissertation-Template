package com.github.goto1134.pfl.model

import com.pploder.rational.Rational
import com.pploder.rational.toRational


sealed class Time : Comparable<Time> {
    abstract operator fun plus(other: Time): Time
    abstract operator fun minus(other: Time): Time
    abstract operator fun div(other: Time): Time
    abstract operator fun times(other: Time): Time

    operator fun compareTo(other: Number) = this.compareTo(other.toTime())

    object Infinity : Time() {
	        private const val INFINITY_SIGN = "\u221E"

        override fun times(other: Time): Time {
            require(other != RationalTime.ZERO) { "Multiplication of $INFINITY_SIGN by 0" }
            return Infinity
        }

        override fun div(other: Time): Time {
            require(other is RationalTime) { "Diversion of Infinity by Infinity" }
            other as RationalTime
            return Infinity
        }

        override fun minus(other: Time): Time = when (other) {
            is Infinity -> RationalTime.ZERO
            else -> Infinity
        }

        override fun compareTo(other: Time): Int = when (other) {
            Infinity -> 0
            else -> 1
        }

        override operator fun plus(other: Time) = Infinity
        override fun toString() = INFINITY_SIGN
    }

    data class RationalTime(val value: Rational) : Time() {
        companion object {
            val ZERO = RationalTime(Rational.ZERO)
        }

        init {
            require(value >= Rational.ZERO)
        }

        override fun times(other: Time): Time {
            if (this == ZERO) {
                require(other != Infinity) { "Multiplication of $other by 0" }
                return ZERO
            }
            return when (other) {
                is Infinity -> Infinity
                is RationalTime -> RationalTime(value * other.value)
            }
        }

        override fun div(other: Time): Time {
            require(other != RationalTime.ZERO) { "Diversion by zero" }
            return when (other) {
                is Infinity -> RationalTime.ZERO
                is RationalTime -> RationalTime(value / other.value)
            }
        }

        override fun minus(other: Time): Time {
            require(other is RationalTime)
            require(other <= this) { "Time can't be negative" }
            other as RationalTime
            return RationalTime(value - other.value)
        }


        override fun compareTo(other: Time): Int = when (other) {
            is RationalTime -> this.value.compareTo(other.value)
            else -> -1
        }

        operator fun plus(other: RationalTime) = RationalTime(other.value + this.value)

        override operator fun plus(other: Time) = when (other) {
            is RationalTime -> this.plus(other)
            else -> Infinity
        }

        override fun toString() = value.toString()

    }
}

fun Number.toTime() = Time.RationalTime(this.toRational())