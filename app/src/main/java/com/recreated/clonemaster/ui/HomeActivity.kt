package com.recreated.clonemaster.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.recreated.clonemaster.App
import com.recreated.clonemaster.R
import com.recreated.clonemaster.databinding.ActivityHomeBinding
import com.recreated.clonemaster.db.SpaceWithClones
import kotlinx.coroutines.launch

/**
 * Recreated Home screen (original: com.cmaster.cloner.ui.HomeActivity + activity_home.xml).
 *
 * Original behavior: banner ad, hidden search bar, RecyclerView of space cards, FAB to clone.
 * Functional difference: the original runs clones inside a native container VM
 * (libcm.so/libcmvm.so); launching a clone here opens the real installed app instead.
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var b: ActivityHomeBinding
    private val spaces = mutableListOf<SpaceWithClones>()
    private var query: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)

        b.fab.setOnClickListener { startActivity(Intent(this, AppsActivity::class.java)) }

        b.ivSearchGo.setOnClickListener { query = b.etSearchSimple.text?.toString()?.trim() ?: ""; refresh() }
        b.ivClearSearch.setOnClickListener { b.etSearchSimple.setText(""); query = ""; refresh() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                App.get().db.spaceDao().all().collect { list ->
                    spaces.clear(); spaces.addAll(list)
                    b.viewLoading.visibility = android.view.View.GONE
                    b.tvEmpty.visibility = if (list.isEmpty()) android.view.View.VISIBLE
                    else android.view.View.GONE
                    refresh()
                    maybeShowRateDialog()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_search -> { b.flSearchContainer.visibility =
            if (b.flSearchContainer.visibility == android.view.View.VISIBLE) android.view.View.GONE
            else android.view.View.VISIBLE; true }
        R.id.action_device -> { startActivity(Intent(this, DevicePropertiesActivity::class.java)); true }
        R.id.action_vip -> { startActivity(Intent(this, VipActivity::class.java)); true }
        R.id.action_scanner -> { startActivity(Intent(this, ScannerActivity::class.java)); true }
        R.id.action_history -> { startActivity(Intent(this, ScanHistoryActivity::class.java)); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun refresh() {
        val filtered = if (query.isEmpty()) spaces else spaces.filter {
            it.space.name.contains(query, true) ||
                it.clones.any { c -> c.label.contains(query, true) }
        }
        b.recyclerView.layoutManager = LinearLayoutManager(this)
        b.recyclerView.adapter = SpaceAdapter(filtered) { pkg -> launchClone(pkg) }
    }

    /** Original launches the clone inside its VM; recreation launches the real app. */
    private fun launchClone(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) startActivity(intent)
        else android.widget.Toast
            .makeText(this, "App not installed on this device", android.widget.Toast.LENGTH_SHORT)
            .show()
    }

    private fun maybeShowRateDialog() {
        val uiPrefs = getSharedPreferences("ui", MODE_PRIVATE)
        lifecycleScope.launch {
            if (App.get().db.spaceDao().count() >= 3 && !uiPrefs.getBoolean("rate_shown", false)) {
                uiPrefs.edit().putBoolean("rate_shown", true).apply()
                val view = layoutInflater.inflate(R.layout.dialog_rate_us, null)
                val dialog = AlertDialog.Builder(this@HomeActivity).setView(view).create()
                view.findViewById<android.widget.Button>(R.id.btnLater)
                    .setOnClickListener { dialog.dismiss() }
                view.findViewById<android.widget.Button>(R.id.btnRate).setOnClickListener {
                    runCatching {
                        startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(
                                "https://play.google.com/store/apps/details?id=com.cmaster.cloner"))
                        )
                    }
                    dialog.dismiss()
                }
                dialog.show()
            }
        }
    }
}

/** Space card → grid of clones (mirrors card_app_space.xml). */
class SpaceAdapter(
    private val data: List<SpaceWithClones>,
    private val onLaunch: (String) -> Unit,
) : RecyclerView.Adapter<SpaceAdapter.Holder>() {

    class Holder(val card: android.view.View) : RecyclerView.ViewHolder(card)

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): Holder =
        Holder(layoutInflater(parent).inflate(R.layout.view_space_card, parent, false))

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(h: Holder, position: Int) {
        val item = data[position]
        val name = h.card.findViewById<android.widget.TextView>(R.id.spaceName)
        val mark = h.card.findViewById<android.widget.TextView>(R.id.anchor)
        val grid = h.card.findViewById<RecyclerView>(R.id.recyclerView)
        name.text = item.space.name
        mark.text = if (item.clones.isEmpty()) "empty" else "${item.clones.size}"
        grid.layoutManager = GridLayoutManager(grid.context, 4)
        grid.adapter = CloneAdapter(item.clones, onLaunch)
    }

    private fun layoutInflater(parent: android.view.ViewGroup) =
        parent.context.getSystemService(android.content.Context.LAYOUT_INFLATER_SERVICE)
            as android.view.LayoutInflater
}

/** Clone grid item (mirrors app_item.xml). */
class CloneAdapter(
    private val clones: List<com.recreated.clonemaster.db.CloneEntity>,
    private val onLaunch: (String) -> Unit,
) : RecyclerView.Adapter<CloneAdapter.Holder>() {

    class Holder(val view: android.view.View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): Holder =
        Holder(
            parent.context.getSystemService(android.content.Context.LAYOUT_INFLATER_SERVICE)
                .let { it as android.view.LayoutInflater }
                .inflate(R.layout.view_app_item, parent, false)
        )

    override fun getItemCount(): Int = clones.size

    override fun onBindViewHolder(h: Holder, position: Int) {
        val c = clones[position]
        val ctx = h.view.context
        val pm = ctx.packageManager
        val icon = runCatching { pm.getApplicationIcon(c.packageName) }.getOrNull()
        h.view.findViewById<android.widget.ImageView>(R.id.icon).setImageDrawable(icon)
        h.view.findViewById<android.widget.TextView>(R.id.title).text = c.label
        h.view.findViewById<android.widget.TextView>(R.id.des).text = c.packageName
        h.view.setOnClickListener { onLaunch(c.packageName) }
    }
}
