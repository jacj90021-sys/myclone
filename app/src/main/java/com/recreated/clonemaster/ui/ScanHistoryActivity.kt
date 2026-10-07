package com.recreated.clonemaster.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.recreated.clonemaster.App
import com.recreated.clonemaster.R
import com.recreated.clonemaster.databinding.ActivityHistoryBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Recreated scan-history screen (original: ActivityBarCodeHistory). */
class ScanHistoryActivity : AppCompatActivity() {

    private lateinit var b: ActivityHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        b.recyclerView.layoutManager = LinearLayoutManager(this)
        val fmt = SimpleDateFormat("MMM d, HH:mm:ss", Locale.US)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                App.get().db.scanDao().all().collect { items ->
                    b.recyclerView.adapter = object : RecyclerView.Adapter<HistoryHolder>() {
                        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                            HistoryHolder(
                                LayoutInflater.from(parent.context)
                                    .inflate(R.layout.view_history_item, parent, false)
                            )
                        override fun getItemCount() = items.size
                        override fun onBindViewHolder(h: HistoryHolder, position: Int) {
                            h.content.text = items[position].content
                            h.time.text = fmt.format(Date(items[position].at))
                        }
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}

class HistoryHolder(v: View) : RecyclerView.ViewHolder(v) {
    val content: TextView = v.findViewById(R.id.content)
    val time: TextView = v.findViewById(R.id.time)
}
