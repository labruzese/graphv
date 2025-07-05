package com.fischerabruzese.graphvcli

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.cinterop.*
import platform.posix.*

@Serializable
data class Vertex(
    val id: String,
    val attributes: Map<String, JsonElement> = emptyMap(),
)

@Serializable
data class Edge(
    val from: String,
    val to: String,
    val weight: Double = 1.0,
    val attributes: Map<String, JsonElement> = emptyMap(),
)

@Serializable
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
data class SGraph(
    val vertices: List<Vertex>,
    val edges: List<Edge>,
) {
    val vertexIds: Set<String> get() = vertices.map { it.id }.toSet()

    fun validateEdges(): Boolean =
        edges.all { edge ->
            edge.from in vertexIds && edge.to in vertexIds
        }

    fun getVertex(id: String): Vertex? = vertices.find { it.id == id }

    fun getEdgesFrom(vertexId: String): List<Edge> = edges.filter { it.from == vertexId }

    fun getEdgesTo(vertexId: String): List<Edge> = edges.filter { it.to == vertexId }

    fun saveToFile(filename: String) {
        val json =
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            }

        val jsonString = json.encodeToString(this)
        val file = fopen(filename, "w") ?: throw Exception("Cannot open file for writing: $filename")

        fputs(jsonString, file)
        fclose(file)
    }

    companion object {
        fun loadFromFile(filename: String): SGraph {
            val json =
                Json {
                    ignoreUnknownKeys = true
                }

            val file = fopen(filename, "r") ?: throw Exception("Cannot open file for reading: $filename")

            // Get file size
            fseek(file, 0, SEEK_END)
            val size = ftell(file)
            fseek(file, 0, SEEK_SET)

            // Read content
            val buffer = ByteArray(size.toInt())
            fread(buffer.refTo(0), 1u, size.toULong(), file)
            fclose(file)

            val content = buffer.decodeToString()
            return json.decodeFromString<SGraph>(content)
        }
    }
}
