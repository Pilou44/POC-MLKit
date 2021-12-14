package com.wechantloup.facedetection.provider

data class PhotoAlbum(
    val id: String,
    val title: String,
    val photoCount: Int,
    val coverPhotoPath: List<String>,
)
