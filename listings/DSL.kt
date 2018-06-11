package com.github.goto1134.pfl

import com.github.goto1134.pfl.model.*
import com.github.goto1134.pfl.model.Time.RationalTime
import com.github.goto1134.pfl.model.Time.RationalTime.Companion.ZERO
import kotlin.reflect.KClass

/**
 * Created by Andrew
 * on 01.04.2018.
 */

fun Element.constraint(time: RationalTime, type: KClass<out Any>, container: ContainerToken? = null) {
    val requirement = when (container) {
        null -> Requirement.Soft(type)
        else -> Requirement.Strong(type, container.container)
    }
    constraints.add(Element.Constraint(requirement, time))
}

fun intface(settings: Interface.() -> Unit): Interface {
    val intface = Interface()
    intface.settings()
    return intface
}

@DslMarker
annotation class InterfaceDSLMarker

fun Interface.element(init: Element.() -> Unit): Element {
    val element = Element()
    element.init()
    addElement(element)
    return element
}

fun Interface.container(name: String, vararg types: KClass<out Any>, init: ContainerToken.() -> Unit = {}): ContainerToken {
    val container = Container(name, types.toList())
    addContainer(container)
    val containerDSL = ContainerToken(this, container)
    containerDSL.init()
    return containerDSL
}

@InterfaceDSLMarker
class ContainerToken(val intface: Interface, val container: Container) {
    fun <T> put(episode: Episode<T>) {
        intface.put(episode, container)
    }

    inline fun <reified T> episode(value: T, duration: Time = Time.Infinity) {
        val episode = Episode(value, RationalTime.ZERO, duration, T::class)
        put(episode)
    }

    fun <T> episode(value: T, kClass: KClass<*>, duration: Time = Time.Infinity) {
        val episode = Episode(value, ZERO, duration, kClass)
        put(episode)
    }

    fun nullEpisode(kClass: KClass<*>, duration: Time = Time.Infinity) {
        val episode = Episode(null, ZERO, duration, kClass)
        put(episode)
    }
}
