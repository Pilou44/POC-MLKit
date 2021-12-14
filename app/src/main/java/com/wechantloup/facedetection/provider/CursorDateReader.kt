package com.wechantloup.facedetection.provider

import android.database.Cursor
import android.provider.MediaStore

class CursorDateReader(private val c: Cursor) {

    private val dateColumns = listOf(
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.DATE_MODIFIED
    ).asSequence()

    private val columnIndexSequence = dateColumns.map { c.getColumnIndex(it) }

    /**
     * Returns the most relevant date or null.
     * This is the order of importance:
     * - MediaStore.Images.Media.DATE_TAKEN,
     * - MediaStore.Images.Media.DATE_ADDED,
     * - MediaStore.Images.Media.DATE_MODIFIED
     *
     * @see MediaStore.Images.Media
     */
    fun getDate(): Long? {
        return columnIndexSequence
            .map { dateColumnIndex -> c.getLong(dateColumnIndex) }
            .firstOrNull { date -> date > 0 }
    }
}
