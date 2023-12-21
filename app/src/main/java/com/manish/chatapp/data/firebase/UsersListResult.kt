package com.manish.chatapp.data.firebase

import com.manish.chatapp.data.model.User

data class UsersListResult(val error: Throwable? = null, val data: List<User>?=null)