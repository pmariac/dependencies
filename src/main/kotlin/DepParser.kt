import io.data2viz.color.Colors
import java.io.File
import io.data2viz.force.*
import io.data2viz.geom.Size
import io.data2viz.viz.JFxVizRenderer
import io.data2viz.viz.viz
import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.stage.Stage

class DependenciesApp : Application() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(DependenciesApp::class.java)
        }
    }

    val viz = io.data2viz.viz.viz {
        width = 1000.0
        height = 1000.0
        rect {
            size = Size(100.0, 100.0)
            fill = Colors.Web.rebeccapurple
        }
    }

    override fun start(stage: Stage) {
        val root = Group()
        val canvas = Canvas(1000.0, 1000.0)
        JFxVizRenderer(canvas, viz)
        root.children.add(canvas)

        stage.let {
            it.scene = (Scene(root, 1000.0, 1000.0))
            it.show()
        }

        val timer = object : AnimationTimer() {
            override fun handle(now: Long) {
                viz.render()
            }
        }

        timer.start()
    }

}


fun getNodesAndLinks(): Pair<Set<Component>, List<Link>> {
    val lines = File("src/main/resources/compile.txt").readLines()
    val results = lines.mapNotNull(::parseLine)
    return buildTree(results)
}


data class Component(val artifact: String)
data class Link(val parent: Component, val child: Component)


//PARSING STUFF

data class ParseResult(val level: Int, val name: String)

val regexp = """[a-zA-Z]+[a-zA-Z0-9:.\-]+""".toRegex()


fun parseLine(line: String): ParseResult? =
    regexp
        .find(line)
        ?.let { ParseResult(it.range.start / 5, it.value) }



fun buildTree(parsedResults: List<ParseResult>): Pair<Set<Component>, List<Link>> {
    val currentComponent = Component("root")
    val components = mutableMapOf("root" to currentComponent)
    val links = mutableListOf<Link>()
    var tree = listOf(currentComponent)

    parsedResults.forEach {
        val component = components.getOrPut(it.name) { Component(it.name) }
        if (it.level <= tree.size) {
            tree = tree.dropLast(tree.size - it.level)
        }
        tree += component
        links += Link(tree[tree.size-2], component)
    }

    return Pair(components.values.toSet(), links)
}


