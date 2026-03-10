package com.example.testgeofenceplayservices

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.*
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.testgeofenceplayservices.data.GeofenceTriggerEventStore
import com.example.testgeofenceplayservices.ui.screens.BackgroundLocationPermissionDialog
import com.example.testgeofenceplayservices.ui.screens.GeofencingScreen
import com.example.testgeofenceplayservices.ui.theme.TestGeofencePlayServicesTheme
import com.example.testgeofenceplayservices.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val triggerEventUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != GeofenceTriggerEventStore.ACTION_TRIGGER_EVENT_UPDATED) return
            viewModel.onTriggerEventsUpdated()
        }
    }

    private val foregroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val hasFineLocation = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (!hasFineLocation) {
            viewModel.onForegroundPermissionDenied()
            return@registerForActivityResult
        }

        if (!hasBackgroundLocationPermission()) {
            viewModel.onBackgroundPermissionRequired()
            return@registerForActivityResult
        }

        if (viewModel.shouldStartAfterPermission()) {
            viewModel.startGeofencing()
        }
    }

    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onBackgroundPermissionResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.onAppStarted()

        setContent {
            TestGeofencePlayServicesTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GeofencingScreen(
                        statusText = viewModel.statusText,
                        playServicesVersionText = viewModel.playServicesVersionText,
                        geofenceDefinitions = viewModel.geofenceDefinitions,
                        triggerEvents = viewModel.triggerEvents,
                        isUserLocationEnabled = hasForegroundLocationPermission(),
                        onStartClick = { ensurePermissionsAndStartGeofencing() },
                        onClearEventsClick = { viewModel.clearTriggerEvents() },
                        modifier = Modifier.padding(innerPadding),
                    )

                    if (viewModel.showBackgroundPermissionDialog) {
                        BackgroundLocationPermissionDialog(
                            onGoToSettings = {
                                viewModel.onBackgroundDialogGoToSettings()
                                requestBackgroundLocationPermission()
                            },
                            onCancel = {
                                viewModel.onBackgroundDialogCancel()
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            triggerEventUpdateReceiver,
            IntentFilter(GeofenceTriggerEventStore.ACTION_TRIGGER_EVENT_UPDATED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        viewModel.onTriggerEventsUpdated()
    }

    override fun onStop() {
        runCatching { unregisterReceiver(triggerEventUpdateReceiver) }
        super.onStop()
    }

    private fun ensurePermissionsAndStartGeofencing() {
        viewModel.onStartGeofencingRequested()

        if (!hasForegroundLocationPermission()) {
            foregroundLocationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
            return
        }

        if (!hasBackgroundLocationPermission()) {
            viewModel.onBackgroundPermissionRequired()
            return
        }

        viewModel.startGeofencing()
    }

    private fun requestBackgroundLocationPermission() {
        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    private fun hasForegroundLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasBackgroundLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
}
