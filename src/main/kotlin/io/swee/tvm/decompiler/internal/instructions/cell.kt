package io.swee.tvm.decompiler.internal.instructions

import io.swee.tvm.decompiler.internal.*
import io.swee.tvm.decompiler.internal.ast.AstElement
import org.ton.bytecode.*

private fun registerCellParsingParsers(registry: ParserRegistry) {
    with(registry) {
//        register<TvmCellParseSrefsInst> { ctx, inst, ident ->
//            val slice = ctx.popSlice()
//            val res = newInt(name("val"))
//            ctx.push(res)
//            ctx.append(declaration(
//                res,
//                composition(
//                    usage(slice),
//                    AstElement.Raw(".slice_refs()")
//                )
//            )).append(AstElement.Raw(";"))
//        }
//        register<TvmCellParseLdslicexInst> { ctx, inst, ident ->
//            val amount = ctx.popInt()
//            val slice = ctx.popSlice()
//
//            val entryHead = newSlice(StackEntryName.Parent(slice.name, "head"))
//            val entryTail = copy(slice)
//
//            ctx
//                .push(entryHead)
//                .push(entryTail)
//
//            ctx
//                .append(destructurization(entryHead, entryTail))
//                .append(usage(slice))
//                .append(AstElement.Raw(".load_bits("))
//                .append(usage(amount))
//                .append(AstElement.Raw(");"))
//        }
        register<TvmCellParseLduInst> { ctx, inst, ident ->
            val entryInt = newInt(name("val"))
            val slice = ctx.popSlice()
            val entryTail = copy(slice)

            ctx
                .push(entryInt)
                .push(entryTail)
            ctx
                .append(
                    AstElement.VariableDeclaration(
                        listOf(
                            entryInt,
                            entryTail
                        ),
                        AstElement.FunctionCall(
                            "load_uint",
                            listOf(
                                AstElement.VariableUsage(slice, true),
                                AstElement.Literal((inst.c + 1).toLong())
                            )
                        )
                    )
                )
        }
//        register<TvmCellParsePlduInst> { ctx, inst, ident ->
//            val entryInt = newInt(name("val"))
//            val slice = ctx.popSlice()
//
//            ctx.push(entryInt)
//            ctx.append(
//                declaration(
//                    entryInt, composition(
//                        usage(slice),
//                        AstElement.Raw(".preload_uint(${inst.c + 1})")
//                    )
//                )
//            ).append(AstElement.Raw(";"))
//        }
//        register<TvmCellParseLdiInst> { ctx, inst, ident ->
//            val entryInt = newInt(name("val"))
//            val slice = ctx.popSlice()
//            val entryTail = copy(slice)
//
//            ctx
//                .push(entryInt)
//                .push(entryTail)
//            ctx
//                .append(
//                    destructurization(
//                        entryInt,
//                        entryTail
//                    )
//                )
//                .append(usage(slice))
//                .append(AstElement.Raw(".load_int(${inst.c + 1});"))
//        }
//        register<TvmCellParseLduxInst> { ctx, inst, ident ->
//            val entryInt = newInt(name("val"))
//            val len = ctx.popInt()
//            val slice = ctx.popSlice()
//            val entryTail = copy(slice)
//
//            ctx
//                .push(entryInt)
//                .push(entryTail)
//            ctx
//                .append(
//                    destructurization(
//                        entryInt,
//                        entryTail
//                    )
//                )
//                .append(usage(slice))
//                .append(AstElement.Raw(".load_uint("))
//                .append(usage(len))
//                .append(AstElement.Raw(")"))
//                .append(AstElement.Raw(";"))
//        }
//        register<TvmCellParseLdixInst> { ctx, inst, ident ->
//            val entryInt = newInt(name("val"))
//            val len = ctx.popInt()
//            val slice = ctx.popSlice()
//            val entryTail = copy(slice)
//
//            ctx
//                .push(entryInt)
//                .push(entryTail)
//            ctx
//                .append(
//                    destructurization(
//                        entryInt,
//                        entryTail
//                    )
//                )
//                .append(usage(slice))
//                .append(AstElement.Raw(".load_int("))
//                .append(usage(len))
//                .append(AstElement.Raw(")"))
//                .append(AstElement.Raw(";"))
//        }
//        register<TvmAppAddrLdmsgaddrInst> { ctx, inst, ident ->
//            val entryAddr = newSlice(name("addr"))
//            val slice = ctx.popSlice()
//            val entryTail = copy(slice)
//
//            ctx
//                .push(entryAddr)
//                .push(entryTail)
//            ctx
//                .append(
//                    destructurization(
//                        entryAddr,
//                        entryTail
//                    )
//                )
//                .append(usage(slice))
//                .append(AstElement.Raw(".load_msg_addr();"))
//        }
//        register<TvmAppCurrencyLdgramsInst> { ctx, inst, ident ->
//            val entryInt = newInt(name("amount"))
//            val slice = ctx.popSlice()
//            val entryTail = copy(slice)
//
//            ctx
//                .push(entryInt)
//                .push(entryTail)
//            ctx
//                .append(
//                    destructurization(
//                        entryInt,
//                        entryTail
//                    )
//                )
//                .append(usage(slice))
//                .append(AstElement.Raw(".load_coins();"))
//        }
//        register<TvmCellParseLdrefInst> { ctx, inst, ident ->
//            val entryCell = newCell(name("ref"))
//            val slice = ctx.popSlice()
//            val entryTail = copy(slice)
//
//            ctx
//                .push(entryCell)
//                .push(entryTail)
//            ctx
//                .append(
//                    destructurization(
//                        entryCell,
//                        entryTail
//                    )
//                )
//                .append(usage(slice))
//                .append(AstElement.Raw(".load_ref();"))
//        }
//        register<TvmDictSerialLddictInst> { ctx, inst, ident ->
//            val entryDict = newCell(name("dict"))
//            val slice = ctx.popSlice()
//            val entryTail = copy(slice)
//
//            ctx
//                .push(entryDict)
//                .push(entryTail)
//            ctx
//                .append(
//                    destructurization(
//                        entryDict,
//                        entryTail
//                    )
//                )
//                .append(usage(slice))
//                .append(AstElement.Raw(".load_maybe_ref();"))
//        }
//        register<TvmCellParseEndsInst> { ctx, inst, ident ->
//            val entrySlice = ctx.popSlice()
//            ctx.append(usage(entrySlice)).append(AstElement.Raw(".end_parse();"))
//        }
//        register<TvmCompareOtherSemptyInst> { ctx, inst, ident ->
//            val entrySlice = ctx.popSlice()
//            val res = newInt(name("result"))
//            ctx.push(res)
//            ctx.append(declaration(res, composition(usage(entrySlice), AstElement.Raw(".slice_empty()")))).append(AstElement.Raw(";"))
//        }
//        register<TvmCompareOtherSdeqInst> { ctx, inst, ident ->
//            val a = ctx.popSlice()
//            val b = ctx.popSlice()
//            val res = newInt(name("result"))
//            ctx.push(res)
//            ctx.append(declaration(res, composition(
//                AstElement.Raw("equal_slice_bits("),
//                usage(a),
//                AstElement.Raw(", "),
//                usage(b),
//                AstElement.Raw(")")
//            ))).append(AstElement.Raw(";"))
//        }
//        register<TvmCellParseSbitsInst> { ctx, inst, ident ->
//            val entrySlice = ctx.popSlice()
//            val res = newInt(name("bits"))
//            ctx.push(res)
//            ctx.append(declaration(res, composition(usage(entrySlice), AstElement.Raw(".slice_bits()")))).append(AstElement.Raw(";"))
//        }
//        register<TvmAppAddrRewritestdaddrInst> { ctx, inst, ident ->
//            val entrySlice = ctx.popSlice()
//            val res1 = newInt(name("wc"))
//            val res2 = newInt(name("addr_hash"))
//            ctx.push(res1)
//            ctx.push(res2)
//            ctx.append(destructurization(
//                res1,
//                res2
//            )).append(AstElement.Raw("parse_std_addr(")).append(usage(entrySlice)).append(AstElement.Raw(");"))
//        }
    }
}

fun registerCellParsers(registry: ParserRegistry) {
    registerCellParsingParsers(registry)
}
