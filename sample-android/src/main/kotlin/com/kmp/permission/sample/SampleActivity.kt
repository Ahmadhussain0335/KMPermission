package com.kmp.permission.sample.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kmp.permission.PermissionManager

class SampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val permissionManager = PermissionManager(this)

        setContent {
            MaterialTheme {
                PermissionDemoScreen(onOpenSettings = permissionManager::openSettings)
            }
        }
    }
}

@Preview
@Composable
private fun SampleAppPreview() {
    MaterialTheme {
        PermissionDemoScreen(onOpenSettings = {})
    }
}
