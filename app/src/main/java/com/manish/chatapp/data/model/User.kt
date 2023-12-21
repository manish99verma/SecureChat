package com.manish.chatapp.data.model

import java.io.Serializable

data class User(
    var name: String? = null,
    var id: String? = null,
    var email: String? = null,
    var profileUrl: String? = null,
    var lastMessage: Message? = null,
) : Serializable {
    override fun equals(other: Any?): Boolean {
        return if (other is User) {
            this.id.equals(other.id) && this.lastMessage == other.lastMessage
        } else {
            false
        }
    }
}
