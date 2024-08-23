package com.mysafe.escale_demo.own_ways

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * 本地称重重量获取工具
 */
object SerialReadTool {

    /**
     * 读取本地校准文件,获取称重重量校准数值
     * @return 校准参数
     */
    fun getLocalSettingFile(): OsConfigData? {
        val file = File(
            Environment.getExternalStorageDirectory(),
            "Documents/EscaleFile/ProjectData/Setting.txt"
        )
        return if (file.exists()) {
            val content = file.readText()
            val deContent =
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
                    decryptAES("YukihoHagiwarass" , content)
                } else
                    ""
            Gson().fromJson<OsConfigData>(
                deContent,
                object : TypeToken<OsConfigData>() {}.type
            )
        } else {
            println("File not found")
            null
        }
    }

    /**
     * 解密设置参数文件内容
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun decryptAES(key: String, toDecrypt: String): String {
        val keyArray = key.toByteArray(StandardCharsets.UTF_8)
        val toEncryptArray = Base64.getDecoder().decode(toDecrypt)

        val secretKey = SecretKeySpec(keyArray, "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)

        val resultArray = cipher.doFinal(toEncryptArray)
        return String(resultArray, StandardCharsets.UTF_8)
    }


    /**
     * 通过命令获取传感器称重数值
     */
    fun readSerialData(): String {
        var process: Process? = null
        var result = -1
        var successResult: BufferedReader? = null
        var errorResult: BufferedReader? = null
        var successMsg: StringBuilder? = null
        var errorMsg: StringBuilder? = null
        var os: DataOutputStream? = null
        val command = "cat /sys/bus/iio/devices/iio:device0/in_voltage0_raw"
        try {
            process = Runtime.getRuntime().exec("sh", null, null)
            os = DataOutputStream(process.outputStream)
            os.write(command.toByteArray())
            os.writeBytes(System.getProperty("line.separator"))
            os.flush()
            os.writeBytes("exit" + System.getProperty("line.separator"))
            os.flush()
            result = process.waitFor()
            successMsg = StringBuilder()
            errorMsg = StringBuilder()
            successResult = BufferedReader(
                InputStreamReader(process.inputStream, StandardCharsets.UTF_8)
            )
            errorResult = BufferedReader(
                InputStreamReader(process.errorStream, StandardCharsets.UTF_8)
            )
            var line: String?
            if (successResult.readLine().also { line = it } != null) {
                successMsg.append(line)
                while (successResult.readLine().also { line = it } != null) {
                    successMsg.append(System.getProperty("line.separator")).append(line)
                }
            }
            if (errorResult.readLine().also { line = it } != null) {
                errorMsg.append(line)
                while (errorResult.readLine().also { line = it } != null) {
                    errorMsg.append(System.getProperty("line.separator")).append(line)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                os?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            try {
                successResult?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            try {
                errorResult?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            process?.destroy()
        }
        return successMsg.toString()
    }
}