package com.wechantloup.facedetection

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wechantloup.facedetection.provider.LocalGalleryProvider
import com.wechantloup.facedetection.provider.Photo
import com.wechantloup.facedetection.provider.PhotoAlbum
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class GalleryViewModel(application: Application): AndroidViewModel(application) {

    private val _stateFlow = MutableStateFlow(State())
    val stateFlow: StateFlow<State> = _stateFlow

    private val calendar = Calendar.getInstance()
    private val outputDateFormat = SimpleDateFormat(OUTPUT_DATE_FORMAT, Locale.getDefault())

    fun onAlbumClicked(albumId: String) {
        LocalGalleryProvider.openAlbum(albumId)
        _stateFlow.value = stateFlow.value.copy(
            photos = emptyList(),
        )
    }

    fun onPhotoClicked(photo: Photo) {
        _stateFlow.value = stateFlow.value.copy(
            selectedPhoto = photo,
        )
    }

    fun loadMorePhotos() {
        if (!LocalGalleryProvider.hasMorePhotos()) return

        val nextPhotos = LocalGalleryProvider.getNextPhotos(getApplication())
        val photos = mutableListOf<Any>().apply { addAll(stateFlow.value.photos) }
        photos.addWithDateSeparator(nextPhotos)
        _stateFlow.value = stateFlow.value.copy(
            photos = photos
        )
    }

    private fun MutableList<Any>.addWithDateSeparator(photos: List<Photo>) {
        photos.forEach { newPhoto ->
            if (isNewDate(this, newPhoto)) {
                val newDate = newPhoto.getDate()
                add(newDate)
            }
            add(newPhoto)
        }
    }

    private fun isNewDate(list: List<Any>, newPhoto: Photo): Boolean {
        if (list.isEmpty() || list.last() !is Photo) {
            return true
        }

        return (list.last() as Photo).getDate() != newPhoto.getDate()
    }

    private fun Photo.getDate(): String {
        val currentDate = Date(date)
        calendar.setTimeWithTimeZoneShift(currentDate)
        return outputDateFormat.format(calendar.time)
    }

    private fun Calendar.setTimeWithTimeZoneShift(date: Date) {
        time = date
        timeInMillis += TimeZone.getDefault().getOffset(timeInMillis)
    }

    data class State(
        val albums: List<PhotoAlbum> = emptyList(),
        val photos: List<Any> = emptyList(),
        val selectedPhoto: Photo? = null,
    )

    suspend fun getAlbums() {
        _stateFlow.value = stateFlow.value.copy(
            albums = LocalGalleryProvider.getNextAlbums(getApplication())
        )
    }

    class Factory(private val activity: Activity) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return GalleryViewModel(activity.application) as T
        }
    }

    companion object {
        private const val OUTPUT_DATE_FORMAT = "dd MMMM yyyy"
    }
}