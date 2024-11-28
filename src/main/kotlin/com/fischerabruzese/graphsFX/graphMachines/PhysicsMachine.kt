package com.fischerabruzese.graphsFX.graphMachines

import com.fischerabruzese.graph.Graph
import com.fischerabruzese.graphsFX.Displacement
import com.fischerabruzese.graphsFX.GraphicComponents
import com.fischerabruzese.graphsFX.Position
import javafx.application.Platform
import java.util.*
import java.util.concurrent.CountDownLatch
import com.fischerabruzese.graphsFX.*
import com.fischerabruzese.graphsFX.vertexManagers.PositionManager
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

class PhysicsMachine(override val positionManager: PositionManager,
                     override var uneffectors: List<FXVertex<*>>,
                     override var unaffected: List<FXVertex<*>>,
                     override var speed: Double,
                     override val graph: Graph<FXVertex<*>>
) : IPhysicsMachine {
    override var physicsThreads = ThreadGroup("Physics Threads")
    override var simulationVertexPositions: MutableMap<FXVertex<*>, Position> = mutableMapOf()
    override var stopped = false

    override fun generateFrame(alternateVertexPositions: Map<FXVertex<*>, Position>): Map<FXVertex<*>, Displacement> {
        val max = 1500
        val scaleFactor = speed.pow(4) * max

        val displacements: MutableMap<FXVertex<*>, Displacement> = graph.associateWith { Displacement(0.0,0.0) }.toMutableMap()
        for ((affectedVertex, affectedPos) in simulationVertexPositions) {
            if (unaffected.contains(affectedVertex)) continue
            val effectors = LinkedList<Pair<Position, (Double) -> Double>>()

            val vertexRepulsionField: (Double) -> Double = { rSqr -> (scaleFactor / rSqr) }
            val vertexAttractionField: (Double) -> Double = { rSqr -> (-scaleFactor * rSqr.pow(2)) }

            val unconnectedVertexField: (Double) -> Double = { rSqr -> 1 * vertexRepulsionField(rSqr)}
            val singleConnectedVertexField: (Double) -> Double = { rSqr -> 1000 * vertexAttractionField(rSqr) + 0.5 * vertexRepulsionField(rSqr)}
            val doubleConnectedVertexField: (Double) -> Double = { rSqr -> 2000 * vertexAttractionField(rSqr) + 0.5 * vertexRepulsionField(rSqr)}
            val edgeFieldEquation: (Double) -> Double = { rSqr ->  0.5 * vertexRepulsionField(rSqr) }
            val wallFieldEquation: (Double) -> Double = { rSqr ->  0.5 * vertexRepulsionField(rSqr) }

            //vertices
            val (effectorVerts, effectorPos) = simulationVertexPositions
                .filterNot { (uneffectors.contains(it.key) || affectedVertex === it.key) }
                .toList()
                .unzip()

            effectorVerts
                .mapIndexedTo(effectors) { i, vertexEffector ->
                    when (graph.countEdgesBetween(vertexEffector, affectedVertex)) {
                        1 -> Pair(effectorPos[i], singleConnectedVertexField)
                        2 -> Pair(effectorPos[i], doubleConnectedVertexField)
                        else -> Pair(effectorPos[i], unconnectedVertexField)
                    }
                }

            /*
            edges.zip(edges.dumpPositions())
                .filterNot { (e, _) -> e.v1 == affectedVertex || e.v2 == affectedVertex } //should I add another filter for effector stuff
                .mapTo(effectors) { (_, effectorPos) -> Pair(effectorPos, edgeFieldEquation) }
             */

            //walls
            listOf(
                Position(1.0, affectedPos.y),
                Position(0.0, affectedPos.y),
                Position(affectedPos.x, 1.0),
                Position(affectedPos.x, 0.0)
            )
                .mapTo(effectors) { wallEffectorPos -> Pair(wallEffectorPos, wallFieldEquation) }

            displacements[affectedVertex] = calculateAdjustmentAtPos(affectedPos, effectors)
        }

        return displacements
    }

    private fun calculateAdjustmentAtPos(
        at: Position,
        froms: List<Pair<Position, (Double) -> Double>>,
        forceCapPerPos: Double = 0.1
    ): Displacement {
        val displacement = Displacement(0.0, 0.0)

        //Adding adjustments
        for ((pos, fieldEq) in froms) {
            val scaleFactor = 0.00006 / (graph.size() + graph.getEdges().size)
            if (at == pos) return Displacement(
                Random.nextDouble(-0.000001, 0.000001),
                Random.nextDouble(-0.000001, 0.000001)
            ) //Nudge slightly if at the same position
            displacement += calculateAdjustmentAtPos(at, pos, scaleFactor, fieldEq)
        }

        //Capping the total force, add some variation
        displacement.constrainBetween(
            forceCapPerPos, //+ Random.nextDouble(-forceCapPerPos/10, forceCapPerPos/10),
            -forceCapPerPos //+ Random.nextDouble(-forceCapPerPos/10, forceCapPerPos/10)
        )
        return displacement
    }

    private fun calculateAdjustmentAtPos(
        at: Position,
        from: Position,
        scaleFactor: Double,
        magnitudeFormula: (radiusSquared: Double) -> Double = { 1 / it }
    ): Displacement {
        //Window scalers
        val xScaler = 2 * pane.width / (pane.width + pane.height)
        val yScaler = 2 * pane.height / (pane.width + pane.height)

        val dx = (at.x - from.x) * xScaler
        val dy = (at.y - from.y) * yScaler
        val radiusSquared = dx.pow(2) + dy.pow(2)

        val magnitude = scaleFactor * magnitudeFormula(radiusSquared)
        val angle = atan2(dy, dx)

        val fdx = magnitude * cos(angle)
        val fdy = magnitude * sin(angle)

        return Displacement(fdx, fdy, 0.9, -0.9)
    }
}
