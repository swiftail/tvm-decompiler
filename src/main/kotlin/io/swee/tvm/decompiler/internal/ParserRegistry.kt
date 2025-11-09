package io.swee.tvm.decompiler.internal

import io.swee.tvm.decompiler.internal.ast.AstElement
import io.swee.tvm.decompiler.internal.instructions.Cp0InstructionRegistry
import org.ton.bytecode.*
import kotlin.reflect.KClass

typealias InstParserFull<T> = (ctx: CodeBlockContext, inst: T, ident: String) -> Boolean
typealias InstParserShort<T> = (ctx: CodeBlockContext, inst: T, ident: String) -> Unit

class ParserRegistry {
    private val parsersByInstructionClass = HashMap<Class<out TvmInst>, InstParserFull<*>>()

    fun <T : TvmInst> getParser(instClass: Class<T>): InstParserFull<T>? {
        return parsersByInstructionClass[instClass]?.let {
            return (it as InstParserFull<T>)
        }
    }

    inline fun <reified T : TvmInst> getParser(): InstParserFull<T>? {
        return getParser(T::class.java)
    }

    fun <T : TvmInst> parse(ctx: CodeBlockContext, inst: T, ident: String): Boolean {
        getParser(inst.javaClass)?.let {
            return it(ctx, inst, ident)
        }
        return false
    }

    fun register(
        instClass: Class<out TvmInst>,
        parser: InstParserFull<*>
    ) {
        check(!parsersByInstructionClass.containsKey(instClass)) {
            "Inst redefinition: ${instClass.simpleName}"
        }
        println("registered: $instClass")
        parsersByInstructionClass[instClass] = parser
    }

    inline fun <reified T : TvmInst> registerFull(noinline parser: InstParserFull<T>) {
        register(T::class.java, parser)
    }

    inline fun <reified T : TvmInst> register(noinline parser: InstParserShort<T>) {
        register(T::class.java, uplift(parser))
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

            return subclasses.filter { !it.isAbstract && !it.java.isInterface }
        }

        val subclasses = TvmInst::class.allSubclasses()
        val implemented = subclasses.filter { it.java in parsersByInstructionClass }
        val notImplemented = subclasses.filter { it.java !in parsersByInstructionClass }
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
