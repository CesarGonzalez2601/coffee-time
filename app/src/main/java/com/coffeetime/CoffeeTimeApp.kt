package com.coffeetime

import android.app.Application
import com.coffeetime.di.AppContainer
import com.coffeetime.di.DefaultAppContainer

class CoffeeTimeApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer()
    }
}
