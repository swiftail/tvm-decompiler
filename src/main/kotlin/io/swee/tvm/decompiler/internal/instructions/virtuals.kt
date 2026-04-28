package io.swee.tvm.decompiler.internal.instructions

import io.swee.tvm.decompiler.internal.*
import io.swee.tvm.decompiler.internal.ir.IRNode
import org.ton.bytecode.*
import java.math.BigInteger

private inline fun <reified T : TvmInst, R : TvmInst> registerVirtualInstruction(
    registry: ParserRegistry,
    crossinline realInstructionBuilder: (location: TvmInstLocation, inst: T) -> R,
    crossinline virtualEntriesFactory: (T) -> List<Pair<TvmStackEntryType, IRNode>> = { _ -> listOf() },
    virtualEntriesOffset: Int = 0
) {
    registry.register (ParserLevel.MANUAL) { ctx: IrBlockBuilder, inst: T ->
        val virtualOffsetEntries = (0 until virtualEntriesOffset).map {
            ctx.stackPop()
        }
        val virtualEntries = virtualEntriesFactory(inst)
        for ((entryType, declaration) in virtualEntries) {
            val concrete = when (declaration) {
                is IRNode.IntLiteral -> ConcreteValue.IntVal(declaration.literal.toString())
                is IRNode.SliceLiteral -> ConcreteValue.SliceVal(declaration.slice)
                else -> null
            }
            val virtualEntry = StackEntry.Simple(entryType, StackEntryName.Const("virtual"), concrete)
            ctx.pushVirtual(virtualEntry)
            ctx.appendNode(IRNode.VariableDeclaration(listOf(virtualEntry), declaration))
        }
        virtualOffsetEntries.reversed().forEach {
            ctx.stackPush(it)
        }

        registry.parse(ctx, realInstructionBuilder(inst.location, inst))
    }
}

fun registerVirtualInstructions(registry: ParserRegistry) {
    registerVirtualInstruction<TvmCompareIntLessintInst, TvmCompareIntLessInst>(
        registry,
        { location, _ -> TvmCompareIntLessInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.y)
        ) },
    )
    registerVirtualInstruction<TvmCompareIntGtintInst, TvmCompareIntGreaterInst>(
        registry,
        { location, _ -> TvmCompareIntGreaterInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.y)
        ) },
    )
    registerVirtualInstruction<TvmCellBuildStiInst, TvmCellBuildStixInst>(
        registry,
        { location, _ -> TvmCellBuildStixInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.c + 1)
        ) }
    )
    registerVirtualInstruction<TvmCellBuildStuInst, TvmCellBuildStuxInst>(
        registry,
        { location, _ -> TvmCellBuildStuxInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.c + 1)
        ) },
    )
    registerVirtualInstruction<TvmExceptionsThrowifnotInst, TvmExceptionsThrowanyifnotInst>(
        registry,
        { location, inst -> TvmExceptionsThrowanyifnotInst(location) },
        { inst ->
            listOf(
                TvmStackEntryType.INT to IRNode.IntLiteral(inst.n)
            )
        },
        1
    )
    registerVirtualInstruction<TvmExceptionsThrowifnotShortInst, TvmExceptionsThrowifnotInst>(
        registry,
        { location, inst -> TvmExceptionsThrowifnotInst(location, inst.n) }
    )
    registerVirtualInstruction<TvmExceptionsThrowifInst, TvmExceptionsThrowanyifInst>(
        registry,
        { location, inst -> TvmExceptionsThrowanyifInst(location) },
        { inst ->
            listOf(
                TvmStackEntryType.INT to IRNode.IntLiteral(inst.n)
            )
        },
        1
    )
    registerVirtualInstruction<TvmExceptionsThrowifShortInst, TvmExceptionsThrowifInst>(
        registry,
        { location, inst -> TvmExceptionsThrowifInst(location, inst.n) }
    )
    registerVirtualInstruction<TvmCellParsePlduInst, TvmCellParsePlduxInst>(
        registry,
        { location, _ -> TvmCellParsePlduxInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.c + 1)
        ) }
    )
    registerVirtualInstruction<TvmCellParseLduInst, TvmCellParseLduxInst>(
        registry,
        { location, _ -> TvmCellParseLduxInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.c + 1)
        ) }
    )
    registerVirtualInstruction<TvmCellParsePldiInst, TvmCellParsePldixInst>(
        registry,
        { location, _ -> TvmCellParsePldixInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.c + 1)
        ) }
    )
    registerVirtualInstruction<TvmCellParseLdiInst, TvmCellParseLdixInst>(
        registry,
        { location, _ -> TvmCellParseLdixInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.c + 1)
        ) }
    )
    registerVirtualInstruction<TvmTupleIndexInst, TvmTupleIndexvarInst>(
        registry,
        { location, _ -> TvmTupleIndexvarInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.k)
        ) }
    )
    registerVirtualInstruction<TvmCompareIntEqintInst, TvmCompareIntEqualInst>(
        registry,
        { location, _ -> TvmCompareIntEqualInst(location) },
        {
            inst -> listOf(
                TvmStackEntryType.INT to IRNode.IntLiteral(inst.y)
            )
        }
    )
    registerVirtualInstruction<TvmCompareIntNeqintInst, TvmCompareIntNeqInst>(
        registry,
        { location, _ -> TvmCompareIntNeqInst(location) },
        {
            inst -> listOf(
                TvmStackEntryType.INT to IRNode.IntLiteral(inst.y)
            )
        }
    )
    registerVirtualInstruction<TvmArithmBasicIncInst, TvmArithmBasicAddInst>(
        registry,
        { location, _ -> TvmArithmBasicAddInst(location) },
        {
            _ -> listOf(
                TvmStackEntryType.INT to IRNode.IntLiteral(1)
            )
        }
    )
    registerVirtualInstruction<TvmArithmBasicDecInst, TvmArithmBasicAddInst>(
        registry,
        { location, _ -> TvmArithmBasicAddInst(location) },
        {
            _ -> listOf(
                TvmStackEntryType.INT to IRNode.IntLiteral(-1)
            )
        }
    )
    registerVirtualInstruction<TvmArithmBasicMulconstInst, TvmArithmBasicMulInst>(
        registry,
        { location, inst -> TvmArithmBasicMulInst(location) },
        {
            inst -> listOf(
                TvmStackEntryType.INT to IRNode.IntLiteral(inst.c)
            )
        }
    )
    registerVirtualInstruction<TvmArithmBasicAddconstInst, TvmArithmBasicAddInst>(
        registry,
        { location, _ -> TvmArithmBasicAddInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.c)
        ) }
    )
    registerVirtualInstruction<TvmExceptionsThrowInst, TvmExceptionsThrowanyInst>(
        registry,
        { location, _ -> TvmExceptionsThrowanyInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.n)
        ) }
    )
    registerVirtualInstruction<TvmExceptionsThrowShortInst, TvmExceptionsThrowInst>(
        registry,
        { location, inst -> TvmExceptionsThrowInst(location, inst.n) }
    )
    registerVirtualInstruction<TvmExceptionsThrowargInst, TvmExceptionsThrowarganyInst>(
        registry,
        { location, _ -> TvmExceptionsThrowarganyInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.n)
        ) }
    )
    registerVirtualInstruction<TvmExceptionsThrowargifInst, TvmExceptionsThrowarganyifInst>(
        registry,
        { location, _ -> TvmExceptionsThrowarganyifInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.n)
        ) },
        1
    )
    registerVirtualInstruction<TvmExceptionsThrowargifnotInst, TvmExceptionsThrowarganyifnotInst>(
        registry,
        { location, _ -> TvmExceptionsThrowarganyifnotInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.n)
        ) },
        1
    )
    registerVirtualInstruction<TvmCellParseLdsliceInst, TvmCellParseLdslicexInst>(
        registry,
        { location, _ -> TvmCellParseLdslicexInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.c + 1)
        ) }
    )
    registerVirtualInstruction<TvmCellParsePldsliceInst, TvmCellParsePldslicexInst>(
        registry,
        { location, _ -> TvmCellParsePldslicexInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.c + 1)
        ) }
    )
    registerVirtualInstruction<TvmArithmDivModpow2Inst, TvmArithmDivModInst>(
        registry,
        { location, _ -> TvmArithmDivModInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(BigInteger.TWO.pow(inst.t + 1))
        ) }
    )
    registerVirtualInstruction<TvmArithmDivModpow2rInst, TvmArithmDivModrInst>(
        registry,
        { location, _ -> TvmArithmDivModrInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(BigInteger.TWO.pow(inst.t + 1))
        ) }
    )
    registerVirtualInstruction<TvmArithmDivModpow2cInst, TvmArithmDivModcInst>(
        registry,
        { location, _ -> TvmArithmDivModcInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(BigInteger.TWO.pow(inst.t + 1))
        ) }
    )
    registerVirtualInstruction<TvmArithmDivMulrshiftInst, TvmArithmDivMuldivInst>(
        registry,
        { location, _ -> TvmArithmDivMuldivInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(BigInteger.TWO.pow(inst.t + 1))
        ) }
    )
    registerVirtualInstruction<TvmArithmDivMulrshiftrInst, TvmArithmDivMuldivrInst>(
        registry,
        { location, _ -> TvmArithmDivMuldivrInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(BigInteger.TWO.pow(inst.t + 1))
        ) }
    )
    registerVirtualInstruction<TvmArithmDivMulrshiftcInst, TvmArithmDivMuldivcInst>(
        registry,
        { location, _ -> TvmArithmDivMuldivcInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(BigInteger.TWO.pow(inst.t + 1))
        ) }
    )
    registerVirtualInstruction<TvmArithmDivLshiftdivInst, TvmArithmDivMuldivInst>(
        registry,
        { location, _ -> TvmArithmDivMuldivInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(BigInteger.TWO.pow(inst.t + 1))
        ) },
        1
    )
    registerVirtualInstruction<TvmArithmDivLshiftdivrInst, TvmArithmDivMuldivrInst>(
        registry,
        { location, _ -> TvmArithmDivMuldivrInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(BigInteger.TWO.pow(inst.t + 1))
        ) },
        1
    )
    registerVirtualInstruction<TvmArithmDivLshiftdivcInst, TvmArithmDivMuldivcInst>(
        registry,
        { location, _ -> TvmArithmDivMuldivcInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(BigInteger.TWO.pow(inst.t + 1))
        ) },
        1
    )
    registerVirtualInstruction<TvmArithmLogicalLshiftInst, TvmArithmLogicalLshiftVarInst>(
        registry,
        { location, _ -> TvmArithmLogicalLshiftVarInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.c + 1)
        ) }
    )
    registerVirtualInstruction<TvmArithmLogicalRshiftInst, TvmArithmLogicalRshiftVarInst>(
        registry,
        { location, _ -> TvmArithmLogicalRshiftVarInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.c + 1)
        ) }
    )
    registerVirtualInstruction<TvmArithmDivRshiftrInst, TvmArithmDivRshiftrVarInst>(
        registry,
        { location, _ -> TvmArithmDivRshiftrVarInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.t + 1)
        ) }
    )
    registerVirtualInstruction<TvmArithmDivRshiftcInst, TvmArithmDivRshiftcVarInst>(
        registry,
        { location, _ -> TvmArithmDivRshiftcVarInst(location) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.t + 1)
        ) }
    )
}
