package com.manish.chatapp.data.firebase

import com.manish.chatapp.data.model.Message
import com.manish.chatapp.data.model.User

data class MessagesListResult(val error: Throwable? = null, val data: List<Message>? = null)