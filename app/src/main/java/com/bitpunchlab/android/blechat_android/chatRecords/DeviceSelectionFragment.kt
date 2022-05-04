package com.bitpunchlab.android.blechat_android.chatRecords

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.blechat_android.R
import com.bitpunchlab.android.blechat_android.database.BLEDatabase
import com.bitpunchlab.android.blechat_android.databinding.DeviceListBinding
import com.bitpunchlab.android.blechat_android.databinding.FragmentDeviceSelectionBinding
import com.bitpunchlab.android.blechat_android.deviceList.*
import kotlinx.coroutines.InternalCoroutinesApi

private const val TAG = ""

class DeviceSelectionFragment : Fragment() {

    private var _binding: FragmentDeviceSelectionBinding? = null
    private val binding get() = _binding!!
    private lateinit var deviceViewModel: DeviceViewModel
    private lateinit var database: BLEDatabase
    private lateinit var deviceAdapter: DeviceModelAdapter
    private var deviceBinding : DeviceListBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    @OptIn(InternalCoroutinesApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDeviceSelectionBinding.inflate(layoutInflater, container, false)
        deviceBinding = binding.deviceLayout
        database = BLEDatabase.getInstance(requireContext())
        deviceViewModel = ViewModelProvider(requireActivity(),
            DeviceViewModelFactory(requireActivity().application))
            .get(DeviceViewModel::class.java)
        deviceAdapter = DeviceModelAdapter( DeviceModelListener { device ->
            deviceViewModel.onModelClicked(device)
        })
        deviceBinding!!.devicesRecycler.adapter = deviceAdapter

        deviceViewModel.getRecordedDevices()
        //deviceViewModel.deviceList.observe(viewLifecycleOwner, Observer { deviceList ->
        deviceViewModel.recordedDeviceList.observe(viewLifecycleOwner, Observer { deviceList ->
            Log.i(TAG, "recorded list: changes")
            Log.i(TAG, "recorded list: count ${deviceList.size}")
            if (!deviceList.isNullOrEmpty()) {
                Log.i(TAG, "recorded list: some devices set")
                deviceAdapter.submitList(deviceList)
                deviceAdapter.notifyDataSetChanged()
            }
        })

        deviceViewModel.chosenModel.observe(viewLifecycleOwner, Observer { device ->
            device?.let {
                Log.i(TAG, "begins to navigate")
                // navigate to chat records fragment
                val bundle = Bundle()
                bundle.putString("deviceAddress", device.address)
                findNavController().navigate(R.id.action_deviceSelectionFragment_to_chatRecordFragment,
                    bundle)
                // reset
                deviceViewModel.finishNavigationOfDeviceModel()
            }
        })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        deviceBinding = null
    }
}