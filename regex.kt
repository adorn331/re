class Match(val text: String,val start: Int,val end: Int){
    override fun toString(): String {
        return "<Match(${this.start},${this.end}): match=\"${text.subSequence(start, end)}\">"
    }
}

//从首字母开始开始匹配，string如果包含pattern子串，则匹配成功，返回Match对象，失败则返回null，若要完全匹配，pattern要以$结尾
fun match(pattern:String, text: String?) :Match?{
    val compiled = compile(pattern) //编译正则表达式

    return compiled.match(text) //编译后再match
}

//返回text中所有与pattern相匹配的全部字串，返回形式为一个Match对象的集合
fun findAll(pattern:String, text: String?): MutableSet<Match>?{
    val compiled = compile(pattern)

    return compiled.findAll(text)
}


//若text中包含pattern子串，有则返回Match对象，否则返回None，如果text中存在多个pattern子串，只返回第一个
fun search(pattern:String, text: String?): Match?{
    val compiled = compile(pattern)

    return compiled.search(text)
}

//替换text中所有与pattern相匹配的全部字串替换为replacement,返回一个替换后的字符串
fun replace(pattern:String, replacement: String,text: String?): String?{
    val compiled = compile(pattern)

    return compiled.replace(replacement, text)
}

//编译正则表达式,返回一个CompiledRe类,里面已经编译构造好dfa图并且封装了各种match findAll等等方法能对编译好了的正则匹配
fun compile(pattern: String): CompiledRe{
    return CompiledRe(pattern)
}


class CompiledRe(val pattern: String){

    val dfaStartNode: Node   //匹配中不断跳转的dfa节点

    val matchHead = pattern[0] == '^' //标志是否有$修饰,需要匹配到开头
    val matchTail = pattern.last() == '$' //标志是否有$修饰,需要匹配到结尾

    init {
        var matchPattern :String  = pattern //存放真正要去匹配的表达式,除去^和$
        if (matchHead)
            matchPattern = matchPattern.substring(1, matchPattern.length)
        if (matchTail)
            matchPattern = matchPattern.substring(0, matchPattern.length - 1)

        val nfaStartNode = re2NFA(matchPattern) //构造nfa图并拿到其头节点
        dfaStartNode = nfa2DFA(nfaStartNode) //将nfa图转换为dfa图拿到dfa图头节点开始匹配状态
    }

    //从首字母开始开始匹配，string如果包含pattern子串，则匹配成功，返回Match对象，失败则返回null，若要完全匹配，pattern要以$结尾
    fun match(text: String?) :Match?{
        var dfaNode: Node? = dfaStartNode  //匹配中不断跳转的dfa节点

        if (text == null)
            return null

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

        return if (matchStack.isEmpty()) null else Match(text, startPos, matchStack.last() + 1)
    }

    //返回text中所有与pattern相匹配的全部字串，返回形式为一个Match对象的集合
    fun findAll(text: String?): MutableSet<Match>?{
        if (text == null)
            return null

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
                matches.add(Match(text, startPos, matchStack.last() + 1))
                matchStack.clear()
            }else{
                endPos += 1
            }
            if (i == text.length)
                break
            startPos = endPos
        }

        return if (matches.isEmpty()) null else matches
    }

    //若text中包含pattern子串，有则返回Match对象，否则返回None，如果text中存在多个pattern子串，只返回第一个
    fun search(text: String?): Match?{
        if (text == null)
            return null

        var dfaNode: Node? = dfaStartNode  //匹配中不断跳转的dfa节点

        var startPos = 0
        var endPos  = 0
        var i: Int

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
                return Match(text, startPos, matchStack.last() + 1)
            }else{
                endPos += 1
            }
            startPos = endPos
        }

        return null
    }

    //替换text中所有与pattern相匹配的全部字串替换为replacement,返回一个替换后的字符串
    fun replace(replacement: String, text: String?): String?{
        if (text == null)
            return null

        val matches = findAll(pattern, text)
        if (matches == null)//没有可以替换的
            return text

        var newText: String = text

        for (match in matches) {
            val matchedSubStr = text.subSequence(match.start, match.end).toString() //text中匹配的子串
            newText = newText.replace(matchedSubStr, replacement)
        }

        return newText
    }
}