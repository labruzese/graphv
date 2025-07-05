package com.fischerabruzese.graphvcli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import kotlinx.serialization.json.*
import com.fischerabruzese.graphvcli.SGraph

class Hello : CliktCommand() {
    override fun run() {
        echo("Hello World!")
        val vertices = listOf(
            Vertex("0", mapOf(
                "data1" to JsonPrimitive("value1")
            )),
            Vertex("1", mapOf(
                "data2" to JsonPrimitive("value2")
            ))
        )
        val edges = listOf(
           Edge("0", "1", attributes = mapOf(
                "edgedata1" to JsonPrimitive("value1")
            ))
        )
        val testGraph = SGraph(vertices, edges)
        testGraph.saveToFile(Config().storageDir + "/graph.json")
    }
}

fun main(args: Array<String>) = Hello().main(args)
