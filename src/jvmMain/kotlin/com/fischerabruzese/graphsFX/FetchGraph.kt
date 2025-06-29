
import com.fischerabruzese.graph.AMGraph
import com.fischerabruzese.graph.Graph

fun fetchGraph(filePath: String): Graph<String> = AMGraph(listOf("A", "B", "C"))
