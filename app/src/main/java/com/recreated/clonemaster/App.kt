package com.recreated.clonemaster

import android.app.Application
import com.recreated.clonemaster.db.AppDatabase

class App : Application() {
    lateinit var db: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        db = AppDatabase.build(this)
    }

    companion object {
        @Volatile private var instance: App? = null
        fun get(): App = instance!!
    }
}
