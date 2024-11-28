package com.fischerabruzese.graphsFX

import com.fischerabruzese.graph.Graph
import com.fischerabruzese.graphsFX.machines.*
import javafx.application.Platform
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.math.*
import kotlin.random.Random

/**
 * Represents all the components of a graph in the graphics pane
 * @param initialGraph graph contained in [pane]
 * @param pane the pane to contain the graph
 */
class GraphicComponents<E : Any>(
    initialGraph: Graph<E>,
    val pane: Pane,
) {
    //TODO: ADD EVERY VERTEX TO LOOKUP TABLE



    //COLORING
    private fun grey(color: Color?): Color? {
        val greyFactor = 0.5
        if (color == null) return null
        val newColor = Color.color(
            (color.red + ((Color.LIGHTGREY.red - color.red) * greyFactor)),
            (color.green + ((Color.LIGHTGREY.green - color.green) * greyFactor)),
            (color.blue + ((Color.LIGHTGREY.blue - color.blue) * greyFactor))
        )

        return Color(newColor.red, newColor.green, newColor.blue, 0.3)
    }

    /**
     * Update colors when the vertex is selected
     */
    private fun setSelected() {
        setColor(ColorType.SELECTED)
        clearColor(ColorType.GREYED)
    }

    /**
     * Update colors when the vertex is hovered
     */
    private fun setHovered() {
        setColor(ColorType.HOVERED)
        clearColor(ColorType.GREYED)
    }

    /**
     * Clears the color of a certain [ColorType]
     * @param type The [ColorType] to clear
     */
    fun clearColor(type: ColorType) {
        val priority = colorPriorityMap[type]!!
        colorStorage[priority] = null
        if (priority == currentColorPriority) {
            val newGreatestPriority = highestActivePriority()
            setColor(colorStorage[newGreatestPriority]!!)
            currentColorPriority = newGreatestPriority
        }
    }

    /*Coloring*/
    private val colorPriorityMap = hashMapOf(
        ColorType.PATH to 0,
        ColorType.SELECTED to 1,
        ColorType.HOVERED to 2,
        ColorType.GREYED to 3,
        ColorType.CLUSTERED to 4,
        ColorType.DEFAULT to 5
    )

    //The colors are stored via priority, where when one color is deactivated it will fall through until it's a colored field is met
    private val colorStorage = Array<Color?>(colorPriorityMap.size) { null }.apply {
        this[colorPriorityMap[ColorType.DEFAULT]!!] = Color.BLUE
    }

    /**
     * The priority of the currently active color (lower number = higher priority)
     */
    private var currentColorPriority = colorPriorityMap[ColorType.DEFAULT]!!

    /**
     * Equivalent to the highest priority color that isn't null.
     * @return The priority of the current color.
     */
    private fun highestActivePriority(): Int {
        colorStorage.forEachIndexed { index, color ->
            if (color != null) return index
        }
        return colorPriorityMap[ColorType.DEFAULT]!!
    }

    /**
     * Change the color of a certain [ColorType]
     * @param type The [ColorType] to change
     * @param color The new color. [ColorType.PATH] is the only type
     */
    fun setColor(type: ColorType, color: Color? = null) {
        val priority = colorPriorityMap[type]!!

        //Update color storage to new color
        colorStorage[priority] = when (type) {
            ColorType.PATH -> color
            ColorType.SELECTED -> Color.RED
            ColorType.HOVERED -> Color.GREEN
            ColorType.GREYED -> grey(colorStorage[highestActivePriority()])
            ColorType.CLUSTERED -> color
                .also { if (colorStorage[colorPriorityMap[ColorType.GREYED]!!] != null) setColor(ColorType.GREYED) } //if grey is active, update it to a new grey based on this color
            ColorType.DEFAULT -> Color.BLUE
                .also { if (colorStorage[colorPriorityMap[ColorType.GREYED]!!] != null) setColor(ColorType.GREYED) } //if grey is active, update it to a new grey based on this color
        }

        //Decide if this color is the new active color
        if (priority <= currentColorPriority) {
            setColor(colorStorage[priority]!!)
            currentColorPriority = priority
        }
    }


    //DRAGGING
    private var xDelta: Double = 0.0
    private var yDelta: Double = 0.0
    internal var draggingFlag = false

    /**
     * Stores the mouse position on the vertex so that it doesn't matter where you click on the vertex
     */
    private fun dragStart(event: MouseEvent) {
        xDelta = event.sceneX / pane.width - x.get()
        yDelta = event.sceneY / pane.height - y.get()
    }

    /**
     * Updates the location of the vertex that's being dragged
     */
    private fun drag(event: MouseEvent) {
        x.set((event.sceneX / pane.width - xDelta).let { if (it > 1) 1.0 else if (it < 0) 0.0 else it })
        y.set((event.sceneY / pane.height - yDelta).let { if (it > 1) 1.0 else if (it < 0) 0.0 else it })
    }

    //HITBOX SETTERS

    private fun setHitboxListeners() {
        hitbox.setOnMouseEntered { if (currentPathVertices.isEmpty()) setHovered() }
        hitbox.setOnMouseExited { if (currentPathVertices.isEmpty()) clearColor(ColorType.HOVERED) }

        hitbox.setOnMousePressed {
            draggingFlag = true
            dragStart(it)
            selectedVertex = this
            if (!currentPathVertices.contains(this)) {
                ungreyEverything()
                currentPathVertices.clear()
                currentPathConnections.clear()
                greyDetached(this)
                setSelected()
            }
        }
        hitbox.setOnMouseDragged { drag(it) }
        hitbox.setOnMouseReleased {
            clearColor(ColorType.SELECTED)
            if (!currentPathVertices.contains(this)) {
                ungreyEverything()
            }
            selectedVertex = null
            draggingFlag = false
        }
        hitbox.pickOnBoundsProperty().set(true)

        hitboxes.add(hitbox)
    }




    //Constants
    companion object {
        private const val DEFAULT_CIRCLE_RADIUS = 20.0
    }

    //Vertex + edge storage
    private val clusterMachine: IClusterMachine = TODO()
    private val colorMachine: IColorMachine = TODO()
    private val dragMachine: IDragMachine<E> = TODO()
    private val pathMachine: IPathMachine<E> = TODO()
    private val physicsMachine: IPhysicsMachine<E> = TODO()

    private var fxGraph: FXGraph<E> = TODO()
    private var graph: Graph<E> = initialGraph.also { syncFXGraphTo(initialGraph) }

    private fun syncFXGraphTo(graph: Graph<E>) {
        pane.children.clear()

        fxGraph.clear()

        for(element in graph.getVertices()) {
            addVertex(element, DEFAULT_CIRCLE_RADIUS, Random.nextDouble(), Random.nextDouble())
        }
    }






    fun hideWeights() = fxGraph.hideWeights()

    fun showWeights() = fxGraph.showWeights()

    fun




    /**
     * Given [clusters] colors the graph so that each cluster is a distinct color
     */

}