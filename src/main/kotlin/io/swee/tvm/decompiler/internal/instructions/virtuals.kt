package io.swee.tvm.decompiler.internal.instructions

import io.swee.tvm.decompiler.internal.*
import io.swee.tvm.decompiler.internal.ast.AstElement
import org.ton.bytecode.*

private inline fun <reified T : TvmInst, R : TvmInst> registerVirtualInstruction(
    registry: ParserRegistry,
    crossinline realInstructionBuilder: (location: TvmInstLocation, inst: T) -> R,
    crossinline virtualEntriesFactory: (T) -> List<Pair<TvmStackEntryType, AstElement>> = { _ -> listOf() },
) {
    registry.register { ctx: CodeBlockContext, inst: T, ident: String ->
        val virtualEntries = virtualEntriesFactory(inst)
        for ((entryType, declaration) in virtualEntries) {
            val virtualEntry = StackEntry.Simple(entryType, StackEntryName.Const("virtual"))
            ctx.pushVirtual(virtualEntry)
            ctx.append(AstElement.VariableDeclaration(listOf(virtualEntry), declaration))
        }

        registry.parse(ctx, realInstructionBuilder(inst.location, inst), ident)
    }
}

fun registerVirtualInstructions(registry: ParserRegistry) {
    registerVirtualInstruction<TvmCompareIntLessintInst, TvmCompareIntLessInst>(
        registry,
        { location, _ -> TvmCompareIntLessInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to AstElement.Literal(inst.y)
        ) },
    )
    registerVirtualInstruction<TvmCellBuildStiInst, TvmCellBuildStixInst>(
        registry,
        { location, _ -> TvmCellBuildStixInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to AstElement.Literal(inst.c + 1)
        ) },
    )
    registerVirtualInstruction<TvmCellBuildStuInst, TvmCellBuildStuxInst>(
        registry,
        { location, _ -> TvmCellBuildStuxInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to AstElement.Literal(inst.c + 1)
        ) },
    )
    registerVirtualInstruction<TvmExceptionsThrowifnotInst, TvmExceptionsThrowargifnotInst>(
        registry,
        { location, inst -> TvmExceptionsThrowargifnotInst(location, inst.n) },
        { _ ->
            listOf(
                TvmStackEntryType.INT to AstElement.Literal(0)
            )
        }
    )
    registerVirtualInstruction<TvmExceptionsThrowifnotShortInst, TvmExceptionsThrowifInst>(
        registry,
        { location, inst -> TvmExceptionsThrowifInst(location, inst.n) }
    )
    registerVirtualInstruction<TvmExceptionsThrowifInst, TvmExceptionsThrowargifInst>(
        registry,
        { location, inst -> TvmExceptionsThrowargifInst(location, inst.n) },
        { _ ->
            listOf(
                TvmStackEntryType.INT to AstElement.Literal(0)
            )
        }
    )
    registerVirtualInstruction<TvmExceptionsThrowifShortInst, TvmExceptionsThrowifInst>(
        registry,
        { location, inst -> TvmExceptionsThrowifInst(location, inst.n) }
    )
    registerVirtualInstruction<TvmCellParsePlduInst, TvmCellParsePlduxInst>(
        registry,
        { location, _ -> TvmCellParsePlduxInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to AstElement.Literal(inst.c + 1)
        ) }
    )
    registerVirtualInstruction<TvmCellParsePldiInst, TvmCellParsePldixInst>(
        registry,
        { location, _ -> TvmCellParsePldixInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to AstElement.Literal(inst.c + 1)
        ) }
    )
    registerVirtualInstruction<TvmTupleIndexInst, TvmTupleIndexvarInst>(
        registry,
        { location, _ -> TvmTupleIndexvarInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to AstElement.Literal(inst.k)
        ) }
    )
}
