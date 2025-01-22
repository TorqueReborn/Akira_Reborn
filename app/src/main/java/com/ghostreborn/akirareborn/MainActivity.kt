package com.ghostreborn.akirareborn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ghostreborn.akirareborn.ui.theme.AkiraRebornTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AkiraRebornTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        FetchAndDisplayAnimeData()
                    }
                }
            }
        }
    }
}

@Composable
fun FetchAndDisplayAnimeData() {
    var animeData by remember { mutableStateOf("") }

    val variables = "\"search\":{\"allowAdult\":false,\"allowUnknown\":false,\"query\":\"One\"},\"limit\":18,\"page\":1,\"translationType\":\"sub\",\"countryOrigin\":\"JP\""
    val queryTypes = "\$search:SearchInput,\$limit:Int,\$page:Int,\$translationType:VaildTranslationTypeEnumType,\$countryOrigin:VaildCountryOriginEnumType"
    val query = "shows(search:\$search,limit:\$limit,page:\$page,translationType:\$translationType,countryOrigin:\$countryOrigin){edges{_id,name,thumbnail}}"

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val url = "https://api.allanime.day/api?variables={$variables}&query=query($queryTypes){$query}"
            val request = Request.Builder()
                .url(url)
                .header("Referer", "https://allanime.to")
                .header("Cipher", "AES256-SHA256")
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/121.0"
                )
                .build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    animeData = response.body?.string() ?: ""
                } else {
                    animeData = "Error: ${response.code}"
                }
            } catch (e: Exception) {
                animeData = "Error fetching data: ${e.message}"
            }
        }
    }

    Text(text = animeData)
}