package com.manish.chatapp.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.manish.chatapp.data.firebase.FirebaseRepository
import com.manish.chatapp.data.firebase.MessagesListResult
import com.manish.chatapp.data.model.Message
import com.manish.chatapp.data.model.User

class ChatViewModel : ViewModel() {
    private val repository = FirebaseRepository.getInstance()

    fun startOnlineUpdates(user: User): LiveData<Long?> {
        return repository.activateUserOnlineUpdates(user)
    }

    fun deActivateOnlineUpdates() {
        repository.deActivateLastUserOnlineUpdates()
    }

    fun getMessages(userId: String): LiveData<MessagesListResult> {
        return repository.getMessages(userId)
    }

    fun sendMessage(msg: String, targetUser: User) {
        repository.sendTextMessage(msg, targetUser)
    }

    fun sendImage(context: Context, uri: Uri, targetUser: User) {
        repository.sendImage(context, uri, targetUser)
    }

    fun deleteMessageForMe(msg: Message, otherUser: User) {
        Log.d("TAGY", "deleteMessageForMe: Calling delete method!!!")
        repository.deleteForMe(msg, otherUser)
    }

    fun deleteMessageForEveryone(msg: Message, otherUser: User) {
        repository.deleteForEveryOne(msg, otherUser)
    }

}