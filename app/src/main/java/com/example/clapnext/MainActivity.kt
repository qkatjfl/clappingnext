package com.example.clapnext

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var isRunning = false
    private var sensitivity = 0.12f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStartStop = findViewById<Button>(R.id.btnStartStop)
        val btnPattern = findViewById<Button>(R.id.btnPattern)
        val seekBar = findViewById<SeekBar>(R.id.sensitivitySeekBar)

        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                sensitivity = progress.toFloat() / 100
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnStartStop.setOnClickListener {
            if(isRunning){
                stopService(Intent(this, ClapService::class.java))
                btnStartStop.text = "시작"
            } else {
                val intent = Intent(this, ClapService::class.java)
                intent.putExtra("sensitivity", sensitivity)
                startForegroundService(intent)
                btnStartStop.text = "중지"
            }
            isRunning = !isRunning
        }

        btnPattern.setOnClickListener {
            startActivity(Intent(this, PatternConfigActivity::class.java))
        }
    }
}
