package com.shalil.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

data class Product(val id: Long, val name: String, val code: String, val stock: Int, val buy: Long, val price: Long)
data class SaleLine(val product: Product, val count: Int)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { ShalilApp() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShalilApp() {
    var page by remember { mutableStateOf("home") }
    var products by remember { mutableStateOf(listOf<Product>()) }
    var cart by remember { mutableStateOf(listOf<SaleLine>()) }
    var history by remember { mutableStateOf(listOf<Long>()) }
    var name by remember { mutableStateOf("") }; var code by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("1") }; var buy by remember { mutableStateOf("") }; var price by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }; var toman by remember { mutableStateOf(false) }; var dark by remember { mutableStateOf(false) }
    val total = cart.sumOf { it.product.price * it.count }
    fun money(value: Long) = NumberFormat.getNumberInstance(Locale("fa", "IR")).format(if (toman) value / 10 else value)
    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        Scaffold(topBar = { TopAppBar(title = { Text("نرم افزار شلیل", fontWeight = FontWeight.Bold) }, navigationIcon = { TextButton(onClick = { page = "menu" }) { Text("☰") } }) }) { pad ->
            Column(Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (page) {
                    "menu" -> { listOf("خانه" to "home", "مدیریت کالا" to "products", "فاکتور جدید" to "invoice", "فاکتورهای قبلی" to "history", "تنظیمات" to "settings").forEach { (label, target) -> Button(onClick = { page = target }, modifier = Modifier.fillMaxWidth()) { Text(label) } } }
                    "home" -> { Text("به نرم افزار فروشگاهی شلیل خوش آمدید"); Button(onClick = { cart = emptyList(); page = "invoice" }, modifier = Modifier.fillMaxWidth().height(64.dp)) { Text("فاکتور جدید") }; Button(onClick = { page = "products" }, modifier = Modifier.fillMaxWidth()) { Text("مدیریت کالاها") }; Button(onClick = { page = "history" }, modifier = Modifier.fillMaxWidth()) { Text("فاکتورهای قبلی") } }
                    "products" -> {
                        Text("افزودن کالا", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(name, { name = it }, label = { Text("نام کالا") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(code, { code = it }, label = { Text("بارکد") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(stock, { stock = it }, label = { Text("تعداد موجودی") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(buy, { buy = it }, label = { Text("قیمت خرید به ریال") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(price, { price = it }, label = { Text("قیمت فروش به ریال") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = { if (name.isNotBlank() && price.toLongOrNull() != null) { products = products + Product(System.currentTimeMillis(), name, code, stock.toIntOrNull() ?: 0, buy.toLongOrNull() ?: 0, price.toLong()); name = ""; code = ""; stock = "1"; buy = ""; price = "" } }, modifier = Modifier.fillMaxWidth()) { Text("ثبت کالا") }
                        LazyColumn { items(products, key = { it.id }) { p -> Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) { Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.Bold); Text("موجودی: ${p.stock}  |  فروش: ${money(p.price)}") }; TextButton(onClick = { products = products.filterNot { it.id == p.id } }) { Text("حذف") } } } } }
                    }
                    "invoice" -> {
                        Text("فاکتور فروش جدید", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(search, { search = it }, label = { Text("جستجوی نام یا بارکد کالا") }, modifier = Modifier.fillMaxWidth())
                        LazyColumn(Modifier.weight(1f)) { items(products.filter { search.isBlank() || it.name.contains(search, true) || it.code.contains(search) }) { p -> ListItem(headlineContent = { Text(p.name) }, supportingContent = { Text("قیمت: ${money(p.price)}") }, trailingContent = { Button(onClick = { val old = cart.find { it.product.id == p.id }; cart = if (old == null) cart + SaleLine(p, 1) else cart.map { if (it.product.id == p.id) it.copy(count = it.count + 1) else it } }) { Text("افزودن") } }) } }
                        Text("جمع کل: ${money(total)} ${if (toman) "تومان" else "ریال"}", style = MaterialTheme.typography.headlineSmall)
                        Button(onClick = { if (cart.isNotEmpty()) { history = history + total; products = products.map { p -> p.copy(stock = (p.stock - cart.filter { it.product.id == p.id }.sumOf { it.count }).coerceAtLeast(0)) }; cart = emptyList(); page = "home" } }, enabled = cart.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("ثبت و بستن فاکتور") }
                    }
                    "history" -> { Text("فاکتورهای قبلی", style = MaterialTheme.typography.titleLarge); if (history.isEmpty()) Text("هنوز فاکتوری ثبت نشده است"); history.forEachIndexed { index, amount -> ListItem(headlineContent = { Text("فاکتور ${index + 1}") }, supportingContent = { Text("مبلغ: ${money(amount)}") }) } }
                    "settings" -> { Text("تنظیمات", style = MaterialTheme.typography.titleLarge); Row { Text("حالت تاریک", Modifier.weight(1f)); Switch(dark, { dark = it }) }; Row { Text("نمایش تومان (خاموش = ریال)", Modifier.weight(1f)); Switch(toman, { toman = it }) }; Text("این نسخه اولیه است؛ اطلاعات هنوز به صورت دائمی ذخیره نمی‌شوند.") }
                }
            }
        }
    }
}
