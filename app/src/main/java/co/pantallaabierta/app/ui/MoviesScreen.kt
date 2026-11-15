package co.pantallaabierta.app.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import co.pantallaabierta.app.PlayerActivity
import co.pantallaabierta.app.data.Archive
import co.pantallaabierta.app.data.Title
import co.pantallaabierta.app.data.TitleDetail
import co.pantallaabierta.app.openOfficialSite
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ok<T>(val value: T) : Load<T>
    data class Error(val message: String) : Load<Nothing>
}

class MoviesViewModel : ViewModel() {
    val shelves = mutableStateMapOf<String, Load<List<Title>>>()
    var searchResult by mutableStateOf<Load<List<Title>>?>(null)
    var selected by mutableStateOf<Title?>(null)
    var detail by mutableStateOf<Load<TitleDetail>?>(null)

    init { loadShelves() }

    fun loadShelves() {
        Archive.shelves.forEach { shelf ->
            shelves[shelf.name] = Load.Loading
            viewModelScope.launch {
                shelves[shelf.name] = runCatching { Archive.list(shelf.query) }
                    .fold({ Load.Ok(it) }, { Load.Error("No se pudo cargar. Revisa tu conexión.") })
            }
        }
    }

    fun search(text: String) {
        if (text.isBlank()) { searchResult = null; return }
        searchResult = Load.Loading
        viewModelScope.launch {
            searchResult = runCatching { Archive.list(Archive.searchQuery(text), rows = 50) }
                .fold({ Load.Ok(it) }, { Load.Error("No se pudo buscar. Revisa tu conexión.") })
        }
    }

    fun open(title: Title) {
        selected = title
        detail = Load.Loading
        viewModelScope.launch {
            detail = runCatching { Archive.detail(title) }
                .fold({ Load.Ok(it) }, { Load.Error("No se pudo abrir este título.") })
        }
    }

    fun close() { selected = null; detail = null }
}

@Composable
fun MoviesScreen(vm: MoviesViewModel = viewModel()) {
    if (vm.selected != null) {
        BackHandler { vm.close() }
        DetailScreen(vm.detail, onBack = vm::close)
    } else {
        Catalog(vm)
    }
}

@Composable
private fun Catalog(vm: MoviesViewModel) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    var query by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }

    LazyColumn(contentPadding = PaddingValues(vertical = 12.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(horizontal = 12.dp)) {
                Text("Películas y series", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Solo títulos de dominio público o con licencia libre, y cine colombiano gratis en su plataforma oficial.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Buscar película o serie") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = ""; vm.search("") }) {
                            Icon(Icons.Filled.Clear, "Borrar")
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { vm.search(query); focus.clearFocus() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                RtvcPlayBanner { openOfficialSite(context, "https://www.rtvcplay.co") }
            }
        }

        val search = vm.searchResult
        if (search != null) {
            item { ShelfRow("Resultados", search, vm::open) }
        } else {
            Archive.shelves.forEach { shelf ->
                item(key = shelf.name) {
                    ShelfRow(shelf.name, vm.shelves[shelf.name] ?: Load.Loading, vm::open)
                }
            }
        }
        item {
            Text(
                "Catálogo: Internet Archive (archive.org). Cine colombiano: RTVC Play.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
private fun RtvcPlayBanner(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F4E9C)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Cine y series colombianas gratis", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    "Abre RTVC Play, la plataforma pública y gratuita de Colombia.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                )
            }
            Icon(Icons.Filled.PlayArrow, null, tint = Color(0xFFFFC72C), modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
private fun ShelfRow(name: String, state: Load<List<Title>>, onOpen: (Title) -> Unit) {
    Column(Modifier.padding(top = 18.dp)) {
        Text(name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp))
        Spacer(Modifier.height(8.dp))
        when (state) {
            is Load.Loading -> Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is Load.Error -> Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp))
            is Load.Ok -> if (state.value.isEmpty()) {
                Text("Sin resultados.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp))
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.value, key = { it.id }) { t -> Poster(t) { onOpen(t) } }
                }
            }
        }
    }
}

@Composable
private fun Poster(title: Title, onClick: () -> Unit) {
    Column(Modifier.width(120.dp).clickable(onClick = onClick)) {
        AsyncImage(
            model = title.thumbnail,
            contentDescription = title.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(120.dp)
                .height(170.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Text(title.name, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        if (title.year.isNotBlank()) {
            Text(title.year, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DetailScreen(state: Load<TitleDetail>?, onBack: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Volver") }
            Text("Volver", modifier = Modifier.clickable(onClick = onBack))
        }
        when (state) {
            null, is Load.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is Load.Error -> Text(state.message, modifier = Modifier.padding(16.dp))
            is Load.Ok -> {
                val d = state.value
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    item {
                        Row {
                            AsyncImage(
                                model = d.title.thumbnail,
                                contentDescription = d.title.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.width(110.dp).height(160.dp).clip(RoundedCornerShape(10.dp)),
                            )
                            Column(Modifier.padding(start = 14.dp)) {
                                Text(d.title.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                if (d.title.year.isNotBlank()) Text(d.title.year, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    Archive.licenseLabel(d.title.license),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                                TextButton(onClick = { openOfficialSite(context, d.title.license) }) {
                                    Text("Ver licencia", fontSize = 12.sp)
                                }
                            }
                        }
                        if (d.description.isNotBlank()) {
                            Text(
                                d.description,
                                maxLines = 8,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        }
                        if (d.episodes.isEmpty()) {
                            Text("Este título no tiene un video reproducible.", modifier = Modifier.padding(top = 12.dp))
                        } else if (d.episodes.size > 1) {
                            Text("${d.episodes.size} videos", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                    items(d.episodes) { ep ->
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(context, PlayerActivity::class.java)
                                        .putExtra(PlayerActivity.EXTRA_URL, ep.url)
                                        .putExtra(PlayerActivity.EXTRA_TITLE, ep.label),
                                )
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        ) {
                            Icon(Icons.Filled.PlayArrow, null)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (d.episodes.size == 1) "Reproducir" else ep.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
