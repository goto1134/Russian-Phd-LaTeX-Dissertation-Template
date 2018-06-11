//Пример контейнера с единственным сортом данных
container("one type", String::class)
//Пример контйенера с несколькими сортами данных
container("many type", String::class, Int::class, List::class)
//Пример контейнера с начальными эпизодами
container("with episodes", String::class, Int::class) {
    episode("String")
    episode(1134)
}
