package com.direader

import android.app.Application
import com.direader.data.AppDatabase

/**
 * DiReader Application 入口。
 * 初始化全局单例：数据库。
 */
class DiReaderApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
    }

    companion object {
        lateinit var instance: DiReaderApp
            private set
    }
}
