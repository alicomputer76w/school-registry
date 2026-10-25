package com.southpunjab.registry
import android.content.*
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.util.Base64
import androidx.core.content.FileProvider
import org.json.*
import java.io.File

private fun out(c: Context, n: String) = File(c.cacheDir, n)

fun slipPdf(c: Context, s: Student, sch: String, addr: String, ph: String): File {
    val d = PdfDocument(); val pg = d.startPage(PdfDocument.PageInfo.Builder(420, 595, 1).create()); val cv = pg.canvas
    val p = Paint().apply { isAntiAlias = true }
    p.color = 0xFF1565C0.toInt(); cv.drawRect(0f, 0f, 420f, 90f, p)
    p.color = Color.WHITE; p.textSize = 18f; p.isFakeBoldText = true; cv.drawText(sch, 20f, 38f, p)
    p.textSize = 10f; p.isFakeBoldText = false; cv.drawText(addr, 20f, 58f, p); cv.drawText(ph, 20f, 74f, p)
    p.color = Color.BLACK; p.textSize = 14f; p.isFakeBoldText = true; cv.drawText("STUDENT REGISTRATION SLIP", 20f, 120f, p)
    if (s.photo.isNotEmpty() && File(s.photo).exists())
        BitmapFactory.decodeFile(s.photo)?.let { cv.drawBitmap(Bitmap.createScaledBitmap(it, 90, 110, true), 310f, 130f, null) }
    p.style = Paint.Style.STROKE; cv.drawRect(310f, 130f, 400f, 240f, p); p.style = Paint.Style.FILL
    val rows = listOf("Admission No" to s.admNo, "Name" to s.name, "Father's Name" to s.father, "Class" to s.cls,
        "Date of Birth" to s.dob, "Admission Date" to s.admDate, "Father's Contact" to s.phone)
    var y = 150f
    rows.forEach { (k, v) -> p.isFakeBoldText = true; p.textSize = 11f; cv.drawText("$k:", 20f, y, p)
        p.isFakeBoldText = false; cv.drawText(v, 120f, y, p); y += 26f }
    cv.drawLine(250f, 520f, 400f, 520f, p); cv.drawText("Principal's Signature", 265f, 536f, p)
    d.finishPage(pg); return File(out(c, "pdf").apply { mkdirs() }, "slip_${s.admNo}.pdf").also { f -> f.outputStream().use { d.writeTo(it) }; d.close() }
}

fun listPdf(c: Context, title: String, l: List<Student>, sch: String): File {
    val d = PdfDocument(); val p = Paint().apply { textSize = 10f }
    l.chunked(40).ifEmpty { listOf(emptyList()) }.forEachIndexed { i, chunk ->
        val pg = d.startPage(PdfDocument.PageInfo.Builder(595, 842, i + 1).create()); val cv = pg.canvas
        p.isFakeBoldText = true; p.textSize = 14f; cv.drawText("$sch - $title", 30f, 40f, p); p.textSize = 10f
        cv.drawText("Adm No        Name                        Father                     Class     Phone", 30f, 66f, p); p.isFakeBoldText = false
        chunk.forEachIndexed { r, s -> val y = 86f + r * 18f
            cv.drawText(s.admNo, 30f, y, p); cv.drawText(s.name.take(24), 110f, y, p); cv.drawText(s.father.take(22), 250f, y, p)
            cv.drawText(s.cls, 380f, y, p); cv.drawText(s.phone, 440f, y, p) }
        d.finishPage(pg) }
    return File(out(c, "pdf").apply { mkdirs() }, "list.pdf").also { f -> f.outputStream().use { d.writeTo(it) }; d.close() }
}

fun csvFile(c: Context, l: List<Student>): File {
    fun q(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
    val h = "AdmissionNo,Name,Father,DOB,Gender,Class,AdmissionDate,Phone,CNIC,Address,PreviousSchool,Remarks"
    return File(out(c, "pdf").apply { mkdirs() }, "students.csv").also { f ->
        f.writeText("\uFEFF" + h + "\n" + l.joinToString("\n") { s ->
            listOf(s.admNo, s.name, s.father, s.dob, s.gender, s.cls, s.admDate, s.phone, s.cnic, s.address, s.prevSchool, s.remarks).joinToString(",") { q(it) } }) }
}

fun openFile(c: Context, f: File, mime: String, share: Boolean) {
    val u = FileProvider.getUriForFile(c, "${c.packageName}.files", f)
    val i = if (share) Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, u).setType(mime)
            else Intent(Intent.ACTION_VIEW).setDataAndType(u, mime)
    c.startActivity(Intent.createChooser(i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Open / Print / Save"))
}

fun toJson(l: List<Student>) = JSONArray().apply { l.forEach { s -> put(JSONObject().apply {
    put("admNo", s.admNo); put("name", s.name); put("father", s.father); put("dob", s.dob); put("gender", s.gender)
    put("cls", s.cls); put("admDate", s.admDate); put("phone", s.phone); put("cnic", s.cnic); put("address", s.address)
    put("prev", s.prevSchool); put("remarks", s.remarks); put("demo", s.demo)
    put("photo", if (s.photo.isNotEmpty() && File(s.photo).exists()) Base64.encodeToString(File(s.photo).readBytes(), Base64.NO_WRAP) else "") }) } }.toString()

fun fromJson(c: Context, t: String): List<Student> = JSONArray(t).let { a -> (0 until a.length()).map { i -> a.getJSONObject(i).let { o ->
    val b = o.optString("photo")
    val path = if (b.isNotEmpty()) File(c.filesDir, "p_${System.nanoTime()}.jpg").also { it.writeBytes(Base64.decode(b, Base64.NO_WRAP)) }.path else ""
    Student(admNo = o.getString("admNo"), name = o.getString("name"), father = o.getString("father"), dob = o.getString("dob"),
        gender = o.getString("gender"), cls = o.getString("cls"), admDate = o.getString("admDate"), photo = path,
        phone = o.getString("phone"), cnic = o.optString("cnic"), address = o.getString("address"),
        prevSchool = o.optString("prev"), remarks = o.optString("remarks"), demo = o.optBoolean("demo")) } } }
