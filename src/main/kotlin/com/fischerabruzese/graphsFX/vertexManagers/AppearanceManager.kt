package com.fischerabruzese.graphsFX.vertexManagers

import com.fischerabruzese.graphsFX.FXGraph
import javafx.scene.layout.Pane
import javafx.scene.paint.Color

class AppearanceManager(val fxGraph: FXGraph<*>) {
    //COLORING
    /**
     * The priority of the currently active color (lower number = higher priority)
     */
    private var currentColorPriority = priority(ColorType.DEFAULT)

    enum class ColorType {
        PATH, SELECTED, HOVERED, GREYED, CLUSTERED, DEFAULT
    }

    private fun priority(colorType: ColorType): Int{
        return colorType.ordinal
    }

    private fun grey(color: Color?, greyFactor: Double = 0.5): Color? {
        if (color == null) return null
        val newColor = Color.color(
            (color.red + ((Color.LIGHTGREY.red - color.red) * greyFactor)),
            (color.green + ((Color.LIGHTGREY.green - color.green) * greyFactor)),
            (color.blue + ((Color.LIGHTGREY.blue - color.blue) * greyFactor))
        )

        return Color(newColor.red, newColor.green, newColor.blue, 0.3)
    }

    /**
     * Clears the color of a certain [ColorType]
     * @param type The [ColorType] to clear
     */
    fun clearColor(type: ColorType) {
        val priority = priority(type)
        colorStorage[priority] = null
        if (priority == currentColorPriority) {
            val newGreatestPriority = highestActivePriority()
            setColor(colorStorage[newGreatestPriority]!!)
            currentColorPriority = newGreatestPriority
        }
    }

    //The colors are stored via priority, where when one color is deactivated it will fall through until it's a colored field is met
    private val colorStorage = Array<Color?>(colorPriorityMap.size) { null }.apply {
        this[colorPriorityMap[ColorType.DEFAULT]!!] = Color.BLUE
    }

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
}