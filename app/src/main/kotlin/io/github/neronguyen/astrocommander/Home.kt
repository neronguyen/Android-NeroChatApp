package io.github.neronguyen.astrocommander

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.neronguyen.astrocommander.core.model.Placeholder

@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
    onItemClick: (Placeholder) -> Unit
) {
    val list by viewModel.list.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    HomeScreen(
        list = list,
        error = error,
        onRefresh = viewModel::refresh,
        onItemClick = onItemClick,
        modifier = modifier
    )
}

@Composable
fun HomeScreen(
    list: List<Placeholder>,
    error: String,
    onRefresh: () -> Unit,
    onItemClick: (Placeholder) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Home",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRefresh) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reload")
            }
        }

        HorizontalDivider()

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            if (error.isNotEmpty()) {
                item {
                    Text(text = error, modifier = Modifier.padding(8.dp))
                }
            }

            items(list) { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(item) }
                        .padding(8.dp)
                ) {
                    AsyncImage(
                        model = "https://dummyjson.com/image/100?text=${item.id}",
                        contentDescription = "Item Image",
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "ID: ${item.id}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(text = item.title)
                    }
                }
            }
        }
    }
}
