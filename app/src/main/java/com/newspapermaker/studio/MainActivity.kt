
package com.newspapermaker.studio

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

private data class NewsItem(
    var kind: String = "text",
    var text: String = "",
    var uri: String = "",
    var x: Float = 40f,
    var y: Float = 200f,
    var w: Float = 500f,
    var h: Float = 180f,
    var font: Float = 28f,
    var color: Int = Color.BLACK,
    var fill: Int = Color.TRANSPARENT,
    var stroke: Int = Color.rgb(25,105,170)
)

private data class NewsPage(
    val items: MutableList<NewsItem> = mutableListOf(),
    var backgroundUri: String = ""
)

class MainActivity : Activity() {

    private val pages = mutableListOf<NewsPage>()
    private var pageIndex = 0
    private var selected = -1

    private var paperW = 1224f
    private var paperH = 1584f
    private var paperSize = "NEWSPAPER"

    private var paperName = "ES NEWS AND ENTERTAINMENT"
    private var tagline = "সংবাদ • মতামত • স্থানীয় খবর"
    private var dateText = ""

    private var mastBg = Color.WHITE
    private var mastText = Color.rgb(25,43,67)
    private var mastBgUri = ""

    private val cache = mutableMapOf<String, Bitmap>()

    private lateinit var preview: PaperView
    private lateinit var pageInfo: TextView
    private lateinit var sizeInfo: TextView

    private val blue = Color.rgb(25,105,170)
    private val navy = Color.rgb(25,43,67)

    private val currentPage: NewsPage
        get() = pages[pageIndex]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dateText = SimpleDateFormat(
            "dd-MM-yyyy", Locale.getDefault()
        ).format(Date())

        pages.add(NewsPage())
        buildUi()
    }

    private fun buildUi() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xffe5eaf0.toInt())
        }

        val header = TextView(this).apply {
            text = "NEWSPAPER MAKER STUDIO"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTypeface(null, 1)
            setPadding(8,14,8,14)
            setBackgroundColor(navy)
        }

        root.addView(header)

        toolbar(root, arrayOf(
            "＋ শিরোনাম",
            "＋ বিস্তারিত",
            "📷 ছবি যোগ",
            "＋ 2 কলাম Grid",
            "＋ 3 কলাম Grid",
            "সম্পাদনা",
            "মুছুন"
        )) { i ->
            when(i) {
                0 -> addText(true)
                1 -> addText(false)
                2 -> chooseImage(10)
                3 -> addGrid(2)
                4 -> addGrid(3)
                5 -> editSelected()
                6 -> deleteSelected()
            }
        }

        toolbar(root, arrayOf(
            "← আগের পৃষ্ঠা",
            "＋ নতুন পৃষ্ঠা",
            "পরের পৃষ্ঠা →",
            "Paper Size",
            "PDF Export",
            "Print"
        )) { i ->
            when(i) {
                0 -> changePage(-1)
                1 -> addNewPage()
                2 -> changePage(1)
                3 -> choosePaper()
                4 -> exportPdf()
                5 -> exportPdf()
            }
        }

        toolbar(root, arrayOf(
            "পত্রিকার নাম",
            "নামের Background",
            "নামের রং",
            "পৃষ্ঠা Background",
            "Masthead ছবি",
            "＋ লোগো যোগ",
            "লোগো মুছুন"
        )) { i ->
            when(i) {
                0 -> editMasthead()
                1 -> chooseMastColor()
                2 -> chooseTitleColor()
                3 -> chooseImage(20)
                4 -> chooseImage(21)
                5 -> chooseImage(22)
                6 -> deleteLogos()
            }
        }

        toolbar(root, arrayOf(
            "Design Save",
            "Design Open",
            "নতুন পত্রিকা"
        )) { i ->
            when(i) {
                0 -> saveDesign()
                1 -> openDesign()
                2 -> newNewspaper()
            }
        }

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10,7,10,7)
            setBackgroundColor(Color.WHITE)
        }

        pageInfo = TextView(this).apply {
            textSize = 14f
            setTextColor(navy)
        }

        sizeInfo = TextView(this).apply {
            textSize = 14f
            setTextColor(blue)
        }

        info.addView(
            pageInfo,
            LinearLayout.LayoutParams(0,-2,1f)
        )

        info.addView(sizeInfo)

        root.addView(info)

        preview = PaperView()

        root.addView(
            preview,
            LinearLayout.LayoutParams(-1,0,1f)
        )

        root.addView(TextView(this).apply {
            text = "খবর/ছবি/লোগো টাচ করে নির্বাচন করুন • টেনে সরান • লাল গোল অংশ টেনে ছোট-বড় করুন"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
            setPadding(5,8,5,8)
            setBackgroundColor(Color.WHITE)
        })

        setContentView(root)
        refresh()
    }

    private fun toolbar(
        root: LinearLayout,
        labels: Array<String>,
        action: (Int)->Unit
    ) {

        val scroll = HorizontalScrollView(this)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(3,2,3,2)
        }

        labels.forEachIndexed { index,label ->

            val button = Button(this).apply {
                text = label
                textSize = 12f
                isAllCaps = false
                setTextColor(Color.WHITE)
                setPadding(8,0,8,0)

                background = GradientDrawable().apply {
                    setColor(blue)
                    cornerRadius = 12f
                }

                setOnClickListener {
                    action(index)
                }
            }

            row.addView(
                button,
                LinearLayout.LayoutParams(-2,44).apply {
                    setMargins(3,2,3,2)
                }
            )
        }

        scroll.addView(row)
        root.addView(scroll)
    }

    private fun refresh() {
        pageInfo.text =
            "পৃষ্ঠা ${pageIndex+1} / ${pages.size}"

        sizeInfo.text = paperSize

        preview.invalidate()
    }

    // ---------------- PAGE CONTROL ----------------

    private fun addNewPage() {
        pages.add(NewsPage())
        pageIndex = pages.lastIndex
        selected = -1
        refresh()
        toast("নতুন পৃষ্ঠা তৈরি হয়েছে")
    }

    private fun changePage(direction: Int) {
        val next = pageIndex + direction

        if(next in pages.indices) {
            pageIndex = next
            selected = -1
            refresh()
        } else {
            toast("আর পৃষ্ঠা নেই")
        }
    }

    private fun newNewspaper() {
        AlertDialog.Builder(this)
            .setTitle("নতুন পত্রিকা")
            .setMessage("বর্তমান সব পৃষ্ঠা মুছে নতুন পত্রিকা শুরু করবেন?")
            .setPositiveButton("হ্যাঁ") { _,_ ->

                pages.clear()
                pages.add(NewsPage())

                pageIndex = 0
                selected = -1

                paperName = "ES NEWS AND ENTERTAINMENT"
                tagline = "সংবাদ • মতামত • স্থানীয় খবর"

                dateText = SimpleDateFormat(
                    "dd-MM-yyyy", Locale.getDefault()
                ).format(Date())

                mastBg = Color.WHITE
                mastText = navy
                mastBgUri = ""

                clearCache()
                refresh()
            }
            .setNegativeButton("না",null)
            .show()
    }

    // ---------------- GRID SYSTEM ----------------

    private fun addGrid(columns: Int) {

        val left = 55f
        val top = if(paperSize=="A4") 125f else 205f
        val gap = 18f
        val totalWidth = paperW - 110f
        val cellWidth = (totalWidth - gap*(columns-1))/columns

        val cellHeight = if(columns==2) 500f else 430f

        for(i in 0 until columns) {

            val box = NewsItem(
                kind = "box",
                x = left + i*(cellWidth+gap),
                y = top,
                w = cellWidth,
                h = cellHeight,
                fill = 0xfff8fbff.toInt(),
                stroke = blue
            )

            currentPage.items.add(box)
        }

        selected = currentPage.items.lastIndex
        refresh()

        toast("$columns কলামের Grid তৈরি হয়েছে")
    }

    private fun targetRect(): RectF? {
        if(selected !in currentPage.items.indices) return null

        val item = currentPage.items[selected]

        if(item.kind=="box") {
            return RectF(
                item.x+12f,
                item.y+12f,
                item.x+item.w-12f,
                item.y+item.h-12f
            )
        }

        return null
    }

    // ---------------- TEXT ----------------

    private fun addText(headline: Boolean) {

        val input = EditText(this).apply {
            hint = if(headline)
                "সংবাদের শিরোনাম লিখুন"
            else
                "সংবাদের বিস্তারিত লিখুন"

            minLines = if(headline) 2 else 5
            gravity = Gravity.TOP

            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }

        AlertDialog.Builder(this)
            .setTitle(if(headline) "শিরোনাম যোগ" else "বিস্তারিত যোগ")
            .setView(input)
            .setPositiveButton("যোগ করুন") { _,_ ->

                val text = input.text.toString()

                if(text.isNotBlank()) {

                    val target = targetRect()

                    val x = target?.left ?: 55f
                    val y = target?.top ?: if(headline) 205f else 400f

                    val w = target?.width() ?: paperW-110f
                    val h = target?.height() ?: if(headline) 130f else 350f

                    val item = NewsItem(
                        kind = "text",
                        text = text,
                        x = x,
                        y = y,
                        w = w,
                        h = h,
                        font = if(headline) 40f else 24f,
                        color = Color.BLACK
                    )

                    currentPage.items.add(item)
                    selected = currentPage.items.lastIndex

                    refresh()
                }
            }
            .setNegativeButton("বাতিল",null)
            .show()
    }

    // ---------------- IMAGE ----------------

    private fun chooseImage(code: Int) {

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivityForResult(intent,code)
    }

    // ---------------- LOGO ----------------

    private fun addLogo(uri: Uri) {

        val item = NewsItem(
            kind = "logo",
            uri = uri.toString(),
            x = paperW/2f-50f,
            y = 15f,
            w = 100f,
            h = 100f
        )

        currentPage.items.add(item)
        selected = currentPage.items.lastIndex

        refresh()
        toast("নতুন লোগো যোগ হয়েছে")
    }

    private fun deleteLogos() {

        val logos = currentPage.items.filter {
            it.kind=="logo"
        }

        if(logos.isEmpty()) {
            toast("এই পাতায় কোনো লোগো নেই")
            return
        }

        currentPage.items.removeAll {
            it.kind=="logo"
        }

        selected = -1
        refresh()
        toast("লোগো মুছে ফেলা হয়েছে")
    }

    // ---------------- EDIT / DELETE ----------------

    private fun editSelected() {

        if(selected !in currentPage.items.indices) {
            toast("আগে একটি লেখা নির্বাচন করুন")
            return
        }

        val item = currentPage.items[selected]

        if(item.kind=="image" || item.kind=="logo") {
            chooseImage(if(item.kind=="logo") 22 else 10)
            return
        }

        if(item.kind=="box") {
            toast("Box সরাতে বা ছোট-বড় করতে টেনে ধরুন")
            return
        }

        val input = EditText(this).apply {
            setText(item.text)
            minLines = 4
            gravity = Gravity.TOP
        }

        AlertDialog.Builder(this)
            .setTitle("সম্পাদনা")
            .setView(input)
            .setPositiveButton("সংরক্ষণ") { _,_ ->
                item.text = input.text.toString()
                refresh()
            }
            .setNegativeButton("বাতিল",null)
            .show()
    }

    private fun deleteSelected() {

        if(selected in currentPage.items.indices) {
            currentPage.items.removeAt(selected)
            selected = -1
            refresh()
        } else {
            toast("কোনো উপাদান নির্বাচন করা হয়নি")
        }
    }

    // ---------------- MASTHEAD ----------------

    private fun editMasthead() {

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20,8,20,4)
        }

        val name = EditText(this).apply {
            hint = "পত্রিকার নাম"
            setText(paperName)
        }

        val tag = EditText(this).apply {
            hint = "স্লোগান"
            setText(tagline)
        }

        val date = EditText(this).apply {
            hint = "প্রকাশের তারিখ"
            setText(dateText)
        }

        box.addView(name)
        box.addView(tag)
        box.addView(date)

        AlertDialog.Builder(this)
            .setTitle("পত্রিকার নাম ও ডিজাইন")
            .setView(box)
            .setPositiveButton("সংরক্ষণ") { _,_ ->
                paperName = name.text.toString()
                tagline = tag.text.toString()
                dateText = date.text.toString()
                refresh()
            }
            .setNegativeButton("বাতিল",null)
            .show()
    }

    private fun chooseMastColor() {

        val labels = arrayOf(
            "সাদা","হালকা নীল","নীল","লাল",
            "কালো","কমলা","সবুজ","গোলাপি"
        )

        val colors = intArrayOf(
            Color.WHITE,
            0xffe3f2fd.toInt(),
            Color.BLUE,
            Color.RED,
            Color.BLACK,
            0xffff9800.toInt(),
            Color.GREEN,
            0xffffc1dc.toInt()
        )

        AlertDialog.Builder(this)
            .setTitle("Masthead Background")
            .setItems(labels) { _,i ->
                mastBg = colors[i]
                refresh()
            }
            .show()
    }

    private fun chooseTitleColor() {

        val labels = arrayOf(
            "কালো","সাদা","নীল","লাল","কমলা","সবুজ"
        )

        val colors = intArrayOf(
            Color.BLACK,
            Color.WHITE,
            Color.BLUE,
            Color.RED,
            0xffff9800.toInt(),
            Color.GREEN
        )

        AlertDialog.Builder(this)
            .setTitle("পত্রিকার নামের রং")
            .setItems(labels) { _,i ->
                mastText = colors[i]
                refresh()
            }
            .show()
    }

    private fun choosePaper() {

        AlertDialog.Builder(this)
            .setTitle("Paper Size")
            .setItems(arrayOf(
                "Newspaper Broadsheet (17 × 22 inch)",
                "A4 Paper"
            )) { _,i ->

                if(i==0) {
                    paperW = 1224f
                    paperH = 1584f
                    paperSize = "NEWSPAPER"
                } else {
                    paperW = 595f
                    paperH = 842f
                    paperSize = "A4"
                }

                refresh()
            }
            .show()
    }

    // ---------------- IMAGE RESULT ----------------

    @Deprecated("Document picker")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode,resultCode,data)

        if(resultCode!=RESULT_OK || data?.data==null) return

        val uri = data.data!!

        try {
            val flags = data.flags and
                Intent.FLAG_GRANT_READ_URI_PERMISSION

            if(flags!=0) {
                contentResolver.takePersistableUriPermission(uri,flags)
            }
        } catch(_: Exception) {}

        when(requestCode) {

            10 -> {

                val target = targetRect()

                val item = NewsItem(
                    kind = "image",
                    uri = uri.toString(),
                    x = target?.left ?: 55f,
                    y = target?.top ?: 220f,
                    w = target?.width() ?: paperW*.55f,
                    h = target?.height() ?: paperH*.22f
                )

                if(selected in currentPage.items.indices &&
                    currentPage.items[selected].kind=="image") {
                    currentPage.items[selected].uri = uri.toString()
                } else {
                    currentPage.items.add(item)
                    selected = currentPage.items.lastIndex
                }

                refresh()
            }

            20 -> {
                currentPage.backgroundUri = uri.toString()
                refresh()
            }

            21 -> {
                mastBgUri = uri.toString()
                refresh()
            }

            22 -> addLogo(uri)

            30 -> loadDesign(uri)

            40 -> saveDesignTo(uri)

            31 -> savePdf(uri)
        }
    }

    // ---------------- DESIGN SAVE / OPEN ----------------

    private fun saveDesign() {

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(
                Intent.EXTRA_TITLE,
                "Newspaper-Design.json"
            )
        }

        startActivityForResult(intent,40)
    }

    private fun openDesign() {

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }

        startActivityForResult(intent,30)
    }

    private fun saveDesignTo(uri: Uri) {

        try {

            val root = JSONObject()

            root.put("paperName",paperName)
            root.put("tagline",tagline)
            root.put("date",dateText)

            root.put("paperW",paperW)
            root.put("paperH",paperH)
            root.put("paperSize",paperSize)

            root.put("mastBg",mastBg)
            root.put("mastText",mastText)
            root.put("mastBgUri",mastBgUri)

            val pageArray = JSONArray()

            pages.forEach { page ->

                val pageObject = JSONObject()
                pageObject.put("backgroundUri",page.backgroundUri)

                val itemArray = JSONArray()

                page.items.forEach { e ->

                    itemArray.put(JSONObject()
                        .put("kind",e.kind)
                        .put("text",e.text)
                        .put("uri",e.uri)
                        .put("x",e.x)
                        .put("y",e.y)
                        .put("w",e.w)
                        .put("h",e.h)
                        .put("font",e.font)
                        .put("color",e.color)
                        .put("fill",e.fill)
                        .put("stroke",e.stroke)
                    )
                }

                pageObject.put("items",itemArray)
                pageArray.put(pageObject)
            }

            root.put("pages",pageArray)

            contentResolver.openOutputStream(uri)!!.bufferedWriter().use {
                it.write(root.toString(2))
            }

            toast("সম্পূর্ণ Design Save হয়েছে")

        } catch(e:Exception) {
            toast("Save Error: ${e.message}")
        }
    }

    private fun loadDesign(uri: Uri) {

        try {

            val json = contentResolver.openInputStream(uri)!!
                .bufferedReader().use { it.readText() }

            val root = JSONObject(json)

            paperName = root.optString("paperName","ES NEWS")
            tagline = root.optString("tagline","")
            dateText = root.optString("date","")

            paperW = root.optDouble("paperW",1224.0).toFloat()
            paperH = root.optDouble("paperH",1584.0).toFloat()
            paperSize = root.optString("paperSize","NEWSPAPER")

            mastBg = root.optInt("mastBg",Color.WHITE)
            mastText = root.optInt("mastText",navy)
            mastBgUri = root.optString("mastBgUri","")

            pages.clear()

            val pageArray = root.getJSONArray("pages")

            for(p in 0 until pageArray.length()) {

                val pageObject = pageArray.getJSONObject(p)
                val page = NewsPage()

                page.backgroundUri =
                    pageObject.optString("backgroundUri","")

                val arr = pageObject.getJSONArray("items")

                for(i in 0 until arr.length()) {

                    val o = arr.getJSONObject(i)

                    page.items.add(
                        NewsItem(
                            kind = o.optString("kind","text"),
                            text = o.optString("text",""),
                            uri = o.optString("uri",""),
                            x = o.optDouble("x",40.0).toFloat(),
                            y = o.optDouble("y",200.0).toFloat(),
                            w = o.optDouble("w",500.0).toFloat(),
                            h = o.optDouble("h",180.0).toFloat(),
                            font = o.optDouble("font",28.0).toFloat(),
                            color = o.optInt("color",Color.BLACK),
                            fill = o.optInt("fill",Color.TRANSPARENT),
                            stroke = o.optInt("stroke",blue)
                        )
                    )
                }

                pages.add(page)
            }

            if(pages.isEmpty()) pages.add(NewsPage())

            pageIndex = 0
            selected = -1

            clearCache()
            refresh()

            toast("Design Open হয়েছে")

        } catch(e:Exception) {
            toast("Design খোলা যায়নি: ${e.message}")
        }
    }

    // ---------------- PDF EXPORT ----------------

    private fun exportPdf() {

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"

            putExtra(
                Intent.EXTRA_TITLE,
                "Newspaper.pdf"
            )
        }

        startActivityForResult(intent,31)
    }

    private fun savePdf(uri: Uri) {

        val pdf = PdfDocument()

        try {

            pages.forEachIndexed { index,pageData ->

                val info = PdfDocument.PageInfo.Builder(
                    paperW.toInt(),
                    paperH.toInt(),
                    index+1
                ).create()

                val page = pdf.startPage(info)

                drawPage(page.canvas,pageData,index+1)

                pdf.finishPage(page)
            }

            contentResolver.openOutputStream(uri)!!.use {
                pdf.writeTo(it)
            }

            toast("সম্পূর্ণ Newspaper PDF তৈরি হয়েছে")

        } catch(e:Exception) {

            toast("PDF Error: ${e.message}")

        } finally {
            pdf.close()
        }
    }

    // ---------------- BITMAP ----------------

    private fun bitmap(uri:String): Bitmap? {

        if(uri.isBlank()) return null

        cache[uri]?.let {
            if(!it.isRecycled) return it
        }

        return try {

            contentResolver.openInputStream(Uri.parse(uri)).use {
                if(it==null) null
                else BitmapFactory.decodeStream(it)
            }?.also {
                cache[uri] = it
            }

        } catch(_:Exception) {
            null
        }
    }

    private fun clearCache() {
        cache.values.forEach {
            if(!it.isRecycled) it.recycle()
        }
        cache.clear()
    }

    // ---------------- PAGE DRAW ----------------

    private fun drawPage(
        canvas: Canvas,
        page: NewsPage,
        pageNumber: Int
    ) {

        canvas.drawColor(Color.WHITE)

        bitmap(page.backgroundUri)?.let {

            canvas.drawBitmap(
                it,null,
                RectF(0f,0f,paperW,paperH),
                Paint(3)
            )
        }

        drawMasthead(canvas)

        page.items.forEach {
            drawItem(canvas,it)
        }

        val footer = Paint(3).apply {
            color = Color.GRAY
            textSize = 15f
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText(
            "পৃষ্ঠা $pageNumber",
            paperW/2f,
            paperH-25f,
            footer
        )
    }

    private fun drawMasthead(canvas: Canvas) {

        val mh = if(paperSize=="A4") 105f else 175f

        canvas.drawRect(
            0f,0f,paperW,mh,
            Paint(3).apply { color=mastBg }
        )

        bitmap(mastBgUri)?.let {

            canvas.drawBitmap(
                it,null,
                RectF(0f,0f,paperW,mh),
                Paint(3)
            )
        }

        val title = Paint(3).apply {
            color = mastText
            textAlign = Paint.Align.CENTER
            textSize = if(paperSize=="A4") 30f else 48f
            typeface = Typeface.create(
                "sans-serif-condensed",
                Typeface.BOLD
            )
        }

        val maxWidth = paperW-100f
        val measured = title.measureText(paperName)

        if(measured>maxWidth && measured>0f) {
            title.textScaleX = maxWidth/measured
        }

        canvas.drawText(
            paperName,
            paperW/2f,
            if(paperSize=="A4") 48f else 75f,
            title
        )

        val meta = Paint(3).apply {
            color = Color.DKGRAY
            textAlign = Paint.Align.CENTER
            textSize = if(paperSize=="A4") 10f else 20f
        }

        canvas.drawText(
            dateText,paperW/2f,
            if(paperSize=="A4") 69f else 110f,meta
        )

        canvas.drawText(
            tagline,paperW/2f,
            if(paperSize=="A4") 87f else 145f,meta
        )

        val line = Paint(3).apply {
            color = blue
            strokeWidth = if(paperSize=="A4") 3f else 7f
        }

        canvas.drawLine(
            35f,mh,paperW-35f,mh,line
        )
    }

    private fun drawItem(canvas: Canvas,e:NewsItem) {

        val rect = RectF(
            e.x,e.y,e.x+e.w,e.y+e.h
        )

        when(e.kind) {

            "image","logo" -> {

                bitmap(e.uri)?.let {
                    canvas.drawBitmap(
                        it,null,rect,Paint(3)
                    )
                }
            }

            "box" -> {

                val fillPaint = Paint(3).apply {
                    color = e.fill
                    style = Paint.Style.FILL
                }

                canvas.drawRect(rect,fillPaint)

                val border = Paint(3).apply {
                    color = e.stroke
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }

                canvas.drawRect(rect,border)
            }

            else -> {

                val paint = TextPaint(3).apply {
                    color = e.color
                    textSize = e.font
                    typeface = Typeface.create(
                        "sans-serif",
                        if(e.font>34f) Typeface.BOLD
                        else Typeface.NORMAL
                    )
                }

                val text = e.text.ifBlank { " " }

                val layout = StaticLayout.Builder.obtain(
                    text,
                    0,
                    text.length,
                    paint,
                    e.w.toInt().coerceAtLeast(1)
                )
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setIncludePad(true)
                    .setLineSpacing(2f,1f)
                    .build()

                canvas.save()
                canvas.clipRect(rect)
                canvas.translate(e.x,e.y)
                layout.draw(canvas)
                canvas.restore()
            }
        }
    }

    // ---------------- PREVIEW & TOUCH ----------------

    private inner class PaperView: View(this@MainActivity) {

        private var scale = 1f
        private var ox = 0f
        private var oy = 0f

        private var lastX = 0f
        private var lastY = 0f

        private var moving = false
        private var resizing = false

        override fun onDraw(canvas: Canvas) {

            super.onDraw(canvas)

            canvas.drawColor(0xffdce2e9.toInt())

            scale = min(
                width.toFloat()/paperW,
                height.toFloat()/paperH
            ).coerceAtLeast(.01f)

            ox = (width-paperW*scale)/2f
            oy = (height-paperH*scale)/2f

            canvas.save()
            canvas.translate(ox,oy)
            canvas.scale(scale,scale)

            drawPage(canvas,currentPage,pageIndex+1)

            if(selected in currentPage.items.indices) {

                val e = currentPage.items[selected]

                val rect = RectF(
                    e.x,e.y,e.x+e.w,e.y+e.h
                )

                val outline = Paint(3).apply {
                    color = Color.BLUE
                    style = Paint.Style.STROKE
                    strokeWidth = 4f
                }

                canvas.drawRect(rect,outline)

                canvas.drawCircle(
                    rect.right,
                    rect.bottom,
                    17f,
                    Paint(3).apply {
                        color = Color.RED
                    }
                )
            }

            val border = Paint(3).apply {
                color = blue
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }

            canvas.drawRect(
                1f,1f,paperW-1f,paperH-1f,border
            )

            canvas.restore()
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {

            val x = (event.x-ox)/scale
            val y = (event.y-oy)/scale

            when(event.action) {

                MotionEvent.ACTION_DOWN -> {

                    lastX = x
                    lastY = y

                    moving = false
                    resizing = false
                    selected = -1

                    for(i in currentPage.items.indices.reversed()) {

                        val e = currentPage.items[i]

                        val right = e.x+e.w
                        val bottom = e.y+e.h

                        if(
                            x>=right-30f && x<=right+30f &&
                            y>=bottom-30f && y<=bottom+30f
                        ) {
                            selected = i
                            resizing = true
                            break
                        }

                        if(
                            x>=e.x && x<=right &&
                            y>=e.y && y<=bottom
                        ) {
                            selected = i
                            moving = true
                            break
                        }
                    }

                    invalidate()
                    return true
                }

                MotionEvent.ACTION_MOVE -> {

                    if(selected in currentPage.items.indices) {

                        val e = currentPage.items[selected]

                        val dx = x-lastX
                        val dy = y-lastY

                        if(moving) {

                            e.x = (e.x+dx).coerceIn(
                                0f,paperW-e.w
                            )

                            e.y = (e.y+dy).coerceIn(
                                0f,paperH-e.h
                            )
                        }

                        if(resizing) {

                            e.w = (e.w+dx).coerceIn(
                                60f,paperW-e.x
                            )

                            e.h = (e.h+dy).coerceIn(
                                50f,paperH-e.y
                            )
                        }

                        lastX = x
                        lastY = y

                        invalidate()
                    }

                    return true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {

                    moving = false
                    resizing = false
                    invalidate()

                    return true
                }
            }

            return true
        }
    }

    private fun toast(message:String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}
