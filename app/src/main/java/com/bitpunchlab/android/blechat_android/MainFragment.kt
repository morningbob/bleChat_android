package com.bitpunchlab.android.blechat_android

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import com.bitpunchlab.android.blechat_android.ConnectionState
import com.bitpunchlab.android.blechat_android.R
import com.bitpunchlab.android.blechat_android.chatService.ChatServiceClient
import com.bitpunchlab.android.blechat_android.chatService.ChatServiceManager
import com.bitpunchlab.android.blechat_android.database.BLEDatabase
import com.bitpunchlab.android.blechat_android.databinding.DeviceListBinding
import com.bitpunchlab.android.blechat_android.databinding.FragmentMainBinding
import com.bitpunchlab.android.blechat_android.deviceList.DeviceListAdapter
import com.bitpunchlab.android.blechat_android.deviceList.DeviceListener
import com.bitpunchlab.android.blechat_android.deviceList.DeviceViewModel
import com.bitpunchlab.android.blechat_android.deviceList.DeviceViewModelFactory
import com.bitpunchlab.android.blechat_android.models.MessageModel
import kotlinx.coroutines.InternalCoroutinesApi

private const val TAG = "MainFragment"

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var deviceAdapter: DeviceListAdapter
    private lateinit var deviceViewModel: DeviceViewModel
    private var connectDevice: BluetoothDevice? = null
    private lateinit var database: BLEDatabase
    private var deviceBinding: DeviceListBinding? = null
    private var appStateHistory = ArrayList<ConnectionState>()
    //private var bluetoothAdapter: BluetoothAdapter? = null

    @OptIn(InternalCoroutinesApi::class)
    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        appStateHistory.add(ConnectionState.STATE_NONE)

        setHasOptionsMenu(true)
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        deviceBinding = binding.deviceLayout

        database = BLEDatabase.getInstance(context)

        deviceViewModel = ViewModelProvider(requireActivity(),
            DeviceViewModelFactory(requireActivity().application))
            .get(DeviceViewModel::class.java)
        deviceAdapter = DeviceListAdapter(DeviceListener { device ->
            // show an alert to confirm user wants to connect to the chosen device
            Log.i(TAG, "device clicked, name ${device!!.name}")

            deviceViewModel.onDeviceClicked(device)
            connectDeviceAlert(device)
        })

        deviceBinding!!.devicesRecycler.adapter = deviceAdapter

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
                binding.startServerButton.text = getString(R.string.start_server)
                // here we also ask if the user wants to start a server to listen to the
                // incoming connections
                startServerAlert()
            } else {
                binding.startServerButton.text = getString(R.string.stop_server)
            }
        })

        ChatServiceManager.connectionState.observe(viewLifecycleOwner, Observer { state ->
            Log.i("connection state changed", "state: ${state}")
            Log.i("appStateHistory, state: ", appStateHistory.last().toString())
            // appStateHistory is used to prevent the incoming connection alert to be shown
            // whenever the connection state is connected, sometimes
            if (state == ConnectionState.STATE_CONNECTED) {
                // ask user if he accepts the incoming connection
                if (appStateHistory.last() == ConnectionState.STATE_CONNECTING ||
                        appStateHistory.last() == ConnectionState.STATE_NONE ||
                        appStateHistory.last() == ConnectionState.STATE_DISCONNECTED) {
                    incomingConnectionAlert(ChatServiceManager.connectedDevice!!)
                }

            } else if (state == ConnectionState.STATE_DISCONNECTED) {
                // alert user
                appStateHistory.add(state)
                Log.i("connection state", "added disconnection")
            } else if (state == ConnectionState.STATE_CONNECTING) {
                appStateHistory.add(state)
                Log.i("connection state", "added connecting")
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
        deviceBinding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return NavigationUI.onNavDestinationSelected(item,
            requireView().findNavController())
                || super.onOptionsItemSelected(item)
    }

    // this is the client side to navigate to the chat fragment
    @SuppressLint("MissingPermission")
    private fun connectDeviceAlert(device: BluetoothDevice) {
        val connectAlert = AlertDialog.Builder(context)
        var identity = ""

        if (!device.name.isNullOrBlank()) {
            identity = device.name
        } else if (!device.address.isNullOrBlank()) {
            identity = "device with the address ${device.address}"
        }
        connectAlert.setCancelable(false)
        connectAlert.setTitle(getString(R.string.connect_device_alert_title))
        connectAlert.setMessage("Do you want to connect to ${identity}")
        connectAlert.setPositiveButton(getString(R.string.confirm_button),
            DialogInterface.OnClickListener() { dialog, button ->
                // connect to the device
                deviceViewModel.connectToDevice(device)
                val bundle = Bundle()
                bundle.putBoolean("isClient", true)
                bundle.putString("deviceName", device.name)
                bundle.putString("deviceAddress", device.address)
                findNavController().navigate(R.id.action_MainFragment_to_chatFragment, bundle)
            })
        connectAlert.setNegativeButton(getString(R.string.cancel_button),
            DialogInterface.OnClickListener() { dialog, button ->
                // do nothing, and let user choose another action
            })
        connectAlert.show()
    }

    // this is the server side to navigate to the chat fragment
    @SuppressLint("MissingPermission")
    private fun incomingConnectionAlert(device: BluetoothDevice) {
        val incomingAlert = AlertDialog.Builder(context)

        var identity = ""

        if (!device.name.isNullOrBlank()) {
            identity = device.name
        } else if (!device.address.isNullOrBlank()) {
            identity = "device with the address ${device.address}"
        }
        incomingAlert.setCancelable(false)
        incomingAlert.setTitle(getString(R.string.incoming_connection_alert_title))
        incomingAlert.setMessage("Do you want to accept the incoming connection from $identity")
        incomingAlert.setPositiveButton(getString(R.string.accept_button),
            DialogInterface.OnClickListener() { dialog, button ->
                val bundle = Bundle()
                bundle.putBoolean("isClient", false)
                //bundle.putParcelable("device", device)
                bundle.putString("deviceName", device.name)
                bundle.putString("deviceAddress", device.address)
                findNavController().navigate(R.id.action_MainFragment_to_chatFragment, bundle)
            })
        incomingAlert.setNegativeButton(getString(R.string.cancel_button),
            DialogInterface.OnClickListener() { dialog, button ->
                // disconnect the device here
                ChatServiceManager.disconnectDevice(device)
            })
        incomingAlert.show()
    }

    private fun startServerAlert() {
        val startAlert = AlertDialog.Builder(context)

        startAlert.setTitle(getString(R.string.start_server_alert_title))
        startAlert.setMessage(getString(R.string.start_server_alert_desc))
        startAlert.setPositiveButton(getString(R.string.start_button),
            DialogInterface.OnClickListener() { dialog, button ->
                // start the chat service manager
                ChatServiceManager.startChatServer(requireActivity().application)
                dialog.dismiss()
            })
        startAlert.setNegativeButton(getString(R.string.cancel_button),
            DialogInterface.OnClickListener() { dialog, button ->
                // do nothing, wait for user's action
            })
        startAlert.show()
    }
}

