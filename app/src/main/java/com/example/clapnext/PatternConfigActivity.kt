package com.example.clapnext

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity

class PatternConfigActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pattern_config)

        val btnSave = findViewById<Button>(R.id.btnSave)
        val editCount = findViewById<EditText>(R.id.editCount)
        val radioSpeed = findViewById<RadioGroup>(R.id.radioSpeed)
        val radioAction = findViewById<RadioGroup>(R.id.radioAction)

        btnSave.setOnClickListener {
            val count = editCount.text.toString().toIntOrNull() ?: 2
            val speed = if(radioSpeed.checkedRadioButtonId == R.id.radioFast) "fast" else "slow"
            val action = when(radioAction.checkedRadioButtonId){
                R.id.radioNext -> "next"
                R.id.radioRestart -> "restart"
                else -> "next"
            }

            val prefs = getSharedPreferences("patterns", MODE_PRIVATE)
            prefs.edit().putInt("count", count)
                .putString("speed", speed)
                .putString("action", action)
                .apply()

            finish()
        }
    }
}
