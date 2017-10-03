val operator_priority = mapOf('(' to -1, '|' to 1, '-' to 2, '*' to 3, '?' to 3, '+' to 3)
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

    //查看这个节点通过非epsilon边可以到达的节点集合
    fun nextNodes (v: Char) :MutableList<Node>{
        val nodeList = mutableListOf<Node>()
        for (edge in this.outEdges)
            if (edge.value != null && edge.value == v)
                nodeList.add(edge.endNode)
        return nodeList
    }

    fun nextNode (v: Char) :Node?{
        for (edge in this.outEdges)
            if (edge.value == v)
                return edge.endNode
        return null
    }
}

class Edge(val value: Char?, var startNode: Node, var endNode: Node){
    init {
        startNode.outEdges.add(this)
        endNode.inEdges.add(this)
    }
}

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
            //endNode1.merge(startNode2)
            Edge(null, endNode1, startNode2)
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
//            subGraphStack.removeAt(subGraphStack.lastIndex)
            val startNode = subGraph[0]
            val endNode = subGraph[1]
//            endNode.end = false
//
//            val newStartNode = Node()
//            val newEndNode = Node(end=true)
//            Edge(null, startNode, endNode)
//            Edge(null, newStartNode, newEndNode)
//            Edge(null, newStartNode, startNode)
//            Edge(null, endNode, newEndNode)
            Edge(null, startNode, endNode)
            Edge(null, endNode, startNode)
//            subGraphStack.add(listOf(newStartNode, newEndNode))
        }

        '?' -> {
            val subGraph = subGraphStack.last()
//            subGraphStack.removeAt(subGraphStack.lastIndex)
            val startNode = subGraph[0]
            val endNode = subGraph[1]
            Edge(null, startNode, endNode)
//            endNode.end = false
//
//            val newStartNode = Node()
//            val newEndNode = Node(end=true)
//            Edge(null, newStartNode, newEndNode)
//            Edge(null, newStartNode, startNode)
//            Edge(null, endNode, newEndNode)
//
//            subGraphStack.add(listOf(newStartNode, newEndNode))
        }

        '+' -> {
            //取出栈顶的一个子图做闭包
            val subGraph = subGraphStack.last()
//            subGraphStack.removeAt(subGraphStack.lastIndex)
            val startNode = subGraph[0]
            val endNode = subGraph[1]
            Edge(null, endNode, startNode)
//            endNode.end = false
//
//            val newStartNode = Node()
//            val newEndNode = Node(end=true)
//            Edge(null, startNode, endNode)
//            Edge(null, newStartNode, startNode)
//            Edge(null, endNode, newEndNode)
//
//            subGraphStack.add(listOf(newStartNode, newEndNode))
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

        if (token == '|'){
            isOp = true
            addCat = false
        }
        else if (token == '*' || token == '?' || token == '+'){
            isOp = true
        }
        else if (token == '('){
            if (addCat){
                //默认要与前一个补全"隐藏的"连接符
                val catOp = '-'
                while (!opStack.isEmpty() && operator_priority[opStack.last()]!! >= operator_priority[catOp]!!) {
                    val op = opStack.removeAt(opStack.lastIndex)
                    mergeSubGraph(op, subGraphStack)
                }
            }
            opStack.add(token)
            addCat = true
            continue
        }
        else if (token == ')'){
            while (opStack[opStack.lastIndex] != '('){
                val op = opStack[opStack.lastIndex]
                opStack.removeAt(opStack.lastIndex)
                mergeSubGraph(op, subGraphStack)
            }
            opStack.removeAt(opStack.lastIndex) //弹出'('
            addCat = true
        }
        else{
            isOp = false
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


//获得一个节点的epsilon闭包
fun epsClosure(node :Node) :MutableSet<Node>{
    val closureSet = mutableSetOf<Node>()
    val watingQueue = mutableListOf<Node>(node)

    while (!watingQueue.isEmpty()){

        val current  = watingQueue[0]
        watingQueue.removeAt(0)

        if (current !in closureSet){
            closureSet.add(current)
            for (outedge in current.outEdges){
                if (outedge.value == null){
                    watingQueue.add(outedge.endNode)
                }
            }

        }
    }

    return closureSet
}




//使用子集构造算法用于将NFA转换为DFA图
//返回DFA图的起点
fun nfa2DFA(nfaStart: Node): Node{
    val watingQueue = mutableListOf<Pair<MutableSet<Node>, Node>>()   //存放待处理的新DFA(集合)节点 to 抽象节点的队列
    var currentSet :MutableSet<Node>   //作为dfa起始子集
    val dfaSets  = mutableSetOf<MutableSet<Node>>(epsClosure(nfaStart)) //存放已经处理过的DFA(集合)节点

    val dfaStart: Node  = Node()//DFA节点虽然是子集但仍最后抽象成一个点, 这个节点也是最终返回的
    var currentDfaNode: Node   //同理,DFA中的一个节点

    watingQueue.add(epsClosure(nfaStart) to dfaStart)

    while (!watingQueue.isEmpty()) {
//        (currentSet, currentDfaNode) = watingQueue[0]
        currentSet = watingQueue[0].first
        currentDfaNode = watingQueue[0].second
        watingQueue.removeAt(0)
        //处理队列中的首个未处理dfa节点


        for (alpha in 1.toChar()..256.toChar()) {//******
            for (nfaNode in currentSet) {
                if (!nfaNode.nextNodes(alpha).isEmpty()) {

                    val newSet = mutableSetOf<Node>()
                    for (reachedNode in nfaNode.nextNodes(alpha)){
                        newSet.addAll(epsClosure(reachedNode))
                    }
                    val newNode = Node()
                    for (i in newSet) {
                        if (i.end)
                            newNode.end = true //这个dfa节点集中包含了原来nfa的结束节点的话那么抽象出来的dfa节点也是结束节点
                    }
                    Edge(alpha, currentDfaNode, newNode)

                    if (newSet !in dfaSets) {
                        dfaSets.add(newSet)
                        watingQueue.add(newSet to newNode)

                } else
                    continue

                }

            }
        }
    }

    return dfaStart
}

fun main(args: Array<String>) {
//    val re = readLine()
//    val graph = re2NFA(re!!)[0]
//    val n1 = graph[0]
//    val n2 = graph[1]
//    println(n1.statu)
//    println(n2.statu)
    val re  = readLine()!!
    val text: String? = readLine()
    if (text == null)
        return

    val nfaStartNode = re2NFA(re)[0][0]
    val dfaStartNode = nfa2DFA(nfaStartNode)
    var dfaNode: Node? = dfaStartNode

    var startPos = 0
    var endPos = 0

    while (endPos < text.length){
        if (dfaNode!!.nextNode(text[endPos]) != null){
            val c = text[endPos]
            endPos += 1
            dfaNode = dfaNode.nextNode(c)

        }
        else
            break
    }

    println(endPos)
}