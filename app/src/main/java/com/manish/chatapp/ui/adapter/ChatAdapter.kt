package com.manish.chatapp.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.manish.chatapp.R
import com.manish.chatapp.data.model.Message
import com.manish.chatapp.data.model.User
import com.manish.chatapp.databinding.ItemHisImageBinding
import com.manish.chatapp.databinding.ItemHisMessageBinding
import com.manish.chatapp.databinding.ItemMyImageBinding
import com.manish.chatapp.databinding.ItemMyMsgBinding
import com.manish.chatapp.databinding.ItemMyProfileBinding
import com.manish.chatapp.databinding.ItemProfileHisBinding
import com.manish.chatapp.ui.utils.UtilsMethods

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        @JvmStatic
        private val VIEW_TYPE_MY_PROFILE = 0

        @JvmStatic
        private val VIEW_TYPE_HIS_PROFILE = 1

        @JvmStatic
        private val VIEW_TYPE_MY_TEXT_MESSAGE = 2

        @JvmStatic
        private val VIEW_TYPE_HIS_TEXT_MESSAGE = 3

        @JvmStatic
        private val VIEW_TYPE_MY_IMAGE_MESSAGE = 4

        @JvmStatic
        private val VIEW_TYPE_HIS_IMAGE_MESSAGE = 5
    }

    private val differCallback = object : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }

    private val differ = AsyncListDiffer(this, differCallback)
    private lateinit var currUser: User
    private lateinit var otherUser: User
    private var longClickListener: ((View, Message) -> Unit)? = null

    fun setList(list: List<Message>) {
        differ.submitList(list)
    }

    fun setUsers(currUser: User, otherUser: User) {
        this.currUser = currUser
        this.otherUser = otherUser
    }

    fun setLongClickListener(listener: ((View, Message) -> Unit)) {
        longClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_MY_PROFILE -> AdapterViewHolder.fromMyProfile(parent)
            VIEW_TYPE_HIS_PROFILE -> AdapterViewHolder.fromHisProfile(parent)
            VIEW_TYPE_MY_TEXT_MESSAGE -> AdapterViewHolder.fromMyTextMessage(parent)
            VIEW_TYPE_HIS_TEXT_MESSAGE -> AdapterViewHolder.fromHisTextMessage(parent)
            VIEW_TYPE_MY_IMAGE_MESSAGE -> AdapterViewHolder.fromMyImageMessage(parent)
            VIEW_TYPE_HIS_IMAGE_MESSAGE -> AdapterViewHolder.fromHisImageMessage(parent)
            else -> AdapterViewHolder.fromMyProfile(parent)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val msg = differ.currentList[position]
        val msgType = msg.type
        var res = 0
        if (msg.isReceived == true) {
            when (msgType) {
                Message.MSG_TYPE_PROFILE -> res = VIEW_TYPE_HIS_PROFILE
                Message.MSG_TYPE_IMAGE -> res = VIEW_TYPE_HIS_IMAGE_MESSAGE
                Message.MSG_TYPE_TEXT -> res = VIEW_TYPE_HIS_TEXT_MESSAGE
            }
        } else {
            when (msgType) {
                Message.MSG_TYPE_PROFILE -> res = VIEW_TYPE_MY_PROFILE
                Message.MSG_TYPE_IMAGE -> res = VIEW_TYPE_MY_IMAGE_MESSAGE
                Message.MSG_TYPE_TEXT -> res = VIEW_TYPE_MY_TEXT_MESSAGE
            }
        }

        return res
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as AdapterViewHolder<*>).bind(
            differ.currentList[position], currUser, otherUser, longClickListener
        )
    }

    class AdapterViewHolder<T : ViewBinding> private constructor(val binding: T) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            msg: Message,
            currUser: User,
            otherUser: User,
            listener: ((View, Message) -> Unit)?
        ) {
            when (binding) {
                is ItemMyProfileBinding -> {
                    binding.txtTime.text = UtilsMethods.formatDate(msg.time)
                    Glide.with(binding.profileImage.context)
                        .load(currUser.profileUrl)
                        .into(binding.profileImage)
                }

                is ItemProfileHisBinding -> {
                    binding.txtName.text = otherUser.name
                    binding.txtTime.text = UtilsMethods.formatDate(msg.time)
                    Glide.with(binding.profileImage.context)
                        .load(otherUser.profileUrl)
                        .placeholder(R.color.color_image_bg)
                        .into(binding.profileImage)
                }

                is ItemMyMsgBinding -> {
                    binding.txtMsg.text = msg.msg

                    setLongClickListener(binding.root, msg, listener)
                }

                is ItemHisMessageBinding -> {
                    binding.txtMsg.text = msg.msg

                    setLongClickListener(binding.root, msg, listener)
                }

                is ItemMyImageBinding -> {
                    Log.d("TAGY", "bind: Loading my image ${msg.msg}")
                    Glide.with(binding.imgMsg.context)
                        .asBitmap()
                        .fitCenter()
                        .placeholder(R.drawable.place_holder_image)
                        .load(msg.msg)
                        .into(binding.imgMsg)

                    setLongClickListener(binding.root, msg, listener)
                }

                is ItemHisImageBinding -> {
                    Log.d("TAGY", "bind: Loading his image ${msg.msg}")
                    Glide.with(binding.imgMsg.context)
                        .load(msg.msg)
                        .into(binding.imgMsg)

                    setLongClickListener(binding.root, msg, listener)
                }
            }
        }

        private fun setLongClickListener(
            view: View,
            msg: Message,
            listener: ((View, Message) -> Unit)?
        ) {
            view.setOnLongClickListener(object : OnLongClickListener {
                override fun onLongClick(v: View?): Boolean {
                    if (listener == null || v == null)
                        return false

                    listener(v, msg)
                    return true
                }
            })
        }

        companion object {
            fun fromMyProfile(parent: ViewGroup): AdapterViewHolder<ItemMyProfileBinding> {
                Log.d("TAGY", "fromMyProfile: ")
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemMyProfileBinding.inflate(layoutInflater, parent, false)
                return AdapterViewHolder(binding)
            }

            fun fromHisProfile(parent: ViewGroup): AdapterViewHolder<ItemProfileHisBinding> {
                Log.d("TAGY", "fromHisProfile: ")
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemProfileHisBinding.inflate(layoutInflater, parent, false)
                return AdapterViewHolder(binding)
            }

            fun fromMyTextMessage(parent: ViewGroup): AdapterViewHolder<ItemMyMsgBinding> {
                Log.d("TAGY", "fromMyTxtProfile: ")
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemMyMsgBinding.inflate(layoutInflater, parent, false)
                return AdapterViewHolder(binding)
            }

            fun fromHisTextMessage(parent: ViewGroup): AdapterViewHolder<ItemHisMessageBinding> {
                Log.d("TAGY", "fromHisTxtProfile: ")
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemHisMessageBinding.inflate(layoutInflater, parent, false)
                return AdapterViewHolder(binding)
            }

            fun fromMyImageMessage(parent: ViewGroup): AdapterViewHolder<ItemMyImageBinding> {
                Log.d("TAGY", "fromMyImageBinding: ")
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemMyImageBinding.inflate(layoutInflater, parent, false)
                return AdapterViewHolder(binding)
            }

            fun fromHisImageMessage(parent: ViewGroup): AdapterViewHolder<ItemHisImageBinding> {
                Log.d("TAGY", "fromHisImageBinding: ")
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemHisImageBinding.inflate(layoutInflater, parent, false)
                return AdapterViewHolder(binding)
            }

        }
    }
}