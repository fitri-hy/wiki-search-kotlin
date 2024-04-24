package com.hytech.wikisearch

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner

class MainActivity : AppCompatActivity() {

    private lateinit var editTextQuery: EditText
    private lateinit var spinnerLanguage: Spinner
    private lateinit var buttonSearch: Button
    private lateinit var scrollView: ScrollView
    private lateinit var container: LinearLayout
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editTextQuery = findViewById(R.id.edit_text_query)
        spinnerLanguage = findViewById(R.id.spinner_language)
        buttonSearch = findViewById(R.id.button_search)
        scrollView = findViewById(R.id.scroll_view)
        container = findViewById(R.id.container)
        progressBar = findViewById(R.id.progress_bar)

        buttonSearch.setOnClickListener {
            val langName = spinnerLanguage.selectedItem.toString()
            val langId = getLanguageId(langName)
            val query = editTextQuery.text.toString()
            FetchWikiData().execute(langId, query)
        }
    }

    private fun getLanguageId(langName: String): String {
        return when (langName) {
            "Indonesia" -> "id"
            "Inggris" -> "en"
            else -> "en"
        }
    }

    inner class FetchWikiData : AsyncTask<String, Void, JSONObject>() {

        override fun onPreExecute() {
            super.onPreExecute()
            progressBar.visibility = View.VISIBLE
        }

        override fun doInBackground(vararg params: String?): JSONObject {
            val langId = params[0]
            val query = params[1]

            val url = URL("https://api.hy-tech.my.id/api/wiki/$langId/$query")
            val connection = url.openConnection() as HttpURLConnection

            try {
                val inputStream = BufferedInputStream(connection.inputStream)
                val response = convertStreamToString(inputStream)
                return JSONObject(response)
            } catch (e: Exception) {
                return JSONObject().put("error", "Data not available, Look for another one.")
            } finally {
                connection.disconnect()
            }
        }

        override fun onPostExecute(result: JSONObject?) {
            super.onPostExecute(result)
            progressBar.visibility = View.GONE
            if (result != null) {
                if (result.has("error")) {
                    Toast.makeText(this@MainActivity, result.getString("error"), Toast.LENGTH_SHORT).show()
                } else {
                    val title = result.optString("title")
                    if (title.isNotEmpty()) {
                        val thumbnailUrl = result.getJSONObject("thumbnail").optString("source")
                        val extract = result.optString("extract")
                        val contentUrl = result.getJSONObject("content_urls").getJSONObject("desktop").optString("page")

                        addSearchResult(title, thumbnailUrl, extract, contentUrl)
                    } else {
                        Toast.makeText(this@MainActivity, "Data not available", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this@MainActivity, "Failed to retrieve data", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun addSearchResult(title: String, thumbnailUrl: String, description: String, contentUrl: String) {
        val cardView = layoutInflater.inflate(R.layout.result_item, null) as LinearLayout

        val imageViewThumbnail = cardView.findViewById<ImageView>(R.id.image_view_thumbnail)
        val textViewTitle = cardView.findViewById<TextView>(R.id.text_view_title)
        val textViewDescription = cardView.findViewById<TextView>(R.id.text_view_description)
        val buttonOpenContent = cardView.findViewById<Button>(R.id.button_open_content)

        textViewTitle.text = title
        textViewDescription.text = description

        buttonOpenContent.setOnClickListener {
            openContentPage(contentUrl)
        }

        DownloadImageTask(imageViewThumbnail).execute(thumbnailUrl)

        container.addView(cardView, 0)
    }

    private fun openContentPage(contentUrl: String) {
        val url = contentUrl
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        startActivity(intent)
    }

    private fun convertStreamToString(inputStream: InputStream): String {
        val scanner = Scanner(inputStream).useDelimiter("\\A")
        return if (scanner.hasNext()) scanner.next() else ""
    }

    inner class DownloadImageTask(private val imageView: ImageView) : AsyncTask<String, Void, Bitmap>() {

        override fun doInBackground(vararg urls: String): Bitmap? {
            val url = urls[0]
            var bitmap: Bitmap? = null
            try {
                val inputStream: InputStream = URL(url).openStream()
                bitmap = BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return bitmap
        }

        override fun onPostExecute(result: Bitmap?) {
            if (result != null) {
                imageView.setImageBitmap(result)
            }
        }
    }
}