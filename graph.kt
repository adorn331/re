import kotlin.concurrent.fixedRateTimer

val operator_priority = mapOf('(' to -1, '[' to -1,'{' to -1, '|' to 1, '-' to 2, '*' to 3, '?' to 3, '+' to 3, '.' to 0)
// 三种基本的符号：或 连接 闭包

val escapeTokensMap =  mapOf('.' to 1.toChar(), 'w' to 2.toChar(), 'd' to 3.toChar(), 's' to 4.toChar()) //转义字符集
//为了优化性能不转换为(a|b|c....|y|z|),将转义字符集合映射为一些不可见ascii字符保存在边上, 然后节点跳转的时候做特殊判断处理.

val sEscapeSet = setOf('\t', '\n', ' ')

class Node(val inEdges: MutableList<Edge> = mutableListOf<Edge>(),
           val outEdges: MutableList<Edge> = mutableListOf<Edge>(), var end: Boolean=false){

    //查看这个nfa节点通过非epsilon边可以到达的节点集合
    fun nextNodes (v: Char) :MutableSet<Node>{

        val nodeList = mutableSetOf<Node>()
        for (edge in this.outEdges) {
            if (edge.value != null && edge.value == v )
                nodeList.add(edge.endNode)
        }

        return nodeList
    }

    //用于dfa图中的转移状态
    fun nextNode (v: Char) :Node?{
        for (edge in this.outEdges) {
            if (edge.value != null && (edge.value == v))
                return edge.endNode
        }
        //是转义字符的时候跳转,跳转映射到特定的转义后集合上
        for (edge in this.outEdges){
            if (edge.value != null && edge.value == escapeTokensMap['.'])
                return edge.endNode
            if (edge.value != null && edge.value == escapeTokensMap['d'] && v in '1'..'9')
                return edge.endNode
            if (edge.value != null && edge.value == escapeTokensMap['w'] && (v in 'a'..'z' || v in 'A'..'Z'))
                return edge.endNode
            if (edge.value != null && edge.value == escapeTokensMap['s'] && v in sEscapeSet)
                return edge.endNode
        }
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

            endNode1.end = startNode1.end
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
            Edge(null, endNode1, newEndNode)
            Edge(null, endNode2, newEndNode)

            subGraphStack.add(listOf(newStartNode, newEndNode))
        }

        '*' -> {
            //取出栈顶的一个子图做闭包
            val subGraph = subGraphStack.last()
            val startNode = subGraph[0]
            val endNode = subGraph[1]

            Edge(null, startNode, endNode)
            Edge(null, endNode, startNode)
        }

        '?' -> {
            val subGraph = subGraphStack.last()
            val startNode = subGraph[0]
            val endNode = subGraph[1]
            Edge(null, startNode, endNode)

        }

        '+' -> {
            val subGraph = subGraphStack.last()
            val startNode = subGraph[0]
            val endNode = subGraph[1]
            Edge(null, endNode, startNode)
        }
    }
}

//根据一个值,构造只有一个开始节点一个结束节点和一条转移边的最基本nfa子图(后面用于各种子图合并)
fun createSingleSubGraph(value: Char, subGraphStack: MutableList<List<Node>>){
        val startNode = Node()
        val endNode = Node(end=true)
        Edge(value, startNode, endNode)
        subGraphStack.add(listOf(startNode, endNode))
}


//用于将一个新的"操作符"压入栈中
//根据类似于逆波兰表达式原理, 栈顶的优先级应该比栈底的高,否则一直弹出栈内操作符直到可以放入位置
fun push_op(operator: Char, opStack: MutableList<Char>, subGraphStack: MutableList<List<Node>>){
        while (!opStack.isEmpty() && operator_priority[opStack.last()]!! >= operator_priority[operator]!!) {
            val op = opStack.removeAt(opStack.lastIndex)
            mergeSubGraph(op, subGraphStack)
        }
        opStack.add(operator)
}

//正则表达式转换为NFA图
fun re2NFA(pattern: String) :Node{
    val subGraphStack: MutableList<List<Node>> = mutableListOf() //存放子图
    val opStack: MutableList<Char> = mutableListOf()    //存放字符间的连接符号
    var isOp = false
    //var isFirst  = true
    var inBracket = false //标志是否在[]当中, 是的话字符间的连接不应该默认是'-'而应该默认是'|'
    var addCat = false //确认是否需要补上字符间默认的连接符'-'

    var i = 0
    while (i < pattern.length){
        val token = pattern[i]

        if (token == '|'){
            isOp = true
            addCat = false
        }
        else if (token == '*' || token == '?' || token == '+'){
            isOp = true
        }
        else if (token == '('){
            if (addCat)
                //补充此字符与前一个字符默认"隐藏的"连接符
                push_op(if (!inBracket) '-' else '|', opStack, subGraphStack)
            opStack.add(token)

            addCat = false
            i += 1
            continue
        }
        else if (token == ')'){
            while (opStack[opStack.lastIndex] != '('){
                val op = opStack.removeAt(opStack.lastIndex)
                mergeSubGraph(op, subGraphStack)
            }
            opStack.removeAt(opStack.lastIndex) //弹出'('
            addCat = true
            i += 1
            continue
        }

        //使用类似与()的处理方法
        //思路:将[abc] 处理为 (a|b|c)
        else if (token == '['){
            if (addCat)
                //补充此字符与前一个字符默认"隐藏的"连接符
                push_op('-', opStack, subGraphStack)
            opStack.add(token)
            addCat = false
            inBracket = true
            i += 1
            continue
        }

        else if (token == ']'){
            while (opStack[opStack.lastIndex] != '['){
                val op = opStack.removeAt(opStack.lastIndex)
                mergeSubGraph(op, subGraphStack)
            }
            opStack.removeAt(opStack.lastIndex) //弹出'['
            addCat = true
            inBracket = false
            i += 1
            continue
        }

        //处理[]括号中的'-',例如[a-z] [0-9]
        //思路:处理为 (0|1|2|3|4|5..|9)
        else if (token == '-' && inBracket){
            val start = pattern[i - 1].toInt()
            val end = pattern[i + 1].toInt()

            for (current in start + 1..end - 1){
                val ch = current.toChar()
                push_op('|', opStack, subGraphStack)
                createSingleSubGraph(ch, subGraphStack)
            }

            addCat = true
            i += 1
            continue
        }

        //原思路:'.' 转化为(`|.|a..|b|...|0|..) 将所有的支持字符并联
        //新思路:'.'映射为一些特殊不可见ascii字符值保存在边上, 然后节点跳转的时候遇到了这个特殊值默认所有给出的字符都可以跳转.
        else if (token == '.'){
            if (addCat)
            //补充此字符与前一个字符默认"隐藏的"连接符
                push_op(if (!inBracket) '-' else '|', opStack, subGraphStack)
            //单个字符构造最基本的子图
            createSingleSubGraph(escapeTokensMap['.']!!, subGraphStack)
            addCat = true

            i += 1
            continue
        }


        //旧思路:'\d'转换为(0|1|...9)等
        //新思路:'.'映射为一些特殊不可见ascii字符值保存在边上, 然后节点跳转的时候遇到了这个特殊值默认所有给出的字符如果满足特定条件(例如\d需要满足0-9)都可以跳转.
        //避免像一开始思路转换为(0|1|...9)和(a|b|c|d|e..|z)太大开销
        else if (token == '\\'){

            if (pattern[i + 1] in escapeTokensMap.keys){
                //发生转义,转义到另一些字符集和,所以要将d w s等字符不当做普通字符,映射为另一些特殊字符保存到边上

                if (addCat)
                    //补充此字符与前一个字符默认"隐藏的"连接符
                    push_op(if (!inBracket) '-' else '|', opStack, subGraphStack)

                createSingleSubGraph(escapeTokensMap[pattern[i + 1]]!!, subGraphStack)
                addCat = true
                i += 2
                continue

            }
            else if (pattern[i + 1] in operator_priority.keys){   //反斜杠用于转义操作符号, 将其当做单个普通字符处理不起特殊作用
                if (addCat)
                    push_op(if (!inBracket) '-' else '|', opStack, subGraphStack)
                //单个字符构造最基本的子图
                createSingleSubGraph(pattern[i + 1], subGraphStack)
                addCat = true

                i += 2 //跳过它以免发生修饰符作用
                continue
            }
            else{ //反斜杠无任何效果, 当做普通字符匹配处理
                isOp = false
            }
            /*若需跳过其不匹配, 则应该是
            else{
                i += 1
                continue
            }
             */
        }

        else if (token == '{'){
            //{n}的情况,转化为nfa子图中n个串联 例如a{3} --> aaa  \d{3} --> \d\d\d
            if (pattern[i+2] == '}'){

            val  repeatChar: Char
            if (i >= 2 && pattern[i - 2] == '\\' && pattern[i - 1] in escapeTokensMap.keys)
                    repeatChar = escapeTokensMap[pattern[i - 1]]!! //需要重复的是转义字符
            else
                    repeatChar = pattern[i - 1]
            val repeatTime = pattern[i + 1].toInt() - '0'.toInt()
            opStack.add('{')
            subGraphStack.removeAt(subGraphStack.lastIndex)

            for (j in 1..repeatTime) {
                createSingleSubGraph(repeatChar, subGraphStack)
                if (j != repeatTime)
                    push_op('-', opStack, subGraphStack)
            }

            while (opStack.last() != '{') {
                val op = opStack.removeAt(opStack.lastIndex)
                mergeSubGraph(op, subGraphStack)
            }

            opStack.removeAt(opStack.lastIndex) //移除刚刚添加进去的'{'
            addCat = true
            i += 3
            continue

            }

            //对应情况{n,m}
            //思路示意: a{1,3} --> (a|aa|aaa) \w --> (\w|\w\w|\w\w\w)
            else if (pattern[i + 2] == ',' && pattern[i + 3] != '}'){

                val minReapt = pattern[i + 1].toInt() - '0'.toInt()
                val maxReapt = pattern[i + 3].toInt() - '0'.toInt()
                val  repeatChar: Char
                if (i >= 2 && pattern[i - 2] == '\\' && pattern[i - 1] in escapeTokensMap.keys)
                    repeatChar = escapeTokensMap[pattern[i - 1]]!!  //需要重复的是转义字符
                else
                    repeatChar = pattern[i - 1]

                subGraphStack.removeAt(subGraphStack.lastIndex)
                opStack.add('(') //压入'(', 方便转换为(||||)形式

                for (repeatTime in minReapt..maxReapt) {
                    //先生成成类似于aaa
                    opStack.add('(')
                    for (j in 1..repeatTime) {
                        createSingleSubGraph(repeatChar, subGraphStack)
                        if (j != repeatTime)
                            push_op('-', opStack, subGraphStack)
                    }
                    while (opStack.last() != '(') {
                        val op = opStack.removeAt(opStack.lastIndex)
                        mergeSubGraph(op, subGraphStack)
                    }
                    opStack.removeAt(opStack.lastIndex) //移除刚刚添加进去的'('
                    //再将aaa与前面的aa相连接 变成a|aa|aaa
                    if (repeatTime != maxReapt) {
                        push_op('|', opStack, subGraphStack)
                    }
                }

                while (opStack.last() != '(') {
                    val op = opStack.removeAt(opStack.lastIndex)
                    mergeSubGraph(op, subGraphStack)
                }
                opStack.removeAt(opStack.lastIndex) //移除刚刚添加进去的'('
                addCat = true
                i += 5
                continue

            }

            //对应情况{n,}
            //思路示意: a{2,} --> aa+  \w{2,} --> \w\w+
            else if (pattern[i + 2] == ',' && pattern[i + 3] == '}'){

                val  repeatChar: Char
                if (i >= 2 && pattern[i - 2] == '\\' && pattern[i - 1] in escapeTokensMap.keys)
                    repeatChar = escapeTokensMap[pattern[i - 1]]!!
                else
                    repeatChar = pattern[i - 1]
                val minRepeat = pattern[i + 1].toInt() - '0'.toInt()
                opStack.add('{')
                subGraphStack.removeAt(subGraphStack.lastIndex)

                for (j in 1..minRepeat) {
                    createSingleSubGraph(repeatChar, subGraphStack)
                    if (j != minRepeat)
                        push_op('-', opStack, subGraphStack)
                }

                push_op('+', opStack, subGraphStack)

                while (opStack.last() != '{') {
                    val op = opStack.removeAt(opStack.lastIndex)
                    mergeSubGraph(op, subGraphStack)
                }

                opStack.removeAt(opStack.lastIndex) //移除刚刚添加进去的'{'
                addCat = true
                i += 4
                continue

            }
        }

        else{
            //这个不是其他特殊符号,是单个文本字符
            isOp = false
        }

        //中缀表达式与后缀表达式原理, 例如逆波兰表达式
        if (isOp)
            push_op(token, opStack, subGraphStack)


        else{//是单个字符
            if (addCat)
                //补充此字符与前一个字符默认"隐藏的"连接符
                push_op(if (!inBracket) '-' else '|', opStack, subGraphStack)
            //单个字符构造最基本的子图
            createSingleSubGraph(token, subGraphStack)
            addCat = true

        }
        i += 1
    }

    //将符号栈内所剩的符号应用后清空栈
    while (!opStack.isEmpty()){
        val op = opStack.removeAt(opStack.lastIndex)
        mergeSubGraph(op, subGraphStack)
    }

    return subGraphStack[0][0] //
}


//获得一个nfa节点的epsilon闭包, 服务于子集构造算法
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

//获得一个nfa节点集合的epsilon闭包
fun epsClosureSet(nfaSet: MutableSet<Node>) :MutableSet<Node>{
    val closureSets = mutableSetOf<Node>()
    for (nfaNode in nfaSet)
        closureSets.addAll(epsClosure(nfaNode))

    return closureSets
}

//一个nfa节点集合能够通过value移动到的节点集合
fun move(nfaSet: MutableSet<Node>, value: Char) :MutableSet<Node>{
    val moveSet = mutableSetOf<Node>()
    for (nfaNode in nfaSet)
        moveSet.addAll(nfaNode.nextNodes(value))
    return moveSet
}

//使用子集构造算法用于将NFA转换为DFA图
//返回DFA图的起点
fun nfa2DFA(nfaStart: Node): Node{
    val watingQueue = mutableListOf<Pair<MutableSet<Node>, Node>>()   //存放待处理的新DFA(集合)节点 to 抽象节点的队列
    val exitDfaSets = mutableSetOf<MutableSet<Node>>()
    val exitDfaNodes  = mutableSetOf<Pair<MutableSet<Node>, Node>>() //存放已经处理过的DFA(集合)节点 to 抽象节点的pair

    var currentSet :MutableSet<Node>   //作为dfa起始子集
    var currentDfaNode: Node   //DFA中的一个抽象出的节点

    val dfaStart  = Node()//DFA节点虽然是子集但仍最后抽象成一个点, 这个节点也是最终返回的值
    watingQueue.add(epsClosure(nfaStart) to dfaStart)

    while (!watingQueue.isEmpty()) {
        currentSet = watingQueue[0].first
        currentDfaNode = watingQueue[0].second
        watingQueue.removeAt(0)
        //处理队列中的首个未处理dfa节点
        exitDfaNodes.add(currentSet to currentDfaNode)
        exitDfaSets.add(currentSet)

        //当前dfa集中包含了结束状态的nfa节点时,它也应该是结束状态
        for (nfaNode in currentSet){
            if (nfaNode.end) {
                currentDfaNode.end = true
            }
        }


        val alphaSet = mutableSetOf<Char>()     //存放可以到达的字符
        for (alpha in 32.toChar()..126.toChar()){   //支持所有可见ASCII字符集
            for (nfaNode in currentSet){
                if (!nfaNode.nextNodes(alpha).isEmpty())
                    alphaSet.add(alpha)
            }
        }
        for (alpha in 1.toChar()..4.toChar()){   //支持所有可见ASCII字符集
            for (nfaNode in currentSet){
                if (!nfaNode.nextNodes(alpha).isEmpty())
                    alphaSet.add(alpha)
            }
        }

        var newSet: MutableSet<Node>
        for (alpha in alphaSet){
            newSet = epsClosureSet(move(currentSet, alpha))
            if (newSet !in exitDfaSets) {
                val newNode = Node()
                Edge(alpha, currentDfaNode, newNode)
                watingQueue.add(newSet to newNode)
            }
            else{
                for (i in exitDfaNodes){
                    if (i.first == newSet) {
                        Edge(alpha, currentDfaNode, i.second)
                        break
                    }
                }
            }
        }

    }

    return dfaStart
}