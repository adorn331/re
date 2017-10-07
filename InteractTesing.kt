//交互试测试各种功能..
//首先选择需要测试的正则函数1.match 2.findAll 3.search 4.replace
//然后输入一个正则表达式,为了节省开销,编译好这个pattern然后不断输入字符串用这个编译好的对象进行测试看是否符合预想.
//其中输入\q有返回上一层功能,可以进行测试新的正则表达式/切换测试函数
fun main(args: Array<String>) {
    while (true) {
        println("\nplease choose which function to test..")
        println("1.match 2.findAll 3.search 4.replace (\\q to quit)")
        var testMod  = readLine()!!
        if (testMod == "\\q")
            break

        while (true) {
            println("\nInput a new regex to compile..then test (\\b to test another func)")
            val re = readLine()!!
            if (re == "\\b")
                break
            val compiled = compile(re)
            println("regex compile Done!\n")

            println("try a str to test if match this compiled regex..(\\b to test another regex)")
            var text: String? = readLine()

            while (text != "\\b") {

                when(testMod){
                    "1" -> {println(compiled.match(text))
                        println(compiled.match(text)!!.groups())}
                    "2" -> println(compiled.findAll(text))
                    "3" -> println(compiled.search(text))
                    "4" ->{
                        println("input the replacement")
                        var replacement = readLine()!!
                        println(compiled.replace(replacement, text))
                    }

                }

                println("try another str to test..(\\b to test another regex)")
                text = readLine()

            }
        }

    }
    println("bye")

}