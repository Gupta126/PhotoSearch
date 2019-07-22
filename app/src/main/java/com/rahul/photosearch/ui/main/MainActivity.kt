package com.rahul.photosearch.ui.main

import android.content.Intent
import android.os.Bundle
import com.rahul.photosearch.R
import dagger.android.support.DaggerAppCompatActivity
import com.rahul.photosearch.ui.common.OnActivityReenterListener

class MainActivity : DaggerAppCompatActivity(), OnActivityReenterListener.Host {

    private val reenterDelegate = OnActivityReenterListener.HostDelegate()
    override fun addListener(listener: OnActivityReenterListener) {
        reenterDelegate.addListener(listener)
    }

    override fun removeListener(listener: OnActivityReenterListener) {
        reenterDelegate.removeListener(listener)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.content, MainFragment())
                .commitNow()
        }
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
        reenterDelegate.onActivityReenter(resultCode, data)
    }
}
