package com.nevar.discovery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val ComponentActivity.store by preferencesDataStore(name = "nevar_preferences")
private val purple = Color(0xFF7652EB)
private val navy = Color(0xFF17132A)
private val peach = Color(0xFFFFA268)
private val pink = Color(0xFFEF6AA7)
private val postsKey = stringPreferencesKey("posts")
private val savedKey = stringPreferencesKey("saved")
private val darkKey = booleanPreferencesKey("dark")
private val cityKey = stringPreferencesKey("city")
private data class Discovery(val id: String, val title: String, val category: String, val city: String, val description: String, val demo: Boolean = true)
private val examples = listOf(
    Discovery("demo1", "Sahil yürüyüşü", "Doğa", "Samsun", "Deniz kenarında keyifli bir yürüyüş fikri."),
    Discovery("demo2", "Seramik atölyesi", "Sanat", "İstanbul", "Yeni bir hobi keşfet."),
    Discovery("demo3", "Kahve keşfi", "Yeme İçme", "Ankara", "Farklı kahve lezzetlerini tanı."),
    Discovery("demo4", "Bisiklet turu", "Spor", "İzmir", "Şehirde hareketli bir gün geçir."),
    Discovery("demo5", "Teknoloji buluşması", "Teknoloji", "Bursa", "Yeni fikirleri keşfet.")
)
private val categories = listOf("Tümü", "Doğa", "Sanat", "Yeme İçme", "Spor", "Teknoloji", "Topluluk")
private fun encodePosts(items: List<Discovery>): String = JSONArray().apply {
    items.forEach { put(JSONObject().apply {
        put("id", it.id); put("title", it.title); put("category", it.category)
        put("city", it.city); put("description", it.description)
    }) }
}.toString()
private fun decodePosts(raw: String): List<Discovery> = runCatching {
    val arr = JSONArray(raw)
    (0 until arr.length()).map { i ->
        val j = arr.getJSONObject(i)
        Discovery(j.getString("id"), j.getString("title"), j.getString("category"), j.getString("city"), j.getString("description"), false)
    }
}.getOrDefault(emptyList())

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NeVarApp(this) }
    }
}

@Composable
private fun NeVarApp(activity: MainActivity) {
    val scope = rememberCoroutineScope()
    val prefs by activity.store.data.collectAsState(initial = emptyPreferences())
    val posts = remember(prefs[postsKey]) { decodePosts(prefs[postsKey] ?: "[]") }
    val saved = remember(prefs[savedKey]) { (prefs[savedKey] ?: "").split("|").filter { it.isNotBlank() }.toSet() }
    val dark = prefs[darkKey] ?: false
    val city = prefs[cityKey] ?: "Tüm şehirler"
    var tab by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<Discovery?>(null) }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tümü") }
    var draftTitle by remember { mutableStateOf("") }
    var draftDescription by remember { mutableStateOf("") }
    var draftCity by remember { mutableStateOf("") }
    var draftCategory by remember { mutableStateOf("Doğa") }
    var showCities by remember { mutableStateOf(false) }
    val all = posts + examples
    val colors = if (dark) darkColorScheme(primary = Color(0xFFB9A5FF), background = Color(0xFF12101C), surface = Color(0xFF242036)) else lightColorScheme(primary = purple, background = Color(0xFFF8F7FC), surface = Color.White)
    BackHandler(selected != null || tab != 0) {
        if (selected != null) selected = null else tab = 0
    }
    MaterialTheme(colorScheme = colors) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                Surface(color = navy) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("NE VAR?", color = Color.White, fontWeight = FontWeight.Black, fontSize = 25.sp)
                            Text("Bak. Keşfet. Katıl. Paylaş.", color = Color(0xFFDFD6FA), fontSize = 11.sp)
                        }
                        Text("BETA 0.2", color = Color.White, fontSize = 11.sp)
                    }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    val labels = listOf("Ana Sayfa", "Keşfet", "Paylaş", "Harita", "Profil")
                    val icons = listOf(Icons.Filled.Home, Icons.Filled.Explore, Icons.Filled.AddCircle, Icons.Filled.Place, Icons.Filled.Person)
                    labels.forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = tab == index && selected == null,
                            onClick = { tab = index; selected = null },
                            icon = { Icon(icons[index], contentDescription = label, tint = if (index == 2) pink else if (tab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                            label = { Text(label, maxLines = 1, fontSize = 10.sp) }
                        )
                    }
                }
            }
        ) { insets ->
            Column(Modifier.fillMaxSize().padding(insets).imePadding().padding(horizontal = 16.dp)) {
                if (selected != null) {
                    val item = selected!!
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Spacer(Modifier.height(12.dp))
                            Text(item.title, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                            Text(item.city + " • " + item.category, color = MaterialTheme.colorScheme.primary)
                            Text(if (item.demo) "DEMO KEŞİF" else "YEREL PAYLAŞIM", color = Color.Gray, fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(item.description)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = {
                                val updated = if (saved.contains(item.id)) saved - item.id else saved + item.id
                                scope.launch { activity.store.edit { it[savedKey] = updated.joinToString("|") } }
                            }) { Text(if (saved.contains(item.id)) "Kaydedildi ✓" else "♡ Kaydet") }
                            TextButton(onClick = { selected = null }) { Text("← Geri dön") }
                        }
                    }
                } else when (tab) {
                    0 -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
                            item {
                                Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(purple, pink, peach)), RoundedCornerShape(24.dp)).padding(22.dp)) {
                                    Column {
                                        Text("Bugün ne var?", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                                        Text("Yeni yerler, yeni deneyimler.", color = Color.White)
                                        Spacer(Modifier.height(12.dp))
                                        Button(onClick = { tab = 1 }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = purple)) { Text("Şimdi keşfet →") }
                                    }
                                }
                            }
                            item { Text("Sana özel keşif fikirleri", fontSize = 21.sp, fontWeight = FontWeight.Bold) }
                            item { Text("Örnek öneriler gerçek etkinlik ilanı değildir.", color = Color.Gray, fontSize = 12.sp) }
                            items(all, key = { it.id }) { item -> DiscoveryCard(item, saved.contains(item.id), onOpen = { selected = item }, onSave = {
                                val updated = if (saved.contains(item.id)) saved - item.id else saved + item.id
                                scope.launch { activity.store.edit { it[savedKey] = updated.joinToString("|") } }
                            }) }
                            item {
                                OutlinedButton(onClick = { selected = all.randomOrNull() }) { Text("✦ Bana sürpriz yap") }
                            }
                        }
                    }
                    1 -> {
                        Spacer(Modifier.height(12.dp))
                        Text("Keşfet", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(search, { search = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Yer, şehir veya deneyim ara") }, singleLine = true, leadingIcon = { Icon(Icons.Filled.Search, null) })
                        Box {
                            OutlinedButton(onClick = { showCities = true }) { Text("📍 " + city + " ▾") }
                            DropdownMenu(expanded = showCities, onDismissRequest = { showCities = false }) {
                                listOf("Tüm şehirler", "Samsun", "İstanbul", "Ankara", "İzmir", "Bursa").forEach { name ->
                                    DropdownMenuItem(text = { Text(name) }, onClick = {
                                        showCities = false
                                        scope.launch { activity.store.edit { it[cityKey] = name } }
                                    })
                                }
                            }
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            categories.forEach { name -> FilterChip(selected = category == name, onClick = { category = name }, label = { Text(name) }) }
                        }
                        val filtered = all.filter {
                            (category == "Tümü" || it.category == category) &&
                            (city == "Tüm şehirler" || it.city == city) &&
                            (search.isBlank() || (it.title + it.city + it.description + it.category).contains(search, ignoreCase = true))
                        }
                        Text("${filtered.size} keşif • Örnek içerikler DEMO", color = Color.Gray, fontSize = 12.sp)
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
                            items(filtered, key = { it.id }) { item -> DiscoveryCard(item, saved.contains(item.id), onOpen = { selected = item }, onSave = {
                                val updated = if (saved.contains(item.id)) saved - item.id else saved + item.id
                                scope.launch { activity.store.edit { it[savedKey] = updated.joinToString("|") } }
                            }) }
                            if (filtered.isEmpty()) item { Text("Aramana uygun keşif bulunamadı. Filtreleri değiştirebilirsin.") }
                        }
                    }
                    2 -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
                            item { Text("Keşfini paylaş", fontSize = 25.sp, fontWeight = FontWeight.Bold) }
                            item { Text("Paylaşımların bu cihazda saklanır. Henüz çevrimiçi yayınlanmaz.", color = Color.Gray) }
                            item { OutlinedTextField(draftTitle, { draftTitle = it }, label = { Text("Başlık") }, modifier = Modifier.fillMaxWidth()) }
                            item { OutlinedTextField(draftDescription, { draftDescription = it }, label = { Text("Açıklama") }, modifier = Modifier.fillMaxWidth(), minLines = 3) }
                            item { OutlinedTextField(draftCity, { draftCity = it }, label = { Text("Şehir") }, modifier = Modifier.fillMaxWidth()) }
                            item {
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    categories.drop(1).forEach { name -> FilterChip(selected = draftCategory == name, onClick = { draftCategory = name }, label = { Text(name) }) }
                                }
                            }
                            item {
                                Button(onClick = {
                                    val item = Discovery(java.util.UUID.randomUUID().toString(), draftTitle.trim(), draftCategory, draftCity.trim(), draftDescription.trim(), false)
                                    scope.launch { activity.store.edit { it[postsKey] = encodePosts(listOf(item) + posts) } }
                                    draftTitle = ""; draftDescription = ""; draftCity = ""; tab = 0
                                }, enabled = draftTitle.isNotBlank() && draftCity.isNotBlank()) { Text("Cihaza kaydet") }
                            }
                        }
                    }
                    3 -> {
                        Spacer(Modifier.height(20.dp))
                        Text("Harita ve şehirler", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Text("Harita servisi henüz bağlı değil. Aşağıdaki keşifler örnek içeriktir.", color = Color.Gray)
                        LazyColumn {
                            items(all, key = { it.id }) { item ->
                                ListItem(headlineContent = { Text(item.title) }, supportingContent = { Text(item.city) }, leadingContent = { Icon(Icons.Filled.Place, null, tint = purple) }, modifier = Modifier.clickable { selected = item })
                                HorizontalDivider()
                            }
                        }
                    }
                    4 -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
                            item { Text("Profil", fontSize = 25.sp, fontWeight = FontWeight.Bold) }
                            item { Text("Misafir kullanıcı • Yerel demo", color = Color.Gray) }
                            item { Text("Kaydedilenler", fontSize = 21.sp, fontWeight = FontWeight.Bold) }
                            val favourites = all.filter { saved.contains(it.id) }
                            if (favourites.isEmpty()) item { Text("Henüz kaydedilen keşif yok.") }
                            items(favourites, key = { it.id }) { item -> DiscoveryCard(item, true, onOpen = { selected = item }, onSave = {
                                scope.launch { activity.store.edit { it[savedKey] = (saved - item.id).joinToString("|") } }
                            }) }
                            item { Text("Paylaşımlarım (${posts.size})", fontSize = 21.sp, fontWeight = FontWeight.Bold) }
                            items(posts, key = { it.id }) { item ->
                                ListItem(headlineContent = { Text(item.title) }, supportingContent = { Text(item.city) }, modifier = Modifier.clickable { selected = item })
                            }
                            item { Text("Görünüm", fontWeight = FontWeight.Bold) }
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Koyu tema", modifier = Modifier.weight(1f))
                                    Switch(checked = dark, onCheckedChange = { value -> scope.launch { activity.store.edit { it[darkKey] = value } } })
                                }
                            }
                            item { Text("Hesap ve çevrimiçi senkronizasyon henüz etkin değil.", color = Color.Gray, fontSize = 12.sp) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoveryCard(item: Discovery, isSaved: Boolean, onOpen: () -> Unit, onSave: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column {
            Box(Modifier.fillMaxWidth().height(92.dp).background(Brush.horizontalGradient(listOf(purple, pink, peach))), contentAlignment = Alignment.CenterStart) {
                Column(Modifier.padding(18.dp)) {
                    Text(item.category.uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(if (item.demo) "DEMO KEŞİF" else "YEREL PAYLAŞIM", color = Color.White, fontSize = 10.sp)
                }
            }
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(item.city, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onSave) { Icon(if (isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = "Kaydet", tint = if (isSaved) pink else MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                Text(item.description, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Text("Detayları gör →", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
