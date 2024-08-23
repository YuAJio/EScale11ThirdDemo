package com.mysafe.escale_demo.aidl_ways

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.ComponentActivity
import androidx.databinding.DataBindingUtil
import com.mysafe.escale_demo.IWeighCmd
import com.mysafe.escale_demo.R
import com.mysafe.escale_demo.databinding.ActivityWeighBinding
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * 使用AIDL做称重数据获取的示例页面
 */
@SuppressLint("RestrictedApi")
class AIDLUseCaseActivity : ComponentActivity(), IWeighCmd {

    /**
     * 广播指令
     */
    private companion object BroadcastCmd {
        //广播Action , 用于指定用来通讯的广播
        const val ReceiverAction = "com.mysafe.escale.ACTION.WEIGHT_CMD_BROADCAST"

        //通讯广播所属的包名, 用于兼容高版本不允许直接和外部广播通讯的问题
        const val ReceiverPackageName = "com.cdmysafe.ccsafe.uchengos"

        //广播指令 : 控制绑定重量获取服务(初始化获取称重服务时)
        const val WeighAction_WeighServiceBind = "com.mysafe.escales.action.WEIGH_SERVICE_BIND"

        //广播指令 : 控制解绑重量获取服务(销毁称重服务时或者关闭APP时调用)
        const val WeighAction_WeighServiceUnbind = "com.mysafe.escales.action.WEIGH_SERVICE_UNBIND"

        //广播指令 : 控制重量置零
        const val WeighAction_SetZero = "com.mysafe.escales.action.SET_ZERO";

        //广播指令 : 开始获取称重重量数据(用于需要获取重量时)
        const val WeighAction_StartReadWeight = "com.mysafe.escales.action.START_READ"

        //广播指令 : 暂时停止获取称重重量数据(用于不需要获取重量时)
        const val WeighAction_StopReadWeight = "com.mysafe.escales.action.STOP_READ"
    }

    private lateinit var binding: ActivityWeighBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_weigh)
        binding.type = 1
        binding.cmd = this
        binding.weightValue = "请先绑定服务,再开启称重开关"

        //先开启服务,避免在发送绑定服务指令时找不到需要绑定的服务
        startWeightService()
        EventBus.getDefault().register(this)
    }

    /**
     * 无需显式调用,GR会根据订阅者自行调用方法
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: OsWeightService.WeighChangedEvent) {
        binding.weightValue = String.format("%.2f", event.weighValue)
    }

    /**
     * 开启AIDL通讯衔接服务
     */
    private fun startWeightService() {
        //开启服务
        val intent = Intent(this, OsWeightService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)//做android11兼容
            startForegroundService(intent)
        else
            startService(intent)
    }


    /**
     * 组装通用服务IntentAction
     */
    private fun getBroadcastDefaultIntent(): Intent {
        val intent = Intent()
        intent.action = ReceiverAction
        intent.setPackage(ReceiverPackageName)
        return intent
    }

    /**
     * 通过广播向OS程序发送称重指令
     * @param cmd 广播指令内容
     */
    private fun sendBroadcastToOs(cmd: String) {
        val intent = getBroadcastDefaultIntent()
        intent.putExtra("cmd", cmd)
        sendBroadcast(intent)
    }


    override fun startWeigh() {
        sendBroadcastToOs(WeighAction_StartReadWeight)
    }

    override fun stopWeigh() {
        sendBroadcastToOs(WeighAction_StopReadWeight)
    }

    override fun setZero() {
        sendBroadcastToOs(WeighAction_SetZero)
    }

    override fun enable() {
        //绑定广播需要指定当前程序包名,才能让OS有效指向被绑定的服务
        val intent = getBroadcastDefaultIntent()
        intent.putExtra("cmd", WeighAction_WeighServiceBind)
        intent.putExtra("package", this.packageName)
        sendBroadcast(intent)
    }

    override fun release() {
        sendBroadcastToOs(WeighAction_WeighServiceUnbind)
    }

}