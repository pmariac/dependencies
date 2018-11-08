import io.data2viz.color.Colors
import io.data2viz.force.*
import io.data2viz.viz.*
import javafx.animation.AnimationTimer
import javafx.application.Application
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.stage.Stage
import java.io.File

const val vizWidth = 1000.0
const val vizHeight = 1000.0

class DependenciesApp : Application() {

//    lateinit var nodesCircles: List<CircleNode>
    lateinit var nodesLabel: List<TextNode>
    lateinit var nodesLinks: List<LineNode>

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(DependenciesApp::class.java)
        }
    }

    override fun start(stage: Stage) {

        val nodesAndLinks = getNodesAndLinks()
        val components = nodesAndLinks.first
        val nodes = components.map { it.node }
        val links = nodesAndLinks.second

        val forceLayout = ForceLink()
        forceLayout.linksAccessor = { links }
        forceLayout.distancesAccessor = { links.map { 80.0 } }

        val forceRepulse = ForceNBody()
        forceRepulse.strength = { _, _, _ -> -200.0}

        val simulation = ForceSimulation()
        simulation.nodes = nodes
        simulation.addForce("links", forceLayout)
        simulation.addForce("repulsion", forceRepulse)


        val viz = io.data2viz.viz.viz {
            width = vizWidth
            height = vizHeight

            group {
                transform {
                    translate(vizWidth / 2,vizHeight / 2)
                }
                nodesLinks = links.map { link ->
                    line {
                        stroke = Colors.Web.black
                        x1 = link.source.x
                        x2 = link.target.x
                        y1 = link.source.y
                        y2 = link.target.y
                    }
                }
//                nodesCircles = simulation.nodes.map { node ->
//                    circle {
//                        stroke = null
//                        radius = 5.0
//                        fill = Colors.Web.red
//                        x = node.x
//                        y = node.y
//                    }
//                }
                nodesLabel = simulation.nodes.map { node ->
                    text {
                        anchor = TextAnchor.MIDDLE
                        textContent = componentRegexp.find(components[node.index].artifact)?.value ?: ""
                        stroke = Colors.Web.black
                        x = node.x
                        y = node.y
                    }
                }
            }
        }

        val root = Group()
        val canvas = Canvas(vizWidth, vizHeight)
        JFxVizRenderer(canvas, viz)
        root.children.add(canvas)

        stage.let {
            it.scene = (Scene(root, vizWidth, vizHeight))
            it.show()
        }

        val timer = object : AnimationTimer() {
            override fun handle(now: Long) {
                simulation.tick()
                /*nodes.forEachIndexed { index, forceNode ->
                    nodesCircles.get(index).apply {
                        x = forceNode.x
                        y = forceNode.y
                    }
                }*/
                nodes.forEachIndexed { index, forceNode ->
                    nodesLabel.get(index).apply {
                        x = forceNode.x
                        y = forceNode.y
                    }
                }
                links.forEachIndexed { index, link ->
                    nodesLinks.get(index).apply {
                        x1 = link.source.x
                        x2 = link.target.x
                        y1 = link.source.y
                        y2 = link.target.y
                    }
                }
                viz.render()
            }
        }
        timer.start()
    }

}


fun getNodesAndLinks(): Pair<List<Component>, List<Link>> {
    val lines = File("src/main/resources/compile.txt").readLines()
    val results = lines.mapNotNull(::parseLine)
    return buildTree(results)
}


data class Component(val artifact: String, val node: ForceNode)


//PARSING STUFF

data class ParseResult(val level: Int, val name: String)

val lineRegexp = """[a-zA-Z]+[a-zA-Z0-9:.\-]+""".toRegex()
val componentRegexp = """(?<=:)(.*?)(?=:)""".toRegex()


fun parseLine(line: String): ParseResult? =
    lineRegexp
        .find(line)
        ?.let { ParseResult(it.range.start / 5, it.value) }


fun buildTree(parsedResults: List<ParseResult>): Pair<List<Component>, List<Link>> {
    val components = mutableListOf<Component>()
    val rootNode = ForceNode(0)
    rootNode.fixedX = .0
    rootNode.fixedY = .0

    val currentComponent = Component("root", rootNode)
    val componentsMap = mutableMapOf("root" to currentComponent)
    components += currentComponent

    val links = mutableListOf<Link>()
    var tree = listOf(currentComponent)

    var nodeIndex = 1
    parsedResults.forEach {
        val name = it.name + "$nodeIndex"
        var component = componentsMap.get(name)
        if (component == null) {
            val node = ForceNode(nodeIndex)

            nodeIndex++

            component = Component(name, node)
            componentsMap.put(name, component)
            components += component
        }
        if (it.level <= tree.size) {
            tree = tree.dropLast(tree.size - it.level)
        }
        tree += component
        links += Link(tree[tree.size - 2].node, component.node)
    }

    return Pair(components, links)
}


