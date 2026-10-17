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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ink = Color(0xFF17132A)
private val violet = Color(0xFF7652EB)
private data class Discovery(val title: String, val category: String, val city: String, val description: String)
private val examples = listOf(
    Discovery("Sahil yürüyüşü", "Doğa", "Samsun", "Deniz kenarında keyifli bir yürüyüş fikri."),
    Discovery("Seramik atölyesi", "Sanat", "İstanbul", "Yeni bir hobi keşfet."),
    Discovery("Kahve keşfi", "Yeme İçme", "Ankara", "Farklı kahve lezzetlerini tanı."),
    Discovery("Bisiklet turu", "Spor", "İzmir", "Şehirde hareketli bir gün geçir."),
    Discovery("Teknoloji buluşması", "Teknoloji", "Bursa", "Yeni fikirleri keşfet.")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NeVarApp() }
    }
}

@Composable
private fun NeVarApp() {
    var page by remember { mutableStateOf("Ana Sayfa") }
    var selected by remember { mutableStateOf<Discovery?>(null) }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tümü") }
    var draft by remember { mutableStateOf("") }
    val posts = remember { mutableStateListOf<Discovery>() }
    val saved = remember { mutableStateListOf<String>() }
    val tabs = listOf("Ana Sayfa", "Keşfet", "+ Paylaş", "Harita", "Profil")
    BackHandler(selected != null || page !in tabs) {
        if (selected != null) selected = null else page = "Ana Sayfa"
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = violet, background = Color(0xFFF9F8FC), surface = Color.White)) {
        Scaffold(
            topBar = {
                Surface(color = ink) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("NE VAR?", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                            Text("Bak. Keşfet. Katıl. Paylaş.", color = Color(0xFFE0D8FF), fontSize = 11.sp)
                        }
                        Text("DEMO", color = Color.White, fontSize = 12.sp)
                    }
                }
            },
            bottomBar = {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = page == tab,
                            onClick = { selected = null; page = tab },
                            icon = { Text(if (tab == "+ Paylaş") "＋" else "●") },
                            label = { Text(tab, fontSize = 10.sp) }
                        )
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                if (selected != null) {
                    val item = selected!!
                    Spacer(Modifier.height(18.dp))
                    Text(item.title, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(item.city + " · " + item.category, color = violet)
                    Spacer(Modifier.height(16.dp))
                    Text(item.description)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { if (saved.contains(item.title)) saved.remove(item.title) else saved.add(item.title) }) {
                        Text(if (saved.contains(item.title)) "Kaydedildi ✓" else "Kaydet")
                    }
                    TextButton(onClick = { selected = null }) { Text("Geri dön") }
                } else when (page) {
                    "Ana Sayfa", "Keşfet" -> {
                        Spacer(Modifier.height(12.dp))
                        Text(if (page == "Ana Sayfa") "Bugün ne var?" else "Keşfet", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(search, { search = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Keşif ara") }, singleLine = true)
                        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Tümü", "Doğa", "Sanat", "Yeme İçme", "Spor", "Teknoloji").forEach { name ->
                                FilterChip(selected = category == name, onClick = { category = name }, label = { Text(name) })
                            }
                        }
                        val filtered = (posts + examples).filter {
                            (category == "Tümü" || it.category == category) &&
                            (search.isBlank() || (it.title + it.city + it.description).contains(search, ignoreCase = true))
                        }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            item { Text("Örnek içerikler · Gerçek etkinlik ilanı değildir", color = Color.Gray, fontSize = 12.sp) }
                            items(filtered) { item ->
                                Card(onClick = { selected = item }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                                    Column(Modifier.padding(20.dp)) {
                                        Text(item.category.uppercase(), color = violet, fontSize = 11.sp)
                                        Text(item.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                        Text(item.city, color = Color.Gray)
                                        Spacer(Modifier.height(6.dp))
                                        Text(item.description)
                                        Text("Detayları gör →", color = violet)
                                    }
                                }
                            }
                            if (filtered.isEmpty()) item { Text("Bu aramaya uygun içerik bulunamadı.") }
                        }
                    }
                    "+ Paylaş" -> {
                        Spacer(Modifier.height(20.dp))
                        Text("Keşfini paylaş", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Text("Bu demo paylaşımları yalnızca uygulama açıkken saklar.", color = Color.Gray)
                        OutlinedTextField(draft, { draft = it }, label = { Text("Keşfinin başlığı") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = {
                            posts.add(0, Discovery(draft.trim(), "Topluluk", "Demo", "Kullanıcının oluşturduğu örnek paylaşım."))
                            draft = ""
                            page = "Ana Sayfa"
                        }, enabled = draft.isNotBlank()) { Text("Demo paylaşım oluştur") }
                    }
                    "Harita" -> {
                        Spacer(Modifier.height(20.dp))
                        Text("Harita", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Text("Harita sağlayıcısı henüz bağlı değil. Şehir bazlı örnek keşifler:", color = Color.Gray)
                        examples.forEach { item -> TextButton(onClick = { selected = item }) { Text("📍 " + item.city + " — " + item.title) } }
                    }
                    "Profil" -> {
                        Spacer(Modifier.height(20.dp))
                        Text("Profil", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Text("Misafir kullanıcı · Demo modu")
                        Spacer(Modifier.height(12.dp))
                        Text("Kaydedilenler", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        if (saved.isEmpty()) Text("Henüz kaydedilen keşif yok.")
                        saved.forEach { title -> TextButton(onClick = { selected = (posts + examples).firstOrNull { it.title == title } }) { Text("♥ " + title) } }
                        Spacer(Modifier.height(12.dp))
                        Text("Gerçek hesap ve çevrimiçi senkronizasyon sonraki sürümde.", color = Color.Gray)
                    }
                }
            }
        }
    }
}
