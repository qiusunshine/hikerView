package com.example.hikerview.utils.view

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.example.hikerview.R
import com.example.hikerview.utils.ScreenUtil
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BasePopupView
import com.lxj.xpopup.interfaces.OnSelectListener

/**
 * 作者：By 15968
 * 日期：On 2021/12/2
 * 时间：At 15:02
 */
object DialogUtil {

    private fun useCardBg(alertDialog: AlertDialog) {
        alertDialog.window?.setBackgroundDrawableResource(R.drawable.shape_dialog_cardbg)
    }

    fun showAsCard(context: Context, alertDialog: AlertDialog) {
        showAsCard(context, alertDialog, 4)
    }

    fun showAsCard(context: Context, alertDialog: AlertDialog, w: Int = 4) {
        useCardBg(alertDialog)
        alertDialog.show()
        if (alertDialog.window != null) {
            val lp = alertDialog.window!!.attributes
            val width = ScreenUtil.getScreenMin(context)
            if (width > 0) {
                lp.width = w * width / 5
                alertDialog.window!!.attributes = lp
            }
        }
    }

    private fun getPopupWidth(context: Context, w: Int = 4): Int {
        val width = ScreenUtil.getScreenMin(context)
        return w * width / 5
    }

    fun showCenterList(
        context: Context,
        list: Array<String>,
        listener: OnSelectListener
    ): BasePopupView {
        return showCenterList(context, null, list, listener)
    }

    fun showCenterList(
        context: Context,
        title: String?,
        list: Array<String>,
        listener: OnSelectListener
    ): BasePopupView {
        return XPopup.Builder(context)
            .popupWidth(getPopupWidth(context, 3))
            .asCenterList(title, list, null, -1, listener, 0, R.layout.popup_center_list_item)
            .show()
    }
}