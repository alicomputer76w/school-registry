package com.southpunjab.registry
import android.content.*
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.room.Room
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

val CLASSES = listOf("Play", "Nursery", "Prep", "1st", "2nd", "3rd") + (4..10).map { "${it}th" }
val BLUE = Color(0xFF1565C0)

class MainActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val db = Room.databaseBuilder(applicationContext, Db::class.java, "school.db").build()
        val prefs = getSharedPreferences("s", MODE_PRIVATE)
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = BLUE)) { App(db.dao(), prefs) } }
    }
}

@Composable fun Photo(path: String, size: Int = 56) {
    val bmp = remember(path) { if (path.isNotEmpty() && File(path).exists()) BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = 4 }) else null }
    Box(Modifier.size(size.dp).clip(CircleShape).background(Color(0xFFBBDEFB)), Alignment.Center) {
        if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else Text("👤") }
}
@Composable fun SCard(s: Student, click: () -> Unit) = Card(Modifier.fillMaxWidth().clickable(onClick = click)) {
    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Photo(s.photo); Column { Text(s.name, fontWeight = FontWeight.Bold); Text("${s.admNo} • Class ${s.cls} • ${s.father}", fontSize = 12.sp) } } }
@Composable fun Dd(label: String, opts: List<String>, v: String, on: (String) -> Unit) {
    var o by remember { mutableStateOf(false) }
    Box { OutlinedButton({ o = true }) { Text("$label: ${v.ifEmpty { "All" }}") }
        DropdownMenu(o, { o = false }) { opts.forEach { DropdownMenuItem({ Text(it.ifEmpty { "All" }) }, { on(it); o = false }) } } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun App(dao: StudentDao, prefs: SharedPreferences) {
    val ctx = LocalContext.current; val scope = rememberCoroutineScope()
    val st by dao.all().collectAsState(emptyList())
    var screen by remember { mutableStateOf("home") }; var sel by remember { mutableStateOf<Student?>(null) }
    val snack = remember { SnackbarHostState() }
    fun msg(m: String) { scope.launch { snack.showSnackbar(m) } }
    fun pr(k: String, d: String) = prefs.getString(k, d)!!
    val today = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
    LaunchedEffect(Unit) { if (!prefs.getBoolean("seeded", false)) {
        val n = listOf("Ali Raza", "Ayesha Khan", "Hamza Iqbal", "Fatima Noor", "Usman Ahmed", "Zainab Bibi")
        n.forEachIndexed { i, nm -> runCatching { dao.insert(Student(admNo = "SPS-D${i + 1}", name = nm, father = "Father of $nm", dob = "01/01/201${i}",
            gender = if (i % 2 == 0) "Male" else "Female", cls = CLASSES[i + 3], admDate = today, phone = "0300123456$i", address = "Chowk Azam, Layyah", demo = true)) } }
        prefs.edit().putBoolean("seeded", true).apply() } }
    BackHandler(screen != "home") { screen = if (screen == "form" && sel != null) "profile" else "home" }
    val bk = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u: Uri? -> u?.let {
        scope.launch(Dispatchers.IO) { runCatching { ctx.contentResolver.openOutputStream(it)!!.use { o -> o.write(toJson(st).toByteArray()) } }
            .onSuccess { msg("Backup saved (${st.size} students)") }.onFailure { msg("Backup failed: ${it.message}") } } } }
    val rs = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u: Uri? -> u?.let {
        scope.launch(Dispatchers.IO) { runCatching { val l = fromJson(ctx, ctx.contentResolver.openInputStream(it)!!.bufferedReader().readText())
            dao.upsertAll(l.map { s -> s.copy(id = 0) }); l.size }.onSuccess { n -> msg("Restored $n students") }.onFailure { e -> msg("Restore failed: ${e.message}") } } } }
    fun guard(f: () -> Unit) = runCatching(f).onFailure { msg("Error: ${it.message}") }

    Scaffold(snackbarHost = { SnackbarHost(snack) }, topBar = { TopAppBar(
        title = { Text(pr("school", "The South Punjab School"), color = Color.White, fontSize = 16.sp) },
        navigationIcon = { if (screen != "home") TextButton({ screen = "home" }) { Text("←", color = Color.White, fontSize = 20.sp) } },
        actions = { TextButton({ screen = "settings" }) { Text("⚙", color = Color.White, fontSize = 20.sp) } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = BLUE)) }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) { when (screen) {
            "home" -> Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(colors = CardDefaults.cardColors(BLUE), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp)) {
                    Text("Total Registered Students", color = Color.White); Text("${st.size}", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold) } }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("➕ Add" to "form", "👥 View" to "list", "🔍 Search" to "list", "📊 Reports" to "reports").forEach { (t, s) ->
                        Button({ if (s == "form") sel = null; screen = s }, Modifier.weight(1f), contentPadding = PaddingValues(2.dp)) { Text(t, fontSize = 11.sp, maxLines = 1) } } }
                Text("Class-wise count", fontWeight = FontWeight.Bold)
                CLASSES.chunked(3).forEach { r -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { r.forEach { c ->
                    Card(Modifier.weight(1f)) { Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(c, fontSize = 12.sp); Text("${st.count { it.cls == c }}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = BLUE) } } } } }
                Text("Recently registered", fontWeight = FontWeight.Bold)
                st.sortedByDescending { it.id }.take(5).forEach { s -> SCard(s) { sel = s; screen = "profile" } } }
            "list" -> {
                var q by remember { mutableStateOf("") }; var cf by remember { mutableStateOf("") }; var gf by remember { mutableStateOf("") }; var so by remember { mutableStateOf("Name") }
                val l = st.filter { (q.isBlank() || listOf(it.name, it.admNo, it.father, it.phone).any { f -> f.contains(q, true) }) &&
                    (cf.isEmpty() || it.cls == cf) && (gf.isEmpty() || it.gender == gf) }
                    .sortedWith(when (so) { "Admission No" -> compareBy { it.admNo }; "Admission Date" -> compareByDescending { it.id }; else -> compareBy { it.name.lowercase() } })
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), label = { Text("Search name, adm no, father, phone") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { Dd("Class", listOf("") + CLASSES, cf) { cf = it }; Dd("Gender", listOf("", "Male", "Female"), gf) { gf = it }
                        Dd("Sort", listOf("Name", "Admission No", "Admission Date"), so) { so = it } }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(l, key = { it.id }) { s -> SCard(s) { sel = s; screen = "profile" } } } } }
            "profile" -> sel?.let { s0 -> val s = st.find { it.id == s0.id } ?: s0; var del by remember { mutableStateOf(false) }
                Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.fillMaxWidth(), Alignment.Center) { Photo(s.photo, 120) }
                    listOf("Admission No" to s.admNo, "Name" to s.name, "Father" to s.father, "DOB" to s.dob, "Gender" to s.gender, "Class" to s.cls,
                        "Admission Date" to s.admDate, "Contact" to s.phone, "CNIC" to s.cnic, "Address" to s.address, "Previous School" to s.prevSchool, "Remarks" to s.remarks)
                        .forEach { (k, v) -> Row { Text("$k: ", fontWeight = FontWeight.Bold); Text(v.ifEmpty { "-" }) } }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button({ screen = "form" }) { Text("Edit") }
                        Button({ guard { openFile(ctx, slipPdf(ctx, s, pr("school", "The South Punjab School"), pr("addr", "Chowk Azam, Layyah"), pr("phone", "")), "application/pdf", false) } }) { Text("Slip PDF/Print") }
                        Button({ del = true }, colors = ButtonDefaults.buttonColors(Color(0xFFC62828))) { Text("Delete") } }
                    if (del) AlertDialog({ del = false }, title = { Text("Delete ${s.name}?") }, text = { Text("This cannot be undone.") },
                        confirmButton = { TextButton({ scope.launch { dao.delete(s); del = false; screen = "list"; msg("Deleted") } }) { Text("Delete") } },
                        dismissButton = { TextButton({ del = false }) { Text("Cancel") } }) } }
            "form" -> {
                val o = sel; var no = pr("prefix", "SPS-") + (st.size + 1).toString().padStart(4, '0'); var k = st.size + 1
                while (o == null && st.any { it.admNo == no }) { k++; no = pr("prefix", "SPS-") + k.toString().padStart(4, '0') }
                var name by remember { mutableStateOf(o?.name ?: "") }; var fa by remember { mutableStateOf(o?.father ?: "") }
                var dob by remember { mutableStateOf(o?.dob ?: "") }; var g by remember { mutableStateOf(o?.gender ?: "Male") }
                var cl by remember { mutableStateOf(o?.cls ?: CLASSES[0]) }; var ad by remember { mutableStateOf(o?.admDate ?: today) }
                var ph by remember { mutableStateOf(o?.phone ?: "") }; var cn by remember { mutableStateOf(o?.cnic ?: "") }
                var addr by remember { mutableStateOf(o?.address ?: "") }; var pv by remember { mutableStateOf(o?.prevSchool ?: "") }
                var rm by remember { mutableStateOf(o?.remarks ?: "") }; var photo by remember { mutableStateOf(o?.photo ?: "") }; var err by remember { mutableStateOf("") }
                fun saveBmp(b: android.graphics.Bitmap) { val f = File(ctx.filesDir, "p_${System.nanoTime()}.jpg"); f.outputStream().use { b.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, it) }; photo = f.path }
                val gal = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u: Uri? -> u?.let { runCatching {
                    ctx.contentResolver.openInputStream(it)!!.use { i -> BitmapFactory.decodeStream(i)?.let(::saveBmp) } } } }
                val cam = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { b -> b?.let(::saveBmp) }
                @Composable fun F(l: String, v: String, on: (String) -> Unit) = OutlinedTextField(v, on, Modifier.fillMaxWidth(), label = { Text(l) })
                Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Admission No: ${o?.admNo ?: no}", fontWeight = FontWeight.Bold, color = BLUE)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Photo(photo, 80)
                        OutlinedButton({ cam.launch(null) }) { Text("📷 Camera") }; OutlinedButton({ gal.launch("image/*") }) { Text("🖼 Gallery") } }
                    F("Full Name *", name) { name = it }; F("Father's Name *", fa) { fa = it }; F("Date of Birth * (dd/mm/yyyy)", dob) { dob = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Dd("Gender", listOf("Male", "Female"), g) { g = it }; Dd("Class", CLASSES, cl) { cl = it } }
                    F("Admission Date *", ad) { ad = it }; F("Father's Contact *", ph) { ph = it }; F("Father's CNIC (optional, 13 digits)", cn) { cn = it }
                    F("Home Address *", addr) { addr = it }; F("Previous School (optional)", pv) { pv = it }; F("Remarks (optional)", rm) { rm = it }
                    if (err.isNotEmpty()) Text(err, color = Color.Red)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button({
                            val dr = Regex("""\d{1,2}/\d{1,2}/\d{4}""")
                            err = when { name.isBlank() || fa.isBlank() || addr.isBlank() -> "Name, father's name and address are required"
                                !dr.matches(dob) || !dr.matches(ad) -> "Dates must be dd/mm/yyyy"
                                ph.filter { it.isDigit() }.length < 10 -> "Enter a valid contact number"
                                cn.isNotBlank() && cn.filter { it.isDigit() }.length != 13 -> "CNIC must have 13 digits"; else -> "" }
                            if (err.isEmpty()) scope.launch { val s = Student(id = o?.id ?: 0, admNo = o?.admNo ?: no, name = name.trim(), father = fa.trim(), dob = dob, gender = g,
                                cls = cl, admDate = ad, photo = photo, phone = ph.trim(), cnic = cn.trim(), address = addr.trim(), prevSchool = pv, remarks = rm, demo = o?.demo ?: false)
                                runCatching { if (o == null) dao.insert(s) else dao.update(s) }.onSuccess { msg(if (o == null) "Student registered" else "Student updated"); sel = null; screen = "list" }
                                    .onFailure { err = "Could not save (duplicate admission number?)" } } }) { Text(if (o == null) "Save" else "Update") }
                        OutlinedButton({ name = ""; fa = ""; dob = ""; ph = ""; cn = ""; addr = ""; pv = ""; rm = ""; photo = ""; err = "" }) { Text("Reset") }
                        OutlinedButton({ screen = if (o != null) "profile" else "home" }) { Text("Cancel") } } } }
            "reports" -> { var rc by remember { mutableStateOf(CLASSES[0]) }; val sch = pr("school", "The South Punjab School")
                Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Students per class", fontWeight = FontWeight.Bold); CLASSES.forEach { c -> Text("$c: ${st.count { it.cls == c }}") }
                    Dd("Class", CLASSES, rc) { rc = it }
                    Button({ guard { openFile(ctx, listPdf(ctx, "Class $rc", st.filter { it.cls == rc }.sortedBy { it.name }, sch), "application/pdf", false) } }) { Text("PDF list: Class $rc") }
                    Button({ guard { openFile(ctx, listPdf(ctx, "All Students", st.sortedBy { it.admNo }, sch), "application/pdf", false) } }) { Text("PDF list: all students") }
                    Button({ guard { openFile(ctx, csvFile(ctx, st), "text/csv", true) } }) { Text("Export CSV (Excel)") }
                    Text("Recently admitted", fontWeight = FontWeight.Bold); st.sortedByDescending { it.id }.take(10).forEach { SCard(it) { sel = it; screen = "profile" } } } }
            "settings" -> { var sn by remember { mutableStateOf(pr("school", "The South Punjab School")) }; var sa by remember { mutableStateOf(pr("addr", "Chowk Azam, Layyah")) }
                var sp by remember { mutableStateOf(pr("phone", "")) }; var px by remember { mutableStateOf(pr("prefix", "SPS-")) }
                Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(sn, { sn = it }, Modifier.fillMaxWidth(), label = { Text("School name") }); OutlinedTextField(sa, { sa = it }, Modifier.fillMaxWidth(), label = { Text("Address") })
                    OutlinedTextField(sp, { sp = it }, Modifier.fillMaxWidth(), label = { Text("Phone") }); OutlinedTextField(px, { px = it }, Modifier.fillMaxWidth(), label = { Text("Admission number prefix") })
                    Button({ prefs.edit().putString("school", sn).putString("addr", sa).putString("phone", sp).putString("prefix", px).apply(); msg("Settings saved") }) { Text("Save settings") }
                    Button({ bk.launch("students_backup.json") }) { Text("Backup") }; Button({ rs.launch(arrayOf("*/*")) }) { Text("Restore (adds to existing records)") }
                    OutlinedButton({ scope.launch { dao.clearDemo(); msg("Demo data removed") } }) { Text("Remove demo data") }
                    Text("School Registry v1.0 — offline student registration", fontSize = 12.sp) } }
        } } }
}
