/*
 * Copyright (C) 2013 poster PCE YoungSee Inc.
 * All Rights Reserved Proprietary and Confidential.
 * 
 * @author LiLiang-Ping
 */

package com.youngsee.osd;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.View;

import com.youngsee.common.DialogUtil;
import com.youngsee.common.DialogUtil.DialogDoubleButtonListener;
import com.youngsee.common.DialogUtil.DialogSingleButtonListener;
import com.youngsee.common.FileUtils;
import com.youngsee.posterdisplayer.PosterApplication;
import com.youngsee.posterdisplayer.PosterOsdActivity;
import com.youngsee.posterdisplayer.R;

public class CleanDisk
 {
	private Context mContext = null;
	private ProgressDialog mProgressDlg = null;

	// Define message ID
	private final static int PROGRAM_CLEANING = 0x7000;
	private final static int PROGRAM_CLEAN_FAILED = 0x7001;
	private final static int PROGRAM_CLEAN_SUCCESS = 0x7002;

	private Dialog dlgClearSuccess = null;
	private Dialog dlgClearFailure = null;
	
	public CleanDisk(Context context) {
		mContext = context;
		mProgressDlg = new ProgressDialog(mContext);
		mProgressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER); // 设置进度条风格，风格为圆形，旋转的
		mProgressDlg.setTitle(R.string.tools_dialog_clear_title); // 设置ProgressDialog
																	// 标题
		mProgressDlg.setMessage(mContext.getString(R.string.tools_dialog_clear_message)); // 设置ProgressDialog
																							// 提示信息
		mProgressDlg.setIndeterminate(false); // 设置ProgressDialog 的进度条是否不明确
		mProgressDlg.setCancelable(false); // 设置ProgressDialog 是否可以按退回按键取消
	}

	public void cleanProgram() {
		new CleanDiskThread().start();
	}

	private final class CleanDiskThread extends Thread {
		@Override
		public void run() {
			mHandler.sendEmptyMessage(PROGRAM_CLEANING);

			if (FileUtils.delDir(PosterApplication.getProgramPath())) {
				mHandler.sendEmptyMessage(PROGRAM_CLEAN_SUCCESS);
			} else {
				mHandler.sendEmptyMessage(PROGRAM_CLEAN_FAILED);
			}
		}
	}

	@SuppressLint({ "HandlerLeak", "ShowToast" })
	final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case PROGRAM_CLEANING:
				if (mProgressDlg != null && !mProgressDlg.isShowing()) {
					mProgressDlg.show();
				}
				return;

			case PROGRAM_CLEAN_SUCCESS:
				if (mProgressDlg != null && mProgressDlg.isShowing()) {
					mProgressDlg.dismiss();
				}

				dlgClearSuccess = DialogUtil.showTipsDialog(mContext, mContext.getString(R.string.tools_dialog_clear_success), mContext.getString(R.string.enter), new DialogSingleButtonListener() {

					@Override
					public void onSingleClick(Context context , View v , int which) {
						PosterOsdActivity.INSTANCE.setDismissTime();
						if (dlgClearSuccess != null) {
							dlgClearSuccess.dismiss();
							dlgClearSuccess = null;
						}
					}

				}, false);
				
				dlgClearSuccess.show();
				
				DialogUtil.dialogTimeOff(dlgClearSuccess,90000);
				
				return;

			case PROGRAM_CLEAN_FAILED:
				if (mProgressDlg != null && mProgressDlg.isShowing()) {
					mProgressDlg.dismiss();
				}
				dlgClearFailure = DialogUtil.showTipsDialog(mContext, mContext.getString(R.string.tools_dialog_clear_failure), mContext.getString(R.string.retry1), mContext.getString(R.string.cancel), new DialogDoubleButtonListener() {

					@Override
					public void onLeftClick(Context context , View v , int which) {
						cleanProgram();
						
						if(dlgClearFailure != null){
							dlgClearFailure.dismiss();
							dlgClearFailure = null;
						}
						
					}

					@Override
					public void onRightClick(Context context , View v , int which) {
						PosterOsdActivity.INSTANCE.setDismissTime();
						if(dlgClearFailure != null){
							dlgClearFailure.dismiss();
							dlgClearFailure = null;
						}
					}

				}, false);
				
				dlgClearFailure.show();
				
				DialogUtil.dialogTimeOff(dlgClearFailure,90000);
				

				return;
			}

			super.handleMessage(msg);
		}
	};
}
