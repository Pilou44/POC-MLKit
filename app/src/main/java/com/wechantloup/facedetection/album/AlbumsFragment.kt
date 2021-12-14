package com.wechantloup.facedetection.album

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.wechantloup.facedetection.GalleryActivity
import com.wechantloup.facedetection.GalleryViewModel
import com.wechantloup.facedetection.R
import com.wechantloup.facedetection.databinding.FragmentAlbumsBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AlbumsFragment : Fragment() {

    private var binding: FragmentAlbumsBinding? = null

    private var viewModel: GalleryViewModel? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAlbumsBinding.inflate(layoutInflater)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = (activity as GalleryActivity?)?.viewModel

        initList()

        subscribeToUpdates()

        if (savedInstanceState != null) return

        lifecycleScope.launch {
            viewModel?.getAlbums()
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun onAlbumClicked(id: String, albumTitle: String) {
        (activity as GalleryActivity).showAlbum(id, albumTitle)
    }

    private fun initList() {
        val columnCount = resources.getInteger(R.integer.albums_column_count)
        binding?.listAlbums?.apply {
            layoutManager = GridLayoutManager(context, columnCount)
            adapter = AlbumsAdapter(::onAlbumClicked)
        }
    }

    private fun subscribeToUpdates() {
        viewModel?.stateFlow
            ?.flowWithLifecycle(lifecycle)
            ?.onEach {
                (binding?.listAlbums?.adapter as AlbumsAdapter?)?.submitList(it.albums)
            }
            ?.launchIn(lifecycleScope)
    }
}