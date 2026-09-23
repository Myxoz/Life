package com.myxoz.life.ui.socialgraph

data class SocialGraph(
    val nodes: Set<Long>,
    val edges: Map<EdgeKey, Long>
) {
    fun totalWeight() = edges.values.sum()

    companion object {
        val EMPTY = SocialGraph(emptySet(), emptyMap())
    }

    @ConsistentCopyVisibility
    data class EdgeKey private constructor(
        val a: Long,
        val b: Long
    ) {
        operator fun contains(has: Long) = a == has || b == has
        fun oppositeTo(searchedNode: Long) = if(a == searchedNode) b else a

        companion object {
            fun of(a: Long, b: Long): EdgeKey {
                return if (a < b) EdgeKey(a, b)
                else EdgeKey(b, a)
            }
        }
    }
}
