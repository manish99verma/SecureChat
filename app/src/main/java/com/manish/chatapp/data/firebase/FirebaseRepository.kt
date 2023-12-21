package com.manish.chatapp.data.firebase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.manish.chatapp.data.model.Message
import com.manish.chatapp.data.model.User
import com.manish.chatapp.ui.utils.UtilsMethods
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID

class FirebaseRepository private constructor() {
    private var usersListResultLiveData = MutableLiveData<UsersListResult>()
    private val usersList = ArrayList<User>()

    /*    private val messagesListResult = MutableLiveData<List<Message>>()
        private val messagesList = ArrayList<Message>()
        private var currentUserMessagesRef: Query? = null
        private var currentUserMessageListener: ChildEventListener? = null*/

    private val firebaseAuth = FirebaseAuth.getInstance()
    private var currUser: FirebaseUser? = null
    private var databaseCurrUser: User? = null

    private var database = FirebaseDatabase.getInstance()
    private val users_ref = database.getReference("Users")
    private val last_seen_ref = database.getReference("LastSeen")
    private var lastOnlineLiveData = MutableLiveData<Long?>()

    //Other last seen listener
    private var otherOnlineUpdater: Job? = null

//    private lateinit var messages_ref: DatabaseReference

    private val logInResult = MutableLiveData<AuthResult>()
    private val signUpResult = MutableLiveData<AuthResult>()

    private val firebaseStorage = FirebaseStorage.getInstance()
    private val profile_pics_bucket = firebaseStorage.getReference("profile_pics")
    private val sent_images_bucket = firebaseStorage.getReference("sent_pics")

    companion object {
        private var instance: FirebaseRepository? = null
        private val MAX_TIME_GROUP_MSG: Long = 60 * 1000

        fun getInstance(): FirebaseRepository {
            if (instance == null) {
                instance = FirebaseRepository()
            }
            return instance as FirebaseRepository
        }
    }

    init {
        changeUser(firebaseAuth.currentUser)
        myOnlineUpdater()
    }

    private fun myOnlineUpdater() {
        CoroutineScope(IO).launch {
            Log.d("TAGY", "lastSeenUpdater: Activated!!!")
            var updating = false

            while (true) {
                if (!updating && currUser?.uid != null) {
                    updating = true
                    last_seen_ref.child(currUser!!.uid).setValue(System.currentTimeMillis())
                        .addOnSuccessListener {
                            Log.d("TAGY", "Last Online: Updated!")
                            updating = false
                        }

                }

                delay(15 * 1000)
            }
        }
    }

    fun deActivateLastUserOnlineUpdates() {
        Log.d("TAGY", "lastSeenUpdater: Deactivated")

        lastOnlineLiveData = MutableLiveData<Long?>()
        otherOnlineUpdater?.cancel()
    }

    fun activateUserOnlineUpdates(targetUser: User): LiveData<Long?> {
        otherOnlineUpdater = CoroutineScope(IO).launch {
            Log.d("TAGY", "lastSeenUpdater: Activated for ${targetUser.name}")
            var updating = false
            val ref = last_seen_ref.child(targetUser.id!!)

            while (true) {
                if (!updating) {
                    updating = true
                    ref.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val lastSeen: Long? = snapshot.getValue(Long::class.java)
                            lastOnlineLiveData.postValue(lastSeen)
                            Log.d(
                                "TAGY",
                                "onDataChange: Last online of ${targetUser.name} -> $lastSeen"
                            )
                            updating = false
                        }

                        override fun onCancelled(error: DatabaseError) {
                            TODO("Not yet implemented")
                        }
                    })
                }

                delay(15 * 1000)
            }
        }

        return lastOnlineLiveData
    }

    //Auth
    fun createUser(
        name: String,
        email: String,
        password: String,
        image: Bitmap?
    ): LiveData<AuthResult> {
        val exceptionMsg =
            if (TextUtils.isEmpty(name))
                "Please enter name!"
            else if (TextUtils.isEmpty(email)
                || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
            )
                "Please enter a valid email!"
            else if (TextUtils.isEmpty(password))
                "Please enter password!"
            else if (password.length < 6)
                "Please enter a longer password!"
            else if (image == null)
                "Please add your profile pic!"
            else
                ""

        if (exceptionMsg.isNotEmpty()) {
            signUpResult.postValue(AuthResult(exceptionMsg, false, false))
            return signUpResult
        }

        signUpResult.postValue(AuthResult(processing = true))

        //Create User On Firebase
        // Step 1 -> Create User
        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnSuccessListener {
            if (it.user == null) {
                signUpResult.postValue(AuthResult("Something went wrong!", false, false))
            } else {
                uploadImage(it.user!!.uid, name, email, image!!)
                changeUser(it.user)
            }
        }.addOnFailureListener {
            if (it.localizedMessage != null)
                signUpResult.postValue(AuthResult(it.localizedMessage, false, false))
            else
                signUpResult.postValue(AuthResult("Something went wrong!", false, false))
        }.addOnCanceledListener {
            signUpResult.postValue(AuthResult("Something went wrong!", false, false))
        }

        return signUpResult
    }

    // Step 2 -> Upload dp
    private fun uploadImage(id: String, name: String, email: String, imageBitmap: Bitmap) {
        CoroutineScope(IO).launch {
            val dp_ref = profile_pics_bucket.child(UUID.randomUUID().toString() + ".jpg")
            val baos = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()

            val uploadTask: UploadTask = dp_ref.putBytes(data)
            uploadTask.addOnSuccessListener {
                dp_ref.downloadUrl.addOnSuccessListener {
                    uploadToDatabase(id, name, email, it.toString())
                }.addOnFailureListener {
                    if (it.localizedMessage != null)
                        signUpResult.postValue(AuthResult(it.localizedMessage, false, false))
                    else
                        signUpResult.postValue(AuthResult("Something went wrong!", false, false))
                }.addOnCanceledListener {
                    signUpResult.postValue(AuthResult("Something went wrong!", false, false))
                }
            }.addOnFailureListener {
                if (it.localizedMessage != null)
                    signUpResult.postValue(AuthResult(it.localizedMessage, false, false))
                else
                    signUpResult.postValue(AuthResult("Something went wrong!", false, false))
            }.addOnCanceledListener {
                signUpResult.postValue(AuthResult("Something went wrong!", false, false))
            }
        }
    }

    // Step 3 -> Upload fields to database
    private fun uploadToDatabase(id: String, name: String, email: String, imageUrl: String) {
        val currentUserRef = users_ref.child(id)
        val map = hashMapOf("id" to id, "name" to name, "email" to email, "profileUrl" to imageUrl)

        currentUserRef.setValue(map).addOnSuccessListener {
            signUpResult.postValue(AuthResult("Success", false, true))
        }.addOnFailureListener {
            if (it.localizedMessage != null)
                signUpResult.postValue(AuthResult(it.localizedMessage, false, false))
            else
                signUpResult.postValue(AuthResult("Something went wrong!", false, false))
        }.addOnCanceledListener {
            signUpResult.postValue(AuthResult("Something went wrong!", false, false))
        }
    }

    fun usesAlreadyLogined(): Boolean {
        return currUser != null
    }

    fun logIn(email: String, password: String): LiveData<AuthResult> {
        val exceptionMsg =
            if (TextUtils.isEmpty(email)
                || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
            )
                "Please enter a valid email!"
            else if (TextUtils.isEmpty(password))
                "Please enter password!"
            else if (password.length < 6)
                "Please enter a longer password!"
            else
                ""

        if (exceptionMsg.isNotEmpty()) {
            logInResult.postValue(AuthResult(exceptionMsg, false, false))
            return logInResult
        }

        logInResult.postValue(AuthResult(processing = true))
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnSuccessListener {
            changeUser(it.user)
            Log.d("TAGY", "logIn: Successful")
            logInResult.postValue(AuthResult("Success", false, true))
        }.addOnFailureListener {
            if (it.localizedMessage == null) {
                logInResult.postValue(AuthResult("Unknown error occurred!", false, false))
            } else {
                logInResult.postValue(AuthResult(it.localizedMessage, false, false))
            }
        }.addOnCanceledListener {
            logInResult.postValue(AuthResult("Unknown error occurred!", false, false))
        }

        return logInResult
    }

    fun logOut() {
        firebaseAuth.signOut()
        changeUser(null)
    }

    private fun changeUser(user: FirebaseUser?) {
        databaseCurrUser = null
        currUser = user
        usersList.clear()
        usersListResultLiveData = MutableLiveData<UsersListResult>()

        if (currUser != null) {
            getUsers()
        }
    }

    // Users List
    fun getCurrUser(): User? {
        return databaseCurrUser
    }

    fun getUsersLiveData(): LiveData<UsersListResult> {
        return usersListResultLiveData
    }

    private fun getUsers() {
        if (currUser == null) {
            usersListResultLiveData.postValue(UsersListResult(Throwable("Please login first!")))
        } else {
            users_ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    usersList.clear()

                    for (s in snapshot.children) {
                        if (s != null) {
                            val user = s.getValue(User::class.java)
                            if (user?.id != null) {
                                if (user.id == currUser!!.uid) {
                                    databaseCurrUser = user
                                } else {
                                    usersList.add(user)
                                }
                            }
                        }
                    }

                    Log.d("TAGY", "onDataChange: Fetch Users Successful!")
                    getLastMessages()
                }

                override fun onCancelled(error: DatabaseError) {
                    usersListResultLiveData.postValue(UsersListResult(error.toException()))
                }
            })
        }
    }

    private fun getLastMessages() {
        var processingTasks = 0
        val currRef = database.getReference("Messages").child(currUser!!.uid)

        for (user in usersList) {
            processingTasks++
            currRef.child(user.id!!).orderByChild("time").limitToLast(1)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (snap in snapshot.children) {
                            user.lastMessage = snap.getValue(Message::class.java)
                        }

                        if (processingTasks == 0 || --processingTasks == 0) {
                            Log.d(
                                "TAGY",
                                "Fetched Last message"
                            )

                            CoroutineScope(IO).launch {
                                usersList.sortByDescending { user ->
                                    user.lastMessage?.time
                                }
                                usersListResultLiveData.postValue(UsersListResult(data = usersList))
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        usersListResultLiveData.postValue(UsersListResult(error.toException()))
                    }
                })
        }
    }

    /*    var startTime = System.currentTimeMillis()
        fun fetchUsers(): LiveData<UsersListResult> {
            if (currUser == null) {
                usersListResultLiveData.postValue(UsersListResult(Throwable("Please login first!")))
            } else {
                messages_ref = database.getReference("Messages").child(currUser!!.uid)
                usersList.clear()
                onGoingTasks = 0

                startTime = System.currentTimeMillis()
                users_ref.get().addOnSuccessListener {
                    Log.d("TAGY", "fetchUsers: Time Taken: ${System.currentTimeMillis() - startTime}")
                    startTime = System.currentTimeMillis()
                    for (snapshot in it.children) {
                        addUser(snapshot)
                    }
                }.addOnFailureListener {
                    usersListResultLiveData.postValue(UsersListResult(Throwable("Something went wrong!")))
                    it.printStackTrace()
                }
            }

            return usersListResultLiveData
        }

        private fun addUser(snapshot: DataSnapshot?) {
            if (snapshot == null)
                return

            val user = snapshot.getValue(User::class.java) ?: return

            if (user.id == null || user.id == currUser?.uid)
                return

            usersList.add(user)

            onGoingTasks++
            messages_ref.child(user.id!!).orderByChild("time").limitToLast(1)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        Log.d("TAGY", "onDataChange: $snapshot")
                        for (data in snapshot.children) {
                            user.lastMessage = data.getValue(Message::class.java)

                            if (--onGoingTasks == 0) {
                                Log.d(
                                    "TAGY",
                                    "fetchMessages: Time Taken: ${System.currentTimeMillis() - startTime}"
                                )

                                CoroutineScope(IO).launch {
                                    usersList.sortByDescending { user ->
                                        user.lastMessage?.time
                                    }
                                    usersListResultLiveData.postValue(UsersListResult(data = usersList))
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }
                })
        }*/
    fun getMessages(userId: String): LiveData<MessagesListResult> {
        val result = MutableLiveData<MessagesListResult>()
        val messageList = ArrayList<Message>()

        val currRef = database.getReference("Messages").child(currUser!!.uid).child(userId)
            .orderByChild("time")
        currRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                //Filter data
                messageList.clear()

                var lastTime: Long = 0
                var lastReceived: Boolean? = null
                for (s in snapshot.children) {
                    val msg = s.getValue(Message::class.java) ?: continue

                    val diff = msg.time!! - lastTime
                    if (lastReceived == msg.isReceived && diff < MAX_TIME_GROUP_MSG) {
                        lastTime = msg.time!!
                        messageList.add(msg)
                    } else {
                        messageList.add(
                            Message(
                                id = UUID.randomUUID().toString(),
                                time = msg.time,
                                isReceived = msg.isReceived,
                                type = Message.MSG_TYPE_PROFILE
                            )
                        )

                        messageList.add(msg)

                        lastReceived = msg.isReceived
                        lastTime = msg.time!!
                    }
                }

                result.postValue(MessagesListResult(data = messageList))
            }

            override fun onCancelled(error: DatabaseError) {
                result.postValue(MessagesListResult(Throwable(error.message)))
            }
        })

        return result
    }

    fun sendTextMessage(text: String, targetUser: User) {
        if (text.isEmpty())
            return

        val msgId = UUID.randomUUID().toString()
        val msgTime = System.currentTimeMillis()
        val msgForMe = Message(
            msgId,
            msgTime,
            Message.MSG_TYPE_TEXT,
            false,
            text
        )
        val msgForHim = Message(
            msgId,
            msgTime,
            Message.MSG_TYPE_TEXT,
            true,
            text
        )

        sendGeneralMessage(msgForMe, msgForHim, targetUser)
    }

    private fun sendGeneralMessage(msgForMe: Message, msgForHim: Message, targetUser: User) {
        //Save for me
        database.getReference("Messages").child(currUser!!.uid).child(targetUser.id!!)
            .child(msgForMe.id!!).setValue(msgForMe)

        //Save for him
        database.getReference("Messages").child(targetUser.id!!).child(currUser!!.uid)
            .child(msgForHim.id!!).setValue(msgForHim)
    }

    fun deleteForMe(msg: Message, otherUser: User) {
        database.getReference("Messages").child(currUser!!.uid).child(otherUser.id!!)
            .child(msg.id!!).removeValue()
    }

    fun deleteForEveryOne(msg: Message, otherUser: User) {
        database.getReference("Messages").child(currUser!!.uid).child(otherUser.id!!)
            .child(msg.id!!).removeValue()

        database.getReference("Messages").child(otherUser.id!!).child(currUser!!.uid)
            .child(msg.id!!).removeValue()
    }

    fun sendImage(context: Context, uri: Uri, targetUser: User) {
        CoroutineScope(IO).launch {
            val bitmap = decodeUri(context, uri, 720)
            if (bitmap == null)
                return@launch

            val id = UUID.randomUUID().toString()
            val image_ref = sent_images_bucket.child(id + ".jpg")
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()

            val uploadTask: UploadTask = image_ref.putBytes(data)
            uploadTask.addOnSuccessListener {
                image_ref.downloadUrl.addOnSuccessListener {
                    //Save to database
                    val msgTime = System.currentTimeMillis()
                    val msgForMe = Message(
                        id,
                        msgTime,
                        Message.MSG_TYPE_IMAGE,
                        false,
                        it.toString()
                    )

                    val msgForHim = Message(
                        id,
                        msgTime,
                        Message.MSG_TYPE_IMAGE,
                        true,
                        it.toString()
                    )

                    sendGeneralMessage(msgForMe, msgForHim, targetUser)
                }
            }
        }
    }

    fun decodeUri(c: Context, uri: Uri, requiredSize: Int): Bitmap? {
        val o = BitmapFactory.Options()
        o.inJustDecodeBounds = true
        BitmapFactory.decodeStream(c.getContentResolver().openInputStream(uri), null, o)
        var width_tmp = o.outWidth
        var height_tmp = o.outHeight
        var scale = 1
        while (true) {
            if (width_tmp / 2 < requiredSize || height_tmp / 2 < requiredSize) break
            width_tmp /= 2
            height_tmp /= 2
            scale *= 2
        }
        val o2 = BitmapFactory.Options()
        o2.inSampleSize = scale
        return BitmapFactory.decodeStream(c.getContentResolver().openInputStream(uri), null, o2)
    }

    /*    private fun getMessages(): LiveData<List<Message>> {
            messagesList.clear()
            currentUserMessagesRef = messages_ref.orderByChild("time")
            currentUserMessageListener =
                currentUserMessagesRef!!.addChildEventListener(object : ChildEventListener {
                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                        snapshot.getValue(Message::class.java)?.let { messagesList.add(it) }
                    }

                    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                        TODO("Not yet implemented")
                    }

                    override fun onChildRemoved(snapshot: DataSnapshot) {
                        TODO("Not yet implemented")
                    }

                    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                        TODO("Not yet implemented")
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }
                })

            return messagesListResult
        }

        fun clearListener() {
            if (currentUserMessagesRef != null && currentUserMessageListener != null)
                currentUserMessagesRef!!.removeEventListener(currentUserMessageListener!!)
        }*/

}