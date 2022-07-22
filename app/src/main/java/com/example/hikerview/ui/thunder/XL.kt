package com.example.hikerview.ui.thunder

import com.example.hikerview.service.parser.JSEngine

/**
 * 作者：By 15968
 * 日期：On 2022/6/13
 * 时间：At 15:53
 */
object XL {

    var key = "ce123e25055f125a2232fde"
    var ver = "Y2VlMjUwNTVmMTI1YTJmZGUw"
    var cd = ""
    var cd2 = ""

    fun getA(): String {
        if (cd.isEmpty()) {
            val cd3 = JSEngine.getInstance().base64Decode(ver)
            cd = JSEngine.getInstance().base64Decode("YXh6TmpBd01RXl55Yj09MF44NTJeMDgzZGJjZmZe")
                .substring(1) + cd3.substring(0, cd3.length - 1)
        }
        return cd
    }

    fun getB(): String {
        if (cd2.isEmpty()) {
            cd2 = JSEngine.getInstance().base64Decode("MjEuMDEuMDcuODAwMDAy")
        }
        return cd2
    }
}