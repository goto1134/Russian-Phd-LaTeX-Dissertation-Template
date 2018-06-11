package com.github.goto1134.pfl.model

import java.util.*

//typealias Timeline = SortedMap<Time.RationalTime, MutableSet<Action>>

class Timeline(private val sortedMap: SortedMap<Time.RationalTime, MutableSet<Action>> = sortedMapOf()) {
    fun copyWith(actions: Collection<Action>): Timeline {
        val treeMap = TreeMap(sortedMap)
        actions.forEach { treeMap.getOrPut(it.time, { mutableSetOf() }).add(it) }
        return Timeline(treeMap)
    }

    fun isEmpty() = sortedMap.isEmpty()
    fun firstKey(): Time.RationalTime? = sortedMap.firstKey()
    fun remove(nextTime: Time.RationalTime): Set<Action>? {
        return sortedMap.remove(nextTime)
    }
}

class ProcessTree(state: State = State()) {

    val root: Node

    init {
        root = Node(Time.RationalTime.ZERO, state)
    }

    fun iterator() = ProcessTreeIterator(this)

    class Node(val time: Time.RationalTime, val state: State, val parent: Node? = null) {

        val children = mutableSetOf<Node>()
        fun newChild(time: Time.RationalTime, state: State): Node {
            val child = Node(time, state, this)
            children.add(child)
            return child
        }
    }

    class ProcessTreeIterator(val tree: ProcessTree) : Iterator<Pair<String, ProcessTree.Node>> {
        val queue: Queue<Pair<String, ProcessTree.Node>> = LinkedList()

        init {
            tree.root.children.mapIndexed { index: Int, node: ProcessTree.Node ->
                Pair(index.toString(), node)
            }.toCollection(queue)
        }

        override fun hasNext() = queue.isNotEmpty()

        override fun next(): Pair<String, ProcessTree.Node> {
            val poll = queue.poll()
            poll.second.children.mapIndexed { index, node -> Pair("${poll.first}.$index", node) }
                    .toCollection(queue)
            return poll
        }
    }

}

data class Branch(val node: ProcessTree.Node, val timeline: Timeline, val state: State = State())

class EventHelper(val time: Time.RationalTime) {
    inline fun <reified T> publishEpisode(value: T, delay: Time.RationalTime, duration: Time, container: Container): Action.PublishEpisode {
        check(delay > 0)
        val episode = Episode.episode(value, this.time + delay, duration)
        return Action.PublishEpisode(episode, container)
    }
}

object Modeler {
    fun buildProcessTree(intface: Interface): ProcessTree {
        val processTree = ProcessTree()
        val initialTimeline = Timeline().copyWith(intface.initialActions)
        var branches = setOf(Branch(processTree.root, initialTimeline))

        while (branches.all { branch -> branch.timeline.isEmpty() }.not()) {
            val nextBranches = mutableSetOf<Branch>()
            val (passive, active) = branches.partition { it.timeline.isEmpty() }
            nextBranches.addAll(passive)

            active.flatMap { (node, actions, state) ->
                nextBranches(actions, node, state, intface)
            }.toCollection(nextBranches)
            branches = nextBranches
        }
        return processTree
    }

    private fun nextBranches(timeline: Timeline, node: ProcessTree.Node, state: State, intface: Interface): Set<Branch> {
        //Form next State and get activated elements
        val nextTime = timeline.firstKey()!!
        val actionsToPerform = timeline.remove(nextTime)!!
        val stateBeforeInvocations = state.performActionsAndCollectGarbage(nextTime, actionsToPerform)
        val nextNode = node.newChild(nextTime, stateBeforeInvocations)

        //Get next timeline
        val activatedElements = getActivatedElements(actionsToPerform, intface)
        val elementInvocation =
                intface.executionDiscipline.getElementInvocation(activatedElements, stateBeforeInvocations)
        return when {
            elementInvocation.isEmpty() -> setOf(Branch(nextNode, timeline, stateBeforeInvocations))
            else -> elementInvocation.asSequence().map {
                var nextState = stateBeforeInvocations
                val nextActions = it.flatMap {
                    nextState = nextState.removeAll(it.second)
                    val arguments = it.second.map { it.value }
                    val eventHelper = EventHelper(nextTime + it.first.maxTime)
                    it.first.function(arguments, eventHelper)
                }.toSet()
                val nextTimeline = timeline.copyWith(nextActions)
                Branch(nextNode, nextTimeline, nextState)
            }.toSet()
        }
    }


    fun getActivatedElements(actionsToPerform: Set<Action>, intface: Interface): List<Element> {
        return actionsToPerform.flatMap {
            when (it) {
                is Action.PublishEpisode -> {
                    val elements = intface.getActivatedBy(it.episode.kClass, it.container)
                    elements
                }
            }
        }.distinct()
    }
}