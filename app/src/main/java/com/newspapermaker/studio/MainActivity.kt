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

private data class Item(
    var kind: String = "text", // text, image, box, logo
    var text: String = "",
    var uri: String = "",
    var x: Float = 40f, var y: Float = 200f,
    var w: Float = 500f, var h: Float = 180f,
    var font: Float = 28f,
    var color: Int = Color.BLACK,
    var fill: Int = Color.TRANSPARENT,
    var stroke: Int = Color.rgb(25,105,170)
)

class MainActivity : Activity() {
    private val items = mutableListOf<Item>()
    private var selected = -1
    private var paperW = 1224f
    private var paperH = 1584f
    private var paperSize = "NEWSPAPER"
    private var paperName = "ES NEWS AND ENTERTAINMENT"
    private var tagline = "সংবাদ • মতামত • স্থানীয় খবর"
    private var dateText = ""
    private var mastBg = Color.WHITE
    private var mastText = Color.rgb(25,43,67)
    private var pageBgUri = ""
    private var mastBgUri = ""
    private val cache = mutableMapOf<String, Bitmap>()
    private lateinit var preview: PaperView
    private lateinit var pageInfo: TextView
    private val blue = Color.rgb(25,105,170)
    private val navy = Color.rgb(25,43,67)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dateText = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xffe5eaf0.toInt()) }
        val head = TextView(this).apply { text = "NEWSPAPER MAKER STUDIO"; textSize = 20f; gravity = Gravity.CENTER; setTextColor(-1); setTypeface(null, 1); setPadding(8,14,8,14); setBackgroundColor(navy) }
        root.addView(head)
        toolbar(root, arrayOf("＋ শিরোনাম", "＋ বিস্তারিত", "📷 ছবি", "＋ Box", "Grid 2 কলাম", "সম্পাদনা", "মুছুন")) { i ->
            when(i) { 0 -> addText(true); 1 -> addText(false); 2 -> chooseImage(10); 3 -> addBox(); 4 -> addTwoColumnGrid(); 5 -> editSelected(); 6 -> deleteSelected() }
        }
        toolbar(root, arrayOf("আগের পৃষ্ঠা", "＋ নতুন পৃষ্ঠা", "পরের পৃষ্ঠা", "Paper Size", "PDF Export", "Print")) { i ->
            when(i) { 0 -> toast("এখন একটি পৃষ্ঠা রয়েছে"); 1 -> toast("একাধিক পৃষ্ঠা ফিচার পরের ধাপে যুক্ত হবে"); 2 -> toast("এখন একটি পৃষ্ঠা রয়েছে"); 3 -> choosePaper(); 4 -> exportPdf(); 5 -> toast("PDF Export করে প্রিন্ট করুন") }
        }
        toolbar(root, arrayOf("পত্রিকার নাম ও ডিজাইন", "Masthead Background", "পৃষ্ঠা Background", "Masthead ছবি", "লোগো যোগ", "Design Save", "Design Open")) { i ->
            when(i) { 0 -> editMasthead(); 1 -> chooseMastColor(); 2 -> chooseImage(20); 3 -> chooseImage(21); 4 -> chooseImage(22); 5 -> saveDesign(); 6 -> openDesign() }
        }
        val info = LinearLayout(this).apply { setPadding(10,7,10,7); setBackgroundColor(-1); gravity = Gravity.CENTER_VERTICAL }
        pageInfo = TextView(this).apply { text = "পৃষ্ঠা 1 / 1"; textSize = 14f; setTextColor(navy) }
        info.addView(pageInfo, LinearLayout.LayoutParams(0,-2,1f))
        info.addView(TextView(this).apply { text = paperSize; setTextColor(blue); textSize = 14f })
        root.addView(info)
        preview = PaperView()
        root.addView(preview, LinearLayout.LayoutParams(-1,0,1f))
        root.addView(TextView(this).apply { text = "খবর/ছবি/লোগো টাচ করে নির্বাচন করুন • টেনে সরান • লাল গোল অংশ টেনে ছোট-বড় করুন"; gravity = Gravity.CENTER; textSize = 11f; setTextColor(Color.DKGRAY); setPadding(5,8,5,8); setBackgroundColor(-1) })
        setContentView(root)
    }

    private fun toolbar(root: LinearLayout, labels: Array<String>, action: (Int)->Unit) {
        val scroll = HorizontalScrollView(this)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(3,2,3,2) }
        labels.forEachIndexed { index, label ->
            val b = Button(this).apply { text = label; textSize = 12f; isAllCaps = false; setTextColor(-1); setPadding(8,0,8,0); background = GradientDrawable().apply { setColor(blue); cornerRadius = 12f }; setOnClickListener { action(index) } }
            row.addView(b, LinearLayout.LayoutParams(-2,44).apply { setMargins(3,2,3,2) })
        }
        scroll.addView(row); root.addView(scroll)
    }

    private fun addText(headline: Boolean) {
        val input = EditText(this).apply { hint = if(headline) "সংবাদের শিরোনাম" else "সংবাদের বিস্তারিত"; minLines = if(headline) 2 else 5; gravity = Gravity.TOP }
        AlertDialog.Builder(this).setTitle(if(headline) "শিরোনাম যোগ" else "বিস্তারিত যোগ").setView(input)
            .setPositiveButton("যোগ করুন") { _,_ -> if(input.text.isNotBlank()) { items.add(Item(kind="text", text=input.text.toString(), x=55f,y=if(headline) 205f else 400f,w=paperW-110f,h=if(headline) 130f else 300f,font=if(headline) 42f else 25f,color=if(headline) Color.BLACK else 0xff222222.toInt())); selected=items.lastIndex; refresh() } }.setNegativeButton("বাতিল",null).show()
    }

    private fun addBox() {
        items.add(Item(kind="box", x=55f,y=230f,w=paperW-110f,h=300f,fill=Color.WHITE,stroke=blue)); selected=items.lastIndex; refresh()
    }

    private fun addTwoColumnGrid() {
        val gap = 18f; val left = (paperW-110f-gap)/2f
        items.add(Item(kind="box",x=55f,y=220f,w=left,h=430f,fill=0xfff8fbff.toInt(),stroke=blue))
        items.add(Item(kind="box",x=55f+left+gap,y=220f,w=left,h=430f,fill=0xfff8fbff.toInt(),stroke=blue))
        selected=items.lastIndex; refresh(); toast("দুটি কলামের Box তৈরি হয়েছে")
    }

    private fun editSelected() {
        if(selected !in items.indices) { toast("আগে একটি লেখা বা Box নির্বাচন করুন"); return }
        val item=items[selected]
        if(item.kind=="image" || item.kind=="logo") { chooseImage(if(item.kind=="logo") 22 else 10); return }
        val input=EditText(this).apply { setText(item.text); minLines=4; gravity=Gravity.TOP }
        AlertDialog.Builder(this).setTitle("সম্পাদনা").setView(input).setPositiveButton("সংরক্ষণ") { _,_ -> item.text=input.text.toString(); refresh() }.setNegativeButton("বাতিল",null).show()
    }

    private fun deleteSelected() { if(selected in items.indices) { items.removeAt(selected); selected=-1; refresh() } else toast("কোনো উপাদান নির্বাচন করা হয়নি") }

    private fun editMasthead() {
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(20,8,20,4) }
        val name=EditText(this).apply { hint="পত্রিকার নাম"; setText(paperName) }
        val tag=EditText(this).apply { hint="স্লোগান"; setText(tagline) }
        val date=EditText(this).apply { hint="তারিখ"; setText(dateText) }
        box.addView(name); box.addView(tag); box.addView(date)
        AlertDialog.Builder(this).setTitle("Masthead ডিজাইন").setView(box).setPositiveButton("সংরক্ষণ") { _,_ -> paperName=name.text.toString(); tagline=tag.text.toString(); dateText=date.text.toString(); refresh() }.setNegativeButton("বাতিল",null).show()
    }

    private fun chooseMastColor() {
        val labels=arrayOf("সাদা","হালকা নীল","নীল","লাল","কালো","কমলা","সবুজ","গোলাপি")
        val colors=intArrayOf(Color.WHITE,0xffe3f2fd.toInt(),Color.BLUE,Color.RED,Color.BLACK,0xffff9800.toInt(),Color.GREEN,0xffffc1dc.toInt())
        AlertDialog.Builder(this).setTitle("Masthead Background").setItems(labels) { _,i -> mastBg=colors[i]; refresh() }.show()
    }

    private fun choosePaper() {
        AlertDialog.Builder(this).setTitle("Paper Size").setItems(arrayOf("Newspaper Broadsheet","A4")) { _,i -> if(i==0){paperW=1224f;paperH=1584f;paperSize="NEWSPAPER"}else{paperW=595f;paperH=842f;paperSize="A4"}; refresh() }.show()
    }

    private fun chooseImage(code:Int) {
        val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type="image/*"; addCategory(Intent.CATEGORY_OPENABLE); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        startActivityForResult(intent,code)
    }

    private fun saveDesign() {
        val intent=Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type="application/json"; putExtra(Intent.EXTRA_TITLE,"Newspaper-Design.json") }
        startActivityForResult(intent,40)
    }

    private fun openDesign() {
        val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type="application/json" }
        startActivityForResult(intent,30)
    }

    private fun loadDesign(uri:Uri) {
        try {
            val json=contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
            val root=JSONObject(json); paperName=root.optString("paperName","ES NEWS"); tagline=root.optString("tagline",""); dateText=root.optString("date","")
            paperW=root.optDouble("paperW",1224.0).toFloat(); paperH=root.optDouble("paperH",1584.0).toFloat(); paperSize=root.optString("paperSize","NEWSPAPER")
            mastBg=root.optInt("mastBg",Color.WHITE); mastText=root.optInt("mastText",navy); pageBgUri=root.optString("pageBgUri",""); mastBgUri=root.optString("mastBgUri","")
            items.clear(); val arr=root.getJSONArray("items")
            for(i in 0 until arr.length()) { val o=arr.getJSONObject(i); items.add(Item(o.optString("kind"),o.optString("text"),o.optString("uri"),o.optDouble("x").toFloat(),o.optDouble("y").toFloat(),o.optDouble("w").toFloat(),o.optDouble("h").toFloat(),o.optDouble("font",28.0).toFloat(),o.optInt("color",Color.BLACK),o.optInt("fill",Color.TRANSPARENT),o.optInt("stroke",blue))) }
            selected=-1; refresh(); toast("Design Open হয়েছে; এখন Edit করতে পারবেন")
        } catch(e:Exception) { toast("Design খোলা যায়নি: ${e.message}") }
    }

    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        if(requestCode==40 && resultCode==RESULT_OK && data?.data!=null) { saveDesignTo(data.data!!); return }
        if(requestCode==31 && resultCode==RESULT_OK && data?.data!=null) { savePdf(data.data!!); return }
        super.onActivityResult(requestCode,resultCode,data)
        if(resultCode!=RESULT_OK || data?.data==null) return
        val uri=data.data!!
        try { val f=data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION; if(f!=0) contentResolver.takePersistableUriPermission(uri,f) } catch(_:Exception) {}
        when(requestCode) { 10 -> { if(selected in items.indices && items[selected].kind=="image") items[selected].uri=uri.toString() else { items.add(Item(kind="image",uri=uri.toString(),x=55f,y=220f,w=paperW*.55f,h=paperH*.22f));selected=items.lastIndex };refresh() };20->{pageBgUri=uri.toString();refresh()};21->{mastBgUri=uri.toString();refresh()};22->{items.add(Item(kind="logo",uri=uri.toString(),x=paperW*.7f,y=20f,w=100f,h=100f));selected=items.lastIndex;refresh()};30->loadDesign(uri) }
    }

    private fun saveDesignTo(uri:Uri) {
        try {
            val root=JSONObject().put("paperName",paperName).put("tagline",tagline).put("date",dateText).put("paperW",paperW).put("paperH",paperH).put("paperSize",paperSize).put("mastBg",mastBg).put("mastText",mastText).put("pageBgUri",pageBgUri).put("mastBgUri",mastBgUri)
            val arr=JSONArray(); items.forEach { e -> arr.put(JSONObject().put("kind",e.kind).put("text",e.text).put("uri",e.uri).put("x",e.x).put("y",e.y).put("w",e.w).put("h",e.h).put("font",e.font).put("color",e.color).put("fill",e.fill).put("stroke",e.stroke)) }; root.put("items",arr)
            contentResolver.openOutputStream(uri)!!.bufferedWriter().use { it.write(root.toString(2)) }; toast("Design Save হয়েছে")
        } catch(e:Exception) { toast("Save Error: ${e.message}") }
    }

    private fun exportPdf() { val i=Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE);type="application/pdf";putExtra(Intent.EXTRA_TITLE,"Newspaper.pdf") }; startActivityForResult(i,31) }
    private fun savePdf(uri:Uri) {
        val pdf=PdfDocument(); try { val info=PdfDocument.PageInfo.Builder(paperW.toInt(),paperH.toInt(),1).create(); val page=pdf.startPage(info); drawPage(page.canvas); pdf.finishPage(page); contentResolver.openOutputStream(uri)!!.use { pdf.writeTo(it) }; toast("PDF তৈরি হয়েছে") } catch(e:Exception){toast("PDF Error: ${e.message}")} finally{pdf.close()}
    }

    private fun bitmap(uri:String):Bitmap? { if(uri.isBlank()) return null; cache[uri]?.let{return it}; return try { contentResolver.openInputStream(Uri.parse(uri)).use { BitmapFactory.decodeStream(it) }?.also{cache[uri]=it} } catch(_:Exception){null} }
    private fun drawPage(canvas:Canvas) {
        canvas.drawColor(Color.WHITE)
        bitmap(pageBgUri)?.let { canvas.drawBitmap(it,null,RectF(0f,0f,paperW,paperH),Paint(3)) }
        val mh=if(paperSize=="A4") 105f else 175f
        val bg=Paint(3).apply{color=mastBg}; canvas.drawRect(0f,0f,paperW,mh,bg)
        bitmap(mastBgUri)?.let{canvas.drawBitmap(it,null,RectF(0f,0f,paperW,mh),Paint(3))}
        val title=Paint(3).apply{color=mastText;textAlign=Paint.Align.CENTER;textSize=if(paperSize=="A4")30f else 48f;typeface=Typeface.create("sans-serif-condensed",Typeface.BOLD)}
        canvas.drawText(paperName,paperW/2f,if(paperSize=="A4")48f else 75f,title)
        val meta=Paint(3).apply{color=Color.DKGRAY;textAlign=Paint.Align.CENTER;textSize=if(paperSize=="A4")10f else 20f}
        canvas.drawText(dateText,paperW/2f,if(paperSize=="A4")69f else 110f,meta); canvas.drawText(tagline,paperW/2f,if(paperSize=="A4")87f else 145f,meta)
        val line=Paint(3).apply{color=blue;strokeWidth=if(paperSize=="A4")3f else 7f};canvas.drawLine(35f,mh,paperW-35f,mh,line)
        items.forEach { drawItem(canvas,it) }
    }

    private fun drawItem(canvas:Canvas,e:Item) {
        val r=RectF(e.x,e.y,e.x+e.w,e.y+e.h)
        when(e.kind) {
            "image","logo" -> bitmap(e.uri)?.let{canvas.drawBitmap(it,null,r,Paint(3))}
            "box" -> { val p=Paint(3).apply{color=e.fill;style=Paint.Style.FILL};canvas.drawRect(r,p); val s=Paint(3).apply{color=e.stroke;style=Paint.Style.STROKE;strokeWidth=3f};canvas.drawRect(r,s) }
            else -> { val p=TextPaint(3).apply{color=e.color;textSize=e.font;typeface=Typeface.create("sans-serif",if(e.font>34f) Typeface.BOLD else Typeface.NORMAL)}; val t=e.text.ifBlank{" "}; val layout=StaticLayout.Builder.obtain(t,0,t.length,p,e.w.toInt().coerceAtLeast(1)).setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(true).build();canvas.save();canvas.clipRect(r);canvas.translate(e.x,e.y);layout.draw(canvas);canvas.restore() }
        }
    }

    private inner class PaperView:View(this@MainActivity) {
        private var scale=1f; private var ox=0f; private var oy=0f; private var lastX=0f; private var lastY=0f; private var moving=false; private var resizing=false
        override fun onDraw(c:Canvas) { super.onDraw(c); c.drawColor(0xffdce2e9.toInt()); scale=min(width/paperW,height/paperH).coerceAtLeast(.01f);ox=(width-paperW*scale)/2;oy=(height-paperH*scale)/2;c.save();c.translate(ox,oy);c.scale(scale,scale);drawPage(c);if(selected in items.indices){val e=items[selected];val r=RectF(e.x,e.y,e.x+e.w,e.y+e.h);val p=Paint(3).apply{color=Color.BLUE;style=Paint.Style.STROKE;strokeWidth=4f};c.drawRect(r,p);c.drawCircle(r.right,r.bottom,17f,Paint(3).apply{color=Color.RED})};val border=Paint(3).apply{color=blue;style=Paint.Style.STROKE;strokeWidth=3f};c.drawRect(1f,1f,paperW-1,paperH-1,border);c.restore() }
        override fun onTouchEvent(ev:MotionEvent):Boolean { val x=(ev.x-ox)/scale;val y=(ev.y-oy)/scale;when(ev.action){MotionEvent.ACTION_DOWN->{lastX=x;lastY=y;moving=false;resizing=false;selected=-1;for(i in items.indices.reversed()){val e=items[i];if(x in (e.x+e.w-30)..(e.x+e.w+30)&&y in (e.y+e.h-30)..(e.y+e.h+30)){selected=i;resizing=true;break};if(x>=e.x&&x<=e.x+e.w&&y>=e.y&&y<=e.y+e.h){selected=i;moving=true;break}};invalidate();return true};MotionEvent.ACTION_MOVE->{if(selected in items.indices){val e=items[selected];val dx=x-lastX;val dy=y-lastY;if(moving){e.x=(e.x+dx).coerceIn(0f,paperW-e.w);e.y=(e.y+dy).coerceIn(0f,paperH-e.h)};if(resizing){e.w=(e.w+dx).coerceIn(60f,paperW-e.x);e.h=(e.h+dy).coerceIn(50f,paperH-e.y)};lastX=x;lastY=y;invalidate()};return true};MotionEvent.ACTION_UP->{moving=false;resizing=false;invalidate();return true}};return true }
    }

    private fun refresh(){pageInfo.text="পৃষ্ঠা 1 / 1";preview.invalidate()}
    private fun toast(s:String){Toast.makeText(this,s,Toast.LENGTH_LONG).show()}
}
