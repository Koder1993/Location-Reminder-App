package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.Locale

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    // Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap
    private var locationMarker: Marker? = null

    private var activityResultLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val areAllPermissionsGranted =
                it.all { entry -> entry.value } // if all permissions are granted
            if (areAllPermissionsGranted) {
                enableLocation()
            } else {
                Snackbar.make(
                    binding.root,
                    R.string.permission_denied_explanation, Snackbar.LENGTH_LONG
                ).show()
            }
        }

    companion object {
        private const val STREET_ZOOM_LEVEL = 18f
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val layoutId = R.layout.fragment_select_location
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        // get the SupportMapFragment instance
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.location_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.saveButton.setOnClickListener {
            onLocationSelected()
        }

        return binding.root
    }

    private fun onLocationSelected() {
        locationMarker?.let {
            _viewModel.latitude.value = it.position.latitude
            _viewModel.longitude.value = it.position.longitude
            _viewModel.reminderSelectedLocationStr.value = it.title
        }
        findNavController().popBackStack()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }

        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }

        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }

        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setMapMarkerOnLongClick(map)
        setPoiOnMapClick(map)
        enableLocation()
        // check if map style has to be added
    }

    private fun enableLocation() {
        if (checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true //location enabled since permissions are present
            val fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(requireContext())

            fusedLocationClient.lastLocation.addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val taskResult = task.result
                    taskResult?.run {
                        val latLng = LatLng(latitude, longitude)
                        map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                latLng, STREET_ZOOM_LEVEL
                            )
                        )
                        addMapMarker(latLng)
                    }
                } else {
                    Snackbar.make(
                        requireView(), R.string.location_required_error, Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            activityResultLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun setPoiOnMapClick(map: GoogleMap) {
        map.setOnPoiClickListener {
            locationMarker?.remove()
            locationMarker = map.addMarker(
                MarkerOptions().position(it.latLng).title(it.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            locationMarker?.showInfoWindow()
        }
    }

    private fun setMapMarkerOnLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener {
            addMapMarker(it)
        }
    }

    private fun addMapMarker(latLng: LatLng) {
        val snippet = String.format(
            Locale.getDefault(), "Lat: %1$.5f, Long: %2$.5f", latLng.latitude, latLng.longitude
        )
        locationMarker?.remove()
        locationMarker = map.addMarker(
            MarkerOptions().position(latLng).title(getString(R.string.dropped_pin)).snippet(snippet)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )
    }
}