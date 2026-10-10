package com.ghostreborn.akira.features.auth.widgets

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.ghostreborn.akira.ui.components.AkiraLogoGradientBrush
import com.ghostreborn.akira.ui.theme.AkiraPrimary
import com.ghostreborn.akira.ui.theme.AkiraSurface
import com.ghostreborn.akira.ui.theme.AkiraTextPrimary
import com.ghostreborn.akira.ui.theme.AkiraTextSecondary

class CaptchaJsInterface(private val onTokenReceived: (String) -> Unit) {
    @JavascriptInterface
    fun postMessage(token: String) {
        val trimmed = token.trim()
        if (trimmed.isNotEmpty()) {
            onTokenReceived(trimmed)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CloudflareVerificationDialog(
    onTokenExtracted: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var tokenDelivered by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .shadow(elevation = 28.dp, shape = RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(AkiraLogoGradientBrush)
                .padding(1.5.dp) // Gradient border
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.5.dp),
                color = AkiraSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Verify you are human",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AkiraTextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Color(0xFFF1F5F9))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = AkiraTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(androidx.compose.ui.graphics.Color(0xFFF8FAFC))
                            .border(1.dp, androidx.compose.ui.graphics.Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    setBackgroundColor(Color.TRANSPARENT)
                                    setLayerType(View.LAYER_TYPE_HARDWARE, null)

                                    // Cookie configuration
                                    val cookieManager = CookieManager.getInstance()
                                    cookieManager.setAcceptCookie(true)
                                    cookieManager.setAcceptThirdPartyCookies(this, true)

                                    // WebView settings for Turnstile execution
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        databaseEnabled = true
                                        javaScriptCanOpenWindowsAutomatically = true
                                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                        cacheMode = WebSettings.LOAD_DEFAULT

                                        userAgentString =
                                            "Mozilla/5.0 (Linux; Android 14; 2312DRA50I Build/CP2A.260605.016; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/130.0.0.0 Mobile Safari/537.36"
                                    }

                                    // JS Channel matching flutter
                                    addJavascriptInterface(
                                        CaptchaJsInterface { token ->
                                            if (!tokenDelivered) {
                                                tokenDelivered = true
                                                post { onTokenExtracted(token) }
                                            }
                                        },
                                        "CaptchaChannel"
                                    )

                                    webChromeClient = WebChromeClient()

                                    val targetHeaders = mapOf(
                                        "Referer" to "https://allmanga.to",
                                        "Origin" to "https://allmanga.to",
                                        "sec-ch-ua" to "\"Not=A?Brand\";v=\"99\", \"Android WebView\";v=\"130\", \"Chromium\";v=\"130\"",
                                        "sec-ch-ua-mobile" to "?1",
                                        "sec-ch-ua-platform" to "\"Android\"",
                                        "upgrade-insecure-requests" to "1",
                                        "accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
                                        "x-requested-with" to "com.allanime.animechicken",
                                        "accept-language" to "en-US,en;q=0.9"
                                    )

                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                            super.onPageStarted(view, url, favicon)
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            isLoading = false
                                            val js = """
                                                (function() {
                                                  try {
                                                    var style = document.createElement('style');
                                                    style.innerHTML = 'body, html { background: transparent !important; display: flex !important; justify-content: center !important; align-items: center !important; min-height: 100vh !important; margin: 0 !important; padding: 0 !important; overflow: hidden !important; width: 100vw !important; height: 100vh !important; } .cf-turnstile { margin: auto !important; }';
                                                    document.head.appendChild(style);
                                                  } catch (e) {}

                                                  function sendToken(t) {
                                                    if (t && window.CaptchaChannel) {
                                                      window.CaptchaChannel.postMessage(t);
                                                    }
                                                  }

                                                  var oldCb = window.captchaCallback;
                                                  window.captchaCallback = function(t) {
                                                    sendToken(t);
                                                    if (oldCb) oldCb(t);
                                                  };

                                                  setInterval(function() {
                                                    var input = document.querySelector('input[name="cf-turnstile-response"]') || 
                                                                document.querySelector('textarea[name="g-recaptcha-response"]') ||
                                                                document.querySelector('input[name="g-recaptcha-response"]');
                                                    if (input && input.value && !window.__tokenExtracted) {
                                                      window.__tokenExtracted = true;
                                                      sendToken(input.value);
                                                    }
                                                  }, 300);
                                                })();
                                            """.trimIndent()
                                            view?.evaluateJavascript(js, null)
                                        }

                                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                            Log.e("Turnstile", "Error loading captcha: ${error?.description}")
                                        }

                                        override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                                            Log.e("Turnstile", "HttpError: ${errorResponse?.statusCode}")
                                        }
                                    }

                                    loadUrl("https://api.allanime.day/captcha/turnstile", targetHeaders)
                                }
                            }
                        )

                        if (isLoading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.5.dp,
                                color = AkiraPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
