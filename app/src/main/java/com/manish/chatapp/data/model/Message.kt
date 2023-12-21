package com.manish.chatapp.data.model

import java.io.Serializable

data class Message(
    var id: String? = null,
    var time: Long? = null,
    var type: Int? = null,
    var isReceived: Boolean? = null,
    var msg: String? = null,
) : Serializable {
    companion object {
        @JvmStatic
        val MSG_TYPE_TEXT = 0

        @JvmStatic
        val MSG_TYPE_IMAGE = 1

        @JvmStatic
        val MSG_TYPE_PROFILE = 2
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Message) {
            this.id == other.id && this.time == other.time && this.type == other.type && this.isReceived == other.isReceived && this.msg == other.msg
        } else {
            false
        }
    }
}