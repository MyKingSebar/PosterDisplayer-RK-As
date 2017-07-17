package com.youngsee.authorization;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.youngsee.common.DialogUtil;
import com.youngsee.common.DialogUtil.DialogDoubleButtonListener;
import com.youngsee.common.DialogUtil.DialogSingleButtonListener;
import com.youngsee.common.FileUtils;
import com.youngsee.logmanager.Logger;
import com.youngsee.posterdisplayer.PosterApplication;
import com.youngsee.posterdisplayer.R;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;

@SuppressLint("DefaultLocale")
public class AuthorizationHelper {
	private final static int EVENT_START = 0x8000;
    private final static int EVENT_SUCCESS = 0x8001;
    private final static int EVENT_FAILURE = 0x8002;
    
    private final static int PDLGTYPE_GETDEVINFO = 0;
    private final static int PDLGTYPE_IMPORTAUTHCODE = 1;
    private final static int PDLGTYPE_IMPORTKEY = 2;
    private final static int PDLGTYPE_UPDATEKEY = 3;
	
	private Context mContext = null;
    private ProgressDialog mProgressDlg = null;
	
	private OnStatusListener mListener = null;
	private Dialog dlg = null;
	
	public interface OnStatusListener {
        public void onCompleted();
    }
	
	public AuthorizationHelper(Context context, String pdlgtitle, String pdlgmsg) {
		mContext = context;
		mProgressDlg = new ProgressDialog(mContext);
		mProgressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProgressDlg.setTitle(pdlgtitle);
		mProgressDlg.setMessage(pdlgmsg);
		mProgressDlg.setIndeterminate(false);
		mProgressDlg.setCancelable(false);
	}
	
	public void setOnStatusListener(OnStatusListener l) {
		mListener = l;
	}
	
	private String getDevInfoSavePath() {
		List<String> listUsbPath = FileUtils.getUsbPathList();
		if (listUsbPath == null || listUsbPath.isEmpty())
		{
			Logger.i("Didn't found Usb path.");
			return null;
		}
		
		long currMaxSpace = 0;
    	String savePath = null;
    	long fileSpace = 0;
    	for (int i = 0; i < listUsbPath.size(); i++)
    	{
    		fileSpace = (new File(listUsbPath.get(i))).getUsableSpace();
    		if (fileSpace > currMaxSpace)
    		{
    			savePath = listUsbPath.get(i);
    			currMaxSpace = fileSpace;
    		}
    	}
    	return savePath;
    }
	
	private boolean doExportDevInfo() {
		String path = getDevInfoSavePath();
		if (path != null) {
			String mac = AuthorizationCommon.getMac(PosterApplication.getEthMacAddress()).toUpperCase();
			String cpuid = PosterApplication.getCpuId().toUpperCase();
			StringBuilder sb = new StringBuilder();
			sb.append(path);
			sb.append(File.separator);
			sb.append(AuthorizationCommon.PREFIX_DEVINFO_FILE);
			sb.append("-");
			sb.append(cpuid);
			sb.append(".");
			sb.append(AuthorizationCommon.EXT_DEVINFO_FILE);
			File file = new File(sb.toString());
			if(!file.exists()){
			     try{
			         file.createNewFile();
			     }
			     catch(IOException exception){
			         exception.printStackTrace();
			     }
			}
			
			sb.setLength(0);
			sb.append("MAC : ");
			sb.append(mac);
			sb.append("\r\n");
			sb.append("CPUID : ");
			sb.append(cpuid);
			String content = sb.toString();
			
			FileUtils.writeTextToFile(file.getPath(), content);
			return true;
		}
		return false;
	}
	
	public void exportDevInfoToUDisk() {
		new Thread() {
			@Override
	        public void run() {
				sendEventStart(PDLGTYPE_GETDEVINFO);
				if (doExportDevInfo()) {
					sendEventSuccess(PDLGTYPE_GETDEVINFO);
				} else {
					sendEventFailure(PDLGTYPE_GETDEVINFO);
				}
			}
		}.start();
	}
	
	private boolean doImportAuthCode() {
		String cpuid = PosterApplication.getCpuId().toUpperCase();
		StringBuilder sb = new StringBuilder();
		sb.append(AuthorizationCommon.PREFIX_AUTHCODE_FILE);
		sb.append("-");
		sb.append(cpuid);
		sb.append(".");
		sb.append(AuthorizationCommon.EXT_AUTHCODE_FILE);
		String filename = sb.toString();
		
		String path = FileUtils.findFilePath(filename);
		if (path != null) {
            FileUtils.saveAuthCodeToDBFile(path);
			return true;
		}
		return false;
	}
	
	public void importAuthCodeFromUDisk() {
		new Thread() {
			@Override
	        public void run() {
				sendEventStart(PDLGTYPE_IMPORTAUTHCODE);
				if (doImportAuthCode()) {
					sendEventSuccess(PDLGTYPE_IMPORTAUTHCODE);
				} else {
					sendEventFailure(PDLGTYPE_IMPORTAUTHCODE);
				}
			}
		}.start();
	}
	
	private boolean doImportOrUpdateKey() {
		String path = FileUtils.findFilePath(AuthorizationCommon.PUBLICKEY_FILE);
		if (path != null) {
			FileUtils.saveKeyToDBFile(path);
			return true;
		} else {
			path = FileUtils.findFilePath(AuthorizationCommon.PRIVATEKEY_FILE);
			if (path != null) {
			    FileUtils.saveKeyToDBFile(path);
				return true;
			}
		}
		return false;
	}
	
	public void importKeyFromUDisk() {
		new Thread() {
			@Override
	        public void run() {
				sendEventStart(PDLGTYPE_IMPORTKEY);
				if (doImportOrUpdateKey()) {
					sendEventSuccess(PDLGTYPE_IMPORTKEY);
				} else {
					sendEventFailure(PDLGTYPE_IMPORTKEY);
				}
			}
		}.start();
	}
	
	public void updateKeyFromUDisk() {
		new Thread() {
			@Override
	        public void run() {
				sendEventStart(PDLGTYPE_UPDATEKEY);
				if (doImportOrUpdateKey()) {
					sendEventSuccess(PDLGTYPE_UPDATEKEY);
				} else {
					sendEventFailure(PDLGTYPE_UPDATEKEY);
				}
			}
		}.start();
	}
	
	private void sendEventStart(int type) {
		Bundle bundle = new Bundle();
        bundle.putInt("type", type);
        Message msg = mHandler.obtainMessage();
        msg.what = EVENT_START;
        msg.setData(bundle);
        msg.sendToTarget();
	}
	
	private void sendEventSuccess(int type) {
		Bundle bundle = new Bundle();
        bundle.putInt("type", type);
        Message msg = mHandler.obtainMessage();
        msg.what = EVENT_SUCCESS;
        msg.setData(bundle);
        msg.sendToTarget();
	}
	
	private void sendEventFailure(int type) {
		Bundle bundle = new Bundle();
        bundle.putInt("type", type);
        Message msg = mHandler.obtainMessage();
        msg.what = EVENT_FAILURE;
        msg.setData(bundle);
        msg.sendToTarget();
	}
	
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
		@Override
        public void handleMessage(Message msg) {
			int residtitle = -1;
			final int type = msg.getData().getInt("type");
			switch (msg.what) {
			case EVENT_START:
				if (mProgressDlg != null && !mProgressDlg.isShowing()) {
					mProgressDlg.show();
				}
				break;
			case EVENT_SUCCESS:
				if (mListener != null) {
					mListener.onCompleted();
				}
				if (mProgressDlg != null && mProgressDlg.isShowing()) {
					mProgressDlg.dismiss();
				}
				switch (type) {
				case PDLGTYPE_GETDEVINFO:
					residtitle = R.string.exportdevinfo_success_title;
					break;
				case PDLGTYPE_IMPORTAUTHCODE:
					residtitle = R.string.importauthcode_success_title;
					break;
				case PDLGTYPE_IMPORTKEY:
					residtitle = R.string.importkey_success_title;
					break;
				case PDLGTYPE_UPDATEKEY:
					residtitle = R.string.updatekey_success_title;
					break;
				}
				if (residtitle != -1) {
					dlg = DialogUtil.showTipsDialog(mContext, mContext.getString(residtitle), mContext.getString(R.string.enter), new DialogSingleButtonListener() {

						@Override
						public void onSingleClick(Context context , View v , int which) {
							if (dlg != null) {
								dlg.dismiss();
								dlg = null;
							}
						}

					}, false);

					dlg.show();
					
					DialogUtil.dialogTimeOff(dlg,90000);
				}
				break;
			case EVENT_FAILURE:
				if (mProgressDlg != null && mProgressDlg.isShowing()) {
					mProgressDlg.dismiss();
				}
				switch (type) {
				case PDLGTYPE_GETDEVINFO:
					residtitle = R.string.exportdevinfo_failure_title;
					break;
				case PDLGTYPE_IMPORTAUTHCODE:
					residtitle = R.string.importauthcode_failure_title;
					break;
				case PDLGTYPE_IMPORTKEY:
					residtitle = R.string.importkey_failure_title;
					break;
				case PDLGTYPE_UPDATEKEY:
					residtitle = R.string.updatekey_failure_title;
					break;
				}
				if (residtitle != -1) {
					dlg = DialogUtil.showTipsDialog(mContext, mContext.getString(residtitle), mContext.getString(R.string.retry1), mContext.getString(R.string.cancel), new DialogDoubleButtonListener() {

						@Override
						public void onLeftClick(Context context , View v , int which) {
							switch (type) {
							case PDLGTYPE_GETDEVINFO:
								exportDevInfoToUDisk();
								break;
							case PDLGTYPE_IMPORTAUTHCODE:
								importAuthCodeFromUDisk();
								break;
							case PDLGTYPE_IMPORTKEY:
								importKeyFromUDisk();
								break;
							case PDLGTYPE_UPDATEKEY:
								updateKeyFromUDisk();
								break;
							}
							if (dlg != null) {
								dlg.dismiss();
								dlg = null;
							}
						}

						@Override
						public void onRightClick(Context context , View v , int which) {
							if (dlg != null) {
								dlg.dismiss();
								dlg = null;
							}
						}

					}, false);
					
					dlg.show();
					
					DialogUtil.dialogTimeOff(dlg,90000);
				}
				return;
			}
			super.handleMessage(msg);
		}
	};
}
