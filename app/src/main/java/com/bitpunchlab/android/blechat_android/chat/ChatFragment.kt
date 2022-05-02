package com.bitpunchlab.android.blechat_android.chat

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
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.blechat_android.ConnectionState
import com.bitpunchlab.android.blechat_android.R
import com.bitpunchlab.android.blechat_android.chatService.ChatServiceClient
import com.bitpunchlab.android.blechat_android.chatService.ChatServiceManager
import com.bitpunchlab.android.blechat_android.database.BLEDatabase
import com.bitpunchlab.android.blechat_android.databinding.FragmentChatBinding
import com.bitpunchlab.android.blechat_android.messages.MessageListAdapter
import com.bitpunchlab.android.blechat_android.messages.MessageViewModel
import com.bitpunchlab.android.blechat_android.messages.MessageViewModelFactory
import kotlinx.coroutines.InternalCoroutinesApi

private const val TAG = "ChatFragment"

class ChatFragment : Fragment() {

    private var _binding : FragmentChatBinding? = null
    private val binding get() = _binding!!
    private var isClient = false
    private lateinit var messageViewModel: MessageViewModel
    private lateinit var messageAdapter: MessageListAdapter
    private lateinit var database: BLEDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    @OptIn(InternalCoroutinesApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        database = BLEDatabase.getInstance(context)

        isClient = requireArguments().getBoolean("isClient")

        messageViewModel = ViewModelProvider(requireActivity(), MessageViewModelFactory(database))
            .get(MessageViewModel::class.java)

        messageAdapter = MessageListAdapter()
        binding.messageRecycler.adapter = messageAdapter

        messageViewModel.messageList.observe(viewLifecycleOwner, Observer { messageList ->
            messageList.isNullOrEmpty().let {
                // submit list
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