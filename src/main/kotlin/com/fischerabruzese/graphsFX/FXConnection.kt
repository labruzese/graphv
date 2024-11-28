package com.fischerabruzese.graphsFX

import javafx.beans.binding.Bindings
import javafx.scene.control.Label
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.Line
import javafx.scene.text.Font
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Represents the connection of two vertices in direction, from -> to
 * @param from the origin of the connection
 * @param to the designation of the connection
 * @param weight the weight of the connection
 * @param mirrored when creating 2 connections one of them should be mirrored so that they don't attempt to draw themselves in the same place.
 */
class FXConnection<E>(val from: FXVertex<E>, val to: FXVertex<E>, weight: Int, val mirrored: Boolean) : Pane() {
    /*Components*/
    private val line = Line()

    private var director1: FXDirector<E>
    private var director2: FXDirector<E>
    private var label = Label(weight.toString())

    init {
        //Create bindings of the displacement between and to on the screen
        val dyTotal = to.vTranslateYBinding.subtract(from.vTranslateYBinding)
        val dxTotal = to.vTranslateXBinding.subtract(from.vTranslateXBinding)

        //Create a binding of the distance between the 2 vertices
        val length = Bindings.createDoubleBinding(
            { sqrt(dyTotal.get().pow(2) + dxTotal.get().pow(2)) },
            dyTotal, dxTotal
        )

        //Calculate a displacement from the center of the vertex to map the start and end points to
        val fromDy = dxTotal.multiply(from.radius / 4).divide(length).multiply(-1)
        val fromDx = dyTotal.multiply(from.radius / 4).divide(length)
        val toDy = dxTotal.multiply(to.radius / 4).divide(length).multiply(-1)
        val toDx = dyTotal.multiply(to.radius / 4).divide(length)

        //Bind the start and end properties
        line.startXProperty().bind(from.vTranslateXBinding.add(fromDx))
        line.startYProperty().bind(from.vTranslateYBinding.add(fromDy))
        line.endXProperty().bind(to.vTranslateXBinding.add(toDx))
        line.endYProperty().bind(to.vTranslateYBinding.add(toDy))

        //Create directions to place on the connection, one at 1/3 of the line and one at 2/3 of the line
        director1 = FXDirector(
            this,
            line.startXProperty().add(dxTotal.multiply(0.33)),
            line.startYProperty().add(dyTotal.multiply(0.33)),
            mirrored
        )
        director2 = FXDirector(
            this,
            line.startXProperty().add(dxTotal.multiply(0.66)),
            line.startYProperty().add(dyTotal.multiply(0.66)),
            mirrored
        )

        //Sets the label to the average of the line endpoints plus some offsets to ensure the label is centered
        label.translateXProperty().bind((line.startXProperty().add(line.endXProperty())).divide(2).subtract(5))
        label.translateYProperty().bind((line.startYProperty().add(line.endYProperty())).divide(2).subtract(10))

        label.textFill = Color.BLACK
        label.font = Font(15.0)

        children.addAll(line, label, director1, director2)
    }

    fun setLineColor(color: Color) {
        line.stroke = color
        director1.setColor(color)
        director2.setColor(color)
    }

    fun boldLine() {
        line.strokeWidth = 3.0
        director1.boldLine()
        director2.boldLine()
    }

    fun unboldLine() {
        line.strokeWidth = 1.0
        director1.unboldLine()
        director2.unboldLine()
    }

    fun setLabelColor(color: Color) {
        label.textFill = color
    }

    fun hideLabel() {
        label.isVisible = false
    }

    fun showLabel() {
        label.isVisible = true
    }

    fun setWeight(weight: String) {
        label.text = weight
    }
}