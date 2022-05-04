package com.bitpunchlab.android.blechat_android.chat

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
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.blechat_android.ConnectionState
import com.bitpunchlab.android.blechat_android.R
import com.bitpunchlab.android.blechat_android.chatService.ChatServiceClient
import com.bitpunchlab.android.blechat_android.chatService.ChatServiceManager
import com.bitpunchlab.android.blechat_android.database.BLEDatabase
import com.bitpunchlab.android.blechat_android.databinding.FragmentChatBinding
import com.bitpunchlab.android.blechat_android.databinding.MessageListBinding
import com.bitpunchlab.android.blechat_android.deviceList.DeviceRepository
import com.bitpunchlab.android.blechat_android.deviceList.DeviceViewModel
import com.bitpunchlab.android.blechat_android.deviceList.DeviceViewModelFactory
import com.bitpunchlab.android.blechat_android.messages.MessageListAdapter
import com.bitpunchlab.android.blechat_android.messages.MessageRepository
import com.bitpunchlab.android.blechat_android.messages.MessageViewModel
import com.bitpunchlab.android.blechat_android.messages.MessageViewModelFactory
import com.bitpunchlab.android.blechat_android.models.DeviceModel
import com.bitpunchlab.android.blechat_android.models.MessageModel
import kotlinx.coroutines.InternalCoroutinesApi

private const val TAG = "ChatFragment"

class ChatFragment : Fragment() {

    private var _binding : FragmentChatBinding? = null
    private val binding get() = _binding!!
    private var isClient = false
    private lateinit var messageViewModel: MessageViewModel
    private lateinit var deviceViewModel: DeviceViewModel
    private lateinit var messageAdapter: MessageListAdapter
    private lateinit var database: BLEDatabase
    private lateinit var deviceRepository: DeviceRepository
    private lateinit var messageRepository: MessageRepository
    private var deviceName: String? = null
    private var deviceAddress: String? = null
    private var messageBinding: MessageListBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    @SuppressLint("MissingPermission")
    @OptIn(InternalCoroutinesApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        messageBinding = binding.messageLayout
        database = BLEDatabase.getInstance(context)
        deviceRepository = DeviceRepository(database)
        messageRepository = MessageRepository(database)

        deviceViewModel = ViewModelProvider(requireActivity(),
            DeviceViewModelFactory(requireActivity().application))
            .get(DeviceViewModel::class.java)
        messageViewModel = ViewModelProvider(requireActivity(), MessageViewModelFactory(database))
            .get(MessageViewModel::class.java)

        isClient = requireArguments().getBoolean("isClient")
        deviceName = requireArguments().getString("deviceName")
        deviceAddress = requireArguments().getString("deviceAddress")

        if (deviceAddress != null) {
            // save the device when the fragment starts
            // this is the client side
            createAndSaveDevice(deviceName, deviceAddress!!)
        } else {
            // this is the server side, if user accepted connection
            createAndSaveDevice(deviceViewModel.connectingDevice!!.name,
                deviceViewModel.connectingDevice!!.address)
        }

        messageAdapter = MessageListAdapter()
        messageBinding!!.messageRecycler.adapter = messageAdapter

        messageViewModel.messageList.observe(viewLifecycleOwner, Observer { messageList ->
            messageList.isNullOrEmpty().let {
                // submit list
                Log.i(TAG, "observed message list changed")
                messageAdapter.submitList(messageList)
                messageAdapter.notifyDataSetChanged()
            }
        })

        ChatServiceManager.message.observe(viewLifecycleOwner, Observer { msg ->
            msg?.let {
                Log.i(TAG, "observed message from manager")
                messageViewModel.addMessage(msg)
            }
        })

        ChatServiceClient.message.observe(viewLifecycleOwner, Observer { msg ->
            msg?.let {
                Log.i(TAG, "observed message from client")
                saveMessage(msg)
                messageViewModel.addMessage(msg)
            }
        })

        ChatServiceManager.connectionState.observe(viewLifecycleOwner, Observer { state ->
            if (state == ConnectionState.STATE_DISCONNECTED) {
                // alert user of the disconnection
                //if (!isClient) {
                    disconnectionAlert(ChatServiceManager.disconnectedDevice!!)
                //} else {
                    //disconnectionAlert(ChatServiceClient.connectedDevice!!)
                //}
            }
        })

        ChatServiceClient.connectionState.observe(viewLifecycleOwner, Observer { state ->
            if (state == ConnectionState.STATE_DISCONNECTED) {
                disconnectionAlert(ChatServiceClient.disconnectedDevice!!)
            }
        })

        binding.sendButton.setOnClickListener {
            if (!binding.messageEditText.text.isNullOrBlank()) {
                val msg = binding.messageEditText.text.toString()
                // need to know if user is client or server
                if (!isClient) {
                    ChatServiceManager.sendMessage(msg)
                } else {
                    ChatServiceClient.sendMessage(msg)
                }
                binding.messageEditText.text = null
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        messageBinding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_chat, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.disconnect -> {
                if (!isClient) {
                    if (ChatServiceManager.connectedDevice != null) {
                        // in this case, we disconnect the current connected device
                        ChatServiceManager.disconnectDevice(ChatServiceManager.connectedDevice!!)
                    } else if (ChatServiceManager.disconnectedDevice != null) {
                        // in this case, we clear the resources
                        ChatServiceManager.disconnectDevice(ChatServiceManager.disconnectedDevice!!)
                    }
                } else {
                    if (ChatServiceClient.connectedDevice != null) {
                        // in this case, we disconnect the current connected device
                        ChatServiceClient.disconnectDevice(ChatServiceClient.connectedDevice!!)
                    } else if (ChatServiceClient.disconnectedDevice != null) {
                        // in this case, we clear the resources
                        ChatServiceClient.disconnectDevice(ChatServiceClient.disconnectedDevice!!)
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // we save the device in the database only if it has messages.
    @SuppressLint("MissingPermission")
    private fun createAndSaveDevice(deviceName: String?, deviceAddress: String) {
        Log.i(TAG, "create and save device ran")
        val deviceModel = DeviceModel(name = deviceName, address = deviceAddress)
        deviceRepository.saveDevice(deviceModel)
        Log.i(TAG, "device saved")
    }

    private fun saveMessage(message: MessageModel) {
        messageRepository.saveMessage(message)
    }

    @SuppressLint("MissingPermission")
    private fun disconnectionAlert(device: BluetoothDevice) {
        val disconnectAlert = AlertDialog.Builder(context)

        var identity = ""

        if (!device.name.isNullOrBlank()) {
            identity = device.name
        } else if (!device.address.isNullOrBlank()) {
            identity = "device with the address ${device.address}"
        }

        disconnectAlert.setTitle(getString(R.string.disconnection_alert_title))
        disconnectAlert.setMessage("Disconnected with $identity")
        disconnectAlert.setPositiveButton(getString(R.string.back_to_device_list_button),
            DialogInterface.OnClickListener() { dialog, button ->
                dialog.dismiss()
                // pop this fragment
                findNavController().popBackStack()
            })
        disconnectAlert.setNegativeButton(getString(R.string.stay_in_chat_button),
            DialogInterface.OnClickListener() { dialog, button ->
                // do nothing, wait for user's next action

            })

        disconnectAlert.show()
    }
}