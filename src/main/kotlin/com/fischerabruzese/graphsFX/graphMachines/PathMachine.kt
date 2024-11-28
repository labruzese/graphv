package com.fischerabruzese.graphsFX.graphMachines

import com.fischerabruzese.graphsFX.FXConnection
import com.fischerabruzese.graphsFX.FXVertex
import com.fischerabruzese.graphsFX.vertexManagers.AppearanceManager

class PathMachine(val appearanceManager: AppearanceManager) {
    var currentPathVerticies: List<FXVertex<*>>
    var currentPathConnections: List<FXConnection<*>>

    //
    fun isHoverAllowed(): Boolean
}