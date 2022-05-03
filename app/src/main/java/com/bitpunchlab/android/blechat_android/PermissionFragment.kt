package com.bitpunchlab.android.blechat_android

import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.blechat_android.databinding.FragmentPermissionBinding
import android.Manifest
import android.content.pm.PackageManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.bitpunchlab.android.blechat_android.R


private const val TAG = "PermissionFragment"

// in this fragment, I'll get all the permissions I need
// they are coarse and fine location, external write, bluetooth permissions
// I'll first request the bluetooth permission, if it is granted,
// I'll ask for location permission
// until then the user can navigate to the home screen
class PermissionFragment : Fragment() {

    private var _binding: FragmentPermissionBinding? = null
    private val binding get() = _binding!!

    private var bluetoothAdapter: BluetoothAdapter? = null
    // whenever bluetooth is enable, I'll check location permission
    private var isBleEnabled = MutableLiveData<Boolean>(false)

    private val requestBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK)  {
                    Log.i(TAG, "bluetooth enabled")
                    // now, I need to check the location permission
                    isBleEnabled.value = true
                    //checkLocationPermission()
                } else {
                    Log.i(TAG, "bluetooth is not enabled")
                    // present an alert
                    // then request again
                    bluetoothAlert()
                }
        }

    private val requestLocationLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        isGranted: Boolean ->
        if (isGranted) {
            Log.i(TAG, "location permission granted")
            findNavController().navigate(R.id.action_PermissionFragment_to_MainFragment)
        } else {
            Log.i(TAG, "request permission")
            locationPermissionAlert()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentPermissionBinding.inflate(inflater, container, false)

        val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        isBleEnabled.observe(viewLifecycleOwner, Observer { enabled ->
            if (enabled) {
                checkLocationPermission()
            }
        })

        checkBluetoothEnabled()

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_PermissionFragment_to_MainFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkBluetoothEnabled() {
        if (bluetoothAdapter?.isEnabled == false) {
            Log.i(TAG, "checking bluetooth")
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetoothLauncher.launch(enableBluetoothIntent)
        } else {
            isBleEnabled.value = true
        }
    }

    private fun bluetoothAlert() {
        val bleAlert = AlertDialog.Builder(context)

        bleAlert.setTitle(getString(R.string.bluetooth_enable_alert_title))
        bleAlert.setMessage(getString(R.string.bluetooth_enable_alert_desc))
        bleAlert.setPositiveButton(getString(R.string.ok_button),
        DialogInterface.OnClickListener() { dialog, button ->
            checkBluetoothEnabled()
        })
        bleAlert.setNegativeButton(getString(R.string.cancel_button),
        DialogInterface.OnClickListener() { dialog, button ->
            // do nothing, and let user stay in permission fragment
        })
        bleAlert.show()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Location permission granted")
            // navigate to the Main Fragment
            findNavController().navigate(R.id.action_PermissionFragment_to_MainFragment)
        } else {
            // request location permission
            requestLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun locationPermissionAlert() {
        val locationAlert = AlertDialog.Builder(context)

        locationAlert.setTitle(getString(R.string.location_permission_alert_title))
        locationAlert.setMessage(getString(R.string.bluetooth_enable_alert_desc))
        locationAlert.setPositiveButton(getString(R.string.ok_button),
            DialogInterface.OnClickListener() { dialog, button ->
                checkBluetoothEnabled()
            })
        locationAlert.setNegativeButton(getString(R.string.cancel_button),
            DialogInterface.OnClickListener() { dialog, button ->
                // do nothing, and let user stay in permission fragment
            })
        locationAlert.show()
    }
}