//Жесткое требование
constraint(
time = (10 over 2).toTime(),
type = String::class,
container = helloContainer
)
//Мягкое требование
constraint(
time = (10 over 2).toTime(),
type = Int::class
)
// Сокращённая форма записи
constraint((10 over 13).toTime(), String::class, worldContainer)