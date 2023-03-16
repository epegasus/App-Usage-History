package dev.pegasus.appusagehistory

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.UserManager
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import dev.pegasus.appusagehistory.databinding.ActivityMainBinding
import dev.pegasus.appusagehistory.utils.GeneralUtils.showToast


class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val usageStatsAdapter by lazy { UsageStatsAdapter() }
    private val usageStatsManager by lazy { getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager }

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        fetchAppUsage()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initRecyclerView()
        fetchAppUsage()

        binding.mbRequestPermission.setOnClickListener { requestForPermission() }
    }

    private fun initRecyclerView() {
        binding.rvList.adapter = usageStatsAdapter
    }

    private fun fetchAppUsage() {
        if (!checkUsageStatsPermission()) {
            showToast("Permission Not Granted")
            binding.mbRequestPermission.visibility = View.VISIBLE
            return
        }
        binding.mbRequestPermission.visibility = View.GONE
        checkUserDeviceUnlocked()
    }

    /**
     * @suppress handled
     */
    @Suppress("DEPRECATION")
    private fun checkUsageStatsPermission(): Boolean {
        val appOpsManager = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            appOpsManager.unsafeCheckOpNoThrow("android:get_usage_stats", Process.myUid(), packageName)
        else
            appOpsManager.checkOpNoThrow("android:get_usage_stats", Process.myUid(), packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun checkUserDeviceUnlocked() {
        val userManager = getSystemService(Context.USER_SERVICE) as UserManager
        val userUnlocked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            userManager.isUserUnlocked
        } else true

        if (!userUnlocked) {
            binding.mbRequestPermission.visibility = View.VISIBLE
            showToast("User device is not unlocked")
            return
        }
        binding.mbRequestPermission.visibility = View.GONE
        //queryAppUsageState()
        getNonSystemAppsList()
    }

    private fun queryAppUsageState() {
        val arrayList = ArrayList<String>()
        val currentTime = System.currentTimeMillis()
        val startTime = currentTime - (1000 * 60 * 10)      // 10 minutes ago
        val usageEvents = usageStatsManager.queryEvents(startTime, currentTime)
        val usageEvent = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(usageEvent)
            arrayList.add(" Package - ${usageEvent.packageName} Time -  ${usageEvent.timeStamp}")
        }
        usageStatsAdapter.submitList(arrayList.toList())
    }

    private fun getNonSystemAppsList() {
        val arrayList = ArrayList<String>()
        val appInfoList = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        for (appInfo in appInfoList) {
            if (appInfo.flags != ApplicationInfo.FLAG_SYSTEM) {
                //val temp = "PackageName: ${appInfo.packageName} ---- ${packageManager.getApplicationLabel(appInfo)}"
                val temp = packageManager.getApplicationLabel(appInfo).toString()
                arrayList.add(temp)
            }
        }
        arrayList.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
        usageStatsAdapter.submitList(arrayList.toList())
    }

    private fun requestForPermission() {
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            resultLauncher.launch(this)
        }
    }

    /**
     * Steps:
     *      1 - Add permission in AndroidManifest.xml file
     *      2 - Check for special permission
     *      3 - Ask if not permitted
     *      4 - Check if device is unlocked
     *      5 - Retrieve Usage Events
     *      6 - Filter User-Installed Apps
     */
}