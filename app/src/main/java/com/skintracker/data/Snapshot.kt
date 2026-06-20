package com.skintracker.data

import kotlinx.serialization.Serializable

/** Snapshot metadata persisted in DataStore as a JSON array.
 * The full entry data lives as a separate file in filesDir/snapshots/snap_{id}.json —
 * keeping it out of DataStore avoids hitting the key-size limit for large datasets.
 * Mirrors the web app's snapshot shape (CLAUDE.md "Snapshot Schema"), minus the inline data map. */
@Serializable
data class SnapshotMeta(
    val id: Long,
    val name: String,
    val ts: Long,
    val days: Int,
)
