
package com.newspapermaker.studio

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.MotionEvent
import android.view.View
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import kotlin.math.min
import kotlin.math.max
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NewsElement(
    var type: String = "body",
    var text: String = "",
    var imageUri: String = "",
    var x: Float = 55f,
    var y: Float = 220f,
    var w: Float = 500f,
    var h: Float = 180f,
    var fontSize: Float = 24f,
    var color: Int = Color.BLACK
)

class MainActivity : Activity() {

    private val pages =
        mutableListOf<MutableList<NewsElement>>(
            mutableListOf()
        )

    private var pageIndex = 0
    private var selectedIndex = -1

    private var paperWidth = 1224f
    private var paperHeight = 1584f
    private var paperType = "NEWSPAPER"

    private var paperName = "ES NEWS"
    private var paperTagline = "সংবাদ • মতামত • স্থানীয় খবর"
    private var paperDate = ""

    // Masthead design
    private var leftLogo = ""
    private var rightLogo = ""
    private var mastheadColor = Color.rgb(25, 43, 67)
    private var mastheadFontSize = 48f

    private lateinit var editor: NewspaperView
    private lateinit var pageLabel: TextView
    private lateinit var sizeLabel: TextView

    private val navy = Color.rgb(25, 43, 67)
    private val blue = Color.rgb(25, 105, 170)

    private val bitmapCache =
        mutableMapOf<String, Bitmap>()

    private fun currentPage() = pages[pageIndex]

    private fun dateNow(): String {
        return SimpleDateFormat(
            "dd-MM-yyyy",
            Locale.getDefault()
        ).format(Date())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        paperDate = dateNow()
        createInterface()
    }

    // MAIN INTERFACE

    private fun createInterface() {

        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(235, 239, 244))
        }

        val header = TextView(this).apply {
            text = "NEWSPAPER MAKER STUDIO"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(8, 17, 8, 17)
            setBackgroundColor(navy)
        }

        main.addView(header)

        val toolbarScroll = HorizontalScrollView(this)

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(5, 5, 5, 5)
        }

        toolbarScroll.addView(toolbar)
        main.addView(toolbarScroll)

        tool(toolbar, "＋ শিরোনাম") {
            addText("headline")
        }

        tool(toolbar, "＋ বিস্তারিত") {
            addText("body")
        }

        tool(toolbar, "📷 ছবি") {
            openGallery(200)
        }

        tool(toolbar, "✎ সম্পাদনা") {
            editSelected()
        }

        tool(toolbar, "মুছুন") {
            deleteSelected()
        }

        val secondScroll = HorizontalScrollView(this)

        val secondToolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(5, 3, 5, 3)
        }

        secondScroll.addView(secondToolbar)
        main.addView(secondScroll)

        tool(secondToolbar, "← আগের পৃষ্ঠা") {
            changePage(-1)
        }

        tool(secondToolbar, "＋ নতুন পৃষ্ঠা") {
            addNewPage()
        }

        tool(secondToolbar, "পরের পৃষ্ঠা →") {
            changePage(1)
        }

        tool(secondToolbar, "Paper Size") {
            changePaperSize()
        }

        tool(secondToolbar, "PDF Export") {
            exportPdfDialog()
        }

        val thirdScroll = HorizontalScrollView(this)

        val thirdToolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(5, 3, 5, 3)
        }

        thirdScroll.addView(thirdToolbar)
        main.addView(thirdScroll)

        tool(thirdToolbar, "📰 পত্রিকার নাম ও ডিজাইন") {
            changePaperName()
        }

        tool(thirdToolbar, "বাঁ পাশের ছবি") {
            openGallery(201)
        }

        tool(thirdToolbar, "ডান পাশের ছবি") {
            openGallery(202)
        }

        tool(thirdToolbar, "নতুন পত্রিকা") {
            newNewspaper()
        }

        val infoBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(5, 5, 5, 5)
            setBackgroundColor(Color.WHITE)
        }

        pageLabel = TextView(this).apply {
            textSize = 13f
            setTextColor(navy)
        }

        sizeLabel = TextView(this).apply {
            textSize = 13f
            setTextColor(blue)
            gravity = Gravity.END
        }

        infoBar.addView(
            pageLabel,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        infoBar.addView(
            sizeLabel,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        main.addView(infoBar)

        editor = NewspaperView()

        main.addView(
            editor,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        val help = TextView(this).apply {
            text = "খবর নির্বাচন করতে টাচ করুন • সরাতে টেনে ধরুন • ছোট-বড় করতে নিচের ডান কোণ টানুন"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
            setPadding(6, 9, 6, 9)
            setBackgroundColor(Color.WHITE)
        }

        main.addView(help)

        setContentView(main)
        updateEditor()
    }

    // TOOL BUTTON

    private fun tool(
        parent: LinearLayout,
        text: String,
        action: () -> Unit
    ) {

        val button = Button(this).apply {
            this.text = text
            textSize = 12f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setPadding(10, 3, 10, 3)

            background = GradientDrawable().apply {
                setColor(blue)
                cornerRadius = 12f
            }

            setOnClickListener { action() }
        }

        parent.addView(
            button,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                48
            ).apply {
                setMargins(3, 2, 3, 2)
            }
        )
    }

    private fun updateEditor() {
        pageLabel.text =
            "পৃষ্ঠা ${pageIndex + 1} / ${pages.size}"

        sizeLabel.text = paperType
        editor.invalidate()
    }

    // PAGE CONTROL

    private fun changePage(direction: Int) {

        val next = pageIndex + direction

        if (next in pages.indices) {
            pageIndex = next
            selectedIndex = -1
            updateEditor()
        } else {
            toast("আর পৃষ্ঠা নেই")
        }
    }

    private fun addNewPage() {
        pages.add(mutableListOf())
        pageIndex = pages.lastIndex
        selectedIndex = -1
        updateEditor()
        toast("নতুন পৃষ্ঠা তৈরি হয়েছে")
    }

    // NEW NEWSPAPER

    private fun newNewspaper() {

        AlertDialog.Builder(this)
            .setTitle("নতুন পত্রিকা তৈরি")
            .setMessage(
                "বর্তমান পত্রিকার সব খবর ও পৃষ্ঠা বন্ধ করে নতুন পত্রিকা শুরু করবেন?"
            )
            .setPositiveButton("হ্যাঁ, নতুন তৈরি করুন") { _, _ ->

                pages.clear()
                pages.add(mutableListOf())

                pageIndex = 0
                selectedIndex = -1

                paperName = "ES NEWS"
                paperTagline = "সংবাদ • মতামত • স্থানীয় খবর"
                paperDate = dateNow()

                leftLogo = ""
                rightLogo = ""

                mastheadColor = navy
                mastheadFontSize = 48f

                bitmapCache.values.forEach {
                    if (!it.isRecycled) it.recycle()
                }

                bitmapCache.clear()

                updateEditor()
                toast("নতুন পত্রিকা তৈরি হয়েছে")
            }
            .setNegativeButton("না", null)
            .show()
    }

    // NEWSPAPER NAME AND DESIGN

    private fun changePaperName() {

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 10, 20, 5)
        }

        val nameInput = EditText(this).apply {
            hint = "পত্রিকার নাম"
            setText(paperName)
            textSize = 17f
        }

        val taglineInput = EditText(this).apply {
            hint = "পত্রিকার নিচের লাইন"
            setText(paperTagline)
            textSize = 15f
        }

        val dateInput = EditText(this).apply {
            hint = "প্রকাশের তারিখ"
            setText(paperDate)
            textSize = 15f
        }

        val colorSpinner = Spinner(this)

        val colorNames = arrayOf(
            "Navy Blue",
            "Black",
            "Red",
            "Green",
            "Orange",
            "Purple"
        )

        colorSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            colorNames
        )

        val colorValues = intArrayOf(
            Color.rgb(25, 43, 67),
            Color.BLACK,
            Color.RED,
            Color.rgb(0, 110, 50),
            Color.rgb(230, 110, 0),
            Color.rgb(110, 40, 150)
        )

        val sizeLabel = TextView(this).apply {
            text = "নামের সাইজ: ${mastheadFontSize.toInt()}"
            setTextColor(navy)
        }

        val sizeSeek = SeekBar(this).apply {
            max = 60
            progress = (mastheadFontSize - 30f).toInt()
                .coerceIn(0, 60)

            setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {

                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        sizeLabel.text =
                            "নামের সাইজ: ${progress + 30}"
                    }

                    override fun onStartTrackingTouch(
                        seekBar: SeekBar?
                    ) {}

                    override fun onStopTrackingTouch(
                        seekBar: SeekBar?
                    ) {}
                }
            )
        }

        container.addView(nameInput)
        container.addView(taglineInput)
        container.addView(dateInput)

        val colorTitle = TextView(this).apply {
            text = "পত্রিকার নামের রং"
            setTextColor(navy)
            setPadding(0, 12, 0, 4)
        }

        container.addView(colorTitle)
        container.addView(colorSpinner)
        container.addView(sizeLabel)
        container.addView(sizeSeek)

        AlertDialog.Builder(this)
            .setTitle("পত্রিকার নাম ও ডিজাইন")
            .setView(container)
            .setPositiveButton("সংরক্ষণ") { _, _ ->

                paperName = nameInput.text.toString()
                    .ifBlank { "ES NEWS" }

                paperTagline = taglineInput.text.toString()
                paperDate = dateInput.text.toString()
                    .ifBlank { dateNow() }

                mastheadColor =
                    colorValues[colorSpinner.selectedItemPosition]

                mastheadFontSize =
                    (sizeSeek.progress + 30).toFloat()

                updateEditor()
                toast("পত্রিকার ডিজাইন পরিবর্তন হয়েছে")
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    // PAPER SIZE

    private fun changePaperSize() {

        val options = arrayOf(
            "Newspaper Broadsheet (17 × 22 inch)",
            "A4 Paper"
        )

        AlertDialog.Builder(this)
            .setTitle("পেপারের সাইজ নির্বাচন করুন")
            .setItems(options) { _, which ->

                if (which == 0) {
                    paperWidth = 1224f
                    paperHeight = 1584f
                    paperType = "NEWSPAPER"
                } else {
                    paperWidth = 595f
                    paperHeight = 842f
                    paperType = "A4"
                }

                updateEditor()
            }
            .show()
    }

    // ADD TEXT

    private fun addText(type: String) {

        val input = EditText(this).apply {
            hint = if (type == "headline")
                "সংবাদের শিরোনাম লিখুন"
            else
                "সংবাদের বিস্তারিত লিখুন"

            textSize = 17f
            minLines = if (type == "body") 5 else 2
            gravity = Gravity.TOP

            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }

        AlertDialog.Builder(this)
            .setTitle("নতুন খবর তৈরি করুন")
            .setView(input)
            .setPositiveButton("যোগ করুন") { _, _ ->

                val text = input.text.toString()

                if (text.isNotBlank()) {

                    val headline = type == "headline"

                    val element = NewsElement(
                        type = type,
                        text = text,
                        x = 55f,
                        y = if (paperType == "A4") 120f else 195f,
                        w = paperWidth - 110f,
                        h = if (headline) 180f else 360f,
                        fontSize = if (headline) 38f else 23f
                    )

                    currentPage().add(element)
                    selectedIndex = currentPage().lastIndex

                    updateEditor()
                }
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    // EDIT TEXT

    private fun editSelected() {

        if (selectedIndex !in currentPage().indices) {
            toast("আগে একটি খবর নির্বাচন করুন")
            return
        }

        val element = currentPage()[selectedIndex]

        if (element.type == "image") {
            toast("ছবি পরিবর্তন করতে নতুন ছবি বসান")
            return
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(15, 5, 15, 5)
        }

        val input = EditText(this).apply {
            setText(element.text)
            textSize = 17f
            minLines = 5
            gravity = Gravity.TOP
            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }

        val size = SeekBar(this).apply {
            max = 50
            progress = (element.fontSize - 12f).toInt()
                .coerceIn(0, 50)
        }

        val colorSpinner = Spinner(this)
        val colors = arrayOf(
            "কালো",
            "লাল",
            "নীল",
            "সবুজ"
        )

        colorSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            colors
        )

        val colorValues = intArrayOf(
            Color.BLACK,
            Color.RED,
            Color.BLUE,
            Color.GREEN
        )

        container.addView(input)
        container.addView(TextView(this).apply {
            text = "লেখার সাইজ"
        })
        container.addView(size)
        container.addView(TextView(this).apply {
            text = "লেখার রং"
        })
        container.addView(colorSpinner)

        AlertDialog.Builder(this)
            .setTitle("খবর সম্পাদনা")
            .setView(container)
            .setPositiveButton("সংরক্ষণ") { _, _ ->

                element.text = input.text.toString()
                element.fontSize = (size.progress + 12).toFloat()
                element.color =
                    colorValues[colorSpinner.selectedItemPosition]

                updateEditor()
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    // DELETE

    private fun deleteSelected() {

        if (selectedIndex in currentPage().indices) {

            currentPage().removeAt(selectedIndex)
            selectedIndex = -1
            updateEditor()

        } else {
            toast("কোনো খবর নির্বাচন করা হয়নি")
        }
    }

    // GALLERY

    private fun openGallery(requestCode: Int) {

        val intent = Intent(
            Intent.ACTION_OPEN_DOCUMENT
        ).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivityForResult(intent, requestCode)
    }

    @Deprecated("Document picker")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (resultCode != RESULT_OK) return

        val uri = data?.data ?: return

        try {

            val flags = data.flags and
                Intent.FLAG_GRANT_READ_URI_PERMISSION

            if (flags != 0) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    flags
                )
            }

            when (requestCode) {

                200 -> {

                    val element = NewsElement(
                        type = "image",
                        imageUri = uri.toString(),
                        x = 55f,
                        y = if (paperType == "A4") 120f else 195f,
                        w = paperWidth * 0.55f,
                        h = paperHeight * 0.20f
                    )

                    currentPage().add(element)
                    selectedIndex = currentPage().lastIndex
                    updateEditor()
                }

                201 -> {
                    leftLogo = uri.toString()
                    updateEditor()
                    toast("বাঁ পাশের ছবি যোগ হয়েছে")
                }

                202 -> {
                    rightLogo = uri.toString()
                    updateEditor()
                    toast("ডান পাশের ছবি যোগ হয়েছে")
                }

                101 -> {
                    savePdf(uri)
                }
            }

        } catch (e: Exception) {
            toast("ছবি নির্বাচন করা যায়নি: ${e.message}")
        }
    }

    // IMAGE LOADER

    private fun getBitmap(uri: String): Bitmap? {

        if (uri.isBlank()) return null

        if (bitmapCache.containsKey(uri)) {
            return bitmapCache[uri]
        }

        return try {

            val bitmap = contentResolver
                .openInputStream(Uri.parse(uri))
                .use { stream ->
                    BitmapFactory.decodeStream(stream)
                }

            if (bitmap != null) {
                bitmapCache[uri] = bitmap
            }

            bitmap

        } catch (_: Exception) {
            null
        }
    }

    // NEWSPAPER DESIGN VIEW

    inner class NewspaperView : View(this@MainActivity) {

        private var scaleFactor = 1f
        private var offsetX = 0f
        private var offsetY = 0f

        private var moving = false
        private var resizing = false

        private var lastX = 0f
        private var lastY = 0f

        private val paperPaint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = Color.WHITE
        }

        private val borderPaint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = blue
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        private val selectedPaint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = Color.rgb(20, 120, 220)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        private val handlePaint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = Color.RED
        }

        override fun onDraw(canvas: Canvas) {

            super.onDraw(canvas)

            canvas.drawColor(Color.rgb(220, 225, 232))

            scaleFactor = min(
                width / paperWidth,
                height / paperHeight
            ).coerceAtLeast(0.01f)

            offsetX =
                (width - paperWidth * scaleFactor) / 2f

            offsetY =
                (height - paperHeight * scaleFactor) / 2f

            canvas.save()

            canvas.translate(offsetX, offsetY)
            canvas.scale(scaleFactor, scaleFactor)

            canvas.drawRect(
                0f, 0f,
                paperWidth, paperHeight,
                paperPaint
            )

            drawMasthead(canvas, pageIndex + 1)

            currentPage().forEachIndexed { index, element ->

                drawElement(canvas, element)

                if (index == selectedIndex) {
                    drawSelection(canvas, element)
                }
            }

            canvas.drawRect(
                1f, 1f,
                paperWidth - 1f,
                paperHeight - 1f,
                borderPaint
            )

            canvas.restore()
        }

        // MASTHEAD

        private fun drawMasthead(
            canvas: Canvas,
            number: Int
        ) {

            val a4 = paperType == "A4"

            val centerY = if (a4) 50f else 75f
            val imageSize = if (a4) 48f else 105f

            val titlePaint = Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = mastheadColor
                textSize = if (a4)
                    mastheadFontSize * 0.56f
                else
                    mastheadFontSize

                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }

            val metaPaint = Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = Color.DKGRAY
                textSize = if (a4) 10f else 20f
                textAlign = Paint.Align.CENTER
            }

            val linePaint = Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = blue
                strokeWidth = if (a4) 3f else 7f
            }

            // LEFT LOGO

            drawLogo(
                canvas,
                leftLogo,
                RectF(
                    45f,
                    15f,
                    45f + imageSize,
                    15f + imageSize
                )
            )

            // RIGHT LOGO

            drawLogo(
                canvas,
                rightLogo,
                RectF(
                    paperWidth - 45f - imageSize,
                    15f,
                    paperWidth - 45f,
                    15f + imageSize
                )
            )

            // NEWSPAPER NAME

            canvas.drawText(
                paperName,
                paperWidth / 2f,
                centerY,
                titlePaint
            )

            canvas.drawText(
                paperDate,
                paperWidth / 2f,
                if (a4) 70f else 110f,
                metaPaint
            )

            canvas.drawText(
                paperTagline,
                paperWidth / 2f,
                if (a4) 88f else 145f,
                metaPaint
            )

            canvas.drawLine(
                45f,
                if (a4) 105f else 175f,
                paperWidth - 45f,
                if (a4) 105f else 175f,
                linePaint
            )

            val pagePaint = Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = Color.GRAY
                textSize = 15f
                textAlign = Paint.Align.CENTER
            }

            canvas.drawText(
                "পৃষ্ঠা $number",
                paperWidth / 2f,
                paperHeight - 25f,
                pagePaint
            )
        }

        private fun drawLogo(
            canvas: Canvas,
            uri: String,
            rect: RectF
        ) {

            if (uri.isBlank()) return

            val bitmap = getBitmap(uri) ?: return

            canvas.drawBitmap(
                bitmap,
                null,
                rect,
                Paint(
                    Paint.ANTI_ALIAS_FLAG or
                    Paint.FILTER_BITMAP_FLAG
                )
            )
        }

        // DRAW ELEMENT

        private fun drawElement(
            canvas: Canvas,
            element: NewsElement
        ) {

            val rect = RectF(
                element.x,
                element.y,
                element.x + element.w,
                element.y + element.h
            )

            if (element.type == "image") {

                val bitmap = getBitmap(element.imageUri)

                if (bitmap != null) {

                    canvas.drawBitmap(
                        bitmap,
                        null,
                        rect,
                        Paint(
                            Paint.ANTI_ALIAS_FLAG or
                            Paint.FILTER_BITMAP_FLAG
                        )
                    )

                } else {
                    canvas.drawRect(rect, borderPaint)
                }

            } else {

                val paint = TextPaint(
                    Paint.ANTI_ALIAS_FLAG
                ).apply {
                    color = element.color
                    textSize = element.fontSize

                    typeface =
                        if (element.type == "headline")
                            Typeface.DEFAULT_BOLD
                        else
                            Typeface.DEFAULT
                }

                val text = element.text.ifBlank { " " }

                val layout = StaticLayout.Builder.obtain(
                    text,
                    0,
                    text.length,
                    paint,
                    max(1, element.w.toInt())
                )
                    .setAlignment(
                        Layout.Alignment.ALIGN_NORMAL
                    )
                    .setIncludePad(true)
                    .setLineSpacing(2f, 1f)
                    .build()

                canvas.save()
                canvas.clipRect(rect)
                canvas.translate(element.x, element.y)
                layout.draw(canvas)
                canvas.restore()
            }
        }

        private fun drawSelection(
            canvas: Canvas,
            element: NewsElement
        ) {

            val rect = RectF(
                element.x,
                element.y,
                element.x + element.w,
                element.y + element.h
            )

            canvas.drawRect(rect, selectedPaint)

            canvas.drawCircle(
                rect.right,
                rect.bottom,
                17f,
                handlePaint
            )
        }

        // TOUCH / DRAG / RESIZE

        override fun onTouchEvent(event: MotionEvent): Boolean {

            val docX =
                (event.x - offsetX) / scaleFactor

            val docY =
                (event.y - offsetY) / scaleFactor

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {

                    moving = false
                    resizing = false

                    lastX = docX
                    lastY = docY

                    selectedIndex = -1

                    for (i in currentPage().indices.reversed()) {

                        val element = currentPage()[i]

                        val right = element.x + element.w
                        val bottom = element.y + element.h

                        if (
                            docX >= right - 30f &&
                            docX <= right + 30f &&
                            docY >= bottom - 30f &&
                            docY <= bottom + 30f
                        ) {
                            selectedIndex = i
                            resizing = true
                            break
                        }

                        if (
                            docX >= element.x &&
                            docX <= right &&
                            docY >= element.y &&
                            docY <= bottom
                        ) {
                            selectedIndex = i
                            moving = true
                            break
                        }
                    }

                    invalidate()
                    return true
                }

                MotionEvent.ACTION_MOVE -> {

                    if (selectedIndex in currentPage().indices) {

                        val element = currentPage()[selectedIndex]

                        val dx = docX - lastX
                        val dy = docY - lastY

                        if (moving) {

                            element.x = (
                                element.x + dx
                            ).coerceIn(
                                0f,
                                paperWidth - element.w
                            )

                            element.y = (
                                element.y + dy
                            ).coerceIn(
                                if (paperType == "A4") 110f else 175f,
                                paperHeight - 40f
                            )
                        }

                        if (resizing) {

                            element.w = (
                                element.w + dx
                            ).coerceIn(
                                80f,
                                paperWidth - element.x
                            )

                            element.h = (
                                element.h + dy
                            ).coerceIn(
                                50f,
                                paperHeight - element.y - 40f
                            )
                        }

                        lastX = docX
                        lastY = docY

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

    // PDF EXPORT

    private fun exportPdfDialog() {

        val intent = Intent(
            Intent.ACTION_CREATE_DOCUMENT
        ).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"

            putExtra(
                Intent.EXTRA_TITLE,
                "${paperName.replace(" ", "-")}-Newspaper.pdf"
            )
        }

        startActivityForResult(intent, 101)
    }

    // SAVE PDF

    private fun savePdf(uri: Uri) {

        val pdf = PdfDocument()

        try {

            pages.forEachIndexed { index, elements ->

                val pageInfo =
                    PdfDocument.PageInfo.Builder(
                        paperWidth.toInt(),
                        paperHeight.toInt(),
                        index + 1
                    ).create()

                val page = pdf.startPage(pageInfo)

                page.canvas.drawColor(Color.WHITE)

                drawPdfMasthead(
                    page.canvas,
                    index + 1
                )

                elements.forEach { element ->
                    drawPdfElement(
                        page.canvas,
                        element
                    )
                }

                pdf.finishPage(page)
            }

            contentResolver.openOutputStream(uri).use { output ->

                requireNotNull(output) {
                    "PDF ফাইল খোলা যায়নি"
                }

                pdf.writeTo(output)
            }

            toast("সম্পূর্ণ Newspaper PDF তৈরি হয়েছে!")

        } catch (e: Exception) {

            toast("PDF Error: ${e.message}")

        } finally {
            pdf.close()
        }
    }

    // PDF MASTHEAD

    private fun drawPdfMasthead(
        canvas: Canvas,
        number: Int
    ) {

        val a4 = paperType == "A4"
        val imageSize = if (a4) 48f else 105f

        val titlePaint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = mastheadColor
            textSize = if (a4)
                mastheadFontSize * 0.56f
            else
                mastheadFontSize

            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        val metaPaint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = Color.DKGRAY
            textSize = if (a4) 10f else 20f
            textAlign = Paint.Align.CENTER
        }

        val linePaint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = blue
            strokeWidth = if (a4) 3f else 7f
        }

        drawPdfLogo(
            canvas,
            leftLogo,
            RectF(45f, 15f, 45f + imageSize, 15f + imageSize)
        )

        drawPdfLogo(
            canvas,
            rightLogo,
            RectF(
                paperWidth - 45f - imageSize,
                15f,
                paperWidth - 45f,
                15f + imageSize
            )
        )

        canvas.drawText(
            paperName,
            paperWidth / 2f,
            if (a4) 50f else 75f,
            titlePaint
        )

        canvas.drawText(
            paperDate,
            paperWidth / 2f,
            if (a4) 70f else 110f,
            metaPaint
        )

        canvas.drawText(
            paperTagline,
            paperWidth / 2f,
            if (a4) 88f else 145f,
            metaPaint
        )

        canvas.drawLine(
            45f,
            if (a4) 105f else 175f,
            paperWidth - 45f,
            if (a4) 105f else 175f,
            linePaint
        )

        val footer = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = Color.GRAY
            textSize = 15f
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText(
            "পৃষ্ঠা $number",
            paperWidth / 2f,
            paperHeight - 25f,
            footer
        )
    }

    private fun drawPdfLogo(
        canvas: Canvas,
        uri: String,
        rect: RectF
    ) {

        if (uri.isBlank()) return

        val bitmap = getBitmap(uri) ?: return

        canvas.drawBitmap(
            bitmap,
            null,
            rect,
            Paint(
                Paint.ANTI_ALIAS_FLAG or
                Paint.FILTER_BITMAP_FLAG
            )
        )
    }

    // PDF ELEMENT

    private fun drawPdfElement(
        canvas: Canvas,
        element: NewsElement
    ) {

        val rect = RectF(
            element.x,
            element.y,
            element.x + element.w,
            element.y + element.h
        )

        if (element.type == "image") {

            val bitmap = getBitmap(element.imageUri)

            if (bitmap != null) {

                canvas.drawBitmap(
                    bitmap,
                    null,
                    rect,
                    Paint(
                        Paint.ANTI_ALIAS_FLAG or
                        Paint.FILTER_BITMAP_FLAG
                    )
                )
            }

        } else {

            val paint = TextPaint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = element.color
                textSize = element.fontSize

                typeface =
                    if (element.type == "headline")
                        Typeface.DEFAULT_BOLD
                    else
                        Typeface.DEFAULT
            }

            val text = element.text.ifBlank { " " }

            val layout = StaticLayout.Builder.obtain(
                text,
                0,
                text.length,
                paint,
                max(1, element.w.toInt())
            )
                .setAlignment(
                    Layout.Alignment.ALIGN_NORMAL
                )
                .setIncludePad(true)
                .setLineSpacing(2f, 1f)
                .build()

            canvas.save()
            canvas.clipRect(rect)
            canvas.translate(element.x, element.y)
            layout.draw(canvas)
            canvas.restore()
        }
    }

    // TOAST

    private fun toast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}
