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
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.youngsee.common.DialogUtil;
import com.youngsee.common.DialogUtil.DialogDoubleButtonListener;
import com.youngsee.common.DialogUtil.DialogSingleButtonListener;
import com.youngsee.common.FileUtils;
import com.youngsee.logmanager.Logger;
import com.youngsee.posterdisplayer.PosterOsdActivity;
import com.youngsee.posterdisplayer.R;
import com.youngsee.posterdisplayer.PosterApplication;
import com.youngsee.screenmanager.ScreenManager;

@SuppressLint({ "SdCardPath", "DefaultLocale" })
public class UDiskUpdata
{
    private Context             mContext                   = null;
    private ProgressDialog      mProgressDlg               = null;
    
    // Define message ID
    private final static int    PROGRAM_UPDATING           = 0x6000;
    private final static int    PROGRAM_UPDATE_FAILED      = 0x6001;
    private final static int    PROGRAM_UPDATE_SUCCESS     = 0x6002;
    private final static int    STARTUP_IMG_UPDATE_SUCCESS = 0x6003;
    private final static int    STARTUP_IMG_UPDATE_FAILED  = 0x6004;
    private final static int    STANDBY_IMG_UPDATE_SUCCESS = 0x6005;
    private final static int    STANDBY_IMG_UPDATE_FAILED  = 0x6006;
    private final static int    APK_SW_UPDATE_FAILED       = 0x6008;
    
    private final static String    STANDBYSCREEN_IMAGE_NAME = "background.jpg";
    private final static String    STARTSCREEN_ZIP_NAME     = "startup.zip";

//====================================		Dialog		===================================    
    private 						Dialog 				dlgUDiskUpdateSuccess 										= 		null;
    private 						Dialog 				dlgUDiskUpdateFailure 											= 		null;
    private 						Dialog 				dlgStartupImgUpdateFailed 									= 		null;
    private 						Dialog 				dlgStartupImgUpdSuc 											= 		null;
    private 						Dialog 				dlgStandbyImgUpdFailed 										= 		null;
    private 						Dialog 				dlgStbImgUpdSucc 												= 		null;
    private 						Dialog 				dlgApkSwUpdFad 													= 		null;
//================================================================================
    public UDiskUpdata(Context context)
    {
        mContext = context;
        mProgressDlg = new ProgressDialog(mContext);
        mProgressDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER); // 设置进度条风格，风格为圆形，旋转的
        mProgressDlg.setTitle(R.string.tools_dialog_u_disk_update_titles); // 设置ProgressDialog 标题
        mProgressDlg.setMessage(mContext.getString(R.string.tools_dialog_u_disk_update_message)); // 设置ProgressDialog
                                                                                                  // 提示信息
        mProgressDlg.setIndeterminate(false);  // 设置ProgressDialog 的进度条是否不明确
        mProgressDlg.setCancelable(false);  // 设置ProgressDialog 是否可以按退回按键取消
    }
    
    public void updateProgram()
    {
        new UDiskUpdateThread().start();
    }
    
    /*
     * 更新开机画面
     */
    public void updateStartupPic()
    {
        String strFileSavePath = PosterApplication.getStartUpScreenImgPath();
        updateImgFromUDisk(STARTSCREEN_ZIP_NAME, strFileSavePath);
    }
    
    /*
     * 更新待机画面
     */
    public void updateStandbyPic()
    {
        String strFileSavePath = PosterApplication.getStandbyScreenImgPath();
        updateImgFromUDisk(STANDBYSCREEN_IMAGE_NAME, strFileSavePath);
    }

    public void updateSW() 
    {
    	String strSWPath = FileUtils.findApkInUdisk();
        if (!TextUtils.isEmpty(strSWPath)) 
        {
        	Intent intent = new Intent(Intent.ACTION_VIEW);
        	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(Uri.fromFile(new File(strSWPath)), "application/vnd.android.package-archive");
            mContext.startActivity(intent);
        } 
        else 
        {
        	mHandler.sendEmptyMessage(APK_SW_UPDATE_FAILED);
        }
    }
    
    private void updateImgFromUDisk(String imgname, String strFileSavePath)
    {
    	String strSrcFile = FileUtils.findFilePath(imgname);
    	if (strSrcFile != null)
    	{
    		try 
    		{
				if (FileUtils.copyFileTo(new File(strSrcFile), new File(strFileSavePath)))
				{
					if (STARTSCREEN_ZIP_NAME.equals(imgname))
				    {
				        mHandler.sendEmptyMessage(STARTUP_IMG_UPDATE_SUCCESS);
				    }
				    else
				    {
				        mHandler.sendEmptyMessage(STANDBY_IMG_UPDATE_SUCCESS);
				    }
					
					return;
				}
			} 
    		catch (IOException e) 
    		{
				e.printStackTrace();
			}
    	}
    	
        if (STARTSCREEN_ZIP_NAME.equals(imgname))
        {
            mHandler.sendEmptyMessage(STARTUP_IMG_UPDATE_FAILED);
        }
        else
        {
            mHandler.sendEmptyMessage(STANDBY_IMG_UPDATE_FAILED);
        }
    }
    
    private final class UDiskUpdateThread extends Thread
    {
        @Override
        public void run()
        {
            mHandler.sendEmptyMessage(PROGRAM_UPDATING);
            
            if (updatePgm())
            {
                ScreenManager.getInstance().osdNotify(ScreenManager.UPDATE_PGM_OPERATE);
                mHandler.sendEmptyMessage(PROGRAM_UPDATE_SUCCESS);
            }
            else
            {
                mHandler.sendEmptyMessage(PROGRAM_UPDATE_FAILED);
            }
        }
        
        // 遍历所有U盘中.pgm文件夹
        private List<File> getUsbProgramList()
        {
        	List<String> listUsbPath = FileUtils.getUsbPathList();
        	if (listUsbPath == null || listUsbPath.isEmpty())
        	{
        		Logger.i("getUsbProgramList(): can't find usb path."); 
        	    return null;	
        	}
        	
        	File usbRootPath = null;
        	File[] usbPaths = null;
        	File[] usbSubPaths = null;
        	List<File> listPgmFile = new ArrayList<File>();
        	for (int i = 0; i < listUsbPath.size(); i++)
        	{
        		usbSubPaths = null;
        		usbRootPath = new File(listUsbPath.get(i));
        		usbPaths = usbRootPath.listFiles();   		
        		if (usbPaths != null)
    			{
        		    if (usbRootPath.getTotalSpace() > 0)
        		    {
        		        for (File usbFile : usbPaths)
        				{
        					if (usbFile.isDirectory() && 
        						usbFile.getName().trim().toLowerCase().endsWith(".pgm"))
        					{
        						listPgmFile.add(usbFile);
        					}
        				}
        		    }
        		    else
        		    {    			
        				for (File usbFile : usbPaths)
        				{
        					if (usbFile.isDirectory() && usbFile.getTotalSpace() > 0)
        					{
        						usbSubPaths = usbFile.listFiles();
        						if (usbSubPaths != null)
        						{
        							for (File usbSubFile : usbSubPaths)
        							{
        								if (usbSubFile.isDirectory() && 
        									usbSubFile.getName().trim().toLowerCase().endsWith(".pgm")) 
        								{
        									listPgmFile.add(usbSubFile);
    									}
        							}
        						}
        					}
        				}   			
        		    }
    			}
        	}

            return listPgmFile;
        }
        
        // 获取U盘上最近更新的节目
        private File findNewestPgm()
        {
            List<File> pgmList = getUsbProgramList();
            if (pgmList == null || pgmList.size() <= 0)
            {
                Logger.i("Didn't found program in the Usb.");
                return null;
            }
            
            File newestFile = pgmList.get(0);
            for (int i = 1; i < pgmList.size(); i++)
            {
                if (pgmList.get(i).lastModified() > newestFile.lastModified())
                {
                    newestFile = pgmList.get(i);
                }
            }
            
            return newestFile;
        }
        
        // 将最近更新的节目.pgm格式的文件夹内容复制到目标磁盘上
        private boolean updatePgm()
        {
            boolean retValue = false;
            File newestFile = findNewestPgm();
            if (newestFile != null)
            {
                try
                {
                	String dstPath = PosterApplication.getProgramPath();
                	FileUtils.cleanupDir(dstPath);
                    retValue = FileUtils.copyDirFilesTo(newestFile.getAbsolutePath(), dstPath);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    retValue = false;
                }
            }
            return retValue;
        }
    }
    
    @SuppressLint({ "HandlerLeak", "ShowToast" })
    final Handler mHandler = new Handler() {
                               @Override
                               public void handleMessage(Message msg)
                               {
                                   switch (msg.what)
                                   {
                                   case PROGRAM_UPDATING:
                                       if (mProgressDlg != null && !mProgressDlg.isShowing())
                                       {
                                           mProgressDlg.show();
                                       }
                                       return;
                                       
                                   case PROGRAM_UPDATE_SUCCESS:
                                       if (mProgressDlg != null && mProgressDlg.isShowing())
                                       {
                                           mProgressDlg.dismiss();
                                       }

                                   dlgUDiskUpdateSuccess = DialogUtil.showTipsDialog(mContext, mContext.getString(R.string.tools_dialog_u_disk_update_success), mContext.getString(R.string.enter), new DialogSingleButtonListener() {
                                    	
										@Override
										public void onSingleClick(Context context , View v , int which) {
											 if (PosterOsdActivity.INSTANCE != null)
											 {
												 PosterOsdActivity.INSTANCE.setDismissTime();
											 }
											 if (dlgUDiskUpdateSuccess != null) {
												 dlgUDiskUpdateSuccess.dismiss();
												 dlgUDiskUpdateSuccess = null;
											}
										}
									}, false);
                                   
                                   dlgUDiskUpdateSuccess.show();
                                   
                                   DialogUtil.dialogTimeOff(dlgUDiskUpdateSuccess, 90000);

                                       return;
                                       
                                   case PROGRAM_UPDATE_FAILED:
                                       if (mProgressDlg != null && mProgressDlg.isShowing())
                                       {
                                           mProgressDlg.dismiss();
                                       }
									   
                                       dlgUDiskUpdateFailure = DialogUtil.showTipsDialog(mContext, mContext.getString(R.string.tools_dialog_u_disk_update_failure),mContext.getString(R.string.retry1),mContext.getString(R.string.cancel), new DialogDoubleButtonListener() {

                                    	   @Override
                                    	   public void onLeftClick(Context context , View v , int which) 
                                    	   {
                                    		   updateProgram();
											 
                                    		   if(dlgUDiskUpdateFailure !=null){
                                    			   dlgUDiskUpdateFailure.dismiss();
                                    			   dlgUDiskUpdateFailure = null;
                                    		   }
											 
                                    	   }

                                    	   @Override
                                    	   public void onRightClick(Context context , View v , int which) 
                                    	   {
                                    		   if (PosterOsdActivity.INSTANCE != null) {
                                    			   PosterOsdActivity.INSTANCE.setDismissTime();
                                    		   }
                                    		   if (dlgUDiskUpdateFailure != null) {
                                    			   dlgUDiskUpdateFailure.dismiss();
                                    			   dlgUDiskUpdateFailure = null;
                                    		   }
                                    	   }

                                       }, false);
                                       
                                       dlgUDiskUpdateFailure.show();
                                       DialogUtil.dialogTimeOff(dlgUDiskUpdateFailure , 90000);
                 
                                       return;
                                   case STARTUP_IMG_UPDATE_FAILED:
                                       if (mProgressDlg != null && mProgressDlg.isShowing())
                                       {
                                           mProgressDlg.dismiss();
                                       }
                                      
                                       dlgStartupImgUpdateFailed =DialogUtil.showTipsDialog(mContext, mContext.getString(R.string.tools_dialog_boot_update_failure), mContext.getString(R.string.retry1), mContext.getString(R.string.cancel), new DialogDoubleButtonListener(){

                                    	   @Override
                                    	   public void onLeftClick(Context context , View v , int which) 
                                    	   {
                                    		   updateStartupPic();
                                    		   if(dlgStartupImgUpdateFailed !=null)
                                    		   {
                                    			   dlgStartupImgUpdateFailed.dismiss();
                                    			   dlgStartupImgUpdateFailed = null;
                                    		   }
                                    	   }

                                    	   @Override
                                    	   public void onRightClick(Context context , View v , int which) 
                                    	   {
                                    		   if (PosterOsdActivity.INSTANCE != null)
                                    		   {
                                    			   PosterOsdActivity.INSTANCE.setDismissTime();
                                    		   }
                                    		   if(dlgStartupImgUpdateFailed !=null)
                                    		   {
                                    			   dlgStartupImgUpdateFailed.dismiss();
                                    			   dlgStartupImgUpdateFailed = null;
                                    		   }
                                    	   }
                                    	   
                                       }, false);
                                       
                                       dlgStartupImgUpdateFailed.show();
                                       DialogUtil.dialogTimeOff(dlgStartupImgUpdateFailed,90000);
  
                                       return;
                                   case STARTUP_IMG_UPDATE_SUCCESS:
                                       if (mProgressDlg != null && mProgressDlg.isShowing())
                                       {
                                           mProgressDlg.dismiss();
                                       }
                                       
                                       dlgStartupImgUpdSuc = DialogUtil.showTipsDialog(mContext, mContext.getString(R.string.tools_dialog_boot_update_success), mContext.getString(R.string.enter), new DialogSingleButtonListener(){
                                    	   @Override
                                    	   public void onSingleClick(Context context , View v , int which) {
                                    		   if (PosterOsdActivity.INSTANCE != null)
                                    		   {
                                    			   PosterOsdActivity.INSTANCE.setDismissTime();
                                    		   }
                                    		   
                                    		   if(dlgStartupImgUpdSuc != null)
                                    		   {
                                    			   dlgStartupImgUpdSuc.dismiss();
                                    			   dlgStartupImgUpdSuc = null;
                                    		   }
                                    		   
                                    	   }
                                       	}, false);
                                       
                                       dlgStartupImgUpdSuc.show();
                                       DialogUtil.dialogTimeOff(dlgStartupImgUpdSuc,90000);
                                       return;
                                   case STANDBY_IMG_UPDATE_FAILED:
                                       if (mProgressDlg != null && mProgressDlg.isShowing())
                                       {
                                           mProgressDlg.dismiss();
                                       }
                                       
                                       dlgStandbyImgUpdFailed = DialogUtil.showTipsDialog(mContext, mContext.getString(R.string.tools_dialog_standby_update_failure), mContext.getString(R.string.retry1), mContext.getString(R.string.cancel), new DialogDoubleButtonListener(){

										@Override
										public void onLeftClick(Context context , View v , int which) {
											updateStandbyPic();
											
											if(dlgStandbyImgUpdFailed != null)
											{
												dlgStandbyImgUpdFailed.dismiss();
												dlgStandbyImgUpdFailed = null;
											}
											
										}

										@Override
										public void onRightClick(Context context , View v , int which) {
											 if (PosterOsdActivity.INSTANCE != null){
                                                 PosterOsdActivity.INSTANCE.setDismissTime();
											 }
												if(dlgStandbyImgUpdFailed != null)
												{
													dlgStandbyImgUpdFailed.dismiss();
													dlgStandbyImgUpdFailed = null;
												}
										}}, false);
                                       
                                       dlgStandbyImgUpdFailed.show();
                                       DialogUtil.dialogTimeOff(dlgStandbyImgUpdFailed,90000);
  
                                       return;
                                   case STANDBY_IMG_UPDATE_SUCCESS:
                                       if (mProgressDlg != null && mProgressDlg.isShowing())
                                       {
                                           mProgressDlg.dismiss();
                                       }
                                       
                                       dlgStbImgUpdSucc = DialogUtil.showTipsDialog(mContext, mContext.getString(R.string.tools_dialog_standby_update_success), mContext.getString(R.string.enter), new DialogSingleButtonListener(){

										@Override
										public void onSingleClick(Context context , View v , int which) {
											 if (PosterOsdActivity.INSTANCE != null){
                                                 PosterOsdActivity.INSTANCE.setDismissTime();
											 }
											 
											 if(dlgStbImgUpdSucc != null)
											 {
												 dlgStbImgUpdSucc.dismiss();
												 dlgStbImgUpdSucc = null;
											 }
											 
										}
                                       	}, false);
                                       
                                       dlgStbImgUpdSucc.show();
                                       DialogUtil.dialogTimeOff(dlgStbImgUpdSucc,90000);
                                       return;
                                   case APK_SW_UPDATE_FAILED:
                                       if (mProgressDlg != null && mProgressDlg.isShowing())
                                       {
                                           mProgressDlg.dismiss();
                                       }
                                       dlgApkSwUpdFad = DialogUtil.showTipsDialog(mContext, mContext.getString(R.string.tools_dialog_sw_update_failure), mContext.getString(R.string.retry1), mContext.getString(R.string.cancel), new DialogDoubleButtonListener(){

										@Override
										public void onLeftClick(Context context , View v , int which) {
											updateSW();
											
											if(dlgApkSwUpdFad != null)
											{
												dlgApkSwUpdFad.dismiss();
												dlgApkSwUpdFad = null;
											}
											
										}

										@Override
										public void onRightClick(Context context , View v , int which) {
											 if (PosterOsdActivity.INSTANCE != null){
                                                 PosterOsdActivity.INSTANCE.setDismissTime();
											 }
											 
												if(dlgApkSwUpdFad != null)
												{
													dlgApkSwUpdFad.dismiss();
													dlgApkSwUpdFad = null;
												}
											 
										}}, false);
                                       
                                       dlgApkSwUpdFad.show();
                                       DialogUtil.dialogTimeOff(dlgApkSwUpdFad,90000);

                                       return;
                                   }
                                   
                                   super.handleMessage(msg);
                               }
                           };
}
