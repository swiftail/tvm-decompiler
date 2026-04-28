package io.swee.tvm.decompiler.internal

import java.util.*
import kotlin.collections.LinkedHashSet

interface UpstreamStack {
    fun pop(): StackEntry
    fun fork(): UpstreamStack
    fun merge(other: UpstreamStack): List<StackEntry>
    fun getUsedEntries(): List<StackEntry>
    fun trackDiscoveries(other: UpstreamStack)
}

data class FixedUpstreamStack private constructor(
    private val entries: LinkedList<StackEntry>,
    private val usedEntries: LinkedHashSet<StackEntry>
) : UpstreamStack {
    constructor(
        args: List<FunctionArg>
    ) : this(
        args.mapTo(LinkedList()) {
            StackEntry.Simple(it.type, it.name?.let {
                StackEntryName.Const(it)
            } ?: StackEntryName.Const("arg"))
        },
        LinkedHashSet()
    )

    override fun pop(): StackEntry {
        val entry = entries.pop()
        usedEntries.add(entry)
        return entry
    }

    override fun fork(): UpstreamStack {
        return FixedUpstreamStack(LinkedList(entries), LinkedHashSet(usedEntries))
    }

    override fun merge(other: UpstreamStack): List<StackEntry> {
        other as FixedUpstreamStack
        usedEntries.addAll(other.usedEntries)

        val entriesToAdd = LinkedList<StackEntry>()
        while (this.entries.size > other.entries.size) {
            entriesToAdd += this.pop()
        }
        return entriesToAdd
    }

    override fun getUsedEntries(): List<StackEntry> {
        return this.usedEntries.toList()
    }

    override fun trackDiscoveries(other: UpstreamStack) {
        other as FixedUpstreamStack
        usedEntries.addAll(other.usedEntries)
    }
}

data class DiscoveryUpstreamStack private constructor(
    private val entries: LinkedList<StackEntry>,
    private val usedEntries: LinkedHashSet<StackEntry>,
    private var generatedCount: Int,
    private val pendingDiscoveries: LinkedList<StackEntry>
): UpstreamStack {
    constructor() : this(LinkedList(), LinkedHashSet(), 0, LinkedList())

    constructor(seededArgs: List<FunctionArg>) : this(
        seededArgs.mapTo(LinkedList()) {
            StackEntry.Simple(it.type, it.name?.let {
                StackEntryName.Const(it)
            } ?: StackEntryName.Const("arg"))
        },
        LinkedHashSet(),
        seededArgs.size,
        LinkedList()
    )

    override fun pop(): StackEntry {
        if (entries.isNotEmpty()) {
            val entry = entries.pop()
            usedEntries.add(entry)
            return entry
        }

        if (pendingDiscoveries.isNotEmpty()) {
            return pendingDiscoveries.pop()
        }

        val newArg = StackEntry.Simple(
            TvmStackEntryType.UNKNOWN,
            StackEntryName.Const("arg_$generatedCount")
        )
        generatedCount++

        usedEntries.add(newArg)
        return newArg
    }

    override fun fork(): UpstreamStack {
        return DiscoveryUpstreamStack(
            LinkedList(entries),
            LinkedHashSet(usedEntries),
            generatedCount,
            LinkedList(pendingDiscoveries)
        )
    }

    override fun merge(other: UpstreamStack): List<StackEntry> {
        other as DiscoveryUpstreamStack

        for (i in this.usedEntries.size until other.usedEntries.size) {
            val newEntry = other.usedEntries.elementAt(i)
            this.usedEntries.add(newEntry)
            this.generatedCount++
        }

        val entriesToAdd = LinkedList<StackEntry>()
        while (this.entries.size > other.entries.size) {
            entriesToAdd += this.entries.pop()
        }
        return entriesToAdd
    }

    override fun getUsedEntries(): List<StackEntry> {
        return usedEntries.toList()
    }

    override fun trackDiscoveries(other: UpstreamStack) {
        other as DiscoveryUpstreamStack
        for (i in this.usedEntries.size until other.usedEntries.size) {
            val newEntry = other.usedEntries.elementAt(i)
            this.usedEntries.add(newEntry)
            this.generatedCount++
            pendingDiscoveries.add(newEntry)
        }
    }
}
