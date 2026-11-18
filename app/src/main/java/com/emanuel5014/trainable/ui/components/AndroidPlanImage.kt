package com.emanuel5014.trainable.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.emanuel5014.trainable.util.UriMigrationHelper

/** The routine photos of the shared screens: Coil, after moving paths saved by older versions to where the file is now. */
val AndroidPlanImage: @Composable (uri: String, modifier: Modifier) -> Unit = { uri, modifier ->
    val context = LocalContext.current
    val fixedUri = remember(uri) { UriMigrationHelper.fixPath(uri, context) }
    AsyncImage(
        model = fixedUri,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}
