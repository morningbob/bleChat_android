package com.bitpunchlab.android.blechat_android.chatRecords

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bitpunchlab.android.blechat_android.R
import com.bitpunchlab.android.blechat_android.database.BLEDatabase
import com.bitpunchlab.android.blechat_android.databinding.FragmentChatRecordBinding
import com.bitpunchlab.android.blechat_android.databinding.MessageListBinding
import com.bitpunchlab.android.blechat_android.messages.MessageListAdapter
import com.bitpunchlab.android.blechat_android.messages.MessageViewModel
import com.bitpunchlab.android.blechat_android.messages.MessageViewModelFactory
import kotlinx.coroutines.InternalCoroutinesApi

private const val TAG = "ChatRecordFragment"

class ChatRecordFragment : Fragment() {

    private var _binding : FragmentChatRecordBinding? = null
    private val binding get() = _binding!!
    private lateinit var messageViewModel: MessageViewModel
    private lateinit var database: BLEDatabase
    private var messageBinding: MessageListBinding? = null
    private lateinit var messageAdapter: MessageListAdapter
    private var deviceAddress: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    @OptIn(InternalCoroutinesApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatRecordBinding.inflate(layoutInflater, container, false)
        messageBinding = binding.messageLayout
        database = BLEDatabase.getInstance(context)
        messageAdapter = MessageListAdapter()
        deviceAddress = requireArguments().getString("deviceAddress")

        messageViewModel = ViewModelProvider(requireActivity(), MessageViewModelFactory(database))
            .get(MessageViewModel::class.java)
        deviceAddress?.let {
            messageViewModel.getDeviceMessages(it)
            Log.i(TAG, "got device address, getting corresponding messages")
        }

        messageBinding!!.messageRecycler.adapter = messageAdapter

        messageViewModel.messageRecordList.observe(viewLifecycleOwner, Observer { messageList ->
            Log.i(TAG, "message record list changes")
            Log.i(TAG, "message list count: ${messageList.size}")
            if (!messageList.isNullOrEmpty()) {
                Log.i(TAG, "message list some set")
                messageAdapter.submitList(messageList)
                messageAdapter.notifyDataSetChanged()
            }
        })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        messageBinding = null
    }
}