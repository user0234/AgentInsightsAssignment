package com.hellow.agentinsightsassignment

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hellow.agentinsightsassignment.databinding.ActivityMainBinding
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences


    companion object {
        const val PERMISSION_REQUEST_CODE = 10
        const val SHARED_PREF_STATUS_TEXT = "shared pref text"
        const val SHARED_PREF_STATUS_SHOW = "shared pref show text"
    }

    private val broadcast = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            showStatusToast(context!!)
        }
    }

    private fun showStatusToast(context: Context) {
        val isToggled = prefs.getBoolean(SHARED_PREF_STATUS_SHOW, false)
        if (isToggled) {
            val statusText = prefs.getString(SHARED_PREF_STATUS_TEXT, null)
            Toast.makeText(context, " status - $statusText", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = this.getSharedPreferences(this.getString(R.string.app_name), Context.MODE_PRIVATE)

        val isToggled = prefs.getBoolean(SHARED_PREF_STATUS_SHOW, false)
        binding.statusToggle.isChecked = isToggled

        binding.requestPermission.setOnClickListener {
            checkForPermission()
        }

        binding.saveStatus.setOnClickListener {
            // save the status in shared pref
            val statusValue = binding.statusEt.text.toString()
            binding.statusEt.clearFocus()
            prefs.edit().putString(SHARED_PREF_STATUS_TEXT, statusValue).apply()
            binding.statusEt.setText("")

        }

        binding.statusToggle.setOnCheckedChangeListener { _, boolean ->
            prefs.edit().putBoolean(SHARED_PREF_STATUS_SHOW,boolean).apply()
        }

    }

    private fun checkForPermission() {
        if (hasPermission()) {
            binding.permissionText.text = "registered"
            registerReceiver(broadcast, filter)
        } else {
            requestPermission()
        }
    }

    private fun requestPermission() {
        EasyPermissions.requestPermissions(
            this, "Permission is Required read state",
            PERMISSION_REQUEST_CODE, Manifest.permission.READ_PHONE_STATE
        )
    }

    private fun hasPermission(): Boolean {
        return EasyPermissions.hasPermissions(
            applicationContext,
            Manifest.permission.READ_PHONE_STATE
        )
    }

    override fun onResume() {

        super.onResume()

    }

    override fun onStop() {
        unregisterReceiver(broadcast)
        super.onStop()

    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionDenied(this, perms.first())) {
            SettingsDialog.Builder(this).build().show()
        } else {
            requestPermission()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                // got the permission
                binding.permissionText.text = "has permission"
                registerReceiver(broadcast, filter)
            }

            else -> {
                requestPermission()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }
}