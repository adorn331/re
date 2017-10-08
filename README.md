# 正则表达式引擎Kotlin实现

  国庆大部分时间除了玩其他时间几乎都用来用之前没有接触过的Kotlin实现了简易的正则表达式引擎,由于之前从来未接触过编译原理的词法分析内容,
  所以花了几乎一天去看龙书和各种博客看关于NFA和DFA的算法和实现,最终还是参照龙书的思路,决定走Regex->NFA->DFA这条路线去实现这个引擎.<br><br>
  正则匹配的语法方面,能够支持一些基本的匹配包括. [] \w \s ^ $ |,其中[]支持[0-9a-z]和[abc]两种使用,并且支持限定符包括* + ? {n} {n,m} {n,},支持非贪婪匹配,支持()捕获匹配字符串.<br><br>
  用户功能接口方面,实现了最常使用用的几个功能: match,findAll,search,replace,是非贪婪匹配同时支持对一个正则表达式进行预先编译compile后重复使用.详细使用见下文.<br><br>
  
# 源码各文件说明
graph.kt :包含正则表达式引擎将正则表达式转换为nfa再转换到dfa图的所需数据结构和函数<br>
regex.kt :包含正则表达式引擎的用户接口,同时包含了匹配成功的类Match和编译正则后的类compiledRe. <br>
InteractingTesing:包含用于测试此正则引擎各项功能的入口<br>

# 总体设计
1. 将表达式分为两部分,用两个栈分别存放pattern的普通字符和各种匹配修饰符/限定符<br>
2. 利用这两个栈分别生成基本的NFA子图然后不断合并生成NFA图, 也用到了汤普森算法的思想<br>
3. 利用最子集构造发生成DFA图<br>
4. 根据DFA图状态去match<br>

## 将表达式分为两部分,用两个栈分别存放pattern的普通字符和各种匹配修饰符
这一步的思路主要是参考了<数据结构与算法分析C语言>一书中中缀表达式与后缀表达式的转换,
利用两个栈来分别将操作符和子图分别存到两个栈了里面.'|' *' '-' 等类比做波兰表达式的操作符, 子图则比作表达式的运算数..
在网上和书中也看到了另一种方法是使用语法分析树,但我选择的是这个栈的方法通过子图不断合并成用来递归构建起nfa图较为简易

## 利用这两个栈分别生成基本的NFA子图然后不断合并生成NFA图
根据龙书里描述的正则表达式是有几种基本的正则表达式组合递归生成的的思想,那么我们最终构造的那么NFA子图也应该是由许多最基本的子图不断合并成的.
其中最进本的子图是<br>
```<start> --a--> <end>```<br>
遍历表达式,遇见了一个字符就生成最基本的子图压如子图栈中.
遇见了一个"操作符"便准备压如栈中,栈顶的操作符优先级应该比栈底的都高才能压入,否则一直弹出栈内操作符进行合并子图操作直到可以放入位置,并将合并后的子图又压如回栈中.
最后弹出剩余在栈内所有操作符用于合并子图,完成后栈内只剩下最后一个图便是完整的NFA图.
其中而合并成子图的规则是:<br>
* ab<br>
```<start> --a--> <e1> --ε--> <s2> --b--> <end><br>```
* a|b<br>
```
   |----ε----> <s1> --a--> <s2> ---ε-------|
   |                                       ∨
<start> --ε--> <s3> --b--> <s4> ---ε---> <end>
```
* a*<br>
```
    |------------------ε-------------------|
    |                                      ∨
  <start> --ε--> <s1> --a--> <s2> --ε--> <end>
                  ^            |
                  |-----ε------|
```
而在我自己思考下,我将a*换了个版本:<br>
* a*<br>
```
                  |------ε------|
                  |             ∨
                 <start> --a--><end>
                  ^             |
                  |------ε------|
```
并且自己构造了:a? a+ 规则<br>
* a?<br>
```
                 <start> --a--> <end>
                  ^              |
                  |-------ε------|
```
* a?<br>
```
                 <start> --a--> <end>
                  ^              |
                  |-------ε------|
```

其中符号ε是epsilon空边，可以不需要接受任何字符跳转到下个状态.

## 利用最子集构造发生成DFA图
参照龙书的最小子集构造算法.

## 根据DFA图状态去match
先根据pattern生成NFA图获得其起始节点,逐个读入text的字符,看DFA图节点能否根据这个字符进行状态跳转,能且如果跳转到一个结束状态,则将text的这个位置压入一个栈内存放,
然后继续读入下一个字符跳转,直到无法跳转为止或到text结尾.查看栈顶的就是text能匹配到的位置.

# 用户接口
* match(pattern, text):首字母开始开始匹配，string如果包含pattern子串，则匹配成功，返回Match对象，失败则返回null，若要完全匹配，pattern要以$结尾

示例:```match("\d{3}\-\d{3,8}", "222-233333xxx")```
返回:```<Match(0,10): match="222-233333">```

注:返回的为Match类,封装了了用于匹配文本和匹配的起始终止位置,同时也封装了group() groups()等方法
Match类打印的格式为(0,10)是匹配的下标(左闭右开),match="222-233333"是字符串所匹配的部分

* findAll(pattern, text):返回text中所有与pattern相匹配的全部子串，返回形式为一个Match对象的集合

示例:```findAll("[0-9]+@qq.com|QQmail", "dvalkmlj4564345@qq.comsdlfj324324234@qq.comsadjflQQmailsdkf")```<br>
返回:```[<Match(8,22): match="4564345@qq.com">, <Match(27,43): match="324324234@qq.com">, <Match(49,55): match="QQmail">]```<br>

* search(pattern, text):若text中包含pattern子串，有则返回Match对象，否则返回None，如果text中存在多个pattern子串，只返回第一个  
示例:```search("[0-9]+@qq.com|QQmail", "dvalkmlj4564345@qq.comsdlfj324324234@qq.comsadjflQQmailsdkf")```<br>
返回:```<Match(8,22): match="4564345@qq.com">```<br>

* replace(pattern, replacement,text):替换text中所有与pattern相匹配的全部字串替换为replacement,返回一个替换后的字符串
示例:```replace('test1|test2',"****", "32432kdltest1hhmmi998test3kmkkktest2jjj")```
返回:```32432kdl****hhmmi998test3kmkkk****jjj```

* compile:编译正则表达式,返回一个CompiledRe类,里面已经编译构造好dfa图并且封装了各种match findAll等等方法能对编译好了的正则直接进行匹配
示例:```val c  = compile("\d{3}\-\d{3,8}")```<br>
```c.match("222-233333x")``` ->  ```<Match(0,10): match="222-233333">```<br>
```c.match("6666-22") ``` -> ```null```<br>



* Match对象的groups()和group: 
示例:
```m = match("aa(bb)cc(dd)ee", "aabbccddee")```<br>
```m.groups()```->```[bb, dd]```<br>
```m.group(0)```->```aabbccddee```<br>
```m.group(1)```->```bb```<br>
 ```m.group(2)```->```dd```<br>

# 使用环境配置 & 测试方法
* 使用配置: 将graph.kt和regex.kt复制到任意Kotlin project的src目录下即可在src其他目录下调用match findAll search replace comile等函数
* 测试此引擎:  将graph.kt和regex.kt复制到任意Kotlin project的src目录并复制InteractingTesing.kt到此目录,运行InteractingTesing的main入口即可交互式进行测试此正则表达式功能函数

# 后续思考&未完成
可以说从龙书中收益良多,对词法分析,正则表达式都有了新的了解,虽然自己写引擎不一定,但以后用正则表达式是更清晰怎么回事了.<br>
而对于DFA图还没有进行最小化工作可能开销会很大..而且感觉也有挺多潜在bug没有找出来.还有Kotlin写得也不像优雅的Kotlin,没有体现语言的特色.然后findAll出来的Match集合忘了补上支持group(),只有match函数返回的对象支持.但时间匆忙先做到这,以后继续慢慢完善.

