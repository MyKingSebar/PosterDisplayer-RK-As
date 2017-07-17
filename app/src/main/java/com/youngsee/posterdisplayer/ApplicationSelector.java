package com.youngsee.posterdisplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

public class ApplicationSelector extends PopupWindow implements OnItemClickListener{
	private Context mContext;
    private ListView mListView;
    private List<AppInfo> mlistAppInfo = null;
    private ItemSelectListener mListener;
    
    public ApplicationSelector(Context context, View view){
        super(view, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

    	mContext = context;
    	mListView = (ListView)view.findViewById(R.id.LVApplications);
    	mlistAppInfo = new ArrayList<AppInfo>();
    	queryAppInfo(mlistAppInfo);
    	BrowseApplicationInfoAdapter browseAppAdapter = new BrowseApplicationInfoAdapter(
    			mContext, mlistAppInfo);
    	mListView.setAdapter(browseAppAdapter);
    	mListView.setOnItemClickListener(this);
    }
    
    public void onItemClick(AdapterView<?> parent, View view, int position, long id){
    	if(mListener != null){
    		mListener.onItemSelected(mlistAppInfo.get(position));
    	}
    }
    
    public void setItemSelectListener(ItemSelectListener listener){
    	mListener = listener;
    }
    
    // 获得所有启动Activity的信息，类似于Launch界面  
    public void queryAppInfo(List<AppInfo> listAppInfo) {
        PackageManager pm = mContext.getPackageManager(); // 获得PackageManager对象  
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        // 通过查询，获得所有ResolveInfo对象.
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);
        // 调用系统排序 ， 根据name排序。
        // 该排序很重要，否则只能显示系统应用，而不能列出第三方应用程序。
        Collections.sort(resolveInfos,new ResolveInfo.DisplayNameComparator(pm));
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
            	if (((info.flags & ApplicationInfo.FLAG_SYSTEM) > 0) &&
            	    ((info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0)) {
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
    
    public static class AppInfo {
        private String appLabel;    
        private Drawable appIcon ;  
        private Intent intent ;     
        private String pkgName ;
        private String iconPath;
        
        public AppInfo(){}
        
        public String getAppLabel() {
            return appLabel;
        }
        public void setAppLabel(String appName) {
            this.appLabel = appName;
        }
        public Drawable getAppIcon() {
            return appIcon;
        }
        public void setAppIcon(Drawable appIcon) {
            this.appIcon = appIcon;
        }
        public Intent getIntent() {
            return intent;
        }
        public void setIntent(Intent intent) {
            this.intent = intent;
        }
        public String getPkgName(){
            return pkgName ;
        }
        public void setPkgName(String pkgName){
            this.pkgName=pkgName ;
        }
        public void setIconPath(String path){
        	this.iconPath = path;
        }
        public String getIconPath(String pkgName){
        	return iconPath ;
        }
    }
    
    public class BrowseApplicationInfoAdapter extends BaseAdapter {
        
        private List<AppInfo> mlistAppInfo = null;
        
        LayoutInflater infater = null;
        
        public BrowseApplicationInfoAdapter(Context context,  List<AppInfo> apps) {
            infater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mlistAppInfo = apps ;
        }
        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            System.out.println("size" + mlistAppInfo.size());
            return mlistAppInfo.size();
        }
        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return mlistAppInfo.get(position);
        }
        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return 0;
        }
        @Override
        public View getView(int position, View convertview, ViewGroup arg2) {
            System.out.println("getView at " + position);
            View view = null;
            ViewHolder holder = null;
            if (convertview == null || convertview.getTag() == null) {
                view = infater.inflate(R.layout.browser_app_item, null);
                holder = new ViewHolder(view);
                view.setTag(holder);
            } 
            else{
                view = convertview ;
                holder = (ViewHolder) convertview.getTag() ;
            }
            AppInfo appInfo = (AppInfo) getItem(position);
            holder.appIcon.setImageDrawable(appInfo.getAppIcon());
            holder.tvAppLabel.setText(appInfo.getAppLabel());
            holder.tvPkgName.setText(appInfo.getPkgName());
            return view;
        }

        class ViewHolder {
            ImageView appIcon;
            TextView tvAppLabel;
            TextView tvPkgName;

            public ViewHolder(View view) {
                this.appIcon = (ImageView) view.findViewById(R.id.imgApp);
                this.tvAppLabel = (TextView) view.findViewById(R.id.tvAppLabel);
                this.tvPkgName = (TextView) view.findViewById(R.id.tvPkgName);
            }
        }
    }
    
    public interface ItemSelectListener{
    	public void onItemSelected(AppInfo app);
    }
}
