package com.fischerabruzese.graphsFX.graphMachines

import com.fischerabruzese.graph.Graph
import com.fischerabruzese.graphsFX.*
import com.fischerabruzese.graphsFX.vertexManagers.PositionManager
import javafx.application.Platform
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.collections.ArrayList

interface IPhysicsMachine {
    val graph: Graph<FXVertex<*>>
    val positionManager: PositionManager
    var ghostSimulation: ArrayList<Pair<*,Position>>
    var physicsThreads: ThreadGroup

    /**
     * @param speed the speed (ie magnitude) of the calculations
     * @param unaffected all the vertices that aren't moved
     * @param uneffectors all the vertices that do not cause movements
     * @returns An array of displacements such that the displacement at each index correspondents with [vertices]
     */
    fun generateFrame(
        speed: Double,
        unaffected: List<FXVertex<*>> = emptyList(),
        uneffectors: List<FXVertex<*>> = emptyList(),
        alternateVertexPositions: List<Pair<*, Position>> = graph.map { fxVert -> fxVert to fxVert.pos }
    ): Array<Displacement>

    /**
     * Opens a thread that will generate and push frames to the gui at [speed] until [stopSimulation]
     */
    fun simulate(speed: Double,
                 unaffected: List<FXVertex<*>>,
                 uneffectors: List<FXVertex<*>>) {

        ghostSimulation = ArrayList(graph.map { it to it.pos })

        Thread(physicsThreads) {
            simulation(speed, unaffected, uneffectors, ghostSimulation)
        }.start()

        Thread(physicsThreads) {
            platformCommunication()
            return@Thread
        }.start()
    }

    private fun simulation(
        speed: Double,
        unaffected: List<FXVertex<*>>,
        uneffectors: List<FXVertex<*>>,
        ghostVertices: java.util.ArrayList<Pair<*, Position>>
    ) {
        while (!Thread.interrupted()) {
            try {
                val displacements = generateFrame(
                    speed,
                    unaffected = unaffected,
                    uneffectors = uneffectors,
                    alternateVertexPositions = ghostVertices.toList()
                )
                pushGhostFrame(displacements)
            }
            //Graph has changed or physics stopped, restart simulation
            catch (ex: Exception) {
                when (ex) {
                    is NoSuchElementException, is IndexOutOfBoundsException -> {
                        if (!isStopped()) {
                            Platform.runLater {
                                stopSimulation()
                                startSimulation()
                            }
                        }
                        return
                    }

                    else -> throw ex
                }
            }
            //Thread.sleep(1)
        }
    }

    fun platformCommunication() {
        while (!Thread.interrupted()) {
            val latch = CountDownLatch(1) // Initialize with a count of 1
            Platform.runLater {
                try {
                    pushRealFrame()
                }
                //Graph has changed or physics stopped, restart simulation
                catch (ex: Exception) {
                    when (ex) {
                        is NoSuchElementException, is IndexOutOfBoundsException -> {
                            if (!isStopped()) { //if unexpected crash
                                stopSimulation()
                                startSimulation()
                            }
                            return@runLater //Don't count down latch and cause a InterruptedException in thread
                        }

                        else -> throw ex
                    }
                }
                latch.countDown() //signal that Platform has executed our frame
            }
            try {
                latch.await()
            } catch (e: InterruptedException) {
                return
            } //wait for platform to execute our frame
        }
    }



    fun isStopped(): Boolean
    fun Stop()

    fun stopSimulation() {
        if (isStopped()) return

        Stop()
        val threadList = Array<Thread?>(2) { null }.apply { physicsThreads.enumerate(this) } as Array<Thread>

        for (t in threadList) {
            t.interrupt()
            t.join() //wait for each thread to die
        }

        ghostSimulation = ArrayList()
        physicsThreads = ThreadGroup("Physics Threads")
    }

    fun isActive(): Boolean {
        return physicsThreads.isNotEmpty()
    }

    /**
     * Starts the simulation if it is inactive
     * @return true if the simulation was inactive and has been started. False if the simulation was already active
     */
    fun startSimulation(): Boolean {
        if (isActive()) return false
        stopping = false

        simulate()
        return true
    }




    /** Updates every vertex with the calculated displacements */
    private fun pushRealFrame() {
        for (vertexIndex in ver    abstract fun generateFrame(
            speed: Double,
            unaffected: List<GraphicComponents<E>.Vertex> = emptyList(),
            uneffectors: List<GraphicComponents<E>.Vertex> = emptyList(),
            verticesPos: List<Pair<GraphicComponents<E>.Vertex, Position>> = vertices.map { it to it.pos }
        ): Array<Displacement>tices.indices) {
            if (!vertices[vertexIndex].draggingFlag) {
                vertices[vertexIndex].pos = ghostVertices[vertexIndex].second
            }
        }
        ghostVertices = ArrayList(vertices.map { it to it.pos }) //reset ghost vertices
    }

    private fun pushGhostFrame(displacementArr: Array<Displacement>) {
        for ((vertexIndex, displacement) in displacementArr.withIndex()) {
            if (!ghostVertices[vertexIndex].first.draggingFlag) {
                ghostVertices[vertexIndex] =
                    ghostVertices[vertexIndex].let { it.first to it.second.plus(displacement) }
                ghostVertices[vertexIndex] = ghostVertices[vertexIndex].let {
                    it.first to Position( //recreating position will constrain position data, but tbh position class should be rewritten anyway its kinda trash
                        it.second.x,
                        it.second.y
                    )
                }
            }
        }
    }
}