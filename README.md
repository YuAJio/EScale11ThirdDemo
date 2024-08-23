
# MySafe 3.0电子秤二次开发接口文档

----

### 目录1: 对接方式说明
	 支持多种对接方案:
	 方案1. 使用AIDL跨进程通讯直接读取OS程序的称重结果
	优点:实现快,重量同步OS显示,无需任何第三方框架支持 .
	缺点:需要一定的AIDL开发经验,了解服务的通讯流程.
	 方案2. 自行读取传感器数值并根据OS校准参数自行计算称重结果
	优点:称重结果灵活,允许自定义校准和计算结果.
	缺点:需要一定的滤波算法稳定称重结果,需要计算公式介入.

---

### 目录2: 对接流程大致梳理
	方案1:
	 1. 创建AIDL文件,创建AIDL绑定实现服务;
	 2. 搭建广播,通过广播通知OS执行服务的绑定以及一些称重操作;
	 3. 绑定服务后在需要称重时发送开启称重广播,接收OS发送的跨进程称重结果;
	 4. 渲染称重结果到页面;
	 5. 程序退出或确保不需要再获取重量后使用广播通知OS解绑服务,防止二次绑定服务失败;
	方案2:
	 1. 获取OS配置称重校准文件,获取称重AD值以及零点值;
	 2. 开始通过命令代码(demo中有示例)读取传感器信号;
	 3. 计算传感器信号为称重数值,并通过零点值和AD值计算实际称重重量结果;

---

### 目录3: 闪光灯控制
** 源码如下, 调用此方法 ** 
```
/**
 * 控制闪光灯
 * @param powerPercent 闪光灯强度 0 - 100  0为关闭  255为最大
 */
public void turnTheFlashLight(int powerPercent) {
    Process process = null;
    int result = -1;
    boolean isNeedResultMsg = true;
    BufferedReader successResult = null;
    BufferedReader errorResult = null;
    StringBuilder successMsg = null;
    StringBuilder errorMsg = null;
    DataOutputStream os = null;
    String echo = "echo ";
    String brightnessNode = " > /sys/devices/platform/backlight_led/backlight/backlight_led/brightness";

    String command = echo + powerPercent + brightnessNode;
    Log.i(TAG, "Set brightness string is:" + command);
    try {
        process = Runtime.getRuntime().exec("sh", null, null);
        os = new DataOutputStream(process.getOutputStream());
        os.write(command.getBytes());
        os.writeBytes(LINE_SEP);
        os.flush();

        os.writeBytes("exit" + LINE_SEP);
        os.flush();
        result = process.waitFor();
        if (isNeedResultMsg) {
            successMsg = new StringBuilder();
            errorMsg = new StringBuilder();
            successResult = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );
            errorResult = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8)
            );
            String line;
            if ((line = successResult.readLine()) != null) {
                successMsg.append(line);
                while ((line = successResult.readLine()) != null) {
                    successMsg.append(LINE_SEP).append(line);
                }
            }
            if ((line = errorResult.readLine()) != null) {
                errorMsg.append(line);
                while ((line = errorResult.readLine()) != null) {
                    errorMsg.append(LINE_SEP).append(line);
                }
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        try {
            if (os != null) {
                os.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (successResult != null) {
                successResult.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (errorResult != null) {
                errorResult.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (process != null) {
            process.destroy();
        }
    }
}
```
