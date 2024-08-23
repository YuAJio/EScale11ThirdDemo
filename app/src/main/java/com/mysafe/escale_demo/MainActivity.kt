package com.mysafe.escale_demo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.core.app.ComponentActivity
import androidx.databinding.DataBindingUtil
import com.mysafe.escale_demo.aidl_ways.AIDLUseCaseActivity
import com.mysafe.escale_demo.databinding.ActivityMainBinding
import com.mysafe.escale_demo.own_ways.CusCalActivity

@SuppressLint("RestrictedApi")
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding =
            DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        binding.acty = this
    }

    fun toAIDLActivity() {
        startActivity(Intent(this, AIDLUseCaseActivity::class.java))
    }

    fun toCusCalActivity() {
        startActivity(Intent(this, CusCalActivity::class.java))
    }
}
