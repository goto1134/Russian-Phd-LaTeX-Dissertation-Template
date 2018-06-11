//Пример объявления эпизода типа String
episode("World!")
//Пример объявления эпизода типа List<Int>
episode(listOf(1, 2, 3, 4))
// Пример объявления эпизода с фиксированной длительностью
episode("fixed duration", (2 over 7).toTime())