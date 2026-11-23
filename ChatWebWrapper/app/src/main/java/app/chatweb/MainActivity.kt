package app.chatweb

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.ViewGroup
import android.webkit.*

class MainActivity : Activity() {

    private lateinit var web: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var pendingPermission: PermissionRequest? = null
    private var lastBack = 0L

    private val desktopUA =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/124.0.0.0 Safari/537.36"

    // Paksa layout sempit (satu panel) seperti WhatsApp mobile
    private val injectJs = """
        (function(){
          var m=document.querySelector('meta[name=viewport]');
          if(!m){m=document.createElement('meta');m.name='viewport';document.head.appendChild(m);}
          m.content='width=device-width, initial-scale=1, maximum-scale=1';
          var s=document.getElementById('cw-style');
          if(!s){s=document.createElement('style');s.id='cw-style';
            s.textContent='html,body{overscroll-behavior:none}';
            document.head.appendChild(s);}
        })();
    """.trimIndent()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF075E54.toInt()

        web = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        setContentView(web)

        CookieManager.getInstance().apply {
            setAcceptCookie(true); setAcceptThirdPartyCookies(web, true)
        }
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            userAgentString = desktopUA
            useWideViewPort = false
            loadWithOverviewMode = false
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            setSupportZoom(false)
            builtInZoomControls = false
        }

        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                view.evaluateJavascript(injectJs, null)
            }
            override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean {
                val host = req.url.host ?: return false
                if (host.endsWith("whatsapp.com") || host.endsWith("whatsapp.net")) return false
                startActivity(Intent(Intent.ACTION_VIEW, req.url)); return true
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                v: WebView, cb: ValueCallback<Array<Uri>>, p: FileChooserParams
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = cb
                return try { startActivityForResult(p.createIntent(), 100); true }
                catch (e: Exception) { filePathCallback = null; false }
            }
            override fun onPermissionRequest(req: PermissionRequest) {
                runOnUiThread {
                    val need = mutableListOf<String>()
                    req.resources.forEach {
                        if (it == PermissionRequest.RESOURCE_VIDEO_CAPTURE) need += Manifest.permission.CAMERA
                        if (it == PermissionRequest.RESOURCE_AUDIO_CAPTURE) need += Manifest.permission.RECORD_AUDIO
                    }
                    val missing = need.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
                    if (missing.isEmpty()) req.grant(req.resources)
                    else { pendingPermission = req; requestPermissions(missing.toTypedArray(), 200) }
                }
            }
        }

        if (savedInstanceState != null) web.restoreState(savedInstanceState)
        else web.loadUrl("https://web.whatsapp.com/")
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        if (req == 100) {
            filePathCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data))
            filePathCallback = null
        } else super.onActivityResult(req, res, data)
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, r: IntArray) {
        if (rc == 200) {
            val ok = r.isNotEmpty() && r.all { it == PackageManager.PERMISSION_GRANTED }
            pendingPermission?.let { if (ok) it.grant(it.resources) else it.deny() }
            pendingPermission = null
        }
    }

    // Back: kirim Escape ke WhatsApp Web (tutup chat); tekan dua kali cepat untuk keluar
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastBack < 1500) { super.onBackPressed(); return }
        lastBack = now
        web.evaluateJavascript(
            "document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',keyCode:27,bubbles:true}));", null)
    }

    override fun onSaveInstanceState(out: Bundle) { super.onSaveInstanceState(out); web.saveState(out) }
    override fun onPause() { super.onPause(); CookieManager.getInstance().flush() }
}
