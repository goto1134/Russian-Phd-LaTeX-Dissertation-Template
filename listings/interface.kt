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