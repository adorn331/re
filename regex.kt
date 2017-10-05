class Match(val text: String,val start: Int,val end: Int){
    override fun toString(): String {
        return "<Match(${this.start},${this.end + 1}): \"${text.subSequence(start, end + 1)}\">"
    }
}

//从首字母开始开始匹配，string如果包含pattern子串，则匹配成功，返回Match对象，失败则返回null，若要完全匹配，pattern要以$结尾
fun match(pattern:String, text: String?) :Match?{
    if (text == null)
        return null

    val matchHead = pattern[0] == '^' //标志是否有$修饰,需要匹配到开头
    val matchTail = pattern.last() == '$' //标志是否有$修饰,需要匹配到结尾
    var matchPattern :String  = pattern //存放真正要去匹配的表达式,除去^和$
    if (matchHead)
        matchPattern = matchPattern.substring(1, matchPattern.length)
    if (matchTail)
        matchPattern = matchPattern.substring(0, matchPattern.length - 1)


    val nfaStartNode = re2NFA(matchPattern)[0][0] //构造nfa图并拿到其头节点
    val dfaStartNode = nfa2DFA(nfaStartNode) //将nfa图转换为dfa图拿到dfa图头节点开始匹配状态
    var dfaNode: Node? = dfaStartNode  //匹配中不断跳转的dfa节点


    var startPos = 0
    var endPos = 0

    val matchStack = mutableListOf<Int>() //保存贪婪匹配时已经接受状态了的在字符串中的位置在栈中,方便回溯,然后继续向后匹配

    while (endPos < text.length){

        if (dfaNode != null){
            val nextNode = dfaNode.nextNode(text[endPos])

            if ( matchTail && nextNode == null )
                return null         //存在$需要匹配到底而存在一个字符不能跳转dfa状态, 直接match失败
            if (nextNode!= null && nextNode.end == true){
                matchStack.clear()
                matchStack.add(endPos) //压入已经匹配的位置方便以后回溯
            }
        }

        else
            break //有字符跳转状态,跳出循环回溯之前是否已经有匹配的地方

        dfaNode = dfaNode.nextNode(text[endPos])
        endPos += 1
    }

    return if (matchStack.isEmpty()) null else Match(text, startPos, matchStack.last())
}

//返回text中所有与pattern相匹配的全部字串，返回形式为一个Match对象的集合
fun findAll(pattern:String, text: String?): MutableSet<Match>?{
    if (text == null)
        return null

    //因为是搜索,所以忽略正则表达式中的^和$
    val matchHead = pattern[0] == '^'
    val matchTail = pattern.last() == '$'
    var matchPattern :String  = pattern //存放真正要去匹配的表达式,除去^和$
    if (matchHead)
        matchPattern = matchPattern.substring(1, matchPattern.length)
    if (matchTail)
        matchPattern = matchPattern.substring(0, matchPattern.length - 1)

    val nfaStartNode = re2NFA(matchPattern)[0][0] //构造nfa图并拿到其头节点
    val dfaStartNode = nfa2DFA(nfaStartNode) //将nfa图转换为dfa图拿到dfa图头节点开始匹配状态
    var dfaNode: Node? = dfaStartNode  //匹配中不断跳转的dfa节点

    var startPos = 0
    var endPos  = 0
    var i: Int
    val matches = mutableSetOf<Match>() //存放所有匹配的子串
    val matchStack = mutableListOf<Int>()

    while (endPos < text.length){
        //endPos = startPos
        dfaNode = dfaStartNode

        i = endPos
        while (i < text.length){
            if (dfaNode != null){
                val nextNode = dfaNode.nextNode(text[i])
                if (nextNode!= null && nextNode.end == true){
                    matchStack.clear()
                    matchStack.add(i) //压入已经匹配的位置方便以后回溯
                    endPos = i
                }
            }
            else
                break //有字符跳转状态,跳出循环回溯之前是否已经有匹配的地方
            dfaNode = dfaNode.nextNode(text[i])
            i += 1
        }

        if (!matchStack.isEmpty()) {
            matches.add(Match(text, startPos, matchStack.last()))
            matchStack.clear()
        }else{
            endPos += 1
        }
        startPos = endPos
    }

    return if (matches.isEmpty()) null else matches
}




fun main(args: Array<String>) {

    while (true) {
        val re = readLine()!!
        val text: String? = readLine()

//        val m = match(re, text)
//        println(m)

        val a = findAll(re, text)
        println(a)

    }
}