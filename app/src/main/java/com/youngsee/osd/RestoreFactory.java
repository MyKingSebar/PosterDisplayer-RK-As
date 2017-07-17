/*
 * Copyright (C) 2013 poster PCE YoungSee Inc.
 * All Rights Reserved Proprietary and Confidential.
 * 
 * @author LiLiang-Ping
 */

package com.youngsee.osd;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import com.youngsee.common.DialogUtil;
import com.youngsee.posterdisplayer.R;
import com.youngsee.posterdisplayer.PosterApplication;

public class RestoreFactory
{ 
    private Context mContext = null;
    private ProgressDialog mProgressDlg = null;
    private Dialog 						dlg 										= null;

	public RestoreFactory(Context context) {
		mContext = context;
		mProgressDlg = new ProgressDialog(mContext);
		mProgressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER); // 设置进度条风格，风格为圆形，旋转的
		mProgressDlg.setTitle(R.string.tools_dialog_factory_titles); // 设置ProgressDialog
																		// 标题
		mProgressDlg.setMessage(context.getString(R.string.tools_dialog_factory_message)); // 设置ProgressDialog
																							// 提示信息
		mProgressDlg.setIndeterminate(false); // 设置ProgressDialog 的进度条是否不明确
		mProgressDlg.setCancelable(false); // 设置ProgressDialog 是否可以按退回按键取消
	}

	public void factoryRestore() {
		if (mProgressDlg != null && !mProgressDlg.isShowing()) {
			mProgressDlg.show();
		}

		PosterApplication.getInstance().factoryRest();

		if (mProgressDlg != null && mProgressDlg.isShowing()) {
			mProgressDlg.dismiss();
		}

		dlg = DialogUtil.showTipsDialog(mContext, mContext.getString(R.string.tools_dialog_factory_success), mContext.getString(R.string.enter), null, false);
		dlg.show();
		DialogUtil.dialogTimeOff(dlg, 90000);
	}
}
