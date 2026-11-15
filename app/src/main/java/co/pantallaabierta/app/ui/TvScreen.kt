package co.pantallaabierta.app.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.pantallaabierta.app.data.Channel
import co.pantallaabierta.app.data.ChannelRepository
import co.pantallaabierta.app.data.Favorites
import co.pantallaabierta.app.LivePlayerActivity
import co.pantallaabierta.app.openOfficialSite

private const val ALL = "Todos"
private const val FAVS = "Favoritos"

@Composable
fun TvScreen() {
    val context = LocalContext.current
    val channels = remember { ChannelRepository.load(context) }
    val favoritesStore = remember { Favorites(context) }
    var favorites by remember { mutableStateOf(favoritesStore.all()) }
    var filter by rememberSaveable { mutableStateOf(ALL) }

    val filters = listOf(ALL, FAVS) + channels.map { it.category }.distinct()
    val shown = when (filter) {
        ALL -> channels
        FAVS -> channels.filter { it.id in favorites }
        else -> channels.filter { it.category == filter }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text("TV de Colombia", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Toca un canal para verlo en vivo. La señal viene del canal oficial de cada uno en YouTube.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filters) { f ->
                        FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f) })
                    }
                }
            }
        }
        if (shown.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    if (filter == FAVS) "Toca el corazón de un canal para agregarlo aquí." else "No hay canales.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
        items(shown, key = { it.id }) { ch ->
            ChannelCard(
                channel = ch,
                isFavorite = ch.id in favorites,
                onToggleFavorite = { favorites = favoritesStore.toggle(ch.id) },
                onWatch = { watchLive(context, ch) },
                onWeb = { openOfficialSite(context, ch.url) },
            )
        }
    }
}

@Composable
private fun ChannelCard(
    channel: Channel,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onWatch: () -> Unit,
    onWeb: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color(channel.color))
                .clickable(onClick = onWatch),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                channel.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp),
            )
            IconButton(onClick = onToggleFavorite, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                    tint = Color.White,
                )
            }
        }
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(channel.category, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = onWatch, contentPadding = PaddingValues(horizontal = 10.dp)) {
                    Text("Ver en vivo", fontSize = 12.sp)
                }
                TextButton(onClick = onWeb, contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Text("Web oficial", fontSize = 12.sp)
                }
            }
        }
    }
}

/** Abre la transmisión en vivo dentro de la app; si el canal no tiene YouTube oficial, abre su web. */
private fun watchLive(context: Context, ch: Channel) {
    if (ch.youtubeChannel.isBlank()) {
        openOfficialSite(context, ch.url)
        return
    }
    context.startActivity(
        Intent(context, LivePlayerActivity::class.java)
            .putExtra(LivePlayerActivity.EXTRA_CHANNEL_ID, ch.youtubeChannel)
            .putExtra(LivePlayerActivity.EXTRA_NAME, ch.name),
    )
}
