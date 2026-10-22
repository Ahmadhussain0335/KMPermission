package io.github.ahmadhussain0335.kmppermission.sample

import io.github.ahmadhussain0335.kmppermission.Permission

/** One row in the demo screen — a human-readable label plus the [Permission]s it requests together. */
data class PermissionDemoItem(
    val title: String,
    val permissions: List<Permission>,
)

val demoItems: List<PermissionDemoItem> = listOf(
    PermissionDemoItem("Camera", listOf(Permission.Camera)),
    PermissionDemoItem("Microphone", listOf(Permission.Microphone)),
    PermissionDemoItem("Bluetooth", listOf(Permission.Bluetooth, Permission.BluetoothScan, Permission.BluetoothConnect)),
    PermissionDemoItem("Bluetooth Advertise", listOf(Permission.BluetoothAdvertise)),
    PermissionDemoItem("Nearby Wi-Fi", listOf(Permission.NearbyWifiDevices)),
    PermissionDemoItem("Location", listOf(Permission.LocationFine, Permission.LocationCoarse)),
    PermissionDemoItem("Background Location", listOf(Permission.LocationBackground)),
    PermissionDemoItem("Notifications", listOf(Permission.Notifications)),
    PermissionDemoItem("Contacts", listOf(Permission.Contacts)),
    PermissionDemoItem("Calendar", listOf(Permission.Calendar)),
    PermissionDemoItem("Storage", listOf(Permission.Storage)),
    PermissionDemoItem("Photos", listOf(Permission.Photos)),
    PermissionDemoItem("NFC", listOf(Permission.Nfc)),
)
