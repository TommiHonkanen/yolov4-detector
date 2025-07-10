package io.github.tommihonkanen.yolov4detector

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import io.github.tommihonkanen.yolov4detector.ui.screens.AboutScreen
import io.github.tommihonkanen.yolov4detector.ui.theme.YoloDetectorTheme

class AboutActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "1.1.0"
        }
        
        setContent {
            YoloDetectorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AboutScreen(
                        versionName = versionName,
                        onBackClick = {
                            finish()
                            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_down)
                        },
                        onLinkClick = { url ->
                            openUrl(url)
                        }
                    )
                }
            }
        }
    }
    
    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_down)
    }
}