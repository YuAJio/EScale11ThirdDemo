//路径必须与OS保持同步
package com.mysafe.escale.aidl;

/**
* 注意点:
* 名称不可修改,路径需要保持一致
*/
interface IWeighValue {
    /**
    * 获取重量接口
    * value : 重量
    * stable : 当前重量是否稳定
    */
 void GetWeighValue(double value,boolean stable);
}