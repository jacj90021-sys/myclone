package com.recreated.clonemaster.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.recreated.clonemaster.App
import com.recreated.clonemaster.R
import com.recreated.clonemaster.databinding.ActivityAppsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Recreated app picker (original: activity_apps.xml).
 * Shows installed launchable apps; picking one creates a clone entry in the newest space.
 */
class AppsActivity : AppCompatActivity() {

    private lateinit var b: ActivityAppsBinding
    private val apps = mutableListOf<AppInfo>()

    data class AppInfo(
        val packageName: String,
        val label: String,
        val icon: android.graphics.drawable.Drawable?,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAppsBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        b.recyclerView.layoutManager = LinearLayoutManager(this)
        b.recyclerView.adapter = AppsAdapter(apps) { createClone(it) }
        b.settings.setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_SETTINGS))
        }
        loadApps()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun loadApps() {
        b.viewLoading.visibility = View.VISIBLE
        lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) {
                val pm = packageManager
                val launchables = pm.getInstalledApplications(0)
                    .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                    .mapNotNull { appInfo ->
                        runCatching {
                            AppInfo(
                                appInfo.packageName,
                                pm.getApplicationLabel(appInfo).toString(),
                                runCatching { pm.getApplicationIcon(appInfo.packageName) }.getOrNull(),
                            )
                        }.getOrNull()
                    }
                    .sortedBy { it.label.lowercase() }
                launchables
            }
            apps.clear(); apps.addAll(list)
            b.viewLoading.visibility = View.GONE
            // If the list is tiny, the user probably denied package-visibility (like the original's error state)
            b.error.visibility = if (list.size < 3) View.VISIBLE else View.GONE
            b.recyclerView.adapter?.notifyDataSetChanged()
        }
    }

    private fun createClone(info: AppInfo) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val dao = App.get().db.spaceDao()
                val space = dao.latest() ?: run {
                    val id = dao.addSpace(com.recreated.clonemaster.db.SpaceEntity(name = "My Space"))
                    com.recreated.clonemaster.db.SpaceEntity(id = id, name = "My Space")
                }
                dao.addClone(
                    com.recreated.clonemaster.db.CloneEntity(
                        spaceId = space.id,
                        packageName = info.packageName,
                        label = info.label,
                    )
                )
            }
            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}

class AppsAdapter(
    private val data: List<AppsActivity.AppInfo>,
    private val onClick: (AppsActivity.AppInfo) -> Unit,
) : RecyclerView.Adapter<AppsAdapter.Holder>() {

    class Holder(v: View) : RecyclerView.ViewHolder(v)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.view_app_item, parent, false))

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(h: Holder, position: Int) {
        val app = data[position]
        h.itemView.findViewById<ImageView>(R.id.icon).setImageDrawable(app.icon)
        h.itemView.findViewById<TextView>(R.id.title).text = app.label
        h.itemView.findViewById<TextView>(R.id.des).text = app.packageName
        h.itemView.setOnClickListener { onClick(app) }
    }
}
