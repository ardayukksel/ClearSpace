package com.example.clearspace

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.Collator
import java.util.Locale

class AppPickerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_APP_PACKAGE = "extra_app_package"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_picker)

        val recyclerView = findViewById<RecyclerView>(R.id.rv_apps)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val apps = loadLaunchableApps()
        recyclerView.adapter = AppAdapter(apps) { selectedApp ->
            val resultIntent = Intent().apply {
                putExtra(EXTRA_APP_NAME, selectedApp.name)
                putExtra(EXTRA_APP_PACKAGE, selectedApp.packageName)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun loadLaunchableApps(): List<AppInfo> {
        val pm = packageManager

        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val collator = Collator.getInstance(Locale.getDefault())

        return pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                val pkg = activityInfo.packageName

                if (pkg == applicationContext.packageName) return@mapNotNull null

                val appName = resolveInfo.loadLabel(pm)?.toString()?.trim().orEmpty()
                val icon = resolveInfo.loadIcon(pm)

                if (appName.isBlank()) return@mapNotNull null

                AppInfo(
                    name = appName,
                    packageName = pkg,
                    icon = icon
                )
            }
            .distinctBy { it.packageName }
            .sortedWith { a, b -> collator.compare(a.name, b.name) }
    }

    data class AppInfo(
        val name: String,
        val packageName: String,
        val icon: Drawable
    )

    class AppAdapter(
        private val apps: List<AppInfo>,
        private val onClick: (AppInfo) -> Unit
    ) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.iv_app_icon)
            val name: TextView = view.findViewById(R.id.tv_app_name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps[position]
            holder.name.text = app.name
            holder.icon.setImageDrawable(app.icon)
            holder.itemView.setOnClickListener { onClick(app) }
        }

        override fun getItemCount(): Int = apps.size
    }
}