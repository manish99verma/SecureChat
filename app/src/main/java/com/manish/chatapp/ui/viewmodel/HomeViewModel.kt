package com.manish.chatapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.manish.chatapp.data.firebase.FirebaseRepository
import com.manish.chatapp.data.firebase.UsersListResult
import com.manish.chatapp.data.model.User

class HomeViewModel : ViewModel() {
    private val firebaseRepository = FirebaseRepository.getInstance()

    fun getUsers(): LiveData<UsersListResult> {
        return firebaseRepository.getUsersLiveData()
    }

    fun logOut() {
        firebaseRepository.logOut()
    }

    fun getCurrDatabaseUser(): User? {
        return firebaseRepository.getCurrUser()
    }
}