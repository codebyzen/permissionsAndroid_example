package com.example.test

import android.os.Bundle
import android.util.Log

class MainActivity : BaseActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        permissionsHelper.requestAppPermission(this) { this.goNextScreen() }


    }


    private fun goNextScreen() {
        Log.d("***", "GO NEXT!")
    }








}