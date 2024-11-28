package com.fischerabruzese.graphsFX.graphMachines

import com.fischerabruzese.graphsFX.FXVertex
import com.fischerabruzese.graphsFX.vertexManagers.PositionManager
import javafx.scene.input.MouseEvent

class MouseMachine(val positionManager: PositionManager) {
    var selectedFXVertex: FXVertex<*>?

    var xDelta: Double
    var yDelta: Double

    //Move into actual implementation
    fun setDragStartPosition(event: MouseEvent) {
        xDelta = event.sceneX / pane.width - selectedFXVertex!!.x.get()
        yDelta = event.sceneY / pane.height - selectedFXVertex!!.y.get()
    }

    fun mouseDragged(event: MouseEvent) {
        selectedFXVertex!!.x.set((event.sceneX / pane.width - xDelta).let { if (it > 1) 1.0 else if (it < 0) 0.0 else it })
        selectedFXVertex!!.y.set((event.sceneY / pane.height - yDelta).let { if (it > 1) 1.0 else if (it < 0) 0.0 else it })
    }
}