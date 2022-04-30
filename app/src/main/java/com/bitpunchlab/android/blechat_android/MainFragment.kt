package com.bitpunchlab.android.blechat_android

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bitpunchlab.android.blechat_android.chatService.ChatServiceClient
import com.bitpunchlab.android.blechat_android.chatService.ChatServiceManager
import com.bitpunchlab.android.blechat_android.databinding.FragmentMainBinding
import com.bitpunchlab.android.blechat_android.deviceList.DeviceListAdapter
import com.bitpunchlab.android.blechat_android.deviceList.DeviceListener
import com.bitpunchlab.android.blechat_android.deviceList.DeviceViewModel
import com.bitpunchlab.android.blechat_android.deviceList.DeviceViewModelFactory
import com.bitpunchlab.android.blechat_android.models.MessageModel

private const val TAG = "MainFragment"

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var deviceAdapter: DeviceListAdapter
    private lateinit var deviceViewModel: DeviceViewModel
    private var connectDevice: BluetoothDevice? = null
    //private var messageList: List<MessageModel>
    //private var bluetoothAdapter: BluetoothAdapter? = null

    @SuppressLint("MissingPermission")
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
            Log.i(TAG, "device clicked, name ${device!!.name}")
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

        ChatServiceManager.isServerRunning.observe(viewLifecycleOwner, Observer { value ->
            if (value == null || value == false) {
                binding.startServerButton.text = "Start Server"
            } else {
                binding.startServerButton.text = "Stop Server"
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
                deviceViewModel.connectToDevice(device)
            })
        connectAlert.setNegativeButton(getString(R.string.cancel_button),
            DialogInterface.OnClickListener() { dialog, button ->
                // do nothing, and let user choose another action
            })
        connectAlert.show()
    }
}

