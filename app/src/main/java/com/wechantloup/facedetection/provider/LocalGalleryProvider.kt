package com.wechantloup.facedetection.provider

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import com.wechantloup.facedetection.provider.PhoneAlbumsLoader.Companion.ALL_PHOTOS_EMULATED_BUCKET
import kotlin.coroutines.suspendCoroutine

object LocalGalleryProvider {

    private const val TAG = "LocalGalleryProvider"

    private const val PAGINATION_COUNT = 50
    private val UNSUPPORTED_IMG_TYPE = setOf("gif")

    @SuppressLint("InlinedApi")
    private val PHOTO_PROJECTION = arrayOf(
        BaseColumns._ID,
        MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.MIME_TYPE,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.ORIENTATION,
        MediaStore.Images.ImageColumns.DATE_TAKEN,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.DATE_MODIFIED
    )

    private var albumsLoading = false
    private var lastLoadedPageNumber = -1
    private var currentAlbumId: String? = null

    private val currentAlbumFetchedPhotos: MutableList<Photo> = mutableListOf()
    private val fetchedAlbums = arrayListOf<PhotoAlbum>()

    fun openAlbum(albumId: String) {
        currentAlbumId = albumId
        currentAlbumFetchedPhotos.clear()
        lastLoadedPageNumber = - 1
    }

    fun hasMorePhotos(): Boolean {
        val currentAlbum = fetchedAlbums.find { it.id == currentAlbumId } ?: return true
        return currentAlbumFetchedPhotos.count() < currentAlbum.photoCount
    }

    suspend fun getNextAlbums(context: Context): List<PhotoAlbum> {
        val contentResolver = context.contentResolver

        albumsLoading = true

        val albums: List<PhotoAlbum> = suspendCoroutine {
            PhoneAlbumsLoader(contentResolver, it).loadAlbums()
        }

        val defaultPhotoAlbum = createDefaultPhotoAlbum(contentResolver, albums)

        fetchedAlbums.clear()
        fetchedAlbums.add(defaultPhotoAlbum)
        fetchedAlbums.addAll(albums)

        albumsLoading = false

        return fetchedAlbums
    }

    fun getNextPhotos(context: Context): List<Photo> {
        if (!hasMorePhotos()) return emptyList()

        val album = fetchedAlbums.find { it.id == currentAlbumId } ?: return emptyList()

        lastLoadedPageNumber++

        val photos = fetchGalleryImages(context.contentResolver, album.title, currentAlbumId, lastLoadedPageNumber)

        currentAlbumFetchedPhotos.addAll(photos)

        return photos
    }

    private fun createDefaultPhotoAlbum(
        contentResolver: ContentResolver,
        albums: List<PhotoAlbum>
    ): PhotoAlbum {
        return PhotoAlbum(
            ALL_PHOTOS_EMULATED_BUCKET,
            getAllLocalPhotosTitle(),
            getAllLocalPhotosCount(contentResolver),
            albums.firstOrNull()?.coverPhotoPath.orEmpty(),
        )
    }

    private fun getAllLocalPhotosTitle(): String = "Gallery"

    private fun getAllLocalPhotosCount(contentResolver: ContentResolver): Int =
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null,
            null,
            null,
            null
        ).use { cursor ->
            if (cursor == null) {
                0
            } else {
                cursor.moveToFirst()
                cursor.count
            }
        }

    fun getAlbumFirstImages(
        contentResolver: ContentResolver,
        albumName: String,
        albumId: String?,
    ): List<Photo> =
        fetchGalleryImages(
            contentResolver,
            albumName,
            albumId,
            0,
        )

    @SuppressLint("InlinedApi")
    private fun fetchGalleryImages(
        contentResolver: ContentResolver,
        albumName: String,
        albumId: String?,
        pageNumber: Int,
    ): List<Photo> {
        val selection: String?
        val selectionArgs: Array<String>?
        if (albumId == ALL_PHOTOS_EMULATED_BUCKET) {
            selection = null
            selectionArgs = null
        } else {
            selection = MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME + "=?"
            selectionArgs = arrayOf(albumName)
        }

        val cursor = getCursor(contentResolver, selection, selectionArgs, pageNumber)
        return cursor.getNextPhotos()
    }

    @SuppressLint("InlinedApi")
    private fun getCursor(
        contentResolver: ContentResolver,
        selection: String?,
        selectionArgs: Array<String>?,
        pageNumber: Int
    ): Cursor? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Get All data in Cursor by sorting in DESC order
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                PHOTO_PROJECTION,
                Bundle().apply {
                    // Limit & Offset
                    putInt(ContentResolver.QUERY_ARG_LIMIT, PAGINATION_COUNT)
                    putInt(ContentResolver.QUERY_ARG_OFFSET, pageNumber * PAGINATION_COUNT)
                    // Sort function
                    putStringArray(
                        ContentResolver.QUERY_ARG_SORT_COLUMNS,
                        arrayOf(MediaStore.Images.ImageColumns.DATE_TAKEN)
                    )
                    putInt(
                        ContentResolver.QUERY_ARG_SORT_DIRECTION,
                        ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                    )
                    // Selection
                    putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                    putStringArray(
                        ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                        selectionArgs
                    )
                },
                null
            )
        } else {
            val sortOrder = MediaStore.Images.ImageColumns.DATE_TAKEN +
                " DESC LIMIT $PAGINATION_COUNT OFFSET ${pageNumber * PAGINATION_COUNT}"

            // Get All data in Cursor by sorting in DESC order
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                PHOTO_PROJECTION,
                selection,
                selectionArgs,
                sortOrder
            )
        }

    private fun Cursor?.getNextPhotos(): List<Photo> {
        if (this == null) return emptyList()

        val result = mutableListOf<Photo>()
        use { cursor ->
            if (cursor.isAfterLast) cursor.moveToFirst(); cursor.moveToPrevious() // reinit the cursor since it is used in several albums
            if (cursor.count <= 0) return emptyList()

            val mimeTypeCol = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)

            cursor.moveToPosition(-1)
            while (cursor.moveToNext()) {
                val mimeType = cursor.getString(mimeTypeCol)
                if (!isAllowedMimeType(mimeType)) continue

                result.addNextPhoto(cursor)
            }
        }

        return result
    }

    private fun isAllowedMimeType(mimeType: String?): Boolean {
        if (mimeType == null) return false
        return UNSUPPORTED_IMG_TYPE.none { mimeType.endsWith(it) }
    }

    @SuppressLint("InlinedApi")
    private fun MutableList<Photo>.addNextPhoto(cursor: Cursor) {
        val cursorDateReader = CursorDateReader(cursor)
        val idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID)
        val widthCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
        val heightCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
        val orientationCol = cursor.getColumnIndex(MediaStore.Images.Media.ORIENTATION)

        val width = cursor.getInt(widthCol)
        val height = cursor.getInt(heightCol)

        if (width == 0 || height == 0) {
            Log.e(TAG, "Null height or width found")
            return
        }

        val photoUri = ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            cursor.getLong(idCol)
        ).toString()

        val orientation = cursor.getInt(orientationCol)
        val newSize = localPhotoSize(width, height, orientation)

        val photo = Photo(
            id = photoUri,
            uri = photoUri,
            widthPx = newSize.width,
            heightPx = newSize.height,
            date = cursorDateReader.getDate() ?: 0L
        )
        add(photo)
    }

    /**
     * Width and height are used for autofill to calculate image ratio and determine the best layout according to it.
     * So if orientation is 90°, we need to invert width and height.
     * Otherwise, orientation is never used.
     */
    private fun localPhotoSize(width: Int, height: Int, orientation: Int): Size =
        if (orientation % 180 != 0) Size(height, width) else Size(width, height)
}
