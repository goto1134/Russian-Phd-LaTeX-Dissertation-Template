package com.github.goto1134.pfl.model

import java.io.File
import kotlin.reflect.KClass

/**
 * Created by Andrew
 * on 08.04.2018.
 */

fun <T> Episode<T>.toPlantUML(): String {
    return "episode(\"$value\", \"$start\",\"$duration\",\"$end\" )"
}

fun Element.toPlantUML(): String = """
class "$name"
${constraints.joinToString("\n") { it.toPlantUML(name) }}
""".trimIndent()

fun Element.Constraint.toPlantUML(name: String): String = when (this.requirement) {
    is Requirement.Soft -> "${requirement.type.simpleName} --> $name : t= $delay"
    is Requirement.Strong -> "${requirement.container.name}.${requirement.type.simpleName} --> $name : t= $delay"
}

fun ProcessTree.Node.toPlantUML(stateFile: File, intface: Interface) {

    val stringBuilder = StringBuilder()
    stringBuilder.appendln("@startuml")

    stringBuilder.appendln("!definelong episode(name,st,dr,en)")
    stringBuilder.appendln("class \"name\" << (E,#FFAAAA) Episode >> {")
    stringBuilder.appendln("start = st")
    stringBuilder.appendln("duration = dr")
    stringBuilder.appendln("end = en")
    stringBuilder.appendln("}")
    stringBuilder.appendln("!enddefinelong")
    stringBuilder.appendln("title Time = $time")

    intface.containers.asSequence().flatMap { it.types.asSequence() }.distinct().forEach {
        stringBuilder.appendln("mix_interface ${it.simpleName}")
    }
    intface.containers.forEach { container: Container ->
        stringBuilder.appendln("package \"${container.name}\" <<Cloud>> {")

        container.types.forEach { kClass: KClass<out Any> ->
            stringBuilder.appendln("package \"${container.name}.${kClass.simpleName}\" {")
            state.episodes.entries
                    .filter { it.value == container && it.key.kClass == kClass }
                    .map { it.key }
                    .forEach { stringBuilder.appendln(it.toPlantUML()) }
            stringBuilder.appendln("}")
        }
        stringBuilder.appendln("}")
        container.types.forEach {
            stringBuilder.appendln("${container.name}.${it.simpleName} -.- ${it.simpleName}")
        }
    }
    intface.elements.forEach { stringBuilder.appendln(it.toPlantUML()) }
    stringBuilder.appendln("@enduml")
    stateFile.writeText(stringBuilder.toString())
}
