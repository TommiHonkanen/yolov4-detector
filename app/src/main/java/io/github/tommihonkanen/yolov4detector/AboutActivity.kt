package io.github.tommihonkanen.yolov4detector

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.tommihonkanen.yolov4detector.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
    }
    
    private fun setupUI() {
        // Configure toolbar
        binding.toolbar.setNavigationOnClickListener {
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_down)
        }
        
        // Set version name
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            binding.versionText.text = "Version ${packageInfo.versionName}"
        } catch (e: Exception) {
            binding.versionText.text = "Version 1.0.0"
        }
        
        // Link clicks
        binding.darknetLink.setOnClickListener {
            openUrl("https://www.ccoderun.ca/programming/darknet_faq")
        }
        
        binding.opencvLink.setOnClickListener {
            openUrl("https://opencv.org/")
        }
        
        binding.darknetRepoLink.setOnClickListener {
            openUrl("https://github.com/hank-ai/darknet")
        }

        binding.yolov4DetectorLink.setOnClickListener {
            openUrl("https://github.com/TommiHonkanen/yolov4-detector")
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