package net.squanchy.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_wifi_config_error.*
import net.squanchy.R

class WifiConfigErrorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_wifi_config_error)

        textSsid.text = intent.getSerializableExtra(EXTRA_WIFI_SSID).toString()
        val password = intent.getSerializableExtra(EXTRA_WIFI_PASSWORD).toString()
        textPassword.text = password
        labelCopyPassword.setOnClickListener {
            copyToClipboard(password)
            Toast.makeText(this, R.string.wifi_config_error_password_copied, Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyToClipboard(password: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(COPIED_PASSWORD_LABEL, password)
        clipboard.primaryClip = clip
    }

    companion object {

        private val EXTRA_WIFI_SSID = WifiConfigErrorActivity::class.java.canonicalName + ".wifi_ssid"
        private val EXTRA_WIFI_PASSWORD = WifiConfigErrorActivity::class.java.canonicalName + ".wifi_password"
        private val COPIED_PASSWORD_LABEL = "password"

        fun createIntent(context: Context, ssid: String, password: String) =
            Intent(context, WifiConfigErrorActivity::class.java).apply {
                putExtra(EXTRA_WIFI_SSID, ssid)
                putExtra(EXTRA_WIFI_PASSWORD, password)
            }
    }
}
