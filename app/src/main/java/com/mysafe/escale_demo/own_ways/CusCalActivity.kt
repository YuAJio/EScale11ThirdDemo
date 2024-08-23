package com.mysafe.escale_demo.own_ways

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.mysafe.escale_demo.IWeighCmd
import com.mysafe.escale_demo.R
import com.mysafe.escale_demo.databinding.ActivityWeighBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

@SuppressLint("RestrictedApi")
class CusCalActivity : ComponentActivity(), IWeighCmd {

    private lateinit var binding: ActivityWeighBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_weigh)
        binding.type = 2
        binding.weightValue = "权限申请中..."
        binding.cmd = this
        //检测文件管理权限
        checkStoragePermission()
    }


    /**
     * 全部权限通过后,开始获取本地称重校准文件
     */
    private fun allPermissionsGranted() {
        binding.weightValue = "读取配置中....."
        val settings = SerialReadTool.getLocalSettingFile()
        if (settings == null) {
            Toast.makeText(this, "解析称重校准参数文件失败,使用默认校准参数", Toast.LENGTH_SHORT)
                .show()
            //设置AD默认值
            setCaliParams(0, 0.333333)
        } else {
            //设置OS校准参数
            setCaliParams(settings.zeroPoint, settings.adValue)
        }
        binding.weightValue = "点击打开,开始计算重量"
    }


    //region 称重获取相关
    /**
     * 零点值(从参数中读取可同步零点值,可选则手动置零)
     */
    private var zeroPointValue = 0L

    /**
     *预置零点值,用于置零
     */
    private var setZerPointValue = 0L

    /**
     * 校准核心参数,用于计算当前重量
     */
    private var caliADValue = 0.0

    /**
     * 控制获取开关
     */
    private var enableRead = false

    /**
     * 上一次获取的称重重量,用于计算差值和滤波
     */
    private var lastCleanValue = 0L

    /**
     * 读取线程
     */
    private var readJob: Job? = null

    /**
     * 设置校准参数
     */
    private fun setCaliParams(zero: Long, ad: Double) {
        this.zeroPointValue = zero
        this.caliADValue = ad
    }

    /**
     * 开启读取重量线程
     */
    private fun initReadThread() {
        enableRead = true
        if (readJob == null) {
            readJob = lifecycleScope.launch {
                while (isActive) {
                    if (enableRead) {
                        val readValue = SerialReadTool.readSerialData()
                        val cleanValue = readValue.substring(0, readValue.length - 2)
                        val cleanValueL = cleanValue.toLong()
                        calActuallyWeight(cleanValueL)
                    }
                    delay(300)
                }
            }
        }
    }

    /**
     * 计算真实重量数值
     * TODO : 计算结果可能会存在不稳定的情况,是因为传感器会受到多方因素的干扰,可以选择性添加一定的滤波算法来稳定最终的值
     */
    private fun calActuallyWeight(readWeight: Long) {
        //设置预制零点值,用于置零
        setZerPointValue = readWeight
        //当前重量减去零点值乘以精度,获取重量纯净值
        var cleanValue = (readWeight - zeroPointValue) * 20//精度 单位g

        //做简易滤波
        if (abs(cleanValue - lastCleanValue) <= 1)
            cleanValue = lastCleanValue
        else
            lastCleanValue = cleanValue
        if (abs(cleanValue) <= 20)
            cleanValue = 0

        if (caliADValue <= 0)//被除数不可为0,在计算时先做判断如果是0则修改为默认AD值
            caliADValue = 0.3333
        val calValue = cleanValue / caliADValue
        onWeight(calValue)
    }

    /**
     * 获取称重值,做对应处理刷新页面数值
     */
    private fun onWeight(weighValue: Double) {
        binding.weightValue = String.format("%.2f", weighValue)
    }
    //endregion

    //region 权限请求相关


    private fun checkStoragePermission() {
        if (!isStoragePermissionsGranted()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                toOpenAllStoragePermission()
            else
                toRequestPermissions()
        } else
            toRequestPermissions()

    }

    private fun toRequestPermissions() {
        val permissions = arrayListOf(
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
            permissions.addAll(
                arrayListOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                )
            )
        val pList = arrayListOf<String>()
        permissions.forEach {
            if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED)
                pList.add(it)
        }
        if (pList.any()) {
            requestPermissions(pList.toTypedArray(), 2)
        } else
            allPermissionsGranted()
    }

    private fun isStoragePermissionsGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            Environment.isExternalStorageManager()
        else {
            val readResult =
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            val writeResult =
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            readResult == PackageManager.PERMISSION_GRANTED && writeResult == PackageManager.PERMISSION_GRANTED
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun toOpenAllStoragePermission() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            .also {
                it.addCategory("android.intent.category.DEFAULT")
                it.data = Uri.parse("package:$packageName")
            }
        startActivityForResult(intent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        checkStoragePermission()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        checkStoragePermission()
    }
    //endregion


    override fun startWeigh() {
        enableRead = true
    }

    override fun stopWeigh() {
        enableRead = false
    }

    override fun setZero() {
        //将零点值设置为预制零点值,达到置零效果
        zeroPointValue = setZerPointValue
    }

    override fun enable() {
        initReadThread()
    }

    override fun release() {
        //useless 此处对获取的控制仅通过是否调用读取重量方法来实现,可直接使用stopWeigh代替

    }


}