package com.mysafe.escale_demo.aidl_ways

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.MutableLiveData
import com.mysafe.escale.aidl.IWeighValue
import com.mysafe.escale_demo.tools.ForegroundProvider
import org.greenrobot.eventbus.EventBus

class OsWeightService : Service() {

    data class WeighChangedEvent(val weighValue: Double, val isStable: Boolean)

    override fun onBind(p0: Intent?): IBinder? {
        //创建binder实例,用于绑定aidl接口
        val _binder = WeighValueBinder()
        return _binder
    }

    override fun onCreate() {
        super.onCreate()
        //region android11 兼容
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ForegroundProvider.startForeground(
                this,
                "${0x11}",
                "WEIGH_VALUE_SERVICE",
                0x11
            )
        }
        //endregion
    }


    /**
     * 获取AIDL传输数据接口实现类
     */
    inner class WeighValueBinder : IWeighValue.Stub(), IWeighValue {
        override fun GetWeighValue(value: Double, stable: Boolean) {
            println("Weigh receiver_value:$value,Stable:$stable")
            //可选任意方式回调获取数据,这里使用的是GreenRobot框架的EventBus
            EventBus.getDefault().post(WeighChangedEvent(value, stable))
        }
    }
}