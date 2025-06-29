package com.fischerabruzese.graphsFX

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

class GraphApp : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(GraphApp::class.java.getResource("graph.fxml"))
        val scene = Scene(fxmlLoader.load(), 1280.0, 820.0)
        val controller: Controller = fxmlLoader.getController()!!
        scene.stylesheets.addAll(GraphApp::class.java.getResource("style.css")!!.toExternalForm())

        stage.title = "Graph"
        stage.scene = scene

        val graph = FetchGraph.fetchGraph()

        controller.initializeGraph(graph, stage)

        stage.show()
    }
}

fun launchJfx() {
    val graphApp = GraphApp()
    Application.launch(graphApp::class.java)
}
