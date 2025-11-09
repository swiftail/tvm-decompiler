package io.swee.tvm.decompiler.internal.ast

import io.swee.tvm.decompiler.internal.*

sealed interface AstElement {
    fun resolve(
        ctx: CodeBlockContext,
        nameResolver: (entry: StackEntryName) -> String,
        entryUsage: MutableMap<StackEntry, Int>,
        varDeclarations: Map<StackEntry, AstElement>,
        varDeclarationsReversed: Map<AstElement, VariableDeclaration>,
        nameCache: MutableMap<StackEntryName, String>,
        usedNames: MutableMap<String, Int>,
        registry: ParserRegistry
    ): String

    fun stackEntries(): List<StackEntry> = listOf()

    fun flat(): List<AstElement> = listOf(this)

    class VariableUsage(val entry: StackEntry, val tracked: Boolean) : AstElement {
        override fun resolve(
            ctx: CodeBlockContext,
            nameResolver: (entry: StackEntryName) -> String,
            entryUsage: MutableMap<StackEntry, Int>,
            varDeclarations: Map<StackEntry, AstElement>,
            varDeclarationsReversed: Map<AstElement, VariableDeclaration>,
            nameCache: MutableMap<StackEntryName, String>,
            usedNames: MutableMap<String, Int>,
            registry: ParserRegistry
        ): String {
            if (varDeclarations.containsKey(entry)) {
                val declaringElement = varDeclarations[entry]
                if (declaringElement != null) {
                    val fullDeclaration = varDeclarationsReversed[declaringElement]
                    if (tracked && fullDeclaration?.isInlined(entryUsage) == true) {
                        return declaringElement.resolve(
                            ctx,
                            nameResolver,
                            entryUsage,
                            varDeclarations,
                            varDeclarationsReversed,
                            nameCache,
                            usedNames,
                            registry
                        )
                    }
                }
            }
            return nameResolver(entry.name)
        }

        override fun stackEntries(): List<StackEntry> {
            return if (tracked) {
                listOf(entry)
            } else {
                emptyList()
            }
        }
    }
//
//    class VariableDeclaration(
//        val entry: StackEntry,
//        val value: AstElement
//    ) : AstElement {
//        override fun resolve(
//            ctx: CodeBlockContext,
//            nameResolver: (entry: StackEntryName) -> String,
//            entryUsage: MutableMap<StackEntry, Int>,
//            varDeclarations: Map<StackEntry, AstElement>,
//            nameCache: MutableMap<StackEntryName, String>,
//            usedNames: MutableMap<String, Int>,
//            registry: ParserRegistry
//        ): String {
//            val usages = entryUsage.getOrDefault(entry, 0)
//
//            if (usages == 0) {
//                return value.resolve(ctx, nameResolver, entryUsage, varDeclarations, nameCache, usedNames, registry)
//            }
//            if (usages > 1) {
//                return "${entry.type.typename} ${nameResolver(entry.name)} = " + value.resolve(
//                    ctx,
//                    nameResolver,
//                    entryUsage,
//                    varDeclarations,
//                    nameCache,
//                    usedNames,
//                    registry
//                )
//            }
//            return ""
//        }
//
//        override fun stackEntries(): List<StackEntry> {
//            return value.stackEntries()
//        }
//
//        override fun flat(): List<AstElement> {
//            return value.flat() + listOf(this)
//        }
//    }

    class VariableDeclaration(
        val entries: List<StackEntry>,
        val value: AstElement,
        val untuple: Boolean = false
    ) : AstElement {
        override fun resolve(
            ctx: CodeBlockContext,
            nameResolver: (entry: StackEntryName) -> String,
            entryUsage: MutableMap<StackEntry, Int>,
            varDeclarations: Map<StackEntry, AstElement>,
            varDeclarationsReversed: Map<AstElement, VariableDeclaration>,
            nameCache: MutableMap<StackEntryName, String>,
            usedNames: MutableMap<String, Int>,
            registry: ParserRegistry
        ): String {
            if (isInlined(entryUsage)) {
                return ""
            }

            return resolve0(ctx, nameResolver, entryUsage, varDeclarations, varDeclarationsReversed, nameCache, usedNames, registry)
        }

        fun resolve0(
            ctx: CodeBlockContext,
            nameResolver: (entry: StackEntryName) -> String,
            entryUsage: MutableMap<StackEntry, Int>,
            varDeclarations: Map<StackEntry, AstElement>,
            varDeclarationsReversed: Map<AstElement, VariableDeclaration>,
            nameCache: MutableMap<StackEntryName, String>,
            usedNames: MutableMap<String, Int>,
            registry: ParserRegistry
        ): String {
            val entriesJoined = entries.joinToString(", ") { entry ->
                when (entryUsage.getOrDefault(entry, 0)) {
                    0 -> "_"
                    else -> "${entry.type.typename} ${nameResolver(entry.name)}"
                }
            }
            val valueResolved = value.resolve(
                ctx,
                nameResolver,
                entryUsage,
                varDeclarations,
                varDeclarationsReversed,
                nameCache,
                usedNames,
                registry
            )

            return when {
                entries.size == 0 -> valueResolved
                untuple -> "[$entriesJoined] = $valueResolved"
                entries.size == 1 -> "$entriesJoined = $valueResolved"
                else -> "($entriesJoined) = $valueResolved"
            }
        }

        override fun stackEntries(): List<StackEntry> {
            return value.stackEntries()
        }

        override fun flat(): List<AstElement> {
            return value.flat() + listOf(this)
        }

        fun isInlined(entryUsage: MutableMap<StackEntry, Int>): Boolean {
            return entries.size == 1 && entries.all { entryUsage.getOrDefault(it, 0) == 1 }
        }

    }

    @Deprecated("should be embedded in other code fragments")
    class Raw(val code: Any) : AstElement {
        override fun resolve(
            ctx: CodeBlockContext,
            nameResolver: (entry: StackEntryName) -> String,
            entryUsage: MutableMap<StackEntry, Int>,
            varDeclarations: Map<StackEntry, AstElement>,
            varDeclarationsReversed: Map<AstElement, VariableDeclaration>,
            nameCache: MutableMap<StackEntryName, String>,
            usedNames: MutableMap<String, Int>,
            registry: ParserRegistry
        ): String {
            return code.toString()
        }
    }

    class Literal(val literal: Any) : AstElement {
        override fun resolve(
            ctx: CodeBlockContext,
            nameResolver: (entry: StackEntryName) -> String,
            entryUsage: MutableMap<StackEntry, Int>,
            varDeclarations: Map<StackEntry, AstElement>,
            varDeclarationsReversed: Map<AstElement, VariableDeclaration>,
            nameCache: MutableMap<StackEntryName, String>,
            usedNames: MutableMap<String, Int>,
            registry: ParserRegistry
        ): String {
            return literal.toString()
        }
    }

    object FunctionReturnType : AstElement {
        override fun resolve(
            ctx: CodeBlockContext,
            nameResolver: (entry: StackEntryName) -> String,
            entryUsage: MutableMap<StackEntry, Int>,
            varDeclarations: Map<StackEntry, AstElement>,
            varDeclarationsReversed: Map<AstElement, VariableDeclaration>,
            nameCache: MutableMap<StackEntryName, String>,
            usedNames: MutableMap<String, Int>,
            registry: ParserRegistry
        ): String {
            return "()"
        }
    }

    @Deprecated("booba")
    class Composition(val fragments: List<AstElement>) : AstElement {
        override fun resolve(
            ctx: CodeBlockContext,
            nameResolver: (entry: StackEntryName) -> String,
            entryUsage: MutableMap<StackEntry, Int>,
            varDeclarations: Map<StackEntry, AstElement>,
            varDeclarationsReversed: Map<AstElement, VariableDeclaration>,
            nameCache: MutableMap<StackEntryName, String>,
            usedNames: MutableMap<String, Int>,
            registry: ParserRegistry
        ): String {
            return fragments.joinToString("") {
                it.resolve(
                    ctx,
                    nameResolver,
                    entryUsage,
                    varDeclarations,
                    varDeclarationsReversed,
                    nameCache,
                    usedNames,
                    registry
                )
            }
        }

        override fun stackEntries(): List<StackEntry> {
            return fragments.flatMap { it.stackEntries() }
        }

        override fun flat(): List<AstElement> {
            return fragments.flatMap { it.flat() } + listOf(this)
        }
    }

    class FunctionReturnStatement(val stack: List<StackEntry>) : AstElement {
        override fun resolve(
            ctx: CodeBlockContext,
            nameResolver: (entry: StackEntryName) -> String,
            entryUsage: MutableMap<StackEntry, Int>,
            varDeclarations: Map<StackEntry, AstElement>,
            varDeclarationsReversed: Map<AstElement, VariableDeclaration>,
            nameCache: MutableMap<StackEntryName, String>,
            usedNames: MutableMap<String, Int>,
            registry: ParserRegistry
        ): String {
            return "return (${ stack.joinToString(", ") { 
                VariableUsage(it, true).resolve(
                    ctx, nameResolver, entryUsage, varDeclarations,
                    varDeclarationsReversed, nameCache, usedNames, registry
                )
            } });"
        }

        override fun stackEntries(): List<StackEntry> {
            return stack
        }
    }

    class FunctionArgs(val inheritedStackTracker: InheritedStackTracker) : AstElement {
        override fun resolve(
            ctx: CodeBlockContext,
            nameResolver: (entry: StackEntryName) -> String,
            entryUsage: MutableMap<StackEntry, Int>,
            varDeclarations: Map<StackEntry, AstElement>,
            varDeclarationsReversed: Map<AstElement, VariableDeclaration>,
            nameCache: MutableMap<StackEntryName, String>,
            usedNames: MutableMap<String, Int>,
            registry: ParserRegistry
        ): String {
            return "(${
                inheritedStackTracker.usedEntries.reversed().joinToString(", ") { "${it.type.typename} ${nameResolver(it.name)}" }
            })"
        }
    }

    class FunctionCall(
        val name: String,
        val args: List<AstElement>
    ) : AstElement {
        override fun resolve(
            ctx: CodeBlockContext,
            nameResolver: (entry: StackEntryName) -> String,
            entryUsage: MutableMap<StackEntry, Int>,
            varDeclarations: Map<StackEntry, AstElement>,
            varDeclarationsReversed: Map<AstElement, VariableDeclaration>,
            nameCache: MutableMap<StackEntryName, String>,
            usedNames: MutableMap<String, Int>,
            registry: ParserRegistry
        ): String {
            return "$name(${args.joinToString (", ") { it.resolve(
                ctx, nameResolver, entryUsage, varDeclarations,
                varDeclarationsReversed, nameCache, usedNames, registry
            ) }})"
        }

        override fun stackEntries(): List<StackEntry> {
            return args.flatMap { it.stackEntries() }
        }

        override fun flat(): List<AstElement> {
            return args.flatMap { it.flat() } + listOf(this)
        }
    }
}
