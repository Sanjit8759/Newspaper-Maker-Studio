
package com.newspapermaker.studio

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*

data class NewsArticle(
    var title: String = "",
    var description: String = "",
    var imageUri: String = ""
)

data class NewsPage(
    val articles: MutableList<NewsArticle> = mutableListOf(
        NewsArticle()
    )
)

class MainActivity : Activity() {

    private lateinit var root: LinearLayout
    private lateinit var paperName: EditText
    private lateinit var paperDate: EditText
    private lateinit var articleTitle: EditText
    private lateinit var articleBody: EditText
    private lateinit var pageInfo: TextView
    private lateinit var articleInfo: TextView
    private lateinit var imagePreview: ImageView

    private val pages = mutableListOf(NewsPage())

    private var pageIndex = 0
    private var articleIndex = 0
    private var selectedImage = ""

    private val navy = Color.rgb(25, 43, 67)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createScreen()
        loadArticle()
    }

    private fun createScreen() {

        val scroll = ScrollView(this)

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 24)
            setBackgroundColor(Color.rgb(242, 244, 248))
        }

        scroll.addView(root)
        setContentView(scroll)

        val header = TextView(this).apply {
            text = "NEWSPAPER MAKER STUDIO"
            textSize = 21f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(12, 24, 12, 24)
            setBackgroundColor(navy)
        }

        root.addView(header)

        addLabel("পত্রিকার নাম")
        paperName = addInput("আপনার পত্রিকার নাম")

        addLabel("প্রকাশের তারিখ")
        paperDate = addInput("তারিখ লিখুন")

        pageInfo = addInfo()
        articleInfo = addInfo()

        root.addView(pageInfo)
        root.addView(articleInfo)

        addLabel("সংবাদের শিরোনাম")
        articleTitle = addInput("খবরের শিরোনাম লিখুন")

        addLabel("সংবাদের বিস্তারিত")
        articleBody = addInput(
            "এখানে সম্পূর্ণ সংবাদ লিখুন...",
            true
        )

        addLabel("সংবাদের ছবি")

        imagePreview = ImageView(this).apply {
            setBackgroundColor(Color.LTGRAY)
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "সংবাদের ছবি"
        }

        root.addView(
            imagePreview,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                180
            )
        )

        addButton("📷 গ্যালারি থেকে ছবি নির্বাচন করুন") {
            openGallery()
        }

        addButton("ছবি সরিয়ে দিন") {
            selectedImage = ""
            imagePreview.setImageDrawable(null)
            imagePreview.setBackgroundColor(Color.LTGRAY)
            saveArticle()
        }

        addButton("＋ একই পৃষ্ঠায় নতুন খবর যোগ করুন") {
            saveArticle()
            currentPage().articles.add(NewsArticle())
            articleIndex = currentPage().articles.lastIndex
            loadArticle()
        }

        addButton("আগের খবর") {
            saveArticle()

            if (articleIndex > 0) {
                articleIndex--
                loadArticle()
            } else {
                message("এটাই প্রথম খবর")
            }
        }

        addButton("পরের খবর") {
            saveArticle()

            if (articleIndex < currentPage().articles.lastIndex) {
                articleIndex++
                loadArticle()
            } else {
                message("নতুন খবর যোগ করুন")
            }
        }

        addButton("বর্তমান খবর মুছে ফেলুন") {
            val list = currentPage().articles

            if (list.size > 1) {
                list.removeAt(articleIndex)
                articleIndex = articleIndex.coerceAtMost(
                    list.lastIndex
                )
                loadArticle()
            } else {
                message("অন্তত একটি খবর রাখতে হবে")
            }
        }

        addButton("＋ নতুন পৃষ্ঠা তৈরি করুন") {
            saveArticle()
            pages.add(NewsPage())
            pageIndex = pages.lastIndex
            articleIndex = 0
            loadArticle()
        }

        addButton("← আগের পৃষ্ঠা") {
            saveArticle()

            if (pageIndex > 0) {
                pageIndex--
                articleIndex = 0
                loadArticle()
            } else {
                message("এটাই প্রথম পৃষ্ঠা")
            }
        }

        addButton("পরের পৃষ্ঠা →") {
            saveArticle()

            if (pageIndex < pages.lastIndex) {
                pageIndex++
                articleIndex = 0
                loadArticle()
            } else {
                message("নতুন পৃষ্ঠা তৈরি করুন")
            }
        }

        addButton("📄 সম্পূর্ণ পত্রিকা PDF তৈরি করুন") {
            saveArticle()
            createPdf()
        }
    }

    private fun currentPage(): NewsPage {
        return pages[pageIndex]
    }

    private fun currentArticle(): NewsArticle {
        return currentPage().articles[articleIndex]
    }

    private fun addLabel(value: String) {
        root.addView(
            TextView(this).apply {
                text = value
                textSize = 16f
                setTextColor(navy)
                setPadding(4, 14, 4, 5)
            }
        )
    }

    private fun addInfo(): TextView {
        return TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(navy)
            setPadding(8, 12, 8, 12)
        }
    }

    private fun addInput(
        hintText: String,
        multiline: Boolean = false
    ): EditText {

        val edit = EditText(this).apply {
            hint = hintText
            textSize = 16f
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)
            setPadding(14, 12, 14, 12)
            setBackgroundColor(Color.WHITE)

            if (multiline) {
                minLines = 8
                gravity = Gravity.TOP
                inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            }
        }

        root.addView(
            edit,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        return edit
    }

    private fun addButton(
        label: String,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            text = label
            textSize = 14f
            setTextColor(Color.WHITE)
            setBackgroundColor(navy)
            setOnClickListener { action() }
        }

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 6, 0, 6)
        root.addView(button, params)
    }

    private fun saveArticle() {
        val article = currentArticle()

        article.title = articleTitle.text.toString()
        article.description = articleBody.text.toString()
        article.imageUri = selectedImage
    }

    private fun loadArticle() {

        val article = currentArticle()

        articleTitle.setText(article.title)
        articleBody.setText(article.description)

        selectedImage = article.imageUri

        imagePreview.setImageDrawable(null)
        imagePreview.setBackgroundColor(Color.LTGRAY)

        if (selectedImage.isNotEmpty()) {
            try {
                val uri = Uri.parse(selectedImage)
                val bitmap = contentResolver.openInputStream(uri).use {
                    BitmapFactory.decodeStream(it)
                }

                if (bitmap != null) {
                    imagePreview.setImageBitmap(bitmap)
                }
            } catch (_: Exception) {
                message("ছবিটি পাওয়া যাচ্ছে না")
            }
        }

        pageInfo.text =
            "পৃষ্ঠা ${pageIndex + 1} / ${pages.size}"

        articleInfo.text =
            "খবর ${articleIndex + 1} / ${currentPage().articles.size}"
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        startActivityForResult(intent, 200)
    }

    @Deprecated("Uses document picker result")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 200 && resultCode == RESULT_OK) {

            val uri = data?.data ?: return

            try {
                val flags = data.flags and
                    Intent.FLAG_GRANT_READ_URI_PERMISSION

                contentResolver.takePersistableUriPermission(
                    uri,
                    flags
                )

                selectedImage = uri.toString()
                imagePreview.setImageURI(uri)
                saveArticle()

            } catch (e: Exception) {
                message("ছবি নির্বাচন করা যায়নি: ${e.message}")
            }
        }

        if (requestCode == 101 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            exportPdf(uri)
        }
    }

    private fun createPdf() {

        val intent = Intent(
            Intent.ACTION_CREATE_DOCUMENT
        ).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(
                Intent.EXTRA_TITLE,
                "Newspaper.pdf"
            )
        }

        startActivityForResult(intent, 101)
    }

    private fun exportPdf(uri: Uri) {

        val pdf = PdfDocument()

        var pdfPageNumber = 0
        var canvas: Canvas
        var y = 0f

        fun startPdfPage(): Canvas {

            pdfPageNumber++

            val info = PdfDocument.PageInfo.Builder(
                595,
                842,
                pdfPageNumber
            ).create()

            val page = pdf.startPage(info)

            val c = page.canvas
            c.drawColor(Color.WHITE)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(
                    "sans-serif",
                    Typeface.BOLD
                )
                textSize = 26f
            }

            val name = paperName.text.toString()
                .ifBlank { "আমার সংবাদপত্র" }

            c.drawText(name, 297f, 42f, paint)

            paint.textSize = 12f
            paint.typeface = Typeface.DEFAULT

            c.drawText(
                paperDate.text.toString(),
                297f,
                65f,
                paint
            )

            paint.strokeWidth = 2f

            c.drawLine(
                30f, 80f, 565f, 80f, paint
            )

            y = 100f

            return c
        }

        fun finishPdfPage() {
            pdf.finishPage(
                pdf.getPage(pdfPageNumber)
            )
        }

        try {

            canvas = startPdfPage()

            for (page in pages) {

                for (article in page.articles) {

                    val titlePaint = TextPaint(
                        Paint.ANTI_ALIAS_FLAG
                    ).apply {
                        color = Color.BLACK
                        textSize = 19f
                        typeface = Typeface.create(
                            "sans-serif",
                            Typeface.BOLD
                        )
                    }

                    val title = article.title.ifBlank {
                        "সংবাদের শিরোনাম"
                    }

                    val titleLayout =
                        StaticLayout.Builder.obtain(
                            title,
                            0,
                            title.length,
                            titlePaint,
                            525
                        )
                        .setAlignment(
                            Layout.Alignment.ALIGN_NORMAL
                        )
                        .setIncludePad(true)
                        .build()

                    val bodyPaint = TextPaint(
                        Paint.ANTI_ALIAS_FLAG
                    ).apply {
                        color = Color.BLACK
                        textSize = 13f
                    }

                    val body = article.description

                    val bodyLayout =
                        StaticLayout.Builder.obtain(
                            body,
                            0,
                            body.length,
                            bodyPaint,
                            525
                        )
                        .setAlignment(
                            Layout.Alignment.ALIGN_NORMAL
                        )
                        .setIncludePad(true)
                        .setLineSpacing(2f, 1f)
                        .build()

                    var bitmap: Bitmap? = null

                    if (article.imageUri.isNotEmpty()) {
                        try {
                            bitmap = contentResolver
                                .openInputStream(
                                    Uri.parse(article.imageUri)
                                ).use {
                                    BitmapFactory.decodeStream(it)
                                }
                        } catch (_: Exception) {
                            bitmap = null
                        }
                    }

                    val imageHeight =
                        if (bitmap != null) 105f else 0f

                    val blockHeight =
                        titleLayout.height +
                        imageHeight +
                        bodyLayout.height + 30f

                    if (y + blockHeight > 800f) {
                        finishPdfPage()
                        canvas = startPdfPage()
                    }

                    canvas.save()
                    canvas.translate(35f, y)
                    titleLayout.draw(canvas)
                    canvas.restore()

                    y += titleLayout.height + 8f

                    if (bitmap != null) {

                        val rect = RectF(
                            35f,
                            y,
                            180f,
                            y + 100f
                        )

                        val imagePaint = Paint(
                            Paint.ANTI_ALIAS_FLAG or
                                Paint.FILTER_BITMAP_FLAG
                        )

                        canvas.drawBitmap(
                            bitmap,
                            null,
                            rect,
                            imagePaint
                        )

                        y += 108f
                    }

                    canvas.save()
                    canvas.translate(35f, y)
                    bodyLayout.draw(canvas)
                    canvas.restore()

                    y += bodyLayout.height + 18f

                    val linePaint = Paint().apply {
                        color = Color.LTGRAY
                        strokeWidth = 1f
                    }

                    canvas.drawLine(
                        35f, y, 560f, y, linePaint
                    )

                    y += 15f
                }
            }

            finishPdfPage()

            contentResolver.openOutputStream(uri).use {
                output ->
                if (output == null) {
                    throw Exception("PDF ফাইল খোলা যায়নি")
                }
                pdf.writeTo(output)
            }

            pdf.close()

            message("সম্পূর্ণ Newspaper PDF তৈরি হয়েছে!")

        } catch (e: Exception) {

            pdf.close()
            message("PDF Error: ${e.message}")
        }
    }

    private fun message(text: String) {
        Toast.makeText(
            this,
            text,
            Toast.LENGTH_LONG
        ).show()
    }
}
