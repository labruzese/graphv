package com.fischerabruzese.graph

import java.math.BigInteger
import java.util.*
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Adjacency Matrix implementation of [Graph]. Improves on efficiency of
 * [Graph]'s operation where improvements can be made from direct access to
 * adjacency matrix. Permits [Any] elements as the vertex type and does **not**
 * permit nullable types.
 *
 * This implementation provides constant time edge and vertex search via
 * [get][AMGraph.getWithIndex] and [contains][AMGraph.contains]; O(n^2) vertex addition
 * and removal via [add][AMGraph.add] and [remove][AMGraph.removeEdgeWithIndex]; Constant
 * time edge addition and removal via [set][AMGraph.setWithIndex] and [removeEdgeWithIndex];
 *
 * The efficiencies for project algorithms are as follows:
 *  * Dijkstra's Algorithm ->  O(V^2*log(V))
 *  * Breadth First Search -> O(V^2)
 *  * Depth First Search -> O(V^2)
 *  * HCS (Highly Connected Subgraphs) -> O(V^4)
 *
 * **Note that this implementation is not synchronized.** If multiple threads
 * access a [AMGraph] concurrently and at least one of the threads modifies the
 * graph structurally, it *must* be synchronized externally.
 *
 * The iterators returned by this class's [iterator] and methods are
 * *fail-fast*: if the graph is structurally modified in any way  at any time
 * after the iterator is created, the iterator will throw a
 * [ConcurrentModificationException]. However, these exceptions are based on
 * the structure of [ArrayList]'s fail-fast implementation. Therefore, it would
 * be wrong to write a program that depended on this exception for
 * its correctness: *the fail-fast behavior of iterators
 * should be used only to detect bugs.*
 *
 * @author Paul Fischer
 * @author Skylar Abruzese
 *
 * @see Graph
 */
class AMGraph<E : Any> private constructor(
    vertices: ArrayList<E>?,
    indexLookup: HashMap<E, Int>?,
    edgeMatrix: Array<IntArray>?
) : Graph<E>() {

    /**
     * An ordered collection of all the vertices in this graph.
     *
     * The location of a vertex in this array is referred to as the vertex 'id'
     */
    private val vertices: ArrayList<E> = vertices ?: ArrayList<E>() // Vert index --> E

    /**
     * An ordered matrix of the edges in the graph.
     *
     * The `[i]` `[j]` element is the weight of the edge **from** vertex id
     * `[i]` **to** vertex id `[j]`
     */
    private var edgeMatrix: Array<IntArray>

    /**
     * Converts a vertex to its index in [vertices] to enable quick access to
     * [edgeMatrix]
     */
    private val indexLookup: HashMap<E, Int> =
        indexLookup ?: HashMap<E, Int>().apply {
            putAll(vertices?.mapIndexed { id, element -> element to id } ?: emptyList())
        } // E --> Vert index

    /**
     * A cached table of the shortest paths from every vertex to every other
     * vertex.
     *
     *
     * - dijkstraTables`[source]` `[destination]` =
     * Pair(previous vertex id, distance from source)
     *
     * This will be null when all paths are invalid, one vertex will be null if
     * it hasn't been calculated. If the vertex is not in the table then it has
     * no path.
     */
    private var dijkstraTables: Array<Array<Pair<Int, Int>>?>? = null

    init {
        if (edgeMatrix != null) this.edgeMatrix = edgeMatrix
        else this.edgeMatrix = Array(size()) { IntArray(size()) { -1 } }
        //otherwise assume that they're (the other constructors body) going to initialize the edge matrix
    }

    /**
     * If an external user wants to specify the creation of a graph all of its
     * information is stored in this obscene type.
     *
     * [outboundConnections] is converted into vertices, indexLookup, and
     * edgeMatrix in this constructors init
     *
     * @param dummy to avoid type confusion with generics, other constructors
     *        that wish to call this with some dummy for JVM clarity
     * @param outboundConnections the data to store in this graph.
     *
     *        Format:
     *        [Source] paired to a [Collection] of its [Destination]s & [Weight]s
     */
    private constructor(dummy: Any, outboundConnections: Collection<Pair<E, Iterable<Pair<E, Int>>>>) : this(
        vertices = ArrayList(outboundConnections.size * 2), //guess about how big our array is going to be
        indexLookup = null, //we can let them create the empty one for us
        edgeMatrix = null
    ) {
        //Add vertices to vertices and indexLookup
        for (connections in outboundConnections) {
            val source = connections.first
            if (indexLookup.putIfAbsent(source, vertices.size) == null) {
                vertices.add(source)
            }
            for (outboundEdge in connections.second) {
                val destination = outboundEdge.first
                if (indexLookup.putIfAbsent(destination, vertices.size) == null) {
                    vertices.add(destination)
                }
            }
        }
        edgeMatrix = Array(vertices.size) { IntArray(vertices.size) { -1 } }

        //Add edges
        for (connections in outboundConnections) {
            for (outboundEdge in connections.second) {

                if (outboundEdge.second <= 0) edgeMatrix[indexLookup[connections.first]!!][indexLookup[outboundEdge.first]!!] =
                    1
                else edgeMatrix[indexLookup[connections.first]!!][indexLookup[outboundEdge.first]!!] =
                    outboundEdge.second
            }
        }
    }

    /**
     * Constructs a new graph containing [vertices] with no edges.
     *
     * @param E The type of the vertices in the graph
     * @param vertices the vertices to include in the graph
     */
    constructor(vertices: Collection<E>) : this(
        vertices = ArrayList(vertices),
        indexLookup = null,
        edgeMatrix = null
    )

    /**
     * Constructs an empty graph
     */
    constructor() : this(null, null, null)

    /**
     * Alternate constructors and calculation/testing methods
     */
    companion object {
        /**
         * Creates a graph from a graph key generated by [Graph.getKey]
         *
         * @param graphKey the string containing the graph information
         *
         * @return a new [AMGraph] with the vertices, edges and weights in the
         *         key
         */
        @JvmName("graphOfCompressed")
        fun graphOf(graphKey: String): AMGraph<String> {
            try {
                val sectionedStrings = graphKey.split("@").also { if (it.size > 2) throw Exception() }

                val vertices = ArrayList(sectionedStrings[0].split("|"))

                val graph = AMGraph(vertices)

                val edges: ArrayList<Triple<String, String, Int>> = ArrayList(
                    sectionedStrings[1].split("|").map { tripleString ->
                        val components = tripleString.split("#").also { if (it.size != 3) throw Exception() }
                        Triple(components[0], components[1], components[2].toInt())
                    }
                )

                for (e in edges) {
                    graph[e.first, e.second] = e.third
                }

                return graph
            } catch (e: Exception) {
                throw IllegalStateException("Unable to read graph key")
            }
        }

        /**
         * Constructs a new [AMGraph] containing all vertices mentioned in
         * [weightedConnections] with their corresponding edges.
         *
         * --
         *
         * *Note that you may have to specify the parameter name (example
         * attached) to avoid being confused with graphOf-unweightedConnections
         * where the [type][E] is [Pair]*
         *
         * - **[AMGraph].graphOf( [weightedConnections] = Collection...)**
         *
         * @param weightedConnections A collection containing all the edges to
         *        be added to the new graph.
         *
         *        Collection Format:
         *        [Source] paired to a [Collection] of its [Destination]s & [Weight]s
         *
         * @param E The type of the vertices in this graph.
         */
        @JvmName("graphOfOutboundConnectionsList")
        fun <E : Any> graphOf(weightedConnections: Collection<Pair<E, Iterable<Pair<E, Int>>>>) =
            AMGraph(0, weightedConnections)

        /**
         * Constructs a new [AMGraph] containing all vertices mentioned in
         * [connections] with edges of weight 1 between the specified vertices.
         *
         * --
         *
         * *Note that you may have to specify the parameter name (example
         * attached) to avoid being confused with graphOf-weightedConnections
         * where the [type][E] is [Pair]*
         *
         * - **[AMGraph].graphOf( [connections] = Collection...)**
         *
         * @param connections A collection containing all the edges to be added
         *        to the new graph.
         *
         *        Collection Format:
         *        [Source] paired to a [Collection] of its [Destination]s
         * @param E The type of the vertices in this graph.
         */
        @JvmName("graphOfConnectionsList")
        fun <E : Any> graphOf(connections: Collection<Pair<E, Iterable<E>?>>) = AMGraph(0,
            connections.map {
                it.first to (it.second?.map { it2 -> it2 to 1 } ?: emptyList())
            }
        )

        /**
         * Made for testing purposes. Finds the success rate of running Karger's
         * Algorithm with min-cut repeated [kargerness] times for the given
         * [graph].
         * This will repeat Karger's on the graph until it produces the wrong
         * answer. It will calculate the proportion of successes from this,
         * averaging together the results over the [totalRepetitions].
         *
         * @param graph The graph you want to test.
         * @param kargerness The kargerness that you want to test.
         * @param totalRepetitions The total amount of wrong karger's found
         *        before calculating the success rate. The higher the value,
         *        the slower, but higher confidence answer.
         * @param updateInterval Prints the current values at the specified
         *        interval. Can be useful for long calculations. Setting this
         *        below totalRepetitions will automatically turn [printing] on
         *        unless otherwise specified.
         * @param printing Prints the answer and [updateInterval]s to the
         *        console.
         *
         * @return The proportion of times karger's failed given the inputs
         *         above.
         *
         * @throws IllegalStateException Very (and I mean very) rarely or under
         *         extremely extreme circumstances will the min-cut used to
         *         verify a correct min-cut be incorrect and throw this
         *         exception.
         */
        fun <E : Any> findKargerSuccessRate(
            graph: AMGraph<E>,
            kargerness: Int,
            totalRepetitions: Int,
            updateInterval: Int = totalRepetitions + 1,
            printing: Boolean = updateInterval < totalRepetitions
        ): Double {
            //Find real min-cut
            val realMinCut: Int = graph.karger(5000).size

            //Loop Variables
            var i = 1
            var failCount = 1
            var prevFailCount = 1

            while (i <= totalRepetitions) {
                //Check if we need to provide an update
                if (i % updateInterval == 0) {
                    val deltaFails = failCount - prevFailCount
                    val intervalAvgFail = deltaFails / updateInterval.toDouble()
                    val totalAvgFail = failCount / i.toDouble()
                    if (printing) println(
                        "${i - updateInterval}-$i:".padEnd((totalRepetitions.toString().length) * 2 + 2) + "${
                            ((intervalAvgFail - 1) / intervalAvgFail).toString().padEnd(25)
                        }|| Current Average: ${(totalAvgFail - 1) / totalAvgFail}"
                    )
                    prevFailCount = failCount
                }
                var minCutAttempts = 1
                while (true) {
                    val minCutAttempt = graph.karger(kargerness).size
                    if (minCutAttempt < realMinCut) throw IllegalStateException("Finding real min-cut (against all odds) failed")
                    if (minCutAttempt > realMinCut) { //Failed min cut
                        failCount += minCutAttempts
                        break
                    }
                    minCutAttempts++
                }
                i++
            }

            val avgFailCount = failCount / totalRepetitions.toDouble()
            val pSuccess = (avgFailCount - 1) / avgFailCount

            if (printing) print("|V| = ${graph.size()}, kargerness = $kargerness, p-success = $pSuccess")
            return pSuccess
        }

        /**
         * Made for testing purposes. Finds the worst case success rate of
         * running Karger's Algorithm with min-cut repeated [kargerness] times
         * for a graph of size [graphSize].
         * This will repeat Karger's on a graph with the specified size and only
         * 1 correct min-cut until it produces the wrong answer. It will
         * calculate the proportion of successes from this, averaging together
         * the results over the [totalRepetitions].
         *
         * @param graphSize This size of the graph you want to test.
         * @param kargerness The kargerness that you want to test.
         * @param totalRepetitions The total amount of wrong karger's found
         *        before calculating the success rate. The higher the value,
         *        the slower, but higher confidence answer.
         * @param updateInterval Prints the current values at the specified
         *        interval. Can be useful for long calculations. Setting this
         *        below totalRepetitions will automatically turn [printing] on
         *        unless otherwise specified.
         * @param printing Prints the answer and [updateInterval]s to the
         *        console.
         *
         * @return The proportion of times karger's failed given the inputs
         *         above.
         *
         * @throws IllegalStateException Very (and I mean very) rarely or under
         *         extremely extreme circumstances will the min-cut used to
         *         verify a correct min-cut be incorrect and throw this
         *         exception.
         *
         * @see findKargerSuccessRate
         */
        fun findKargerSuccessRate(
            graphSize: Int,
            kargerness: Int,
            totalRepetitions: Int,
            updateInterval: Int = totalRepetitions + 1,
            printing: Boolean = updateInterval < totalRepetitions
        ): Double {
            /* GRAPH CREATION */
            val verts = (0 until graphSize).toList()
            val graph = AMGraph(verts)
            graph.randomize(1.0, 0, 1)
            (graph as Graph<Int>).removeEdge(
                from = 0,
                to = 1
            )
            return findKargerSuccessRate(graph, kargerness, totalRepetitions, updateInterval, printing)
        }
    }

    /*------------------ FUNCTIONALITY ------------------*/

    override fun size() = vertices.size

    override operator fun get(from: E, to: E): Int? {
        return indexLookup[from]?.let { f ->
            indexLookup[to]?.let { t ->
                getWithIndex(f, t) ?: return null
            } ?: throw NoSuchElementException("To Element {$to} does not exist")
        } ?: throw NoSuchElementException("From Element {$from} does not exist")
    }

    /**
     * Uses vertex id's for faster internal access to the edges.
     *
     * @see AMGraph.get
     */
    private fun getWithIndex(from: Int, to: Int): Int? {
        return if (edgeMatrix[from][to] == -1) null
        else edgeMatrix[from][to]
    }

    override operator fun set(from: E, to: E, value: Int): Int? {
        val f = indexLookup[from] ?: throw NoSuchElementException()
        val t = indexLookup[to] ?: throw NoSuchElementException()
        return setWithIndex(f, t, value)
    }

    /**
     * Uses vertex id's for faster internal access to the edges.
     *
     * @see AMGraph.set
     */
    private fun setWithIndex(from: Int, to: Int, value: Int): Int? {
        dijkstraTables = null
        return getWithIndex(from, to).also { edgeMatrix[from][to] = value }
    }

    override fun removeEdge(from: E, to: E): Int? {
        val f = indexLookup[from] ?: throw IllegalArgumentException()
        val t = indexLookup[to] ?: throw IllegalArgumentException()
        return removeEdgeWithIndex(f, t)
    }

    /**
     * Uses vertex id's for faster internal access to the edges.
     *
     * @see AMGraph.removeEdge
     */
    private fun removeEdgeWithIndex(from: Int, to: Int): Int? {
        return setWithIndex(from, to, -1)
    }

    override fun contains(vertex: E): Boolean {
        return indexLookup.containsKey(vertex)
    }

    override fun iterator(): Iterator<E> = vertices.iterator()

    override fun getVertices(): Set<E> {
        return vertices.toSet()
    }

    override fun getEdges(): Set<Pair<E, E>> {
        val edges = mutableSetOf<Pair<E, E>>()
        for (from in edgeMatrix.indices) {
            for (to in edgeMatrix[from].indices) {
                if (edgeMatrix[from][to] >= 0) edges.add(vertices[from] to vertices[to])
            }
        }
        return edges
    }

    override fun clearEdges() {
        edgeMatrix = Array(size()) { IntArray(size()) { -1 } }
        dijkstraTables = null
    }


    override fun addAll(vertices: Collection<E>): Collection<E> {
        val failed = ArrayList<E>()
        this.vertices.ensureCapacity(this.vertices.size + vertices.size)
        for (vert in vertices) {
            if (this.contains(vert)) {
                failed += vert
                continue
            }
            indexLookup[vert] = size()
            this.vertices.add(vert)
        }

        edgeMatrix = Array(size()) { i ->
            IntArray(size()) { j ->
                edgeMatrix.getOrNull(i)?.getOrNull(j) ?: -1
            }
        }
        return failed
    }

    override fun removeAll(vertices: Collection<E>): Collection<E> {
        val failed = LinkedList<E>()
        //Marking vertices to remove + removing from hashmap
        val vertexToRemove = BooleanArray(size())
        for (vertex in vertices) {
            val id = indexLookup.remove(vertex)
            if (id == null) {
                failed.add(vertex)
                continue
            }
            vertexToRemove[id] = true
        }

        //Removing vertices from vertices list
        for (i in vertexToRemove.indices.reversed()) {
            if (vertexToRemove[i]) this.vertices.removeAt(i)

        }

        //New edge matrix with vertices removed
        val newEdgeMatrix = Array(size()) { IntArray(size()) { -1 } }
        var fromOffset = 0

        //Copy over edges to new edge matrix
        for (from in edgeMatrix.indices) {
            if (vertexToRemove[from])
                fromOffset++
            else {
                var toOffset = 0
                for (to in edgeMatrix.indices) {
                    if (vertexToRemove[to])
                        toOffset++
                    else
                        newEdgeMatrix[from - fromOffset][to - toOffset] = edgeMatrix[from][to]
                }
            }
        }
        edgeMatrix = newEdgeMatrix

        //Nuke dijkstra table
        dijkstraTables = null

        indexLookup.clear()
        for ((i, v) in this.vertices.withIndex()) {
            indexLookup[v] = i
        }

        return failed
    }

    override fun <R : Any> mapVertices(transform: (vertex: E) -> R): Graph<R> {
        val newLookupTable = HashMap<R, Int>()
        val newGraph = AMGraph<R>(
            vertices = ArrayList(vertices.mapIndexed { index, e ->
                transform(e).also { r ->
                    newLookupTable[r] = index
                }
            }),
            indexLookup = newLookupTable,
            edgeMatrix = edgeMatrix.map { it.clone() }.toTypedArray()
        )
        return newGraph
    }

    /**
     * Returns a string representation of the graph
     *
     * Note that vertices are not labeled however they appear in the same
     * order as [iterator] and [getVertices]
     */
    override fun toString(): String {
        val string = StringBuilder()
        for (destinations in edgeMatrix) {
            string.append("[")
            for (weight in destinations) {
                string.append("${weight.let { if (it == -1) " " else it }}][")
            }
            string.deleteRange(string.length - 2, string.length)
            string.append("]\n")
        }
        return string.toString()
    }

    override fun neighbors(source: E): Collection<E> {
        val vertexId = indexLookup[source]!!
        val neighbors = mutableListOf<E>()
        for (vert in vertices.indices) {
            if (getWithIndex(vertexId, vert) != null) neighbors.add(vertices[vert])
        }
        return neighbors
    }

    override fun countEdgesBetween(v1: E, v2: E): Int {
        var edges = 0
        get(v1, v2)?.let { edges++ }
        get(v2, v1)?.let { edges++ }
        return edges
    }

    override fun copy(): AMGraph<E> {
        return AMGraph(
            vertices.clone() as ArrayList<E>,
            indexLookup.clone() as HashMap<E, Int>,
            edgeMatrix.map { it.clone() }.toTypedArray()
        )
    }

    override fun subgraph(vertices: Collection<E>): AMGraph<E> {
        return subgraphFromIds(this.vertices.map { indexLookup[it]!! })
    }

    /**
     * Uses vertex id's for faster internal access to the edges.
     *
     * @see AMGraph.subgraph
     */
    private fun subgraphFromIds(verticesIds: Collection<Int>): AMGraph<E> {
        val verticesIds = verticesIds.sorted()
        val newVertices = ArrayList<E>(verticesIds.size)
        val newIndexLookup = HashMap<E, Int>()
        val isCopied = BooleanArray(vertices.size) { false }

        for ((newId, oldId) in verticesIds.withIndex()) {
            val vertex = vertices[oldId]
            newIndexLookup[vertex] = newId
            newVertices += vertex
            isCopied[oldId] = true
        }

        //New edge matrix with vertices removed
        val newEdgeMatrix = Array(newVertices.size) { IntArray(newVertices.size) { -1 } }
        var fromOffset = 0

        //Copy over edges to new edge matrix
        for (from in edgeMatrix.indices) {
            if (!isCopied[from]) {
                fromOffset++
                continue
            }

            var toOffset = 0
            for (to in edgeMatrix.indices) {
                if (!isCopied[to])
                    toOffset++
                else
                    newEdgeMatrix[from - fromOffset][to - toOffset] = edgeMatrix[from][to]
            }
        }
        return AMGraph(
            vertices = newVertices,
            indexLookup = newIndexLookup,
            edgeMatrix = newEdgeMatrix
        )
    }


    /*------------------ RANDOMIZATION ------------------*/

    override fun randomize(
        probability: Double,
        minWeight: Int,
        maxWeight: Int,
        allowDisjoint: Boolean,
        random: Random
    ) { //when removed add default values
        for (i in edgeMatrix.indices) {
            for (j in edgeMatrix.indices) {
                if (random.nextDouble() < probability) {
                    setWithIndex(i, j, random.nextInt(minWeight, maxWeight))
                } else {
                    setWithIndex(i, j, -1)
                }
            }
        }
        if (!allowDisjoint) mergeDisjoint(minWeight, maxWeight, random)
    }

    override fun mergeDisjoint(minWeight: Int, maxWeight: Int, random: Random) {
        val bidirectional = this.getBidirectionalUnweighted() as AMGraph<E>
        var vertex = random.nextInt(size())
        var unreachables: List<Int>

        //Runs while there are any unreachable vertices from `vertex` (store all the unreachable ones in `unreachables`)
        //Vertices --> Unreachable non-self vertices --> Unreachable non-self id's
        while (vertices.filterIndexed { id, _ -> id != vertex && bidirectional.path(vertex, id).isEmpty() }
                .map { indexLookup[it]!! }.also { unreachables = it }.isNotEmpty()) {
            val from: Int
            val to: Int
            val weight = random.nextInt(minWeight, maxWeight)
            if (random.nextBoolean()) {
                from = vertex
                to = unreachables.random(random)
            } else {
                from = unreachables.random(random)
                to = vertex
            }
            edgeMatrix[from][to] = weight
            bidirectional.setWithIndex(from, to, 1)
            bidirectional.setWithIndex(to, from, 1)

            vertex = random.nextInt(size())
            unreachables = emptyList()
        }
    }

    /*------------------ PATHING ------------------*/
    /*BFS and DFS */

    override fun depthFirstSearch(source: E, destination: E): List<E> {
        return dfsRecursive(
            indexLookup[source]!!,
            indexLookup[destination]!!,
            BooleanArray(size())
        ).map { vertices[it] }
    }

    override fun search(depth: Boolean, source: E, destination: E): List<E> {
        val dest = indexLookup[destination]!!
        val start = indexLookup[source]!!
        val q = LinkedList<Int>()
        val prev = IntArray(size()) { -1 }

        q.addFirst(start)
        prev[start] = -2

        while (!q.isEmpty()) {
            val curPath = q.pop()

            for ((ob, dist) in edgeMatrix[curPath].withIndex()) {

                if (prev[ob] < 0 && dist != -1 && ob != curPath) {
                    if (depth)
                        q.addFirst(ob)
                    else //breadth
                        q.addLast(ob)
                    prev[ob] = curPath
                    if (ob == dest) break
                }
            }
        }
        return LinkedList<E>().apply {
            if (prev[dest] == -1) return@apply

            var next: Int = dest
            while (next != start) {
                addFirst(vertices[next])
                next = prev[next]
            }
            addFirst(vertices[start])
        }
    }

    private fun dfsRecursive(src: Int, dest: Int, visited: BooleanArray): LinkedList<Int> {
        visited[src] = true

        for ((ob, dist) in edgeMatrix[src].withIndex()) {

            if (!visited[ob] && dist != -1) {
                if (ob == dest)
                    return LinkedList<Int>().apply { addFirst(ob); addFirst(src) }
                dfsRecursive(ob, dest, visited).let {
                    if (it.isNotEmpty())
                        return it.apply { addFirst(src) }
                }
            }
        }
        return LinkedList()
    }

    /* DIJKSTRA'S */
    override fun path(from: E, to: E): List<E> {
        return path(indexLookup[from]!!, indexLookup[to]!!).map { vertices[it] }
    }

    /**
     * Allows specification of [useArray] to use dijkstra's without a
     * fib heap which preforms slightly better, but is currently non-functional
     * on disjoint graphs.
     */
    fun path(from: E, to: E, useArray: Boolean): List<E> {
        return path(indexLookup[from]!!, indexLookup[to]!!, useArray).map { vertices[it] }
    }

    /**
     * Uses vertex id's for faster internal access to the edges
     *
     * @return A list of vertex id's representing the path between from and to
     * where [from] is the first element and [to] is the last. Returns an empty
     * list if there is no path
     *
     * @see AMGraph.path
     */
    private fun path(from: Int, to: Int, useArray: Boolean = true): List<Int> {
        return try {
            tracePath(
                from,
                to,
                getDijkstraTable(from, useArray)
            )
        } catch (e: IndexOutOfBoundsException) { //More nodes were added that are disjoint and not in cached tables (we know there's no path)
            emptyList()
        }
    }


    override fun distance(from: E, to: E): Int {
        val fromIndex = indexLookup[from]!!
        val toIndex = indexLookup[to]!!
        return (getDijkstraTable(fromIndex))[toIndex].second
    }

    /**
     * An implementation of Dijkstra's algorithm using a Fibonacci Heap as a
     * queue.
     *
     * Due to the nature of this graph using an adjacency matrix, this runs in
     * O(V^2*log(V)) time.
     */
    private fun dijkstraFibHeap(from: Int, to: Int? = null): Array<Pair<Int, Int>> {
        //Initialize each vertex's info mapped to ids
        val prev = IntArray(size()) { -1 }
        val dist = IntArray(size()) { Int.MAX_VALUE }
        dist[from] = 0

        //PriorityQueue storing Priority = dist, Value = id
        val heap = FibonacciHeap<Int, Int>()

        //Store Queue's nodes for easy search/updates
        val nodeCollection = Array<Node<Int, Int>?>(size()) { null }
        nodeCollection[from] = heap.insert(dist[from], from)

        //loop forever, or until we have visited to
        while (to == null || heap.minimum() == to) {

            //store and remove next node, mark as visited, break if empty
            val cur = heap.extractMin() ?: break

            //iterate through potential outbound connections
            for ((i, edge) in edgeMatrix[cur].withIndex()) {

                //relax all existing connections
                if (edge != -1
                    //table update required if it's the shortest path (so far)
                    && dist[cur] + edge < dist[i]
                ) {

                    //update
                    dist[i] = dist[cur] + edge
                    prev[i] = cur

                    //re-prioritize node or create and add it
                    if (nodeCollection[i] != null)
                        heap.decreaseKey(nodeCollection[i]!!, dist[i])
                    else nodeCollection[i] = heap.insert(dist[i], i)
                }
            }
        }
        return prev.zip(dist).toTypedArray()
    }

    /**
     * An implementation of Dijkstra's using looping rather than queues.
     */
    private fun dijkstraArr(from: Int, to: Int? = null): Array<Pair<Int, Int>> {
        val distance = IntArray(size()) { Int.MAX_VALUE }
        val prev = IntArray(size()) { -1 }
        val visited = BooleanArray(size()) { false }

        distance[from] = 0
        while (to == null || !visited[to]) {
            //Determine the next vertex to visit
            var currVert = visited.indexOfFirst { !it } //Finds first unvisited
            if (currVert == -1) break //All visited
            for (i in currVert + 1 until visited.size) {
                if (!visited[i] && distance[i] < distance[currVert]) {
                    currVert = i
                }
            }
            if (distance[currVert] == Int.MAX_VALUE) break //Only disjoint nodes left

            //Update distances and previous
            val currDist = distance[currVert]
            for ((i, edge) in edgeMatrix[currVert].withIndex()) {
                if (!visited[i] && edge != -1 && currDist + edge < distance[i]) {
                    distance[i] = (currDist + edgeMatrix[currVert][i])
                    prev[i] = currVert
                }
            }
            //Update visited
            visited[currVert] = true
        }
        return prev.zip(distance).toTypedArray() //funky function
    }

    /**
     * Attempts to retrieve the dijkstra's table from the
     * [cache][dijkstraTables]. If it is not cached, it will create and cache
     * it using [dijkstraFibHeap].
     *
     * @return The table retrieved from [dijkstraTables]
     */
    private fun getDijkstraTable(fromIndex: Int, useArray: Boolean = false): Array<Pair<Int, Int>> {
        if (dijkstraTables == null) dijkstraTables = Array(size()) { null }
        if (dijkstraTables!![fromIndex] == null) dijkstraTables!![fromIndex] =
            if (useArray) dijkstraArr(fromIndex) else dijkstraFibHeap(fromIndex)
        return dijkstraTables!![fromIndex]!!
    }

    /**
     *  Iterates backwards through an entry in the given [dijkstraTable] to
     *  create a list of the path between the 2 vertices.
     *
     *  @param from the vertex to trace the path
     *  @param to the vertex to trace the path to
     *
     *  @return A list of vertex id's corresponding to the path between [from]
     *  and [to] according to this [dijkstraTable]
     */
    private fun tracePath(from: Int, to: Int, dijkstraTable: Array<Pair<Int, Int>>): List<Int> {
        val path = LinkedList<Int>()
        path.add(to)
        var curr = to
        while (path.firstOrNull() != from) {
            path.addFirst(dijkstraTable[curr].run { curr = first; first })
            if (path[0] == -1) {
                path.removeFirst(); break
            }
        }
        return if (path.first() == from) path else emptyList()
    }

    override fun getConnected(source: E): List<E> {
        return getConnected(indexLookup[source]!!).map { vertices[it] }
    }

    /**
     * Uses vertex id's for faster internal access to the edges.
     *
     * @see AMGraph.getConnected
     */
    private fun getConnected(vertex: Int): List<Int> {
        val connected = ArrayList<Int>()
        for (id in vertices.indices) {
            if (id == vertex || path(id, vertex).isNotEmpty())
                connected.add(id)
        }
        return connected
    }

    /*------------------ COLORING ------------------*/
    @Deprecated("This algorithm was never finished and should not be used", level = DeprecationLevel.HIDDEN)
    fun color(maxColors: Int): Array<List<E>>? {
        require(maxColors > 0)
        val max = BigInteger(maxColors.toString()).pow(size() + 1)
        var colors = BigInteger.ZERO //storing this array as an int for FANCY iteration

        fun getColor(index: Int): Int {
            return colors.divide(maxColors.toDouble().pow(index.toDouble()).toInt().toBigInteger())
                .mod(maxColors.toBigInteger()).toInt()
        }

        fun check(): Boolean {
            for (vert in vertices) {
                for ((ob, w) in edgeMatrix[indexLookup[vert]!!].withIndex()) {
                    if (w != -1 && getColor(ob) == getColor(indexLookup[vert]!!)) {
                        return false
                    }
                }
            }
            return true
        }

        while (!check()) {
            if (colors == max) return null
            colors = colors.plus(BigInteger.ONE)
        }

        return ArrayList<ArrayList<E>>().apply {
            for (vert in vertices.indices) {
                while (getColor(vert) >= size()) {
                    add(ArrayList())
                }
                this[getColor(vert)].addLast(vertices[vert])
            }
        }.toTypedArray()
    }

    /*------------------ CLUSTERING ------------------*/
    override fun highlyConnectedSubgraphs(connectedness: Double, kargerness: Int): Collection<AMGraph<E>> {
        val minCut = karger(kargerness)
        //check if minCut size is acceptable or there's no cut (ie there's only 1 node in the graph)
        if (minCut.size >= connectedness * size() || minCut.size == -1) return listOf(this)

        val clusters = ArrayList<AMGraph<E>>()
        val subgraph1 = subgraphFromIds(minCut.cluster1)
        val subgraph2 = subgraphFromIds(minCut.cluster2)

        clusters.addAll(subgraph1.highlyConnectedSubgraphs(connectedness, kargerness))
        clusters.addAll(subgraph2.highlyConnectedSubgraphs(connectedness, kargerness))

        return clusters
    }

    /**
     * Returns the minimum cut of this graph using Karger's Algorithm—a randomized algorithm.
     *
     * @param numAttempts the number of attempts of [minCut] before picking the
     *        best one
     *
     * @return the smallest cut found.
     */
    private fun karger(numAttempts: Int): Cut {
        var bestCut = minCut()

        repeat(numAttempts - 1) {
            if (Thread.currentThread().isInterrupted) throw InterruptedException()
            bestCut = minCut().takeIf {
                it < bestCut
            } ?: bestCut
        }

        return bestCut
    }

    /**
     * Represents a cut of the graph, dividing it into two subgraphs or clusters.
     * Is [Comparable] based on its desirability as a minCut (lower is better).
     *
     * @param size the number of cuts necessary to separate the minCut
     * @param cluster1 the first cluster created by the cut
     * @param cluster2 the second cluster created by the cut
     */
    private data class Cut(val size: Int, val cluster1: Collection<Int>, val cluster2: Collection<Int>) :
        Comparable<Cut> {
        /**
         * @return the smallest cluster of this cut
         */
        fun minCluster() = min(cluster1.size, cluster2.size)
        override fun compareTo(other: Cut): Int {
            return (this.size - other.size).let {
                if (it == 0) other.minCluster() - this.minCluster()
                else it
            }
        }
    }

    /**
     *  Repeatedly contracts random edges in the graph until only two nodes
     *  remain, and then calculates the cut between those two nodes.
     *
     *  @return the [Cut] found by randomly collapsing edges
     */
    private fun minCut(): Cut {
        //'from' > 'to' in edges
        var edges: MutableList<Pair<Int, Int>> = ArrayList()
        //Initializing edges from edge-matrix, triangular to ensure from > to
        for (from in 1 until size()) {
            for (to in 0 until from) {
                if (edgeMatrix[from][to] > -1) edges.add(from to to)
                if (edgeMatrix[to][from] > -1) edges.add(from to to)
            }
        }

        //If the nodes index contains a list whose last value that is not itself, it is a reference
        //If a node contains a list with last()==itself, it is a cluster head
        val nodeRedirection = Array(size()) { LinkedList<Int>().apply { add(it) } }

        //Navigates through references until it finds the redirected value
        fun getLinkedCluster(node: Int): List<Int> {
            if (nodeRedirection[node].last() == node) return nodeRedirection[node]
            return getLinkedCluster(nodeRedirection[node].last())
        }

        var numNodes = size()

        //Randomize edge list
        randomizeList(edges) //randomize prefers an array list
        edges = LinkedList(edges) //turn into a queue so we can pop

        //Delete the next edge and merge the two vertices (or clusters) into one
        fun collapse() {
            val edge = edges.pop()
            //from and to are the merged values (if they've previously been merged)
            val cluster1 = getLinkedCluster(edge.first)
            val cluster2 = getLinkedCluster(edge.second)

            if (cluster1 == cluster2) return //If both nodes are in the same cluster, do nothing

            //Redirect the cluster with the smaller max node into the cluster with the larger max node
            val bigHead = maxOf(cluster1.last(), cluster2.last())
            val smallHead = minOf(cluster1.last(), cluster2.last())

            nodeRedirection[bigHead].addAll(
                0,
                nodeRedirection[smallHead]
            ) //Merge smallHead's cluster into bigHead's cluster
            nodeRedirection[smallHead] = LinkedList<Int>().apply { add(bigHead) } //Make smallHead redirect to bigHead

            //Finished collapsing 2 clusters into 1
            numNodes--
        }

        fun getClusters(): List<LinkedList<Int>> = nodeRedirection.filterIndexed { id, cluster -> cluster.last() == id }

        fun getCut(): Cut {
            require(numNodes == 2)
            val clusters = getClusters()

            //count from all possible connections
            var edgesToCut = 0
            for (from in clusters[0]) {
                for (to in clusters[1]) {
                    if (edgeMatrix[from][to] > -1) edgesToCut++
                    if (edgeMatrix[to][from] > -1) edgesToCut++
                }
            }
            return Cut(edgesToCut, clusters[0], clusters[1])
        }

        //we cut the connections when we've collapsed everything into 2 nodes
        while (numNodes > 2 && edges.isNotEmpty()) {
            collapse()
        }
        return when (numNodes) {
            1 -> Cut(-1, emptyList(), emptyList()) //SINGLE NODE CASE
            2 -> getCut() //REGULAR CASE
            else -> {//DISJOINT CASE
                val clusters = getClusters()
                val cluster1 = clusters.subList(0, clusters.size / 2).flatten()
                val cluster2 = clusters.subList(clusters.size / 2, clusters.size).flatten()
                Cut(0, cluster1, cluster2)
            }
        }
    }

    /**
     * Randomizes the given list
     *
     * @param list the list to randomize. The list itself is modified and thus a new.
     */
    private fun <T> randomizeList(list: MutableList<T>) {
        fun swap(index1: Int, index2: Int) {
            list[index1] = list.set(index2, list[index1])
        }

        for (i in list.indices) {
            swap(i, Random.nextInt(i, list.size))
        }
    }
}
