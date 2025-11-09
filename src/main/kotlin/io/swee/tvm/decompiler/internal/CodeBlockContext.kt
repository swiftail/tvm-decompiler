package io.swee.tvm.decompiler.internal

import io.swee.tvm.decompiler.internal.ast.AstElement
import org.ton.bytecode.TvmInst
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

class CodeBlockContext(var parent: ParentContext) {
    internal val stack = LinkedList<StackEntry>()
    private val body = mutableListOf<AstElement>()

    private val varDeclarations = LinkedHashMap<StackEntry, AstElement>()
    private val varDeclarationsReversed = LinkedHashMap<AstElement, AstElement.VariableDeclaration>()
    private val continuations = HashMap<StackEntry, List<TvmInst>>()

    fun initContinuation(entry: StackEntry, instList: List<TvmInst>) {
        continuations[entry] = instList
    }

    fun getContinuation(entry: StackEntry) = continuations[entry]!!

    fun append(fragment: AstElement): CodeBlockContext {
        fragment.flat().filterIsInstance<AstElement.VariableDeclaration>().forEach {
            it.entries.forEach { entry ->
                if (varDeclarations.containsKey(entry)) {
                    error("Variable redeclared ${entry}")
                }
                varDeclarations[entry] = it.value
            }
            if (varDeclarationsReversed.containsKey(it.value)) {
                error("smth bad")
            }
            varDeclarationsReversed[it.value] = it
        }
        body += fragment
        return this
    }

    fun ensureStackDepth(depth: Int) {
        val diff = depth - stack.size
        repeat(diff) {
            stack.addLast(parent.pop())
        }
    }

    private fun pop(): StackEntry {
        return stack.removeFirst()
    }

    fun stackDump(): String = stack.withIndex().joinToString("\n") { it.index.toString() + " " + it.value }

    internal fun pop(typename: String): StackEntry {
        ensureStackDepth(1)

        val entry = stack.first
        check(entry.type.typename == typename || entry.type.typename == TvmStackEntryType.UNKNOWN.typename || typename == TvmStackEntryType.UNKNOWN.typename) {
            "Expected entry to be $typename, was ${entry.type.typename}. Stack:\n${stackDump()}"
        }
        return pop()
    }

    fun push(entry: StackEntry): CodeBlockContext {
        stack.addFirst(entry)
        return this
    }

    fun resolveCode(
        entryUsage: MutableMap<StackEntry, Int> = mutableMapOf(),
        varDeclarations: Map<StackEntry, AstElement> = emptyMap(),
        varDeclarationsReversed: Map<AstElement, AstElement.VariableDeclaration> = emptyMap(),
        nameCache: MutableMap<StackEntryName, String> = mutableMapOf(),
        usedNames: MutableMap<String, Int> = mutableMapOf(),
        registry: ParserRegistry
    ): String {
        val builder = StringBuilder()
        for (codeFragment in body) {
            val entries = codeFragment.stackEntries()
            for (entry in entries) {
                entryUsage[entry] = entryUsage.getOrDefault(entry, 0) + 1
            }
        }
        fun resolveName(entry: StackEntryName): String {
            if (entry in nameCache) {
                return nameCache[entry]!!
            }

            val notCountedName = when (entry) {
                is StackEntryName.Const -> entry.value
                is StackEntryName.Parent -> resolveName(entry.parent) + "_" + entry.suffix
            }
            val usedTimes = usedNames.getOrDefault(notCountedName, 0)

            val finalName = if (usedTimes == 0) {
                notCountedName
            } else {
                notCountedName + "_" + usedTimes
            }

            usedNames[notCountedName] = usedTimes + 1
            nameCache[entry] = finalName
            return finalName
        }
        for (codeFragment in body) {
            builder.append(
                codeFragment.resolve(
                    this,
                    ::resolveName,
                    entryUsage,
                    varDeclarations + this.varDeclarations,
                    varDeclarationsReversed + this.varDeclarationsReversed,
                    nameCache,
                    usedNames,
                    registry
                )
            )
        }
        return builder.toString().removeSuffix(";")
    }

    fun pushVirtual(entry: StackEntry): CodeBlockContext {
        return push(entry) // todo track
    }
}
