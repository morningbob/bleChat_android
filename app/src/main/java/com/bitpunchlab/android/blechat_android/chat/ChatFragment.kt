package com.bitpunchlab.android.blechat_android.chat

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.app.NotificationManager
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.*
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bitpunchlab.android.blechat_android.*
import com.bitpunchlab.android.blechat_android.chatNotification.MessageAlertService
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
import kotlinx.coroutines.*

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
    private lateinit var coroutineScope: CoroutineScope
    private var dismissDialogFinished = MutableLiveData(false)
    private var isDisconnectedShown = false
    private var disconnectedDevice : BluetoothDevice? = null
    private var connectionStateHistory = ArrayList<ConnectionState>()
    private var status = MutableLiveData<String>("")
    private lateinit var messageList: LiveData<List<MessageModel>>
    private var isNotificationOn = MutableLiveData<Boolean>(false)
    private lateinit var messageAlertService : MessageAlertService
    private var mBound = false
    //private lateinit var messageServiceConnection : ServiceConnection

    //private var disconnectAlert = MutableLiveData<Dialog>()

    private val messageServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as MessageAlertService.LocalBinder
            messageAlertService = binder.getMessageServiceInstance()
            mBound = true
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            mBound = false
        }
    }

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
        coroutineScope = CoroutineScope(Dispatchers.Default)

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

        deviceAddress?.let {
            messageViewModel.getDeviceMessages(deviceAddress!!)
        }

        messageAdapter = MessageListAdapter()
        messageBinding!!.messageRecycler.adapter = messageAdapter

        messageViewModel.messageRecordList.observe(viewLifecycleOwner, Observer { messageList ->
            messageList?.let {
                // submit list
                //messageViewModel.messageRecordList.value = messageList
                Log.i(TAG, "observed message list changed")
                Log.i(TAG, "for message: ${messageList.last().content}")
                messageAdapter.submitList(messageList)
                messageBinding!!.messageRecycler.smoothScrollToPosition(messageList.size - 1)
                messageAdapter.notifyDataSetChanged()
            }
        })

        ChatServiceManager.message.observe(viewLifecycleOwner, Observer { msg ->
            msg?.let {
                Log.i(TAG, "observed message from manager")
                // we start the notification with the content of the message

                //saveMessage(msg)
                messageViewModel.saveMessage(msg)
                if (msg.deviceName != "You" && isNotificationOn.value!!) {
                    startMessageNotification(msg.content)
                }
            }
        })

        ChatServiceClient.message.observe(viewLifecycleOwner, Observer { msg ->
            msg?.let {
                Log.i(TAG, "observed message from client")
                // we start the notification with the content of the message
                // "You" identifies the messages are from device, or just messages
                // the user sent.  We only send notification when receive message from
                // the other device.

                //saveMessage(msg)
                messageViewModel.saveMessage(msg)
                if (msg.deviceName != "You" && isNotificationOn.value!!) {
                    startMessageNotification(msg.content)
                }
            }
        })

        ChatServiceManager.connectionState.observe(viewLifecycleOwner, Observer { state ->
            if (state == ConnectionState.STATE_DISCONNECTED) {
                // alert user of the disconnection
                //if (!isClient) {
                // we check if we showed the disconnection alert already before showing it again
                //if (!isDisconnectedShown && disconnectedDevice != ChatServiceManager.disconnectedDevice) {
                //if (connectionStateHistory.last() != null &&
                    //connectionStateHistory.last() != ConnectionState.STATE_CONNECTED) {
                    //disconnectionAlert(ChatServiceManager.disconnectedDevice!!)
                    //connectionStateHistory.add(ConnectionState.STATE_DISCONNECTED)
                //}
                //}
                getAppStatus(state, null)

            } else if (state == ConnectionState.STATE_CONNECTED) {
                // we record that in app state history
                connectionStateHistory.add(ConnectionState.STATE_CONNECTED)
                getAppStatus(state, ChatServiceManager.connectedDevice)
            } else {
                getAppStatus(state, null)
            }
        })

        ChatServiceClient.connectionState.observe(viewLifecycleOwner, Observer { state ->

            if (state == ConnectionState.STATE_DISCONNECTED) {
                //if (!isDisconnectedShown && disconnectedDevice != ChatServiceManager.disconnectedDevice) {
                //if (connectionStateHistory.last() == ConnectionState.STATE_CONNECTED) {
                //    if (ChatServiceManager.disconnectedDevice != null) {
                //        disconnectionAlert(ChatServiceManager.disconnectedDevice!!)
                //    } else {
                 //       Log.i("connection state changes", "null disconnected device")
                 //   }
                //}
                getAppStatus(state, null)
            } else if (state == ConnectionState.STATE_CONNECTED) {
                // we record that in app state history
                //connectionStateHistory.add(ConnectionState.STATE_CONNECTED)
                getAppStatus(state, ChatServiceClient.connectedDevice)
            } else {
                getAppStatus(state, null)
            }
        })

        binding.toggleNotification.setOnClickListener {
            isNotificationOn.value = !isNotificationOn.value!!
        }

        binding.sendButton.setOnClickListener {
            //messageViewModel.verifyConfirmCode("abc")
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

        dismissDialogFinished.observe(viewLifecycleOwner, Observer { dismissed ->
            if (dismissed) {
                findNavController().popBackStack()
            }
        })

        status.observe(viewLifecycleOwner, Observer { appStatus ->
            binding.stateInfo.text = appStatus
        })
        // for every confirm code I got from the chat service, I run a coroutine to run
        // through the message list once, updated the sent in message model.
        // so the confirm code will only be checked when it arrived, for the last 15 messages
        // then, it will be discarded.
        ChatServiceManager.confirmCode.observe(viewLifecycleOwner, Observer { code ->
            messageViewModel.verifyConfirmationCode(code)
        })

        ChatServiceClient.confirmCode.observe(viewLifecycleOwner, Observer { code ->
            messageViewModel.verifyConfirmationCode(code)
        })

        isNotificationOn.observe(viewLifecycleOwner, Observer { isOn ->
            // we use the isOn value to control whether to send notification or not
            // so, when it is on, we won't do anything, won't trigger the startNotification
            // function.  wait until there is new message from the peer.
            if (!isOn) {
                // stop the message alert service
                if (mBound) {
                    // cancel the already shown notification
                    messageAlertService.cancelNotificationSent()
                }
                stopMessageNotification()
                // update the button to "turn on notification"
                binding.toggleNotification.text = getString(R.string.start_notification)
            } else {
                binding.toggleNotification.text = getString(R.string.stop_notification)
            }
        })
        return binding.root
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unbindService(messageServiceConnection)
        mBound = false
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
                        if (ChatServiceManager.sendMessage("disconnect735946")) {
                            // in this case, we disconnect the current connected device
                            ChatServiceManager.disconnectDevice(ChatServiceManager.connectedDevice!!)
                        }
                    } else if (ChatServiceManager.disconnectedDevice != null) {
                        // in this case, we clear the resources
                        if (ChatServiceManager.sendMessage("disconnect735946")) {
                            // in this case, we disconnect the current connected device
                            ChatServiceManager.disconnectDevice(ChatServiceManager.disconnectedDevice!!)
                        }

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

    @SuppressLint("MissingPermission")
    private fun disconnectionAlert(device: BluetoothDevice) {
        var disconnectAlert = AlertDialog.Builder(context)

        var identity = ""

        if (!device.name.isNullOrBlank()) {
            identity = device.name
        } else if (!device.address.isNullOrBlank()) {
            identity = "device with the address ${device.address}"
        }

        disconnectAlert.setTitle(getString(R.string.disconnection_alert_title))
        disconnectAlert.setMessage("Disconnected with $identity")
        //dismissedDialog.value = disconnectAlert
        disconnectAlert.setPositiveButton(getString(R.string.back_to_device_list_button),
            DialogInterface.OnClickListener() { dialog, button ->
                // Bug: for the server, or the samsung tablet,
                // the dialog is dimissed,  But for the client
                // or the samsung phone, the dialog can't be
                // dismissed.
                //coroutineScope.launch {
                    dialog.dismiss()
                //    dismissDialogFinished.postValue(true)
                findNavController().popBackStack()
                //findNavController().navigateUp()

                //}
                // pop this fragment
                //findNavController().popBackStack()
            })
        disconnectAlert.setNegativeButton(getString(R.string.stay_in_chat_button),
            DialogInterface.OnClickListener() { dialog, button ->
                // do nothing, wait for user's next action

            })
        isDisconnectedShown = true
        disconnectAlert.show()
    }

    // we keep calling this method whenever there is new message from the peer
    // when the notification service is on.
    // we simply replace the old notification with the old message with the new ones.
    private fun startMessageNotification(message: String) {
        val intent = Intent(requireContext(), MessageAlertService::class.java)
        // this action is used to identify our intention inside the service
        // the service check if it is start, or stop, and behave so accordingly.
        //intent.action = START_MESSAGE_NOTIFICATION
        intent.putExtra("message", message)
        requireContext().startService(intent)
        //isNotificationOn.value = true
        Log.i(TAG, "started notification service")
    }

    private fun stopMessageNotification() {

        //val notificationManager = NotificationManagerCompat.from(requireContext())
        //notificationManager.cancel(NOTIFICATION_ID)

        // we stop the service here
        val intent = Intent(requireContext(), MessageAlertService::class.java)
        //intent.action = STOP_MESSAGE_NOTIFICATION
        requireContext().stopService(intent)
        //isNotificationOn.value = false
        Log.i(TAG, "stopping notification service")
    }

    @SuppressLint("MissingPermission")
    private fun getAppStatus(state: ConnectionState, device: BluetoothDevice?)  {
        when (state) {
            ConnectionState.STATE_NONE -> {
                status.value = "Waiting for your instruction..."
            }
            ConnectionState.STATE_CONNECTING -> {
                status.value = "Connecting..."
            }
            ConnectionState.STATE_CONNECTED -> {
                status.value = "Connected with ${device?.name ?: device?.address}"
            }
            ConnectionState.STATE_DISCONNECTED -> {
                status.value = "Disconnected."
            }
            else -> status.value = "Waiting for you instruction..."
        }
    }
}