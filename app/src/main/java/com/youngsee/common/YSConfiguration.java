package com.youngsee.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import android.app.Application;
import android.content.Context;

/**
 * The class of get all configuration from config.propertie. Created by
 * TianXuguang on 2015/8/8.
 */
public class YSConfiguration{
    private final static String    configFileName          = "config.properties";
    private final static String    key_feature             = "feature";
    private final static String    key_defualt_server_url  = "defualt_server_url";
    private final static String    key_dock_bar            = "lunch_app_dock_bar";
    private final static String    key_environment_monitor = "environment_monitor";
    private final static String    key_install_ysctrl      = "install_ysctrl";
    private final static String    key_board_type          = "board_type";
    private final static String    key_loadpgm_when_media_ready = "loadpgm_when_media_ready";
    private final static String     quit_dialog_show= "quit_dialog_show";
    

    
    public final static String     FEATURE_CODE_YUESHI     = "YueShi";
    public final static String     FEATURE_CODE_COMMON     = "common";
    
    public final static String     BOARD_CODE_01           = "01";
    public final static String     BOARD_CODE_GW           = "GW";
    public final static String     BOARD_CODE_MR           = "MR";
    public final static String     BOARD_CODE_MR_M3        = "MR3";
    
    private static YSConfiguration instance                = null;
    
    // For get the application context. It should has same life circle with
    // application.
    // Because this configuration is global in application.
    private static Application     mApplication            = null;
    
    private String                 mFeatureCode            = null;
    private String                 mServerUrl              = null;
    private Boolean                mHasDockBar             = null;
    private Boolean                mHasEnvironmentMonitor  = null;
    private Boolean                mIsNeedInstallYsctrl    = null;
    private String                 mBoardCode              = null;
    private Boolean                mIsWaitForMediaReady    = null;
    private Boolean                mIsNeedQuitDialog    = null;
    

    
    /**
     * Get Configuration by this function to avoid create multiple object of
     * Configuration.
     * 
     * @return the object of Configuration
     */
    public static YSConfiguration getInstance(Application application){
        if(instance == null){
            mApplication = application;
            instance = new YSConfiguration();
        }
        
        return instance;
    }
    
    /**
     * get the feature code.
     * 
     * @return "YueShi" for YueShi version or "common" for common version
     */
    public String getFeatureCode(){
        if(mFeatureCode == null){
            mFeatureCode = (String)getProperties(key_feature);
        }
        
        return mFeatureCode;
    }

    /**
     * get the defualt server URL.
     * 
     * @return server URL
     */
    public String getDefualtServerUrl(){
        if(mServerUrl == null){
        	mServerUrl = (String)getProperties(key_defualt_server_url);
        }
        
        return mServerUrl;
    }
    
    
    /**
     * get the board code.
     *
     * @return "01" for 01 board
     *         "GW" for GuoWei board
     *         "MR" for MeiRui board
     */
    public String getBoardCode(){
        if(mBoardCode == null){
            mBoardCode = (String)getProperties(key_board_type);
        }

        return mBoardCode;
    }
    
    /**
     * Does the application has dock bar at the bottom of main window.
     * 
     * @return
     */
    public Boolean hasDockBar(){
        if(mHasDockBar == null){
            String temp = getProperties(key_dock_bar);
            if(temp != null){
                mHasDockBar = Boolean.valueOf(temp);
            }
            else{
                mHasDockBar = false;
            }
        }
        
        return mHasDockBar;
    }
    
    /**
     * Does the application has environment monitor.
     * 
     * @return
     */
    public Boolean hasEnvironmentMonitor(){
        if(mHasEnvironmentMonitor == null){
            String temp = getProperties(key_environment_monitor);
            if(temp != null){
                mHasEnvironmentMonitor = Boolean.valueOf(temp);
            }
            else{
                mHasEnvironmentMonitor = false;
            }
        }
        
        return mHasEnvironmentMonitor;
    }
    
    /**
     * Whether need install install YSSysCtroller.apk.
     * 
     * @return
     */
    public Boolean isInstallYsctrl(){
        if(mIsNeedInstallYsctrl == null){
            String temp = getProperties(key_install_ysctrl);
            if(temp != null){
            	mIsNeedInstallYsctrl = Boolean.valueOf(temp);
            }
            else{
            	mIsNeedInstallYsctrl = false;
            }
        }
        
        return mIsNeedInstallYsctrl;
    }
    
    public Boolean showQuitDialog(){
        if(mIsNeedQuitDialog == null){
            String temp = getProperties(quit_dialog_show);
            if(temp != null){
            	mIsNeedQuitDialog = Boolean.valueOf(temp);
            }
            else{
            	mIsNeedQuitDialog = false;
            }
        }
        
        return mIsNeedQuitDialog;
    }
    
    /**
     * Whether need install install YSSysCtroller.apk.
     * 
     * @return
     */
    public Boolean isWaitForMediaReady(){
        if(mIsWaitForMediaReady == null){
            String temp = getProperties(key_loadpgm_when_media_ready);
            if(temp != null){
            	mIsWaitForMediaReady = Boolean.valueOf(temp);
            }
            else{
            	mIsWaitForMediaReady = false;
            }
        }
        
        return mIsWaitForMediaReady;
    }
    
    // get the property by key.
    private String getProperties(String key){
        Context c = mApplication.getApplicationContext();
        String value = null;
        
        try{
            InputStream is = c.getAssets().open(configFileName);
            Properties properties = (new Properties());
            properties.load(is);
            value = properties.getProperty(key);
            
            is.close();
        }
        catch(IOException e){
            e.printStackTrace();
        }
        
        return value;
    }
}
