package com.example.test

import androidx.appcompat.app.AppCompatActivity

open class BaseActivity: AppCompatActivity() {

    val permissionsHelper = PermissionsHelper()

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        this.permissionsHelper.onRequestPermissionsResult(
            this,
            requestCode,
            permissions,
            grantResults
        )
    }


}