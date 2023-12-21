package com.manish.chatapp.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.manish.chatapp.R
import com.manish.chatapp.data.model.Message
import com.manish.chatapp.data.model.User
import com.manish.chatapp.databinding.ActivityChatBinding
import com.manish.chatapp.ui.adapter.ChatAdapter
import com.manish.chatapp.ui.utils.UtilsMethods
import com.manish.chatapp.ui.viewmodel.ChatViewModel


class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var viewModel: ChatViewModel
    private lateinit var currUsr: User
    private lateinit var otherUser: User
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat)
        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        currUsr = intent.getSerializableExtra("curr_user") as User
        otherUser = intent.getSerializableExtra("other_user") as User

        binding.user = otherUser

        //Back
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        Glide.with(this)
            .load(otherUser.profileUrl)
            .into(binding.profileImage)


        // Send message
        binding.imgSend.setOnClickListener {
            val msg = binding.edtMsg.text.toString()
            sendMessage(msg)
            binding.edtMsg.setText("")
        }

        //Messages
        setUpRecyclerView()

        //Send Image
        binding.imgSelectImage.setOnClickListener {
            ImagePicker.with(this)
                .galleryOnly()    //User can only select image from Gallery
                .start()
        }
    }

    // Load messages
    private fun setUpRecyclerView() {
        chatAdapter = ChatAdapter()
        chatAdapter.setUsers(currUsr, otherUser)

        val manager = LinearLayoutManager(this@ChatActivity)

        binding.rvChat.apply {
            adapter = chatAdapter
            layoutManager = manager
            setHasFixedSize(true)
        }

        viewModel.getMessages(otherUser.id!!).observe(this) {
            if (it.error != null) {
                Toast.makeText(this@ChatActivity, it.error.message, Toast.LENGTH_SHORT)
                    .show()
            } else if (it.data != null) {
                chatAdapter.setList(it.data)
                chatAdapter.notifyDataSetChanged()
                if (!it.data.isNullOrEmpty())
                    manager.scrollToPosition(it.data.size - 1);
            }
        }

        // Delete Message
        chatAdapter.setLongClickListener { v, msg ->
            if (msg.isReceived == true)
                showMenu(v, R.menu.delete_menu_his_msg, msg)
            else
                showMenu(v, R.menu.delete_menu_my_msg, msg)
        }
    }

    private fun sendMessage(msg: String) {
        if (msg.isEmpty())
            return
        viewModel.sendMessage(msg, otherUser)
    }

    override fun onPause() {
        viewModel.deActivateOnlineUpdates()
        super.onPause()
    }

    override fun onResume() {
        viewModel.startOnlineUpdates(otherUser).observe(this) {
            binding.txtLastOnline.text = it?.let { it1 -> UtilsMethods.formatTimeWithLastSeen(it1) }
        }
        super.onResume()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            //Image Uri will not be null for RESULT_OK
            val uri: Uri = data?.data!!
            viewModel.sendImage(applicationContext, uri, otherUser)

            // Use Uri object instead of File to avoid storage permissions
            Log.i("TAGY", "onActivityResult: $uri")

        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
        } else {
            Log.i("TAGY", "onActivityResult: Task Cancelled")
        }
    }

    private fun showMenu(v: View, @MenuRes menuRes: Int, msg: Message) {
        val popup = PopupMenu(this, v)
        popup.menuInflater.inflate(menuRes, popup.menu)

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            // Respond to menu item click.
            if (menuItem.itemId == R.id.delete_for_me) {
                viewModel.deleteMessageForMe(msg, otherUser)
            } else if (menuItem.itemId == R.id.delete_for_every_one) {
                viewModel.deleteMessageForEveryone(msg, otherUser)
            } else {

            }
            return@setOnMenuItemClickListener true
        }
        popup.setOnDismissListener {
            // Respond to popup being dismissed.
        }
        // Show the popup menu.
        popup.show()
    }
}