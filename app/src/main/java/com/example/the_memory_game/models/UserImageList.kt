package com.example.the_memory_game.models

import com.google.firebase.firestore.PropertyName

data class UserImageList (
     @PropertyName("images") val images : List<String>? = null
    )
