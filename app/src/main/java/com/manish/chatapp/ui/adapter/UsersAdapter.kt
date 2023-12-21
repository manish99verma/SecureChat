package com.manish.chatapp.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.manish.chatapp.R
import com.manish.chatapp.data.model.Message
import com.manish.chatapp.data.model.User
import com.manish.chatapp.databinding.ItemUsersBinding
import com.manish.chatapp.ui.utils.UtilsMethods
import java.text.SimpleDateFormat
import java.util.Date

class UsersAdapter : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {
    private var onClickListener: ((User) -> Unit)? = null

    private val differCallback = object : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }

    private val differ = AsyncListDiffer(this, differCallback)

    fun setList(list: List<User>) {
        Log.d("TAGY", "setList: New list passed")
        differ.submitList(list)
    }

    fun setOnClickListener(lis: ((User) -> Unit)) {
        onClickListener = lis
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUsersBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(differ.currentList[position])
    }

    inner class UserViewHolder(val binding: ItemUsersBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            Glide.with(binding.root.context)
                .load(user.profileUrl)
                .placeholder(R.color.color_image_bg)
                .into(binding.profileImage)

            binding.txtName.text = user.name

            if (user.lastMessage != null) {
                if (user.lastMessage!!.type == Message.MSG_TYPE_TEXT) {
                    binding.txtMsg.text = user.lastMessage?.msg
                } else if (user.lastMessage!!.type == Message.MSG_TYPE_IMAGE) {
                    if (user.lastMessage!!.isReceived == true) {
                        binding.txtMsg.text = "Sent you a Photo"
                    } else {
                        binding.txtMsg.text = "You sent a Photo"
                    }
                }

                binding.txtTime.text =
                    UtilsMethods.formatDate(user.lastMessage!!.time)
            }

            binding.root.setOnClickListener { v ->
                onClickListener?.let { it(user) }
            }
        }
    }
}