package com.github.goto1134.pfl

import com.github.goto1134.pfl.model.EventHelper
import com.github.goto1134.pfl.model.Modeler
import com.github.goto1134.pfl.model.toPlantUML
import com.github.goto1134.pfl.model.toTime
import com.pploder.rational.over
import java.io.File

/**
 * Created by Andrew
 * on 08.04.2018.
 */
fun main(args: Array<String>) {
    val intface =
            intface {
                val helloContainer =

                        container("hello", String::class) {
                            episode("Hello")
                        }
                val worldContainer =
                        container("world", String::class) {
                            episode("World!")
                        }
                val resultContainer =
                        container("result", String::class)



                element {
                    name = "printer"

                    constraint((10 over 2).toTime(), String::class, helloContainer)
                    constraint((10 over 13).toTime(), String::class, worldContainer)

                    function = { arguments: List<*>, eventHelper: EventHelper ->
                        val resultEvent =
                                eventHelper.publishEpisode(
                                        value = "${arguments[0]} ${arguments[1] ?: ""}",
                                        delay = (1 over 5).toTime(),
                                        duration = (6 over 1).toTime(),
                                        container = resultContainer.container)
                        setOf(resultEvent)
                    }
                }

            }
    val model = Modeler.buildProcessTree(intface)

    for (pair in model.iterator()) {
        pair.second.toPlantUML(stateFile(pair.first), intface)
    }
}

private fun stateFile(stateNum: String): File {
    val s = "state$stateNum.puml"
    val file = getFile(s)
    return file
}


private fun getFile(fileName: String): File {
    val file = File("./" + fileName)
    if (!file.exists()) file.createNewFile()
    return file
}