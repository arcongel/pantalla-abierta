package co.pantallaabierta.app.ui

import android.app.Application
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import co.pantallaabierta.app.PlayerActivity
import co.pantallaabierta.app.data.M3u
import co.pantallaabierta.app.data.M3uChannel
import co.pantallaabierta.app.data.M3uList
import co.pantallaabierta.app.data.SavedLists
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.net.URLEncoder

class ListsViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SavedLists(app)
    var lists by mutableStateOf(store.all())
    var noticeAccepted by mutableStateOf(store.acceptedNotice())
    var open by mutableStateOf<M3uList?>(null)
    var channels by mutableStateOf<Load<List<M3uChannel>>?>(null)

    fun acceptNotice() { store.acceptNotice(); noticeAccepted = true }
    fun add(name: String, url: String) { lists = store.add(M3uList(name.ifBlank { "Mi lista" }, url.trim())) }
    fun remove(l: M3uList) { lists = store.remove(l) }

    fun openList(l: M3uList) {
        open = l
        channels = Load.Loading
        viewModelScope.launch {
            channels = runCatching { M3u.load(l.url) }
                .fold({ Load.Ok(it) }, { Load.Error("No se pudo cargar la lista: ${it.message ?: "error de conexión"}") })
        }
    }

    fun close() { open = null; channels = null }
}

@Composable
fun ListsScreen(vm: ListsViewModel = viewModel()) {
    if (!vm.noticeAccepted) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Tus propias listas") },
            text = {
                Text(
                    "Pantalla Abierta no incluye ninguna lista M3U. Aquí puedes cargar listas propias, " +
                        "como la que te da un proveedor de TV que contrataste.\n\n" +
                        "Carga solo listas que tengas derecho a ver. Retransmitir canales sin permiso " +
                        "de sus dueños es ilegal, y la responsabilidad por lo que cargues es tuya.",
                )
            },
            confirmButton = { TextButton(onClick = vm::acceptNotice) { Text("Entendido") } },
        )
    }
    val open = vm.open
    if (open != null) {
        BackHandler { vm.close() }
        ChannelList(open, vm.channels, onBack = vm::close)
    } else {
        SavedListsView(vm)
    }
}

@Composable
private fun SavedListsView(vm: ListsViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    val validUrl = url.trim().startsWith("http://") || url.trim().startsWith("https://")

    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Mis listas", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(
                "Agrega el enlace de una lista M3U que tengas derecho a ver.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Nombre (opcional)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = url, onValueChange = { url = it },
                label = { Text("Enlace de la lista (http/https)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.add(name, url); name = ""; url = "" },
                enabled = validUrl,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Agregar lista") }
            Spacer(Modifier.height(18.dp))
            XtreamForm { name, url -> vm.add(name, url) }
        }
        if (vm.lists.isEmpty()) {
            item {
                Text(
                    "Todavía no has agregado listas.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
        items(vm.lists, key = { it.url }) { l ->
            Card(
                onClick = { vm.openList(l) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(l.name, fontWeight = FontWeight.SemiBold)
                        Text(displayUrl(l.url), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { vm.remove(l) }) { Icon(Icons.Filled.Delete, "Quitar lista") }
                }
            }
        }
    }
}

@Composable
private fun ChannelList(list: M3uList, state: Load<List<M3uChannel>>?, onBack: () -> Unit) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf("Todos") }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Volver") }
            Text(list.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        when (state) {
            null, is Load.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is Load.Error -> Text(state.message, modifier = Modifier.padding(16.dp))
            is Load.Ok -> {
                val all = state.value
                val groups = listOf("Todos") + all.map { it.group }.distinct().sorted()
                val shown = all.filter {
                    (group == "Todos" || it.group == group) &&
                        (query.isBlank() || it.name.contains(query.trim(), ignoreCase = true))
                }
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("Buscar canal") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(groups) { g -> FilterChip(selected = group == g, onClick = { group = g }, label = { Text(g) }) }
                }
                Text(
                    "${shown.size} canales",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(shown) { ch ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    context.startActivity(
                                        Intent(context, PlayerActivity::class.java)
                                            .putExtra(PlayerActivity.EXTRA_URL, ch.url)
                                            .putExtra(PlayerActivity.EXTRA_TITLE, ch.name)
                                            .putExtra(PlayerActivity.EXTRA_USER_AGENT, ch.userAgent)
                                            .putExtra(PlayerActivity.EXTRA_REFERRER, ch.referrer),
                                    )
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = ch.logo.ifBlank { null },
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(ch.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(ch.group, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Cuenta de proveedor tipo "Xtream Codes" (la que usan apps como IPTV Smarters). */
@Composable
private fun XtreamForm(onAdd: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var server by rememberSaveable { mutableStateOf("") }
    var user by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    val ok = (server.trim().startsWith("http://") || server.trim().startsWith("https://")) &&
        user.isNotBlank() && pass.isNotBlank()

    Text("Cuenta de proveedor (tipo Smarters / Xtream)", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    Text(
        "Usa los datos que te dio tu proveedor de TV contratado.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 14.sp,
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = name, onValueChange = { name = it },
        label = { Text("Nombre (opcional)") }, singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = server, onValueChange = { server = it },
        label = { Text("Servidor (http://...)") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = user, onValueChange = { user = it },
        label = { Text("Usuario") }, singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = pass, onValueChange = { pass = it },
        label = { Text("Contraseña") }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = {
            onAdd(name.ifBlank { user.trim() }, xtreamUrl(server, user, pass))
            name = ""; server = ""; user = ""; pass = ""
        },
        enabled = ok,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Agregar cuenta") }
}

private fun xtreamUrl(server: String, user: String, pass: String): String {
    fun enc(s: String) = URLEncoder.encode(s.trim(), "UTF-8")
    return server.trim().trimEnd('/') + "/get.php?username=" + enc(user) +
        "&password=" + enc(pass) + "&type=m3u_plus&output=m3u8"
}

/** No muestra la contraseña de las cuentas en pantalla. */
private fun displayUrl(url: String): String =
    url.replace(Regex("password=[^&]*"), "password=••••")
