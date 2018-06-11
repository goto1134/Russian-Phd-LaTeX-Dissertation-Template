// Определение элемента
element {
    name = "printer"
    discipline = EpisodeChooseDiscipline.FIFO

    //Ограничения
    constraint((10 over 2).toTime(), String::class, helloContainer)
    constraint((10 over 13).toTime(), String::class, worldContainer)

    //Функция
    function = { arguments: List<*>, eventHelper: EventHelper ->
        //Эпизод - результат конкатенации двух входных значений
        val resultEvent =
                eventHelper.publishEpisode(
                        value = "${arguments[0]} ${arguments[1] ?: ""}",
                        //Задержка появления эпизода
                        delay = (1 over 5).toTime(),
                        //Длительность эпизода
                        duration = 6.toTime(),
                        //Контейнер, содержащий результат
                        container = resultContainer.container)
        setOf(resultEvent)
    }
}
