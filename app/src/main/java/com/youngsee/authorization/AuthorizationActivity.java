package com.youngsee.authorization;

import com.youngsee.authorization.AuthCodeWindow.OnCodeListener;
import com.youngsee.authorization.AuthorizationManager.OnAuthStatusListener;
import com.youngsee.customview.WaitingDialog;
import com.youngsee.network.HttpResultHandler;
import com.youngsee.network.JSONUtil;
import com.youngsee.network.NetWorkUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentHashMap;

import com.youngsee.common.Base64Utils;
import com.youngsee.common.Contants;
import com.youngsee.common.DbHelper;
import com.youngsee.common.DialogUtil;
import com.youngsee.common.DialogUtil.DialogDoubleButtonListener;
import com.youngsee.common.FileUtils;
import com.youngsee.common.RSAUtils;
import com.youngsee.common.SysParamManager;
import com.youngsee.posterdisplayer.PosterApplication;
import com.youngsee.posterdisplayer.R;
import com.youngsee.webservices.WsClient;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint({ "DefaultLocale", "HandlerLeak" })
public class AuthorizationActivity extends Activity implements OnClickListener {
	private static final String TAG = "AuthorizationActivity";

	public static AuthorizationActivity INSTANCE = null;

	private static final int EVENT_UPDATE_MSG_AUTHSUCCEEDED = 0x8000;
	private static final int EVENT_UPDATE_MSG_KEYDOWNLOADED = 0x8001;
	private static final int EVENT_START_AUTO_AUTHORIZE = 0x8002;

	private PowerManager mPowerManager = null;
	private WakeLock mWakeLock = null;

	private TextView mTxtvMainInfo = null;
	private TextView mTxtvSubInfo = null;

	private TextView mAuthOnline = null;
	private ImageView mImgvGetDevInfo = null;
	private ImageView mImgvImportAuthCode = null;
	private ImageView mImgvImpOrUpdKey = null;

	private int mInfoDlgViewResId = -1;

	private ImageView mImgvSettings = null;
	private ImageView mImgvServerParam = null;

	private AuthCodeWindow mAuthCodeWindow = null;
	private WaitingDialog mWaitingDialog;
	private String mCpuID;
	private String mCode;
	private String mCompany;
	private Bitmap mBgBitmap = null;

	private NetWorkUtil mNetWork;
	private boolean mStopAutoAuth = false;

	private Dialog dlg = null;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PosterApplication.setSystemBarVisible(this, false);
		setContentView(R.layout.activity_authorization);

		INSTANCE = this;

		mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = mPowerManager.newWakeLock((PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), TAG);

		if (mWakeLock != null) {
			mWakeLock.acquire();
		}

		mTxtvMainInfo = (TextView) findViewById(R.id.txtv_auth_maininfo);
		mTxtvSubInfo = (TextView) findViewById(R.id.txtv_auth_subinfo);

		mAuthOnline = (TextView) findViewById(R.id.TVAuthOnline);
		mAuthOnline.setOnClickListener(this);
		mImgvGetDevInfo = (ImageView) findViewById(R.id.imgv_auth_getdevinfo);
		mImgvGetDevInfo.setOnClickListener(this);
		mImgvImportAuthCode = (ImageView) findViewById(R.id.imgv_auth_importauthcode);
		mImgvImportAuthCode.setOnClickListener(this);
		mImgvImpOrUpdKey = (ImageView) findViewById(R.id.imgv_auth_imtorupdkey);
		mImgvImpOrUpdKey.setOnClickListener(this);

		mImgvSettings = (ImageView) findViewById(R.id.imgv_auth_settings);
		mImgvSettings.setOnClickListener(this);
		mImgvServerParam = (ImageView) findViewById(R.id.imgv_auth_serverparam);
		mImgvServerParam.setOnClickListener(this);

		mNetWork = new NetWorkUtil(mNetWorkHandler);
		initView();

		AuthorizationManager.getInstance().setOnViewListener(new OnAuthStatusListener() {
			@Override
			public void onAuthSucceeded() {
				updateMsgOnAuthSucceeded(AuthorizationCommon.DEFAULT_DELAY_SECOND_SERVER);
			}

			@Override
			public void onKeyDownloaded() {
				updateMsgOnKeyDownloaded();
			}
		});

		startAutoAuthorize();
	}

	private void initView() {
		if (AuthorizationManager.getInstance().getStatus() != AuthorizationManager.STATUS_AUTHORIZED) {
			mTxtvMainInfo.setTextColor(Color.RED);
			mTxtvMainInfo.setText(getResString(R.string.unauthorized_msg));
		}
		mTxtvSubInfo.setText(getResString(R.string.importauth_info));
		if (TextUtils.isEmpty(DbHelper.getInstance().getAuthKey())) {
			mTxtvSubInfo.setText(getResString(R.string.keynotexist_info));
			mImgvImpOrUpdKey.setBackgroundResource(R.drawable.auth_importkey_sel);
		} else {
			mImgvImpOrUpdKey.setBackgroundResource(R.drawable.auth_updatekey_sel);
		}
	}

	private String getResString(int resid) {
		return getResources().getString(resid);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mWaitingDialog != null) {
			mWaitingDialog.dissmiss();
			mWaitingDialog = null;
		}
	}

	@SuppressLint("Wakelock")
	@Override
	protected void onDestroy() {
		if (mNetWork != null) {
			mNetWorkHandler.removeMessages(NetWorkUtil.CMD_REGISTER_CODE);
			mNetWork.close();
		}

		if (mAuthCodeWindow != null) {
			mAuthCodeWindow.dismiss();
			mAuthCodeWindow = null;
		}

		if (AuthorizationManager.getInstance() != null) {
			AuthorizationManager.getInstance().destroy();
		}

		mHandler.removeMessages(EVENT_UPDATE_MSG_AUTHSUCCEEDED);
		mHandler.removeMessages(EVENT_UPDATE_MSG_KEYDOWNLOADED);
		mHandler.removeMessages(EVENT_START_AUTO_AUTHORIZE);

		if (mBgBitmap != null && !mBgBitmap.isRecycled()) {
			mBgBitmap.recycle();
			mBgBitmap = null;
		}

		if (mWakeLock != null) {
			mWakeLock.release();
			mWakeLock = null;
		}

		INSTANCE = null;

		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onClick(View v) {
		mInfoDlgViewResId = v.getId();
		switch (mInfoDlgViewResId) {
		case R.id.imgv_auth_settings:
			switchToSettings();
			break;
		case R.id.imgv_auth_serverparam:
			setServerParam();
			break;
		case R.id.TVAuthOnline:
			mStopAutoAuth = true;
			authOnline();
			break;
		case R.id.imgv_auth_getdevinfo:
			getDevInfo();
			break;
		case R.id.imgv_auth_importauthcode:
			mStopAutoAuth = true;
			importAuthCode();
			break;
		case R.id.imgv_auth_imtorupdkey:
			mStopAutoAuth = true;
			importOrUpdateKey();
			break;
		}
	}

	private void switchToSettings() {
		PosterApplication.startApplication(this, Contants.SETTING_PACKAGENAME);
	}

	private void showServerParamDialog(final ConcurrentHashMap<String, String> serverParam) {
		String oldweburl = null;
		if (serverParam != null) {
			oldweburl = serverParam.get("weburl");
		}

		if (oldweburl != null && oldweburl.endsWith("asmx") && oldweburl.length() > WsClient.SERVICE_URL_SUFFIX.length()) {
			oldweburl = oldweburl.substring(0, (oldweburl.length() - WsClient.SERVICE_URL_SUFFIX.length()));
		}

		final View dlgview = LayoutInflater.from(this).inflate(R.layout.serverparam_dialog, null);
		final EditText etxtWebUrl = (EditText) dlgview.findViewById(R.id.etxt_auth_weburl);
		etxtWebUrl.setText(oldweburl != null ? oldweburl : "");

		final String saveweburl = etxtWebUrl.getText().toString();
		dlg = DialogUtil.showTipsDialog(this, getString(R.string.dlg_title_serverparam), dlgview, getString(R.string.enter), getString(R.string.cancel), new DialogDoubleButtonListener() {

			@Override
			public void onLeftClick(Context context, View v, int which) {
				String newweburl = etxtWebUrl.getText().toString();
				if (!newweburl.equals(saveweburl)) {
					if (newweburl != null && !"".equals(newweburl) && !newweburl.endsWith("asmx")) {
						newweburl = newweburl + WsClient.SERVICE_URL_SUFFIX;
					}
					serverParam.put("weburl", newweburl != null ? newweburl : "");

					SysParamManager.getInstance().setServerParam(serverParam);

					WsClient.getInstance().osdChangeServerConfig();
				}
				if (dlg != null) {
					DialogUtil.hideInputMethod(context, etxtWebUrl, dlg);
					dlg.dismiss();
					dlg = null;
				}
			}

			@Override
			public void onRightClick(Context context, View v, int which) {
				if (dlg != null) {
					DialogUtil.hideInputMethod(context, etxtWebUrl, dlg);
					dlg.dismiss();
					dlg = null;
				}
			}

		}, false);

		dlg.show();

		DialogUtil.dialogTimeOff(dlg, dlgview,90000);

	}

	private void setServerParam() {
		showServerParamDialog(SysParamManager.getInstance().getServerParam());
	}

	private void updateMsgOnAuthSucceeded(int sec) {
		Message msg = mHandler.obtainMessage();
		msg.what = EVENT_UPDATE_MSG_AUTHSUCCEEDED;
		msg.arg1 = sec;
		msg.sendToTarget();
	}

	private void updateMsgOnKeyDownloaded() {
		Message msg = mHandler.obtainMessage();
		msg.what = EVENT_UPDATE_MSG_KEYDOWNLOADED;
		msg.sendToTarget();
	}

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case EVENT_UPDATE_MSG_AUTHSUCCEEDED:
				mTxtvMainInfo.setTextColor(Color.GREEN);
				mTxtvMainInfo.setText(getResString(R.string.auth_success_msg));
				mTxtvSubInfo.setText(String.format(getResString(R.string.auth_success_submsg), msg.arg1));
				break;
			case EVENT_UPDATE_MSG_KEYDOWNLOADED:
				if (mBgBitmap == null) {
					mBgBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.auth_updatekey_sel);
				}
				mTxtvSubInfo.setText(getResString(R.string.importauth_info));
				mImgvImpOrUpdKey.setBackground(new BitmapDrawable(getResources(), mBgBitmap));
				break;
			case EVENT_START_AUTO_AUTHORIZE:
				startAutoAuthorize();
				break;
			}
			super.handleMessage(msg);
		}
	};

	private void showToast(String msg) {
		Toast tst = Toast.makeText(this, msg, Toast.LENGTH_LONG);
		tst.setGravity(Gravity.CENTER, 0, 0);
		tst.show();
	}

	private void showInfoDialog(String title, String msg, String postxt, String negtxt) {
		dlg = DialogUtil.showTipsDialog(INSTANCE, title, msg, postxt, negtxt, new DialogDoubleButtonListener() {

			@Override
			public void onLeftClick(Context context, View v, int which) {
				AuthorizationHelper helper = null;
				switch (mInfoDlgViewResId) {
				case R.id.imgv_auth_getdevinfo:
					helper = new AuthorizationHelper(INSTANCE, getResString(R.string.pdlg_exportdevinfo_title), getResString(R.string.pdlg_exportdevinfo_msg));
					helper.exportDevInfoToUDisk();
					break;
				case R.id.imgv_auth_importauthcode:
					helper = new AuthorizationHelper(INSTANCE, getResString(R.string.pdlg_importauthcode_title), getResString(R.string.pdlg_importauthcode_msg));
					helper.setOnStatusListener(new AuthorizationHelper.OnStatusListener() {
						@Override
						public void onCompleted() {
							if (AuthorizationManager.getInstance().checkAuthStatus(AuthorizationManager.MODE_DELAY_LOCAL)) {
								if (AuthorizationManager.getInstance() != null) {
									AuthorizationManager.getInstance().stopAuth();
								}
								mTxtvMainInfo.setTextColor(Color.GREEN);
								mTxtvMainInfo.setText(getResString(R.string.auth_success_msg));
								mTxtvSubInfo.setText(String.format(getResString(R.string.auth_success_submsg), AuthorizationCommon.DEFAULT_DELAY_SECOND_LOCAL));
							} else {
								mTxtvSubInfo.setText(getResString(R.string.auth_failure_submsg));
							}
						}
					});
					helper.importAuthCodeFromUDisk();
					break;
				case R.id.imgv_auth_imtorupdkey:
					if (TextUtils.isEmpty(DbHelper.getInstance().getAuthKey())) {
						helper = new AuthorizationHelper(INSTANCE, getResString(R.string.pdlg_importkey_title), getResString(R.string.pdlg_importkey_msg));
						helper.setOnStatusListener(new AuthorizationHelper.OnStatusListener() {
							@Override
							public void onCompleted() {
								mTxtvSubInfo.setText(getResString(R.string.importauth_info));
								mImgvImpOrUpdKey.setBackgroundResource(R.drawable.auth_updatekey_sel);
							}
						});
						helper.importKeyFromUDisk();
					} else {
						helper = new AuthorizationHelper(INSTANCE, getResString(R.string.pdlg_updatekey_title), getResString(R.string.pdlg_updatekey_msg));
						helper.updateKeyFromUDisk();
					}
					break;
				}
				if (dlg != null) {
					dlg.dismiss();
					dlg = null;
				}
			}

			@Override
			public void onRightClick(Context context, View v, int which) {
				if (dlg != null) {
					dlg.dismiss();
					dlg = null;
				}
			}
		}, false);

		dlg.show();

		DialogUtil.dialogTimeOff(dlg, 90000);

	}

	@SuppressWarnings("deprecation")
	private void authOnline() {
		View contentView = LayoutInflater.from(this).inflate(R.layout.pop_input_code, null);

		OnCodeListener listener = new OnCodeListener() {
			@Override
			public void onCode(String code, String company) {
				mCpuID = PosterApplication.getCpuId().toUpperCase();
				mCode = code;
				mCompany = company;

				mWaitingDialog = WaitingDialog.getInstance();
				mWaitingDialog.show(AuthorizationActivity.this);
				mNetWork.register(mCpuID, mCode, mCompany);
			}
		};

		mAuthCodeWindow = new AuthCodeWindow(contentView, listener);

		mAuthCodeWindow.setFocusable(true);
		mAuthCodeWindow.setBackgroundDrawable(new BitmapDrawable());

		// 重写onKeyListener
		contentView.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					if (mAuthCodeWindow != null) {
						mAuthCodeWindow.dismiss();
						mAuthCodeWindow = null;
					}
					return true;
				}
				return false;
			}
		});

		mAuthCodeWindow.showAtLocation(findViewById(R.id.root), Gravity.TOP, 0, 0);
	}

	private void getDevInfo() {
		String mac = AuthorizationCommon.getMac(PosterApplication.getEthMacAddress());
		String cpuid = PosterApplication.getCpuId();
		StringBuilder sb = new StringBuilder();
		sb.append(getResString(R.string.auth_mac_info)).append(mac.toUpperCase());
		sb.append("\n");
		sb.append(getResString(R.string.auth_cpuid_info)).append(cpuid.toUpperCase());
		showInfoDialog(getResString(R.string.dialog_devinfo_title), sb.toString(), getResString(R.string.dialog_devinfo_postxt), getResString(R.string.dialog_devinfo_negtxt));
	}

	private void importAuthCode() {
		if (TextUtils.isEmpty(DbHelper.getInstance().getAuthKey())) {
			showToast(getResString(R.string.importauth_warning));
		} else {
			showInfoDialog(getResString(R.string.dialog_authcode_title), null, getResString(R.string.dialog_authcode_postxt), getResString(R.string.dialog_authcode_negtxt));
		}
	}

	private void importOrUpdateKey() {
		String title = null;
		String postxt = null;
		if (TextUtils.isEmpty(DbHelper.getInstance().getAuthKey())) {
			title = getResString(R.string.dialog_importkey_title);
			postxt = getResString(R.string.dialog_importkey_postxt);
		} else {
			title = getResString(R.string.dialog_updatekey_title);
			postxt = getResString(R.string.dialog_updatekey_postxt);
		}
		showInfoDialog(title, null, postxt, getResString(R.string.dialog_key_negtxt));
	}

	private boolean checkAuthResult(JSONUtil jsonUtil) {
		String publicKey = jsonUtil.getString("public_key");
		String encryptData = jsonUtil.getString("encrypt_data");

		String authInfo = "";
		try {
			authInfo = new String(RSAUtils.decryptPkcs1ByPublicKey(Base64Utils.decode(encryptData), publicKey));
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (authInfo.equals(PosterApplication.getCpuId().toUpperCase())) {
			DbHelper helper = DbHelper.getInstance();
			helper.setAuthKey(publicKey);
			helper.setAuthCode(encryptData);

			return true;
		}

		return false;
	}

	private void startAutoAuthorize() {
		if (getAuthCodeFromFile()) {
			if (mCode != null && mCompany != null && !mStopAutoAuth) {
				// mWaitingDialog = WaitingDialog.getInstance();
				// mWaitingDialog.show(AuthorizationActivity.this);
				mCpuID = PosterApplication.getCpuId().toUpperCase();
				mNetWork.register(mCpuID, mCode, mCompany);
			}
		}
	}

	private boolean getAuthCodeFromFile() {
		String path = FileUtils.findFilePath(AuthorizationCommon.GROUP_AUTHCODE_FILE);
		if (path != null) {
			File file = new File(path);
			if (!file.exists()) {
				return false;
			}

			try {
				InputStream instream = new FileInputStream(file);
				if (instream != null) {
					InputStreamReader inputreader = new InputStreamReader(instream);
					BufferedReader buffreader = new BufferedReader(inputreader);
					String line;
					// 分行读取
					while ((line = buffreader.readLine()) != null) {
						int index = line.indexOf("code:");
						if (index >= 0) {
							mCode = line.substring(index + "code:".length());
							continue;
						}

						index = line.indexOf("group:");
						if (index >= 0) {
							mCompany = line.substring(index + "group:".length());
							continue;
						}
					}
					instream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}

		return false;
	}

	private Handler mNetWorkHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if (mWaitingDialog != null) {
				mWaitingDialog.dissmiss();
				mWaitingDialog = null;
			}

			if (msg.arg1 != NetWorkUtil.NETWORK_OK) {
				if (mStopAutoAuth) {
					Toast.makeText(AuthorizationActivity.this, AuthorizationActivity.this.getResources().getString(R.string.toast_network_failed), Toast.LENGTH_LONG).show();
				}

				mHandler.sendMessageDelayed(mHandler.obtainMessage(EVENT_START_AUTO_AUTHORIZE), 3000);
				return;
			}

			switch (msg.what) {
			case NetWorkUtil.CMD_REGISTER_CODE:
				HttpResultHandler httpHandler = new HttpResultHandler(AuthorizationActivity.this);
				int errorCode = httpHandler.getErrorCode((String) msg.obj);
				if (errorCode >= NetWorkUtil.RESULT_OK) {
					JSONUtil jsonUtil = httpHandler.getJSONUtilObjec((String) msg.obj);

					if (checkAuthResult(jsonUtil)) {
						if (AuthorizationManager.getInstance().checkAuthStatus(AuthorizationManager.MODE_IMMEDIATE)) {
							AuthorizationManager.getInstance().stopAuth();

							mTxtvMainInfo.setTextColor(Color.GREEN);
							mTxtvMainInfo.setText(getResString(R.string.auth_success_msg));
							mTxtvSubInfo.setText(String.format(getResString(R.string.auth_success_submsg), AuthorizationCommon.DEFAULT_DELAY_SECOND_LOCAL));
						}
					}
				}
				break;
			}
		}
	};
}
