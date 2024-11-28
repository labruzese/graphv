package com.fischerabruzese.graphsFX

import com.fischerabruzese.graph.AMGraph
import com.fischerabruzese.graph.Graph
import javafx.scene.paint.Color
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.random.Random

class FXGraph<E : Any>(initial: Graph<E> = AMGraph()) {
    val graph = object : AMGraph<E>() {
        init {
            becomeCloneOf(initial)
        }

        private val itemHashtable = LinkedHashMap<Pair<E,E>, FXConnection<E>>()

        fun getConnectionItem(edge: Pair<E, E>) : FXConnection<E>? { return itemHashtable[edge] }
        fun getConnectionItem(from: E, to: E) : FXConnection<E>? { return itemHashtable[from to to] }
    }

    companion object {
        const val DEFAULT_RADIUS = 20.0
    }

    init {
        for (vertex in initialGraph) {
            addVertex(vertex, DEFAULT_RADIUS, Random.nextDouble(), Random.nextDouble())
        }

        for ((from, to) in initialGraph.getEdges()) {
            addConnection(vertices[from]!!, vertices[to]!!, initialGraph[from, to]!!)
        }
    }

    val vertices: MutableMap<E, FXVertex<E>> = mutableMapOf()
    val edges: MutableList<FXEdge<E>> = mutableListOf()

    fun clear() {
        vertices.clear()
    }


    private fun addVertex(element: E, radius: Double, xPos: Double, yPos: Double) {
        //Add bindings to vertex
        //Add to FXGraph
        //add vertex to pane
    }

    private fun addConnection(from: FXVertex<E>, to: FXVertex<E>, weight: Int) {
        val reversedEdge = findReverseEdge(from to to)

        val newEdge =
            if(reversedEdge != null) {
                FXEdge(reversedEdge.v1tov2Connection!!, FXConnection(to, from, weight, !reversedEdge.v1tov2Connection.mirrored))
            } else {
                FXEdge(from, to, weight, null)
            }

        if(reversedEdge != null) edges.remove(reversedEdge)

        edges.add(newEdge)
    }


    fun findReverseEdge(connection: Pair<FXVertex<E>, FXVertex<E>>): FXEdge<E>? {
        return edges.find {
            it.v2 == connection.first && it.v1 == connection.second
        }
    }

    fun hideWeights() {
        for (edge in edges) {
            edge.hideLabels()
        }
    }

    fun showWeights() {
        for (edge in edges) {
            edge.showLabels()
        }
    }

    /* COLORING */

    fun grey() {
        for (edge in edges) {
            edge.grey() //TODO make a method to change the color of edges like this
        }
        for (vert in vertices) {
            vert.setColor(ColorType.GREYED)
        }
    }

    fun greyDetached(src: GraphicComponents<E>.Vertex) {
        for (vert in vertices.filterNot { it == src }) {
            vert.setColor(ColorType.GREYED)
        }
        for (edge in edges) {
            if (edge.v1 != src && edge.v2 != src) {
                edge.grey()
            } else {
                edge.v1.let { if (it != src) it.clearColor(ColorType.GREYED) }
                edge.v2.let { if (it != src) it.clearColor(ColorType.GREYED) }
                edge.setLineColor(Color.GREEN, Color.RED, src)
                edge.setLabelColor(Color.GREEN, Color.RED, src)
            }
        }
    }

    fun ungrey() {
        for (edge in edges) {
            edge.ungrey()
        }
        for (vert in vertices) {
            vert.clearColor(ColorType.GREYED)
            vert.clearColor(ColorType.PATH)
        }
    }

    /**
     * Given [path] grey stuff no in path and make the path fancy. And add path to [currentPathVertices] and [currentPathConnections]
     */
    fun colorPath(path: List<Any>) {
        currentPathVertices.clear()
        for (v in path) {
            for (vertex in vertices) {
                if (vertex.v == v)
                    currentPathVertices.add(vertex)
            }
        }

        currentPathConnections.clear()
        for ((v1, v2) in currentPathVertices.dropLast(1).zip(currentPathVertices.drop(1))) {
            for (edge in edges) {
                if (edge.v1 == v1 && edge.v2 == v2) {
                    currentPathConnections.addLast(edge.v1tov2Connection)
                    break
                } else if (edge.v1 == v2 && edge.v2 == v1) {
                    currentPathConnections.addLast(edge.v2tov1Connection)
                    break
                }
            }
        }

        greyEverything()
        makePathFancyColors()
    }

    /**
     * Creates a gradient for the current [currentPathVertices] and [currentPathConnections]
     */
    private fun makePathFancyColors() {
        val startColor = Controller.PATH_START
        val endColor = Controller.PATH_END
        val segments: Double = currentPathVertices.size + currentPathConnections.size.toDouble()
        var currColor = startColor
        val connections = LinkedList(currentPathConnections)
        val verts = LinkedList(currentPathVertices)

        while (!verts.isEmpty()) {
            val vert = verts.removeFirst()
            if (verts.isEmpty()) vert.setColor(ColorType.PATH, endColor) else vert.setColor(ColorType.PATH, currColor)
            currColor = Color.color(
                (currColor.red + ((endColor.red - startColor.red) / segments)),
                (currColor.green + ((endColor.green - startColor.green) / segments)),
                (currColor.blue + ((endColor.blue - startColor.blue) / segments))
            )

            if (!connections.isEmpty()) {
                val connection = connections.removeFirst()
                connection.setLineColor(currColor)
                connection.boldLine()
                currColor = Color.color(
                    (currColor.red + ((endColor.red - startColor.red) / segments)),
                    (currColor.green + ((endColor.green - startColor.green) / segments)),
                    (currColor.blue + ((endColor.blue - startColor.blue) / segments))
                )
            }
        }
    }

    fun colorClusters(clusters: Collection<Graph<Any>>) {
        fun randomColor(): Color = Color.color(Math.random(), Math.random(), Math.random())
        val colors = LinkedList(
            listOf(
                Color.rgb(148, 0, 211), // Deep Purple
                Color.rgb(102, 205, 170), // Aquamarine
                Color.rgb(218, 165, 32), // Goldenrod
                Color.rgb(0, 191, 255), // Deep Sky Blue
                Color.rgb(218, 112, 214), // Orchid
                Color.rgb(154, 205, 50), // Yellow Green
                Color.rgb(255, 69, 0), // Orange Red
                Color.rgb(139, 69, 19), // Saddle Brown
                Color.rgb(176, 224, 230), // Powder Blue
                Color.rgb(255, 105, 180), // Hot Pink
                Color.rgb(70, 130, 180), // Steel Blue
                Color.rgb(0, 128, 128), // Teal
                Color.rgb(255, 99, 71), // Tomato
                Color.rgb(255, 215, 0), // Gold
                Color.rgb(255, 160, 122) // Light Salmon
            )
        )

        //Sorted first by minSize, then by lexicographic order of the minimum toString of the vertices
        val sortedClusters = clusters.sortedBy { cluster -> cluster.getVertices().minOfOrNull { it.toString() } }
            .sortedByDescending { it.size() }
        for (cluster in sortedClusters) {
            val color = if (colors.isNotEmpty()) colors.removeFirst() else randomColor()
            for (vertex in cluster) {
                stringToVMap[vertex.toString()]?.setColor(ColorType.CLUSTERED, color)
            }
        }
    }

    fun clearClusterColoring() {
        for (vertex in vertices) {
            vertex.clearColor(ColorType.CLUSTERED)
        }
    }
}