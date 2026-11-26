package com.newspapermaker.studio

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

data class NewsElement(
    var type: String = "body",
    var text: String = "",
    var imageUri: String = "",
    var x: Float = 55f,
    var y: Float = 220f,
    var w: Float = 500f,
    var h: Float = 180f,
    var fontSize: Float = 24f
)

class MainActivity : Activity() {
    private val pages = mutableListOf<MutableList<NewsElement>>()
    private var pageIndex = 0
    private var selectedIndex = -1
    private var paperWidth = 1224f
    private var paperHeight = 1584f
    private var paperType = "NEWSPAPER"

    private var paperName = "ES NEWS"
    private var paperTagline = "সংবাদ • মতামত • স্থানীয় খবর"
    private var paperDate = ""
    private var mastheadBgColor = Color.WHITE
    private var mastheadGradient = false
    private var mastheadTitleColor = Color.rgb(25, 43, 67)
    private var mastheadTitleSize = 48f
    private var mastheadFont = "sans-serif-condensed"
    private var mastheadLeftLogoUri = ""
    private var mastheadRightLogoUri = ""
    private var mastheadBackgroundUri = ""
    private var pageBackgroundUri = ""

    private lateinit var editor: NewspaperView
    private lateinit var pageLabel: TextView
    private lateinit var sizeLabel: TextView
    private val navy = Color.rgb(25, 43, 67)
    private val blue = Color.rgb(25, 105, 170)
    private val bitmapCache = mutableMapOf<String, Bitmap>()

    private fun currentPage() = pages[pageIndex]
    private fun dateNow() = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        paperDate = dateNow()
        pages.add(mutableListOf())
        createInterface()
    }

    private fun createInterface() {
        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(235, 239, 244))
        }
        val header = TextView(this).apply {
            text = "NEWSPAPER MAKER STUDIO"
            textSize = 19f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(8, 14, 8, 14)
            setBackgroundColor(navy)
        }
        main.addView(header)

        val row1 = makeToolbar(main)
        tool(row1, "＋ শিরোনাম") { addText("headline") }
        tool(row1, "＋ বিস্তারিত") { addText("body") }
        tool(row1, "📷 ছবি যোগ") { openGallery(200) }
        tool(row1, "✎ সম্পাদনা") { editSelected() }
        tool(row1, "মুছুন") { deleteSelected() }

        val row2 = makeToolbar(main)
        tool(row2, "← আগের পৃষ্ঠা") { changePage(-1) }
        tool(row2, "＋ নতুন পৃষ্ঠা") { addNewPage() }
        tool(row2, "পরের পৃষ্ঠা →") { changePage(1) }
        tool(row2, "Paper Size") { changePaperSize() }
        tool(row2, "PDF Export") { exportPdfDialog() }

        val row3 = makeToolbar(main)
        tool(row3, "📰 পত্রিকার নাম") { changePaperName() }
        tool(row3, "🎨 নামের Background") { changeMastheadBackground() }
        tool(row3, "নামের ছবি") { openGallery(203) }
        tool(row3, "🖼 পুরো পৃষ্ঠার ছবি") { openGallery(204) }
        tool(row3, "পৃষ্ঠা Background রং") { changePageBackgroundColor() }

        val row4 = makeToolbar(main)
        tool(row4, "🅰 Font") { changeMastheadFont() }
        tool(row4, "নামের রং") { changeMastheadTitleColor() }
        tool(row4, "বাঁ পাশের লোগো") { openGallery(201) }
        tool(row4, "ডান পাশের লোগো") { openGallery(202) }
        tool(row4, "লোগো/Background মুছুন") { removeDesignImage() }

        val row5 = makeToolbar(main)
        tool(row5, "💾 Design Save") { saveProjectDialog() }
        tool(row5, "📂 Design Open") { openProjectDialog() }
        tool(row5, "＋ নতুন পত্রিকা") { newNewspaper() }

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(8, 6, 8, 6)
            setBackgroundColor(Color.WHITE)
        }
        pageLabel = TextView(this).apply { textSize = 13f; setTextColor(navy) }
        sizeLabel = TextView(this).apply { textSize = 13f; setTextColor(blue); gravity = Gravity.END }
        info.addView(pageLabel, LinearLayout.LayoutParams(0, -2, 1f))
        info.addView(sizeLabel, LinearLayout.LayoutParams(0, -2, 1f))
        main.addView(info)

        editor = NewspaperView()
        main.addView(editor, LinearLayout.LayoutParams(-1, 0, 1f))
        val help = TextView(this).apply {
            text = "লেখা/ছবি টাচ করে নির্বাচন করুন • টেনে সরান • লাল গোল অংশ টেনে ছোট-বড় করুন"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
            setPadding(6, 8, 6, 8)
            setBackgroundColor(Color.WHITE)
        }
        main.addView(help)
        setContentView(main)
        updateEditor()
    }

    private fun makeToolbar(main: LinearLayout): LinearLayout {
        val scroll = HorizontalScrollView(this)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(4, 3, 4, 3) }
        scroll.addView(row)
        main.addView(scroll)
        return row
    }

    private fun tool(parent: LinearLayout, text: String, action: () -> Unit) {
        val button = Button(this).apply {
            this.text = text
            textSize = 12f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setPadding(9, 2, 9, 2)
            background = GradientDrawable().apply { setColor(blue); cornerRadius = 12f }
            setOnClickListener { action() }
        }
        parent.addView(button, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 44).apply {
            setMargins(3, 2, 3, 2)
        })
    }

    private fun updateEditor() {
        pageLabel.text = "পৃষ্ঠা ${pageIndex + 1} / ${pages.size}"
        sizeLabel.text = paperType
        editor.invalidate()
    }

    private fun changePage(direction: Int) {
        val next = pageIndex + direction
        if (next in pages.indices) { pageIndex = next; selectedIndex = -1; updateEditor() }
        else toast("আর পৃষ্ঠা নেই")
    }

    private fun addNewPage() {
        pages.add(mutableListOf())
        pageIndex = pages.lastIndex
        selectedIndex = -1
        updateEditor()
        toast("নতুন পৃষ্ঠা তৈরি হয়েছে")
    }

    private fun newNewspaper() {
        AlertDialog.Builder(this).setTitle("নতুন পত্রিকা")
            .setMessage("বর্তমান ডিজাইন মুছে নতুন পত্রিকা শুরু করবেন?")
            .setPositiveButton("হ্যাঁ") { _, _ ->
                pages.clear(); pages.add(mutableListOf()); pageIndex = 0; selectedIndex = -1
                paperName = "ES NEWS"; paperTagline = "সংবাদ • মতামত • স্থানীয় খবর"; paperDate = dateNow()
                mastheadLeftLogoUri = ""; mastheadRightLogoUri = ""; mastheadBackgroundUri = ""; pageBackgroundUri = ""
                mastheadBgColor = Color.WHITE; mastheadGradient = false
                clearBitmapCache(); updateEditor()
            }.setNegativeButton("না", null).show()
    }

    private fun changePaperName() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 8, 20, 4) }
        val name = EditText(this).apply { hint = "পত্রিকার নাম"; setText(paperName); textSize = 17f }
        val tagline = EditText(this).apply { hint = "স্লোগান"; setText(paperTagline); textSize = 15f }
        val date = EditText(this).apply { hint = "প্রকাশের তারিখ"; setText(paperDate); textSize = 15f }
        val size = SeekBar(this).apply { max = 80; progress = (mastheadTitleSize - 20f).toInt().coerceIn(0, 80) }
        box.addView(name); box.addView(tagline); box.addView(date)
        box.addView(TextView(this).apply { text = "নামের সাইজ" }); box.addView(size)
        AlertDialog.Builder(this).setTitle("পত্রিকার নাম ও স্লোগান").setView(box)
            .setPositiveButton("সংরক্ষণ") { _, _ ->
                paperName = name.text.toString().ifBlank { "ES NEWS" }
                paperTagline = tagline.text.toString(); paperDate = date.text.toString().ifBlank { dateNow() }
                mastheadTitleSize = 20f + size.progress; updateEditor()
            }.setNegativeButton("বাতিল", null).show()
    }

    private fun changeMastheadBackground() {
        val names = arrayOf("সাদা", "হালকা নীল", "নীল", "লাল", "কালো", "কমলা", "সবুজ", "বেগুনি", "হলুদ", "Gradient চালু/বন্ধ")
        val colors = intArrayOf(Color.WHITE, 0xFFE3F2FD.toInt(), Color.BLUE, Color.RED, Color.BLACK,
            0xFFFF9800.toInt(), Color.GREEN, 0xFF800080.toInt(), Color.YELLOW)
        AlertDialog.Builder(this).setTitle("নামের Background Color")
            .setItems(names) { _, which ->
                if (which == names.lastIndex) mastheadGradient = !mastheadGradient else mastheadBgColor = colors[which]
                updateEditor()
            }.show()
    }

    private fun changePageBackgroundColor() {
        val names = arrayOf("সাদা", "হালকা ধূসর", "হালকা নীল", "হালকা হলুদ", "কালো")
        val colors = intArrayOf(Color.WHITE, 0xFFF2F2F2.toInt(), 0xFFE3F2FD.toInt(), 0xFFFFF9C4.toInt(), Color.BLACK)
        AlertDialog.Builder(this).setTitle("পৃষ্ঠা Background Color")
            .setItems(names) { _, which ->
                pageBackgroundUri = ""
                pageBackgroundColor = colors[which]
                updateEditor()
            }.show()
    }

    private var pageBackgroundColor = Color.WHITE

    private fun changeMastheadTitleColor() {
        val names = arrayOf("কালো", "সাদা", "নীল", "লাল", "কমলা", "সবুজ", "বেগুনি")
        val colors = intArrayOf(Color.BLACK, Color.WHITE, Color.BLUE, Color.RED, 0xFFFF9800.toInt(), Color.GREEN, 0xFF800080.toInt())
        AlertDialog.Builder(this).setTitle("পত্রিকার নামের রং").setItems(names) { _, which ->
            mastheadTitleColor = colors[which]; updateEditor()
        }.show()
    }

    private fun changeMastheadFont() {
        val fonts = arrayOf("Default Bold", "Serif Bold", "Sans Bold", "Condensed Bold", "Monospace")
        val values = arrayOf("sans-serif", "serif", "sans-serif", "sans-serif-condensed", "monospace")
        AlertDialog.Builder(this).setTitle("নামের Font").setItems(fonts) { _, which ->
            mastheadFont = values[which]; updateEditor()
        }.show()
    }

    private fun removeDesignImage() {
        val options = arrayOf("বাঁ পাশের লোগো", "ডান পাশের লোগো", "নামের Background ছবি", "পুরো পৃষ্ঠার Background ছবি", "সব ছবি")
        AlertDialog.Builder(this).setTitle("কোন ছবি মুছবেন?").setItems(options) { _, which ->
            when (which) {
                0 -> mastheadLeftLogoUri = ""
                1 -> mastheadRightLogoUri = ""
                2 -> mastheadBackgroundUri = ""
                3 -> pageBackgroundUri = ""
                else -> { mastheadLeftLogoUri = ""; mastheadRightLogoUri = ""; mastheadBackgroundUri = ""; pageBackgroundUri = "" }
            }
            updateEditor()
        }.show()
    }

    private fun changePaperSize() {
        val options = arrayOf("Newspaper Broadsheet (17 × 22 inch)", "A4 Paper")
        AlertDialog.Builder(this).setTitle("পেপারের সাইজ").setItems(options) { _, which ->
            if (which == 0) { paperWidth = 1224f; paperHeight = 1584f; paperType = "NEWSPAPER" }
            else { paperWidth = 595f; paperHeight = 842f; paperType = "A4" }
            updateEditor()
        }.show()
    }

    private fun addText(type: String) {
        val input = EditText(this).apply {
            hint = if (type == "headline") "সংবাদের শিরোনাম লিখুন" else "সংবাদের বিস্তারিত লিখুন"
            textSize = 17f; minLines = if (type == "body") 5 else 2; gravity = Gravity.TOP
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        AlertDialog.Builder(this).setTitle(if (type == "headline") "নতুন শিরোনাম" else "সংবাদের বিস্তারিত").setView(input)
            .setPositiveButton("যোগ করুন") { _, _ ->
                val value = input.text.toString()
                if (value.isNotBlank()) {
                    val headline = type == "headline"
                    currentPage().add(NewsElement(type, value, x = 55f, y = if (paperType == "A4") 125f else 195f,
                        w = paperWidth - 110f, h = if (headline) 180f else 360f, fontSize = if (headline) 38f else 23f))
                    selectedIndex = currentPage().lastIndex; updateEditor()
                }
            }.setNegativeButton("বাতিল", null).show()
    }

    private fun editSelected() {
        if (selectedIndex !in currentPage().indices) { toast("আগে একটি খবর বা ছবি নির্বাচন করুন"); return }
        val element = currentPage()[selectedIndex]
        if (element.type == "image") {
            AlertDialog.Builder(this).setTitle("ছবি").setItems(arrayOf("ছবি পরিবর্তন", "বাতিল")) { _, which -> if (which == 0) openGallery(200) }.show()
            return
        }
        val input = EditText(this).apply { setText(element.text); textSize = 17f; minLines = 5; gravity = Gravity.TOP
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE }
        AlertDialog.Builder(this).setTitle("খবর সম্পাদনা").setView(input)
            .setPositiveButton("সংরক্ষণ") { _, _ -> element.text = input.text.toString(); updateEditor() }
            .setNegativeButton("বাতিল", null).show()
    }

    private fun deleteSelected() {
        if (selectedIndex in currentPage().indices) { currentPage().removeAt(selectedIndex); selectedIndex = -1; updateEditor() }
        else toast("কোনো খবর বা ছবি নির্বাচন করা হয়নি")
    }

    private fun openGallery(requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivityForResult(intent, requestCode)
    }

    @Deprecated("Document picker")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) return
        val uri = data.data!!
        try {
            val flags = data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
            if (flags != 0) contentResolver.takePersistableUriPermission(uri, flags)
            when (requestCode) {
                200 -> {
                    val old = if (selectedIndex in currentPage().indices) currentPage()[selectedIndex] else null
                    if (old != null && old.type == "image") old.imageUri = uri.toString()
                    else {
                        currentPage().add(NewsElement(type = "image", imageUri = uri.toString(), x = 55f,
                            y = if (paperType == "A4") 125f else 195f, w = paperWidth * 0.55f, h = paperHeight * 0.20f))
                        selectedIndex = currentPage().lastIndex
                    }
                }
                201 -> mastheadLeftLogoUri = uri.toString()
                202 -> mastheadRightLogoUri = uri.toString()
                203 -> mastheadBackgroundUri = uri.toString()
                204 -> pageBackgroundUri = uri.toString()
                101 -> { savePdf(uri); return }
                301 -> { saveProject(uri); return }
                302 -> { loadProject(uri); return }
            }
            updateEditor()
        } catch (e: Exception) { toast("ফাইল খোলা যায়নি: ${e.message}") }
    }

    inner class NewspaperView : View(this@MainActivity) {
        private var scaleFactor = 1f
        private var offsetX = 0f
        private var offsetY = 0f
        private var moving = false
        private var resizing = false
        private var lastX = 0f
        private var lastY = 0f
        private val paperPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = blue; style = Paint.Style.STROKE; strokeWidth = 3f }
        private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(20, 120, 220); style = Paint.Style.STROKE; strokeWidth = 4f }
        private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(Color.rgb(220, 225, 232))
            scaleFactor = min(width / paperWidth, height / paperHeight).coerceAtLeast(0.01f)
            offsetX = (width - paperWidth * scaleFactor) / 2f
            offsetY = (height - paperHeight * scaleFactor) / 2f
            canvas.save(); canvas.translate(offsetX, offsetY); canvas.scale(scaleFactor, scaleFactor)
            paperPaint.color = pageBackgroundColor
            canvas.drawRect(0f, 0f, paperWidth, paperHeight, paperPaint)
            drawBackgroundImage(canvas, pageBackgroundUri, 0f, 0f, paperWidth, paperHeight)
            drawMastheadDesign(canvas, pageIndex + 1)
            currentPage().forEachIndexed { index, element ->
                drawElement(canvas, element)
                if (index == selectedIndex) drawSelection(canvas, element)
            }
            canvas.drawRect(1f, 1f, paperWidth - 1f, paperHeight - 1f, borderPaint)
            canvas.restore()
        }

        private fun drawElement(canvas: Canvas, element: NewsElement) {
            val rect = RectF(element.x, element.y, element.x + element.w, element.y + element.h)
            if (element.type == "image") {
                val bitmap = getCachedBitmap(element.imageUri)
                if (bitmap != null) canvas.drawBitmap(bitmap, null, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                else canvas.drawRect(rect, borderPaint)
            } else drawTextElement(canvas, element)
        }

        private fun drawTextElement(canvas: Canvas, element: NewsElement) {
            val rect = RectF(element.x, element.y, element.x + element.w, element.y + element.h)
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK; textSize = element.fontSize
                typeface = if (element.type == "headline") Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
            }
            val text = element.text.ifBlank { " " }
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, max(1, element.w.toInt()))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(true).setLineSpacing(2f, 1f).build()
            canvas.save(); canvas.clipRect(rect); canvas.translate(element.x, element.y); layout.draw(canvas); canvas.restore()
        }

        private fun drawSelection(canvas: Canvas, element: NewsElement) {
            val rect = RectF(element.x, element.y, element.x + element.w, element.y + element.h)
            canvas.drawRect(rect, selectedPaint); canvas.drawCircle(rect.right, rect.bottom, 17f, handlePaint)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val docX = (event.x - offsetX) / scaleFactor
            val docY = (event.y - offsetY) / scaleFactor
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    moving = false; resizing = false; lastX = docX; lastY = docY; selectedIndex = -1
                    for (i in currentPage().indices.reversed()) {
                        val e = currentPage()[i]; val right = e.x + e.w; val bottom = e.y + e.h
                        if (docX >= right - 30f && docX <= right + 30f && docY >= bottom - 30f && docY <= bottom + 30f) {
                            selectedIndex = i; resizing = true; break
                        }
                        if (docX >= e.x && docX <= right && docY >= e.y && docY <= bottom) {
                            selectedIndex = i; moving = true; break
                        }
                    }
                    invalidate(); return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (selectedIndex in currentPage().indices) {
                        val e = currentPage()[selectedIndex]; val dx = docX - lastX; val dy = docY - lastY
                        if (moving) { e.x = (e.x + dx).coerceIn(0f, paperWidth - e.w); e.y = (e.y + dy).coerceIn(0f, paperHeight - 40f) }
                        if (resizing) { e.w = (e.w + dx).coerceIn(80f, paperWidth - e.x); e.h = (e.h + dy).coerceIn(50f, paperHeight - e.y - 40f) }
                        lastX = docX; lastY = docY; invalidate()
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { moving = false; resizing = false; invalidate(); return true }
            }
            return true
        }
    }

    private fun drawMastheadDesign(canvas: Canvas, number: Int) {
        val a4 = paperType == "A4"
        val headerHeight = if (a4) 105f else 175f
        val rect = RectF(0f, 0f, paperWidth, headerHeight)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG)
        if (mastheadGradient) bg.shader = LinearGradient(0f, 0f, paperWidth, headerHeight, mastheadBgColor, Color.WHITE, Shader.TileMode.CLAMP)
        else bg.color = mastheadBgColor
        canvas.drawRect(rect, bg)
        drawBackgroundImage(canvas, mastheadBackgroundUri, 0f, 0f, paperWidth, headerHeight)

        val logoSize = if (a4) 42f else 100f
        val logoY = if (a4) 10f else 25f
        drawLogo(canvas, mastheadLeftLogoUri, 35f, logoY, logoSize)
        drawLogo(canvas, mastheadRightLogoUri, paperWidth - logoSize - 35f, logoY, logoSize)
        val centerWidth = paperWidth - (logoSize * 2f) - 110f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mastheadTitleColor; textAlign = Paint.Align.CENTER
            typeface = Typeface.create(mastheadFont, Typeface.BOLD); textSize = if (a4) mastheadTitleSize * 0.56f else mastheadTitleSize
        }
        val titleWidth = titlePaint.measureText(paperName)
        if (titleWidth > centerWidth && titleWidth > 0f) titlePaint.textScaleX = centerWidth / titleWidth
        canvas.drawText(paperName, paperWidth / 2f, if (a4) 48f else 75f, titlePaint)
        val meta = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textAlign = Paint.Align.CENTER; textSize = if (a4) 10f else 20f }
        canvas.drawText(paperDate, paperWidth / 2f, if (a4) 69f else 110f, meta)
        canvas.drawText(paperTagline, paperWidth / 2f, if (a4) 87f else 145f, meta)
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = blue; strokeWidth = if (a4) 3f else 7f }
        canvas.drawLine(35f, headerHeight, paperWidth - 35f, headerHeight, line)
        val footer = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GRAY; textSize = 15f; textAlign = Paint.Align.CENTER }
        canvas.drawText("পৃষ্ঠা $number", paperWidth / 2f, paperHeight - 25f, footer)
    }

    private fun drawBackgroundImage(canvas: Canvas, uri: String, left: Float, top: Float, right: Float, bottom: Float) {
        if (uri.isBlank()) return
        val bitmap = getCachedBitmap(uri) ?: return
        canvas.drawBitmap(bitmap, null, RectF(left, top, right, bottom), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }

    private fun drawLogo(canvas: Canvas, uri: String, x: Float, y: Float, size: Float) {
        if (uri.isBlank()) return
        val bitmap = getCachedBitmap(uri) ?: return
        canvas.drawBitmap(bitmap, null, RectF(x, y, x + size, y + size), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }

    private fun getCachedBitmap(uri: String): Bitmap? {
        if (uri.isBlank()) return null
        bitmapCache[uri]?.let { if (!it.isRecycled) return it }
        return try {
            val bitmap = contentResolver.openInputStream(Uri.parse(uri)).use { if (it == null) null else BitmapFactory.decodeStream(it) }
            if (bitmap != null) bitmapCache[uri] = bitmap
            bitmap
        } catch (_: Exception) { null }
    }

    private fun clearBitmapCache() { bitmapCache.values.forEach { if (!it.isRecycled) it.recycle() }; bitmapCache.clear() }

    private fun exportPdfDialog() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, "${paperName.replace(" ", "-")}-Newspaper.pdf")
        }
        startActivityForResult(intent, 101)
    }

    private fun savePdf(uri: Uri) {
        val pdf = PdfDocument()
        try {
            pages.forEachIndexed { index, elements ->
                val info = PdfDocument.PageInfo.Builder(paperWidth.toInt(), paperHeight.toInt(), index + 1).create()
                val page = pdf.startPage(info)
                page.canvas.drawColor(pageBackgroundColor)
                drawBackgroundImage(page.canvas, pageBackgroundUri, 0f, 0f, paperWidth, paperHeight)
                drawMastheadDesign(page.canvas, index + 1)
                elements.forEach { drawPdfElement(page.canvas, it) }
                pdf.finishPage(page)
            }
            contentResolver.openOutputStream(uri).use { output -> requireNotNull(output) { "PDF ফাইল খোলা যায়নি" }; pdf.writeTo(output) }
            toast("Newspaper PDF তৈরি হয়েছে")
        } catch (e: Exception) { toast("PDF Error: ${e.message}") } finally { pdf.close() }
    }

    private fun drawPdfElement(canvas: Canvas, element: NewsElement) {
        val rect = RectF(element.x, element.y, element.x + element.w, element.y + element.h)
        if (element.type == "image") {
            val bitmap = getCachedBitmap(element.imageUri)
            if (bitmap != null) canvas.drawBitmap(bitmap, null, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        } else {
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK; textSize = element.fontSize
                typeface = if (element.type == "headline") Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
            }
            val text = element.text.ifBlank { " " }
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, max(1, element.w.toInt()))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(true).setLineSpacing(2f, 1f).build()
            canvas.save(); canvas.clipRect(rect); canvas.translate(element.x, element.y); layout.draw(canvas); canvas.restore()
        }
    }

    private fun saveProjectDialog() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "${paperName.replace(" ", "_")}.nps")
        }
        startActivityForResult(intent, 301)
    }

    private fun openProjectDialog() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"; addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivityForResult(intent, 302)
    }

    private fun saveProject(uri: Uri) {
        try {
            val root = JSONObject()
            root.put("version", 1)
            root.put("paperName", paperName); root.put("paperTagline", paperTagline); root.put("paperDate", paperDate)
            root.put("paperWidth", paperWidth); root.put("paperHeight", paperHeight); root.put("paperType", paperType)
            root.put("pageBackgroundColor", pageBackgroundColor); root.put("pageBackgroundUri", pageBackgroundUri)
            root.put("mastheadBgColor", mastheadBgColor); root.put("mastheadGradient", mastheadGradient)
            root.put("mastheadTitleColor", mastheadTitleColor); root.put("mastheadTitleSize", mastheadTitleSize)
            root.put("mastheadFont", mastheadFont); root.put("mastheadLeftLogoUri", mastheadLeftLogoUri)
            root.put("mastheadRightLogoUri", mastheadRightLogoUri); root.put("mastheadBackgroundUri", mastheadBackgroundUri)
            root.put("pageIndex", pageIndex)
            val pageArray = JSONArray()
            pages.forEach { page ->
                val elementArray = JSONArray()
                page.forEach { e ->
                    val obj = JSONObject()
                    obj.put("type", e.type); obj.put("text", e.text); obj.put("imageUri", e.imageUri)
                    obj.put("x", e.x); obj.put("y", e.y); obj.put("w", e.w); obj.put("h", e.h); obj.put("fontSize", e.fontSize)
                    elementArray.put(obj)
                }
                pageArray.put(elementArray)
            }
            root.put("pages", pageArray)
            contentResolver.openOutputStream(uri).use { output -> requireNotNull(output) { "ফাইল সেভ করা যায়নি" }; output.write(root.toString().toByteArray(Charsets.UTF_8)) }
            toast("Editable Design সেভ হয়েছে")
        } catch (e: Exception) { toast("Design Save Error: ${e.message}") }
    }

    private fun loadProject(uri: Uri) {
        try {
            val json = contentResolver.openInputStream(uri).use { input -> requireNotNull(input) { "ফাইল খোলা যায়নি" }.bufferedReader().use { it.readText() } }
            val root = JSONObject(json)
            pages.clear()
            val pageArray = root.getJSONArray("pages")
            for (p in 0 until pageArray.length()) {
                val elementArray = pageArray.getJSONArray(p)
                val page = mutableListOf<NewsElement>()
                for (i in 0 until elementArray.length()) {
                    val o = elementArray.getJSONObject(i)
                    page.add(NewsElement(o.optString("type", "body"), o.optString("text", ""), o.optString("imageUri", ""),
                        o.optDouble("x", 55.0).toFloat(), o.optDouble("y", 220.0).toFloat(),
                        o.optDouble("w", 500.0).toFloat(), o.optDouble("h", 180.0).toFloat(), o.optDouble("fontSize", 24.0).toFloat()))
                }
                pages.add(page)
            }
            if (pages.isEmpty()) pages.add(mutableListOf())
            paperName = root.optString("paperName", "ES NEWS"); paperTagline = root.optString("paperTagline", "")
            paperDate = root.optString("paperDate", dateNow()); paperWidth = root.optDouble("paperWidth", 1224.0).toFloat()
            paperHeight = root.optDouble("paperHeight", 1584.0).toFloat(); paperType = root.optString("paperType", "NEWSPAPER")
            pageBackgroundColor = root.optInt("pageBackgroundColor", Color.WHITE); pageBackgroundUri = root.optString("pageBackgroundUri", "")
            mastheadBgColor = root.optInt("mastheadBgColor", Color.WHITE); mastheadGradient = root.optBoolean("mastheadGradient", false)
            mastheadTitleColor = root.optInt("mastheadTitleColor", navy); mastheadTitleSize = root.optDouble("mastheadTitleSize", 48.0).toFloat()
            mastheadFont = root.optString("mastheadFont", "sans-serif-condensed")
            mastheadLeftLogoUri = root.optString("mastheadLeftLogoUri", ""); mastheadRightLogoUri = root.optString("mastheadRightLogoUri", "")
            mastheadBackgroundUri = root.optString("mastheadBackgroundUri", "")
            pageIndex = root.optInt("pageIndex", 0).coerceIn(0, pages.lastIndex)
            selectedIndex = -1; clearBitmapCache(); updateEditor(); toast("Design খুলেছে — এখন Edit করতে পারবেন")
        } catch (e: Exception) { toast("Design Open Error: ${e.message}") }
    }

    private fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
}
