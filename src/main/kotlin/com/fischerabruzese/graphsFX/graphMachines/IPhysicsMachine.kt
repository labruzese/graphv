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

    var speed: Double
    var unaffected: List<FXVertex<*>>
    var uneffectors: List<FXVertex<*>>

    var simulationVertexPositions: MutableMap<FXVertex<*>,Position>

    var physicsThreads: ThreadGroup
    var stopped: Boolean

    /**
     * @param speed the speed (ie magnitude) of the calculations
     * @param unaffected all the vertices that aren't moved
     * @param uneffectors all the vertices that do not cause movements
     * @returns An array of displacements such that the displacement at each index correspondents with [vertices]
     */
    fun generateFrame(
        alternateVertexPositions: Map<FXVertex<*>, Position> = graph.associateWith { fxVert -> fxVert.pos }
    ): Map<FXVertex<*>, Displacement>

    /**
     * Opens a thread that will generate and push frames to the gui at [speed] until [stopSimulation]
     */
    fun newSimulation() {
        simulationVertexPositions = graph.associateWith { fxVert -> fxVert.pos }.toMutableMap()

        Thread(physicsThreads) {
            simulationUsing(simulationVertexPositions)
        }.start()

        Thread(physicsThreads) {
            platformCommunication()
            return@Thread
        }.start()
    }

    private fun simulationUsing(
        vertexPositions: Map<FXVertex<*>, Position>
    ) {
        while (!Thread.interrupted()) {
            try {
                val displacements = generateFrame(vertexPositions)
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



    fun isStopped(): Boolean = stopped

    fun stopSimulation() {
        if (isStopped()) return
        stopped = true

        val threadList = Array<Thread?>(2) { null }.apply { physicsThreads.enumerate(this) } as Array<Thread>

        for (t in threadList) {
            t.interrupt()
            t.join() //wait for each thread to die
        }

        simulationVertexPositions = mutableMapOf()
        physicsThreads = ThreadGroup("Physics Threads")
    }

    fun isActive(): Boolean {
        return Array<Thread?>(2) { null }.run { physicsThreads.enumerate(this) } > 0
    }

    /**
     * Starts the simulation if it is inactive
     * @return true if the simulation was inactive and has been started. False if the simulation was already active
     */
    fun startSimulation(): Boolean {
        if (isActive()) return false
        stopped = false
        newSimulation()
        return true
    }

    private fun pushRealFrame() {
        for (vertex in graph) {
            if (!vertex.positionLock) {
                vertex.pos = simulationVertexPositions[vertex] ?: vertex.pos
            }
        }
        simulationVertexPositions = graph.associateWith { fxVert -> fxVert.pos }.toMutableMap() //reset ghost vertices
    }

    private fun pushGhostFrame(displacementArr: Map<FXVertex<*>, Displacement>) {
        for ((vertex, displacement) in displacementArr) {
            if (vertex.positionLock) continue
            simulationVertexPositions[vertex]?.let { simulationVertexPositions[vertex] = it + displacement }
        }
    }
}