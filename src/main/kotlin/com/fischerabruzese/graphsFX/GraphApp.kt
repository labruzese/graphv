package com.fischerabruzese.graphsFX

import com.fischerabruzese.graph.Graph
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

class GraphApp : Application() {
    val pgraph: Graph<String>? = null

    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(GraphApp::class.java.getResource("graph.fxml"))
        val scene = Scene(fxmlLoader.load(), 1280.0, 820.0)
        val controller: Controller = fxmlLoader.getController()!!
        scene.stylesheets.addAll(GraphApp::class.java.getResource("style.css")!!.toExternalForm())

        stage.title = "Graph"
        stage.scene = scene

        val graph = pgraph ?: return

        controller.initializeGraph(graph, stage)

        stage.show()
    }
}

fun launchJfx() {
    val graphApp = GraphApp()
    Application.launch(graphApp::class.java)
}

