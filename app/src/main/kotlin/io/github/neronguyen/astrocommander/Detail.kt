package io.github.neronguyen.astrocommander

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.neronguyen.astrocommander.core.model.Placeholder

@Composable
fun DetailRoute(
    item: Placeholder,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val detailItem by viewModel.item.collectAsStateWithLifecycle()

    LaunchedEffect(item) {
        viewModel.setItem(item)
    }

    detailItem?.let {
        DetailScreen(item = it, modifier = modifier, onBack = onBack)
    }
}

@Composable
fun DetailScreen(
    item: Placeholder,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "Details",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        HorizontalDivider()

        AsyncImage(
            model = "https://dummyjson.com/image/400x200?text=${item.title.replace(" ", "+")}",
            contentDescription = "Detail Image",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentScale = ContentScale.Crop
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "ID: ${item.id}",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Title: ${item.title}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Completed: ${item.completed}",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
