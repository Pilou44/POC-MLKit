package com.wechantloup.facedetection.provider

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.database.Cursor
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
import android.util.Log
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

@SuppressLint("InlinedApi")
class PhoneAlbumsLoader(
    private val contentResolver: ContentResolver,
    private val continuation: Continuation<List<PhotoAlbum>>,
) {
    private val albums = mutableMapOf<String, PhotoAlbum>()

    fun loadAlbums() {
        var cursor: Cursor? = null

        try {
            cursor = contentResolver.query(EXTERNAL_CONTENT_URI, PROJECTION_BUCKETS, null, null, SORT_ORDER)

            cursor.ifValid {
                do {
                    val bucketId = getString(getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID))

                    if (!albums.containsKey(bucketId)) {
                        albums[bucketId] = createProviderAlbumFromId(bucketId)
                    }
                } while (moveToNext())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading local provider albums", e)
        } finally {
            cursor?.close()
        }

        val albumsList = albums.map { it.value }

        continuation.resume(albumsList)
    }

    private fun Cursor.createProviderAlbumFromId(bucketId: String): PhotoAlbum {
        val bucketName = getString(getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME))

        val photos = getMostRecentPhotoPath(bucketId, bucketName)
        val count = getCount(bucketId, bucketName)

        return PhotoAlbum(bucketId, bucketName, count, photos)
    }

    private fun getMostRecentPhotoPath(bucketId: String, bucketName: String): List<String> {
        val photos = LocalGalleryProvider.getAlbumFirstImages(
            contentResolver,
            bucketName,
            bucketId
        )

        return photos.map { it.uri }
    }

    private fun getCount(bucketId: String, bucketName: String): Int {
        var cursor: Cursor? = null
        var photoCount = 0

        try {
            cursor = contentResolver.query(
                EXTERNAL_CONTENT_URI,
                null,
                "${MediaStore.Images.Media.BUCKET_ID}=?",
                arrayOf(bucketId),
                null
            )

            cursor.ifValid { photoCount = count }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading local provider album identified by id `$bucketName`", e)
        } finally {
            cursor?.close()
        }

        return photoCount
    }

    private fun Cursor?.ifValid(action: Cursor.() -> Unit) {
        if (this != null && moveToFirst()) action()
    }

    companion object {
        private const val TAG = "PhoneAlbumsLoader"

        private const val SORT_ORDER =
            " CASE ${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} WHEN  'Camera' THEN 1 ELSE 2 END "
        private val PROJECTION_BUCKETS = arrayOf(
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID
        )

        const val ALL_PHOTOS_EMULATED_BUCKET = "ALL_PHOTOS_EMULATED_BUCKET"
    }
}
