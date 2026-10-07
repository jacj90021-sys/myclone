package com.recreated.clonemaster.ui

import android.os.Bundle
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.SwitchCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.recreated.clonemaster.App
import com.recreated.clonemaster.R
import com.recreated.clonemaster.db.DeviceProfileEntity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Recreated device-properties editor (original: fragment_device.xml).
 * In the original these PRODUCT/BRAND/MODEL/DEVICE values are pushed into the VM so cloned
 * apps see a different device; here they are stored and shown locally (honest demo).
 */
class DevicePropertiesActivity : AppCompatActivity() {

    private lateinit var fields: List<AutoCompleteTextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fields = listOf(R.id.etProduct, R.id.etBrand, R.id.etModel, R.id.etDevice)
            .map { findViewById(it) }

        val properties = findViewById<View>(R.id.properties)
        val editSwitch = findViewById<SwitchCompat>(R.id.editSwitch)
        val toggle: (View) -> Unit = { properties.visibility =
            if (editSwitch.isChecked) View.VISIBLE else View.GONE }
        editSwitch.setOnClickListener(toggle)

        findViewById<Button>(R.id.actionRestore).setOnClickListener {
            fields.forEachIndexed { i, et ->
                et.setText(arrayOf(android.os.Build.PRODUCT, android.os.Build.BRAND,
                    android.os.Build.MODEL, android.os.Build.DEVICE)[i])
            }
        }
        findViewById<Button>(R.id.actionRandom).setOnClickListener {
            fields.forEach { et ->
                et.setText(Hex8() + "-" + Hex4())
            }
        }
        findViewById<Button>(R.id.actionSave).setOnClickListener {
            lifecycleScope.launch {
                App.get().db.deviceDao().put(
                    DeviceProfileEntity(
                        product = fields[0].text.toString(),
                        brand = fields[1].text.toString(),
                        model = fields[2].text.toString(),
                        device = fields[3].text.toString(),
                    )
                )
                android.widget.Toast.makeText(this@DevicePropertiesActivity,
                    "Profile saved (stored locally)", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        // prefill from store, else from real device (DB access off the main thread)
        lifecycleScope.launch {
            val saved = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                App.get().db.deviceDao().profile().firstOrNull()
            }
            if (saved != null) {
                fields[0].setText(saved.product)
                fields[1].setText(saved.brand)
                fields[2].setText(saved.model)
                fields[3].setText(saved.device)
            } else {
                fields[0].setText(android.os.Build.PRODUCT)
                fields[1].setText(android.os.Build.BRAND)
                fields[2].setText(android.os.Build.MODEL)
                fields[3].setText(android.os.Build.DEVICE)
            }
        }
        properties.visibility = View.GONE
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun Hex4(): String = (1..4).joinToString("") {
        "0123456789ABCDEF".random().toString()
    }
    private fun Hex8(): String = (1..8).joinToString("") {
        "0123456789ABCDEF".random().toString()
    }
}
