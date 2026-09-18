package com.example.tools.builtin

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import com.example.security.model.PermissionLevel
import com.example.tools.contract.NousTool
import com.example.tools.model.ToolDefinition
import com.example.tools.model.ToolExecutionResult
import com.example.tools.model.VerificationStatus

class DeviceStatusTool(private val context: Context) : NousTool {
    override val definition = ToolDefinition(
        id = "device_status",
        name = "Device Status & Telemetry",
        description = "Read-only query of battery percentage, charging state, network connectivity, and available internal storage.",
        permissionLevel = PermissionLevel.L0_READ_ONLY
    )

    override suspend fun execute(parameters: Map<String, Any>): ToolExecutionResult {
        // Battery status
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0
        val isCharging = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING

        // Network connectivity
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val networkType = when {
            isWifi -> "Wi-Fi"
            isCellular -> "Cellular LTE/5G"
            hasInternet -> "Other Active Network"
            else -> "Disconnected / Offline"
        }

        // Storage status
        val stat = StatFs(Environment.getDataDirectory().path)
        val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        val freeGb = String.format("%.2f", freeBytes / (1024.0 * 1024.0 * 1024.0))
        val totalGb = String.format("%.2f", totalBytes / (1024.0 * 1024.0 * 1024.0))

        val output = """
            [SYSTEM TELEMETRY REPORT]
            • Battery: $batteryPct% (Charging: $isCharging)
            • Network: $networkType (Internet Access: $hasInternet)
            • Internal Storage: $freeGb GB free of $totalGb GB
        """.trimIndent()

        return ToolExecutionResult(
            success = true,
            output = output,
            verificationStatus = VerificationStatus.VERIFIED_VALID,
            verificationEvidence = "Battery: $batteryPct%, Network: $networkType, Storage: ${freeGb}GB"
        )
    }

    override suspend fun verify(
        parameters: Map<String, Any>,
        executionResult: ToolExecutionResult
    ): ToolExecutionResult {
        return executionResult
    }
}
