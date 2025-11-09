package io.swee.tvm.decompiler.internal.instructions

import io.swee.tvm.decompiler.internal.*
import io.swee.tvm.decompiler.internal.ast.AstElement
import org.ton.bytecode.*

// todo move somewhere
fun resolveContinuation(
    registry: ParserRegistry,
    ctx: CodeBlockContext,
    ident: String,
    continuation: List<TvmInst>,
    ret: Boolean,
): () -> Unit {
    val stackBefore = ctx.stack.toList()
    val parentBefore = ctx.parent.backup()
//    val parentContext = ctx.asParent()
    TvmDecompilerImpl.parseCodeBlock(
        registry,
        ctx,
        continuation,
        ident + "\t",
        ret,
    )

    val stackAfter = ctx.stack.toList()
    val parentAfter = ctx.parent.backup()

    ctx.stack.clear()
    ctx.stack.addAll(stackBefore)
    ctx.parent = parentBefore

    return {
        ctx.stack.clear()
        ctx.stack.addAll(stackAfter)
        ctx.parent = parentAfter
    }
}

fun CodeBlockContext.inlineContinuation(
    registry: ParserRegistry,
    ident: String,
    continuation: List<TvmInst>,
    ret: Boolean = false
): () -> Unit {
    return resolveContinuation(registry, this, ident, continuation, ret)
}

fun registerContinuationParsers(registry: ParserRegistry) {
    with(registry) {
        register<TvmConstDataPushcontShortInst> { ctx, inst, ident ->
            val entry = newContinuation(name("continuation"))
            ctx.push(entry)
            ctx.initContinuation(entry, inst.c)
        }
        register<TvmConstDataPushcontInst> { ctx, inst, ident ->
            val entry = newContinuation(name("continuation"))
            ctx.push(entry)
            ctx.initContinuation(entry, inst.c)
        }
        register<TvmContLoopsWhileInst> { ctx, inst, ident ->
            val body = ctx.popContinuation()
            val condition = ctx.popContinuation()

            ctx
                .append(AstElement.Raw("while ("))

            ctx.inlineContinuation(registry, ident, ctx.getContinuation(condition))()

            ctx.append(AstElement.Raw(") {\n"))
            ctx.inlineContinuation(registry, ident, ctx.getContinuation(body))()
            ctx.append(AstElement.Raw("\n$ident}"))
        }
        register<TvmContConditionalIfjmpInst> { ctx, inst, ident ->
            val continuation = ctx.popContinuation()
            val condition = ctx.popInt()

            ctx
                .append(AstElement.Raw("if "))
                .append(AstElement.VariableUsage(condition, true))
                .append(AstElement.Raw(" {\n"))
            ctx.inlineContinuation(registry, ident, ctx.getContinuation(continuation), ret = true)
            ctx.append(AstElement.Raw("\n$ident}"))
        }
        register<TvmContConditionalIfrefInst> { ctx, inst, ident ->
            val condition = ctx.popInt()
            val continuation = newContinuation(name("continuation"))

            ctx.initContinuation(continuation, inst.c)

            ctx
                .append(AstElement.Raw("if "))
                .append(AstElement.VariableUsage(condition, true))
                .append(AstElement.Raw(" {\n"))
            ctx.inlineContinuation(registry, ident, ctx.getContinuation(condition))()
            ctx.append(AstElement.Raw("\n$ident}"))
        }
        register<TvmContConditionalIfelseInst> { ctx, inst, ident ->
            val continuationElse = ctx.popContinuation()
            val continuation = ctx.popContinuation()
            val condition = ctx.popInt()

            ctx
                .append(AstElement.Raw("if "))
                .append(AstElement.VariableUsage(condition, true))
                .append(AstElement.Raw(" {\n"))
            ctx.inlineContinuation(registry, ident, ctx.getContinuation(continuation))
            ctx.append(AstElement.Raw("\n$ident} else {\n"))
            ctx.inlineContinuation(registry, ident, ctx.getContinuation(continuationElse))()
            ctx.append(AstElement.Raw("\n$ident}"))
        }
        // todo нужно применять стек после активной оставшейся континуации если она есть
        register<TvmContConditionalIfrefelseInst> { ctx, inst, ident ->
            val continuationElse = ctx.popContinuation()
            val condition = ctx.popInt()
            val continuation = newContinuation(name("continuation"))

            ctx.initContinuation(continuation, inst.c)

            ctx
                .append(AstElement.Raw("if "))
                .append(AstElement.VariableUsage(condition, true))
                .append(AstElement.Raw(" {\n"))
            ctx.inlineContinuation(registry, ident, ctx.getContinuation(continuation))
            ctx.append(AstElement.Raw("\n$ident} else {\n"))
            ctx.inlineContinuation(registry, ident, ctx.getContinuation(continuationElse))()
            ctx.append(AstElement.Raw("\n$ident}"))
        }
        register<TvmContConditionalIfelserefInst> { ctx, inst, ident ->
            val continuation = ctx.popContinuation()
            val condition = ctx.popInt()
            val continuationElse = newContinuation(name("continuation"))

            ctx.initContinuation(continuationElse, inst.c)

            ctx
                .append(AstElement.Raw("if "))
                .append(AstElement.VariableUsage(condition, true))
                .append(AstElement.Raw(" {\n"))
            ctx.inlineContinuation(registry, ident, ctx.getContinuation(continuation))
            ctx.append(AstElement.Raw("\n$ident} else {\n"))
            ctx.inlineContinuation(registry, ident, ctx.getContinuation(continuationElse))()
            ctx.append(AstElement.Raw("\n$ident}"))
        }
        register<TvmContConditionalIfrefelserefInst> { ctx, inst, ident ->
            val continuationElse = newContinuation(name("continuation"))
            val condition = ctx.popInt()
            val continuation = newContinuation(name("continuation"))

            ctx.initContinuation(continuationElse, inst.c2)
            ctx.initContinuation(continuation, inst.c1)

            ctx
                .append(AstElement.Raw("if "))
                .append(AstElement.VariableUsage(condition, true))
                .append(AstElement.Raw(" {\n"))
            ctx.inlineContinuation(registry, ident, ctx.getContinuation(continuation))
            ctx.append(AstElement.Raw("\n$ident} else {\n"))
            ctx.inlineContinuation(registry, ident, ctx.getContinuation(continuationElse))()
            ctx.append(AstElement.Raw("\n$ident}"))
        }
    }
}
