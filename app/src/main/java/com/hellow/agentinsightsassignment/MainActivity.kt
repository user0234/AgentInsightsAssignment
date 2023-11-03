package com.hellow.agentinsightsassignment

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.telephony.PhoneStateListener
import android.telephony.SmsManager
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
    var isSent = false

    companion object {
        const val PERMISSION_REQUEST_CODE = 10
        const val SHARED_PREF_STATUS_TEXT = "shared pref text"
        const val SHARED_PREF_STATUS_SHOW = "shared pref show text"
        val permsi = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CALL_LOG
        )
    }

    private val broadcast = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            showStatusToast(context!!)
            isSent = false
            // send the text message to the number

            val state: String = intent!!.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
            var wasRinging = false
            var wasPicked = false
            val telephonyManager: TelephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager;
            telephonyManager.listen(object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    super.onCallStateChanged(state, phoneNumber)

                    if(state == TelephonyManager.CALL_STATE_RINGING){
                        wasRinging = true
                    }
                    if(state == TelephonyManager.CALL_STATE_OFFHOOK){
                        wasPicked = true
                    }
                    if(state == TelephonyManager.CALL_STATE_IDLE) {
                        if(!wasPicked && wasRinging&&!isSent){
                            // call is missed
                            val statusText = prefs.getString(SHARED_PREF_STATUS_TEXT, null)
                            sendAutomatedSms(statusText!!,phoneNumber!!)
                            wasRinging = false
                            wasPicked = false
                            isSent = true
                        }
                    }

                }
            }, PhoneStateListener.LISTEN_CALL_STATE);
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
            prefs.edit().putBoolean(SHARED_PREF_STATUS_SHOW, boolean).apply()
        }

    }

    private fun checkForPermission() {

        if (hasPermission(permsi)) {
            binding.permissionText.text = "registered"
            registerReceiver(broadcast, filter)
        } else {
            requestPermission(permsi)
        }
    }

    private fun sendAutomatedSms(stateValue: String, phoneNumber: String) {
     //   Toast.makeText(this, "phone - $phoneNumber", Toast.LENGTH_SHORT).show()
        try {
            val smsManager: SmsManager = SmsManager.getDefault()
            val info = "stateValue - $stateValue"
            smsManager.sendTextMessage(phoneNumber, null, info, null, null)
            isSent = true
        } catch (e: Exception) {
           // Toast.makeText(this, "SMS not sent", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun requestPermission(prems: Array<String>) {

        EasyPermissions.requestPermissions(
            this, "this", PERMISSION_REQUEST_CODE,
            *prems
        )
    }

    private fun hasPermission(prems: Array<String>): Boolean {
        return EasyPermissions.hasPermissions(
            applicationContext,
            *prems
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
            requestPermission(permsi)
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
                requestPermission(permsi)
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