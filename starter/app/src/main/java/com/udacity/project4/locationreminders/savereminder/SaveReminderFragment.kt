package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity.RESULT_OK
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {

    // Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geoClient: GeofencingClient
    private lateinit var reminderDataItem: ReminderDataItem

    companion object {
        const val TAG = "SaveReminderFragment"
        private const val GEOFENCE_RADIUS_METERS = 100f
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private var activityResultLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val areAllPermissionsGranted =
                it.all { entry -> entry.value } // if all permissions are granted
            if (areAllPermissionsGranted) {
                checkRequiredPermissions()
            } else {
                Snackbar.make(
                    binding.root, R.string.permission_denied_explanation, Snackbar.LENGTH_LONG
                ).show()
            }
        }

    private var locationLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
            if (res.resultCode == RESULT_OK) {
                checkNotificationPermissionForGeofence()
            }
        }

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            Log.d(TAG, "requestNotificationPermission, isGranted $isGranted")
            if (isGranted) {
                createGeoFenceForLocation()
            } else {
                Snackbar.make(
                    binding.root, R.string.error_notification_permission, Snackbar.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val layoutId = R.layout.fragment_save_reminder
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = _viewModel

        geoClient = LocationServices.getGeofencingClient(requireActivity())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            val directions =
                SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment()
            _viewModel.navigationCommand.value = NavigationCommand.To(directions)
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            reminderDataItem = ReminderDataItem(title, description, location, latitude, longitude)

            if (_viewModel.validateEnteredData(reminderDataItem)) {
                checkRequiredPermissions()
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun checkRequiredPermissions() {
        val isFineLocationPermissionGranted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val isCoarseLocationPermissionGranted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val areForegroundPermissionsGranted =
            isCoarseLocationPermissionGranted && isFineLocationPermissionGranted
        val isBackgroundLocationPermissionGranted = checkBackgroundLocationPermission()

        Log.d(
            TAG,
            "isFineLocationPermissionGranted: $isFineLocationPermissionGranted" +
                    "\nisCoarseLocationPermissionGranted: $isCoarseLocationPermissionGranted" +
                    "\nisBackgroundLocationPermissionGranted: $isBackgroundLocationPermissionGranted"
        )

        if (areForegroundPermissionsGranted && isBackgroundLocationPermissionGranted) {
            checkDeviceLocationSettings()
        } else if (areForegroundPermissionsGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                activityResultLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                )
            }
        } else {
            val permissionsArray = arrayOf<String>()
            if (!isFineLocationPermissionGranted) permissionsArray[0] =
                Manifest.permission.ACCESS_FINE_LOCATION
            if (!isCoarseLocationPermissionGranted) permissionsArray[0] =
                Manifest.permission.ACCESS_COARSE_LOCATION
            activityResultLauncher.launch(permissionsArray)
        }
    }

    private fun checkDeviceLocationSettings() {
        val locationRequest =
            LocationRequest.create().apply { priority = LocationRequest.PRIORITY_LOW_POWER }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d(TAG, "locationSettingsResponseTask: successful")
                checkNotificationPermissionForGeofence()
            }
        }

        locationSettingsResponseTask.addOnFailureListener { exception ->
            Log.d(TAG, "locationSettingsResponseTask: ${exception.message}")
            if (exception is ResolvableApiException) {
                try {
                    val request = IntentSenderRequest.Builder(exception.resolution).build()
                    locationLauncher.launch(request)
                } catch (exception: IntentSender.SendIntentException) {
                    Log.d(TAG, "Location settings error: ${exception.message}")
                }
            } else {
                Snackbar.make(
                    requireView(), R.string.location_required_error, Snackbar.LENGTH_LONG
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettings()
                }.show()
            }
        }
    }

    private fun createGeoFenceForLocation() {
        val geofence = Geofence.Builder()
            .setRequestId(reminderDataItem.id)
            .setCircularRegion(
                reminderDataItem.latitude!!,
                reminderDataItem.longitude!!,
                GEOFENCE_RADIUS_METERS
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        geoClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                _viewModel.validateAndSaveReminder(reminderDataItem)
            }
            addOnFailureListener {
                Toast.makeText(requireContext(), R.string.geofences_not_added, Toast.LENGTH_SHORT)
                    .show()
                _viewModel.showSnackBarInt.value = R.string.error_adding_geofence
            }
        }
    }

    private fun checkNotificationPermissionForGeofence() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "checkNotificationPermissionForGeofence, permission granted")
                createGeoFenceForLocation()
            } else {
                Log.d(TAG, "checkNotificationPermissionForGeofence, requesting permission")
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            createGeoFenceForLocation()
        }
    }

    private fun checkBackgroundLocationPermission() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}