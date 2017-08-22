/*
 * Copyright (C) 2013 poster PCE YoungSee Inc. 
 * All Rights Reserved Proprietary and Confidential.
 * 
 * @author LiLiang-Ping
 */

package com.youngsee.posterdisplayer;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.youngsee.authorization.AuthorizationManager;
import com.youngsee.common.Actions;
import com.youngsee.common.Contants;
import com.youngsee.common.DbHelper;
import com.youngsee.common.DialogUtil;
import com.youngsee.common.DialogUtil.DialogDoubleButtonListener;
import com.youngsee.common.FileUtils;
import com.youngsee.common.MediaInfoRef;
import com.youngsee.common.PackageInstaller;
import com.youngsee.common.SubWindowInfoRef;
import com.youngsee.common.SysParamManager;
import com.youngsee.customview.AudioView;
import com.youngsee.customview.DateTimeView;
import com.youngsee.customview.GalleryView;
import com.youngsee.customview.MarqueeView;
import com.youngsee.customview.MultiMediaView;
import com.youngsee.customview.PosterBaseView;
import com.youngsee.customview.TimerView;
import com.youngsee.customview.YSHorizontalScrollView;
import com.youngsee.customview.YSWebView;
import com.youngsee.logmanager.LogManager;
import com.youngsee.logmanager.LogUtils;
import com.youngsee.logmanager.Logger;
import com.youngsee.multicast.MulticastCommon;
import com.youngsee.multicast.MulticastManager;
import com.youngsee.multicast.MulticastSyncInfoRef;
import com.youngsee.osd.UDiskUpdata;
import com.youngsee.posterdisplayer.ApplicationSelector.AppInfo;
import com.youngsee.posterdisplayer.ApplicationSelector.ItemSelectListener;
import com.youngsee.posterdisplayer.R;
import com.youngsee.power.PowerOnOffManager;
import com.youngsee.screenmanager.ScreenManager;
import com.youngsee.socket.MusicServiceHelper;
import com.youngsee.update.APKUpdateManager;
import com.youngsee.webservices.SocketServer;
import com.youngsee.webservices.SocketServerMinor;
import com.youngsee.webservices.SocketServerMinor1;
import com.youngsee.webservices.SocketServerMinor2;
import com.youngsee.webservices.WsClient;

@SuppressLint({ "Wakelock", "InflateParams" })
public class PosterMainActivity extends Activity {
	public static PosterMainActivity INSTANCE = null;
	private WakeLock mWklk = null;
	private FrameLayout mMainLayout = null;
	private PopupWindow mOsdPupupWindow = null; // OSD 弹出菜单
	private PopupWindow TongRengNotify = null;

	private Intent popService = null;
	private boolean isPopServiceRunning = false;

	private Dialog mUpdateApkDialog = null;
	private Dialog mUpdateProgramDialog = null;

	private BroadcastReceiver mReceiver = null;

	private MediaInfoRef mBgImgInfo = null;

	private Set<PosterBaseView> mSubWndCollection = null; // 屏幕布局信息

	private ApplicationSelector mSelector = null;
	private List<AppInfo> mAppInfo = null;
	private YSHorizontalScrollView mScrollView = null;

	private static final int EVENT_CHECK_SET_ONOFFTIME = 0;

	private static int MAX_CLICK_CNTS = 5;
	private long mLastClickTime = 0;
	private static int mCurrentClickCnts = 0;

	private long mExitTime = 0;

	private Dialog dlg = null;
	private View mQuitView = null;
	private String EXIT_DEFAULT_PWD = null;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PosterApplication.setSystemBarVisible(this, false);
		setContentView(R.layout.activity_main);
		// getWindow().setFormat(PixelFormat.TRANSLUCENT);

		Logger.d("====>PosterMainActivity onCreate: " + getIntent().toString());

		INSTANCE = this;

	SharedPreferences sharedPreferences = getSharedPreferences("ys_poster_displayer", Activity.MODE_PRIVATE);
		// 初始安装APK时，需安装YSSysCtroller.apk
		if (PosterApplication.getInstance().getConfiguration().isInstallYsctrl()) {
			int versionCode = PosterApplication.getInstance().getVerCode();
			int installed = sharedPreferences.getInt("monitorInstalled", 0);
			int installedVersion = sharedPreferences.getInt("versionCode", 0);
			if (installed == 0 || versionCode != installedVersion) {
				// install system ctrl APK
				PackageInstaller install = new PackageInstaller();
				String controller = install.retrieveSourceFromAssets("YSSysController.apk");
				if (!TextUtils.isEmpty(controller) && install.installSystemPkg(controller, "YSSysController.apk")) {
					SharedPreferences.Editor editor = sharedPreferences.edit();
					editor.putInt("monitorInstalled", 1);
					editor.putInt("versionCode", versionCode);
					editor.apply();

					// start the APK
				}
			}
			startService(new Intent(Actions.SYSCTRL_SERVICE_ACTION));
		}

		// 初始化背景颜色
		mMainLayout = ((FrameLayout) findViewById(R.id.root));
		mMainLayout.setBackgroundColor(Color.BLACK);

		if (mWklk == null) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			mWklk = pm.newWakeLock((PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "PosterMain");
		}

		// 唤醒屏幕
		if (mWklk != null) {
			mWklk.acquire();
		}

		// 初始化系统参数
		PosterApplication.getInstance().initAppParam();

		// 检测是否鉴权
		if (!AuthorizationManager.getInstance().checkAuthStatus(AuthorizationManager.MODE_IMMEDIATE)) {
			AuthorizationManager.getInstance().startAuth();
		}

		// 同步播放节目检测
		if (DbHelper.getInstance().getPgmSyncFlagFromDB() != MulticastCommon.MC_VALUE_PROGSYNC_CLOSE) {
			MulticastManager.getInstance().startWork();
		}

		// 启动屏幕管理线程
		if (ScreenManager.getInstance() == null) {
			ScreenManager.createInstance(this).startRun();
		}

		// Socket数据管理线程
/*        if (SocketServer.getInstance() == null) {
            SocketServer.createInstance(this).startRun();
        }*/

/*		if (SocketServerMinor1.getInstance() == null){
			SocketServerMinor1.createInstance(this).startRun();
		}*/

		if (SocketServerMinor2.getInstance() == null){
			SocketServerMinor2.createInstance(this).startRun();
		}

		// 启动网络管理线程
		if (WsClient.getInstance() == null) {
			WsClient.createInstance(this).startRun();
		}

		// 启动日志输出线程
		if (LogUtils.getInstance() == null) {
			LogUtils.createInstance(this).startRun();
		}

		// 初始化OSD菜单
		initOSD();

		// 定义OSD菜单弹出方式
		mMainLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				long clickTime = SystemClock.uptimeMillis();
				long dtime = clickTime - mLastClickTime;
				if (mLastClickTime == 0 || dtime < 3000) {
					mCurrentClickCnts++;
					mLastClickTime = clickTime;
				} else {
					mLastClickTime = 0;
					mCurrentClickCnts = 0;
				}

				// When click times is more than 5, then popup the tools bar
				if (mCurrentClickCnts > MAX_CLICK_CNTS) {
					showOsd();
					mLastClickTime = 0;
					mCurrentClickCnts = 0;
				}
			}
		});

		// 启动定时器，定时清理文件和上传日志
		PosterApplication.getInstance().startTimingDel();
		PosterApplication.getInstance().startTimingUploadLog();

		// 检测定时开关机状态
		PowerOnOffManager.getInstance().checkAndSetOnOffTime(PowerOnOffManager.AUTOSCREENOFF_COMMON);

		// 检测是否需要升级新版本
		if (SysParamManager.getInstance().getAutoUpgrade() == 1) {
			APKUpdateManager.getInstance().startAutoDetector();
		}

		// 启动环境监控板功能
		if (PosterApplication.getInstance().getConfiguration().hasEnvironmentMonitor()) {
			int install_lib=sharedPreferences.getInt("libserial_port", 0);
			PackageInstaller install = new PackageInstaller();
			if(install_lib==0){
				String lib_name="libserial_port.so";
				String lib_serialPort = install.retrieveSourceFromAssets(lib_name);
				if(!TextUtils.isEmpty(lib_serialPort)){
				if(install.installSysLib(lib_serialPort, lib_name)){
				SharedPreferences.Editor editor=sharedPreferences.edit();
				editor.putInt("libserial_port", 1);
				editor.apply();
				}
				}
			}
			
			
			// install the APK
 			if (!apkIsExist(Contants.ENVMANAGER_PACKAGENAME)&&sharedPreferences.getInt("libserial_port", 0)==1) {
				
				String envService = install.retrieveSourceFromAssets("EnvmntService.apk");
				if(!TextUtils.isEmpty(envService))
				install.installSystemPkg(envService, "EnvmntService.apk");
				
			}

			// start the APK
			startService(new Intent(Actions.ENVMANAGER_SERVICE_ACTION));

			// Update EnvManger param
			Intent intent = new Intent(Actions.UPDATA_ENVMONITOR_DEVID_ACTION);
			intent.putExtra("CpuId", PosterApplication.getCpuId().toUpperCase(Locale.getDefault()));
			intent.putExtra("Mac", PosterApplication.getEthMacStr());
			intent.putExtra("term", SysParamManager.getInstance().getTerm());
			intent.putExtra("termGroup", SysParamManager.getInstance().getTermGrp());
			PosterApplication.getInstance().sendStickyBroadcast(intent);
		}
		// 监测U盘是否插入
		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				String path = intent.getData().getPath();
				if ((action.equals(Intent.ACTION_MEDIA_MOUNTED) || 
					 action.equals(Intent.ACTION_MEDIA_REMOVED) || 
					 action.equals(Intent.ACTION_MEDIA_BAD_REMOVAL))  &&
					 FileUtils.isUsbPath(path))
				{
					if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) 
					{
						String strApkPath = FileUtils.findApkInUdisk();
						if (!TextUtils.isEmpty(strApkPath))
						{
							showUpdateApkDialog();
						}
						
						if (PosterApplication.existsPgmInUdisk(path))
						{
						    showUpdateProgramDialog();
						}
					} 
					else if (action.equals(Intent.ACTION_MEDIA_REMOVED) || action.equals(Intent.ACTION_MEDIA_BAD_REMOVAL))
					{
						dismissUpdateApkDialog();
						dismissUpdateProgramDialog();
					}
				}
			}
		};
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_REMOVED);
		filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
		filter.addDataScheme("file");
		registerReceiver(mReceiver, filter);


	}

	public void showOsd() {
		if (mOsdPupupWindow != null) {
			if (mOsdPupupWindow.isShowing()) {
				mOsdPupupWindow.dismiss();
			} else {
				mOsdPupupWindow.showAtLocation(mMainLayout, Gravity.TOP | Gravity.START, 0, 0);
				mHandler.postDelayed(rHideOsdPopWndDelay, 30000);
			}
		}
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	protected void onResume() {
		// 初始化停靠栏
		initDockBar();

		if (mSubWndCollection != null) {
			for (PosterBaseView wnd : mSubWndCollection) {
				wnd.onViewResume();
			}
		}

		if (TextUtils.isEmpty(ScreenManager.getInstance().getPlayingPgmId())) {
			LogUtils.getInstance().toAddPLog(0, Contants.PlayProgramStart, ScreenManager.getInstance().getPlayingPgmId(), "", "");
		}

		if (PowerOnOffManager.getInstance().getCurrentStatus() == PowerOnOffManager.STATUS_STANDBY) {
			PowerOnOffManager.getInstance().setCurrentStatus(PowerOnOffManager.STATUS_ONLINE);
			PowerOnOffManager.getInstance().checkAndSetOnOffTime(PowerOnOffManager.AUTOSCREENOFF_URGENT);
		}

		super.onResume();
	}

	@Override
	protected void onPause() {
		mHandler.removeCallbacks(rSetWndBgDelay);
		mHandler.removeCallbacks(rHideOsdPopWndDelay);

		if (mSubWndCollection != null) {
			for (PosterBaseView wnd : mSubWndCollection) {
				wnd.onViewPause();
			}
		}

		if (!TextUtils.isEmpty(ScreenManager.getInstance().getPlayingPgmId())) {
			LogUtils.getInstance().toAddPLog(0, Contants.PlayProgramEnd, ScreenManager.getInstance().getPlayingPgmId(), "", "");
		}

		if (mOsdPupupWindow.isShowing()) {
			mOsdPupupWindow.dismiss();
		}

		super.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		mHandler.removeCallbacks(rSetWndBgDelay);
		mHandler.removeCallbacks(rHideOsdPopWndDelay);
		mHandler.removeMessages(EVENT_CHECK_SET_ONOFFTIME);
		if (mReceiver != null)
		{
			unregisterReceiver(mReceiver);
		}

		cleanupLayout();

		if (mOsdPupupWindow.isShowing()) {
			mOsdPupupWindow.dismiss();
		}

		synchronized (this) {
			if (isPopServiceRunning) {
				stopService(popService);
				isPopServiceRunning = false;
			}
		}

		MulticastManager.getInstance().stopWork();

		// 结束屏幕管理线程
		if (ScreenManager.getInstance() != null) {
			ScreenManager.getInstance().stopRun();
		}

		// 结束网络管理线程
		if (WsClient.getInstance() != null) {
			WsClient.getInstance().stopRun();
		}

		// 结束日志输出线程
		if (LogUtils.getInstance() != null) {
			LogUtils.getInstance().stopRun();
		}

		// 结束APK更新
		if (APKUpdateManager.getInstance() != null) {
			APKUpdateManager.getInstance().destroy();
		}

		if (PowerOnOffManager.getInstance() != null) {
			PowerOnOffManager.getInstance().destroy();
		}

		if (AuthorizationManager.getInstance() != null) {
			AuthorizationManager.getInstance().destroy();
		}

		if (LogManager.getInstance() != null) {
			LogManager.getInstance().destroy();
		}

		// 终止定时器
		PosterApplication.getInstance().cancelTimingDel();
		PosterApplication.getInstance().cancelTimingUploadLog();

		dismissUpdateApkDialog();
		dismissUpdateProgramDialog();

		// 恢复屏幕
		if (mWklk != null) {
			mWklk.release();
			mWklk = null;
		}

		INSTANCE = null;
		super.onDestroy();
		System.exit(0);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if ((System.currentTimeMillis() - mExitTime) > 2000) {
				mExitTime = System.currentTimeMillis();
			} else {

				if (PosterApplication.getInstance().getConfiguration().showQuitDialog()) {
					EXIT_DEFAULT_PWD = SysParamManager.getInstance().getSysPasswd();
					mQuitView = LayoutInflater.from(this).inflate(R.layout.osd_quit_pwd, null);
					dlg = DialogUtil.showTipsDialog(PosterMainActivity.this, getString(R.string.dialog_quit), mQuitView, getString(R.string.enter), getString(R.string.cancel), new DialogDoubleButtonListener() {

						@Override
						public void onLeftClick(Context context , View v , int which) {
							if (((EditText) mQuitView.findViewById(R.id.quit_password)).getText().toString().equals(EXIT_DEFAULT_PWD)) {
								Intent mHomeIntent = new Intent(Intent.ACTION_MAIN);
								mHomeIntent.addCategory(Intent.CATEGORY_HOME);
								mHomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
								startActivity(mHomeIntent);
							} else {
								Toast.makeText(PosterMainActivity.this, R.string.login_dialog_msgerror, Toast.LENGTH_SHORT).show();
							}
							if (dlg !=null) {
								DialogUtil.hideInputMethod(context, v, dlg);
								dlg.dismiss();
								dlg = null;
							}
						}

						@Override
						public void onRightClick(Context context , View v , int which) {
							if (dlg !=null) {
								DialogUtil.hideInputMethod(PosterMainActivity.this, mQuitView, dlg);
								dlg.dismiss();
								dlg = null;
							}
						}

					}, false);

					dlg.show();
					DialogUtil.dialogTimeOff(dlg, mQuitView,90000);

				} else {
					Intent mHomeIntent = new Intent(Intent.ACTION_MAIN);
					mHomeIntent.addCategory(Intent.CATEGORY_HOME);
					mHomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
					startActivity(mHomeIntent);
				}

			}

			return true; // 不响应Back键
		case KeyEvent.KEYCODE_MENU:
			enterToOSD(PosterOsdActivity.OSD_MAIN_ID);
			return true; // 打开OSD主菜单

		case KeyEvent.KEYCODE_PAGE_UP:
			return true; // 主窗口中上一个素材

		case KeyEvent.KEYCODE_PAGE_DOWN:
			return true; // 主窗口中下一个素材

		case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
			return true; // 主窗口视频播放

		case KeyEvent.KEYCODE_MEDIA_STOP:
			return true; // 主窗口视频暂停
		}

		return super.onKeyDown(keyCode, event);
	}

	public void initTongRenNotifyPw(String title, String content){
		final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View NotifyView = inflater.inflate(R.layout.pop_tongren,null);

		TongRengNotify  = new PopupWindow(1500, 750);
		TongRengNotify.setFocusable(false);
		TongRengNotify.setContentView(NotifyView);
		TongRengNotify.setOutsideTouchable(false);

		TextView tv_pop_title = (TextView) NotifyView.findViewById(R.id.tv_pop_title);
		TextView tv_pop_content = (TextView) NotifyView.findViewById(R.id.tv_pop_content);

		tv_pop_title.setText(title);
		tv_pop_content.setText(content);

		TongRengNotify.showAtLocation(mMainLayout,Gravity.CENTER,0,0);
		mHandler.postDelayed(rHideTongRenPopWndDelay, 1000*10);

	}

	private boolean apkIsExist(String packageName) {
		if (!TextUtils.isEmpty(packageName)) {
			try {
				ApplicationInfo info = getPackageManager().getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
				return (info != null);
			} catch (NameNotFoundException e) {
				return false;
			}
		}

		return false;
	}

	private void showUpdateProgramDialog() {
		if ((mUpdateProgramDialog != null) && mUpdateProgramDialog.isShowing()) {
			mUpdateProgramDialog.dismiss();
			mUpdateProgramDialog = null;
		}
		
        mUpdateProgramDialog = DialogUtil.showTipsDialog(this, getString(R.string.udisk_update_pgm), getString(R.string.udisk_content), getString(R.string.udisk_title), getString(R.string.udisk_btn_cancel), new DialogDoubleButtonListener() {			
			@Override
			public void onRightClick(Context context , View v , int which) {
				if (mUpdateProgramDialog != null) {
					mUpdateProgramDialog.dismiss();
					mUpdateProgramDialog = null;
				}
			}
			
			@Override
			public void onLeftClick(Context context , View v , int which) {
				UDiskUpdata diskUpdate = new UDiskUpdata(PosterMainActivity.this);
				diskUpdate.updateProgram();
				
				if (mUpdateProgramDialog != null) {
					mUpdateProgramDialog.dismiss();
					mUpdateProgramDialog = null;
				}
			}
		}, true);
		
		mUpdateProgramDialog.show();
		
		DialogUtil.dialogTimeOff(mUpdateProgramDialog, 90000);
	}

	private void dismissUpdateProgramDialog() {
		if ((mUpdateProgramDialog != null) && mUpdateProgramDialog.isShowing()) {
			mUpdateProgramDialog.dismiss();
			mUpdateProgramDialog = null;
		}
	}

	private void showUpdateApkDialog() {
		if ((mUpdateApkDialog != null) && mUpdateApkDialog.isShowing()) {
			mUpdateApkDialog.dismiss();
			mUpdateApkDialog = null;
		}
		
		mUpdateApkDialog = DialogUtil.showTipsDialog(this, getString(R.string.udisk_update_apk), getString(R.string.udisk_content_apk), getString(R.string.udisk_title), getString(R.string.udisk_btn_cancel), new DialogDoubleButtonListener() {

			@Override
			public void onLeftClick(Context context, View v, int which) {
				UDiskUpdata diskUpdate = new UDiskUpdata(PosterMainActivity.this);
				diskUpdate.updateSW();
				
				if (mUpdateApkDialog != null) {
					mUpdateApkDialog.dismiss();
					mUpdateApkDialog = null;
				}
			}

			@Override
			public void onRightClick(Context context, View v, int which) {
				if (mUpdateApkDialog != null) {
					mUpdateApkDialog.dismiss();
					mUpdateApkDialog = null;
				}
			}			
			
		}, true);
		
		mUpdateApkDialog.show();
		DialogUtil.dialogTimeOff(mUpdateApkDialog, 90000);
	}

	private void dismissUpdateApkDialog() {
		if ((mUpdateApkDialog != null) && mUpdateApkDialog.isShowing()) {
			mUpdateApkDialog.dismiss();
			mUpdateApkDialog = null;
		}
	}
	
	@SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case EVENT_CHECK_SET_ONOFFTIME:
				PowerOnOffManager.getInstance().checkAndSetOnOffTime((msg.getData().getInt("type")));
				break;
			}
			super.handleMessage(msg);
		}
	};

	public void checkAndSetOnOffTime(int type) {
		Bundle bundle = new Bundle();
		bundle.putInt("type", type);
		Message msg = mHandler.obtainMessage();
		msg.what = EVENT_CHECK_SET_ONOFFTIME;
		msg.setData(bundle);
		msg.sendToTarget();
	}

	private void cleanupLayout() {
		// 移除所有窗口
		if (mSubWndCollection != null) {
			for (PosterBaseView wnd : mSubWndCollection) {
				wnd.onViewDestroy();
			}
			mMainLayout.removeAllViews();
			mSubWndCollection.clear();
			mSubWndCollection = null;
		}

		// 清除背景图片
		if (mBgImgInfo != null) {
			mBgImgInfo = null;
			mMainLayout.setBackground(null);
		}

		// 清空上一个节目的缓存
		PosterApplication.clearMemoryCache();
	}

	// 加载新节目
	public void loadNewProgram(ArrayList<SubWindowInfoRef> subWndList) {
		// Create new program windows
		if (subWndList != null) {
			Logger.i("Window number is: " + subWndList.size());

			// Clean old program
			cleanupLayout();

			// initialize
			int xPos = 0;
			int yPos = 0;
			int width = 0;
			int height = 0;
			String wndName = null;
			String wndType = null;
			List<MediaInfoRef> mediaList = null;

			PosterBaseView tempSubWnd = null;
			mSubWndCollection = new HashSet<PosterBaseView>();

			// Through the sub window list, and create the correct view for it.
			for (SubWindowInfoRef subWndInfo : subWndList) {
				tempSubWnd = null;

				// 窗体类型和名称
				if ((wndType = subWndInfo.getSubWindowType()) == null) {
					continue;
				}
				wndName = subWndInfo.getSubWindowName();

				// 窗体位置
				xPos = subWndInfo.getXPos();
				yPos = subWndInfo.getYPos();
				width = subWndInfo.getWidth();
				height = subWndInfo.getHeight();

				// 素材
				mediaList = subWndInfo.getSubWndMediaList();

				// 创建窗口
				if (wndType.contains("Main") || wndType.contains("StandbyScreen")) {
					tempSubWnd = new MultiMediaView(this, true);
				} else if (wndType.contains("Background")) {
					// 背景图片
					if (mediaList != null && mediaList.size() > 0 && "File".equals(mediaList.get(0).source)) {
						mBgImgInfo = mediaList.get(0);
						setWindowBackgroud();
					}
					continue;
				} else if (wndType.contains("Image") || wndType.contains("Weather")) {
					tempSubWnd = new MultiMediaView(this);
				} else if (wndType.contains("Audio")) {
					tempSubWnd = new AudioView(this);
				} else if (wndType.contains("Scroll")) {
					tempSubWnd = new MarqueeView(this);
				} else if (wndType.contains("Clock")) {
					tempSubWnd = new DateTimeView(this);
				} else if (wndType.contains("Gallery")) {
					tempSubWnd = new GalleryView(this);
				} else if (wndType.contains("Web")) {
					tempSubWnd = new YSWebView(this);
				} else if (wndType.contains("Timer")) {
					tempSubWnd = new TimerView(this);
				}

				// 设置窗口参数，并添加
				if (tempSubWnd != null) {
					tempSubWnd.setViewName(wndName);
					tempSubWnd.setViewType(wndType);
					tempSubWnd.setMediaList(mediaList);
					tempSubWnd.setViewPosition(xPos, yPos);
					tempSubWnd.setViewSize(width, height);
					mMainLayout.addView(tempSubWnd);
					mSubWndCollection.add(tempSubWnd);
				}
			}
		}

		if (mSubWndCollection != null) {
			for (PosterBaseView subWnd : mSubWndCollection) {
				subWnd.startWork();
			}
		}
	}

	/**
	 * Set the background picture of the window.
	 */
	private boolean setWindowBackgroud() {
		mHandler.removeCallbacks(rSetWndBgDelay);

		if (mMainLayout == null || mBgImgInfo == null) {
			Logger.i("Main layout didn't ready, can't load background image.");
			mHandler.postDelayed(rSetWndBgDelay, 500);
			return false;
		} else if (!FileUtils.isExist(mBgImgInfo.filePath)) {
			Logger.i("Background Image [" + mBgImgInfo.filePath + "] didn't exist.");
			PosterBaseView.downloadMedia(mBgImgInfo);
			mHandler.postDelayed(rSetWndBgDelay, 500);
			return false;
		} else if (!PosterBaseView.md5IsCorrect(mBgImgInfo)) {
			Logger.i("Background Image [" + mBgImgInfo.filePath + "] verifycode is wrong.");
			PosterBaseView.downloadMedia(mBgImgInfo);
			mHandler.postDelayed(rSetWndBgDelay, 500);
			return false;
		}

		// 读取图片
		Bitmap mBgBmp = loadBgPicture(mBgImgInfo);

		// 图片生成失败
		if (mBgBmp == null) {
			mHandler.postDelayed(rSetWndBgDelay, 500);
			return false;
		} else {
			// 设置背景
			mMainLayout.setBackground(new BitmapDrawable(getResources(), mBgBmp));
		}

		return true;
	}

	private Bitmap loadBgPicture(final MediaInfoRef picInfo) {
		Bitmap srcBmp = null;

		try {
			if (picInfo == null || FileUtils.mediaIsPicFromNet(picInfo)) {
				Logger.e("picture is come from network");
				return null;
			}

			// Create the Stream
			InputStream isImgBuff = PosterBaseView.createImgInputStream(picInfo);

			try {
				if (isImgBuff != null) {
					// Create the bitmap for BitmapFactory
					srcBmp = BitmapFactory.decodeStream(isImgBuff, null, PosterBaseView.setBitmapOption(picInfo));
				}
			} catch (java.lang.OutOfMemoryError e) {
				Logger.e("picture is too big, out of memory!");

				if (srcBmp != null && !srcBmp.isRecycled()) {
					srcBmp.recycle();
					srcBmp = null;
				}

				System.gc();
			} finally {
				if (isImgBuff != null) {
					isImgBuff.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return srcBmp;
	}

	private Runnable rHideOsdPopWndDelay = new Runnable() {
		@Override
		public void run() {
			mHandler.removeCallbacks(rHideOsdPopWndDelay);
			if (mOsdPupupWindow != null && mOsdPupupWindow.isShowing()) {
				mOsdPupupWindow.dismiss();
			}
		}
	};

	private Runnable rHideTongRenPopWndDelay = new Runnable() {
		@Override
		public void run() {
			mHandler.removeCallbacks(rHideTongRenPopWndDelay);
			if(TongRengNotify != null && TongRengNotify.isShowing()){
				TongRengNotify.dismiss();
				SocketServer.getInstance().isLoadingNotifyProgram = false;
				PosterApplication.setSystemBarVisible(INSTANCE, false);
			}
		}
	};

	/**
	 * 如果背景图片不存在，则轮循检测图片文件是否下载完成.
	 */
	private Runnable rSetWndBgDelay = new Runnable() {
		@Override
		public void run() {
			setWindowBackgroud();
		}
	};

	public void setPopServiceRunning(boolean isRunning) {
		synchronized (this) {
			isPopServiceRunning = isRunning;
		}
	}

	public void startPopSub(String text, int playSpeed, int duration, int number, String fontName, int fontColor) {
		synchronized (this) {
			if (isPopServiceRunning == true) {
				stopService(popService);
			}

			popService = new Intent(this, PopSubService.class);
			popService.putExtra(PopSubService.DURATION, duration);
			popService.putExtra(PopSubService.NUMBER, number);
			popService.putExtra(PopSubService.TEXT, text);
			popService.putExtra(PopSubService.FONTCOLOR, fontColor);
			popService.putExtra(PopSubService.FONTNAME, (fontName != null) ? fontName.substring(fontName.lastIndexOf(File.separator) + 1, fontName.lastIndexOf(".")) : null);
			popService.putExtra(PopSubService.SPEED, playSpeed);
			Logger.i("Start popService");
			startService(popService);
			isPopServiceRunning = true;
		}
	}

	public void startAudio() {
		if (mSubWndCollection != null) {
			for (PosterBaseView wnd : mSubWndCollection) {
				if (wnd.getViewName().startsWith("Audio")) {
					wnd.onViewResume();
				}
			}
		}
	}

	public void stopAudio() {
		if (mSubWndCollection != null) {
			for (PosterBaseView wnd : mSubWndCollection) {
				if (wnd.getViewName().startsWith("Audio")) {
					wnd.onViewPause();
				}
			}
		}
	}

	public Bitmap combineScreenCap(Bitmap bitmap) {

		MultiMediaView mainWnd = null;
		if (mSubWndCollection != null) {
			for (PosterBaseView wnd : mSubWndCollection) {
				if (wnd.getViewType().contains("Main") && (wnd instanceof MultiMediaView)) {
					mainWnd = (MultiMediaView) wnd;
				}
			}
		}

		if (mainWnd != null && mainWnd.needCombineCap()) {
			Bitmap videoCap = ((MultiMediaView) mainWnd).getVideoCap();
			if (videoCap != null) {
				Bitmap newb = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
				Canvas cv = new Canvas(newb);
				cv.drawBitmap(bitmap, 0, 0, null);
				Paint paint = new Paint();
				paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
				cv.drawBitmap(videoCap, mainWnd.getXPos(), mainWnd.getYPos(), paint);
				cv.save(Canvas.ALL_SAVE_FLAG);
				cv.restore();
				return newb;
			}
		}

		return bitmap;
	}

	public void syncPlay(MulticastSyncInfoRef syncInfo) {
		if (mSubWndCollection != null) 
		{
			for (PosterBaseView wnd : mSubWndCollection) 
			{
				if (wnd.getViewName().equals(syncInfo.WndName)) 
				{
					wnd.recvSyncPlayInfo(syncInfo);
					break;
				}
			}
		}
	}

	// 初始化OSD弹出菜单
	private void initOSD() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View osdView = inflater.inflate(R.layout.osd_pop_menu_view, null);
		osdView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showOsd();
			}
		});

		mOsdPupupWindow = new PopupWindow(osdView, 100, LinearLayout.LayoutParams.MATCH_PARENT, true);
		mOsdPupupWindow.setAnimationStyle(R.style.osdAnimation);
		mOsdPupupWindow.setOutsideTouchable(false);
		mOsdPupupWindow.setFocusable(true);

		// 初始化点击动作
		((ImageView) osdView.findViewById(R.id.osd_mainmenu)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				enterToOSD(PosterOsdActivity.OSD_MAIN_ID);
			}
		});

		((ImageView) osdView.findViewById(R.id.osd_server)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				enterToOSD(PosterOsdActivity.OSD_SERVER_ID);
			}
		});

		((ImageView) osdView.findViewById(R.id.osd_clock)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				enterToOSD(PosterOsdActivity.OSD_CLOCK_ID);
			}
		});

		((ImageView) osdView.findViewById(R.id.osd_system)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				enterToOSD(PosterOsdActivity.OSD_SYSTEM_ID);
			}
		});

		((ImageView) osdView.findViewById(R.id.osd_filemanage)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				enterToOSD(PosterOsdActivity.OSD_FILEMANAGER_ID);
			}
		});

		((ImageView) osdView.findViewById(R.id.osd_tools)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				enterToOSD(PosterOsdActivity.OSD_TOOL_ID);
			}
		});

		((ImageView) osdView.findViewById(R.id.osd_about)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				enterToOSD(PosterOsdActivity.OSD_ABOUT_ID);
			}
		});
	}

	private void enterToOSD(int menuId) {
		// PosterApplication.getInstance().initLanguage();
		Intent intent = new Intent(this, PosterOsdActivity.class);
		intent.putExtra("menuId", menuId);
		startActivity(intent);

		if (mOsdPupupWindow.isShowing()) {
			mOsdPupupWindow.dismiss();
		}
	}

	private void initDockBar() {
		if (PosterApplication.getInstance().getConfiguration().hasDockBar()) {
			findViewById(R.id.LLDockBar).setVisibility(View.VISIBLE);
			mAppInfo = new ArrayList<AppInfo>();
			Button button = (Button) this.findViewById(R.id.BSelectApp);
			button.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					showAppSelector();
				}
			});
			queryInitAppInfo(mAppInfo);
			saveAppInfo();
			showAppInfo();
		} else {
			findViewById(R.id.LLDockBar).setVisibility(View.GONE);
		}
	}

	private boolean showAppSelector() {
		if (mSelector != null && mSelector.isShowing()) {
			mSelector.dismiss();
			mSelector = null;
		}

		View contentView = LayoutInflater.from(this).inflate(R.layout.application_list, null);
		mSelector = new ApplicationSelector(this, contentView);
		mSelector.setFocusable(true);
		mSelector.setBackgroundDrawable(new BitmapDrawable(getResources()));
		mSelector.setItemSelectListener(new ItemSelectListener() {
			@Override
			public void onItemSelected(AppInfo app) {
				if (mSelector != null) {
					mSelector.dismiss();
					mSelector = null;
				}

				boolean found = false;
				for (int i = 0; i < mAppInfo.size(); i++) {
					if (mAppInfo.get(i).getPkgName().equals(app.getPkgName())) {
						found = true;
						mAppInfo.set(i, app);
						break;
					}
				}

				if (!found) {
					mAppInfo.add(app);
				}

				saveAppInfo();
				showAppInfo();
			}
		});

		// 重写onKeyListener
		contentView.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					if (mSelector != null) {
						mSelector.dismiss();
						mSelector = null;
					}
					return true;
				}
				return false;
			}
		});

		mSelector.showAtLocation(PosterMainActivity.this.findViewById(R.id.root), Gravity.TOP, 0, 0);
		return true;
	}

	private void saveAppInfo() {
		SharedPreferences sp = getSharedPreferences("applist", Context.MODE_PRIVATE);
		SharedPreferences.Editor mEdit1 = sp.edit();
		mEdit1.putInt("app_size", mAppInfo.size()); /* sKey is an array */

		for (int i = 0; i < mAppInfo.size(); i++) {
			mEdit1.remove("app_" + i);
			mEdit1.putString("app_" + i, mAppInfo.get(i).getPkgName());
		}

		mEdit1.apply();
	}

	private void showAppInfo() {
		mScrollView = (YSHorizontalScrollView) this.findViewById(R.id.HSVDockBar);
		mScrollView.setItemWidth(144);
		mScrollView.setItemNumber(this.mAppInfo.size() + 1);

		GridView gridView = (GridView) this.findViewById(R.id.gridview);
		GridViewAdapter adapter = new GridViewAdapter();
		ViewGroup.LayoutParams para = gridView.getLayoutParams();

		// float scale = this.getResources().getDisplayMetrics().density;
		para.height = LayoutParams.MATCH_PARENT;
		para.width = (this.mAppInfo.size() + 1) * 108;
		gridView.setAdapter(adapter);
		gridView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Intent intent = mAppInfo.get(position).getIntent();
				startActivity(intent);
			}
		});

		gridView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				mAppInfo.remove(position);
				saveAppInfo();
				showAppInfo();
				return true;
			}
		});
	}

	private List<String> getInitAppInfo() {
		ArrayList<String> appList = new ArrayList<String>();
		SharedPreferences sp = getSharedPreferences("applist", Context.MODE_PRIVATE);
		int nAppCnt = sp.getInt("app_size", 0);

		for (int i = 0; i < nAppCnt; i++) {
			appList.add(sp.getString("app_" + i, ""));
		}

		return appList;
	}

	private boolean isInitApp(String appPkgName) {
		List<String> initAppList = getInitAppInfo();
		if (initAppList.isEmpty()) {
			return true; // 如果没有保存APP列表，则将全体APP列出在状态栏中
		}

		for (int i = 0; i < initAppList.size(); i++) {
			if (appPkgName.equals(initAppList.get(i))) {
				return true;
			}
		}

		return false;
	}

	// 获得所有启动Activity的信息，类似于Launch界面
	private void queryInitAppInfo(List<AppInfo> listAppInfo) {
		PackageManager pm = this.getPackageManager(); // 获得PackageManager对象
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		// 通过查询，获得所有ResolveInfo对象.
		List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);
		// 调用系统排序 ， 根据name排序。
		// 该排序很重要，否则只能显示系统应用，而不能列出第三方应用程序。
		Collections.sort(resolveInfos, new ResolveInfo.DisplayNameComparator(pm));

		if (listAppInfo != null) {
			listAppInfo.clear();
			ApplicationInfo info = null;
			String activityName = null;
			String pkgName = null;
			String appLabel = null;
			Drawable icon = null;
			Intent launchIntent = null;
			AppInfo appInfo = null;

			for (ResolveInfo reInfo : resolveInfos) {
				info = reInfo.activityInfo.applicationInfo;
				if (((info.flags & ApplicationInfo.FLAG_SYSTEM) > 0 && (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) || (!isInitApp(reInfo.activityInfo.packageName))) {
					continue;
				}

				activityName = reInfo.activityInfo.name; // 获得该应用程序的启动Activity的name
				pkgName = reInfo.activityInfo.packageName; // 获得应用程序的包名
				appLabel = (String) reInfo.loadLabel(pm); // 获得应用程序的Label
				icon = reInfo.loadIcon(pm); // 获得应用程序图标
				// 为应用程序的启动Activity 准备Intent
				launchIntent = new Intent();
				launchIntent.setComponent(new ComponentName(pkgName, activityName));

				// 创建一个AppInfo对象，并赋值
				appInfo = new AppInfo();
				appInfo.setAppLabel(appLabel);
				appInfo.setPkgName(pkgName);
				appInfo.setAppIcon(icon);
				appInfo.setIntent(launchIntent);
				listAppInfo.add(appInfo); // 添加至列表中
			}
		}
	}

	final class GridViewAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mAppInfo.size();
		}

		@Override
		public Object getItem(int position) {
			return mAppInfo.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) PosterMainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.app_item, parent, false);
			}

			// TextView textView =
			// (TextView)convertView.findViewById(R.id.item_textview);
			// String name = mAppInfo.get(position).getAppLabel();
			// if(name != null){
			// textView.setText(name);
			// }

			ImageView iv = (ImageView) convertView.findViewById(R.id.app_icon);
			iv.setImageDrawable(mAppInfo.get(position).getAppIcon());

			return convertView;
		}
	}

	private void hideNavigationBar() {
		int uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide
																																													// nav
																																													// bar
				| View.SYSTEM_UI_FLAG_FULLSCREEN; // hide status bar

		if (android.os.Build.VERSION.SDK_INT >= 19) {
			uiFlags |= 0x00001000; // SYSTEM_UI_FLAG_IMMERSIVE_STICKY: hide
									// navigation bars - compatibility: building
									// API level is lower thatn 19, use magic
									// number directly for higher API target
									// level
		} else {
			uiFlags |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
		}

		getWindow().getDecorView().setSystemUiVisibility(uiFlags);
	}
}
