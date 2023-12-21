package com.manish.chatapp.ui.utils

import java.text.SimpleDateFormat

object UtilsMethods {
    @JvmStatic
    private val format = SimpleDateFormat("HH:mm a")

    @JvmStatic
    private val format2 = SimpleDateFormat("dd MMM")

    @JvmStatic
    private val oneDay: Long = 24 * 60 * 60 * 1000

    @JvmStatic
    fun formatDate(time: Long?): String {
        if (time == null)
            return ""

        val diff = System.currentTimeMillis() - time
        if (diff > oneDay) {
            if (diff < oneDay * 2)
                return "Yesterday"
            return format2.format(time)
        }

        return format.format(time)
    }

    @JvmStatic
    fun formatTimeWithLastSeen(time: Long?): String {
        if (time == null)
            return ""
        if (System.currentTimeMillis() - time < 10 * 1000)
            return "Online"
        return "Last seen at ${formatDate(time)}"
    }
}