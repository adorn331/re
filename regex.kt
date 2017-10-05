
class Match(val text: String,val start: Int,val end: Int){
    override fun toString(): String {
        return "<Match(${this.start},${this.end + 1}): \"${text.subSequence(start, end + 1)}\">"
    }
}


//从首字母开始开始匹配，string如果包含pattern子串，则匹配成功，返回Match对象，失败则返回null，若要完全匹配，pattern要以$结尾
fun match(pattern:String, text: String?) :Match?{
    if (text == null)
        return null

    val nfaStartNode = re2NFA(pattern)[0][0] //构造nfa图并拿到其头节点
    val dfaStartNode = nfa2DFA(nfaStartNode) //将nfa图转换为dfa图拿到dfa图头节点开始匹配状态
    var dfaNode: Node? = dfaStartNode  //匹配中不断跳转的dfa节点


    var startPos = 0
    var endPos = 0

    val matchStack = mutableListOf<Int>() //保存贪婪匹配时已经接受状态了的在字符串中的位置在栈中,方便回溯,然后继续向后匹配

    while (endPos < text.length){

        if (dfaNode != null){
            val nextNode = dfaNode.nextNode(text[endPos])

            if (nextNode!= null && nextNode.end == true){
                matchStack.clear()
                matchStack.add(endPos) //压入已经匹配的位置方便以后回溯,
            }
        }

        else
            break//现字符在dfa中没这条跳转边,只能回溯查看栈中是否有之前已经匹配成功的


        dfaNode = dfaNode.nextNode(text[endPos])
        endPos += 1
    }


    return if (matchStack.isEmpty()) null else Match(text, startPos, matchStack.last())

}




fun main(args: Array<String>) {

    while (true) {
        val re = readLine()!!
        val text: String? = readLine()

        val m = match(re, text)
        println(m)

    }
}