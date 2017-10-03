val operator_priority = mapOf('|' to 1, '-' to 2, '*' to 3, '?' to 3, '+' to 3)
// 三种基本的符号：或 连接 闭包

var s = 0

class Node(val inEdges: MutableList<Edge> = mutableListOf<Edge>(),
           val outEdges: MutableList<Edge> = mutableListOf<Edge>(), var end: Boolean=false){

//    var statu: Int = 0 //标识自动机每个不同节点的状态号码
    var statu:Int = 0
    init {
        s += 1
        statu = s
    }

    fun addInEdge(inEdge: Edge){
        inEdge.endNode = this
        this.inEdges.add(inEdge)
    }

    fun addOutEdge(outEdge: Edge){
        outEdge.startNode = this
        this.outEdges.add(outEdge)
    }

    //用于汤普森算法合并子图
    fun merge(node: Node){
        this.end = node.end

        for (edge in this.outEdges){
            node.outEdges.add(edge)
        }
        for (edge in this.inEdges){
            node.inEdges.add(edge)
        }
    }
//
//    fun nextNodes () :MutableList<Map<Char, Node>>{
//        val nodeList = mutableListOf<Map<Char, Node>>()
//        for (edge in this.outEdges)
//            nodeList.add(mapOf(edge.value!! to edge.endNode))
//        return nodeList
//    }
}

class Edge(val value: Char?, var startNode: Node, var endNode: Node){
    init {
        startNode.outEdges.add(this)
        endNode.inEdges.add(this)
    }
}

//fun traverseNFA(start :Node, end: Node){
//    val visitedSet = mutableSetOf(start)
//    while (start != end){
//        for (i in start.nextNodes()){
//            println("")
//        }
//    }
//
//
//}

fun mergeSubGraph(op: Char, subGraphStack: MutableList<List<Node>>){

    when(op){
        '-' -> {
            //取出两个栈顶的子图
            val subGraph2 = subGraphStack.last()
            subGraphStack.removeAt(subGraphStack.lastIndex)
            //println(subGraph2)
            val startNode2 = subGraph2[0]
            val endNode2 = subGraph2[1]
            val subGraph1 = subGraphStack.last()
            subGraphStack.removeAt(subGraphStack.lastIndex)
            val startNode1 = subGraph1[0]
            val endNode1 = subGraph1[1]

            //串联两个子图
            endNode1.merge(startNode2)
            subGraphStack.add(listOf(startNode1, endNode2))
        }

        '|' -> {
            //取出两个栈顶的子图
            val subGraph2 = subGraphStack.last()
            subGraphStack.removeAt(subGraphStack.lastIndex)
            val startNode2 = subGraph2[0]
            val endNode2 = subGraph2[1]
            val subGraph1 = subGraphStack.last()
            subGraphStack.removeAt(subGraphStack.lastIndex)
            val startNode1 = subGraph1[0]
            val endNode1 = subGraph1[1]

            endNode1.end = false
            endNode2.end = false
            //并联两个子图
            val newStartNode = Node()
            val newEndNode = Node(end=true)
            //加四条epsilon边
            Edge(null, newStartNode, startNode1)
            Edge(null, newStartNode, startNode2)
            Edge(null, startNode1, newEndNode)
            Edge(null, startNode1, newEndNode)

            subGraphStack.add(listOf(newStartNode, newEndNode))
        }

        '*' -> {
            //取出栈顶的一个子图做闭包
            val subGraph = subGraphStack.last()
            subGraphStack.removeAt(subGraphStack.lastIndex)
            val startNode = subGraph[0]
            val endNode = subGraph[1]
            endNode.end = false

            val newStartNode = Node()
            val newEndNode = Node(end=true)
            Edge(null, startNode, endNode)
            Edge(null, newStartNode, newEndNode)
            Edge(null, newStartNode, startNode)
            Edge(null, endNode, newEndNode)

            subGraphStack.add(listOf(newStartNode, newEndNode))
        }

        '?' -> {
            val subGraph = subGraphStack.last()
            subGraphStack.removeAt(subGraphStack.lastIndex)
            val startNode = subGraph[0]
            val endNode = subGraph[1]
            endNode.end = false

            val newStartNode = Node()
            val newEndNode = Node(end=true)
            Edge(null, newStartNode, newEndNode)
            Edge(null, newStartNode, startNode)
            Edge(null, endNode, newEndNode)

            subGraphStack.add(listOf(newStartNode, newEndNode))
        }

        '+' -> {
            //取出栈顶的一个子图做闭包
            val subGraph = subGraphStack.last()
            subGraphStack.removeAt(subGraphStack.lastIndex)
            val startNode = subGraph[0]
            val endNode = subGraph[1]
            endNode.end = false

            val newStartNode = Node()
            val newEndNode = Node(end=true)
            Edge(null, startNode, endNode)
            Edge(null, newStartNode, startNode)
            Edge(null, endNode, newEndNode)

            subGraphStack.add(listOf(newStartNode, newEndNode))
        }
    }
}

//正则表达式转换为NFA图
fun re2NFA(pattern: String) :MutableList<List<Node>>{
    val subGraphStack: MutableList<List<Node>> = mutableListOf() //存放子图
    val opStack: MutableList<Char> = mutableListOf()    //存放字符间的连接符号
    var isOp = false
    var addCat = false //确认是否需要补上字符间默认的连接符'-'

    for (token in pattern){

//        if (token == '|'){
//            isOp = true
//            addCat = false
//        }
//        else if (token == '*'){
//            isOp = true
//        }
//        else{
//            isOp = false
//        }
        when(token){
            '|' -> {
                isOp = true
                addCat  = false
            }
            '*', '?', '+' -> isOp = true
            else -> isOp = false
        }
        //中缀表达式与后缀表达式原理, 例如逆波兰表达式
        if (isOp){
            while (!opStack.isEmpty() && operator_priority[opStack.last()]!! >= operator_priority[token]!!){
                val op = opStack.removeAt(opStack.lastIndex)
                mergeSubGraph(op, subGraphStack)
            }
            opStack.add(token)
            isOp = false
        }

        else{//是字符
            if (addCat){
                //字符之间默认要与前一个补全"隐藏的"连接符
                val catOp = '-'
                while (!opStack.isEmpty() && operator_priority[opStack.last()]!! >= operator_priority[catOp]!!){
                    val op = opStack.removeAt(opStack.lastIndex)
                    mergeSubGraph(op, subGraphStack)
                }
                opStack.add(catOp)
            }
            //单个字符构造最基本的子图
            val startNode = Node()
            val endNode = Node(end=true)
            Edge(token, startNode, endNode)
            subGraphStack.add(listOf(startNode, endNode))
            addCat = true

        }
    }

    //将符号栈内所剩的符号应用后清空栈
    while (!opStack.isEmpty()){
        val op = opStack.last()
        opStack.removeAt(opStack.lastIndex)
        mergeSubGraph(op, subGraphStack)
    }

    return subGraphStack
}

fun main(args: Array<String>) {
    val re = readLine()
    val graph = re2NFA(re!!)[0]
    val n1 = graph[0]
    val n2 = graph[1]
    println(n1.statu)
    println(n2.statu)

}