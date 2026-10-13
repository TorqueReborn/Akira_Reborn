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

private const val TURNSTILE_SETUP_JS = """
(function() {
  function sendToken(t) {
    if (t && window.CaptchaChannel) {
      window.CaptchaChannel.postMessage(t);
    }
  }

  // Pre-bind captchaCallback directly so when Cloudflare calls it, we get the token immediately
  var oldCb = window.captchaCallback;
  window.captchaCallback = function(t) {
    sendToken(t);
    if (oldCb) oldCb(t);
  };

  // Mock flutter_inappwebview interface that allanime captcha HTML checks natively
  if (!window.flutter_inappwebview) {
    window.flutter_inappwebview = {
      callHandler: function(handlerName, payload) {
        if (payload && payload.token) {
          sendToken(payload.token);
        }
      }
    };
  }

  // Poller + MutationObserver using official Cloudflare turnstile.getResponse() API
  function checkToken() {
    if (window.__tokenExtracted) return;
    try {
      if (window.turnstile && typeof window.turnstile.getResponse === 'function') {
        var resp = window.turnstile.getResponse();
        if (resp) {
          window.__tokenExtracted = true;
          sendToken(resp);
          return;
        }
      }
    } catch (e) {}

    var input = document.querySelector('input[name="cf-turnstile-response"]') || 
                document.querySelector('textarea[name="g-recaptcha-response"]') ||
                document.querySelector('input[name="g-recaptcha-response"]');
    if (input && input.value) {
      window.__tokenExtracted = true;
      sendToken(input.value);
    }
  }

  try {
    var observer = new MutationObserver(function() { checkToken(); });
    observer.observe(document.body || document.documentElement, { childList: true, subtree: true, attributes: true });
  } catch (e) {}

  var pollInterval = setInterval(function() {
    checkToken();
    if (window.__tokenExtracted) {
      clearInterval(pollInterval);
    }
  }, 50);
})();
"""

private const val TURNSTILE_STYLE_JS = """
(function() {
  try {
    var style = document.createElement('style');
    style.innerHTML = `
      body, html {
        background: transparent !important;
        margin: 0 !important;
        padding: 0 !important;
        display: flex !important;
        justify-content: center !important;
        align-items: center !important;
        height: 100% !important;
        width: 100% !important;
        overflow: hidden !important;
      }
      .cf-turnstile {
        transform: scale(1) !important;
        transform-origin: center center !important;
        margin: 0 auto !important;
      }
    `;
    document.head.appendChild(style);
  } catch (e) {}
})();
"""

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TurnstileWebView(
    onTokenExtracted: (String) -> Unit,
    onPageFinishedLoading: () -> Unit = {}
) {
    var tokenDelivered by remember { mutableStateOf(false) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(Color.TRANSPARENT)
                setLayerType(View.LAYER_TYPE_HARDWARE, null)

                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    javaScriptCanOpenWindowsAutomatically = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    useWideViewPort = false
                    loadWithOverviewMode = false
                    cacheMode = WebSettings.LOAD_DEFAULT

                    userAgentString =
                        "Mozilla/5.0 (Linux; Android 14; 2312DRA50I Build/CP2A.260605.016; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/130.0.0.0 Mobile Safari/537.36"
                }

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
                        // Early injection before turnstile executes
                        view?.evaluateJavascript(TURNSTILE_SETUP_JS, null)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onPageFinishedLoading()
                        view?.evaluateJavascript(TURNSTILE_STYLE_JS, null)
                        view?.evaluateJavascript(TURNSTILE_SETUP_JS, null)
                    }

                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        Log.e("Turnstile", "Error loading captcha: ${error?.description}")
                    }

                    override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                        Log.e("Turnstile", "HttpError: ${errorResponse?.statusCode}")
                    }
                }

                val captchaUrl = com.ghostreborn.akira.core.config.MkissaConfigProvider.currentEndpoints.captchaUrl
                loadUrl(captchaUrl, targetHeaders)
            }
        }
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CloudflareVerificationDialog(
    onTokenExtracted: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }

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
                color = MaterialTheme.colorScheme.surface
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
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        TurnstileWebView(
                            onTokenExtracted = onTokenExtracted,
                            onPageFinishedLoading = { isLoading = false }
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
