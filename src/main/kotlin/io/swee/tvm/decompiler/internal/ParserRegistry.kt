package io.swee.tvm.decompiler.internal

import org.ton.bytecode.*
import kotlin.reflect.KClass

typealias InstParserFull<T> = (ctx: CodeBlockContext, inst: T, ident: String) -> Boolean
typealias InstParserShort<T> = (ctx: CodeBlockContext, inst: T, ident: String) -> Unit

class ParserRegistry {
    private val instParsers = HashMap<Class<out TvmInst>, InstParserFull<TvmInst>>()
    private val chainParsers = Trie<Class<out TvmInst>, InstParserFull<List<TvmInst>>>()

    fun getParser(instClass: Class<out TvmInst>): InstParserFull<TvmInst>? {
        return instParsers[instClass]
    }

    inline fun <reified T : TvmInst> getParser(): InstParserFull<T>? {
        return getParser(T::class.java)
    }

    private fun matchesChain(
        chain: List<Class<out TvmInst>>,
        actual: List<TvmInst>
    ): Boolean {
        if (actual.size < chain.size) return false
        return chain.indices.all { i -> chain[i].isInstance(actual[i]) }
    }

    private fun collectChains(
        node: Trie<Class<out TvmInst>, InstParserFull<List<TvmInst>>>,
        prefix: List<Class<out TvmInst>>
    ): List<Pair<List<Class<out TvmInst>>, InstParserFull<List<TvmInst>>>> {
        val result = mutableListOf<Pair<List<Class<out TvmInst>>, InstParserFull<List<TvmInst>>>>()
        node.value?.let { result.add(prefix to it) }
        for ((clazz, child) in node.children) {
            result += collectChains(child, prefix + clazz)
        }
        return result
    }

    fun parse(ctx: CodeBlockContext, inst: TvmInst, ident: String, nextElements: MutableList<TvmInst> = mutableListOf()): Boolean {
        val directParser = getParser(inst.javaClass)
        if (directParser != null) {
            return directParser(ctx, inst, ident)
        }

        val firstClass = inst.javaClass
        val subTrie = chainParsers.getSubTrie(listOf(firstClass)) ?: return false

        val possibleChains = collectChains(subTrie, listOf(firstClass))

        for ((chain, parser) in possibleChains) {
            if (matchesChain(chain, listOf(inst) + nextElements)) {
                val result = parser(ctx, buildList {
                    add(inst)
                    addAll(nextElements.take(chain.size - 1))
                }, ident)
                if (result) {
                    repeat(chain.size - 1) {
                        nextElements.removeFirst()
                    }
                    return true
                }
            }
        }

        return false
    }


    fun register(
        instClass: Class<out TvmInst>,
        parser: InstParserFull<TvmInst>
    ) {
        check(!instParsers.containsKey(instClass)) {
            "Inst redefinition: ${instClass.simpleName}"
        }
//        println("registered: $instClass")
        instParsers[instClass] = parser
    }

    inline fun <reified T : TvmInst> registerFull(noinline parser: InstParserFull<T>) {
        register(T::class.java, parser as InstParserFull<TvmInst>)
    }

    inline fun <reified T : TvmInst> register(noinline parser: InstParserShort<T>) {
        register(T::class.java, uplift(parser) as InstParserFull<TvmInst>)
    }

    fun registerChain(
        instClasses: List<Class<out TvmInst>>,
        parser: InstParserFull<List<*>>
    ) {
        check(chainParsers.get(instClasses) == null) {
            "Chain redefinition: ${instClasses.map { it.simpleName }}"
        }
//        println("registered: ${instClasses.map { it.simpleName }}")
        chainParsers.insert(
            instClasses,
            parser
        )
    }

    fun <T> uplift(parserShort: InstParserShort<T>): InstParserFull<T> {
        return { ctx, inst, ident ->
            parserShort(ctx, inst, ident)
            true
        }
    }

    fun dumpUnknownInstructions() {
        fun <T : Any> KClass<T>.allSubclasses(): List<KClass<out T>> {
            val subclasses = mutableListOf<KClass<out T>>()
            val queue = ArrayDeque(this.sealedSubclasses)

            while (queue.isNotEmpty()) {
                val subclass = queue.removeFirst()
                subclasses.add(subclass)
                queue.addAll(subclass.allSubclasses())
            }

            return subclasses.filter { !it.isAbstract && !it.java.isInterface }.distinctBy { it.simpleName }
        }

        val subclasses = TvmInst::class.allSubclasses()


        val implemented: MutableList<KClass<out TvmInst>> = mutableListOf()
        val notImplemented: MutableList<KClass<out TvmInst>> = mutableListOf()

        for (subclass in subclasses) {
            if (subclass.java !in instParsers) {
                if (chainParsers.getSubTrie(listOf(subclass.java)) == null) {
                    notImplemented.add(subclass)
                    continue
                }
            }
            implemented.add(subclass)
        }

        notImplemented.forEach { inst ->
            val companion = inst.nestedClasses.find { it.simpleName == "Companion" }
            val companionInstance = companion?.objectInstance
            val mnemonicField = companion?.members?.find { it.name == "MNEMONIC" }
            val mnemonic = mnemonicField?.call(companionInstance)
            println("Not implemented: ${mnemonic}\t${inst.simpleName}")
        }
        println("implemented: ${implemented.size} not implemented: ${notImplemented.size}")
    }

}
