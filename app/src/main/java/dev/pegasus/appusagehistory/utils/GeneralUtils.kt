package dev.pegasus.appusagehistory.utils

import android.app.Activity
import android.widget.Toast

/**
 * @Author: SOHAIB AHMED
 * @Date: 15,March,2023
 * @Accounts
 *      -> https://github.com/epegasus
 *      -> https://stackoverflow.com/users/20440272/sohaib-ahmed
 */

object GeneralUtils {

    fun Activity.showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}