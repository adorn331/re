/**包含用于测试此正则引擎的函数入口**/


/**交互测试此正则引擎所有接口功能..
 *首先选择需要测试的正则函数1.match 2.findAll 3.search 4.replace 5.Match.group 6.Match.groups
 *然后输入一个正则表达式,然后不断输入字符串对这个正则表达式进行匹配测试是否符合预想
 *其中输入\b有返回上一层功能,可以进行测试新的正则表达式/切换测试函数
*/
fun main(args: Array<String>) {
    while (true) {
        println("\n选择需要测试函数的序号..")
        println("1.match 2.findAll 3.search \n4.replace 5.Match.group 6.Match.groups() (输入\\q to quit)")
        val testMod  = readLine()!!
        if (testMod == "\\q")
            break

        while (true) {
            println("输入一个新正则表达式进行不断测试..(\\b to 测试另一个函数)")
            val re = readLine()!!
            if (re == "\\b")
                break

            println("输入一个字符串去匹配这个正则表达式(\\b to 测试另一个表达式)")
            var text: String? = readLine()

            while (text != "\\b") {

                when(testMod) {
                    "1" -> println(match(re, text))
                    "2" -> println(findAll(re, text))
                    "3" -> println(search(re, text))
                    "4" -> {
                        println("输入匹配部分替换成的字符串")
                        val replacement = readLine()!!
                        println(replace(re, replacement, text))
                    }
                    "5" -> {
                        val match = match(re, text)
                        if (match != null) {
                            println("匹配结果:")
                            println(match)
                            println("输入要group捕获的组别编号")
                            val groupId = readLine()!!.toInt()
                            println(match.group(groupId))
                        }
                        else
                            print("不匹配正则表达式!")
                    }
                    "6" -> {
                        val match = match(re, text)
                        if (match != null) {
                            println("匹配结果:")
                            println(match)
                            println("所有捕获的组别:")
                            println(match.groups())
                        }
                        else
                            print("不匹配正则表达式!")
                    }
                }

                println("输入另个字符串去匹配这个正则表达式(\\b to 测试另一个表达式)")
                text = readLine()

            }
        }

    }
    println("bye")

}