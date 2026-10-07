package com.recreated.clonemaster.ui

import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import com.recreated.clonemaster.R

/**
 * Recreated Free VIP screen (original: fragment_free_vip.xml).
 * Original: coins earned from rewarded ads, VIP plan radio group, redeem codes.
 * Recreation: local demo store of coins (SharedPreferences), plans, and redeem code check.
 */
class VipActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences("vip", MODE_PRIVATE) }
    private val plans = listOf(
        "1 day  ·  100 coins",
        "7 days ·  500 coins",
        "30 days · 1800 coins",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vip)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val coin = findViewById<TextView>(R.id.coin)
        val status = findViewById<TextView>(R.id.vipStatus)
        val group = findViewById<RadioGroup>(R.id.vipGroup)

        fun render() {
            coin.text = prefs.getInt("coins", 0).toString()
            val until = prefs.getLong("vip_until", 0L)
            status.text = if (until > System.currentTimeMillis()) {
                val days = ((until - System.currentTimeMillis()) / 86_400_000L).toString()
                "$days day(s)"
            } else "free"
        }
        render()

        var checked = 0
        plans.forEachIndexed { i, label ->
            val rb = android.widget.RadioButton(this).apply { text = label }
            rb.setOnClickListener { checked = i }
            group.addView(rb)
        }
        if (group.childCount > 0) (group.getChildAt(0) as android.widget.RadioButton).isChecked = true

        findViewById<Button>(R.id.goWatch).setOnClickListener {
            // Original plays a rewarded ad; recreation grants 20 coins directly.
            prefs.edit().putInt("coins", prefs.getInt("coins", 0) + 20).apply()
            Toast.makeText(this, "+20 coins (ad stub)", Toast.LENGTH_SHORT).show()
            render()
        }

        findViewById<Button>(R.id.redeem).setOnClickListener {
            val input = TextInputEditText(this)
            AlertDialog.Builder(this)
                .setTitle("Redeem code")
                .setView(input)
                .setPositiveButton("OK") { _, _ ->
                    val code = input.text?.toString()?.trim()
                    if (code == "CLONE-FREE") {
                        prefs.edit().putLong(
                            "vip_until",
                            System.currentTimeMillis() + 86_400_000L
                        ).apply()
                        Toast.makeText(this, "VIP activated for 1 day!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Invalid code", Toast.LENGTH_SHORT).show()
                    }
                    render()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
