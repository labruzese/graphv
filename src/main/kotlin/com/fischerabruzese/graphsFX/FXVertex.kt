package com.fischerabruzese.graphsFX

import javafx.beans.binding.Bindings
import javafx.beans.binding.DoubleBinding
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.control.Label
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.StrokeType

class FXVertex<E>(val v: E, xInit: Double, yInit: Double, var radius: Double, val parentPane: Pane) : StackPane() {
    /*Position*/
    //x and y are between 0 and 1, everything should be modified in terms of these
    internal var x: DoubleProperty = SimpleDoubleProperty(xInit)
    internal var y: DoubleProperty = SimpleDoubleProperty(yInit)

    var lockPosition: Boolean = false
    //The current position of the vertex
    internal var pos
        get() = Position(x.get(), y.get())
        set(value) {
            if(lockPosition) return
            x.set(value.x)
            y.set(value.y)
        }

    //Graphical Display Components
    private val circle = Circle(radius, Color.BLUE)
    private val label = Label(v.toString())

    /**
     * Captures all inputs
     */
    val hitbox = Circle(radius, Color.TRANSPARENT)

    /*Location Bindings*/
    private val usablePercentPaneWidth: DoubleBinding = Bindings.createDoubleBinding(
        { 1.0 - 2 * radius / parentPane.widthProperty().get() },
        parentPane.widthProperty()
    )
    private val usablePercentPaneHeight: DoubleBinding = Bindings.createDoubleBinding(
        { 1.0 - 2 * radius / parentPane.heightProperty().get() },
        parentPane.heightProperty()
    )

    //These are actually what get read by the components
    var vTranslateXBinding: DoubleBinding =
        parentPane.widthProperty().multiply(this.x).multiply(usablePercentPaneWidth).add(radius)
    var vTranslateYBinding: DoubleBinding =
        parentPane.heightProperty().multiply(this.y).multiply(usablePercentPaneHeight).add(radius)

    private fun bindProperties() {
        //Circle
        val offsetBinding: DoubleBinding = circle.strokeWidthProperty().add(radius)
        circle.translateXProperty().bind(vTranslateXBinding.subtract(offsetBinding))
        circle.translateYProperty().bind(vTranslateYBinding.subtract(offsetBinding))
        circle.strokeType = StrokeType.OUTSIDE
        clearOutline()

        //Label
        label.translateXProperty().bind(vTranslateXBinding.subtract(offsetBinding))
        label.translateYProperty().bind(vTranslateYBinding.subtract(offsetBinding))

        label.textFill = Color.WHITE

        //Hitbox
        hitbox.translateXProperty().bind(vTranslateXBinding)
        hitbox.translateYProperty().bind(vTranslateYBinding)
    }

    fun setColor(color: Color) {
        circle.fill = color
    }

    fun setOutline(color: Color) {
        circle.stroke = color
        circle.strokeWidth = 5.0
    }

    fun clearOutline() {
        circle.stroke = Color.TRANSPARENT
        circle.strokeWidth = 0.0
    }

    init {
        //Bind all properties
        bindProperties()
        //Start a family
        children.addAll(circle, label, hitbox)

        hitbox.pickOnBoundsProperty().set(true)
    }

    override fun toString(): String {
        return v.toString()
    }

//    fun copy(): Vertex<E> {
//        val v = Vertex(v, x.get(), y.get(), radius, parentPane)
//        v.circle.fill = circle.fillProperty().get()
//        v.circle.stroke = circle.strokeProperty().get()
//        v.circle.strokeWidth = circle.strokeWidthProperty().get()
//    }
}