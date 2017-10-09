/**包含正则表达式引擎将正则表达式转换为nfa再转换到dfa图的所需数据结构和函数*/

val operator_priority = mapOf('(' to -1, '[' to -1,'{' to -1, '|' to 1, '-' to 2, '*' to 3, '?' to 3, '+' to 3, '.' to 0)
// 为了满足将"操作符"(其实是限定符修饰符) 压栈需要,要有优先级.但'.'的存在只是为了后面判断是不是转义\.判断它要变成普通的.

val escapeTokensMap =  mapOf('.' to 1.toChar(), 'w' to 2.toChar(), 'd' to 3.toChar(), 's' to 4.toChar()) //转义字符集
//处理'.' \d \w等时候为了优化性能不转换为(a|b|c....|y|z|),将转义字符集合映射为一些不可见ascii字符保存在边上, 然后节点跳转的时候做特殊判断处理.

val sEscapeSet = setOf('\t', '\n', ' ')

/**
 * 存放nfa/dfa图的节点的结构
 *
 * attr:
 *      inEdges:入边集合
 *      outEdegs:出边集合
 *      end:标识是否为结束状态
 *      startOfGroup:用于()分组的时候标识是哪些组的开始
 *      endOfGroup:用于()分组的时候标识是哪些组的结束
 *
 * method:
 *      nextNodes:查看这个nfa节点通过非epsilon边可以到达的节点集合
 *      nextNode:用于dfa图中的转移状态,返回跳转后的下一个dfa节点
 */
class Node(val inEdges: MutableList<Edge> = mutableListOf(),
           val outEdges: MutableList<Edge> = mutableListOf(), var end: Boolean=false){

    val startOfGroup = mutableSetOf<Int>()
    val endOfGroup = mutableSetOf<Int>()

    //查看这个nfa节点通过非epsilon边可以到达的节点集合
    fun nextNodes (v: Char) :MutableSet<Node>{

        val nodeList = mutableSetOf<Node>()
        for (edge in this.outEdges.filter { it.value != null && it.value == v })
                nodeList.add(edge.endNode)

        return nodeList
    }

    /**用于dfa图中的转移状态*/
    fun nextNode (v: Char) :Node?{
        for (edge in this.outEdges) {
            if (edge.value != null && (edge.value == v))
                return edge.endNode
        }
        //是转义字符的时候跳转,跳转映射到特定的转义后集合上
        for (edge in this.outEdges){
            if (edge.value != null && edge.value == escapeTokensMap['.'])
                return edge.endNode
            if (edge.value != null && edge.value == escapeTokensMap['d'] && v in '0'..'9')
                return edge.endNode
            if (edge.value != null && edge.value == escapeTokensMap['w'] && (v in 'a'..'z' || v in 'A'..'Z'))
                return edge.endNode
            if (edge.value != null && edge.value == escapeTokensMap['s'] && v in sEscapeSet)
                return edge.endNode
        }
        return null
    }
}

/**
 * 用于表示nfa/dfa图中的边结构
 *
 * param:
 *      value:边上的跳转值
 *      startNode:边的起始节点
 *      endNode:边的结束节点
 *
 * note:
 *      初始化的时候会自动将边加到起始节点和结束节点的数据结构中
 */
class Edge(val value: Char?, var startNode: Node, var endNode: Node){
    init {
        startNode.outEdges.add(this)
        endNode.inEdges.add(this)
    }
}


/**
 * 根据一个字符值,构造只有一个开始节点一个结束节点和一条转移边的最基本nfa子图(后面用于各种子图合并)
 *
 * param:
 *      value: 构造的最小子图边上的值
 *      subGraphStack:存放子图的栈(后面用于不断弹出最顶子图,合并子图最构造成nfa图)
 *
 */
fun createSingleSubGraph(value: Char, subGraphStack: MutableList<List<Node>>){
    val startNode = Node()
    val endNode = Node(end=true)
    Edge(value, startNode, endNode)
    subGraphStack.add(listOf(startNode, endNode))
}



/**
 * 合并基本的nfa子图
 *
 * param:
 *      operator:合并subGraphStack最顶的两个子图所用的的"操作符",'|'代表并联,'-'代表串联,'*'代表闭包.还有'?' '+' 等等
 *      subGraphStack:将弹出后处理成新的子图再压回这个栈中
 */
fun mergeSubGraph(operator: Char, subGraphStack: MutableList<List<Node>>){

    when(operator){
        //串联两个子图
        '-' -> {
            //取出两个栈顶的子图
            val subGraph2 = subGraphStack.last()
            subGraphStack.removeAt(subGraphStack.lastIndex)

            val startNode2 = subGraph2[0]
            val endNode2 = subGraph2[1]
            val subGraph1 = subGraphStack.last()
            subGraphStack.removeAt(subGraphStack.lastIndex)
            val startNode1 = subGraph1[0]
            val endNode1 = subGraph1[1]

            endNode1.end = startNode1.end
            //给前一个子图的结束节点加条epsilon边到后一个子图的起始节点
            Edge(null, endNode1, startNode2)
            subGraphStack.add(listOf(startNode1, endNode2))
        }

        //并联两个子图
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
            //申请两个新节点作为并联后子图的起始节点与终止节点
            val newStartNode = Node()
            val newEndNode = Node(end=true)
            //加四条epsilon边达到并联效果
            Edge(null, newStartNode, startNode1)
            Edge(null, newStartNode, startNode2)
            Edge(null, endNode1, newEndNode)
            Edge(null, endNode2, newEndNode)

            subGraphStack.add(listOf(newStartNode, newEndNode))
        }

        //取出栈顶的一个子图做闭包
        '*' -> {
            val subGraph = subGraphStack.last()
            val startNode = subGraph[0]
            val endNode = subGraph[1]
            //加一条epsilon边从起始节点指向终止节点,再加一条epsilon边从终止节点指向起始节点
            Edge(null, startNode, endNode)
            Edge(null, endNode, startNode)
        }

        //取出栈顶的一个子图做类似于闭包的操作
        '?' -> {
            val subGraph = subGraphStack.last()
            val startNode = subGraph[0]
            val endNode = subGraph[1]
            //加一条epsilon边从起始节点指向终止节点,因为是经过0次或一次
            Edge(null, startNode, endNode)

        }

        //取出栈顶的一个子图做类似于闭包的操作
        '+' -> {
            val subGraph = subGraphStack.last()
            val startNode = subGraph[0]
            val endNode = subGraph[1]
            //加一条epsilon边从终止节点指向终止节点,因为是经过至少一次
            Edge(null, endNode, startNode)
        }
    }
}


/**
 * 将一个新的"操作符"压入栈中,方便后面弹出用于合并nfa子图
 *
 * param:
 *      op:合并subGraphStack最顶的两个子图所用的的"操作符",'|'代表并联,'-'代表串联,'*'代表闭包.还有'?' '+' 等等
 *      subGraphStack:压入操作符可能有需要合并子图的情况,若需要则将弹出后的子图处理成新的子图再压回这个栈中
 *
 * note:
 *      栈顶的操作符优先级应该比栈底的都高才能压入
 *      否则一直弹出栈内操作符进行合并子图操作直到可以放入位置
 */
fun pushOp(operator: Char, opStack: MutableList<Char>, subGraphStack: MutableList<List<Node>>){
        while (!opStack.isEmpty() && operator_priority[opStack.last()]!! >= operator_priority[operator]!!) {
            val op = opStack.removeAt(opStack.lastIndex)
            mergeSubGraph(op, subGraphStack)
        }
        opStack.add(operator)
}


/**
 * 正则表达式转换为NFA图
 *
 * param:
 *      pattern:用于转换为NFA图的正则表达式模板
 *       * return:
 *      转换后NFA图的起始节点
 * note:
 *      使用了两个重要的栈进行构造整个NFA图:一个栈主要用于存放当时已经合并好的NFA子图
 *      另一个用于存放合并这些NFA子图所需要的"操作符"
 *      最开始就基本的子图都是通过单个字符构成的,然后不停合并最终成为NFA图
 *      思想参照龙书:正则表达式都是由最基本的几种正则表达式递归组成的,那么NFA子图也应该是由许多最基本的子图不断合并成的
 */
fun re2NFA(pattern: String) :Node{
    val subGraphStack: MutableList<List<Node>> = mutableListOf() //存放子图
    val opStack: MutableList<Char> = mutableListOf()    //存放字符间的连接符号
    var isOp = false
    //var isFirst  = true
    var inBracket = false //标志是否在[]当中, 是的话字符间的连接不应该默认是'-'而应该默认是'|'
    var addCat = false //确认是否需要补上字符间默认的连接符'-'

    val groupIdStack = mutableListOf<Int>() //保存组别序号的栈,用于组开始时压入'(',有')' 配对时候弹出
    var groupId = 0

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

        //标记开始括号,要注意分组信息,并且压入'('方便优先处理括号里面的子图
        else if (token == '('){
            groupId += 1 //新的一个组
            groupIdStack.add(groupId)

            if (addCat)
                //补充此字符与前一个字符默认"隐藏的"连接符
                pushOp(if (!inBracket) '-' else '|', opStack, subGraphStack)
            opStack.add(token)

            addCat = false
            i += 1
            continue
        }
        //括号结束,优先将括号内的表达式合成成为一个子图,然后弹出opStack里的占位符'('
        else if (token == ')'){
            val currentGroupId = groupIdStack.removeAt(groupIdStack.lastIndex)

            while (opStack[opStack.lastIndex] != '('){
                val op = opStack.removeAt(opStack.lastIndex)
                mergeSubGraph(op, subGraphStack)
            }
            opStack.removeAt(opStack.lastIndex) //弹出'('

            val subGraph = subGraphStack.last()
            val startNode = subGraph[0]
            val endNode = subGraph[1]

            startNode.startOfGroup.add(currentGroupId)
            endNode.endOfGroup.add(currentGroupId)

            addCat = true
            i += 1
            continue
        }

        //使用类似与()的处理方法
        //思路:将[abc] 处理为 (a|b|c)
        else if (token == '['){
            if (addCat)
                //补充此字符与前一个字符默认"隐藏的"连接符
                pushOp('-', opStack, subGraphStack)
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

            for (current in start + 1 until end){
                val ch = current.toChar()
                pushOp('|', opStack, subGraphStack)
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
                pushOp(if (!inBracket) '-' else '|', opStack, subGraphStack)
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
                    pushOp(if (!inBracket) '-' else '|', opStack, subGraphStack)

                createSingleSubGraph(escapeTokensMap[pattern[i + 1]]!!, subGraphStack)
                addCat = true
                i += 2
                continue

            }
            else if (pattern[i + 1] in operator_priority.keys){   //反斜杠用于转义操作符号, 将其当做单个普通字符处理不起特殊作用
                if (addCat)
                    pushOp(if (!inBracket) '-' else '|', opStack, subGraphStack)
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
                    pushOp('-', opStack, subGraphStack)
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
                            pushOp('-', opStack, subGraphStack)
                    }
                    while (opStack.last() != '(') {
                        val op = opStack.removeAt(opStack.lastIndex)
                        mergeSubGraph(op, subGraphStack)
                    }
                    opStack.removeAt(opStack.lastIndex) //移除刚刚添加进去的'('
                    //再将aaa与前面的aa相连接 变成a|aa|aaa
                    if (repeatTime != maxReapt) {
                        pushOp('|', opStack, subGraphStack)
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
                        pushOp('-', opStack, subGraphStack)
                }

                pushOp('+', opStack, subGraphStack)

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
            pushOp(token, opStack, subGraphStack)


        else{//是单个字符
            if (addCat)
                //补充此字符与前一个字符默认"隐藏的"连接符
                pushOp(if (!inBracket) '-' else '|', opStack, subGraphStack)
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

    val nfaStartNode = subGraphStack[0][0] //subGraphStack[0]子图栈顶的子图,也就是最后的nfa图,返回nfa图的起始节点
    val nfaEndNode = subGraphStack[0][1]

    //因为整个字符串都是第0组的
    nfaStartNode.startOfGroup.add(0)
    nfaEndNode.endOfGroup.add(0)

    return nfaStartNode //返回nfa图的起始节点
}


/**
 * 获得一个nfa节点的epsilon闭包, 服务于子集构造算法,思想参照龙书
 *
 * param:
 *      node:需要构造epsilon闭包的nfa节点
 * return:
 *      此nfa节点通过epsilon边能够到达的节点集合
 */
fun epsClosure(node :Node) :MutableSet<Node>{
    val closureSet = mutableSetOf<Node>()
    val watingQueue = mutableListOf(node)

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

/**
 * 获得一个nfa节点的集合的epsilon闭包, 服务于子集构造算法,思想参照龙书
 *
 * param:
 *      nfaSet:需要构造epsilon闭包的nfa节点的集合
 * return:
 *      这些nfa节点通过epsilon边能够到达的节点集合
 */
fun epsClosureSet(nfaSet: MutableSet<Node>) :MutableSet<Node>{
    val closureSets = mutableSetOf<Node>()
    for (nfaNode in nfaSet)
        closureSets.addAll(epsClosure(nfaNode))

    return closureSets
}

/**
 * 获得一个nfa节点集合能够通过value移动到的节点集合
 *
 * param:
 *      nfaSet:起始的nfa节点集合
 *      value: 跳转边上的值
 * return:
 *      这些nfa节点通过值为value的边能够到达的节点集合
 */
fun move(nfaSet: MutableSet<Node>, value: Char) :MutableSet<Node>{
    val moveSet = mutableSetOf<Node>()
    for (nfaNode in nfaSet)
        moveSet.addAll(nfaNode.nextNodes(value))
    return moveSet
}

/**
 * 使用子集构造算法用于将NFA转换为DFA图,思想参照龙书
 *
 * param:
 *      nfaStart:NFA图的起始节点
 * return:
 *      返回DFA图的起点
 */
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
            currentDfaNode.startOfGroup.addAll(nfaNode.startOfGroup)
            currentDfaNode.endOfGroup.addAll(nfaNode.endOfGroup)

        }

        val alphaSet = mutableSetOf<Char>()     //存放可以到达的字符

        for (alpha in 32.toChar()..126.toChar()){   //支持所有可见ASCII字符集
            for (nfaNode in currentSet){
                if (!nfaNode.nextNodes(alpha).isEmpty())
                    alphaSet.add(alpha)
            }
        }
        for (alpha in 1.toChar()..4.toChar()){   //查看是否有到达转义字符
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
