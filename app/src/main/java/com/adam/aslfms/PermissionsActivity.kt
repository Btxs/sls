/**
 * This file is part of Simple Scrobbler.
 *
 *
 * https://github.com/simple-last-fm-scrobbler/sls
 *
 *
 * Copyright 2011 Simple Scrobbler Team
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adam.aslfms

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources.Theme
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.adam.aslfms.util.AppSettings
import com.adam.aslfms.util.MyContextWrapper
import com.adam.aslfms.util.Util

/**
 * @author a93h
 * @since 1.5.8
 */
class PermissionsActivity : AppCompatActivity() {
    private enum class ButtonChoice {
        SKIP, CONTINUE, BACK
    }

    private val WRITE_EXTERNAL_STORAGE = 0
    private val skipPermissions = false
    private var settings: AppSettings? = null
    private var skipBtn: Button? = null
    private var continueBtn: Button? = null
    private var externalPermBtn: Button? = null
    private var notifiPermBtn: Button? = null
    private var batteryPermBtn: Button? = null
    private var privacyLinkBtn: ImageButton? = null
    var ctx: Context = this
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(MyContextWrapper.wrap(newBase))
    }

    override fun getTheme(): Theme {
        settings = AppSettings(this)
        val theme = super.getTheme()
        theme.applyStyle(settings!!.appTheme, true)
        //Log.d(TAG, getResources().getResourceName(settings.getAppTheme()));
        // you could also use a switch if you have many themes that could apply
        return theme
    }

    override fun onResume() {
        super.onResume()
        checkAndSetColors()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)
        settings = AppSettings(this)
        setTheme(settings!!.appTheme)
        settings!!.keyBypassNewPermissions = 2
        checkCurrrentPermissions()
    }

    override fun onBackPressed() {
        leavePermissionsDialogue(this, ButtonChoice.BACK)
    }

    fun checkCurrrentPermissions() {
        privacyLinkBtn = findViewById(R.id.privacy_link_button)
        skipBtn = findViewById(R.id.button_skip)
        skipBtn!!.setBackgroundColor(enabledColor)
        continueBtn = findViewById(R.id.button_continue)
        externalPermBtn = findViewById(R.id.button_permission_external_storage)
        notifiPermBtn = findViewById(R.id.button_permission_notification_listener)
        batteryPermBtn = findViewById(R.id.button_permission_battery_optimizations)
        val findBattery = findViewById<TextView>(R.id.text_find_battery_optimization_setting)
        val findNotify = findViewById<TextView>(R.id.text_find_notification_setting)
        checkAndSetColors()
        externalPermBtn!!.setOnClickListener(View.OnClickListener { view: View? ->
            try {
                if (Util.checkExternalPermission(this)) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } else {
                    ActivityCompat.requestPermissions(
                        this@PermissionsActivity,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        WRITE_EXTERNAL_STORAGE
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
        })
        notifiPermBtn!!.setOnClickListener(View.OnClickListener { view: View? ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    val intent = Intent()
                    intent.action = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                    startActivity(intent)
                } catch (e: Exception) {
                    findNotify.setTextColor(warningColor)
                    findNotify.setText(R.string.find_notifications_settings)
                    Log.e(TAG, e.toString())
                }
            }
        })
        batteryPermBtn!!.setOnClickListener(View.OnClickListener { view: View? ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val intent = Intent()
                    val packageName = this.packageName
                    val pm = this.getSystemService(POWER_SERVICE) as PowerManager
                    if (pm.isIgnoringBatteryOptimizations(packageName)) intent.action =
                        Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS else {
                        intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        intent.data = Uri.parse("package:$packageName")
                    }
                    this.startActivity(intent)
                } catch (e: Exception) {
                    findBattery.setTextColor(warningColor)
                    findBattery.setText(R.string.find_battery_settings)
                    Log.e(TAG, e.toString())
                }
            }
        })
        skipBtn!!.setOnClickListener(View.OnClickListener { view: View ->
            leavePermissionsDialogue(
                view.context,
                ButtonChoice.SKIP
            )
        })
        continueBtn!!.setOnClickListener(View.OnClickListener { view: View ->
            leavePermissionsDialogue(
                view.context,
                ButtonChoice.CONTINUE
            )
        })
        privacyLinkBtn!!.setOnClickListener(View.OnClickListener { view: View? ->
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/simple-last-fm-scrobbler/sls/wiki/Privacy-Concerns")
                )
            )
        })
    }

    fun colorPermission(enabled: Boolean, button: Button?) {
        if (enabled) {
            button!!.setBackgroundColor(enabledColor)
            return
        }
        button!!.setBackgroundColor(disabledColor)
    }

    private fun checkAndSetColors() {
        colorPermission(Util.checkExternalPermission(this), externalPermBtn)
        colorPermission(Util.checkNotificationListenerPermission(this), notifiPermBtn)
        colorPermission(Util.checkBatteryOptimizationsPermission(this), batteryPermBtn)
        colorPermission(allPermsCheck(), continueBtn)
        colorPermission(!allPermsCheck(), skipBtn)
    }

    private fun allPermsCheck(): Boolean {
        return (Util.checkNotificationListenerPermission(this)
                && Util.checkExternalPermission(this)
                && Util.checkBatteryOptimizationsPermission(this))
    }

    private fun resolveChoice(bypass: Int) {
        settings!!.whatsNewViewedVersion = Util.getAppVersionCode(ctx, packageName)
        settings!!.keyBypassNewPermissions = bypass
        finish()
    }

    private fun leavePermissionsDialogue(context: Context, buttonChoice: ButtonChoice) {
        if (allPermsCheck() && buttonChoice != ButtonChoice.SKIP) {
            resolveChoice(0) // user has bypassed permissions is False
        } else if (!allPermsCheck() && buttonChoice != ButtonChoice.CONTINUE) {
            val dialogClickListener =
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> resolveChoice(1) // user has bypassed permissions is True
                        DialogInterface.BUTTON_NEGATIVE -> {}
                    }
                }
            val builder = AlertDialog.Builder(context)
            var message =
                context.resources.getString(R.string.warning) + "! " + context.resources.getString(R.string.are_you_sure)
            if (Build.VERSION_CODES.O <= Build.VERSION.SDK_INT && !Util.checkNotificationListenerPermission(
                    context
                )
            ) {
                message += " - " + context.resources.getString(R.string.warning_will_not_scrobble)
                message += " - " + context.resources.getString(R.string.permission_notification_listener)
            }
            builder.setMessage(message).setPositiveButton(R.string.yes, dialogClickListener)
                .setNegativeButton(R.string.no, dialogClickListener).show()
        }
    }

    companion object {
        private const val TAG = "PermissionsActivity"
        private val disabledColor = Color.argb(25, 0, 0, 0)
        private val enabledColor = Color.argb(75, 0, 255, 0)
        private val warningColor = Color.argb(80, 255, 0, 0)
    }
}