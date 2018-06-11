intface {
    val aContainer =
            container("a", Int::class) {
                episode(50)
            }
    val bContainer =
            container("b", Int::class) {
                episode(14)
            }
    val resultContainer =
            container("result", Int::class)


    element {
        name = "Euclidean"

        constraint((1 over 5).toTime(), Int::class, aContainer)
        constraint((1 over 5).toTime(), Int::class, bContainer)

        function = { arguments: List<*>, eventHelper: EventHelper ->
            val actions = mutableSetOf<Action>()
            val a = arguments[0] as Int
            val b = arguments[1] as Int
            if (a == 0 || b == 0)
                actions += eventHelper.publishEpisode(
                        value = a + b,
                        delay = (1 over 5).toTime(),
                        container = resultContainer.container)
            else {
                val max = max(a, b)
                val min = min(a, b)
                actions += eventHelper.publishEpisode(
                        value = max % min,
                        delay = (1 over 5).toTime(),
                        container = aContainer.container)
                actions += eventHelper.publishEpisode(
                        value = min,
                        delay = (1 over 5).toTime(),
                        container = bContainer.container)
            }
            actions
        }
    }

}