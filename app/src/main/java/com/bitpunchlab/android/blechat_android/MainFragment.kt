package com.bitpunchlab.android.blechat_android

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private lateinit var chatServiceManager: ChatServiceManager
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


}

