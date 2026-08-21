package com.lagradost.cloudstream3.ui.splash

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.lagradost.cloudstream3.CommonActivity.loadThemes
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.plugins.PluginManager
import com.lagradost.cloudstream3.utils.Coroutines.ioSafe
import com.lagradost.cloudstream3.utils.UIHelper.enableEdgeToEdgeCompat
import com.lagradost.cloudstream3.utils.UIHelper.setNavigationBarColorCompat

class SplashActivity : FragmentActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var pluginsLoaded = false
    private var timerExpired = false
    private var navigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadThemes(this)
        enableEdgeToEdgeCompat()
        setNavigationBarColorCompat(R.attr.primaryBlackBackground)

        setContentView(R.layout.activity_splash)

        val splashTitle = findViewById<TextView>(R.id.splash_title)
        startShimmerAnimation(splashTitle)

        loadPluginsInBackground()

        handler.postDelayed({
            timerExpired = true
            tryNavigate()
        }, SPLASH_DURATION_MS)
    }

    private fun startShimmerAnimation(textView: TextView) {
        val shimmer = ObjectAnimator.ofFloat(textView, View.ALPHA, 0.3f, 1f)
        shimmer.duration = 1500L
        shimmer.repeatCount = ObjectAnimator.INFINITE
        shimmer.repeatMode = ObjectAnimator.REVERSE
        shimmer.interpolator = AccelerateDecelerateInterpolator()
        shimmer.start()
    }

    private fun loadPluginsInBackground() {
        ioSafe {
            try {
                PluginManager.___DO_NOT_CALL_FROM_A_PLUGIN_loadBundledPlugins(this@SplashActivity)
                PluginManager.___DO_NOT_CALL_FROM_A_PLUGIN_installTlnPluginsOnFirstBoot(this@SplashActivity)
                PluginManager.___DO_NOT_CALL_FROM_A_PLUGIN_loadAllLocalPlugins(this@SplashActivity, false)
            } catch (_: Throwable) {
            }
            pluginsLoaded = true
            runOnUiThread {
                tryNavigate()
            }
        }
    }

    private fun tryNavigate() {
        if (navigated) return
        if (!timerExpired || !pluginsLoaded) return
        navigated = true

        if (isFinishing || isDestroyed) return

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        private const val SPLASH_DURATION_MS = 8000L
    }
}
