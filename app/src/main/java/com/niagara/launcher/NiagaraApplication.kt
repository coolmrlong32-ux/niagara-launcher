package com.niagara.launcher

import android.app.Application
import android.content.ComponentCallbacks2
import com.niagara.launcher.core.data.AppRepository
import com.niagara.launcher.core.data.PreferencesRepository

class NiagaraApplication : Application() {
    lateinit var appRepository: AppRepository
        private set
    lateinit var preferencesRepository: PreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        appRepository = AppRepository(this)
        preferencesRepository = PreferencesRepository(this)
        appRepository.startObservingPackages()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            appRepository.onTrimMemory()
        }
    }
}
