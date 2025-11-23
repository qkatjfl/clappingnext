package com.example.clapnext

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlin.math.sqrt

class ClapService : Service() {
    private var isRunning = true
    private var sensitivity = 0.12f
    private val clapTimes = mutableListOf<Long>()
    private var userCount = 3
    private var userSpeed = "fast"
    private var userAction = "next"
    private val maxFastInterval = 800L
    private val maxSlowInterval = 1500L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sensitivity = intent?.getFloatExtra("sensitivity", 0.12f) ?: 0.12f
        val prefs = getSharedPreferences("patterns", MODE_PRIVATE)
        userCount = prefs.getInt("count", 3)
        userSpeed = prefs.getString("speed", "fast")!!
        userAction = prefs.getString("action", "next")!!

        val channelId = "clapnext_channel"
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(channelId, "ClapNext", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ClapNext")
            .setContentText("박수 감지 중 👏")
            .setSmallIcon(R.drawable.ic_notification)
            .setOnlyAlertOnce(true)
            .setColor(0xFF007AFF.toInt())
            .build()
        startForeground(1, notification)

        Thread { detectClap() }.start()
        return START_STICKY
    }

    private fun detectClap(){
        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        val buffer = ShortArray(bufferSize)
        recorder.startRecording()

        var sum = 0f
        repeat(30){
            val read = recorder.read(buffer, 0, buffer.size)
            sum += calculateRMS(buffer, read)
            Thread.sleep(100)
        }
        val autoThreshold = sum / 30
        if(autoThreshold > sensitivity) sensitivity = autoThreshold * 1.5f

        while(isRunning){
            val read = recorder.read(buffer, 0, buffer.size)
            val rms = calculateRMS(buffer, read)
            val now = System.currentTimeMillis()
            if(rms > sensitivity){
                clapTimes.add(now)
                matchUserPattern()
            }
        }
        recorder.stop()
        recorder.release()
    }

    private fun calculateRMS(buffer: ShortArray, read: Int): Float {
        var sum = 0.0
        for(i in 0 until read) sum += (buffer[i]*buffer[i]).toDouble()
        return sqrt(sum/read).toFloat() / 32768f
    }

    private fun matchUserPattern(){
        val now = System.currentTimeMillis()
        clapTimes.removeAll { now - it > 3000 }

        if(clapTimes.size >= userCount){
            val intervals = clapTimes.takeLast(userCount).zipWithNext { a,b -> b-a }
            val maxInterval = if(userSpeed=="fast") maxFastInterval else maxSlowInterval
            if(intervals.all { it <= maxInterval }){
                when(userAction){
                    "next" -> nextTrack()
                    "restart" -> restartTrack()
                }
                clapTimes.clear()
            }
        }
    }

    private fun nextTrack(){
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val down = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
        val up = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
        audioManager.dispatchMediaKeyEvent(down)
        audioManager.dispatchMediaKeyEvent(up)
    }

    private fun restartTrack(){
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val down = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        val up = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        audioManager.dispatchMediaKeyEvent(down)
        audioManager.dispatchMediaKeyEvent(up)
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { isRunning = false; super.onDestroy() }
}
