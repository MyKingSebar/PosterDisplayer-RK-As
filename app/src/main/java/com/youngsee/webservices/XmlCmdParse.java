/*
 * Copyright (C) 2013 poster PCE YoungSee Inc.
 * All Rights Reserved Proprietary and Confidential.
 * 
 * @author LiLiang-Ping
 */

package com.youngsee.webservices;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.xmlpull.v1.XmlPullParser;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Xml;

import com.youngsee.ftpoperation.FtpFileInfo;
import com.youngsee.ftpoperation.FtpHelper;
import com.youngsee.ftpoperation.FtpOperationInterface;
import com.youngsee.authorization.AuthorizationManager;
import com.youngsee.common.Actions;
import com.youngsee.common.DbHelper;
import com.youngsee.common.FileUtils;
import com.youngsee.common.Md5;
import com.youngsee.common.RuntimeExec;
import com.youngsee.common.SysParamManager;
import com.youngsee.logmanager.LogManager;
import com.youngsee.multicast.MulticastCommon;
import com.youngsee.multicast.MulticastManager;
import com.youngsee.multicast.MulticastSyncInfoRef;
import com.youngsee.osd.OsdSubMenuFragment;
import com.youngsee.posterdisplayer.PosterApplication;
import com.youngsee.power.PowerOnOffManager;
import com.youngsee.screenmanager.ScreenManager;
import com.youngsee.update.APKUpdateManager;

public class XmlCmdParse
{
    private static final String ENCODING = "UTF-8";     // 字节流解析的编码方式

    private XmlCmdParse()
    {
        /*
         * This Class is a single instance mode, and define a private constructor to avoid external use the 'new'
         * keyword to instantiate a objects directly.
         */
    }
    
    private static class XmlCmdParseHolder
    {
        static final XmlCmdParse INSTANCE = new XmlCmdParse();
    }
    
    public synchronized static XmlCmdParse getInstance()
    {
        return XmlCmdParseHolder.INSTANCE;
    }
    
    private boolean isCmdValid(int cmd) {
    	if((cmd != XmlCmdInfoRef.CMD_PTL_DOWNPLAYLIST)
    			&& cmd != XmlCmdInfoRef.CMD_PTL_PLAYLIST) {
    		return true;
    	}
    	return false;
    }
    
    /******************************************************
     * Parsing and execute the command which is received by heartbeat *
     ******************************************************/
    public void parseAndExecuteCmd(String strRspCommand, final Handler mHandler)
    {
        int cmdId = 0;
        int regCode = 0;
        int result = 0;
        String addStr = "";
        String strValue = null;
        try
        {
            // parse command ID
            if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_CMD)) != null)
            {
                cmdId = Integer.parseInt(strValue);
            }
            
            // parse Terminal registration code
            if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_ID)) != null)
            {
                regCode = Integer.parseInt(strValue);
            }
            
            // This machine didn't authorization, can't do any thing.
            if (AuthorizationManager.getInstance().getStatus() != AuthorizationManager.STATUS_AUTHORIZED)
            {
                if (!isCmdValid(cmdId))
                {
                    WsClient.getInstance().postResultBack(cmdId, regCode, -1, "");
                    return;
                }
            }
            
            // parse command content and execute it
            switch (cmdId)
            {
            case XmlCmdInfoRef.CMD_PTL_SETNAME:   // 设置终端名称&终端组
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_CPENAME)) != null)
                {
                    SysParamManager.getInstance().setTerm(strValue);
                }
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_CPEGRP)) != null)
                {
                    SysParamManager.getInstance().setTermGrp(strValue);
                }

                if (PosterApplication.getInstance().getConfiguration().hasEnvironmentMonitor()) {
                	// Update EnvManger param
                	Intent intent = new Intent(Actions.UPDATA_ENVMONITOR_DEVID_ACTION);
                	intent.putExtra("CpuId", PosterApplication.getCpuId().toUpperCase());
                	intent.putExtra("Mac", PosterApplication.getEthMacStr());
                	intent.putExtra("term", SysParamManager.getInstance().getTerm());
                	intent.putExtra("termGroup", SysParamManager.getInstance().getTermGrp());
                	PosterApplication.getInstance().sendStickyBroadcast(intent);
                }

                break;
            
            case XmlCmdInfoRef.CMD_PTL_SETOUT:   // 设置输出参数
                ConcurrentHashMap<String, String> sigOutSet = new ConcurrentHashMap<String, String>();
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_REPRATIO)) != null)
                {
                    sigOutSet.put("repratio", strValue);
                }
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_VIDEOTYPE)) != null)
                {
                    sigOutSet.put("mode", strValue);
                }
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_PIX)) != null)
                {
                    sigOutSet.put("value", strValue);
                }
                SysParamManager.getInstance().setSigOutParam(sigOutSet);
                
                // 生效
                break;
            
            case XmlCmdInfoRef.CMD_PTL_CERTI:
                // Didn't implement
                break;
            
            case XmlCmdInfoRef.CMD_PTL_SETTIME: // 设置系统时间
                /*if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_TIMEZONE)) != null)
                {
                    if (!strValue.equals(sysParam.timeZonevalue)) {
                    	sysParam.timeZonevalue = strValue;
                    	PosterApplication.getInstance().saveSysParamToXML(sysParam);
                    	PosterApplication.getInstance().setTimeZone(strValue);
                    }
                }*/
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_TIME)) != null)
                {
                	strValue = strValue.replaceAll("-", "");
                    strValue = strValue.replaceAll(":", "");
                    strValue = strValue.replaceAll(" ", ".");
                    RuntimeExec.getInstance().runRootCmd("date -s " + strValue);
                    RuntimeExec.getInstance().runRootCmd("clock -w");
                    
                    // 系统校时后重新检测定时开关机
    				PowerOnOffManager.getInstance().checkAndSetOnOffTime(
                    		PowerOnOffManager.AUTOSCREENOFF_IMMEDIATE);
                }
                break;
            
            case XmlCmdInfoRef.CMD_PTL_DOWNPLAYLIST: // download Normal PlayList
            	if (WsClient.getInstance().normalPlayListDownload() != 0)
            	{
                    result = 1;
            	}
            	else
            	{
            		result = 0;
            	}
                break;
            
            case XmlCmdInfoRef.CMD_PTL_PLAYLIST: // download Emergency PlayList
                if (WsClient.getInstance().emergencyPlayListDownload() != 0)
                {
                    result = 1;
            	}
            	else
            	{
            		result = 0;
            	}
                break;
            
            case XmlCmdInfoRef.CMD_PTL_SETPW: // 设置开关机
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_POWER)) != null)
                {
                    switch (Integer.parseInt(strValue))
                    {
                    case XmlCmdInfoRef.CMD_POWER_ON:// 开机
                    	PowerOnOffManager.getInstance().wakeUp();
                        break;
                    case XmlCmdInfoRef.CMD_REBOOT:// 重启
                        RuntimeExec.getInstance().runRootCmd("reboot");
                        break;
                    case XmlCmdInfoRef.CMD_STAND_BY:// 待机
                    	PowerOnOffManager.getInstance().shutDown();
                        break;
                    }
                }
                break;
            
            case XmlCmdInfoRef.CMD_PTL_CPEPRSCRN: // 终端截屏
                int width = 0, height = 0;
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_CPEPRSCRN_WIDTH)) != null)
                {
                    width = Integer.parseInt(strValue);
                }
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_CPEPRSCRN_HEIGHT)) != null)
                {
                    height = Integer.parseInt(strValue);
                }

                if (width > 0 && height > 0)
                {
                	RuntimeExec.getInstance().runRootCmd("rm -f " + PosterApplication.getScreenCaptureImgPath() + "*");
                	
                    StringBuilder sb = new StringBuilder();
                    sb.append(PosterApplication.getScreenCaptureImgPath()).append("capture.png");
                    String strCaptureFilePath = sb.toString();
                    
                    // Execute capture screen shell command
                    sb = new StringBuilder();
                    sb.append("screencap ").append(strCaptureFilePath);
                    RuntimeExec.getInstance().runRootCmd(sb.toString());
                    
                    sb = new StringBuilder();
                    sb.append(PosterApplication.getScreenCaptureImgPath());
                    sb.append(PosterApplication.getEthMacStr()).append(".jpg");
                    String strDestFilePath = sb.toString();
                    
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(strCaptureFilePath, opts);
                    opts.inSampleSize = 1;
                    opts.inJustDecodeBounds = false;
                    Bitmap orgBitmap = BitmapFactory.decodeFile(strCaptureFilePath, opts);
                    Bitmap newBitmap = orgBitmap;
                    int rotation = PosterApplication.getInstance().getHwRotation();
                    if ((rotation != -1) && (rotation != 0)) {
						switch (rotation) {
						case 2:
							rotation = 90;
							break;
						case 4:
							rotation = 180;
							break;
						case 8:
							rotation = 270;
							break;
						default:
							break;
						}
	                    Matrix matrix = new Matrix();
	                	matrix.setRotate((-1) * rotation);
	                	newBitmap = Bitmap.createBitmap(orgBitmap, 0, 0, orgBitmap.getWidth(), orgBitmap.getHeight(), matrix, true);
                    }
                    Bitmap bitmap = PosterApplication.getInstance().combineScreenCap(newBitmap);
                    PosterApplication.resizeImage(bitmap, strDestFilePath, width, height);
                    
                    // 启动FTP上传文件列表
                    FtpFileInfo toUploadFile = new FtpFileInfo();
                    toUploadFile.setRemotePath("/screens");
                    toUploadFile.setLocalPath(strDestFilePath);
                    List<FtpFileInfo> toUploadList = new ArrayList<FtpFileInfo>();
                    toUploadList.add(toUploadFile);
                    final int nReg = regCode;
                    FtpHelper.getInstance().uploadFileList(toUploadList, new FtpOperationInterface() {
                        @Override
                        public void started(String file, long size)
                        {
                        }

                        @Override
                        public void aborted()
                        {
                            WsClient.getInstance().postResultBack(XmlCmdInfoRef.CMD_PTL_CPEPRSCRN, nReg, 1, "");
                        }

                        @Override
                        public void progress(long length)
                        {
                        }

                        @Override
                        public void completed()
                        {
                            StringBuilder sbr = new StringBuilder();
                            sbr.append("<FILE>/screens/");
                            sbr.append(PosterApplication.getEthMacStr());
                            sbr.append(".jpg</FILE>");        
                            WsClient.getInstance().postResultBack(XmlCmdInfoRef.CMD_PTL_CPEPRSCRN, nReg, 0, sbr.toString());
                        }

                        @Override
                        public void failed()
                        {
                            WsClient.getInstance().postResultBack(XmlCmdInfoRef.CMD_PTL_CPEPRSCRN, nReg, 1, "");
                        } 
                    });
                }
                
                //LogManager.getInstance().uploadLog(LogManager.LOGTYPE_NORMAL, 0);

                break;
                
            case XmlCmdInfoRef.CMD_PTL_SETBRI:  // 设置屏幕亮度
                // 准备参数
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_BRI)) != null)
                {
                    PosterApplication.setScreenBright(Integer.parseInt(strValue));
                }
                break;
                
            case XmlCmdInfoRef.CMD_PTL_SETVOL:  // 设置音量
                // 准备参数
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_VOL)) != null)
                {
                    PosterApplication.getInstance().setDeviceVol(Integer.parseInt(strValue));
                }
                break;
                
            case XmlCmdInfoRef.CMD_PTL_FILEDEL:  // 清空节目文件

                FileUtils.delDir(PosterApplication.getProgramPath());
                ScreenManager.getInstance().osdNotify(ScreenManager.CLEAR_PGM_OPERATE);
                break;
                
            case XmlCmdInfoRef.CMD_PTL_FILELIST:  // 获取文件列表
            {
                String programPath = PosterApplication.getProgramPath();
                String str = FileUtils.getFileListString(programPath, true);
                
                StringBuilder sb  = new StringBuilder();
                sb.append("<Items><![CDATA[[").append(str).append("]]]></Items>");
                addStr = sb.toString();
            }
                break;
                
            case XmlCmdInfoRef.CMD_PTL_PLAYFILE:   // 插播文字
            {
            	String remoteFileName = null;
                String localFilePath = null;
                String localFileName = null;
                String playSpeed = null;
                String fontName = null;
                String fontColor = null;
                String duration = null;
                String number = null;
                String verifyKey = null;
                String verifyCode = null;
                
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_FILE)) != null)
                {
                	remoteFileName = strValue;
                    StringBuilder sbd = new StringBuilder();
                    sbd.append(PosterApplication.getProgramPath());
                    sbd.append(File.separator);
                    sbd.append("text");
                    localFilePath = sbd.toString();
                    sbd.append(File.separator);
                    sbd.append(FileUtils.getFilename(strValue));
                    localFileName = sbd.toString();
                }
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_FILESPECIALMD5)) != null)
                {
                    verifyKey = strValue;
                }
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_VERCODE)) != null)
                {
                    verifyCode = strValue;
                }
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_PLAYSPEED)) != null)
                {
                    playSpeed = strValue;
                }
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_FONTNMAE)) != null)
                {
                    fontName = strValue;
                }
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_FONTCOLOR)) != null)
                {
                    fontColor = strValue;
                }
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_TIME)) != null)
                {
                    duration = strValue;
                }
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_PLAYNUM)) != null)
                {
                    number = strValue;
                }
                
                if (remoteFileName != null &&
                	localFilePath != null && localFileName != null && 
                	playSpeed != null &&/* fontName != null &&*/ 
                	fontColor != null && (duration != null || number != null) &&
                	verifyKey != null && verifyCode != null)
                {
                	final String serLocalFilePath = localFilePath;
                    final String serLocalFileName = localFileName;
                    final String serPlaySpeed = playSpeed;
                    final String serFontName = fontName;
                    final String serFontColor = fontColor;
                    final String serDuration = duration;
                    final String serNumber = number;
                    final String serVerifyKey = verifyKey;
                    final String serVerifyCode = verifyCode;
                    
                    // Build ftp file information
                    FtpFileInfo toDownloadFile = new FtpFileInfo();
                    toDownloadFile.setRemotePath(remoteFileName);
                    toDownloadFile.setLocalPath(serLocalFilePath);
                    List<FtpFileInfo> toDownloadList = new ArrayList<FtpFileInfo>();
                    toDownloadList.add(toDownloadFile);
                    
                    final int finRegCode = regCode;
                    // 启动FTP下载文字
                    FtpHelper.getInstance().downloadFileList(toDownloadList,
                    		new FtpOperationInterface() {
								@Override
								public void started(String file, long size) {
									// TODO Auto-generated method stub
								}

								@Override
								public void aborted() {
									// TODO Auto-generated method stub
									WsClient.getInstance().postResultBack(XmlCmdInfoRef.CMD_PTL_PLAYFILE, finRegCode, 1, "");
								}

								@Override
								public void progress(long length) {
									// TODO Auto-generated method stub
								}

								@Override
								public void completed() {
									// TODO Auto-generated method stub
									if (serVerifyCode.equals(new Md5(PosterApplication.stringHexToInt(
											serVerifyKey)).ComputeFileMd5(serLocalFileName))) {
										Bundle bundle = new Bundle();
		                                bundle.putSerializable("playSpeed", (Serializable)serPlaySpeed);
		                                bundle.putSerializable("fontName", (Serializable)serFontName);
		                                bundle.putSerializable("fontColor", (Serializable)serFontColor);
		                                bundle.putSerializable("duration", (Serializable)serDuration);
		                                bundle.putSerializable("number", (Serializable)serNumber);
		                                bundle.putSerializable("text", (Serializable)
		                                		FileUtils.readTextFile(serLocalFileName));
		                                
		                                // 发送消息
		                                Message message = mHandler.obtainMessage();
		                                message.what = XmlCmdInfoRef.CMD_PTL_PLAYFILE;
		                                message.setData(bundle);
		                                mHandler.sendMessage(message);
		                                WsClient.getInstance().postResultBack(XmlCmdInfoRef.CMD_PTL_PLAYFILE, finRegCode, 0, "");
									}
								}

								@Override
								public void failed() {
									// TODO Auto-generated method stub
									WsClient.getInstance().postResultBack(XmlCmdInfoRef.CMD_PTL_PLAYFILE, finRegCode, 1, "");
								}
                    });
                }
                else
                {
                    WsClient.getInstance().postResultBack(cmdId, regCode, 1, addStr);
                }
            }
                break;
                
            case XmlCmdInfoRef.CMD_PTL_CPESYSLOGFTPUP:// 上传系统日志
                String localpath = PosterApplication.getLogFileFullPath(1, 0);
                if (localpath == null || !FileUtils.isExist(localpath))
                {
                    WsClient.getInstance().postResultBack(XmlCmdInfoRef.CMD_PTL_CPESYSLOGFTPUP, regCode, 1, "");
                }
                else
                {
                    // 启动FTP上传文件列表
                    FtpFileInfo upldFile = new FtpFileInfo();
                    upldFile.setRemotePath("/logs");
                    upldFile.setLocalPath(localpath);
                    List<FtpFileInfo> uploadlist = new ArrayList<FtpFileInfo>();
                    uploadlist.add(upldFile);
                    final int nRegLog = regCode;
                    FtpHelper.getInstance().uploadFileList(uploadlist, new FtpOperationInterface() {
                        @Override
                        public void started(String file, long size)
                        {
                        }

                        @Override
                        public void aborted()
                        {
                            WsClient.getInstance().postResultBack(XmlCmdInfoRef.CMD_PTL_CPESYSLOGFTPUP, nRegLog, 1, "");
                        }

                        @Override
                        public void progress(long length)
                        {
                        }

                        @Override
                        public void completed()
                        {
                            String slogname = PosterApplication.getLogFileFullPath(1, 0);
                            StringBuilder sbr = new StringBuilder();
                            sbr.append("<FILE>/logs/");
                            sbr.append(slogname);
                            sbr.append("</FILE><VERCODE>0</VERCODE><SIZE>");
                            sbr.append(FileUtils.getFileLength(slogname));
                            sbr.append("</SIZE><TYPE>1</TYPE>");
                            WsClient.getInstance().postResultBack(XmlCmdInfoRef.CMD_PTL_CPESYSLOGFTPUP, nRegLog, 0, sbr.toString());
                        }

                        @Override
                        public void failed()
                        {
                            WsClient.getInstance().postResultBack(XmlCmdInfoRef.CMD_PTL_CPESYSLOGFTPUP, nRegLog, 1, "");
                        } 
                    });
                }
                
                LogManager.getInstance().uploadLog(LogManager.LOGTYPE_NORMAL, 0);
                
                break;
                
            case XmlCmdInfoRef.CMD_PTL_CPEPLAYLOGFTPUP:// 上传播放日志
                String plocalpath = PosterApplication.getLogFileFullPath(0, 0);
                if (plocalpath == null || !FileUtils.isExist(plocalpath))
                {
                    WsClient.getInstance().postResultBack(XmlCmdInfoRef.CMD_PTL_CPEPLAYLOGFTPUP, regCode, 1, "");
                }
                else
                {
                    // 启动FTP上传文件列表
                    FtpFileInfo upldFile = new FtpFileInfo();
                    upldFile.setRemotePath("/logs");
                    upldFile.setLocalPath(plocalpath);
                    List<FtpFileInfo> uploadlist = new ArrayList<FtpFileInfo>();
                    uploadlist.add(upldFile);
                    final int nRegPLog = regCode;
                    FtpHelper.getInstance().uploadFileList(uploadlist, new FtpOperationInterface() {
                        @Override
                        public void started(String file, long size)
                        {
                        }

                        @Override
                        public void aborted()
                        {
                            WsClient.getInstance().postResultBack(XmlCmdInfoRef.CMD_PTL_CPEPLAYLOGFTPUP, nRegPLog, 1, "");
                        }

                        @Override
                        public void progress(long length)
                        {
                        }

                        @Override
                        public void completed()
                        {
                       	 String slogname = PosterApplication.getLogFileFullPath(0, 0);
                         String slogname2 = PosterApplication.getLogFileFullPath2(0, 0);
                         StringBuilder sbr = new StringBuilder();
                         sbr.append("<FILE>/logs/");
                         sbr.append(slogname2);
                         sbr.append("</FILE><VERCODE>0</VERCODE><SIZE>");
                         sbr.append(FileUtils.getFileLength(slogname));
                         sbr.append("</SIZE><TYPE>2</TYPE>");
//                         WsClient.getInstance().postResultBack(30, nRegPLog, 0, sbr.toString());
                         WsClient.getInstance().postResultBack(30, 0, 0, sbr.toString());
                        }

                        @Override
                        public void failed()
                        {
                            WsClient.getInstance().postResultBack(XmlCmdInfoRef.CMD_PTL_CPEPLAYLOGFTPUP, nRegPLog, 1, "");
                        } 
                    });
                }
                break;
                
            case XmlCmdInfoRef.CMD_PTL_SYSUPDATE:  // 自动更新
            	int update = -1;
            	String file = null;
                String verifyCode = null;
                final int nSysUpdateRegCode = regCode;
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_UPDATE)) != null)
                {
                    update = Integer.parseInt(strValue);
                }
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_FILE)) != null)
                {
                    file = strValue;
                }
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_VERCODE)) != null)
                {
                    verifyCode = strValue;
                }
                if ((update != -1) && (file != null) && (verifyCode != null))
                {
                    if ((update == 2) && file.contains("DMA200_APP"))  // APK更新
                    {
                        Bundle bundle = new Bundle();
                        bundle.putString("apkXmlFile", file);
                        bundle.putString("verifyCode", verifyCode);
                        bundle.putInt("regCode", regCode);
                        Message message = mHandler.obtainMessage();
                        message.what = XmlCmdInfoRef.CMD_PTL_SYSUPDATE;
                        message.setData(bundle);
                        mHandler.sendMessage(message);
                        break;
                    }
                    else if (update == 3) // 开机画面更新
                    {
                        // TBD
                    	WsClient.getInstance().postResultBack(XmlCmdInfoRef.CMD_PTL_SYSUPDATE, nSysUpdateRegCode, 1, "");
                    }
                    else if (update == 4)  // 待机画面更新
                    {
                        String imgPath = "/0/upload/background.jpg";
                        StringBuilder sb = new StringBuilder();
                        sb.append(PosterApplication.getTempFolderPath());
                        sb.append(FileUtils.getFilename(imgPath));
                        if (FileUtils.isExist(sb.toString()))
                        {
                            FileUtils.delFile(new File(sb.toString()));
                        }
                        
                        // Build Ftp file information
                        FtpFileInfo toDownloadFile = new FtpFileInfo();
                        toDownloadFile.setRemotePath(imgPath);
                        toDownloadFile.setLocalPath(PosterApplication.getTempFolderPath());
                        List<FtpFileInfo> toDownloadList = new ArrayList<FtpFileInfo>();
                        toDownloadList.add(toDownloadFile);
                        
                        // 启动FTP下载图片
                        FtpHelper.getInstance().downloadFileList(toDownloadList, new FtpOperationInterface()
                        {
                            @Override
                            public void started(String file, long size)
                            {
                                // TODO Auto-generated method stub
                                
                            }

                            @Override
                            public void aborted()
                            {
                                // TODO Auto-generated method stub
                                
                            }

                            @Override
                            public void progress(long length)
                            {
                                // TODO Auto-generated method stub
                                
                            }

                            @Override
                            public void completed()
                            {
                                StringBuilder sb = new StringBuilder();
                                sb.append(PosterApplication.getTempFolderPath());
                                sb.append(FileUtils.getFilename("background.jpg"));
                                FileUtils.delFile(new File(PosterApplication.getStandbyScreenImgPath()));
                                try
                                {
                                    FileUtils.moveFileTo(
                                            new File(sb.toString()), new File(PosterApplication.getStandbyScreenImgPath()));
                                    WsClient.getInstance().postResultBack(XmlCmdInfoRef.CMD_PTL_SYSUPDATE, nSysUpdateRegCode, 0, "");
                                }
                                catch (IOException e)
                                {
                                    e.printStackTrace();
                                    WsClient.getInstance().postResultBack(XmlCmdInfoRef.CMD_PTL_SYSUPDATE, nSysUpdateRegCode, 1, "");
                                }
                            }

                            @Override
                            public void failed()
                            {
                                
                            }
                        });
                    }
                    else
                    {
                        WsClient.getInstance().postResultBack(XmlCmdInfoRef.CMD_PTL_SYSUPDATE, nSysUpdateRegCode, 1, "");
                    }
                }
                break;
                
            case XmlCmdInfoRef.CMD_PTL_SETOSD:  // 设置OSD参数(语言&密码)
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.LANG)) != null)
                {
                    int osdLang = Integer.parseInt(strValue);
                    if (osdLang != SysParamManager.getInstance().getOsdLang())
                    {
                        SysParamManager.getInstance().setOsdLang(osdLang);
                        PosterApplication.getInstance().initLanguage();
                    }
                }
                
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.PWD)) != null)
                {
                    if (!strValue.equals(SysParamManager.getInstance().getSysPasswd()))
                    {
                        SysParamManager.getInstance().setSysPasswd(strValue);
                    }
                }
                break;
                
            case XmlCmdInfoRef.CMD_PTL_FILEDELT:   // 设置自动删除文件周期
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_FILETIME)) != null)
                {
                    int periodtime = Integer.parseInt(strValue);
                    if (periodtime != SysParamManager.getInstance().getDelFilePeriodTime())
                    {
                        SysParamManager.getInstance().setDelFilePeriodTime(periodtime);
                        PosterApplication.getInstance().startTimingDel();
                    }
                }
                break;
                
            case XmlCmdInfoRef.CMD_PTL_SETCYCTIME:  // 设置心跳间隔
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_TIME)) != null)
                {
                    int cycleTime = Integer.parseInt(strValue);
                    if (cycleTime != SysParamManager.getInstance().getCycleTime())
                    {
                        SysParamManager.getInstance().setCycleTime(cycleTime);
                    }
                }
                
                LogManager.getInstance().uploadLog(LogManager.LOGTYPE_PERIOD, 60*1000);
                
                break;
                
            case XmlCmdInfoRef.CMD_PTL_FORBIDDOWNLOADTIME:// 禁止下载时间段
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_GRPNUM)) != null)
                {
                    ConcurrentHashMap<String, String> offdlTime = new ConcurrentHashMap<String, String>();
                    int num = Integer.parseInt(strValue);
                    offdlTime.put("group", strValue);
                    for (int i = 1; i < num + 1; i++)
                    {
                        offdlTime.put("on_time" + i, GetValueFromTag(strRspCommand, (XmlCmdInfoRef.CMD_KEYWORDS_ON + i)));
                        offdlTime.put("off_time" + i, GetValueFromTag(strRspCommand, (XmlCmdInfoRef.CMD_KEYWORDS_OFF + i)));
                        offdlTime.put("week" + i, GetValueFromTag(strRspCommand, (XmlCmdInfoRef.CMD_KEYWORDS_WEEK + i)));
                    }
                    SysParamManager.getInstance().setOffDlTimeParam(offdlTime);
                }
                break;
                
            case XmlCmdInfoRef.CMD_PTL_SETONOFF: // 设置开关机时间
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_GRPNUM)) != null)
                {
                    ConcurrentHashMap<String, String> onOffTime = new ConcurrentHashMap<String, String>();
                    int num = Integer.parseInt(strValue);
                    onOffTime.put("group", strValue);
                    for (int i = 1; i < num + 1; i++)
                    {
                        onOffTime.put("on_time" + i, GetValueFromTag(strRspCommand, (XmlCmdInfoRef.CMD_KEYWORDS_ON + i)));
                        onOffTime.put("off_time" + i, GetValueFromTag(strRspCommand, (XmlCmdInfoRef.CMD_KEYWORDS_OFF + i)));
                        onOffTime.put("week" + i, GetValueFromTag(strRspCommand, (XmlCmdInfoRef.CMD_KEYWORDS_WEEK + i)));
                    }
                    SysParamManager.getInstance().setOnOffTimeParam(onOffTime);
                    

                    if (OsdSubMenuFragment.INSTANCE != null) {
                    	OsdSubMenuFragment.INSTANCE.reflashOnOffTime();
                    }
                    // 生效
                    PowerOnOffManager.getInstance().checkAndSetOnOffTime(
                    		PowerOnOffManager.AUTOSCREENOFF_IMMEDIATE);
                }
                break;
                
            case XmlCmdInfoRef.CMD_PTL_SETBNET:// 设置网络链接方式
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_MODE)) != null)
                {
                    if (strValue.equals("POPOE"))
                    {
                        break;
                    }
                    
                    ConcurrentHashMap<String, String> netConn = new ConcurrentHashMap<String, String>();
                    netConn.put("mode", strValue);
                    if (strValue.equals("DHCP"))
                    {
                        if (GetValueFromTag(strRspCommand, (XmlCmdInfoRef.CMD_KEYWORDS_IP)) == null)
                        {
                            netConn.put("ip", "0.0.0.0");
                        }
                        else
                        {
                            netConn.put("ip", GetValueFromTag(strRspCommand, (XmlCmdInfoRef.CMD_KEYWORDS_IP)));
                        }
                    }
                    else if (strValue.equals("StaticIP"))
                    {
                        netConn.put("ip", GetValueFromTag(strRspCommand, (XmlCmdInfoRef.CMD_KEYWORDS_IP)));
                        netConn.put("mask", GetValueFromTag(strRspCommand, (XmlCmdInfoRef.CMD_KEYWORDS_MASK)));
                        netConn.put("gateway", GetValueFromTag(strRspCommand, (XmlCmdInfoRef.CMD_KEYWORDS_GATEWAY)));
                        netConn.put("dns1", GetValueFromTag(strRspCommand, (XmlCmdInfoRef.CMD_KEYWORDS_DNS1)));
                        netConn.put("dns2", GetValueFromTag(strRspCommand, (XmlCmdInfoRef.CMD_KEYWORDS_DNS2)));
                    }
                    else if (strValue.equals("3G"))
                    {
                        netConn.put("module", GetValueFromTag(strRspCommand, (XmlCmdInfoRef.CMD_KEYWORDS_3GMODULE)));
                        netConn.put("time", GetValueFromTag(strRspCommand, (XmlCmdInfoRef.CMD_KEYWORDS_3GSPACETIME)));
                    }
                    
                    SysParamManager.getInstance().setNetConnParam(netConn);
                    
                    // 生效
                }

                break;
                
            case XmlCmdInfoRef.CMD_PTL_SETSERVER:// 设置服务器信息
                ConcurrentHashMap<String, String> serverSet = new ConcurrentHashMap<String, String>();
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_WEBURL)) != null)
                {
                    serverSet.put("weburl", strValue);
                }
                else
                {
                    serverSet.put("weburl", "");
                }
                
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_FTPIP)) != null)
                {
                    serverSet.put("ftpip", strValue);
                }
                else
                {
                    serverSet.put("ftpip", "");
                }
                
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_FTPPORT)) != null)
                {
                    serverSet.put("ftpport", strValue);
                }
                else
                {
                    serverSet.put("ftpport", "");
                }
                
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_FTPNAME)) != null)
                {
                    serverSet.put("ftpname", strValue);
                }
                else
                {
                    serverSet.put("ftpname", "");
                }
                
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_FTPPW)) != null)
                {
                    serverSet.put("ftppasswd", strValue);
                }
                else
                {
                    serverSet.put("ftppasswd", "");
                }
                SysParamManager.getInstance().setServerParam(serverSet);
                break;
            
            case XmlCmdInfoRef.CMD_PTL_SYSHD: // 磁盘格式化
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_PROTYPE)) != null)
                {
                    if (strValue.equals("4"))
                    {
                        if (!FileUtils.formatDisk())
                        {
                            result = 1;
                        }
                        ScreenManager.getInstance().osdNotify(ScreenManager.CLEAR_PGM_OPERATE);
                    }
                }
                break;
                
            case XmlCmdInfoRef.CMD_PTL_SETDLRATE:// 设置下载速度
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_DLRATE)) != null)
                {
                    SysParamManager.getInstance().setDwnLdrSpd(Integer.parseInt(strValue));
                }
                break;
                
            case XmlCmdInfoRef.CMD_PTL_AUTOUPGRADE:// 设置是否自动升级
                if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_AOTUUPGRADE)) != null)
                {
                	int autoUpgrade = Integer.parseInt(strValue);
                	if ((autoUpgrade == 0) || (autoUpgrade == 1)) {
                	    SysParamManager.getInstance().setAutoUpgrade(autoUpgrade);
	                    if (autoUpgrade == 1) {
	                    	APKUpdateManager.getInstance().startAutoDetector();
	                    } else {
	                    	APKUpdateManager.getInstance().stopAutoDetector();
	                    }
                	}
                }
                break;
                
            case XmlCmdInfoRef.CMD_PTL_AUTHKEYUPDATE:
                AuthorizationManager.getInstance().updateKey(regCode);
                break;
                
            case XmlCmdInfoRef.CMD_PTL_MULTICASTMANAGER: // 同步播放
            	if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_MULTICAST_PROGSYNC)) != null)
                {
                    int port = 0;
                    int localPort = 0;
                    int followed = 0;
                    int progsync = Integer.parseInt(strValue);
                    
                    // 获取服务器下发的同步参数
                    if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_MULTICAST_BCASTPORT)) != null)
                    {
                    	port = Integer.parseInt(strValue);
                    }
                    if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_MULTICAST_BLOCALPORT)) != null)
                    {
                    	localPort = Integer.parseInt(strValue);
                    }
                    if ((strValue = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_MULTICAST_FOLLOWDELT)) != null)
                    {
                    	followed = Integer.parseInt(strValue);
                    }
                    String strBcastIp = GetValueFromTag(strRspCommand, XmlCmdInfoRef.CMD_KEYWORDS_MULTICAST_BCASTIP);
                    
                    // 保存同步参数
                    DbHelper.getInstance().saveBcastParamToDB(progsync, port, localPort, followed, strBcastIp);
                    
                    // 获取当前使用的同步参数
                    int currentProgsync = MulticastManager.getInstance().getCurrentSynIdicate();
                    int currentLocalPort = MulticastManager.getInstance().getCurrentLocalPort();
                    int currentPort = MulticastManager.getInstance().getCurrentPort();
                    String currentIp = MulticastManager.getInstance().getCurrentGroupIp();
                    
                    if (progsync != MulticastCommon.MC_VALUE_PROGSYNC_CLOSE && 
                    	currentProgsync == MulticastCommon.MC_VALUE_PROGSYNC_CLOSE)
                    {
                        // 启动同步
                    	MulticastManager.getInstance().startWork();
                    }
                    else if (progsync == MulticastCommon.MC_VALUE_PROGSYNC_CLOSE && 
                    		 currentProgsync != MulticastCommon.MC_VALUE_PROGSYNC_CLOSE)
                    {
                        // 关闭同步
                    	if (currentProgsync == MulticastCommon.MC_VALUE_PROGSYNC_OPEN_GROUP_LEADER)
                    	{
                    	    MulticastSyncInfoRef syncInfo = new MulticastSyncInfoRef();
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
                    	MulticastManager.getInstance().stopWork();
                    }
                    else if (progsync != MulticastCommon.MC_VALUE_PROGSYNC_CLOSE &&
                    		 currentProgsync != MulticastCommon.MC_VALUE_PROGSYNC_CLOSE &&
                    		 progsync != currentProgsync)
                    {
                    	// 改变组长或组员的身份
                    	MulticastManager.getInstance().restartWork();
                    }
                    else if(port != currentPort || localPort != currentLocalPort || 
                            (!TextUtils.isEmpty(strBcastIp) && !strBcastIp.equals(currentIp)))
                    {
                    	// 改变同步参数
                    	MulticastManager.getInstance().restartWork();
                    }
                }
                break;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            // Send Result to Server
            if (cmdId != XmlCmdInfoRef.CMD_PTL_NOCMD && 
                cmdId != XmlCmdInfoRef.CMD_PTL_CERTI && 
                cmdId != XmlCmdInfoRef.CMD_PTL_CYC && 
                cmdId != XmlCmdInfoRef.CMD_PTL_PLAYTASK && 
                cmdId != XmlCmdInfoRef.CMD_PTL_PLAYPLATE && 
                cmdId != XmlCmdInfoRef.CMD_PTL_NULL && 
                cmdId != XmlCmdInfoRef.CMD_PTL_SYNCSYSDATA &&
                cmdId != XmlCmdInfoRef.CMD_PTL_PLAYFILE &&
                cmdId != XmlCmdInfoRef.CMD_PTL_CPEPRSCRN &&
                cmdId != XmlCmdInfoRef.CMD_PTL_SYSUPDATE &&
                cmdId != XmlCmdInfoRef.CMD_PTL_CPEPLAYLOGFTPUP &&
                cmdId != XmlCmdInfoRef.CMD_PTL_CPESYSLOGFTPUP &&
                cmdId != XmlCmdInfoRef.CMD_PTL_AUTHKEYUPDATE)
            {
                WsClient.getInstance().postResultBack(cmdId, regCode, result, addStr);
            }
        }
    }
    
    /****************************************************
     * General Decode the XML file interface, and find * the value from command tag. *
     ****************************************************/
    private String GetValueFromTag(String strCommand, String strCmdTag)
    {
        InputStream xmlInput = new ByteArrayInputStream(strCommand.getBytes());
        
        try
        {
            XmlPullParser parser = Xml.newPullParser(); // 由android.util.Xml创建一个XmlPullParser实例
            parser.setInput(xmlInput, ENCODING); // 设置输入流 并指明编码方式
            
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT)
            {
                if (eventType == XmlPullParser.START_TAG && 
                	parser.getName().equals(strCmdTag) && 
                	parser.next() == XmlPullParser.TEXT)
                {
                    return parser.getText();
                }
                
                eventType = parser.next();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            // Close input stream
            if (xmlInput != null)
            {
                try
                {
                    xmlInput.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        
        return null;
    }
}
