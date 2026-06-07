package io.swee.tvm.decompiler.internal

import org.ton.bytecode.TvmCell
import org.ton.ton4j.address.Address
import org.ton.ton4j.cell.Cell
import org.ton.ton4j.cell.CellBuilder
import java.math.BigInteger

object SliceLiteralChooser {

    fun sliceToFuncLiteral(cell: TvmCell): String? {
        if (cell.refs.isNotEmpty()) return null
        val ton4jCell = convertCell(cell)
        val bits = ton4jCell.toBitString()
        chooseStdAddress(bits)?.let { return it }
        chooseAscii(bits)?.let { return it }
        return chooseHex(cell)
    }

    fun chooseStdAddress(bits: String): String? {
        if (bits.length != 267) return null
        if (!bits.startsWith("100")) return null
        val wcBits = bits.substring(3, 11)
        val hashBits = bits.substring(11, 267)
        val wc = signedInt8(wcBits)
        val hash = BigInteger(hashBits, 2).toString(16).padStart(64, '0')
        val friendly = Address.of("$wc:$hash").toBounceable()
        return "\"$friendly\"a"
    }

    fun chooseAscii(bits: String): String? {
        if (bits.isEmpty() || bits.length % 8 != 0) return null
        val sb = StringBuilder()
        for (i in bits.indices step 8) {
            val b = bits.substring(i, i + 8).toInt(2)
            if (b < 0x20 || b > 0x7E || b == 0x22 || b == 0x5C) return null
            sb.append(b.toChar())
        }
        return "\"$sb\""
    }

    private fun chooseHex(cell: TvmCell): String {
        val inner = Literals.cellLiteral(cell).removePrefix("x{").removeSuffix("}")
        return "\"$inner\"s"
    }

    private fun signedInt8(bits: String): Int {
        val v = bits.toInt(2)
        return if (bits[0] == '1') v - 256 else v
    }

    private fun convertCell(tvmCell: TvmCell): Cell {
        val cb = CellBuilder.beginCell()
        cb.storeBits(tvmCell.data.bits)
        cb.storeRefs(tvmCell.refs.map { convertCell(it) })
        return cb.endCell()
    }
}
