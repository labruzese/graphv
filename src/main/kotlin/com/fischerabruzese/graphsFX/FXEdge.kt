package com.fischerabruzese.graphsFX

import javafx.scene.layout.StackPane
import javafx.scene.paint.Color

//Basically a container for 2 connections
class FXEdge<E>(val v1tov2Connection: FXConnection<E>?, val v2tov1Connection: FXConnection<E>? = null) : StackPane() {
    val v1: FXVertex<E> = v1tov2Connection?.from ?: v2tov1Connection?.to ?: throw IllegalArgumentException("Both connections can't be null")
    val v2: FXVertex<E> = v1tov2Connection?.to ?: v2tov1Connection?.from ?: throw IllegalArgumentException("Both connections can't be null")

    constructor(v1: FXVertex<E>, v2: FXVertex<E>, v1tov2: Int?, v2tov1: Int?)
            : this(
                if (v1tov2 != null) FXConnection(v1, v2, v1tov2, true) else null,
                if (v2tov1 != null) FXConnection(v2, v1, v2tov1, false) else null
            )

    init {
        if (v1tov2Connection != null) {
            v1tov2Connection.setLineColor(Color.rgb(0, 0, 0, 0.6))
            children.add(v1tov2Connection)
        }
        if (v2tov1Connection != null) {
            v2tov1Connection.setLineColor(Color.rgb(0, 0, 0, 0.6))
            children.add(v2tov1Connection)
        }
    }

    fun hideLabels() {
        v1tov2Connection?.hideLabel()
        v2tov1Connection?.hideLabel()
    }

    fun showLabels() {
        v1tov2Connection?.showLabel()
        v2tov1Connection?.showLabel()
    }

    /**
     * Make both connections in this edge whatever color you want
     */
    fun setLineColor(color: Color) {
        v1tov2Connection?.setLineColor(color)
        v2tov1Connection?.setLineColor(color)
    }

    /**
     * @param outBoundColor the new color for the outbound connection
     * @param inboundColor the new color ro the inbound connection
     * @param from the vertex you want to calculate inbound and outbound from. Must be either [v1] or [v2].
     */
    fun setLineColor(outBoundColor: Color, inboundColor: Color, from: FXVertex<E>) {
        if (v1 == from) {
            v1tov2Connection?.setLineColor(outBoundColor)
            v2tov1Connection?.setLineColor(inboundColor)
        } else if (v2 == from) {
            v1tov2Connection?.setLineColor(inboundColor)
            v2tov1Connection?.setLineColor(outBoundColor)
        } else {
            throw IllegalArgumentException("from the vertex you want to calculate inbound and outbound from. Must be either v1 or v2.")
        }
    }

    /**
     * Make both connections' labels in this edge whatever color you want
     */
    fun setLabelColor(color: Color) {
        v1tov2Connection?.setLabelColor(color)
        v2tov1Connection?.setLabelColor(color)
    }

    /**
     * @param outBoundColor the new color for the outbound connection's label
     * @param inboundColor the new color ro the inbound connection's label
     * @param from the vertex you want to calculate inbound and outbound from. Must be either [v1] or [v2].
     */
    fun setLabelColor(outBoundColor: Color, inboundColor: Color, from: FXVertex<E>) {
        if (v1 == from) {
            v1tov2Connection?.setLabelColor(outBoundColor)
            v2tov1Connection?.setLabelColor(inboundColor)
        } else {
            v1tov2Connection?.setLabelColor(inboundColor)
            v2tov1Connection?.setLabelColor(outBoundColor)
        }
    }

    operator fun component1(): FXVertex<E> {
        return v1
    }

    operator fun component2(): FXVertex<E> {
        return v2
    }
}
