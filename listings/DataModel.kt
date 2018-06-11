package com.github.goto1134.pfl.model

import java.util.*
import kotlin.reflect.KClass

/**
 * Resources of the computational net.
 *
 * @property value value of the resource. Used for computations as parameter of a function.
 * @property start a moment of time when the resource appeared in its [Container]
 * @property duration duration of the episode life after [start]. Defines [end] time.
 */
data class Episode<out T>(val value: T, val start: Time.RationalTime, val duration: Time = Time.Infinity, val kClass: KClass<*>) {
    companion object {

        inline fun <reified T> episode(value: T, start: Time.RationalTime, duration: Time = Time.Infinity): Episode<T> {
            return Episode(value, start, duration, T::class)
        }
    }

    /**
     * end time of the episode. When end time is reached, the episode can't be used for computations.
     */
    val end: Time
        get() = start + duration
}


/**
 * Contains [Episode]s of defined types.
 *
 * @property name container's identifier
 * @param types types of supported episodes
 */
class Container(val name: String, val types: Collection<KClass<out Any>>)

/**
 * Contains current initialState of the interface
 * It is supposed that all types are orthogonal (you shouldn't put a type and it's subtype or supertype to one container)
 *
 *  TODO Implement subtype type hierarchy when the episode is put to the container with the nearest supertype
 *
 * @property episodes is a map representing relation of an [Episode] contained in [Container]
 */
class State(val episodes: Map<Episode<*>, Container> = emptyMap()) {

    fun remove(episode: Episode<*>): State {
        val episodes = episodes.toMutableMap()
        remove(episodes, setOf(episode))
        return State(episodes)
    }

    private fun remove(episodes: MutableMap<Episode<*>, Container>, episode: Collection<Episode<*>>) {
        episode.forEach { episodes.remove(it) }
    }

    fun removeAll(arguments: Collection<Episode<*>>): State {
        val episodes = episodes.toMutableMap()
        remove(episodes, arguments)
        return State(episodes)
    }

    fun add(episode: Episode<*>, container: Container): State {
        check(container.types.contains(episode.kClass)) {
            "container ${container.name} cant't contain ${episode.kClass.qualifiedName} class"
        }
        val episodes = episodes.toMutableMap()
        episodes[episode] = container
        return State(episodes)
    }

    fun getElementsAsSequence(targetContainer: Container, targetKClass: KClass<*>): Sequence<Pair<Episode<*>, Container>> {
        return episodes.asSequence().filter { (episode, container) ->
            targetContainer == container && targetKClass == episode.kClass
        }.map { entry -> Pair(entry.key, entry.value) }
    }

    fun performActionsAndCollectGarbage(time: Time.RationalTime, actionsToPerform: Set<Action>): State {
        val newEpisodes = episodes.filter { it.key.end >= time }.toMutableMap()
        actionsToPerform.forEach {
            when (it) {
                is Action.PublishEpisode -> {
                    check(it.container.types.contains(it.episode.kClass)) {
                        "container ${it.container.name} cant't contain ${it.episode.kClass.qualifiedName} class"
                    }
                    newEpisodes.put(it.episode, it.container)
                }
            }
        }
        return State(newEpisodes)
    }

    fun getElementsFor(constraint: Element.Constraint, discipline: EpisodeChooseDiscipline): Sequence<Episode<*>> {
        val requirement = constraint.requirement
        return when (requirement) {
            is Requirement.Strong -> {
                episodes.entries.asSequence().filter {
                    it.key.kClass == requirement.type && it.value == requirement.container
                }
            }
            is Requirement.Soft -> {
                episodes.entries.asSequence().filter {
                    it.key.kClass == requirement.type
                }
            }
        }.map {
            it.key
        }.sortedWith(discipline.comparator)
    }
}

/**
 * Итерфейс системы. Задаёт упорядоченный набор элементов, а так же набор контейнеров.
 */
class Interface {
    private val mutableContainers: MutableSet<Container> = mutableSetOf()
    val executionDiscipline: ExecutionDiscipline = ExecutionDiscipline.Sequential
    val containers: Set<Container>
        get() = mutableContainers

    private val mutableElements: MutableList<Element> = mutableListOf()
    val elements: List<Element>
        get() = mutableElements
    var initialActions = mutableSetOf<Action.PublishEpisode>()

    /**
     * Ассоциативный массив для поиска контейнеров, которые могу содержать эпизоды нужного типа
     */
    private val klassToContainer: MutableMap<KClass<*>, MutableSet<Container>> = mutableMapOf()
    /**
     * Ассоциативный массив для поиска элементов, которые могут обработать эпизод
     */
    private val requirementsData: MutableMap<Requirement, SortedSet<Element>> = mutableMapOf()

    fun addContainer(container: Container) {
        container.types.forEach { klassToContainer.getOrPut(it, ::mutableSetOf).add(container) }
        mutableContainers.add(container)
    }

    fun addElement(element: Element) {
        element.constraints.map(Element.Constraint::requirement).forEach {
            requirementsData.getOrPut(it) { sortedSetOf() }
                    .add(element)
        }
        mutableElements.add(element)
    }


    fun getActivatedBy(kClass: KClass<*>, container: Container): List<Element> {
        // TODO("Учесть индексы-приоритеты)
        val strongRequired = requirementsData.getOrDefault(
                Requirement.Strong(kClass, container), sortedSetOf())
        val softRequired = requirementsData.getOrDefault(Requirement.Soft(kClass), sortedSetOf())
        return strongRequired.union(softRequired).sorted()
    }

    fun <T> put(episode: Episode<T>, container: Container) {
        initialActions.add(Action.PublishEpisode(episode, container))
    }
}

typealias ElementInvocation = Pair<Element, List<Episode<*>>>

sealed class ExecutionDiscipline {
    abstract fun getElementInvocation(elements: List<Element>, startState: State): Set<Set<ElementInvocation>>

    object Sequential : ExecutionDiscipline() {
        private val comparator = compareBy<Pair<Int, Element>> { it.second }.thenBy { it.first }

        override fun getElementInvocation(elements: List<Element>, startState: State): Set<Set<ElementInvocation>> {
            var state = startState
            //Order elements by 1. maxTime of constraint and 2. Index in the initial list
            val orderedElements = elements.asSequence()
                    .mapIndexed { index, element -> Pair(index, element) }
                    .sortedWith(comparator)
                    .map { it.second }
            val actions = mutableSetOf<ElementInvocation>()
            for (element: Element in orderedElements) {
                var successful: Boolean
                elements@ do {
                    successful = false

                    val arguments = mutableListOf<Episode<*>>()
                    for (constraint in element.constraints) {
                        val argument =
                                state.getElementsFor(constraint, element.discipline)
                                        .filterNot { it in arguments }.firstOrNull() ?: continue@elements
                        arguments.add(argument)
                        state = state.remove(argument)
                    }
                    successful = true
                    state = state.removeAll(arguments)
                    actions.add(Pair(element, arguments))
                } while (successful)
            }
            return setOf(actions)
        }
    }
}

enum class EpisodeChooseDiscipline(val comparator: Comparator<Episode<*>>) {
    FIFO(compareBy { it.start }),
    LIFO(compareByDescending { it.start }),
    FEFO(compareBy { it.end })
}

class Element : Comparable<Element> {
    override fun compareTo(other: Element): Int {
        return maxTime.compareTo(other.maxTime)
    }

    class Constraint(val requirement: Requirement, val delay: Time.RationalTime) : Comparable<Constraint> {
        override fun compareTo(other: Constraint) = delay.compareTo(other.delay)
    }

    var name: String = "Element"
    var function: (args: List<*>, eventHelper: EventHelper) -> Set<Action> = { _: List<*>, _: EventHelper -> emptySet() }
    val constraints: SortedSet<Constraint> = sortedSetOf(compareByDescending<Constraint> { it })
    val maxTime: Time.RationalTime by lazy {
        constraints.first()!!.delay
    }
    var discipline = EpisodeChooseDiscipline.FIFO
}


sealed class Requirement(val type: KClass<out Any>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Requirement) return false

        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }

    class Soft(type: KClass<out Any>) : Requirement(type)
    class Strong(type: KClass<out Any>, val container: Container) : Requirement(type) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Strong) return false
            if (!super.equals(other)) return false

            if (container != other.container) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + container.hashCode()
            return result
        }

    }
}

sealed class Action(val time: Time.RationalTime) {
    class PublishEpisode(val episode: Episode<*>, val container: Container) : Action(episode.start)
}