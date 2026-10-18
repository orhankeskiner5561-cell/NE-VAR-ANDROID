package com.nevar.discovery

import android.os.Bundle
import java.util.Locale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File
import java.io.FileOutputStream
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
private val photoKey = stringPreferencesKey("photo_paths")
private data class Discovery(val id: String, val title: String, val category: String, val city: String, val description: String, val demo: Boolean = true, val photoPath: String? = null)
private val examples = listOf(
    Discovery("demo1", "Sahil yürüyüşü", "Doğa", "Samsun", "Deniz kenarında keyifli bir yürüyüş fikri."),
    Discovery("demo2", "Seramik atölyesi", "Sanat", "İstanbul", "Yeni bir hobi keşfet."),
    Discovery("demo3", "Kahve keşfi", "Yeme İçme", "Ankara", "Farklı kahve lezzetlerini tanı."),
    Discovery("demo4", "Bisiklet turu", "Spor", "İzmir", "Şehirde hareketli bir gün geçir."),
    Discovery("demo5", "Teknoloji buluşması", "Teknoloji", "Bursa", "Yeni fikirleri keşfet.")
)
private val provinces = """Adana|Adıyaman|Afyonkarahisar|Ağrı|Amasya|Ankara|Antalya|Artvin|Aydın|Balıkesir|Bilecik|Bingöl|Bitlis|Bolu|Burdur|Bursa|Çanakkale|Çankırı|Çorum|Denizli|Diyarbakır|Edirne|Elazığ|Erzincan|Erzurum|Eskişehir|Gaziantep|Giresun|Gümüşhane|Hakkari|Hatay|Isparta|Mersin|İstanbul|İzmir|Kars|Kastamonu|Kayseri|Kırklareli|Kırşehir|Kocaeli|Konya|Kütahya|Malatya|Manisa|Kahramanmaraş|Mardin|Muğla|Muş|Nevşehir|Niğde|Ordu|Rize|Sakarya|Samsun|Siirt|Sinop|Sivas|Tekirdağ|Tokat|Trabzon|Tunceli|Şanlıurfa|Uşak|Van|Yozgat|Zonguldak|Aksaray|Bayburt|Karaman|Kırıkkale|Batman|Şırnak|Bartın|Ardahan|Iğdır|Yalova|Karabük|Kilis|Osmaniye|Düzce""".split("|").sortedWith(compareBy(java.text.Collator.getInstance(Locale("tr", "TR"))) { it })
private fun normalizedTr(value: String) = java.text.Normalizer.normalize(value.lowercase(Locale("tr", "TR")), java.text.Normalizer.Form.NFD).replace(Regex("[\\u0300-\\u036f]"), "").replace("ı", "i")
private fun canonicalCity(value: String): String = provinces.firstOrNull { normalizedTr(it) == normalizedTr(value.trim()) } ?: value.trim()
private val categories = listOf("Tümü", "Doğa", "Sanat", "Yeme İçme", "Spor", "Teknoloji", "Topluluk")
private fun encodePosts(items: List<Discovery>): String = JSONArray().apply {
    items.forEach { put(JSONObject().apply {
        put("id", it.id); put("title", it.title); put("category", it.category)
        put("city", it.city); put("description", it.description); put("photoPath", it.photoPath ?: "")
    }) }
}.toString()
private fun decodePosts(raw: String): List<Discovery> {
    // Parse records independently: one damaged post must not hide every other saved post.
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val entry = array.optJSONObject(index) ?: continue
                val id = entry.optString("id").trim()
                val title = entry.optString("title").trim()
                if (id.isBlank() || title.isBlank()) continue
                add(Discovery(
                    id = id,
                    title = title,
                    category = entry.optString("category", "Doğa"),
                    city = canonicalCity(entry.optString("city")),
                    description = entry.optString("description"),
                    demo = false,
                    photoPath = entry.optString("photoPath").takeIf { it.isNotBlank() }
                ))
            }
        }
    }.getOrDefault(emptyList())
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NeVarApp(this) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NeVarApp(activity: MainActivity) {
    val scope = rememberCoroutineScope()
    val prefs by activity.store.data.collectAsState(initial = emptyPreferences())
    val posts = remember(prefs[postsKey]) { decodePosts(prefs[postsKey] ?: "[]") }
    val saved = remember(prefs[savedKey]) { (prefs[savedKey] ?: "").split("|").filter { it.isNotBlank() }.toSet() }
    val dark = prefs[darkKey] ?: false
    val city = (prefs[cityKey] ?: "Tüm şehirler").let { if (it == "Tüm şehirler") it else canonicalCity(it) }
    var tab by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<Discovery?>(null) }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tümü") }
    var draftTitle by remember { mutableStateOf("") }
    var draftDescription by remember { mutableStateOf("") }
    var draftCity by remember { mutableStateOf("") }
    var draftCategory by remember { mutableStateOf("Doğa") }
    var draftPhoto by remember { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val folder = File(activity.filesDir, "photos").apply { mkdirs() }
                val dest = File(folder, java.util.UUID.randomUUID().toString() + ".jpg")
                activity.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                } ?: error("Photo cannot be opened")
                draftPhoto = dest.absolutePath
            }
        }
    }
    var showCities by remember { mutableStateOf(false) }
    var cityQuery by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val all = posts + examples
    val localDiscoveries = posts
    val view = LocalView.current
    DisposableEffect(dark, view) {
        val window = activity.window
        @Suppress("DEPRECATION")
        window.statusBarColor = navy.toArgb()
        @Suppress("DEPRECATION")
        window.navigationBarColor = (if (dark) Color(0xFF12101C) else Color.White).toArgb()
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = if (dark) 0 else android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        // The top app bar is always navy: light status icons are required in both themes.
        androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
        onDispose { }
    }
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
                    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("NE VAR?", color = Color.White, fontWeight = FontWeight.Black, fontSize = 25.sp)
                            Text("Bak. Keşfet. Katıl. Paylaş.", color = Color(0xFFDFD6FA), fontSize = 11.sp)
                        }
                        Text("BETA " + BuildConfig.VERSION_NAME, color = Color.White, fontSize = 11.sp)
                    }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, windowInsets = NavigationBarDefaults.windowInsets) {
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
                            if (item.photoPath != null) DiscoveryPhoto(item.photoPath, Modifier.fillMaxWidth().height(210.dp))
                            else Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)) { Text(item.category + " • Fotoğraf eklenmemiş", modifier = Modifier.fillMaxWidth().padding(16.dp), color = MaterialTheme.colorScheme.onPrimaryContainer) }
                            Text(item.title, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                            Text(canonicalCity(item.city) + " • " + item.category, color = MaterialTheme.colorScheme.primary)
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
                        OutlinedTextField(search, { search = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Mekân, etkinlik veya deneyim ara") }, singleLine = true, leadingIcon = { Icon(Icons.Filled.Search, null) })
                        OutlinedButton(onClick = { cityQuery = ""; keyboard?.hide(); showCities = true }) { Text("📍 " + city + " ▾") }
                        if (showCities) {
                            ModalBottomSheet(onDismissRequest = { showCities = false }) {
                                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp)) {
                                    Text("Şehir seç", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(cityQuery, { cityQuery = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("81 il arasında ara") })
                                    Spacer(Modifier.height(8.dp))
                                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 430.dp)) {
                                        items(listOf("Tüm şehirler") + provinces.filter { normalizedTr(it).contains(normalizedTr(cityQuery)) }) { name ->
                                            ListItem(headlineContent = { Text(name) }, modifier = Modifier.fillMaxWidth().clickable {
                                                showCities = false
                                                keyboard?.hide()
                                                search = ""
                                                scope.launch { activity.store.edit { it[cityKey] = name } }
                                            })
                                        }
                                    }
                                }
                            }
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            categories.forEach { name -> FilterChip(selected = category == name, onClick = { category = name }, label = { Text(name) }) }
                        }
                        val filtered = localDiscoveries.filter {
                            (category == "Tümü" || it.category == category) &&
                            (city == "Tüm şehirler" || normalizedTr(it.city) == normalizedTr(city)) &&
                            (search.isBlank() || normalizedTr(it.title + it.description + it.category).contains(normalizedTr(search)))
                        }
                        Text("${filtered.size} yerel keşif • Henüz çevrimiçi paylaşım yok", color = Color.Gray, fontSize = 12.sp)
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
                            items(filtered, key = { it.id }) { item -> DiscoveryCard(item, saved.contains(item.id), onOpen = { selected = item }, onSave = {
                                val updated = if (saved.contains(item.id)) saved - item.id else saved + item.id
                                scope.launch { activity.store.edit { it[savedKey] = updated.joinToString("|") } }
                            }) }
                            if (filtered.isEmpty()) item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(if (city != "Tüm şehirler" && search.isBlank()) "$city\u0027da henüz keşif bulunamadı." else "Aramana uygun keşif bulunamadı.", fontWeight = FontWeight.Bold)
                                    Text("Bu şehirde ilk keşfi sen paylaşabilirsin.")
                                    Button(onClick = { draftCity = if (city == "Tüm şehirler") "" else city; keyboard?.hide(); tab = 2 }) { Text("Keşif Paylaş") }
                                }
                            }
                        }
                    }
                    2 -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
                            item { Spacer(Modifier.height(8.dp)); Text("Keşfini paylaş", fontSize = 25.sp, fontWeight = FontWeight.Bold) }
                            item { Text("Paylaşımların bu cihazda saklanır. Henüz çevrimiçi yayınlanmaz.", color = Color.Gray) }
                            item { OutlinedTextField(draftTitle, { draftTitle = it }, label = { Text("Başlık") }, modifier = Modifier.fillMaxWidth()) }
                            item { OutlinedTextField(draftDescription, { draftDescription = it }, label = { Text("Açıklama") }, modifier = Modifier.fillMaxWidth(), minLines = 3) }
                            item { OutlinedTextField(draftCity, { draftCity = it }, label = { Text("Şehir") }, modifier = Modifier.fillMaxWidth()) }
                            item { OutlinedButton(onClick = { photoPicker.launch("image/*") }) { Text(if (draftPhoto == null) "Fotoğraf seç" else "Fotoğraf seçildi ✓") } }
                            if (draftPhoto != null) item { DiscoveryPhoto(draftPhoto, Modifier.fillMaxWidth().height(180.dp)) }
                            item {
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    categories.drop(1).forEach { name -> FilterChip(selected = draftCategory == name, onClick = { draftCategory = name }, label = { Text(name) }) }
                                }
                            }
                            item {
                                Button(onClick = {
                                    val item = Discovery(java.util.UUID.randomUUID().toString(), draftTitle.trim(), draftCategory, canonicalCity(draftCity), draftDescription.trim(), false, draftPhoto)
                                    scope.launch { activity.store.edit { prefs ->
                                        prefs[postsKey] = encodePosts(listOf(item) + posts)
                                        // Photo path is stored atomically with the post JSON.
                                    } }
                                    draftTitle = ""; draftDescription = ""; draftCity = ""; draftPhoto = null; tab = 0
                                }, enabled = draftTitle.isNotBlank() && draftCity.isNotBlank()) { Text("Cihaza kaydet") }
                            }
                        }
                    }
                    3 -> {
                        Spacer(Modifier.height(12.dp))
                        Text("Harita ve şehirler", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Text("Harita servisi henüz bağlı değil. Paylaşımların şehirlerini haritada arayabilir, yol tarifi uygulamasını açabilirsin.", color = Color.Gray, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = { cityQuery = ""; showCities = true }) { Text("📍 " + city + " ▾") }
                        if (showCities) {
                            ModalBottomSheet(onDismissRequest = { showCities = false }) {
                                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp)) {
                                    Text("Şehir seç", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(cityQuery, { cityQuery = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("81 il arasında ara") })
                                    LazyColumn(Modifier.heightIn(max = 420.dp)) {
                                        items(listOf("Tüm şehirler") + provinces.filter { normalizedTr(it).contains(normalizedTr(cityQuery)) }) { name ->
                                            ListItem(headlineContent = { Text(name) }, modifier = Modifier.fillMaxWidth().clickable {
                                                showCities = false
                                                scope.launch { activity.store.edit { it[cityKey] = name } }
                                            })
                                        }
                                    }
                                }
                            }
                        }
                        val mapPosts = localDiscoveries.filter { city == "Tüm şehirler" || normalizedTr(it.city) == normalizedTr(city) }
                        Text("${mapPosts.size} yerel paylaşım", color = Color.Gray, fontSize = 12.sp)
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (mapPosts.isEmpty()) item {
                                Text("Bu şehirde henüz konumlu paylaşım yok. İlk keşfi sen ekleyebilirsin.", modifier = Modifier.padding(vertical = 16.dp))
                            }
                            items(mapPosts, key = { "map_" + it.id }) { discovery ->
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(discovery.title, fontWeight = FontWeight.Bold)
                                        Text(discovery.city + " • Kesin adres eklenmemiş", color = Color.Gray, fontSize = 12.sp)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TextButton(onClick = { selected = discovery }) { Text("Detay") }
                                            TextButton(onClick = {
                                                val query = Uri.encode(discovery.city + ", Türkiye")
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + query))
                                                runCatching { activity.startActivity(intent) }
                                            }) { Text("Şehri haritada aç ↗") }
                                        }
                                    }
                                }
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
                            items(favourites, key = { "favorite_" + it.id }) { item -> DiscoveryCard(item, true, onOpen = { selected = item }, onSave = {
                                scope.launch { activity.store.edit { it[savedKey] = (saved - item.id).joinToString("|") } }
                            }) }
                            item { Text("Paylaşımlarım (${posts.size})", fontSize = 21.sp, fontWeight = FontWeight.Bold) }
                            items(posts, key = { "my_post_" + it.id }) { item ->
                                ListItem(headlineContent = { Text(item.title) }, supportingContent = { Text(item.city) }, leadingContent = { DiscoveryPhoto(item.photoPath, Modifier.size(60.dp)) }, modifier = Modifier.clickable { selected = item })
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
            if (item.photoPath != null) DiscoveryPhoto(item.photoPath, Modifier.fillMaxWidth().height(160.dp))
            else Box(Modifier.fillMaxWidth().height(58.dp).background(Brush.horizontalGradient(listOf(purple, pink, peach))), contentAlignment = Alignment.CenterStart) {
                Column(Modifier.padding(18.dp)) {
                    Text(item.category.uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(if (item.demo) "DEMO KEŞİF" else "YEREL PAYLAŞIM", color = Color.White, fontSize = 10.sp)
                }
            }
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(canonicalCity(item.city), color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
private fun DiscoveryPhoto(path: String?, modifier: Modifier = Modifier) {
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) {
        bitmap = withContext(Dispatchers.IO) {
            path?.let { filePath ->
                runCatching {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(filePath, bounds)
                    var sample = 1
                    while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 1200) sample *= 2
                    val options = BitmapFactory.Options().apply { inSampleSize = sample }
                    BitmapFactory.decodeFile(filePath, options)?.asImageBitmap()
                }.getOrNull()
            }
        }
    }
    if (bitmap != null) {
        Image(bitmap = bitmap!!, contentDescription = "Keşif fotoğrafı", modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Image, contentDescription = "Fotoğraf yok", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
