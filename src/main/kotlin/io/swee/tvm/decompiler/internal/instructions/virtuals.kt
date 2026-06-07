package io.swee.tvm.decompiler.internal.instructions

import io.swee.tvm.decompiler.internal.*
import io.swee.tvm.decompiler.internal.TvmStackEntryType
import io.swee.tvm.decompiler.internal.ir.IRNode
import org.ton.bytecode.*
import org.ton.disasm.TvmPhysicalInstLocation
import java.math.BigInteger

private val DUMMY_PHYSICAL_LOCATION = TvmPhysicalInstLocation("", 0)

private inline fun <reified T : TvmInst, R : TvmInst> registerReversedStore(
    registry: ParserRegistry,
    crossinline target: (location: TvmInstLocation, physicalLocation: TvmPhysicalInstLocation, inst: T) -> R
) {
    registry.register(ParserLevel.MANUAL) { ctx: IrBlockBuilder, inst: T ->
        ctx.stackEnsureMoreThan(1)
        val ref = ctx.stackFetch(0)
        ctx.stackSet(0, ctx.stackFetch(1))
        ctx.stackSet(1, ref)
        val physLoc = (inst as? TvmRealInst)?.physicalLocation ?: DUMMY_PHYSICAL_LOCATION
        registry.parse(ctx, target(inst.location, physLoc, inst))
    }
}

private inline fun <reified T : TvmInst, R : TvmInst> registerVirtualInstruction(
    registry: ParserRegistry,
    crossinline realInstructionBuilder: (location: TvmInstLocation, physicalLocation: TvmPhysicalInstLocation, inst: T) -> R,
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

        val physLoc = (inst as? TvmRealInst)?.physicalLocation ?: DUMMY_PHYSICAL_LOCATION
        registry.parse(ctx, realInstructionBuilder(inst.location, physLoc, inst))
    }
}

fun registerVirtualInstructions(registry: ParserRegistry) {
    registerVirtualInstruction<TvmCompareIntLessintInst, TvmCompareIntLessInst>(
        registry,
        { location, pl, _ -> TvmCompareIntLessInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.y)
        ) },
    )
    registerVirtualInstruction<TvmCompareIntGtintInst, TvmCompareIntGreaterInst>(
        registry,
        { location, pl, _ -> TvmCompareIntGreaterInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.y)
        ) },
    )
    registerVirtualInstruction<TvmCellBuildStiInst, TvmCellBuildStixInst>(
        registry,
        { location, pl, _ -> TvmCellBuildStixInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.c + 1)
        ) }
    )
    registerVirtualInstruction<TvmCellBuildStuInst, TvmCellBuildStuxInst>(
        registry,
        { location, pl, _ -> TvmCellBuildStuxInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.c + 1)
        ) },
    )
    registerVirtualInstruction<TvmExceptionsThrowifnotInst, TvmExceptionsThrowanyifnotInst>(
        registry,
        { location, pl, inst -> TvmExceptionsThrowanyifnotInst(location, pl) },
        { inst ->
            listOf(
                TvmStackEntryType.INT to IRNode.IntLiteral(inst.n)
            )
        },
        1
    )
    registerVirtualInstruction<TvmExceptionsThrowifnotShortInst, TvmExceptionsThrowifnotInst>(
        registry,
        { location, pl, inst -> TvmExceptionsThrowifnotInst(location, pl, inst.n) }
    )
    registerVirtualInstruction<TvmExceptionsThrowifInst, TvmExceptionsThrowanyifInst>(
        registry,
        { location, pl, inst -> TvmExceptionsThrowanyifInst(location, pl) },
        { inst ->
            listOf(
                TvmStackEntryType.INT to IRNode.IntLiteral(inst.n)
            )
        },
        1
    )
    registerVirtualInstruction<TvmExceptionsThrowifShortInst, TvmExceptionsThrowifInst>(
        registry,
        { location, pl, inst -> TvmExceptionsThrowifInst(location, pl, inst.n) }
    )
    registerVirtualInstruction<TvmCellParsePlduInst, TvmCellParsePlduxInst>(
        registry,
        { location, pl, _ -> TvmCellParsePlduxInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.c + 1)
        ) }
    )
    registerVirtualInstruction<TvmCellParseLduInst, TvmCellParseLduxInst>(
        registry,
        { location, pl, _ -> TvmCellParseLduxInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.c + 1)
        ) }
    )
    registerVirtualInstruction<TvmCellParsePldiInst, TvmCellParsePldixInst>(
        registry,
        { location, pl, _ -> TvmCellParsePldixInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.c + 1)
        ) }
    )
    registerVirtualInstruction<TvmCellParseLdiInst, TvmCellParseLdixInst>(
        registry,
        { location, pl, _ -> TvmCellParseLdixInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.c + 1)
        ) }
    )
    registerVirtualInstruction<TvmTupleIndexInst, TvmTupleIndexvarInst>(
        registry,
        { location, pl, _ -> TvmTupleIndexvarInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.k)
        ) }
    )
    registerVirtualInstruction<TvmCompareIntEqintInst, TvmCompareIntEqualInst>(
        registry,
        { location, pl, _ -> TvmCompareIntEqualInst(location, pl) },
        {
            inst -> listOf(
                TvmStackEntryType.INT to IRNode.IntLiteral(inst.y)
            )
        }
    )
    registerVirtualInstruction<TvmCompareIntNeqintInst, TvmCompareIntNeqInst>(
        registry,
        { location, pl, _ -> TvmCompareIntNeqInst(location, pl) },
        {
            inst -> listOf(
                TvmStackEntryType.INT to IRNode.IntLiteral(inst.y)
            )
        }
    )
    registerVirtualInstruction<TvmArithmBasicIncInst, TvmArithmBasicAddInst>(
        registry,
        { location, pl, _ -> TvmArithmBasicAddInst(location, pl) },
        {
            _ -> listOf(
                TvmStackEntryType.INT to IRNode.IntLiteral(1)
            )
        }
    )
    registerVirtualInstruction<TvmArithmBasicDecInst, TvmArithmBasicAddInst>(
        registry,
        { location, pl, _ -> TvmArithmBasicAddInst(location, pl) },
        {
            _ -> listOf(
                TvmStackEntryType.INT to IRNode.IntLiteral(-1)
            )
        }
    )
    registerVirtualInstruction<TvmArithmBasicMulconstInst, TvmArithmBasicMulInst>(
        registry,
        { location, pl, inst -> TvmArithmBasicMulInst(location, pl) },
        {
            inst -> listOf(
                TvmStackEntryType.INT to IRNode.IntLiteral(inst.c)
            )
        }
    )
    registerVirtualInstruction<TvmArithmBasicAddconstInst, TvmArithmBasicAddInst>(
        registry,
        { location, pl, _ -> TvmArithmBasicAddInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.c)
        ) }
    )
    registerVirtualInstruction<TvmExceptionsThrowInst, TvmExceptionsThrowanyInst>(
        registry,
        { location, pl, _ -> TvmExceptionsThrowanyInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.n)
        ) }
    )
    registerVirtualInstruction<TvmExceptionsThrowShortInst, TvmExceptionsThrowInst>(
        registry,
        { location, pl, inst -> TvmExceptionsThrowInst(location, pl, inst.n) }
    )
    registerVirtualInstruction<TvmExceptionsThrowargInst, TvmExceptionsThrowarganyInst>(
        registry,
        { location, pl, _ -> TvmExceptionsThrowarganyInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.n)
        ) }
    )
    registerVirtualInstruction<TvmExceptionsThrowargifInst, TvmExceptionsThrowarganyifInst>(
        registry,
        { location, pl, _ -> TvmExceptionsThrowarganyifInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.n)
        ) },
        1
    )
    registerVirtualInstruction<TvmExceptionsThrowargifnotInst, TvmExceptionsThrowarganyifnotInst>(
        registry,
        { location, pl, _ -> TvmExceptionsThrowarganyifnotInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.n)
        ) },
        1
    )
    registerVirtualInstruction<TvmCellParseLdsliceInst, TvmCellParseLdslicexInst>(
        registry,
        { location, pl, _ -> TvmCellParseLdslicexInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.c + 1)
        ) }
    )
    registerVirtualInstruction<TvmCellParsePldsliceInst, TvmCellParsePldslicexInst>(
        registry,
        { location, pl, _ -> TvmCellParsePldslicexInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.c + 1)
        ) }
    )
    registerVirtualInstruction<TvmArithmDivModpow2Inst, TvmArithmDivModInst>(
        registry,
        { location, pl, _ -> TvmArithmDivModInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(BigInteger.TWO.pow(inst.t + 1))
        ) }
    )
    registerVirtualInstruction<TvmArithmDivModpow2rInst, TvmArithmDivModrInst>(
        registry,
        { location, pl, _ -> TvmArithmDivModrInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(BigInteger.TWO.pow(inst.t + 1))
        ) }
    )
    registerVirtualInstruction<TvmArithmDivModpow2cInst, TvmArithmDivModcInst>(
        registry,
        { location, pl, _ -> TvmArithmDivModcInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(BigInteger.TWO.pow(inst.t + 1))
        ) }
    )
    registerVirtualInstruction<TvmArithmDivMulrshiftInst, TvmArithmDivMuldivInst>(
        registry,
        { location, pl, _ -> TvmArithmDivMuldivInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(BigInteger.TWO.pow(inst.t + 1))
        ) }
    )
    registerVirtualInstruction<TvmArithmDivMulrshiftrInst, TvmArithmDivMuldivrInst>(
        registry,
        { location, pl, _ -> TvmArithmDivMuldivrInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(BigInteger.TWO.pow(inst.t + 1))
        ) }
    )
    registerVirtualInstruction<TvmArithmDivMulrshiftcInst, TvmArithmDivMuldivcInst>(
        registry,
        { location, pl, _ -> TvmArithmDivMuldivcInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(BigInteger.TWO.pow(inst.t + 1))
        ) }
    )
    registerVirtualInstruction<TvmArithmDivLshiftdivInst, TvmArithmDivMuldivInst>(
        registry,
        { location, pl, _ -> TvmArithmDivMuldivInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(BigInteger.TWO.pow(inst.t + 1))
        ) },
        1
    )
    registerVirtualInstruction<TvmArithmDivLshiftdivrInst, TvmArithmDivMuldivrInst>(
        registry,
        { location, pl, _ -> TvmArithmDivMuldivrInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(BigInteger.TWO.pow(inst.t + 1))
        ) },
        1
    )
    registerVirtualInstruction<TvmArithmDivLshiftdivcInst, TvmArithmDivMuldivcInst>(
        registry,
        { location, pl, _ -> TvmArithmDivMuldivcInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(BigInteger.TWO.pow(inst.t + 1))
        ) },
        1
    )
    registerVirtualInstruction<TvmArithmLogicalLshiftInst, TvmArithmLogicalLshiftVarInst>(
        registry,
        { location, pl, _ -> TvmArithmLogicalLshiftVarInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.c + 1)
        ) }
    )
    registerVirtualInstruction<TvmArithmLogicalRshiftInst, TvmArithmLogicalRshiftVarInst>(
        registry,
        { location, pl, _ -> TvmArithmLogicalRshiftVarInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.c + 1)
        ) }
    )
    registerVirtualInstruction<TvmArithmDivRshiftrInst, TvmArithmDivRshiftrVarInst>(
        registry,
        { location, pl, _ -> TvmArithmDivRshiftrVarInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.t + 1)
        ) }
    )
    registerVirtualInstruction<TvmArithmDivRshiftcInst, TvmArithmDivRshiftcVarInst>(
        registry,
        { location, pl, _ -> TvmArithmDivRshiftcVarInst(location, pl) },
        { inst -> listOf(
            TvmStackEntryType.INT to IRNode.IntLiteral(inst.t + 1)
        ) }
    )
    registerReversedStore<TvmCellBuildSturInst, TvmCellBuildStuInst>(
        registry,
        { location, pl, inst -> TvmCellBuildStuInst(location, pl, inst.c) }
    )
    registerReversedStore<TvmCellBuildStirInst, TvmCellBuildStiInst>(
        registry,
        { location, pl, inst -> TvmCellBuildStiInst(location, pl, inst.c) }
    )
    registerReversedStore<TvmCellBuildStrefrInst, TvmCellBuildStrefInst>(
        registry,
        { location, pl, _ -> TvmCellBuildStrefInst(location, pl) }
    )
    registry.register<TvmDebugDebugInst>(ParserLevel.MANUAL) { _, _ -> }
}
