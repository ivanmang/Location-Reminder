package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import kotlinx.android.synthetic.main.fragment_reminders.*
import org.koin.android.ext.android.inject
import java.util.*

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_BACKGROUND_PERMISSION_RESULT_CODE = 35
private const val ZOOM_LEVEL = 18f

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var lastLocation: Location
    private val runningOreoOrBefore = Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1
    private val runningQ = Build.VERSION.SDK_INT == Build.VERSION_CODES.Q
    private val runningROrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    private var selectedPointOfInterest: PointOfInterest? = null
    private var selectedMarker: Marker? = null
    private lateinit var snippet: String
    private var snackbar: Snackbar? = null

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)


        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        onLocationSelected()

        return binding.root
    }

    private fun onLocationSelected() {
        binding.saveButton.setOnClickListener {
            selectedMarker?.let {
                if (selectedPointOfInterest != null) {
                    _viewModel.selectedPOI.value = selectedPointOfInterest
                    _viewModel.reminderSelectedLocationStr.value = selectedPointOfInterest!!.name
                } else {
                    _viewModel.reminderSelectedLocationStr.value = it.position.latitude.toString() + ", \n" + it.position.longitude.toString()
                }

                _viewModel.latitude.value = it.position.latitude
                _viewModel.longitude.value = it.position.longitude
                _viewModel.navigationCommand.value = NavigationCommand.Back
            }
        }
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
        updateCamera()
        setMapLongClick(map)
        setPoiClick(map)
        setMapStyle(map)
        enableMyLocation()
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            // A Snippet is Additional text that's displayed below the title.
            snippet = String.format(
                    Locale.getDefault(),
                    getString(R.string.lat_long_snippet),
                    latLng.latitude,
                    latLng.longitude
            )
            if (selectedMarker != null) {
                selectedMarker?.position = latLng
                selectedMarker?.title = getString(R.string.dropped_pin)
                selectedMarker?.snippet = snippet
                selectedPointOfInterest = null
            } else {
                selectedMarker = map.addMarker(
                        MarkerOptions()
                                .position(latLng)
                                .title(getString(R.string.dropped_pin))
                                .snippet(snippet)

                )
            }
            selectedMarker?.showInfoWindow()
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            snippet = poi.name
            if (selectedMarker != null) {
                selectedMarker?.position = poi.latLng
                selectedPointOfInterest = poi
                selectedMarker!!.snippet = snippet
                selectedMarker!!.title = poi.name
            } else {
                selectedMarker = map.addMarker(
                        MarkerOptions()
                                .position(poi.latLng)
                                .title(poi.name)
                                .snippet(snippet)
                )
            }
            selectedMarker?.showInfoWindow()

        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            map.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            context,
                            R.raw.map_style
                    )
            )
        } catch (e: Resources.NotFoundException) {

        }
    }


    @SuppressLint("MissingPermission")
    private fun updateCamera() {
        val locationProvider = LocationServices.getFusedLocationProviderClient(activity as Activity)
        if (isForegroundLocationGranted() && isBackgroundLocationGranted()) {
            val locationResult = locationProvider.lastLocation
            locationResult.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    lastLocation = task.result!!
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lastLocation.latitude, lastLocation.longitude), ZOOM_LEVEL))
                } else {
                    val defaultLocation = LatLng(37.422160, -122.084270)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, ZOOM_LEVEL))
                }
            }
        }
    }

    @SuppressLint("MissingPermission", "NewApi")
    private fun enableMyLocation() {
        if (isForegroundLocationGranted() && isBackgroundLocationGranted()) {
            map.isMyLocationEnabled = true
            snackbar?.dismiss()
            Log.i("permitted", "permitted")

        } else {
            var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            when {
                runningOreoOrBefore -> {
                    //Running API 27 or earlier
                    requestPermissions(
                            permissionsArray,
                            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
                    )
                }
                runningQ -> {
                    //Running API 29
                    permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    requestPermissions(
                            permissionsArray,
                            REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
                    )
                }
                runningROrLater -> {
                    //Running Api 30

                    //Only foreground permission granted
                    if (isForegroundLocationGranted()) {
                        AlertDialog.Builder(requireContext())
                            .setTitle(R.string.background_location_permission_title)
                            .setMessage(R.string.background_location_permission_message)
                            .setPositiveButton(R.string.yes) { _,_ ->
                                // this request will take user to Application's Setting page
                                requestPermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE)
                            }
                            .setNegativeButton(R.string.no) { dialog, _ ->
                                dialog.dismiss()
                            }.show()
                    } else {
                        // Only Background is granted
                        requestPermissions(
                                permissionsArray,
                                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
                        )
                    }

                }
                else -> {
                    requestPermissions(
                            //Running API 28
                            permissionsArray,
                            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
                    )
                }
            }
        }

    }



    private fun createSnackBarForPermission() {
        snackbar = Snackbar.make(
                binding.activityMap,
                R.string.permission_denied_explanation, Snackbar.LENGTH_LONG
        )
                .setAction(R.string.settings) {
                    // Displays App settings screen.
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }
        snackbar!!.show()

    }


    private fun isForegroundLocationGranted(): Boolean {
        return PackageManager.PERMISSION_GRANTED ==
                ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun isBackgroundLocationGranted(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            return true
        }
    }


    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray) {
        // Check if location permissions are granted and if so enable the
        // location data layer.
        if ((grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            enableMyLocation()
            map.clear()
            Log.i("location", "permitted")

        } else {
            createSnackBarForPermission()
        }
        updateCamera()
    }


}
