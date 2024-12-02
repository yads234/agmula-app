package com.nervagodz.agmula

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.app.NotificationCompat
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.widget.EditText
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext



class MainActivity : ComponentActivity() {

    private lateinit var currentImageUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_page)

        Toast.makeText(
            applicationContext,
            "Ensure internet is enabled for the best experience",
            Toast.LENGTH_LONG
        ).show()


        // Notification setup
        setupNotification()

        // Setup YouTube Player
        setupYouTubePlayer()

        // Camera and Gallery Buttons
        findViewById<Button>(R.id.cameraButton).setOnClickListener {openCamera() }
        findViewById<Button>(R.id.galleryButton).setOnClickListener {openGallery() }
    }

    private fun setupNotification() {
        val CHANNEL_ID = "ChannelID"
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.gears)
            .setContentTitle("Agmula")
            .setContentText("Agmula is running, please enjoy the app!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
    }
    private fun showAddEventDialog() {
        val dialogView = layoutInflater.inflate(R.layout.add_event_dialog, null)
        val cropNameEditText = dialogView.findViewById<EditText>(R.id.cropNameEditText)
        val plantingDateEditText = dialogView.findViewById<EditText>(R.id.plantingDateEditText)
        val harvestDaysEditText = dialogView.findViewById<EditText>(R.id.harvestDaysEditText)

        AlertDialog.Builder(this)
            .setTitle("Add Event")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val cropName = cropNameEditText.text.toString()
                val plantingDate = plantingDateEditText.text.toString()
                val harvestDays = harvestDaysEditText.text.toString().toIntOrNull() ?: 0

                if (cropName.isNotEmpty() && plantingDate.isNotEmpty()) {
                    scheduleHarvestNotification(cropName, plantingDate, harvestDays)
                    Toast.makeText(this, "Event saved successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun scheduleHarvestNotification(cropName: String, plantingDate: String, harvestDays: Int) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val plantingDateMillis = sdf.parse(plantingDate)?.time ?: return
        val harvestDateMillis = plantingDateMillis + harvestDays * 24 * 60 * 60 * 1000L

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "HarvestChannel")
            .setSmallIcon(R.drawable.gears)
            .setContentTitle("Harvest Reminder")
            .setContentText("$cropName will be ready to harvest soon!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = NotificationManagerCompat.from(this)

        // Ensure NotificationChannel is created for API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "HarvestChannel",
                "Harvest Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val delayMillis = harvestDateMillis - System.currentTimeMillis()
            if (delayMillis > 0) {
                delay(delayMillis)
            }
            withContext(Dispatchers.Main) {
                notificationManager.notify(1, notification)
            }
        }
    }



    private fun setupYouTubePlayer() {
        val youTubePlayerView: YouTubePlayerView = findViewById(R.id.youtube_player_view)
        lifecycle.addObserver(youTubePlayerView)
        youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                val videoId = "5bhcCpNTbC0"
                youTubePlayer.loadVideo(videoId, 0f)
            }
        })
    }
    // Camera functionality
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageUri = result.data?.data
            imageUri?.let { uri ->
                // Process the image (e.g., upload to server)
                currentImageUri = uri
            }
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(cameraIntent)
    }

    // Gallery functionality
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageUri = result.data?.data
            imageUri?.let {
                // Process the image (e.g., upload to server)
                currentImageUri = it
            }
        }
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(galleryIntent)
    }
    // Navigation functions
    fun homePage(view: View) {
        setContentView(R.layout.home_page)
    }

    fun seeCalendar(view: View) {
        setContentView(R.layout.calendar_page)
    }

    fun seeForecast(view: View) {
        setContentView(R.layout.forecast_page)
        val webView = findViewById<WebView>(R.id.forecastView)
        webView.loadUrl("https://weather.com/en-PH/weather/today/l/dd6a0ea2120e46fec2a46e541100ae99ffe70fe5edcffe73af67ddf0980042db")
    }

    fun seeAvailPlants(view: View) {
        setContentView(R.layout.garden_database)
        Toast.makeText(
            applicationContext,
            "Credits: PlantVillage | plantvillage.psu.edu",
            Toast.LENGTH_SHORT
        ).show()
    }

    // Example for plant pages
    fun seeBaguioBeans(view: View) {
        loadPlantInfoPage(R.layout.baguio_beans, R.id.beanView, "https://plantvillage.psu.edu/topics/bean/infos")
    }

    fun seeBroccoli(view: View) {
        loadPlantInfoPage(R.layout.brocolli, R.id.broccoliView, "https://plantvillage.psu.edu/topics/broccoli/infos")
    }

    fun seeCabbage(view: View) {
        loadPlantInfoPage(
            R.layout.cabbage,
            R.id.cabbageView,
            "https://plantvillage.psu.edu/topics/cabbage-red-white-savoy/infos"
        )
    }

    fun seeCarrots(view: View) {
        loadPlantInfoPage(R.layout.carrots, R.id.carrotView, "https://plantvillage.psu.edu/topics/carrot/infos")
    }
    fun seeCauliflower(view: View) {
        setContentView(R.layout.cauliflower)
        val webView = findViewById<WebView>(R.id.cauliView)
        webView.loadUrl("https://plantvillage.psu.edu/topics/cauliflower/infos")
    }

    fun seeLettuce(view: View) {
        setContentView(R.layout.lettuce)
        val webView = findViewById<WebView>(R.id.lettuceView)
        webView.loadUrl("https://plantvillage.psu.edu/topics/lettuce/infos")
    }

    fun seePotato(view: View) {
        setContentView(R.layout.potato)
        val webView = findViewById<WebView>(R.id.potatoView)
        webView.loadUrl("https://plantvillage.psu.edu/topics/potato/infos")
    }

    fun seeSayote(view: View) {
        setContentView(R.layout.sayote)
        val webView = findViewById<WebView>(R.id.sayoteView)
        webView.loadUrl("https://plantvillage.psu.edu/topics/chayote/infos")
    }

    fun seeStrawberry(view: View) {
        setContentView(R.layout.strawberry)
        val webView = findViewById<WebView>(R.id.strawberryView)
        webView.loadUrl("https://plantvillage.psu.edu/topics/strawberry/infos")
    }

    fun showToast(view: View) {
        Toast.makeText(applicationContext, "Hello!", Toast.LENGTH_SHORT).show()
    }
    private fun loadPlantInfoPage(layoutId: Int, webViewId: Int, url: String) {
        setContentView(layoutId)
        val webView = findViewById<WebView>(webViewId)
        webView.loadUrl(url)
    }

    fun openCamera(view: View) {}
    fun openGallery(view: View) {}
}
