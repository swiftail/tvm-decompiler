package io.swee.tvm.decompiler.internal

import org.ton.bytecode.*
import java.util.logging.Logger
import kotlin.reflect.KClass

typealias InstParserFull<T> = (ctx: IrBlockBuilder, inst: T) -> Boolean
typealias InstParserShort<T> = (ctx: IrBlockBuilder, inst: T) -> Unit

typealias InstParser = InstParserFull<List<TvmInst>>
typealias InstPredicate = (List<TvmInst>) -> Boolean

enum class ParserLevel {
    MANUAL,
    STDLIB,
    RAW_ASM
}

data class RegisteredParser(
    val predicate: InstPredicate?,
    val parser: InstParser,
    val level: ParserLevel
)

class ParserRegistry {
    private val logger = Logger.getLogger(ParserRegistry::class.java.name)
    private val instParsers = HashMap<Class<out TvmInst>, MutableList<RegisteredParser>>()
    private val chainParsers = Trie<Class<out TvmInst>, MutableList<RegisteredParser>>()

    private fun matchesChain(
        chain: List<Class<out TvmInst>>,
        actual: List<TvmInst>
    ): Boolean {
        if (actual.size < chain.size) return false
        return chain.indices.all { i -> chain[i].isInstance(actual[i]) }
    }

    private fun collectChains(
        node: Trie<Class<out TvmInst>, MutableList<RegisteredParser>>,
        prefix: List<Class<out TvmInst>>
    ): List<Pair<List<Class<out TvmInst>>, List<RegisteredParser>>> {
        val result = mutableListOf<Pair<List<Class<out TvmInst>>, List<RegisteredParser>>>()
        node.value?.let { result.add(prefix to it) }
        for ((clazz, child) in node.children) {
            result += collectChains(child, prefix + clazz)
        }
        return result
    }

    fun parse(ctx: IrBlockBuilder, inst: TvmInst, nextElements: MutableList<TvmInst> = mutableListOf()): Boolean {
        val singleInput = listOf(inst)
        instParsers[inst.javaClass]?.let { parsers ->
            val sortedParsers = parsers.sortedBy { it.level.ordinal }

            for (entry in sortedParsers) {
                if (entry.predicate == null || entry.predicate(singleInput)) {
                    if (entry.parser(ctx, singleInput)) return true
                }
            }
        }

        val subTrie = chainParsers.getSubTrie(listOf(inst.javaClass)) ?: return false
        val possibleChains = collectChains(subTrie, listOf(inst.javaClass))
            .sortedByDescending { it.first.size }

        for ((chainClasses, parserEntries) in possibleChains) {
            if (nextElements.size < chainClasses.size - 1) continue

            val currentChainInput = listOf(inst) + nextElements.take(chainClasses.size - 1)

            if (!matchesChain(chainClasses, currentChainInput)) continue

            val sortedParsers = parserEntries.sortedBy { it.level.ordinal }

            for (entry in sortedParsers) {
                if (entry.predicate == null || entry.predicate(currentChainInput)) {
                    if (entry.parser(ctx, currentChainInput)) {
                        repeat(chainClasses.size - 1) { nextElements.removeFirst() }
                        return true
                    }
                }
            }
        }
        return false
    }


    fun <T : TvmInst> register(
        instClass: Class<out T>,
        level: ParserLevel,
        parser: InstParserFull<T>,
        predicate: InstPredicate? = null
    ) {
        instParsers.merge(
            instClass,
            mutableListOf(
                RegisteredParser(
                    predicate?.let { { predicate(it) } },
                    { ctx, inst -> parser(ctx, inst.single() as T) },
                    level
                )),
            { a, b -> a.apply { addAll(b) } }
        )
    }


    inline fun <reified T : TvmInst> register(
        level: ParserLevel,
        noinline parser: InstParserShort<T>,
    ) {
        register(T::class.java, level, uplift(parser) as InstParserFull<TvmInst>)
    }

    fun registerChain(
        instClasses: List<Class<out TvmInst>>,
        level: ParserLevel,
        parser: InstParser,
        predicate: InstPredicate? = null,
    ) {
        val existing = chainParsers.get(instClasses) ?: mutableListOf()
        existing.add(RegisteredParser(predicate, parser, level))
        chainParsers.insert(instClasses, existing)
    }

    fun <T> uplift(parserShort: InstParserShort<T>): InstParserFull<T> {
        return { ctx, inst ->
            parserShort(ctx, inst)
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
            logger.fine("Not implemented: ${mnemonic}\t${inst.simpleName}")
        }
        logger.info("implemented: ${implemented.size} not implemented: ${notImplemented.size}")
    }

    fun hasNonConditionalParser(instClass: Class<out TvmInst>): Boolean {
        return instParsers[instClass]?.find { it.predicate == null } != null
    }

}
