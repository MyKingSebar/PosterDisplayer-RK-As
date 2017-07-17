/*
 * Copyright (C) 2013 poster PCE YoungSee Inc.
 * All Rights Reserved Proprietary and Confidential.
 * 
 * @author LiLiang-Ping
 */

package com.youngsee.osd;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.youngsee.common.DialogUtil;
import com.youngsee.common.DialogUtil.DialogDoubleButtonListener;
import com.youngsee.common.DbHelper;
import com.youngsee.common.DialogUtil.DialogThreeButtonListener;
import com.youngsee.common.FileUtils;
import com.youngsee.common.RuntimeExec;
import com.youngsee.common.SysOnOffTimeInfo;
import com.youngsee.common.SysParamManager;
import com.youngsee.logmanager.LogManager;
import com.youngsee.logmanager.LogUtils;
import com.youngsee.logmanager.Logger;
import com.youngsee.multicast.MulticastCommon;
import com.youngsee.multicast.MulticastManager;
import com.youngsee.multicast.MulticastSyncInfoRef;
import com.youngsee.posterdisplayer.PosterMainActivity;
import com.youngsee.posterdisplayer.R;
import com.youngsee.posterdisplayer.PosterApplication;
import com.youngsee.posterdisplayer.PosterOsdActivity;
import com.youngsee.power.PowerOnOffManager;
import com.youngsee.screenmanager.ScreenManager;
import com.youngsee.webservices.WsClient;
import com.youngsee.webservices.XmlCmdInfoRef;

public class OsdSubMenuFragment extends Fragment {
	private static final int MENU_ITEM_COUNT = 4;

	private int mCurrentPage = 0;

	private ViewPager mViewPager = null;
	private PagerAdapter mAdapter = null;

	// ========================================View===========================================
	private View[] mMenus = null;

	// ========================================Dot===========================================
	private ImageView[] mDots = null;

	// ====================================================Server======================================
	private EditText server_webURL = null;
	private EditText server_ftp_IP = null;
	private EditText server_ftp_port = null;
	private EditText server_ftp_count = null;
	private EditText server_ftp_password = null;
	private EditText server_ntp_ip = null;
	private EditText server_ntp_port = null;

	// =====================================================logmanager=================================
	private final static int CHANGEPALYSET = 0xa000;
	private final static int CHANGESYSTEMSET = 0xa001;
	private View logmamagerView = null;
	private CheckBox playcbLogM = null;
	private CheckBox systemcbLogM = null;
	private SharedPreferences spfLogM = null;
	// ====================================================Clock======================================

	private View mEidtClockView = null;
	private EditText mOnTimeEditText = null;
	private EditText mOffTimeEditText = null;
	private CheckBox[] mWeekCheckBoxs = new CheckBox[7];
	private ClockAdapter onOffTimeAdapter = null;
	private Button mAddTimeBtn = null;
	private int mSelectedItemIdx = -1;
	private static final int CONTEXTMENU_REFRESH = 0;
	private static final int CONTEXTMENU_ADDITEM = 1;
	private static final int CONTEXTMENU_EDITITEM = 2;
	private static final int CONTEXTMENU_DELETEITEM = 3;
	private static final int CONTEXTMENU_CLEANITEMS = 4;

	// ====================================================About======================================
	private TextView about_cfe = null;
	private TextView about_kernelver = null;
	private TextView about_swVer = null;
	private TextView about_hwVer = null;
	private TextView about_id = null;
	private TextView about_MAC = null;
	private TextView about_IP = null;
	private TextView about_termGrp = null;
	private TextView about_certNum = null;
	private TextView about_connStatus = null;
	private TextView about_diskStatus = null;

	private TableRow about_tableRow_certNum = null;
	private TableRow about_tableRow_termGrp = null;

	private final Handler mHandler = new Handler();

	// Action id for login/pw fragment
	public final static int UPDATE_PROGRAM_ACTION_ID = 1;
	public final static int CLEAN_PROGRAM_ACTION_ID = 2;
	public final static String LOGIN_FRAGMENT_TAG = "OsdLoginTag";

	private List<ClockItem> mOldClockItemList = new ArrayList<ClockItem>();

	public static OsdSubMenuFragment INSTANCE = null;

	private final int DEFAULT_ONOFF_MINUTE = 5;
	private final int ONOFF_MINIMUM_INTERVAL = DEFAULT_ONOFF_MINUTE * 60;

	private final int ONEDAYSECONDS = 24 * 3600;

	private boolean mIsKeptAlertDialog = false;

	private boolean mIsInit = false;

	// ====================================== Dialog =====================================

	private Dialog mOnOffAlertDialog = null;
	private Dialog dlgFactoryDeader = null;
	private Dialog dlgUDiskUpdateHeader = null;
	private Dialog dlgClearAllPro = null;
	private Dialog dlgMulticastDisplayControl = null;
	private Dialog dlgCancelSavePwd = null;
	private Dialog reloadWebViewDlg = null;
	private Dialog logManagerDialog = null;

	// ====================================================================================

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		INSTANCE = this;

		Bundle args = getArguments();
		if (args != null) {
			mCurrentPage = args.getInt("osdMenuId");
		}
	}

	/**
	 * Create the view for this fragment, using the arguments given to it.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// 不能将Fragment的视图附加到此回调的容器元素，因此attachToRoot参数必须为false
		return inflater.inflate(R.layout.fragment_osd_submenu, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		mIsInit = true;
		super.onActivityCreated(savedInstanceState);
		initOsdSubMenuFragment();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onDestroy() {
		saveOsdParam();
		INSTANCE = null;
		super.onDestroy();
	}

	private boolean isClockParamChanged() {
		if (onOffTimeAdapter.getCount() != mOldClockItemList.size()) {
			return true;
		} else {
			ClockItem newclockItem, oldclockItem;
			for (int i = 0; i < onOffTimeAdapter.getCount(); i++) {
				newclockItem = (ClockItem) onOffTimeAdapter.getItem(i);
				oldclockItem = mOldClockItemList.get(i);
				if ((!newclockItem.getOnTime().equals(oldclockItem.getOnTime())) || !newclockItem.getOffTime().equals(oldclockItem.getOffTime()) || newclockItem.getWeek() != oldclockItem.getWeek()) {
					return true;
				}
			}
		}
		return false;
	}

	private void saveOsdParam() {
		saveClockParam();
		saveServerParam();
		if (isClockParamChanged()) {
			new Thread(new Runnable() {
				public void run() {
					String strLocalSysParam = SysParamManager.getInstance().getFormatXmlParam();
					WsClient.getInstance().postResultBack(XmlCmdInfoRef.CMD_PTL_SYNCSYSDATA, 0, 0, strLocalSysParam);
				}
			}).start();

			PosterMainActivity.INSTANCE.checkAndSetOnOffTime(PowerOnOffManager.AUTOSCREENOFF_COMMON);
		}
	}

	private void initOsdSubMenuFragment() {
		initDot();
		initViewPager();
		// initLanguage();
		initServerView();
		initClockView();
		initToolsView();
		initAboutView();
		mViewPager.setCurrentItem(mCurrentPage);

		ViewTreeObserver vto = getView().getViewTreeObserver();
		vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				if (!mIsInit) {
					// 检查软键盘是否弹出
					if (getActivity() != null && getActivity().getWindow() != null && getActivity().getWindow().peekDecorView() != null) {
						View decorView = getActivity().getWindow().peekDecorView();
						Rect rect = new Rect();
						decorView.getWindowVisibleDisplayFrame(rect);
						int displayHight = rect.bottom - rect.top;
						int hight = decorView.getHeight();
						boolean softInputIsVisible = (double) displayHight / hight < 0.8;
						if (softInputIsVisible && PosterOsdActivity.INSTANCE != null) {
							PosterOsdActivity.INSTANCE.cancelDismissTime();
						} else {
							PosterOsdActivity.INSTANCE.setDismissTime();
						}
					}
				} else {
					mIsInit = false;
				}
			}
		});
	}

	private void initDot() {
		((ImageView) getActivity().findViewById(R.id.home)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (getActivity() instanceof PosterOsdActivity) {
					((PosterOsdActivity) getActivity()).startOsdMenuFragment(PosterOsdActivity.OSD_MAIN_ID);
				}
			}
		});

		((ImageView) getActivity().findViewById(R.id.exit)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getActivity().finish();
			}
		});

		((ImageView) getActivity().findViewById(R.id.forward)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (++mCurrentPage < MENU_ITEM_COUNT) {
					mViewPager.setCurrentItem(mCurrentPage);
				} else {
					mCurrentPage = MENU_ITEM_COUNT - 1;
				}
			}
		});

		((ImageView) getActivity().findViewById(R.id.back)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (--mCurrentPage >= 0) {
					mViewPager.setCurrentItem(mCurrentPage);
				} else {
					mCurrentPage = 0;
				}
			}
		});

		mDots = new ImageView[MENU_ITEM_COUNT];
		mDots[PosterOsdActivity.OSD_SERVER_ID] = (ImageView) getActivity().findViewById(R.id.iv_server);
		mDots[PosterOsdActivity.OSD_CLOCK_ID] = (ImageView) getActivity().findViewById(R.id.iv_clock);
		mDots[PosterOsdActivity.OSD_TOOL_ID] = (ImageView) getActivity().findViewById(R.id.iv_tools);
		mDots[PosterOsdActivity.OSD_ABOUT_ID] = (ImageView) getActivity().findViewById(R.id.iv_about);
		mDots[mCurrentPage].setBackgroundResource(R.drawable.dot_white);
	}

	private void initViewPager() {
		mMenus = new View[MENU_ITEM_COUNT];
		mMenus[PosterOsdActivity.OSD_SERVER_ID] = LayoutInflater.from(getActivity()).inflate(R.layout.osd_server, null);
		mMenus[PosterOsdActivity.OSD_CLOCK_ID] = LayoutInflater.from(getActivity()).inflate(R.layout.osd_clock, null);
		mMenus[PosterOsdActivity.OSD_TOOL_ID] = LayoutInflater.from(getActivity()).inflate(R.layout.osd_tools, null);
		mMenus[PosterOsdActivity.OSD_ABOUT_ID] = LayoutInflater.from(getActivity()).inflate(R.layout.osd_about, null);

		mAdapter = new PagerAdapter() {
			public boolean isViewFromObject(View arg0, Object arg1) {
				return (arg0 == arg1);
			}

			public Object instantiateItem(ViewGroup container, int position) {
				container.addView(mMenus[position]);
				return mMenus[position];
			}

			public void destroyItem(ViewGroup container, int position, Object object) {
				container.removeView(mMenus[position]);
			}

			public int getCount() {
				return mMenus.length;
			}
		};

		mViewPager = (ViewPager) getActivity().findViewById(R.id.viewpager);
		mViewPager.setAdapter(mAdapter);
		mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
			public void onPageSelected(int arg0) {
				setHighlight(arg0);
				if (arg0 == PosterOsdActivity.OSD_ABOUT_ID) {
					updateAboutView();
				} else if (arg0 == PosterOsdActivity.OSD_CLOCK_ID) {
					mSelectedItemIdx = -1;
					saveServerParam();
				} else if (arg0 == PosterOsdActivity.OSD_SERVER_ID) {
					updateServerView();
				}
			}

			public void onPageScrolled(int arg0, float arg1, int arg2) {

			}

			public void onPageScrollStateChanged(int arg0) {

			}
		});
	}

	private void initServerView() {
		server_webURL = (EditText) mMenus[PosterOsdActivity.OSD_SERVER_ID].findViewById(R.id.server_webURL);
		server_ftp_IP = (EditText) mMenus[PosterOsdActivity.OSD_SERVER_ID].findViewById(R.id.server_ftp_IP);
		server_ftp_port = (EditText) mMenus[PosterOsdActivity.OSD_SERVER_ID].findViewById(R.id.server_ftp_port);
		server_ftp_count = (EditText) mMenus[PosterOsdActivity.OSD_SERVER_ID].findViewById(R.id.server_ftp_count);
		server_ftp_password = (EditText) mMenus[PosterOsdActivity.OSD_SERVER_ID].findViewById(R.id.server_ftp_password);
		server_ntp_ip = (EditText) mMenus[PosterOsdActivity.OSD_SERVER_ID].findViewById(R.id.server_ntp_ip);
		server_ntp_port = (EditText) mMenus[PosterOsdActivity.OSD_SERVER_ID].findViewById(R.id.server_ntp_port);

		server_webURL.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
					if (imm.isActive()) {
						imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
					}

					saveServerParam();
				}
			}
		});

		server_ftp_IP.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
					if (imm.isActive()) {
						imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
					}
				}
			}
		});

		server_ftp_port.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
					if (imm.isActive()) {
						imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
					}
				}
			}
		});

		server_ftp_count.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
					if (imm.isActive()) {
						imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
					}
				}
			}
		});

		server_ftp_password.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
					if (imm.isActive()) {
						imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
					}
				}
			}
		});

		server_ntp_ip.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
					if (imm.isActive()) {
						imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
					}
				}
			}
		});

		server_ntp_port.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
					if (imm.isActive()) {
						imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
					}
				}
			}
		});

		updateServerView();
	}

	private void updateServerView() {
		String weburl = null;
		String ftpip = null;
		String ftpport = null;
		String ftpname = null;
		String ftppasswd = null;
		String ntpip = null;
		String ntpport = null;

		ConcurrentHashMap<String, String> serverSet = SysParamManager.getInstance().getServerParam();
		if (serverSet != null) {
			weburl = serverSet.get("weburl");
			ftpip = serverSet.get("ftpip");
			ftpport = serverSet.get("ftpport");
			ftpname = serverSet.get("ftpname");
			ftppasswd = serverSet.get("ftppasswd");
			ntpip = serverSet.get("ntpip");
			ntpport = serverSet.get("ntpport");
		}

		if (weburl != null && weburl.endsWith("asmx") && weburl.length() > WsClient.SERVICE_URL_SUFFIX.length()) {
			weburl = weburl.substring(0, (weburl.length() - WsClient.SERVICE_URL_SUFFIX.length()));
		}

		server_webURL.setText(weburl != null ? weburl : "");
		server_ftp_IP.setText(ftpip != null ? ftpip : "");
		server_ftp_port.setText(ftpport != null ? ftpport : "");
		server_ftp_count.setText(ftpname != null ? ftpname : "");
		server_ftp_password.setText(ftppasswd != null ? ftppasswd : "");
		server_ntp_ip.setText(ntpip != null ? ntpip : "");
		server_ntp_port.setText(ntpport != null ? ntpport : "");
	}

	private Runnable rPopupDelay = new Runnable() {
		@Override
		public void run() {
			mHandler.removeCallbacks(rPopupDelay);
			addOnOffTimeItem();
		}
	};

	private void closeContextMenuOrAlertDialog() {
		getActivity().closeContextMenu();
		if ((mOnOffAlertDialog != null) && (mOnOffAlertDialog.isShowing() || mIsKeptAlertDialog)) {
			if (mIsKeptAlertDialog) {
				dismissDialog(mOnOffAlertDialog);
			}
			mOnOffAlertDialog.dismiss();
		}
	}

	public void reflashOnOffTime() {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				closeContextMenuOrAlertDialog();
				ConcurrentHashMap<String, String> onOffTime = SysParamManager.getInstance().getOnOffTimeParam();
				if (onOffTime != null) {
					mOldClockItemList.clear();
					onOffTimeAdapter.removeAllItem();

					ClockItem item = null;
					int week = 0;
					String onTime = null;
					String offTime = null;
					int nGroup = Integer.parseInt(onOffTime.get("group"));
					for (int i = 1; i <= nGroup; i++) {
						item = new ClockItem();
						onTime = (onOffTime.get("on_time" + i) != null) ? onOffTime.get("on_time" + i) : "";
						offTime = (onOffTime.get("off_time" + i) != null) ? onOffTime.get("off_time" + i) : "";
						week = (onOffTime.get("week" + i) != null) ? Integer.parseInt(onOffTime.get("week" + i)) : 0;
						item.setWeek(week);
						item.setOnTime(onTime);
						item.setOffTime(offTime);
						onOffTimeAdapter.addItem(item);

						mOldClockItemList.add(new ClockItem(item.getOnTime(), item.getOffTime(), item.getWeek()));
					}
				}
			}
		});
	}

	private void initClockView() {
		mEidtClockView = LayoutInflater.from(getActivity()).inflate(R.layout.osd_edit_clock, null);
		mOnTimeEditText = (EditText) mEidtClockView.findViewById(R.id.et_clock_onTime);
		mOffTimeEditText = (EditText) mEidtClockView.findViewById(R.id.et_clock_offTime);
		mWeekCheckBoxs[1] = (CheckBox) mEidtClockView.findViewById(R.id.osd_onoffTime_mon_dgcbox);
		mWeekCheckBoxs[2] = (CheckBox) mEidtClockView.findViewById(R.id.osd_onoffTime_tue_dgcbox);
		mWeekCheckBoxs[3] = (CheckBox) mEidtClockView.findViewById(R.id.osd_onoffTime_wed_dgcbox);
		mWeekCheckBoxs[4] = (CheckBox) mEidtClockView.findViewById(R.id.osd_onoffTime_thu_dgcbox);
		mWeekCheckBoxs[5] = (CheckBox) mEidtClockView.findViewById(R.id.osd_onoffTime_fri_dgcbox);
		mWeekCheckBoxs[6] = (CheckBox) mEidtClockView.findViewById(R.id.osd_onoffTime_sat_dgcbox);
		mWeekCheckBoxs[0] = (CheckBox) mEidtClockView.findViewById(R.id.osd_onoffTime_sun_dgcbox);

		ListView clock_listview = (ListView) mMenus[PosterOsdActivity.OSD_CLOCK_ID].findViewById(R.id.clock_listview);
		mAddTimeBtn = (Button) mMenus[PosterOsdActivity.OSD_CLOCK_ID].findViewById(R.id.osd_clock_addbtn);
		onOffTimeAdapter = new ClockAdapter(getActivity());

		ConcurrentHashMap<String, String> onOffTime = SysParamManager.getInstance().getOnOffTimeParam();
		if (onOffTime != null) {
			ClockItem item = null;
			int week = 0;
			String onTime = null;
			String offTime = null;
			int nGroup = Integer.parseInt(onOffTime.get("group"));
			for (int i = 1; i <= nGroup; i++) {
				item = new ClockItem();
				onTime = (onOffTime.get("on_time" + i) != null) ? onOffTime.get("on_time" + i) : "";
				offTime = (onOffTime.get("off_time" + i) != null) ? onOffTime.get("off_time" + i) : "";
				week = (onOffTime.get("week" + i) != null) ? Integer.parseInt(onOffTime.get("week" + i)) : 0;
				item.setOnTime(onTime);
				item.setOffTime(offTime);
				item.setWeek(week);
				onOffTimeAdapter.addItem(item);

				mOldClockItemList.add(new ClockItem(item.getOnTime(), item.getOffTime(), item.getWeek()));
			}
		}

		clock_listview.setAdapter(onOffTimeAdapter);
		clock_listview.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				mSelectedItemIdx = position;
				return false;
			}
		});

		clock_listview.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					if (onOffTimeAdapter.getCount() == 0) {
						mHandler.postDelayed(rPopupDelay, 2000);
					}
					break;
				case MotionEvent.ACTION_UP:
					if (onOffTimeAdapter.getCount() == 0) {
						mHandler.removeCallbacks(rPopupDelay);
					}

					break;
				}

				return false;
			}

		});

		mAddTimeBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				addOnOffTimeItem();
			}
		});
		registerForContextMenu(clock_listview);
	}

	/*
	 * private void updateClockView() { ConcurrentHashMap<String, String>
	 * onOffTime = SysParamManager.getInstance().getOnOffTimeParam(); if
	 * (onOffTimeAdapter != null && onOffTime != null) {
	 * onOffTimeAdapter.removeAllItem(); ClockItem item = null; int week = 0;
	 * String onTime = null; String offTime = null; int nGroup =
	 * Integer.parseInt(onOffTime.get("group")); for (int i = 1; i <= nGroup;
	 * i++) { item = new ClockItem(); onTime = (onOffTime.get("on_time" + i) !=
	 * null) ? onOffTime.get("on_time" + i) : ""; offTime =
	 * (onOffTime.get("off_time" + i) != null) ? onOffTime.get("off_time" + i) :
	 * ""; week = (onOffTime.get("week" + i) != null) ?
	 * Integer.parseInt(onOffTime.get("week" + i)) : 0; item.setOnTime(onTime);
	 * item.setOffTime(offTime); item.setWeek(week);
	 * onOffTimeAdapter.addItem(item); } } }
	 */

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle(R.string.clock_dialog_header);
		menu.add(0, CONTEXTMENU_REFRESH, 0, R.string.clock_dialog_refrech);
		menu.add(0, CONTEXTMENU_ADDITEM, 0, R.string.clock_dialog_add);
		menu.add(0, CONTEXTMENU_EDITITEM, 0, R.string.clock_dialog_modify);
		menu.add(0, CONTEXTMENU_DELETEITEM, 0, R.string.clock_dialog_del);
		menu.add(0, CONTEXTMENU_CLEANITEMS, 0, R.string.clock_dialog_delall);
	}

	@Override
	public boolean onContextItemSelected(MenuItem aItem) {
		if (mSelectedItemIdx < 0) {
			return false;
		}

		boolean bRet = false;
		if (onOffTimeAdapter != null) {
			switch (aItem.getItemId()) {
			case CONTEXTMENU_REFRESH:
				bRet = true;
				onOffTimeAdapter.notifyDataSetChanged();
				break;

			case CONTEXTMENU_ADDITEM:
				bRet = true;
				addOnOffTimeItem();
				break;

			case CONTEXTMENU_EDITITEM:
				bRet = true;
				editOnOffTimeItem();
				break;

			case CONTEXTMENU_DELETEITEM:
				bRet = true;
				onOffTimeAdapter.removeItem(mSelectedItemIdx);
				break;

			case CONTEXTMENU_CLEANITEMS:
				bRet = true;
				onOffTimeAdapter.removeAllItem();
				break;
			}
		}

		return bRet;
	}

	private void initAboutView() {
		about_cfe = (TextView) mMenus[PosterOsdActivity.OSD_ABOUT_ID].findViewById(R.id.about_cfe);
		about_kernelver = (TextView) mMenus[PosterOsdActivity.OSD_ABOUT_ID].findViewById(R.id.about_kernelver);
		about_swVer = (TextView) mMenus[PosterOsdActivity.OSD_ABOUT_ID].findViewById(R.id.about_swVer);
		about_hwVer = (TextView) mMenus[PosterOsdActivity.OSD_ABOUT_ID].findViewById(R.id.about_hwVer);
		about_id = (TextView) mMenus[PosterOsdActivity.OSD_ABOUT_ID].findViewById(R.id.about_id);
		about_MAC = (TextView) mMenus[PosterOsdActivity.OSD_ABOUT_ID].findViewById(R.id.about_MAC);
		about_IP = (TextView) mMenus[PosterOsdActivity.OSD_ABOUT_ID].findViewById(R.id.about_IP);
		about_termGrp = (TextView) mMenus[PosterOsdActivity.OSD_ABOUT_ID].findViewById(R.id.about_termGrp);
		about_certNum = (TextView) mMenus[PosterOsdActivity.OSD_ABOUT_ID].findViewById(R.id.about_certNum);
		about_connStatus = (TextView) mMenus[PosterOsdActivity.OSD_ABOUT_ID].findViewById(R.id.about_connStatus);
		about_diskStatus = (TextView) mMenus[PosterOsdActivity.OSD_ABOUT_ID].findViewById(R.id.about_diskStatus);
		about_tableRow_certNum = (TableRow) mMenus[PosterOsdActivity.OSD_ABOUT_ID].findViewById(R.id.about_tableRow_certNum);
		about_tableRow_termGrp = (TableRow) mMenus[PosterOsdActivity.OSD_ABOUT_ID].findViewById(R.id.about_tableRow_termGrp);

		updateAboutView();
	}

	private void updateAboutView() {
		about_cfe.setText(SysParamManager.getInstance().getCfeVer());
		about_kernelver.setText(SysParamManager.getInstance().getKernelVer());
		about_swVer.setText(SysParamManager.getInstance().getSwVer());
		about_hwVer.setText(SysParamManager.getInstance().getHwVer());
		about_connStatus.setText(WsClient.getInstance().isOnline() ? R.string.serv_online : R.string.serv_offline);
		about_id.setText(PosterApplication.getCpuId().toUpperCase(Locale.getDefault()));
		about_MAC.setText(PosterApplication.getEthFormatMac().toUpperCase(Locale.getDefault()));
		about_diskStatus.setText(FileUtils.getDiskUseSpace() + "/" + FileUtils.getDiskSpace());
		if (WsClient.getInstance().isOnline()) {
			about_IP.setText(PosterApplication.getLocalIpAddress());
			about_tableRow_certNum.setVisibility(View.VISIBLE);
			about_certNum.setText(SysParamManager.getInstance().getCertNum());
			about_tableRow_termGrp.setVisibility(View.VISIBLE);
			about_termGrp.setText(SysParamManager.getInstance().getTermGrp());
		} else {
			about_IP.setText("0.0.0.0");
			about_tableRow_certNum.setVisibility(View.GONE);
			about_tableRow_termGrp.setVisibility(View.GONE);
		}
	}

	private void initToolsView() {
		// 清空磁盘节目
		((Button) mMenus[PosterOsdActivity.OSD_TOOL_ID].findViewById(R.id.tools_format_disk)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				PosterOsdActivity.INSTANCE.cancelDismissTime();
				dlgClearAllPro = DialogUtil.showTipsDialog(getActivity(), getString(R.string.tools_dialog_clearallporgram), getString(R.string.enter), getString(R.string.cancel), new DialogDoubleButtonListener() {

					@Override
					public void onLeftClick(Context context, View v, int which) {
						new CleanDisk(getActivity()).cleanProgram();
						ScreenManager.getInstance().osdNotify(ScreenManager.CLEAR_PGM_OPERATE);
						if (dlgClearAllPro != null) {
							dlgClearAllPro.dismiss();
							dlgClearAllPro = null;
						}
					}

					@Override
					public void onRightClick(Context context, View v, int which) {
						PosterOsdActivity.INSTANCE.setDismissTime();
						if (dlgClearAllPro != null) {
							dlgClearAllPro.dismiss();
							dlgClearAllPro = null;
						}
					}
				}, false);

				dlgClearAllPro.show();

				DialogUtil.dialogTimeOff(dlgClearAllPro, 90000);

			}
		});

		// USB更新节目
		((Button) mMenus[PosterOsdActivity.OSD_TOOL_ID].findViewById(R.id.tools_usb_update_program)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				PosterOsdActivity.INSTANCE.cancelDismissTime();
				dlgUDiskUpdateHeader = DialogUtil.showTipsDialog(getActivity(), getString(R.string.tools_dialog_u_disk_update_header), getString(R.string.enter), getString(R.string.cancel), new DialogDoubleButtonListener() {

					@Override
					public void onLeftClick(Context context, View v, int which) {
						UDiskUpdata diskUpdate = new UDiskUpdata(getActivity());
						diskUpdate.updateProgram();
						if (dlgUDiskUpdateHeader != null) {
							dlgUDiskUpdateHeader.dismiss();
							dlgUDiskUpdateHeader = null;
						}
					}

					@Override
					public void onRightClick(Context context, View v, int which) {
						PosterOsdActivity.INSTANCE.setDismissTime();
						if (dlgUDiskUpdateHeader != null) {
							dlgUDiskUpdateHeader.dismiss();
							dlgUDiskUpdateHeader = null;
						}
					}

				}, false);

				dlgUDiskUpdateHeader.show();

				DialogUtil.dialogTimeOff(dlgUDiskUpdateHeader, 90000);

			}
		});

		// 配置文件备份
		((Button) mMenus[PosterOsdActivity.OSD_TOOL_ID].findViewById(R.id.tools_configuration_file_backup)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// copyConfigfile();
			}
		});

		// 配置文件恢复
		((Button) mMenus[PosterOsdActivity.OSD_TOOL_ID].findViewById(R.id.tools_configuration_file_restore)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				/*
				 * recoverConfigfile(); updateServerView(); updateClockView();
				 * updateAboutView();
				 */
			}
		});

		// 开机画面更新
		/*
		 * ((Button) mMenus[PosterOsdActivity.OSD_TOOL_ID].findViewById(R.id.
		 * tools_boot_screen_update)) .setOnClickListener(new OnClickListener()
		 * {
		 * 
		 * @Override public void onClick(View v) { new
		 * AlertDialog.Builder(getActivity
		 * ()).setTitle(R.string.tools_dialog_u_disk_update_header)
		 * .setPositiveButton(R.string.enter, new
		 * DialogInterface.OnClickListener() {
		 * 
		 * @Override public void onClick(DialogInterface dialog, int which) { //
		 * PosterApplication.getInstance().updateStartUpPicFromUSB();
		 * UDiskUpdata diskUpdate = new UDiskUpdata(getActivity());
		 * diskUpdate.updateStartupPic(); }
		 * }).setNegativeButton(R.string.cancel, new
		 * DialogInterface.OnClickListener() {
		 * 
		 * @Override public void onClick(DialogInterface dialog, int which) { }
		 * }).show(); } });
		 */

		// 待机画面更新
		((Button) mMenus[PosterOsdActivity.OSD_TOOL_ID].findViewById(R.id.tools_standby_screen_update)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				PosterOsdActivity.INSTANCE.cancelDismissTime();
				dlgUDiskUpdateHeader = DialogUtil.showTipsDialog(getActivity(), getString(R.string.tools_dialog_u_disk_update_header), getString(R.string.enter), getString(R.string.cancel), new DialogDoubleButtonListener() {
					@Override
					public void onLeftClick(Context context, View v, int which) {
						UDiskUpdata diskUpdate = new UDiskUpdata(getActivity());
						diskUpdate.updateStandbyPic();
						if (dlgUDiskUpdateHeader != null) {
							dlgUDiskUpdateHeader.dismiss();
							dlgUDiskUpdateHeader = null;
						}
					}

					@Override
					public void onRightClick(Context context, View v, int which) {
						PosterOsdActivity.INSTANCE.setDismissTime();
						if (dlgUDiskUpdateHeader != null) {
							dlgUDiskUpdateHeader.dismiss();
							dlgUDiskUpdateHeader = null;
						}
					}

				}, false);

				dlgUDiskUpdateHeader.show();

				DialogUtil.dialogTimeOff(dlgUDiskUpdateHeader, 90000);
			}
		});

		// 升级应用软件
		((Button) mMenus[PosterOsdActivity.OSD_TOOL_ID].findViewById(R.id.tools_upgrage_app)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				PosterOsdActivity.INSTANCE.cancelDismissTime();
				dlgUDiskUpdateHeader = DialogUtil.showTipsDialog(getActivity(), getString(R.string.tools_dialog_u_disk_update_header), getString(R.string.enter), getString(R.string.cancel), new DialogDoubleButtonListener() {

					@Override
					public void onLeftClick(Context context, View v, int which) {
						UDiskUpdata diskUpdate = new UDiskUpdata(getActivity());
						diskUpdate.updateSW();
						if (dlgUDiskUpdateHeader != null) {
							dlgUDiskUpdateHeader.dismiss();
							dlgUDiskUpdateHeader = null;
						}
					}

					@Override
					public void onRightClick(Context context, View v, int which) {
						PosterOsdActivity.INSTANCE.setDismissTime();
						if (dlgUDiskUpdateHeader != null) {
							dlgUDiskUpdateHeader.dismiss();
							dlgUDiskUpdateHeader = null;
						}
					}

				}, false);

				dlgUDiskUpdateHeader.show();

				DialogUtil.dialogTimeOff(dlgUDiskUpdateHeader, 90000);
			}
		});

		// 恢复出厂设置
		((Button) mMenus[PosterOsdActivity.OSD_TOOL_ID].findViewById(R.id.tools_restore_factoty_setting)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				PosterOsdActivity.INSTANCE.cancelDismissTime();
				dlgFactoryDeader = DialogUtil.showTipsDialog(getActivity(), getString(R.string.tools_dialog_factory_header), getString(R.string.enter), getString(R.string.cancel), new DialogDoubleButtonListener() {

					@Override
					public void onLeftClick(Context context, View v, int which) {
						RestoreFactory rstFactory = new RestoreFactory(getActivity());
						rstFactory.factoryRestore();

						if (dlgFactoryDeader != null) {
							dlgFactoryDeader.dismiss();
							dlgFactoryDeader = null;
						}

						try {
							RuntimeExec.getInstance().runRootCmd("reboot");
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					@Override
					public void onRightClick(Context context, View v, int which) {
						PosterOsdActivity.INSTANCE.setDismissTime();
						if (dlgFactoryDeader != null) {
							dlgFactoryDeader.dismiss();
							dlgFactoryDeader = null;
						}
					}

				}, false);

				dlgFactoryDeader.show();

				DialogUtil.dialogTimeOff(dlgFactoryDeader, 90000);
			}
		});

		// 网络检测
		((Button) mMenus[PosterOsdActivity.OSD_TOOL_ID].findViewById(R.id.tools_check_network)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				PosterOsdActivity.INSTANCE.cancelDismissTime();
				checkNet();
			}
		});

		// 日志管理
		((Button) mMenus[PosterOsdActivity.OSD_TOOL_ID].findViewById(R.id.tools_log_manager)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				logManager();
			}
		});
		// 取消密码记录
		((Button) mMenus[PosterOsdActivity.OSD_TOOL_ID].findViewById(R.id.tools_cancel_savepwd)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				cancelSavePwd();
			}
		});
		// 网页刷新控制
		((Button) mMenus[PosterOsdActivity.OSD_TOOL_ID].findViewById(R.id.tools_reload_webview)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				reloadWebView();
			}
		});
		// 同步播放
		((Button) mMenus[PosterOsdActivity.OSD_TOOL_ID].findViewById(R.id.multicast_display_control)).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				synchronousPlayControl();
			}
		});

	}

	@SuppressLint("InflateParams")
	private void reloadWebView() {
		final SharedPreferences sharedPreferences = getActivity().getSharedPreferences("reload_for_priod", Activity.MODE_PRIVATE);
		boolean onOffReloadForPeriod = sharedPreferences.getBoolean("isReload", false);
		int timeForPeriod = sharedPreferences.getInt("time_period", 10);
		final View reloadView = LayoutInflater.from(getActivity()).inflate(R.layout.reload_webview_control, null);
		final EditText reloadPeriodET = (EditText) reloadView.findViewById(R.id.reload_period);
		reloadPeriodET.setText(String.valueOf(timeForPeriod));

		final CheckBox reloadWebviewCB = (CheckBox) reloadView.findViewById(R.id.reload_webview);
		reloadWebviewCB.setChecked(onOffReloadForPeriod);
		reloadWebViewDlg = DialogUtil.showTipsDialog(getActivity(), getString(R.string.reload_webview), reloadView, getString(R.string.enter), getString(R.string.cancel), new DialogDoubleButtonListener() {

			@Override
			public void onRightClick(Context context, View v, int which) {
				if (reloadWebViewDlg != null) {
					DialogUtil.hideInputMethod(getActivity(), reloadView, reloadWebViewDlg);
					reloadWebViewDlg.dismiss();
					reloadWebViewDlg = null;
				}
			}

			@Override
			public void onLeftClick(Context context, View v, int which) {
				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.putBoolean("time_period", reloadWebviewCB.isChecked());
				String strTime = reloadPeriodET.getText().toString();
				int t = 10;
				if (!TextUtils.isEmpty(strTime)) {
					t = Integer.valueOf(strTime);
				}
				if (t < 10 && t > 3 * 60 * 60) {
					Toast.makeText(getActivity(), "刷新时间不能低于10秒，或者大于10800", Toast.LENGTH_LONG).show();
				} else {
					editor.putInt("time_period", t);
				}
				editor.apply();
				if (reloadWebViewDlg != null) {
					DialogUtil.hideInputMethod(getActivity(), reloadView, reloadWebViewDlg);
					reloadWebViewDlg.dismiss();
					reloadWebViewDlg = null;
				}
			}
		}, false);
		reloadWebViewDlg.show();
		DialogUtil.dialogTimeOff(reloadWebViewDlg, reloadView, 90000);
	}

	@SuppressLint("InflateParams")
	private void synchronousPlayControl() {
		View multicastCtrlView = LayoutInflater.from(getActivity()).inflate(R.layout.multicast_control, null);

		RadioButton mulGroLed = (RadioButton) multicastCtrlView.findViewById(R.id.multicast_group_leader);
		RadioButton mulGroMember = (RadioButton) multicastCtrlView.findViewById(R.id.multicast_group_member);
		RadioButton mulClo = (RadioButton) multicastCtrlView.findViewById(R.id.multicast_close);

		TextView mulIpAddCon = (TextView) multicastCtrlView.findViewById(R.id.multicast_ip_address_context);
		TextView mulLocalPortCon = (TextView) multicastCtrlView.findViewById(R.id.multicast_local_port_context);
		TextView mulPort = (TextView) multicastCtrlView.findViewById(R.id.multicast_port_context);

		// Read @param from DB
		String strBcastIp = DbHelper.getInstance().getBcastIpFromDB();
		int nLocalPort = DbHelper.getInstance().getBcastLocalPortFromDB();
		int nSyncFlag = DbHelper.getInstance().getPgmSyncFlagFromDB();
		int nPort = DbHelper.getInstance().getBcastPortFromDB();

		switch (nSyncFlag) {
		case MulticastCommon.MC_VALUE_PROGSYNC_OPEN_GROUP_LEADER:
			mulGroLed.setChecked(true);
			break;
		case MulticastCommon.MC_VALUE_PROGSYNC_OPEN_GROUP_MEMBERS:
			mulGroMember.setChecked(true);
			break;
		default:
			mulClo.setChecked(true);
			break;
		}

		mulIpAddCon.setText(strBcastIp);
		mulLocalPortCon.setText(String.valueOf(nLocalPort));
		mulPort.setText(String.valueOf(nPort));
		dlgMulticast(multicastCtrlView, mulGroLed, mulGroMember, mulClo, mulIpAddCon, mulLocalPortCon, mulPort);
	}

	private void dlgMulticast(final View multicastCtrlView, final RadioButton mulGroLed, final RadioButton mulGroMember, final RadioButton mulClo, final TextView mulIpAddCon, final TextView mulLocalPortCon, final TextView mulPort) {
		dlgMulticastDisplayControl = DialogUtil.showTipsDialog(getActivity(), getString(R.string.multicast_display_control), multicastCtrlView, getString(R.string.enter), getString(R.string.cancel), new DialogDoubleButtonListener() {

			@Override
			public synchronized void onLeftClick(Context context, View v, int which) {
				// 获取数据库同步参数
				String strBcastIp = mulIpAddCon.getText().toString();
				int port = 0;
				int localPort = 0;
				String strPort = mulPort.getText().toString();
				String strLocalPort = mulLocalPortCon.getText().toString();

				if (!TextUtils.isEmpty(strLocalPort)) {
					localPort = Integer.valueOf(mulLocalPortCon.getText().toString());
				}

				if (!TextUtils.isEmpty(strPort)) {
					port = Integer.valueOf(mulPort.getText().toString());
				}

				int progsync = MulticastCommon.MC_VALUE_PROGSYNC_CLOSE;

				if (mulGroLed.isChecked() == true) {
					progsync = MulticastCommon.MC_VALUE_PROGSYNC_OPEN_GROUP_LEADER;
				} else if (mulGroMember.isChecked() == true) {
					progsync = MulticastCommon.MC_VALUE_PROGSYNC_OPEN_GROUP_MEMBERS;
				} else if (mulClo.isChecked() == true) {
					progsync = MulticastCommon.MC_VALUE_PROGSYNC_CLOSE;
				}

				if (progsync != MulticastCommon.MC_VALUE_PROGSYNC_CLOSE) {
					Pattern ipCheckString = Pattern.compile("(2{2}[4-9]|23\\d)(\\.( 25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]\\d|\\d)))){3}");
					Matcher m = ipCheckString.matcher(strBcastIp);
					if (!m.matches() && !TextUtils.isEmpty(strBcastIp)) {
						Toast.makeText(getActivity(), "ip地址输入错误，请按照:(224-239).(0-225).(0-225).(0-225)的格式录入。或不做任何输入恢复默认值", Toast.LENGTH_LONG).show();
						strBcastIp = null;
					} else if (!(localPort > 1023 && localPort < 9999) && localPort != 0) {
						Toast.makeText(getActivity(), "本地端口号输入错误，请在（1024-9998）范围内录入。或输入 0 恢复默认值", Toast.LENGTH_SHORT).show();
						localPort = 0;
					} else if ((port == localPort) && port != 0) {
						Toast.makeText(getActivity(), "组播端口号与本地端口号冲突。", Toast.LENGTH_SHORT).show();
					} else if (!(port > 1023 && port < 9999) && port != 0) {
						Toast.makeText(getActivity(), "端口号输入错误，请在（1024-9998）范围内录入。", Toast.LENGTH_SHORT).show();
						port = 0;
					}
				} else {
					Toast.makeText(getActivity(), "关闭同步", Toast.LENGTH_LONG).show();
				}

				DbHelper.getInstance().saveSyncFlagToDb(progsync);
				DbHelper.getInstance().saveSyncIpToDb(strBcastIp);
				DbHelper.getInstance().saveSyncPortToDb(port);
				DbHelper.getInstance().saveSynLocalPortToDb(localPort);

				final int progsyncdb = progsync;
				final int portdb = port;
				final int localPortdb = localPort;
				final String strBcastIpdb = strBcastIp;

				// 获取当前使用的同步参数
				final int currentProgsync = MulticastManager.getInstance().getCurrentSynIdicate();
				final int currentLocalPort = MulticastManager.getInstance().getCurrentLocalPort();
				final int currentPort = MulticastManager.getInstance().getCurrentPort();
				final String currentIp = MulticastManager.getInstance().getCurrentGroupIp();
				Thread synCtr = new Thread("sunCtrol") {
					@Override
					public void run() {
						// 数据库为开，当前为关，开启同步
						if (progsyncdb != MulticastCommon.MC_VALUE_PROGSYNC_CLOSE && currentProgsync == MulticastCommon.MC_VALUE_PROGSYNC_CLOSE) {
							// 启动同步
							MulticastManager.getInstance().startWork();
							Logger.i("启动同步" + currentThread().getId());
						} // 数据库为关，当前为开，关闭同步
						else if (progsyncdb == MulticastCommon.MC_VALUE_PROGSYNC_CLOSE && currentProgsync != MulticastCommon.MC_VALUE_PROGSYNC_CLOSE) {
							// 关闭同步
							MulticastManager.getInstance().stopWork();
							if (currentProgsync == MulticastCommon.MC_VALUE_PROGSYNC_OPEN_GROUP_LEADER) {
								final MulticastSyncInfoRef syncInfo = new MulticastSyncInfoRef();
								syncInfo.SyncFlag = MulticastCommon.MC_SYNCFLAG_CLOSE;
								syncInfo.ProgramState = 0;
								syncInfo.ProgramId = "0";
								syncInfo.PgmVerifyCode = "0";
								syncInfo.WndName = "0";
								syncInfo.MediaFullName = "0";
								syncInfo.MediaSource = "0";
								syncInfo.MediaVerifyCode = "0";
								syncInfo.MediaPosition = 0;
								MulticastManager.getInstance().sendSyncInfo(syncInfo);
							}
							Logger.i("关闭同步" + currentThread().getId());
						} // 数据库为开，当前为开，组长组员身份不同 改变组长组员身份
						else if (progsyncdb != MulticastCommon.MC_VALUE_PROGSYNC_CLOSE && currentProgsync != MulticastCommon.MC_VALUE_PROGSYNC_CLOSE && progsyncdb != currentProgsync) {
							// 改变组长或组员的身份
							MulticastManager.getInstance().restartWork();
							Logger.i("同步身份切换 " + currentThread().getId());
						}// 端口 本地端口 IP改变
						else if ((portdb != currentPort && portdb != 0) || (localPortdb != currentLocalPort && localPortdb != 0) || (!TextUtils.isEmpty(strBcastIpdb) && !strBcastIpdb.equals(currentIp))) {
							// 改变同步参数
							MulticastManager.getInstance().restartWork();
						}
					}
				};
				synCtr.start();
				if (dlgMulticastDisplayControl != null) {
					DialogUtil.hideInputMethod(getActivity(), multicastCtrlView, dlgMulticastDisplayControl);
					dlgMulticastDisplayControl.dismiss();
					dlgMulticastDisplayControl = null;
				}
			}

			@Override
			public void onRightClick(Context context, View v, int which) {
				if (dlgMulticastDisplayControl != null) {
					DialogUtil.hideInputMethod(getActivity(), multicastCtrlView, dlgMulticastDisplayControl);
					dlgMulticastDisplayControl.dismiss();
					dlgMulticastDisplayControl = null;
				}
			}
		}, false);

		dlgMulticastDisplayControl.show();

		DialogUtil.dialogTimeOff(dlgMulticastDisplayControl, multicastCtrlView, 90000);
	}

	private void saveServerParam() {
		String newWeburl = server_webURL.getText().toString();
		String ftpip = server_ftp_IP.getText().toString();
		String ftpport = server_ftp_port.getText().toString();
		String ftpname = server_ftp_count.getText().toString();
		String ftppasswd = server_ftp_password.getText().toString();
		String ntpip = server_ntp_ip.getText().toString();
		String ntpport = server_ntp_port.getText().toString();

		if (!TextUtils.isEmpty(newWeburl) && !newWeburl.endsWith("asmx")) {
			newWeburl = newWeburl + WsClient.SERVICE_URL_SUFFIX;
		}

		String oldWebUrl = "";
		ConcurrentHashMap<String, String> oldServerSet = SysParamManager.getInstance().getServerParam();
		if (oldServerSet != null) {
			oldWebUrl = oldServerSet.get("weburl");
		}

		ConcurrentHashMap<String, String> serverSet = new ConcurrentHashMap<String, String>();
		serverSet.put("weburl", newWeburl != null ? newWeburl : "");
		serverSet.put("ftpip", ftpip != null ? ftpip : "");
		serverSet.put("ftpport", ftpport != null ? ftpport : "");
		serverSet.put("ftpname", ftpname != null ? ftpname : "");
		serverSet.put("ftppasswd", ftppasswd != null ? ftppasswd : "");
		serverSet.put("ntpip", ntpip != null ? ntpip : "");
		serverSet.put("ntpport", ntpport != null ? ntpport : "");
		SysParamManager.getInstance().setServerParam(serverSet);

		if (!TextUtils.isEmpty(newWeburl) && !newWeburl.equals(oldWebUrl)) {
			WsClient.getInstance().osdChangeServerConfig();
		}
	}

	private void saveClockParam() {
		if (onOffTimeAdapter != null) {
			ClockItem item = null;
			int nGroup = onOffTimeAdapter.getCount();
			ConcurrentHashMap<String, String> onOffTime = new ConcurrentHashMap<String, String>();
			onOffTime.put("group", String.valueOf(nGroup));
			for (int i = 0; i < nGroup; i++) {
				item = (ClockItem) onOffTimeAdapter.getItem(i);
				onOffTime.put(("on_time" + (i + 1)), item.getOnTime());
				onOffTime.put(("off_time" + (i + 1)), item.getOffTime());
				onOffTime.put(("week" + (i + 1)), String.valueOf(item.getWeek()));
			}
			SysParamManager.getInstance().setOnOffTimeParam(onOffTime);
		}
	}

	private void setHighlight(int nIdx) {
		for (int i = 0; i < MENU_ITEM_COUNT; i++) {
			mDots[i].setBackgroundResource(R.drawable.dot_black);
		}
		mDots[nIdx].setBackgroundResource(R.drawable.dot_white);
	}

	/*
	 * private void copyConfigfile() { String strMessage = null; File srcFile =
	 * new File(PosterApplication.getSysParamFullPath()); File desFile = new
	 * File(PosterApplication.getSysParamBackupFullPath()); try { if
	 * (FileUtils.copyFileTo(srcFile, desFile)) { strMessage =
	 * getString(R.string.tools_dialog_backup_warn_success); } else { strMessage
	 * = getString(R.string.tools_dialog_backup_warn_failure); } } catch
	 * (IOException e) { e.printStackTrace(); strMessage =
	 * getString(R.string.tools_dialog_backup_warn_failure); }
	 * 
	 * new AlertDialog.Builder(getActivity()).setTitle(strMessage)
	 * .setPositiveButton(R.string.enter, new DialogInterface.OnClickListener()
	 * {
	 * 
	 * @Override public void onClick(DialogInterface dialog, int which) { }
	 * }).show(); }
	 */

	/**
	 * 日志管理
	 */
	private void logManager() {
		spfLogM = getActivity().getSharedPreferences("logset", Activity.MODE_PRIVATE);
		logmamagerView = LayoutInflater.from(getActivity()).inflate(R.layout.logmanager_dialog, null);
		playcbLogM = (CheckBox) logmamagerView.findViewById(R.id.logmanager_dialog_palycbox);
		systemcbLogM = (CheckBox) logmamagerView.findViewById(R.id.logmanager_dialog_systemcbox);
		playcbLogM.setChecked(spfLogM.getBoolean("play", true));
		systemcbLogM.setChecked(spfLogM.getBoolean("system", true));
		playcbLogM.setOnCheckedChangeListener(onCheckedChangeListener);
		systemcbLogM.setOnCheckedChangeListener(onCheckedChangeListener);
		/*
		 * logManagerDialog = DialogUtil.showTipsDialog(getActivity(),
		 * getString(R.string.tools_dialog_log_header), logmamagerView,
		 * getString(R.string.tools_dialog_log_upload),
		 * getString(R.string.cancel), new DialogDoubleButtonListener() {
		 * 
		 * @Override public void onRightClick(Context context, View v, int
		 * which) { if (logManagerDialog != null) { logManagerDialog.dismiss();
		 * logManagerDialog = null; } }
		 * 
		 * @Override public void onLeftClick(Context context, View v, int which)
		 * { LogManager.getInstance().uploadLog(LogManager.LOGTYPE_NORMAL, 0);
		 * if (logManagerDialog != null) { logManagerDialog.dismiss();
		 * logManagerDialog = null; } } }, false);
		 */
		logManagerDialog = DialogUtil.showTipsDialog(getActivity(), getString(R.string.tools_dialog_log_header), null, logmamagerView, getString(R.string.tools_dialog_log_upload), getString(R.string.tools_dialog_log_clear), getString(R.string.cancel), new DialogThreeButtonListener() {

			@Override
			public void onThirdClick(Context context, View v, int which) {
				// TODO Auto-generated method stub
				if (logManagerDialog != null) {
					logManagerDialog.dismiss();
					logManagerDialog = null;
				}
			}

			@Override
			public void onSecondClick(Context context, View v, int which) {
				// TODO Auto-generated method stub
				StringBuilder sb = new StringBuilder();
				sb.append(FileUtils.getHardDiskPath());
				sb.append(File.separator);
				sb.append("ystemplog");
				FileUtils.delDir(sb.toString());
				if (logManagerDialog != null) {
					logManagerDialog.dismiss();
					logManagerDialog = null;
				}
			}

			@Override
			public void onFirstClick(Context context, View v, int which) {
				// TODO Auto-generated method stub
				LogManager.getInstance().uploadLog(LogManager.LOGTYPE_NORMAL, 0);
				if (logManagerDialog != null) {
					logManagerDialog.dismiss();
					logManagerDialog = null;
				}
			}
		}, false);
		logManagerDialog.show();

		DialogUtil.dialogTimeOff(logManagerDialog, 90000);
		/*
		 * LogManagerDialog ldlg = new LogManagerDialog(getActivity(),
		 * R.style.logsetdialog); ldlg.setCancelable(true); Window wd =
		 * ldlg.getWindow(); android.view.WindowManager.LayoutParams lp =
		 * wd.getAttributes(); lp.gravity = Gravity.CENTER;
		 * wd.setAttributes(lp); ldlg.setCanceledOnTouchOutside(false);
		 * ldlg.show();
		 */
	}

	OnCheckedChangeListener onCheckedChangeListener = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
			switch (arg0.getId()) {
			case R.id.logmanager_dialog_palycbox:
				changeLogSet(CHANGEPALYSET, arg1);
				Logger.i("change playlogset");
				break;
			case R.id.logmanager_dialog_systemcbox:
				changeLogSet(CHANGESYSTEMSET, arg1);
				Logger.i("change systemlogset");
				break;
			default:
				break;
			}
		}
	};

	/**
	 * 改变日志记录设置
	 * 
	 * @param which
	 *            哪个日志
	 * @param isselected
	 *            是否记录
	 */
	private void changeLogSet(int which, boolean isselected) {
		boolean isresult = true;
		SharedPreferences.Editor edi = spfLogM.edit();
		switch (which) {
		case CHANGEPALYSET:
			isresult = isselected == spfLogM.getBoolean("play", true) ? isselected : isselected;
			edi.putBoolean("play", isresult);
			LogUtils.getInstance().setPlayLogFlag(isresult);

			break;
		case CHANGESYSTEMSET:
			isresult = isselected == spfLogM.getBoolean("system", true) ? isselected : isselected;
			edi.putBoolean("system", isresult);
			LogUtils.getInstance().setSysLogFlag(isresult);
			break;
		default:
			break;
		}
		edi.commit();
	}

	private void cancelSavePwd() {

		dlgCancelSavePwd = DialogUtil.showTipsDialog(getActivity(), getString(R.string.tools_dialog_cancel_title), getString(R.string.enter), getString(R.string.cancel), new DialogDoubleButtonListener() {

			@Override
			public void onLeftClick(Context context, View v, int which) {
				SharedPreferences mSharedPreferences = getActivity().getSharedPreferences(PosterOsdActivity.OSD_CONFIG, Context.MODE_PRIVATE);
				SharedPreferences.Editor mEditor = mSharedPreferences.edit();
				mEditor.putBoolean(PosterOsdActivity.OSD_ISMEMORY, false);
				mEditor.commit();
				Toast.makeText(getActivity(), getResources().getText(R.string.tools_dialog_cancel_success), Toast.LENGTH_LONG).show();

				if (dlgCancelSavePwd != null) {
					dlgCancelSavePwd.dismiss();
					dlgCancelSavePwd = null;
				}

			}

			@Override
			public void onRightClick(Context context, View v, int which) {
				if (dlgCancelSavePwd != null) {
					dlgCancelSavePwd.dismiss();
					dlgCancelSavePwd = null;
				}
			}

		}, false);

		dlgCancelSavePwd.show();

		DialogUtil.dialogTimeOff(dlgCancelSavePwd, 90000);
	}

	/*
	 * private void recoverConfigfile() { String strMessage = null; File srcFile
	 * = new File(PosterApplication.getSysParamBackupFullPath()); File desFile =
	 * new File(PosterApplication.getSysParamFullPath()); try { if
	 * (FileUtils.copyFileTo(srcFile, desFile)) { strMessage =
	 * getString(R.string.tools_dialog_back_warn_success); mSysParam =
	 * PosterApplication.getInstance().getSysParam(true, false); } else {
	 * strMessage = getString(R.string.tools_dialog_back_warn_failure); } }
	 * catch (IOException e) { e.printStackTrace(); strMessage =
	 * getString(R.string.tools_dialog_back_warn_failure); }
	 * 
	 * new AlertDialog.Builder(getActivity()).setTitle(strMessage)
	 * .setPositiveButton(R.string.enter, new DialogInterface.OnClickListener()
	 * {
	 * 
	 * @Override public void onClick(DialogInterface dialog, int which) { }
	 * }).show(); }
	 */

	private void checkNet() {
		CheckNetDialog dlg = new CheckNetDialog(getActivity(), R.style.netdialog);
		dlg.setCancelable(true);
		Window window = dlg.getWindow();
		android.view.WindowManager.LayoutParams lp = window.getAttributes();
		lp.gravity = Gravity.CENTER;
		window.setAttributes(lp);
		dlg.setCanceledOnTouchOutside(false);
		dlg.show();
	}

	private boolean checkOnOffTimeValid(String ontime, String offtime, int week, int modifiedindex) {
		String[] strOnTime = ontime.split(":");
		String[] strOffTime = offtime.split(":");
		if (strOnTime.length < 3 || strOffTime.length < 3) {
			Logger.i("The given time format is invaild.");
			return false;
		}

		SysOnOffTimeInfo currInfo = new SysOnOffTimeInfo();
		currInfo.week = week;
		currInfo.onhour = Integer.parseInt(strOnTime[0]);
		currInfo.onminute = Integer.parseInt(strOnTime[1]);
		currInfo.onsecond = Integer.parseInt(strOnTime[2]);
		currInfo.offhour = Integer.parseInt(strOffTime[0]);
		currInfo.offminute = Integer.parseInt(strOffTime[1]);
		currInfo.offsecond = Integer.parseInt(strOffTime[2]);

		int currOnSeconds = currInfo.onhour * 3600 + currInfo.onminute * 60 + currInfo.onsecond;
		int currOffSeconds = currInfo.offhour * 3600 + currInfo.offminute * 60 + currInfo.offsecond;
		if ((currOffSeconds - currOnSeconds) <= ONOFF_MINIMUM_INTERVAL) {
			return false;
		}

		if ((24 * 3600 - (currOffSeconds - currOnSeconds)) <= ONOFF_MINIMUM_INTERVAL) {
			for (int i = 0; i < 7; i++) {
				if (i != 6) {
					if (((currInfo.week & (1 << i)) != 0) && ((currInfo.week & (1 << (i + 1))) != 0)) {
						return false;
					}
				} else {
					if (((currInfo.week & (1 << 6)) != 0) && ((currInfo.week & 1) != 0)) {
						return false;
					}
				}
			}
		}

		int group = onOffTimeAdapter.getCount();
		if (group > 0) {
			SysOnOffTimeInfo[] sysInfo = new SysOnOffTimeInfo[group];
			for (int i = 0; i < group; i++) {
				ClockItem cm = (ClockItem) onOffTimeAdapter.getItem(i);
				sysInfo[i] = new SysOnOffTimeInfo();
				sysInfo[i].week = cm.getWeek();
				String[] onTimeStr = cm.getOnTime().split(":");
				sysInfo[i].onhour = Integer.parseInt(onTimeStr[0]);
				sysInfo[i].onminute = Integer.parseInt(onTimeStr[1]);
				sysInfo[i].onsecond = Integer.parseInt(onTimeStr[2]);
				String[] offTimeStr = cm.getOffTime().split(":");
				sysInfo[i].offhour = Integer.parseInt(offTimeStr[0]);
				sysInfo[i].offminute = Integer.parseInt(offTimeStr[1]);
				sysInfo[i].offsecond = Integer.parseInt(offTimeStr[2]);
			}

			for (int i = 0; i < 7; i++) {
				if ((currInfo.week & (1 << i)) != 0) {
					for (int j = 0; j < group; j++) {
						if (j != modifiedindex) {
							int dayIdx;
							int tmpCurrOnSeconds, tmpCurrOffSeconds;
							int onSeconds, offSeconds;

							// Check on time
							dayIdx = i;
							for (int k = 0; k < 2; k++) {
								if ((sysInfo[j].week & (1 << dayIdx)) != 0) {
									if (k != 0) {
										tmpCurrOnSeconds = ONEDAYSECONDS * k + currOnSeconds;
									} else {
										tmpCurrOnSeconds = currOnSeconds;
									}
									offSeconds = sysInfo[j].offhour * 3600 + sysInfo[j].offminute * 60 + sysInfo[j].offsecond;
									if (tmpCurrOnSeconds > offSeconds) {
										if ((tmpCurrOnSeconds - offSeconds) <= ONOFF_MINIMUM_INTERVAL) {
											return false;
										} else {
											break;
										}
									}
								}
								dayIdx = (dayIdx != 6) ? dayIdx + 1 : 0;
							}

							// Check off time
							dayIdx = i;
							for (int k = 0; k < 2; k++) {
								if ((sysInfo[j].week & (1 << dayIdx)) != 0) {
									tmpCurrOffSeconds = currOffSeconds;
									if (k != 0) {
										onSeconds = ONEDAYSECONDS * k + sysInfo[j].onhour * 3600 + sysInfo[j].onminute * 60 + sysInfo[j].onsecond;
									} else {
										onSeconds = sysInfo[j].onhour * 3600 + sysInfo[j].onminute * 60 + sysInfo[j].onsecond;
									}

									if (onSeconds > tmpCurrOffSeconds) {
										if ((onSeconds - tmpCurrOffSeconds) <= ONOFF_MINIMUM_INTERVAL) {
											return false;
										} else {
											break;
										}
									}
								}
								dayIdx = (dayIdx != 0) ? dayIdx - 1 : 6;
							}
						}
					}
				}
			}
		}

		return true;
	}

	private void keepDialogShowing(Dialog dialog) {
		try {
			Field field = dialog.getClass().getDeclaredField("mShowing");
			field.setAccessible(true);
			field.set(dialog, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		mIsKeptAlertDialog = true;
	}

	private void dismissDialog(Dialog dialog) {
		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
		if (imm.isActive()) {
			if (mOnTimeEditText != null) {
				imm.hideSoftInputFromWindow(mOnTimeEditText.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
			}

			if (mOffTimeEditText != null) {
				imm.hideSoftInputFromWindow(mOffTimeEditText.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
			}
		}

		try {
			Field field = dialog.getClass().getDeclaredField("mShowing");
			field.setAccessible(true);
			field.set(dialog, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		mIsKeptAlertDialog = false;
	}

	private void addOnOffTimeItem() {
		if (mEidtClockView == null) {
			return;
		}

		if (onOffTimeAdapter.getCount() > 10) {
			Toast.makeText(getActivity(), R.string.clock_dialog_warn_timenumtransfinite, Toast.LENGTH_SHORT).show();
			return;
		}

		if (mEidtClockView.getParent() != null) {
			((ViewGroup) mEidtClockView.getParent()).removeView(mEidtClockView);
		}

		mOnTimeEditText.setText("00:00:00");
		mOffTimeEditText.setText("00:00:00");
		for (int i = 0; i < 7; i++) {
			mWeekCheckBoxs[i].setChecked(false);
		}
		mOnOffAlertDialog = DialogUtil.showTipsDialog(getActivity(), getString(R.string.clock_dialog_add), mEidtClockView, getString(R.string.enter), getString(R.string.cancel), new DialogDoubleButtonListener() {

			@Override
			public void onLeftClick(Context context, View v, int which) {
				int week = 0;
				for (int i = 0; i < 7; i++) {
					if (mWeekCheckBoxs[i].isChecked()) {
						week |= (1 << i);
					}
				}
				if (!isValidTime(mOnTimeEditText.getText().toString()) || !isValidTime(mOffTimeEditText.getText().toString())) {
					Toast.makeText(getActivity(), R.string.clock_dialog_warn_invalidtime, Toast.LENGTH_LONG).show();
					keepDialogShowing(mOnOffAlertDialog);
				} else if (PosterApplication.compareTwoTime(mOnTimeEditText.getText().toString(), mOffTimeEditText.getText().toString()) >= 0) {
					Toast.makeText(getActivity(), R.string.clock_dialog_warn_format, Toast.LENGTH_LONG).show();
					keepDialogShowing(mOnOffAlertDialog);
				} else if (isTimeConfict(mOnTimeEditText.getText().toString(), mOffTimeEditText.getText().toString(), week, -1)) {
					Toast.makeText(getActivity(), R.string.clock_dialog_warn_timeconfilct, Toast.LENGTH_LONG).show();
					keepDialogShowing(mOnOffAlertDialog);
				} else if (!checkOnOffTimeValid(mOnTimeEditText.getText().toString(), mOffTimeEditText.getText().toString(), week, -1)) {
					Toast.makeText(getActivity(), String.format(getResources().getString(R.string.clock_dialog_warn_timeinvalid), DEFAULT_ONOFF_MINUTE), Toast.LENGTH_LONG).show();
					keepDialogShowing(mOnOffAlertDialog);
				} else {
					ClockItem item = new ClockItem();
					item.setOnTime(mOnTimeEditText.getText().toString());
					item.setOffTime(mOffTimeEditText.getText().toString());
					item.setWeek(week);
					onOffTimeAdapter.addItem(item);

				}

				if (mOnOffAlertDialog != null) {
					DialogUtil.hideInputMethod(getActivity(), mEidtClockView, mOnOffAlertDialog);
					mOnOffAlertDialog.dismiss();
					mOnOffAlertDialog = null;
				}

			}

			public void onRightClick(Context context, View v, int which) {

				if (mOnOffAlertDialog != null) {
					DialogUtil.hideInputMethod(getActivity(), mEidtClockView, mOnOffAlertDialog);
					mOnOffAlertDialog.dismiss();
					mOnOffAlertDialog = null;
				}

			}

		}, false);

		mOnOffAlertDialog.show();

		DialogUtil.dialogTimeOff(mOnOffAlertDialog, mEidtClockView, 90000);
	}

	private boolean isValidTime(final String timeStr) {
		Pattern pattern = Pattern.compile("^([0-2]?[0-9]{1}):([0-5]?[0-9]{1}):([0-5]?[0-9]{1})$");
		Matcher matcher = pattern.matcher(timeStr);
		if (matcher.find()) {
			if ((Integer.parseInt(matcher.group(1)) > 23) || (Integer.parseInt(matcher.group(2)) > 59) || (Integer.parseInt(matcher.group(3)) > 59)) {
				return false;
			}
		} else {
			return false;
		}
		return true;
	}

	public boolean isTimeConfict(String ontime, String offtime, int week, int modifiedindex) {
		int group = onOffTimeAdapter.getCount();

		for (int i = 0; i < group; i++) {
			if (i == modifiedindex) {
				continue;
			}
			ClockItem cm = (ClockItem) onOffTimeAdapter.getItem(i);

			for (int j = 0; j < 7; j++) {
				if (((week & (1 << j)) != 0) && ((cm.getWeek() & (1 << j)) != 0)) {
					if ((PosterApplication.compareTwoTime(ontime, cm.getOnTime()) >= 0 && PosterApplication.compareTwoTime(ontime, cm.getOffTime()) <= 0) || (PosterApplication.compareTwoTime(offtime, cm.getOnTime()) >= 0 && PosterApplication.compareTwoTime(offtime, cm.getOffTime()) <= 0) || (PosterApplication.compareTwoTime(ontime, cm.getOnTime()) < 0 && PosterApplication.compareTwoTime(offtime, cm.getOnTime()) > 0)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private void editOnOffTimeItem() {
		if (mEidtClockView == null) {
			return;
		}

		if (mEidtClockView.getParent() != null) {
			((ViewGroup) mEidtClockView.getParent()).removeView(mEidtClockView);
		}

		ClockItem item = (ClockItem) onOffTimeAdapter.getItem(mSelectedItemIdx);
		mOnTimeEditText.setText(item.getOnTime());
		mOffTimeEditText.setText(item.getOffTime());
		int week = item.getWeek();
		for (int i = 0; i < 7; i++) {
			if ((week & (1 << i)) != 0) {
				mWeekCheckBoxs[i].setChecked(true);
			} else {
				mWeekCheckBoxs[i].setChecked(false);
			}
		}
		mOnOffAlertDialog = DialogUtil.showTipsDialog(getActivity(), getString(R.string.clock_dialog_modify), mEidtClockView, getString(R.string.enter), getString(R.string.cancel), new DialogDoubleButtonListener() {

			@Override
			public void onLeftClick(Context context, View v, int which) {
				int week = 0;
				for (int i = 0; i < 7; i++) {
					if (mWeekCheckBoxs[i].isChecked()) {
						week |= (1 << i);
					}
				}
				if (!isValidTime(mOnTimeEditText.getText().toString()) || !isValidTime(mOffTimeEditText.getText().toString())) {
					Toast.makeText(getActivity(), R.string.clock_dialog_warn_invalidtime, Toast.LENGTH_LONG).show();
					keepDialogShowing(mOnOffAlertDialog);
				} else if (PosterApplication.compareTwoTime(mOnTimeEditText.getText().toString(), mOffTimeEditText.getText().toString()) >= 0) {
					Toast.makeText(getActivity(), R.string.clock_dialog_warn_format, Toast.LENGTH_LONG).show();
					keepDialogShowing(mOnOffAlertDialog);
				} else if (isTimeConfict(mOnTimeEditText.getText().toString(), mOffTimeEditText.getText().toString(), week, mSelectedItemIdx)) {
					Toast.makeText(getActivity(), R.string.clock_dialog_warn_timeconfilct, Toast.LENGTH_LONG).show();
					keepDialogShowing(mOnOffAlertDialog);
				} else if (!checkOnOffTimeValid(mOnTimeEditText.getText().toString(), mOffTimeEditText.getText().toString(), week, mSelectedItemIdx)) {
					Toast.makeText(getActivity(), String.format(getResources().getString(R.string.clock_dialog_warn_timeinvalid), DEFAULT_ONOFF_MINUTE), Toast.LENGTH_LONG).show();
					keepDialogShowing(mOnOffAlertDialog);
				} else {
					ClockItem editItem = (ClockItem) onOffTimeAdapter.getItem(mSelectedItemIdx);
					editItem.setOnTime(mOnTimeEditText.getText().toString());
					editItem.setOffTime(mOffTimeEditText.getText().toString());
					editItem.setWeek(week);
					onOffTimeAdapter.notifyDataSetChanged();

				}

				if (mOnOffAlertDialog != null) {
					DialogUtil.hideInputMethod(getActivity(), mEidtClockView, mOnOffAlertDialog);
					mOnOffAlertDialog.dismiss();
					mOnOffAlertDialog = null;
				}

			}

			@Override
			public void onRightClick(Context context, View v, int which) {

				if (mOnOffAlertDialog != null) {
					DialogUtil.hideInputMethod(getActivity(), mEidtClockView, mOnOffAlertDialog);
					mOnOffAlertDialog.dismiss();
					mOnOffAlertDialog = null;
				}

			}
		}, false);

		mOnOffAlertDialog.show();

		DialogUtil.dialogTimeOff(mOnOffAlertDialog, mEidtClockView, 90000);
	}
}
