package io.swee.tvm.decompiler.internal

import org.ton.bytecode.TvmCell
import org.ton.ton4j.cell.Cell
import org.ton.ton4j.cell.CellBuilder

object Literals {
    fun cellLiteral(cell: TvmCell): String {
        return convertCell(cell).print().trim()
    }

    private fun convertCell(tvmCell: TvmCell): Cell {
        val cb = CellBuilder.beginCell()
        cb.storeBits(tvmCell.data.bits)
        cb.storeRefs(tvmCell.refs.map { convertCell(it) })
        return cb.endCell()
    }
}
