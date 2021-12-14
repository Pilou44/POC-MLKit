package com.wechantloup.facedetection

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.wechantloup.facedetection.album.AlbumsFragment
import com.wechantloup.facedetection.databinding.ActivityGalleryBinding
import com.wechantloup.facedetection.gallery.PhotosFragment
import com.wechantloup.facedetection.photoDisplay.DisplayPhotoFragment
import com.wechantloup.facedetection.provider.Photo

class GalleryActivity : AppCompatActivity() {

    val viewModel by viewModels<GalleryViewModel> {
        GalleryViewModel.Factory(this)
    }

    private lateinit var binding: ActivityGalleryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState != null) {
            title = savedInstanceState.getCharSequence(ARG_TITLE)
            return
        }

        title = "Gallery"
        val fragment = AlbumsFragment()
        supportFragmentManager.beginTransaction()
            .replace(binding.galleryContainer.id, fragment, TAG_FRAGMENT_ALBUMS)
            .commit()
    }

    override fun onBackPressed() {
        title = "Gallery"
        super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence(ARG_TITLE, title)
    }

    fun showAlbum(albumId: String, albumTitle: String) {
        title = albumTitle
        viewModel.onAlbumClicked(albumId)
        val fragment = PhotosFragment()
        supportFragmentManager.beginTransaction()
            .replace(binding.galleryContainer.id, fragment, TAG_FRAGMENT_PHOTOS)
            .addToBackStack(null)
            .commit()
    }

    fun showPhoto(photo: Photo) {
        title = photo.id
        viewModel.onPhotoClicked(photo)
        val fragment = DisplayPhotoFragment()
        supportFragmentManager.beginTransaction()
            .replace(binding.galleryContainer.id, fragment, TAG_FRAGMENT_PHOTO)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        private const val TAG_FRAGMENT_ALBUMS = "fragment_albums"
        private const val TAG_FRAGMENT_PHOTOS = "fragment_photos"
        private const val TAG_FRAGMENT_PHOTO = "fragment_photo"
        private const val ARG_TITLE = "title"
    }
}