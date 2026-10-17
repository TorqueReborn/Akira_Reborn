package com.ghostreborn.akira.features.player.models

/**
 * Represents a timestamped skip interval for Opening (OP), Ending (ED), or Recap segments.
 */
enum class SkipSegmentType {
    OPENING,
    ENDING,
    RECAP,
    MIXED
}

data class SkipInterval(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val type: SkipSegmentType = SkipSegmentType.OPENING,
    val label: String = "Skip Intro"
) {
    fun contains(positionMs: Long): Boolean = positionMs in startTimeMs..endTimeMs
}
