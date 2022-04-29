package com.bitpunchlab.android.blechat_android

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.blechat_android.base.GenericListener
import com.bitpunchlab.android.blechat_android.chat.ChatServiceManager
import com.bitpunchlab.android.blechat_android.databinding.FragmentMainBinding
import com.bitpunchlab.android.blechat_android.deviceList.DeviceListAdapter
import com.bitpunchlab.android.blechat_android.deviceList.DeviceListener
import com.bitpunchlab.android.blechat_android.deviceList.DeviceViewModel
import com.bitpunchlab.android.blechat_android.deviceList.DeviceViewModelFactory


class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var deviceAdapter: DeviceListAdapter
    private lateinit var deviceViewModel: DeviceViewModel
    private var connectDevice: BluetoothDevice? = null
    //private var bluetoothAdapter: BluetoothAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentMainBinding.inflate(inflater, container, false)

        deviceViewModel = ViewModelProvider(requireActivity(),
            DeviceViewModelFactory(requireActivity().application))
            .get(DeviceViewModel::class.java)
        deviceAdapter = DeviceListAdapter(DeviceListener { device ->
            // show an alert to confirm user wants to connect to the chosen device
            connectDeviceAlert(device)
            deviceViewModel.onDeviceClicked(device)
        })

        binding.devicesRecycler.adapter = deviceAdapter

        deviceViewModel.deviceList.observe(viewLifecycleOwner, Observer { deviceList ->
            if (!deviceList.isNullOrEmpty()) {
                deviceAdapter.submitList(deviceList)
                deviceAdapter.notifyDataSetChanged()
            }
        })

        deviceViewModel.chosenDevice.observe(viewLifecycleOwner, Observer { device ->
            device?.let {
                // connect to the chosen device
                deviceViewModel.finishNavigationOfDevice()
            }
        })

        binding.scanButton.setOnClickListener {
            deviceViewModel.scanLeDevice()
        }

        binding.startServerButton.setOnClickListener {
            ChatServiceManager.startChatServer(requireActivity().application)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("MissingPermission")
    private fun connectDeviceAlert(device: BluetoothDevice) {
        val connectAlert = AlertDialog.Builder(context)
        var identity = ""

        if (!device.name.isNullOrBlank()) {
            identity = device.name
        } else if (!device.address.isNullOrBlank()) {
            identity = "device with the address ${device.address}"
        }

        connectAlert.setTitle(getString(R.string.connect_device_alert_title))
        connectAlert.setMessage("Do you want to connect to ${identity}")
        connectAlert.setPositiveButton(getString(R.string.confirm_button),
            DialogInterface.OnClickListener() { dialog, button ->
                // connect to the device
            })
        connectAlert.setNegativeButton(getString(R.string.cancel_button),
            DialogInterface.OnClickListener() { dialog, button ->
                // do nothing, and let user choose another action
            })
        connectAlert.show()
    }
}

