package com.fischerabruzese.graphsFX

import com.fischerabruzese.graph.AMGraph
import com.fischerabruzese.graph.Graph
import com.fischerabruzese.graphsFX.graphMachines.*
import com.fischerabruzese.graphsFX.vertexManagers.*
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import java.util.*
import kotlin.collections.HashMap
import kotlin.random.Random

//type of node is V
//mutable
class FXGraph<V : Any>(initial: Graph<V> = AMGraph(), val pane: Pane) {
    val graph: Graph<FXVertex<V>> = AMGraph()
    var fxConnections = HashMap<Pair<FXVertex<V>, FXVertex<V>>, FXConnection<V>>()
    val vertex = HashMap<String, FXVertex<V>>()

    private var clusterMachine: ClusterMachine = ClusterMachine(appearanceManager)
    private var mouseMachine: MouseMachine = MouseMachine(appearanceManager, positionManager)
    private var pathMachine: PathMachine = PathMachine(appearanceManager)
    private var physicsMachine: PhysicsMachine = PhysicsMachine(positionManager)

    private var appearanceManager: AppearanceManager = AppearanceManager(this)
    private var positionManager: PositionManager = PositionManager(this)

    companion object {
        const val DEFAULT_RADIUS = 20.0
    }

    init {
        //Init vertices
        val vertexToFXVertex = HashMap<V, FXVertex<V>>()
        for(v: V in initial.getVertices()){
            vertexToFXVertex[v] = addVertex(v)
        }

        //Init edges
        for((from, to) in initial.getEdges()){
            val fxFrom = vertexToFXVertex[from]!!
            val fxTo = vertexToFXVertex[to]!!

            addConnection(fxFrom, fxTo, initial[from,to]!!)
        }
    }

    private fun addVertex(element: V,
                          radius: Double = DEFAULT_RADIUS,
                          xPos: Double = Random.nextDouble(),
                          yPos: Double = Random.nextDouble()
    ): FXVertex<V> {
        val vert = FXVertex(element, xPos, yPos, radius, pane)
        graph.add(vert)
        pane.children.add(vert)
        vertex[vert.toString()] = vert

        //Do bindings
        val hitbox = vert.hitbox
        hitbox.setOnMouseEntered {
            mouseMachine.mouseEntered(vert)
        }
        hitbox.setOnMouseExited {
            mouseMachine.mouseExited(vert)
        }

        hitbox.setOnMousePressed {
            mouseMachine.mousePressed(vert)
            pathMachine.mousePressed(vert)
        }

        hitbox.setOnMouseDragged(mouseMachine::mouseDragged)

        hitbox.setOnMouseReleased {
            mouseMachine.mouseReleased()
            pathMachine.mouseReleased()
        }
        return vert
    }

    private fun addConnection(from: FXVertex<V>, to: FXVertex<V>, weight: Int) {
        fxConnections[from to to] = FXConnection(from, to, weight, !(fxConnections[to to from]?.mirrored ?: true))
        graph[from, to] = weight
    }

    fun hideWeights() {
        for (connection in fxConnections.values) {
            connection.hideLabel()
        }
    }

    fun showWeights() {
        for (connection in fxConnections.values) {
            connection.showLabel()
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

    fun greyDetached(src: GraphicComponents<V>.Vertex) {
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