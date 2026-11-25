
package com.newspapermaker.studio

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.min

data class NewsArticle(
    var title: String = "",
    var body: String = "",
    var image: String = ""
)

data class NewsPage(
    val articles: MutableList<NewsArticle> =
        mutableListOf(NewsArticle())
)

class MainActivity : Activity() {

    private val pages = mutableListOf(NewsPage())

    private var pageIndex = 0
    private var articleIndex = 0
    private var selectedImage = ""

    private lateinit var root: LinearLayout
    private lateinit var paperName: EditText
    private lateinit var paperDate: EditText
    private lateinit var titleInput: EditText
    private lateinit var bodyInput: EditText
    private lateinit var imagePreview: ImageView
    private lateinit var pageInfo: TextView
    private lateinit var articleInfo: TextView

    private val navy = Color.rgb(25, 43, 67)
    private val blue = Color.rgb(25, 105, 170)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createEditor()
        loadArticle()
    }

    private fun createEditor() {

        val scroll = ScrollView(this)

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 24)
            setBackgroundColor(
                Color.rgb(243, 246, 250)
            )
        }

        scroll.addView(root)
        setContentView(scroll)

        val header = TextView(this).apply {
            text = "NEWSPAPER MAKER STUDIO\nআধুনিক বাংলা সংবাদপত্র"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(12, 25, 12, 25)
            background = rounded(navy)
        }

        root.addView(header)

        label("পত্রিকার নাম")

        paperName = input("পত্রিকার নাম লিখুন")
        paperName.setText("ES NEWS")

        label("প্রকাশের তারিখ")

        paperDate = input("তারিখ")
        paperDate.setText(
            SimpleDateFormat(
                "dd-MM-yyyy",
                Locale.getDefault()
            ).format(Date())
        )

        label("পৃষ্ঠা ও খবর")

        pageInfo = info()
        articleInfo = info()

        root.addView(pageInfo)
        root.addView(articleInfo)

        label("সংবাদের শিরোনাম")

        titleInput = input(
            "বড় করে সংবাদের শিরোনাম লিখুন"
        )

        label("সংবাদের বিস্তারিত")

        bodyInput = input(
            "সম্পূর্ণ সংবাদ এখানে লিখুন",
            true
        )

        label("সংবাদের ছবি")

        imagePreview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(Color.LTGRAY)
            contentDescription = "সংবাদের ছবি"
        }

        root.addView(
            imagePreview,
            LinearLayout.LayoutParams(-1, 190)
        )

        button("📷 গ্যালারি থেকে ছবি নিন") {
            openGallery()
        }

        button("ছবি সরিয়ে দিন") {
            selectedImage = ""
            imagePreview.setImageDrawable(null)
            saveArticle()
        }

        label("খবর পরিচালনা")

        button("＋ একই পৃষ্ঠায় নতুন খবর") {
            saveArticle()

            currentPage().articles.add(
                NewsArticle()
            )

            articleIndex =
                currentPage().articles.lastIndex

            loadArticle()
        }

        button("← আগের খবর") {
            saveArticle()

            if (articleIndex > 0) {
                articleIndex--
                loadArticle()
            } else {
                toast("এটাই প্রথম খবর")
            }
        }

        button("পরের খবর →") {
            saveArticle()

            if (articleIndex <
                currentPage().articles.lastIndex
            ) {
                articleIndex++
                loadArticle()
            } else {
                toast("নতুন খবর যোগ করুন")
            }
        }

        button("বর্তমান খবর মুছুন") {
            val list = currentPage().articles

            if (list.size > 1) {
                list.removeAt(articleIndex)

                articleIndex =
                    articleIndex.coerceAtMost(
                        list.lastIndex
                    )

                loadArticle()
            } else {
                toast("অন্তত একটি খবর রাখতে হবে")
            }
        }

        label("পৃষ্ঠা পরিচালনা")

        button("＋ নতুন পৃষ্ঠা") {
            saveArticle()

            pages.add(NewsPage())

            pageIndex = pages.lastIndex
            articleIndex = 0

            loadArticle()
        }

        button("← আগের পৃষ্ঠা") {
            saveArticle()

            if (pageIndex > 0) {
                pageIndex--
                articleIndex = 0
                loadArticle()
            } else {
                toast("এটাই প্রথম পৃষ্ঠা")
            }
        }

        button("পরের পৃষ্ঠা →") {
            saveArticle()

            if (pageIndex < pages.lastIndex) {
                pageIndex++
                articleIndex = 0
                loadArticle()
            } else {
                toast("নতুন পৃষ্ঠা তৈরি করুন")
            }
        }

        label("Newspaper PDF")

        button("📄 সম্পূর্ণ পত্রিকা PDF তৈরি করুন") {
            saveArticle()
            createPdf()
        }
    }

    private fun rounded(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = 16f
        }
    }

    private fun label(text: String) {
        root.addView(
            TextView(this).apply {
                this.text = text
                textSize = 16f
                setTextColor(navy)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(4, 18, 4, 7)
            }
        )
    }

    private fun info(): TextView {
        return TextView(this).apply {
            textSize = 14f
            setTextColor(blue)
            setPadding(12, 10, 12, 10)
            background = rounded(Color.WHITE)
        }
    }

    private fun input(
        hintText: String,
        multiline: Boolean = false
    ): EditText {

        val edit = EditText(this).apply {
            hint = hintText
            textSize = 15f
            setTextColor(Color.BLACK)
            setPadding(12, 12, 12, 12)
            background = rounded(Color.WHITE)

            if (multiline) {
                minLines = 7
                gravity = Gravity.TOP

                inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            }
        }

        root.addView(edit)
        return edit
    }

    private fun button(
        text: String,
        action: () -> Unit
    ) {

        val b = Button(this).apply {
            this.text = text
            isAllCaps = false
            textSize = 14f
            setTextColor(Color.WHITE)
            background = rounded(blue)
            setOnClickListener {
                action()
            }
        }

        root.addView(
            b,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 5, 0, 5)
            }
        )
    }

    private fun currentPage() =
        pages[pageIndex]

    private fun currentArticle() =
        currentPage().articles[articleIndex]

    private fun saveArticle() {

        if (!::titleInput.isInitialized) return

        currentArticle().apply {
            title = titleInput.text.toString()
            body = bodyInput.text.toString()
            image = selectedImage
        }
    }

    private fun loadArticle() {

        val article = currentArticle()

        titleInput.setText(article.title)
        bodyInput.setText(article.body)

        selectedImage = article.image

        imagePreview.setImageDrawable(null)

        if (selectedImage.isNotEmpty()) {
            try {
                imagePreview.setImageURI(
                    Uri.parse(selectedImage)
                )
            } catch (_: Exception) {
                selectedImage = ""
            }
        }

        pageInfo.text =
            "পৃষ্ঠা ${pageIndex + 1} / ${pages.size}"

        articleInfo.text =
            "এই পাতার খবর ${articleIndex + 1} / " +
            "${currentPage().articles.size}"
    }

    private fun openGallery() {

        val intent = Intent(
            Intent.ACTION_OPEN_DOCUMENT
        ).apply {
            type = "image/*"
            addCategory(
                Intent.CATEGORY_OPENABLE
            )
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
                    contentResolver
                        .takePersistableUriPermission(
                            uri,
                            flags
                        )
                }

                selectedImage = uri.toString()
                imagePreview.setImageURI(uri)
                saveArticle()

            } catch (_: Exception) {
                toast("ছবি নির্বাচন করা যায়নি")
            }
        }

        if (requestCode == 101) {
            data?.data?.let {
                exportPdf(it)
            }
        }
    }

    private fun createPdf() {

        val intent = Intent(
            Intent.ACTION_CREATE_DOCUMENT
        ).apply {
            addCategory(
                Intent.CATEGORY_OPENABLE
            )

            type = "application/pdf"

            putExtra(
                Intent.EXTRA_TITLE,
                "ES-NEWS-Newspaper.pdf"
            )
        }

        startActivityForResult(intent, 101)
    }

    private fun exportPdf(uri: Uri) {

        val pdf = PdfDocument()

        // BROADSHEET NEWSPAPER
        // 17 x 22 inches at 72 PDF points/inch

        val pageWidth = 1224
        val pageHeight = 1584

        val margin = 55f
        val gap = 28f
        val columns = 3

        val columnWidth =
            (pageWidth - margin * 2 -
                gap * (columns - 1)) / columns

        val top = 190f
        val bottom = 1510f

        val headingPaint = TextPaint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = Color.BLACK
            textSize = 25f
            typeface = Typeface.create(
                "sans-serif",
                Typeface.BOLD
            )
        }

        val bodyPaint = TextPaint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = Color.rgb(35, 35, 35)
            textSize = 17f
            typeface = Typeface.create(
                "sans-serif",
                Typeface.NORMAL
            )
        }

        val metaPaint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = Color.DKGRAY
            textSize = 18f
            textAlign = Paint.Align.CENTER
        }

        val mastheadPaint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = navy
            textSize = 48f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        var currentPage: PdfDocument.Page? = null
        var canvas: Canvas? = null

        var physicalPage = 0
        var column = 0
        var y = top

        fun xPosition(): Float {
            return margin +
                column * (columnWidth + gap)
        }

        fun startPage() {

            physicalPage++

            val pageInfo =
                PdfDocument.PageInfo.Builder(
                    pageWidth,
                    pageHeight,
                    physicalPage
                ).create()

            currentPage = pdf.startPage(pageInfo)
            canvas = currentPage!!.canvas

            canvas!!.drawColor(Color.WHITE)

            canvas!!.drawText(
                paperName.text.toString().ifBlank {
                    "ES NEWS"
                },
                pageWidth / 2f,
                65f,
                mastheadPaint
            )

            canvas!!.drawText(
                paperDate.text.toString(),
                pageWidth / 2f,
                100f,
                metaPaint
            )

            canvas!!.drawText(
                "সংবাদ • মতামত • স্থানীয় খবর",
                pageWidth / 2f,
                130f,
                metaPaint
            )

            val linePaint = Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = blue
                strokeWidth = 6f
            }

            canvas!!.drawLine(
                margin,
                155f,
                pageWidth - margin,
                155f,
                linePaint
            )

            column = 0
            y = top

            val pageNumberPaint = Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = Color.GRAY
                textSize = 15f
                textAlign = Paint.Align.CENTER
            }

            canvas!!.drawText(
                "পৃষ্ঠা $physicalPage",
                pageWidth / 2f,
                1540f,
                pageNumberPaint
            )
        }

        fun finishPage() {
            currentPage?.let {
                pdf.finishPage(it)
            }

            currentPage = null
            canvas = null
        }

        fun nextColumn() {

            if (column < columns - 1) {
                column++
                y = top
            } else {
                finishPage()
                startPage()
            }
        }

        fun newManualPage() {
            if (currentPage != null) {
                finishPage()
            }
            startPage()
        }

        fun makeLayout(
            text: String,
            paint: TextPaint
        ): StaticLayout {

            val safeText = text.ifBlank {
                " "
            }

            return StaticLayout.Builder.obtain(
                safeText,
                0,
                safeText.length,
                paint,
                columnWidth.toInt()
            )
                .setAlignment(
                    Layout.Alignment.ALIGN_NORMAL
                )
                .setIncludePad(true)
                .setLineSpacing(3f, 1f)
                .build()
        }

        // Draws Bengali text line by line.
        // If a column fills, continues in the next column.

        fun drawTextFlow(layout: StaticLayout) {

            for (i in 0 until layout.lineCount) {

                val lineTop =
                    layout.getLineTop(i).toFloat()

                val lineHeight = (
                    layout.getLineBottom(i) -
                    layout.getLineTop(i)
                ).toFloat().coerceAtLeast(1f)

                if (y + lineHeight > bottom) {
                    nextColumn()
                }

                val x = xPosition()
                val currentY = y

                canvas!!.save()

                canvas!!.clipRect(
                    x,
                    currentY,
                    x + columnWidth,
                    currentY + lineHeight + 2f
                )

                canvas!!.translate(
                    x,
                    currentY - lineTop
                )

                layout.draw(canvas!!)

                canvas!!.restore()

                y += lineHeight
            }
        }

        fun drawArticle(article: NewsArticle) {

            if (article.title.isBlank() &&
                article.body.isBlank() &&
                article.image.isBlank()
            ) return

            val titleLayout = makeLayout(
                article.title.ifBlank {
                    "সংবাদের শিরোনাম"
                },
                headingPaint
            )

            val bodyLayout = makeLayout(
                article.body,
                bodyPaint
            )

            var bitmap: Bitmap? = null

            if (article.image.isNotBlank()) {
                try {
                    bitmap = contentResolver
                        .openInputStream(
                            Uri.parse(article.image)
                        ).use {
                            BitmapFactory.decodeStream(it)
                        }
                } catch (_: Exception) {
                    bitmap = null
                }
            }

            var imageWidth = 0f
            var imageHeight = 0f

            bitmap?.let {
                val scale = min(
                    columnWidth / it.width.toFloat(),
                    240f / it.height.toFloat()
                )

                imageWidth = it.width * scale
                imageHeight = it.height * scale
            }

            val requiredSpace =
                titleLayout.height +
                imageHeight +
                50f

            if (requiredSpace < bottom - top &&
                y + requiredSpace > bottom
            ) {
                nextColumn()
            }

            // Headline

            drawTextFlow(titleLayout)

            y += 12f

            // News photograph

            bitmap?.let { image ->

                if (y + imageHeight > bottom) {
                    nextColumn()
                }

                val x = xPosition() +
                    (columnWidth - imageWidth) / 2f

                val rect = RectF(
                    x,
                    y,
                    x + imageWidth,
                    y + imageHeight
                )

                canvas!!.drawBitmap(
                    image,
                    null,
                    rect,
                    Paint(
                        Paint.ANTI_ALIAS_FLAG or
                        Paint.FILTER_BITMAP_FLAG
                    )
                )

                y += imageHeight + 12f

                image.recycle()
            }

            // News details

            drawTextFlow(bodyLayout)

            y += 16f

            // Article separator

            if (y + 10f < bottom) {

                val divider = Paint(
                    Paint.ANTI_ALIAS_FLAG
                ).apply {
                    color = Color.LTGRAY
                    strokeWidth = 1.5f
                }

                canvas!!.drawLine(
                    xPosition(),
                    y,
                    xPosition() + columnWidth,
                    y,
                    divider
                )

                y += 20f
            }
        }

        try {

            startPage()

            pages.forEachIndexed { index, newsPage ->

                // Each manually created newspaper page
                // begins on a fresh PDF page.

                if (index > 0) {
                    newManualPage()
                }

                newsPage.articles.forEach { article ->
                    drawArticle(article)
                }
            }

            finishPage()

            contentResolver.openOutputStream(uri).use {
                output ->

                requireNotNull(output) {
                    "PDF ফাইল খোলা যায়নি"
                }

                pdf.writeTo(output)
            }

            pdf.close()

            toast("সম্পূর্ণ Newspaper PDF তৈরি হয়েছে!")

        } catch (e: Exception) {

            try {
                finishPage()
                pdf.close()
            } catch (_: Exception) {
            }

            toast(
                "PDF Error: ${e.message}"
            )
        }
    }

    private fun toast(text: String) {
        Toast.makeText(
            this,
            text,
            Toast.LENGTH_LONG
        ).show()
    }
}
