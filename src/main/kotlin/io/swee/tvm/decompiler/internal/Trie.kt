package io.swee.tvm.decompiler.internal

class Trie<K, V> {
    val children: MutableMap<K, Trie<K, V>> = HashMap()
    var value: V? = null
        private set

    fun insert(keySequence: List<K>, value: V) {
        var node = this
        for (keyPart in keySequence) {
            node = node.children.computeIfAbsent(keyPart) { Trie() }
        }
        node.value = value
    }

    fun get(keySequence: List<K>): V? {
        var node: Trie<K, V>? = this
        for (keyPart in keySequence) {
            node = node!!.children[keyPart]
            if (node == null) return null
        }
        return node!!.value
    }

    fun getSubTrie(prefix: List<K>): Trie<K, V>? {
        var node: Trie<K, V>? = this
        for (keyPart in prefix) {
            node = node!!.children[keyPart]
            if (node == null) return null
        }
        return node
    }

    fun collectAllValues(): List<V> {
        val result: MutableList<V> = ArrayList()
        value?.let { result.add(it) }
        for (child in children.values) {
            result.addAll(child.collectAllValues())
        }
        return result
    }
}
