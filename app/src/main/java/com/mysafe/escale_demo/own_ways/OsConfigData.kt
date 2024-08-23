package com.mysafe.escale_demo.own_ways

import com.google.gson.annotations.SerializedName

data class OsConfigData(
    @SerializedName("WeigtJingDuADValue")
    val adValue: Double,
    @SerializedName("WeighZeroValue")
    val zeroPoint: Long
)
