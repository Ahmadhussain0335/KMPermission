package com.kmp.permission.internal

import android.os.Build
import com.kmp.permission.Permission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun bluetoothConnect_isAutoGranted_withNoManifestEntry_belowApi31() {
        assertTrue(AndroidPermissionMapper.isAutoGranted(Permission.BluetoothConnect, sdkInt = 30))
        assertTrue(AndroidPermissionMapper.manifestPermissions(Permission.BluetoothConnect, sdkInt = 30).isEmpty())
    }

    @Test
    fun bluetoothScan_requiresFineLocation_onApi29And30() {
        assertFalse(AndroidPermissionMapper.isAutoGranted(Permission.BluetoothScan, sdkInt = 30))
        assertEquals(listOf("android.permission.ACCESS_FINE_LOCATION"), AndroidPermissionMapper.manifestPermissions(Permission.BluetoothScan, sdkInt = 30))
        assertEquals(listOf("android.permission.ACCESS_FINE_LOCATION"), AndroidPermissionMapper.manifestPermissions(Permission.BluetoothScan, sdkInt = Build.VERSION_CODES.Q))
    }

    @Test
    fun bluetoothScan_requiresCoarseLocation_belowApi29() {
        assertEquals(listOf("android.permission.ACCESS_COARSE_LOCATION"), AndroidPermissionMapper.manifestPermissions(Permission.BluetoothScan, sdkInt = 28))
        assertEquals(listOf("android.permission.ACCESS_COARSE_LOCATION"), AndroidPermissionMapper.manifestPermissions(Permission.BluetoothScan, sdkInt = 26))
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
    fun bluetooth_isNeverAutoGranted() {
        assertFalse(AndroidPermissionMapper.isAutoGranted(Permission.Bluetooth, sdkInt = 28))
        assertFalse(AndroidPermissionMapper.isAutoGranted(Permission.Bluetooth, sdkInt = 30))
        assertFalse(AndroidPermissionMapper.isAutoGranted(Permission.Bluetooth, sdkInt = Build.VERSION_CODES.S))
    }

    @Test
    fun bluetooth_requiresScanAndConnect_fromApi31() {
        assertEquals(
            listOf("android.permission.BLUETOOTH_SCAN", "android.permission.BLUETOOTH_CONNECT"),
            AndroidPermissionMapper.manifestPermissions(Permission.Bluetooth, sdkInt = Build.VERSION_CODES.S),
        )
    }

    @Test
    fun bluetooth_requiresLegacyPermissionsAndScanLocation_belowApi31() {
        assertEquals(
            listOf("android.permission.BLUETOOTH", "android.permission.BLUETOOTH_ADMIN", "android.permission.ACCESS_FINE_LOCATION"),
            AndroidPermissionMapper.manifestPermissions(Permission.Bluetooth, sdkInt = 30),
        )
        assertEquals(
            listOf("android.permission.BLUETOOTH", "android.permission.BLUETOOTH_ADMIN", "android.permission.ACCESS_COARSE_LOCATION"),
            AndroidPermissionMapper.manifestPermissions(Permission.Bluetooth, sdkInt = 28),
        )
    }

    @Test
    fun camera_isNeverAutoGranted() {
        assertFalse(AndroidPermissionMapper.isAutoGranted(Permission.Camera, sdkInt = 26))
        assertFalse(AndroidPermissionMapper.isAutoGranted(Permission.Camera, sdkInt = 36))
    }

    @Test
    fun storage_isAutoGranted_withNoManifestEntry_fromApi33() {
        assertTrue(AndroidPermissionMapper.isAutoGranted(Permission.Storage, sdkInt = Build.VERSION_CODES.TIRAMISU))
        assertTrue(AndroidPermissionMapper.manifestPermissions(Permission.Storage, sdkInt = Build.VERSION_CODES.TIRAMISU).isEmpty())
    }

    @Test
    fun storage_requiresReadExternalStorage_belowApi33() {
        assertFalse(AndroidPermissionMapper.isAutoGranted(Permission.Storage, sdkInt = 32))
        assertEquals(
            listOf("android.permission.READ_EXTERNAL_STORAGE"),
            AndroidPermissionMapper.manifestPermissions(Permission.Storage, sdkInt = 32),
        )
    }

    @Test
    fun locationFine_alsoRequestsCoarse_fromApi31() {
        assertEquals(
            listOf("android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"),
            AndroidPermissionMapper.manifestPermissions(Permission.LocationFine, sdkInt = Build.VERSION_CODES.S),
        )
        assertEquals(
            listOf("android.permission.ACCESS_COARSE_LOCATION"),
            AndroidPermissionMapper.partialGrantPermissions(Permission.LocationFine, sdkInt = Build.VERSION_CODES.S),
        )
    }

    @Test
    fun locationFine_requestsOnlyFine_belowApi31() {
        assertEquals(
            listOf("android.permission.ACCESS_FINE_LOCATION"),
            AndroidPermissionMapper.manifestPermissions(Permission.LocationFine, sdkInt = 30),
        )
        assertTrue(AndroidPermissionMapper.partialGrantPermissions(Permission.LocationFine, sdkInt = 30).isEmpty())
    }

    @Test
    fun locationBackground_isCoveredByFineLocation_belowApi29() {
        assertEquals(
            listOf("android.permission.ACCESS_FINE_LOCATION"),
            AndroidPermissionMapper.manifestPermissions(Permission.LocationBackground, sdkInt = 28),
        )
        assertFalse(AndroidPermissionMapper.requiresSeparateRequest(Permission.LocationBackground, sdkInt = 28))
    }

    @Test
    fun locationBackground_isRequestedSeparately_fromApi29() {
        assertEquals(
            listOf("android.permission.ACCESS_BACKGROUND_LOCATION"),
            AndroidPermissionMapper.manifestPermissions(Permission.LocationBackground, sdkInt = Build.VERSION_CODES.Q),
        )
        assertTrue(AndroidPermissionMapper.requiresSeparateRequest(Permission.LocationBackground, sdkInt = Build.VERSION_CODES.Q))
    }

    @Test
    fun photos_offersSelectedPhotosAccess_fromApi34() {
        val sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE

        assertEquals(listOf("android.permission.READ_MEDIA_VISUAL_USER_SELECTED"), AndroidPermissionMapper.optionalManifestPermissions(Permission.Photos, sdkInt))
        assertEquals(listOf("android.permission.READ_MEDIA_VISUAL_USER_SELECTED"), AndroidPermissionMapper.partialGrantPermissions(Permission.Photos, sdkInt))
    }

    @Test
    fun photos_hasNoSelectedPhotosAccess_belowApi34() {
        assertTrue(AndroidPermissionMapper.optionalManifestPermissions(Permission.Photos, sdkInt = Build.VERSION_CODES.TIRAMISU).isEmpty())
    }
}
