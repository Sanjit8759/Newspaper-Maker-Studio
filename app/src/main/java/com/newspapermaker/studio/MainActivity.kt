
package com.newspapermaker.studio

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import android.graphics.Color

data class NewsPage(
    var headline: String = "",
    var content: String = ""
)

class MainActivity : Activity() {

    private lateinit var mainLayout: LinearLayout
    private lateinit var paperName: EditText
    private lateinit var paperDate: EditText
    private lateinit var headline: EditText
    private lateinit var content: EditText
    private lateinit var pageInfo: TextView

    private val pages = mutableListOf(NewsPage())
    private var currentPage = 0

    private val BLUE = Color.rgb(25, 43, 67)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createEditor()
        loadPage()
    }

    private fun createEditor() {

        val scroll = ScrollView(this)

        mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 18)
            setBackgroundColor(Color.rgb(245, 246, 249))
        }

        scroll.addView(mainLayout)
        setContentView(scroll)

        val title = TextView(this).apply {
            text = "NEWSPAPER MAKER STUDIO"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(12, 22, 12, 22)
            setBackgroundColor(BLUE)
        }

        mainLayout.addView(title)

        addLabel("পত্রিকার নাম")
        paperName = addInput("আপনার সংবাদপত্রের নাম")

        addLabel("প্রকাশের তারিখ")
        paperDate = addInput("তারিখ লিখুন")

        addLabel("সংবাদের শিরোনাম")
        headline = addInput("এখানে বড় হেডলাইন লিখুন")

        addLabel("সম্পূর্ণ সংবাদ")
        content = addInput("এখানে বিস্তারিত সংবাদ লিখুন...", true)

        pageInfo = TextView(this).apply {
            textSize = 17f
            setTextColor(BLUE)
            gravity = Gravity.CENTER
            setPadding(10, 18, 10, 18)
        }

        mainLayout.addView(pageInfo)

        addButton("＋ নতুন পৃষ্ঠা যোগ করুন") {
            savePage()
            pages.add(NewsPage())
            currentPage = pages.lastIndex
            loadPage()
        }

        addButton("← আগের পৃষ্ঠা") {
            if (currentPage > 0) {
                savePage()
                currentPage--
                loadPage()
            } else {
                toast("এটাই প্রথম পৃষ্ঠা")
            }
        }

        addButton("পরের পৃষ্ঠা →") {
            if (currentPage < pages.lastIndex) {
                savePage()
                currentPage++
                loadPage()
            } else {
                toast("নতুন পৃষ্ঠা যোগ করুন")
            }
        }

        addButton("📄 PDF তৈরি করুন") {
            savePage()
            createPdf()
        }
    }

    private fun addLabel(text: String) {
        val label = TextView(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(BLUE)
            setPadding(4, 15, 4, 5)
        }
        mainLayout.addView(label)
    }

    private fun addInput(
        hintText: String,
        multiline: Boolean = false
    ): EditText {

        return EditText(this).apply {
            hint = hintText
            textSize = 16f
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)
            setPadding(14, 12, 14, 12)
            setBackgroundColor(Color.WHITE)

            if (multiline) {
                minLines = 8
                gravity = Gravity.TOP
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            }

            mainLayout.addView(
                this,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun addButton(
        text: String,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(Color.WHITE)
            setBackgroundColor(BLUE)
            setOnClickListener { action() }
        }

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 8, 0, 8)

        mainLayout.addView(button, params)
    }

    private fun savePage() {
        pages[currentPage] = NewsPage(
            headline.text.toString(),
            content.text.toString()
        )
    }

    private fun loadPage() {
        val page = pages[currentPage]

        headline.setText(page.headline)
        content.setText(page.content)

        pageInfo.text =
            "পৃষ্ঠা ${currentPage + 1} / ${pages.size}"
    }

    private fun createPdf() {

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(
                Intent.EXTRA_TITLE,
                "Newspaper.pdf"
            )
        }

        startActivityForResult(intent, 101)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101 && resultCode == RESULT_OK) {

            val uri = data?.data ?: return

            try {
                val pdf = PdfDocument()

                pages.forEachIndexed { index, page ->

                    val pageInfoPdf =
                        PdfDocument.PageInfo.Builder(
                            595, 842, index + 1
                        ).create()

                    val pdfPage = pdf.startPage(pageInfoPdf)
                    val canvas = pdfPage.canvas

                    drawNewsPage(
                        canvas,
                        page,
                        index + 1
                    )

                    pdf.finishPage(pdfPage)
                }

                contentResolver.openOutputStream(uri)?.use {
                    pdf.writeTo(it)
                } ?: throw Exception("PDF ফাইল খোলা যায়নি")

                pdf.close()

                toast("PDF সফলভাবে তৈরি হয়েছে!")

            } catch (e: Exception) {
                toast("PDF Error: ${e.message}")
            }
        }
    }

    private fun drawNewsPage(
        canvas: Canvas,
        page: NewsPage,
        pageNumber: Int
    ) {

        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.BLACK
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(
            "sans-serif",
            Typeface.BOLD
        )
        paint.textSize = 28f

        val name = paperName.text.toString()
        val date = paperDate.text.toString()

        canvas.drawText(
            name,
            297f,
            48f,
            paint
        )

        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT

        canvas.drawText(
            date,
            297f,
            72f,
            paint
        )

        paint.strokeWidth = 2f
        canvas.drawLine(
            30f, 85f, 565f, 85f, paint
        )

        val headlinePaint = TextPaint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = Color.BLACK
            textSize = 24f
            typeface = Typeface.create(
                "sans-serif",
                Typeface.BOLD
            )
        }

        val headlineLayout = StaticLayout.Builder
            .obtain(
                page.headline,
                0,
                page.headline.length,
                headlinePaint,
                525
            )
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(true)
            .build()

        canvas.save()
        canvas.translate(35f, 105f)
        headlineLayout.draw(canvas)
        canvas.restore()

        val bodyPaint = TextPaint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = Color.BLACK
            textSize = 15f
            typeface = Typeface.create(
                "sans-serif",
                Typeface.NORMAL
            )
        }

        val bodyLayout = StaticLayout.Builder
            .obtain(
                page.content,
                0,
                page.content.length,
                bodyPaint,
                525
            )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(3f, 1f)
            .setIncludePad(true)
            .build()

        canvas.save()
        canvas.translate(35f, 180f)
        bodyLayout.draw(canvas)
        canvas.restore()

        paint.textSize = 11f

        canvas.drawText(
            "পৃষ্ঠা $pageNumber",
            297f,
            820f,
            paint
        )
    }

    private fun toast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}
