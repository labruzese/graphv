package com.fischerabruzese.graphsFX

import javafx.beans.binding.Bindings
import javafx.beans.binding.DoubleBinding
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.Line
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * A chevron placed on the line of a connection indicating direction
 * @param posX the x location of the chevron's tip
 * @param posY the y location of the chevron's tip
 * @param mirror determines the direction of the chevron
 */
class FXDirector<E>(val parentConnection: FXConnection<E>, posX: DoubleBinding, posY: DoubleBinding, mirror: Boolean) : Pane() {
    //The 2 lines of the chevron
    private val line1 = Line()
    private val line2 = Line()

    init {
        //Bind start positions
        line1.startXProperty().bind(posX)
        line1.startYProperty().bind(posY)
        line2.startXProperty().bind(posX)
        line2.startYProperty().bind(posY)

        //Calculate end positions
        val dyTotal = parentConnection.to.vTranslateYBinding.subtract(parentConnection.from.vTranslateYBinding)
        val dxTotal = parentConnection.to.vTranslateXBinding.subtract(parentConnection.from.vTranslateXBinding)

        val theta = Bindings.createDoubleBinding(
            { atan2(dyTotal.get(), dxTotal.get()) },
            dyTotal, dxTotal
        )

        val dx1 = Bindings.createDoubleBinding(
            { parentConnection.from.radius / 4.8 * cos(theta.get() + (PI / 4)) },
            theta
        )
        val dy1 = Bindings.createDoubleBinding(
            { parentConnection.from.radius / 4.8 * sin(theta.get() + (PI / 4)) },
            theta
        )
        val dx2 = Bindings.createDoubleBinding(
            { parentConnection.to.radius / 4.8 * cos(theta.get() - (PI / 4)) },
            theta
        )
        val dy2 = Bindings.createDoubleBinding(
            { parentConnection.to.radius / 4.8 * sin(theta.get() - (PI / 4)) },
            theta
        )

        val endX1 = posX.add(dx1.multiply(if (mirror) -1 else 1))
        val endY1 = posY.add(dy1.multiply(if (mirror) -1 else 1))
        val endX2 = posX.add(dx2.multiply(if (mirror) -1 else 1))
        val endY2 = posY.add(dy2.multiply(if (mirror) -1 else 1))

        //Bind end positions
        line1.endXProperty().bind(endX1)
        line1.endYProperty().bind(endY1)
        line2.endXProperty().bind(endX2)
        line2.endYProperty().bind(endY2)

        children.addAll(line1, line2)
    }

    fun setColor(color: Color) {
        line1.stroke = color
        line2.stroke = color
    }

    fun boldLine() {
        line1.strokeWidth = 3.0
        line2.strokeWidth = 3.0
    }

    fun unboldLine() {
        line1.strokeWidth = 1.0
        line2.strokeWidth = 1.0
    }
}