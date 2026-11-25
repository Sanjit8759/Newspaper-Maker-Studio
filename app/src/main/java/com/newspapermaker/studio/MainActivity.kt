
package com.newspapermaker.studio

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.MotionEvent
import android.view.View
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import android.graphics.drawable.GradientDrawable
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
    var textColor: Int = Color.BLACK,
    var bold: Boolean = false,
    var alignment: String = "LEFT"
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

    // Newspaper masthead settings

    private var paperName = "ES NEWS"
    private var paperSubtitle = "সংবাদ • মতামত • স্থানীয় খবর"
    private var paperDate = ""

    private var mastheadColor = Color.rgb(25, 43, 67)
    private var mastheadFontSize = 48f
    private var mastheadAlignment = "CENTER"
    private var mastheadLogoUri = ""

    private var galleryTarget = "article"

    private lateinit var editor: NewspaperView
    private lateinit var pageLabel: TextView
    private lateinit var sizeLabel: TextView

    private val navy = Color.rgb(25, 43, 67)
    private val blue = Color.rgb(25, 105, 170)

    private val bitmapCache =
        mutableMapOf<String, Bitmap>()

    private fun currentPage() = pages[pageIndex]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        paperDate = today()

        createInterface()
    }

    private fun today(): String {
        return SimpleDateFormat(
            "dd-MM-yyyy",
            Locale.getDefault()
        ).format(Date())
    }

    // ---------------- MAIN INTERFACE ----------------

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
            openGallery(false)
        }

        tool(toolbar, "✎ সম্পাদনা") {
            editSelected()
        }

        tool(toolbar, "মুছুন") {
            deleteSelected()
        }

        tool(toolbar, "✦ ডিজাইন লেখা") {
            addText("custom")
        }

        val secondScroll = HorizontalScrollView(this)

        val secondToolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(5, 3, 5, 3)
        }

        secondScroll.addView(secondToolbar)
        main.addView(secondScroll)

        tool(secondToolbar, "📰 পত্রিকার নাম") {
            newspaperDesign()
        }

        tool(secondToolbar, "＋ লোগো") {
            openGallery(true)
        }

        tool(secondToolbar, "NEW PAPER") {
            newPaper()
        }

        tool(secondToolbar, "← আগের পৃষ্ঠা") {
            changePage(-1)
        }

        tool(secondToolbar, "＋ নতুন পৃষ্ঠা") {
            pages.add(mutableListOf())
            pageIndex = pages.lastIndex
            selectedIndex = -1
            updateEditor()
        }

        tool(secondToolbar, "পরের পৃষ্ঠা →") {
            changePage(1)
        }

        val thirdScroll = HorizontalScrollView(this)

        val thirdToolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(5, 3, 5, 3)
        }

        thirdScroll.addView(thirdToolbar)
        main.addView(thirdScroll)

        tool(thirdToolbar, "Paper Size") {
            changePaperSize()
        }

        tool(thirdToolbar, "PDF Export") {
            exportPdfDialog()
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
            LinearLayout.LayoutParams(
                -1, 0, 1f
            )
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

            setOnClickListener {
                action()
            }
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

    // ---------------- NEW PAPER ----------------

    private fun newPaper() {

        AlertDialog.Builder(this)
            .setTitle("নতুন পত্রিকা তৈরি করুন")
            .setMessage(
                "পুরোনো পত্রিকার সমস্ত খবর, ছবি ও পৃষ্ঠা মুছে নতুন পত্রিকা শুরু করবেন?\n\nবর্তমান পত্রিকার PDF আগে তৈরি করে রাখুন।"
            )
            .setPositiveButton("হ্যাঁ, নতুন পত্রিকা") { _, _ ->

                pages.clear()
                pages.add(mutableListOf())

                pageIndex = 0
                selectedIndex = -1

                // Keep newspaper branding and paper size.
                // Reset article content only.

                bitmapCache.clear()

                paperDate = today()

                updateEditor()

                toast("নতুন পত্রিকা তৈরি হয়েছে!")

            }
            .setNegativeButton("না", null)
            .show()
    }

    // ---------------- NEWSPAPER NAME DESIGN ----------------

    private fun newspaperDesign() {

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 10, 20, 5)
        }

        fun addLabel(text: String) {
            layout.addView(
                TextView(this).apply {
                    this.text = text
                    textSize = 14f
                    setTextColor(navy)
                    setPadding(0, 10, 0, 4)
                }
            )
        }

        fun addInput(
            value: String,
            hintText: String
        ): EditText {

            val edit = EditText(this).apply {
                setText(value)
                hint = hintText
                textSize = 16f
                setTextColor(Color.BLACK)
                setPadding(10, 8, 10, 8)
            }

            layout.addView(edit)
            return edit
        }

        addLabel("পত্রিকার নাম")

        val nameInput = addInput(
            paperName,
            "নিজের পত্রিকার নাম লিখুন"
        )

        addLabel("পত্রিকার সাবটাইটেল")

        val subtitleInput = addInput(
            paperSubtitle,
            "যেমন: সংবাদ • মতামত • স্থানীয় খবর"
        )

        addLabel("প্রকাশের তারিখ")

        val dateInput = addInput(
            paperDate,
            "তারিখ"
        )

        addLabel("নামের ফন্ট সাইজ")

        val sizeInput = addInput(
            mastheadFontSize.toInt().toString(),
            "যেমন 48"
        )

        addLabel("নামের রং")

        val colors = arrayOf(
            "Navy Blue",
            "Black",
            "Red",
            "Green",
            "Blue",
            "Orange",
            "Purple"
        )

        val colorValues = intArrayOf(
            Color.rgb(25, 43, 67),
            Color.BLACK,
            Color.RED,
            Color.rgb(0, 120, 60),
            Color.BLUE,
            Color.rgb(230, 100, 0),
            Color.rgb(110, 40, 150)
        )

        val colorSpinner = Spinner(this)

        colorSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            colors
        )

        val currentColorIndex =
            colorValues.indexOf(mastheadColor)

        colorSpinner.setSelection(
            currentColorIndex.coerceAtLeast(0)
        )

        layout.addView(colorSpinner)

        addLabel("নামের অবস্থান")

        val alignments = arrayOf(
            "CENTER",
            "LEFT",
            "RIGHT"
        )

        val alignSpinner = Spinner(this)

        alignSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf(
                "মাঝখানে",
                "বাঁদিকে",
                "ডানদিকে"
            )
        )

        alignSpinner.setSelection(
            alignments.indexOf(mastheadAlignment)
                .coerceAtLeast(0)
        )

        layout.addView(alignSpinner)

        AlertDialog.Builder(this)
            .setTitle("Newspaper Name & Design")
            .setView(
                ScrollView(this).apply {
                    addView(layout)
                }
            )
            .setPositiveButton("ডিজাইন সংরক্ষণ") { _, _ ->

                paperName =
                    nameInput.text.toString()
                        .ifBlank { "ES NEWS" }

                paperSubtitle =
                    subtitleInput.text.toString()

                paperDate =
                    dateInput.text.toString()
                        .ifBlank { today() }

                mastheadFontSize =
                    (sizeInput.text.toString()
                        .toFloatOrNull() ?: 48f)
                        .coerceIn(15f, 100f)

                mastheadColor =
                    colorValues[colorSpinner.selectedItemPosition]

                mastheadAlignment =
                    alignments[alignSpinner.selectedItemPosition]

                updateEditor()

                toast("পত্রিকার ডিজাইন সংরক্ষণ হয়েছে")
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    // ---------------- PAPER SIZE ----------------

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

    // ---------------- ADD TEXT ----------------

    private fun addText(type: String) {

        val isHeadline = type == "headline"
        val isCustom = type == "custom"

        val input = EditText(this).apply {

            hint = when (type) {
                "headline" -> "সংবাদের শিরোনাম লিখুন"
                "custom" -> "নিজের পছন্দমতো যেকোনো লেখা লিখুন"
                else -> "সংবাদের বিস্তারিত লিখুন"
            }

            textSize = 17f
            minLines = if (type == "body") 5 else 2

            gravity = Gravity.TOP

            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }

        AlertDialog.Builder(this)
            .setTitle(
                if (isCustom) "নিজের ডিজাইন লেখা"
                else "নতুন খবর তৈরি করুন"
            )
            .setView(input)
            .setPositiveButton("যোগ করুন") { _, _ ->

                val text = input.text.toString()

                if (text.isNotBlank()) {

                    val element = NewsElement(
                        type = type,
                        text = text,
                        x = 55f,
                        y = 220f,
                        w = paperWidth - 110f,
                        h = when {
                            isHeadline -> 180f
                            isCustom -> 120f
                            else -> 360f
                        },
                        fontSize = when {
                            isHeadline -> 38f
                            isCustom -> 34f
                            else -> 23f
                        },
                        textColor = if (isCustom) blue else Color.BLACK,
                        bold = isHeadline || isCustom
                    )

                    currentPage().add(element)

                    selectedIndex =
                        currentPage().lastIndex

                    updateEditor()

                    if (isCustom) {
                        toast("ডিজাইন লেখা যোগ হয়েছে। টেনে যেখানে খুশি রাখুন।")
                    }
                }
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    // ---------------- EDIT SELECTED ----------------

    private fun editSelected() {

        if (selectedIndex !in currentPage().indices) {
            toast("আগে একটি খবর নির্বাচন করুন")
            return
        }

        val element = currentPage()[selectedIndex]

        if (element.type == "image") {
            AlertDialog.Builder(this)
                .setTitle("ছবি সম্পাদনা")
                .setItems(
                    arrayOf(
                        "ছবি পরিবর্তন",
                        "বাতিল"
                    )
                ) { _, which ->
                    if (which == 0) {
                        openGallery(false)
                    }
                }
                .show()
            return
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 10, 20, 5)
        }

        val input = EditText(this).apply {
            setText(element.text)
            textSize = 17f
            minLines = 4
            gravity = Gravity.TOP
            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }

        layout.addView(input)

        layout.addView(
            TextView(this).apply {
                text = "ফন্ট সাইজ"
                setTextColor(navy)
                setPadding(0, 12, 0, 4)
            }
        )

        val fontInput = EditText(this).apply {
            setText(element.fontSize.toInt().toString())
            inputType =
                android.text.InputType.TYPE_CLASS_NUMBER
        }

        layout.addView(fontInput)

        val boldCheck = CheckBox(this).apply {
            text = "Bold লেখা"
            isChecked = element.bold
            setTextColor(navy)
        }

        layout.addView(boldCheck)

        layout.addView(
            TextView(this).apply {
                text = "লেখার রং"
                setTextColor(navy)
                setPadding(0, 10, 0, 4)
            }
        )

        val colorNames = arrayOf(
            "Black",
            "Blue",
            "Red",
            "Green",
            "Orange",
            "Purple"
        )

        val colorValues = intArrayOf(
            Color.BLACK,
            Color.BLUE,
            Color.RED,
            Color.rgb(0, 120, 60),
            Color.rgb(230, 100, 0),
            Color.rgb(110, 40, 150)
        )

        val colorSpinner = Spinner(this)

        colorSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            colorNames
        )

        colorSpinner.setSelection(
            colorValues.indexOf(element.textColor)
                .coerceAtLeast(0)
        )

        layout.addView(colorSpinner)

        val alignSpinner = Spinner(this)

        alignSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("বাঁদিকে", "মাঝখানে", "ডানদিকে")
        )

        val alignments = arrayOf(
            "LEFT",
            "CENTER",
            "RIGHT"
        )

        alignSpinner.setSelection(
            alignments.indexOf(element.alignment)
                .coerceAtLeast(0)
        )

        layout.addView(
            TextView(this).apply {
                text = "লেখার অবস্থান"
                setTextColor(navy)
                setPadding(0, 10, 0, 4)
            }
        )

        layout.addView(alignSpinner)

        AlertDialog.Builder(this)
            .setTitle("লেখা ও ডিজাইন সম্পাদনা")
            .setView(
                ScrollView(this).apply {
                    addView(layout)
                }
            )
            .setPositiveButton("সংরক্ষণ") { _, _ ->

                element.text = input.text.toString()

                element.fontSize =
                    (fontInput.text.toString()
                        .toFloatOrNull() ?: 24f)
                        .coerceIn(8f, 100f)

                element.bold = boldCheck.isChecked

                element.textColor =
                    colorValues[colorSpinner.selectedItemPosition]

                element.alignment =
                    alignments[alignSpinner.selectedItemPosition]

                updateEditor()
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    private fun deleteSelected() {

        if (selectedIndex in currentPage().indices) {

            currentPage().removeAt(selectedIndex)

            selectedIndex = -1

            updateEditor()

        } else {
            toast("কোনো খবর নির্বাচন করা হয়নি")
        }
    }

    // ---------------- IMAGE GALLERY ----------------

    private fun openGallery(isLogo: Boolean) {

        galleryTarget =
            if (isLogo) "logo" else "article"

        val intent = Intent(
            Intent.ACTION_OPEN_DOCUMENT
        ).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivityForResult(intent, 200)
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

        if (requestCode == 200) {

            val uri = data?.data ?: return

            try {

                val flags = data.flags and
                    Intent.FLAG_GRANT_READ_URI_PERMISSION

                if (flags != 0) {
                    contentResolver.takePersistableUriPermission(
                        uri, flags
                    )
                }

                if (galleryTarget == "logo") {

                    mastheadLogoUri = uri.toString()

                    bitmapCache.remove(mastheadLogoUri)

                    updateEditor()

                    toast("পত্রিকার লোগো যোগ হয়েছে")

                } else {

                    val element = NewsElement(
                        type = "image",
                        imageUri = uri.toString(),
                        x = 55f,
                        y = 220f,
                        w = paperWidth * 0.55f,
                        h = paperHeight * 0.20f
                    )

                    currentPage().add(element)

                    selectedIndex =
                        currentPage().lastIndex

                    updateEditor()

                }

            } catch (e: Exception) {
                toast("ছবি যোগ করা যায়নি: ${e.message}")
            }
        }

        if (requestCode == 101) {
            data?.data?.let {
                savePdf(it)
            }
        }
    }

    // ---------------- NEWSPAPER DESIGN VIEW ----------------

    inner class NewspaperView :
        View(this@MainActivity) {

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
            style = Paint.Style.FILL
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

        // ---------------- MASTHEAD ----------------

        private fun drawMasthead(
            canvas: Canvas,
            number: Int
        ) {

            val isA4 = paperType == "A4"

            val titlePaint = Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = mastheadColor
                textSize = if (isA4) {
                    mastheadFontSize * 0.56f
                } else {
                    mastheadFontSize
                }
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }

            val metaPaint = Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = Color.DKGRAY
                textSize = if (isA4) 10f else 20f
                textAlign = Paint.Align.CENTER
            }

            val titleY =
                if (isA4) 50f else 75f

            val dateY =
                if (isA4) 70f else 110f

            val subtitleY =
                if (isA4) 88f else 145f

            val lineY =
                if (isA4) 105f else 175f

            val x = when (mastheadAlignment) {
                "LEFT" -> 220f
                "RIGHT" -> paperWidth - 220f
                else -> paperWidth / 2f
            }

            // Newspaper logo

            if (mastheadLogoUri.isNotBlank()) {

                val logo = getBitmap(mastheadLogoUri)

                if (logo != null) {

                    val logoSize =
                        if (isA4) 65f else 120f

                    val logoRect = RectF(
                        45f,
                        25f,
                        45f + logoSize,
                        25f + logoSize
                    )

                    canvas.drawBitmap(
                        logo,
                        null,
                        logoRect,
                        Paint(
                            Paint.ANTI_ALIAS_FLAG or
                                Paint.FILTER_BITMAP_FLAG
                        )
                    )
                }
            }

            canvas.drawText(
                paperName,
                x,
                titleY,
                titlePaint
            )

            canvas.drawText(
                paperDate,
                paperWidth / 2f,
                dateY,
                metaPaint
            )

            canvas.drawText(
                paperSubtitle,
                paperWidth / 2f,
                subtitleY,
                metaPaint
            )

            val linePaint = Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = blue
                strokeWidth = if (isA4) 3f else 7f
            }

            canvas.drawLine(
                45f,
                lineY,
                paperWidth - 45f,
                lineY,
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

        // ---------------- DRAW ELEMENT ----------------

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

                val textPaint = TextPaint(
                    Paint.ANTI_ALIAS_FLAG
                ).apply {
                    color = element.textColor
                    textSize = element.fontSize

                    typeface = if (element.bold) {
                        Typeface.DEFAULT_BOLD
                    } else {
                        Typeface.DEFAULT
                    }
                }

                val alignment = when (element.alignment) {
                    "CENTER" ->
                        Layout.Alignment.ALIGN_CENTER

                    "RIGHT" ->
                        Layout.Alignment.ALIGN_OPPOSITE

                    else ->
                        Layout.Alignment.ALIGN_NORMAL
                }

                val safeText =
                    element.text.ifBlank { " " }

                val layout = StaticLayout.Builder.obtain(
                    safeText,
                    0,
                    safeText.length,
                    textPaint,
                    max(1, element.w.toInt())
                )
                    .setAlignment(alignment)
                    .setIncludePad(true)
                    .setLineSpacing(2f, 1f)
                    .build()

                canvas.save()

                canvas.clipRect(rect)

                canvas.translate(
                    element.x,
                    element.y
                )

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

        // ---------------- IMAGE CACHE ----------------

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

        // ---------------- TOUCH / DRAG / RESIZE ----------------

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
                                175f,
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

    // ---------------- PDF EXPORT ----------------

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

    // ---------------- PDF MASTHEAD ----------------

    private fun drawPdfMasthead(
        canvas: Canvas,
        number: Int
    ) {

        val isA4 = paperType == "A4"

        val titlePaint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = mastheadColor
            textSize = if (isA4) {
                mastheadFontSize * 0.56f
            } else {
                mastheadFontSize
            }
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        val metaPaint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = Color.DKGRAY
            textSize = if (isA4) 10f else 20f
            textAlign = Paint.Align.CENTER
        }

        val titleY = if (isA4) 50f else 75f
        val dateY = if (isA4) 70f else 110f
        val subtitleY = if (isA4) 88f else 145f
        val lineY = if (isA4) 105f else 175f

        val x = when (mastheadAlignment) {
            "LEFT" -> 220f
            "RIGHT" -> paperWidth - 220f
            else -> paperWidth / 2f
        }

        // Logo

        if (mastheadLogoUri.isNotBlank()) {

            val bitmap = getPdfBitmap(mastheadLogoUri)

            if (bitmap != null) {

                val logoSize = if (isA4) 65f else 120f

                canvas.drawBitmap(
                    bitmap,
                    null,
                    RectF(
                        45f,
                        25f,
                        45f + logoSize,
                        25f + logoSize
                    ),
                    Paint(
                        Paint.ANTI_ALIAS_FLAG or
                            Paint.FILTER_BITMAP_FLAG
                    )
                )
            }
        }

        canvas.drawText(
            paperName,
            x,
            titleY,
            titlePaint
        )

        canvas.drawText(
            paperDate,
            paperWidth / 2f,
            dateY,
            metaPaint
        )

        canvas.drawText(
            paperSubtitle,
            paperWidth / 2f,
            subtitleY,
            metaPaint
        )

        val linePaint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = blue
            strokeWidth = if (isA4) 3f else 7f
        }

        canvas.drawLine(
            45f,
            lineY,
            paperWidth - 45f,
            lineY,
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

    // ---------------- PDF ELEMENT ----------------

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

            val bitmap = getPdfBitmap(element.imageUri)

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
                color = element.textColor
                textSize = element.fontSize

                typeface = if (element.bold) {
                    Typeface.DEFAULT_BOLD
                } else {
                    Typeface.DEFAULT
                }
            }

            val alignment = when (element.alignment) {
                "CENTER" ->
                    Layout.Alignment.ALIGN_CENTER

                "RIGHT" ->
                    Layout.Alignment.ALIGN_OPPOSITE

                else ->
                    Layout.Alignment.ALIGN_NORMAL
            }

            val text = element.text.ifBlank { " " }

            val layout = StaticLayout.Builder.obtain(
                text,
                0,
                text.length,
                paint,
                max(1, element.w.toInt())
            )
                .setAlignment(alignment)
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

    private fun getPdfBitmap(uri: String): Bitmap? {

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

    // ---------------- MESSAGE ----------------

    private fun toast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}
