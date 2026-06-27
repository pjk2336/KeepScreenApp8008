package com.example.keepscreen

import android.os.Bundle
import android.os.PowerManager
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.keepscreen.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ========== 1. 全屏沉浸式模式（隐藏状态栏和导航栏） ==========
        setupFullScreen()

        // ========== 2. 保持屏幕常亮 + 防止亮度降低 ==========
        setupKeepScreenOn()

        // ========== 3. 设置布局 ==========
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 确保内容区域也是全屏的
        binding.root.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        )
    }

    /**
     * 设置全屏沉浸式模式
     */
    private fun setupFullScreen() {
        // 使用 WindowInsetsController (API 30+) 实现沉浸式全屏
        val controller = window.insetsController ?: return
        controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        controller.systemBarsBehavior =
            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    /**
     * 保持屏幕常亮，并防止亮度降低
     */
    private fun setupKeepScreenOn() {
        // 方式一：FLAG_KEEP_SCREEN_ON —— 阻止屏幕自动关闭和变暗
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 方式二：设置屏幕亮度为最大值，防止系统自动降低亮度
        val layoutParams = window.attributes
        layoutParams.screenBrightness = 1.0f  // 最大亮度 (0.0f ~ 1.0f)
        window.attributes = layoutParams

        // 方式三（双重保险）：使用 WakeLock 确保屏幕保持全亮
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "KeepScreenApp::ScreenWakeLock"
        )
        wakeLock?.acquire(24 * 60 * 60 * 1000L) // 最长保持24小时（防止意外释放）
    }

    override fun onResume() {
        super.onResume()
        // 恢复时重新设置全屏（防止从后台回来时状态丢失）
        setupFullScreen()

        // 确保 WakeLock 有效
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire(24 * 60 * 60 * 1000L)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // 窗口重新获得焦点时，再次设置全屏
            setupFullScreen()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 释放 WakeLock，避免内存泄漏
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }
}
