package com.kmp.permission.internal

import android.os.Build
import com.kmp.permission.Permission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidPermissionMapperTest {

    @Test
    fun photos_fallsBackToExternalStorage_belowApi33() {
        val permissions = AndroidPermissionMapper.manifestPermissions(Permission.Photos, sdkInt = 32)

        assertEquals(listOf("android.permission.READ_EXTERNAL_STORAGE"), permissions)
    }

    @Test
    fun photos_usesMediaPermissions_fromApi33() {
        val permissions = AndroidPermissionMapper.manifestPermissions(Permission.Photos, sdkInt = Build.VERSION_CODES.TIRAMISU)

        assertEquals(
            listOf("android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO"),
            permissions,
        )
    }

    @Test
    fun bluetoothScanAndConnect_requireNoManifestEntry_belowApi31() {
        assertTrue(AndroidPermissionMapper.manifestPermissions(Permission.BluetoothScan, sdkInt = 30).isEmpty())
        assertTrue(AndroidPermissionMapper.manifestPermissions(Permission.BluetoothConnect, sdkInt = 30).isEmpty())
    }

    @Test
    fun bluetoothScanAndConnect_requireManifestEntry_fromApi31() {
        val sdkInt = Build.VERSION_CODES.S

        assertEquals(listOf("android.permission.BLUETOOTH_SCAN"), AndroidPermissionMapper.manifestPermissions(Permission.BluetoothScan, sdkInt))
        assertEquals(listOf("android.permission.BLUETOOTH_CONNECT"), AndroidPermissionMapper.manifestPermissions(Permission.BluetoothConnect, sdkInt))
    }

    @Test
    fun notifications_requireNoManifestEntry_belowApi33() {
        assertTrue(AndroidPermissionMapper.manifestPermissions(Permission.Notifications, sdkInt = 32).isEmpty())
    }

    @Test
    fun notifications_requireManifestEntry_fromApi33() {
        val permissions = AndroidPermissionMapper.manifestPermissions(Permission.Notifications, sdkInt = Build.VERSION_CODES.TIRAMISU)

        assertEquals(listOf("android.permission.POST_NOTIFICATIONS"), permissions)
    }

    @Test
    fun isAutoGrantedBelowRuntime_isTrue_forLegacyBluetooth_onAnySdk() {
        assertTrue(AndroidPermissionMapper.isAutoGrantedBelowRuntime(Permission.Bluetooth, sdkInt = 21))
        assertTrue(AndroidPermissionMapper.isAutoGrantedBelowRuntime(Permission.Bluetooth, sdkInt = 36))
    }

    @Test
    fun isAutoGrantedBelowRuntime_isFalse_forCameraOnAnySdk() {
        assertTrue(!AndroidPermissionMapper.isAutoGrantedBelowRuntime(Permission.Camera, sdkInt = 21))
        assertTrue(!AndroidPermissionMapper.isAutoGrantedBelowRuntime(Permission.Camera, sdkInt = 36))
    }
}
