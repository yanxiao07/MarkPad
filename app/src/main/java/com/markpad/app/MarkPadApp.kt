package com.markpad.app

import android.app.Application
import com.markpad.app.data.local.AppDatabase

class MarkPadApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
