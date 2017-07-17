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
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.text.DateFormat;
import java.net.UnknownHostException;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.KeepAliveHttpTransportSE;
import org.xmlpull.v1.XmlSerializer;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Xml;

import com.youngsee.common.Actions;
import com.youngsee.common.Contants;
import com.youngsee.common.FileUtils;
import com.youngsee.common.Md5;
import com.youngsee.common.RuntimeExec;
import com.youngsee.common.SysParamManager;
import com.youngsee.ftpoperation.FtpHelper;
import com.youngsee.logmanager.LogUtils;
import com.youngsee.logmanager.Logger;
import com.youngsee.osd.OsdSubMenuFragment;
import com.youngsee.posterdisplayer.PosterApplication;
import com.youngsee.posterdisplayer.PosterMainActivity;
import com.youngsee.power.PowerOnOffManager;
import com.youngsee.screenmanager.Playbills;
import com.youngsee.screenmanager.Programs;
import com.youngsee.screenmanager.ScheduleLists;
import com.youngsee.screenmanager.Schedules;
import com.youngsee.screenmanager.ScreenManager;
import com.youngsee.update.APKUpdateManager;


/******************************************************************
 * This is a client for communication with web server through    ** 
 * SOAP protocol, it is a single instance mode(that's to say the ** 
 * terminal can only have one client communication with server), ** 
 * and multi-thread safe access. ** 
 * CPE: Customer Premises Equipment (i.e. Client) ** 
 * ACS: Auto Configure Server (i.e. Web Server) **
 ******************************************************************/
@SuppressLint("DefaultLocale")
public class WsClient
{
    private final int           SOAP_SESSION_TIMEOUT                     = 30 * 1000;
    private final int           HEARTBEAT_MAX_FAIL_TIMES                 = 3;
    private static WsClient     mWsClientInstance                        = null;
    private Context             mContext                                 = null;
    private final String        NAME_SPACE                               = "http://dare-tech.com/";
    public static final String  SERVICE_URL_SUFFIX                       = "/services/Heart.asmx";
    
    // Define action for SOAP
    private static final String WS_SOAP_ACTION_AUTHENTICATION            = "Authentication";
    private static final String WS_SOAP_ACTION_HEARTCOMM                 = "HeartComm";
    private static final String WS_SOAP_ACTION_PLAYLISTTASKDOWN          = "PlayListTaskDown";
    private static final String WS_SOAP_ACTION_TEMPLATELDOWN             = "TemplatelDown";
    private static final String WS_SOAP_ACTION_RESULTBACK                = "ResultBack";
    private static final String WS_SOAP_ACTION_EMERGENCYPLAYLISTTASKDOWN = "EmergencyPlayListTaskDown";
    // private static final String WS_SOAP_ACTION_GETTERMINALFILELIST =
    // "GetTerminalFileList";
    
    // Define the Property name of Authentication
    private static final String WS_AUTHENTICATION_MAC                    = "Mac";
    private static final String WS_AUTHENTICATION_PRODUCTVER             = "productver";
    private static final String WS_AUTHENTICATION_SWVER                  = "swVer";
    private static final String WS_AUTHENTICATION_HWVER                  = "hwVer";
    
    // Define the Property name of HeartComm
    private static final String WS_HEARTCOMM_MAC                         = "Mac";
    private static final String WS_HEARTCOMM_CERNUM                      = "AuthenticationCode";
    private static final String WS_HEARTCOMM_STATUS                      = "OnOffStatus";
    private static final String WS_HEARTCOMM_DLINFO                      = "DownFileInfo";
    private static final String WS_HEARTCOMM_ULINFO                      = "UpFileInfo";
    private static final String WS_HEARTCOMM_TEMPLATE                    = "TemplateFn";
    private static final String WS_HEARTCOMM_PLAYFILE                    = "PlayFile";
    
    // Define the Property name of ResultBack
    private static final String WS_RESULTBACK_MAC                        = "Mac";
    private static final String WS_RESULTBACK_CERNUM                     = "AuthenticationCode";
    private static final String WS_RESULTBACK_CMD                        = "CMDstr";
    private static final String WS_RESULTBACK_ID                         = "CMDID";
    private static final String WS_RESULTBACK_INT                        = "ResultInt";
    private static final String WS_RESULTBACK_STR                        = "AddStr";
    
    // Define the Property name of PlayListTaskDown
    private static final String WS_DLPLAYLIST_MAC                        = "Mac";
    private static final String WS_DLPLAYLIST_CERNUM                     = "AuthenticationCode";
    
    // Define the Property name of EmergencyPlayListTaskDown
    private static final String WS_DLEGCYPLAYLIST_MAC                    = "Mac";
    private static final String WS_DLEGCYPLAYLIST_CERNUM                 = "AuthenticationCode";
    
    // Define the Property name of TemplatelDown
    private static final String WS_DLTEMPLATE_MAC                        = "Mac";
    private static final String WS_DLTEMPLATE_CERNUM                     = "AuthenticationCode";
    private static final String WS_DLTEMPLATE_ID                         = "ID";
    
    // Define the Property name of GetTerminalFileList
    // private static final String WS_UPLOADFILE_MAC = "Mac";
    // private static final String WS_UPLOADFILE_CERNUM = "AuthenticationCode";
    
    // Client state
    private final static int    STATE_IDLE                               = 0;
    private final static int    STATE_ONLINE                             = 1;
    
    private ClientFSM           mClientFSMThread                         = null;
    private int                 mState                                   = STATE_IDLE;
    private int                 mHeartbeatFailTimes                      = 0;
    
    private String              mLocalMac                                = null;
    private boolean             mServerConfigChanged                     = false;
    private boolean          mIsRecvPgmCmd = false;

    private WsClient(Context context)
    {
        /*
         * This Class is a single instance mode, and define a private constructor to avoid external use the 'new'
         * keyword to instantiate a objects directly.
         */
        mContext = context;
    }
    
    public static WsClient createInstance(Context context)
    {
        if (mWsClientInstance == null && context != null)
        {
            mWsClientInstance = new WsClient(context);
        }
        return mWsClientInstance;
    }
    
    public synchronized static WsClient getInstance()
    {
        return mWsClientInstance;
    }
    
    public int getClientState()
    {
        return mState;
    }
    
    public void osdChangeServerConfig()
    {
        mServerConfigChanged = true;
    }
    
    /**********************************************
     * Start Net Client *
     **********************************************/
    public void startRun()
    {
        stopRun();
        mClientFSMThread = new ClientFSM("ClientNetFSMThread");
        mClientFSMThread.start();
    }
    
    /**********************************************
     * Stop Net Client *
     **********************************************/
    public void stopRun()
    {
        if (mClientFSMThread != null)
        {
            mClientFSMThread.setRunFlag(false);
            mClientFSMThread.interrupt();
            mClientFSMThread = null;
        }
    }
    
    public boolean isRuning()
    {
        return (mClientFSMThread != null && mClientFSMThread.getRunFlag() == true);
    }
    
    public boolean isOnline()
    {
        return (mState == STATE_ONLINE);
    }
    
    /**
     * 改变冲突mac地址
     */
    private void changeLocalMac(){
		if (!TextUtils.isEmpty(mLocalMac)) {
			StringBuilder sb = new StringBuilder(mLocalMac);
			sb.replace(1, 2, String.valueOf((int) (Math.random() * 10)));
			sb.replace(3, 4, String.valueOf((int) (Math.random() * 10)));
			mLocalMac = sb.toString();

			byte[] mByte = new byte[6];
			for (int i = 0, j = 0; i < mByte.length; i++) {
				j = Integer.valueOf(mLocalMac.substring(i * 2, (i + 1) * 2), 16);
				mByte[i] = (byte) j;
			}
			
			PosterApplication.updateEthMacAddress(mByte);
			Logger.i("change MAC because of mac conflict :"+PosterApplication.getEthFormatMac());
		}
    }
    
    /**********************************************
     * CPE send the authentication request to ACS *
     **********************************************/
    protected boolean authentication()
    {
        // 读取系统参数文件内容
        String strLocalSysParam = SysParamManager.getInstance().getFormatXmlParam();

        // Build the parameter map for SOAP
        HashMap<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put(WS_AUTHENTICATION_MAC, mLocalMac);
        paramMap.put(WS_AUTHENTICATION_PRODUCTVER, strLocalSysParam);
        paramMap.put(WS_AUTHENTICATION_SWVER, "0");
        paramMap.put(WS_AUTHENTICATION_HWVER, "0");
        
        // Create a new soap session, and send the command
        SoapObject responseObj = createSoapSession(WS_SOAP_ACTION_AUTHENTICATION, paramMap);
        if (responseObj == null)
        {
            Logger.e("CPE [Authentication] no response.");
            return false;
        }
        
        // Determine the effectiveness of the response
        String strResponse = responseObj.getProperty(0).toString();
        if (strResponse == null)
        {
            Logger.e("CPE Authentication ERROR[null string]");
            return false;
        }
        else if (strResponse.equals(XmlCmdInfoRef.IS_STR_INVALIDTERMINAL) || strResponse.equals(XmlCmdInfoRef.IS_STR_UNKNOWNTERMINAL) || strResponse.equals(XmlCmdInfoRef.IS_STR_SERVEREXCEPTION)
                || strResponse.equals(XmlCmdInfoRef.IS_STR_AUTHFAIL) || strResponse.equals(XmlCmdInfoRef.IS_STR_UNKOWNCOMMAND) || strResponse.equals(XmlCmdInfoRef.IS_STR_BADCOMMAND))
        {
            Logger.e("CPE Authentication ERROR[Invalid Terminal]");
            return false;
        }
        else if (strResponse.length() < 99)
        {
            Logger.e("CPE Authentication ERROR[response length < 99]");
            return false;
        }

        // Parser the Response
        XmlParser xml = new XmlParser();
        InputStream xmlIs = new ByteArrayInputStream(strResponse.getBytes());
        SysParam retSysParam = (SysParam) xml.getXmlObject(xmlIs, SysParam.class);
        if (retSysParam == null)
        {
            Logger.e("CPE Authentication ERROR[GetSysParamInfo]\n");
            return false;
        }
        
        // 保存参数
        SysParamManager.getInstance().setCertNum(retSysParam.certNumvalue);
        SysParamManager.getInstance().setAutoUpgrade(retSysParam.autoupgradevalue);
        SysParamManager.getInstance().setBrightness(retSysParam.brightnessvalue);
        SysParamManager.getInstance().setCycleTime(retSysParam.cycleTimevalue);
        SysParamManager.getInstance().setDelFilePeriodTime(retSysParam.delFilePeriodtime);
        SysParamManager.getInstance().setDevSel(retSysParam.devSelvalue);
        SysParamManager.getInstance().setDispScale(retSysParam.dispScalevalue);
        SysParamManager.getInstance().setDwnLdrSpd(retSysParam.dwnLdrSpdvalue);
        SysParamManager.getInstance().setTaskPeriodTime(retSysParam.getTaskPeriodtime);
        SysParamManager.getInstance().setOsdLang(retSysParam.osdLangSetosd_lang);
        SysParamManager.getInstance().setPasswd(retSysParam.passwdvalue);
        SysParamManager.getInstance().setRunMode(retSysParam.runmodevalue);
        SysParamManager.getInstance().setScrnRotate(retSysParam.scrnRotatevalue);
        SysParamManager.getInstance().setStatus(retSysParam.setBit);
        SysParamManager.getInstance().setSysPasswd(retSysParam.syspasswdvalue);
        SysParamManager.getInstance().setTermGrp(retSysParam.termGrpvalue);
        SysParamManager.getInstance().setTerm(retSysParam.termname);
        SysParamManager.getInstance().setTermMode(retSysParam.termmodelvalue);
        SysParamManager.getInstance().setTimeZone(retSysParam.timeZonevalue);
        SysParamManager.getInstance().setVolume(retSysParam.volumevalue);
        
        if (retSysParam.netConn != null)
        {
            SysParamManager.getInstance().setNetConnParam(retSysParam.netConn);
        }
        
        if (retSysParam.offdlTime != null)
        {
            SysParamManager.getInstance().setOffDlTimeParam(retSysParam.offdlTime);
        }
        
        if (retSysParam.onOffTime != null)
        {
            SysParamManager.getInstance().setOnOffTimeParam(retSysParam.onOffTime);  
            mHandler.sendEmptyMessage(XmlCmdInfoRef.CMD_PTL_SETONOFF);
        }
        
        if (retSysParam.serverSet != null)
        {
            SysParamManager.getInstance().setServerParam(retSysParam.serverSet);
        }
        
        if (retSysParam.wifiSet != null)
        {
            SysParamManager.getInstance().setWifiParam(retSysParam.wifiSet);
        }
        
        if (retSysParam.sigOutSet != null)
        {
            // 信号输出参数只有重显率有效
            retSysParam.sigOutSet.put("mode", "3");
            retSysParam.sigOutSet.put("value", "10");
            SysParamManager.getInstance().setSigOutParam(retSysParam.sigOutSet);
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

        // 重启设备，参数立即生效
        if (mServerConfigChanged)
        {
        	PosterApplication.getInstance().rebootSystem();
        }
        
        return true;
    }
    
    /**********************************************
     * CPE send the heartbeat request to ACS *
     **********************************************/
    protected boolean heartbeat()
    {
        StringBuilder sb = null;
        
        // 当前播放的节目ID
        String strPLATE = "";
        if (ScreenManager.getInstance() != null)
        {
            strPLATE = ScreenManager.getInstance().getPlayingPgmId();
        }
        
        // 当前播放的Normal Schedule id + Md5
        String localMd5 = "";
        String strSchFilePath = getNormalSchFileName();
        if (FileUtils.isExist(strSchFilePath))
        {
            String strContent = FileUtils.readSDFileData(strSchFilePath);
            localMd5 = getSchListMd5Value(strContent.getBytes());
        }
        sb = new StringBuilder();
        if (ScreenManager.getInstance() != null)
        {
            sb.append(ScreenManager.getInstance().getNormalPlaySchId());
        }
        sb.append(" ");
        sb.append(localMd5);
        String strPLAYLIST = sb.toString();
        
        // 当前播放的Urgent Schedule id + Md5
        localMd5 = "";
        strSchFilePath = getEmgcySchFileName();
        if (FileUtils.isExist(strSchFilePath))
        {
            String strContent = FileUtils.readSDFileData(strSchFilePath);
            localMd5 = getSchListMd5Value(strContent.getBytes());
        }
        sb = new StringBuilder();
        if (ScreenManager.getInstance() != null)
        {
            sb.append(ScreenManager.getInstance().getUrgentPlaySchId());
        }
        sb.append(" ");
        sb.append(localMd5);
        String strPLAYLISTE = sb.toString();
        
        // 设置心跳包状态信息内容
        HashMap<String, Object> statusParamMap = new HashMap<String, Object>();
        statusParamMap.put(XmlCmdInfoRef.CMD_KEYWORDS_IP, PosterApplication.getLocalIpAddress());
        statusParamMap.put(XmlCmdInfoRef.CMD_KEYWORDS_TIME, PosterApplication.getCurrentTime());
        if (PowerOnOffManager.getInstance().getCurrentStatus() !=
        		PowerOnOffManager.STATUS_STANDBY) {
        	statusParamMap.put(XmlCmdInfoRef.CMD_KEYWORDS_POWER, "1");
        } else {
        	statusParamMap.put(XmlCmdInfoRef.CMD_KEYWORDS_POWER, "2");
        }
        statusParamMap.put(XmlCmdInfoRef.CMD_KEYWORDS_DISK, getDiskRatio());
        statusParamMap.put(XmlCmdInfoRef.CMD_KEYWORDS_PLATE, strPLATE);
        statusParamMap.put(XmlCmdInfoRef.CMD_KEYWORDS_PLAYLIST, strPLAYLIST);
        statusParamMap.put(XmlCmdInfoRef.CMD_KEYWORDS_PLAYLISTE, strPLAYLISTE);
        statusParamMap.put(XmlCmdInfoRef.CMD_KEYWORDS_CPUINFO, readCPUUsage());
        statusParamMap.put(XmlCmdInfoRef.CMD_KEYWORDS_MEMINFO, readMemUsage());
        
        if (FtpHelper.getInstance().ftpDownloadIsWorking())
        {
            statusParamMap.put(XmlCmdInfoRef.CMD_KEYWORDS_SOCKETSPEED_DOWM, getRxBytes());
            sb = new StringBuilder();
            sb.append(FtpHelper.getInstance().getDownloadFileName());
            sb.append(";");
            sb.append(FtpHelper.getInstance().getDownloadFileSize());
            sb.append(";");
            sb.append(FtpHelper.getInstance().getDownloadFileCurrentSize());
            statusParamMap.put(XmlCmdInfoRef.CMD_KEYWORDS_DLFILE, sb.toString());
        }
        else
        {
            statusParamMap.put(XmlCmdInfoRef.CMD_KEYWORDS_SOCKETSPEED_DOWM, "0");
        }
        if (FtpHelper.getInstance().ftpUploadIsWorking())
        {
            statusParamMap.put(XmlCmdInfoRef.CMD_KEYWORDS_SOCKETSPEED_UP, getTxBytes());
        }
        else
        {
            statusParamMap.put(XmlCmdInfoRef.CMD_KEYWORDS_SOCKETSPEED_UP, "0");
        }
        String strStatusParams = EncodeCmdPages(statusParamMap);
        
        // Build the parameter map for SOAP
        String certNum = SysParamManager.getInstance().getCertNum();
        HashMap<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put(WS_HEARTCOMM_MAC, mLocalMac);
        paramMap.put(WS_HEARTCOMM_CERNUM, certNum);
        paramMap.put(WS_HEARTCOMM_STATUS, strStatusParams);
        if (FtpHelper.getInstance().ftpDownloadIsWorking())
        {
            paramMap.put(WS_HEARTCOMM_DLINFO, FtpHelper.getInstance().getDownloadFileName());
        }
        else
        {
            paramMap.put(WS_HEARTCOMM_DLINFO, "0");
        }
        if (FtpHelper.getInstance().ftpUploadIsWorking())
        {
            paramMap.put(WS_HEARTCOMM_ULINFO, FtpHelper.getInstance().getUploadFileName());
        }
        else
        {
            paramMap.put(WS_HEARTCOMM_ULINFO, "0");
        }
        paramMap.put(WS_HEARTCOMM_TEMPLATE, "0");
        paramMap.put(WS_HEARTCOMM_PLAYFILE, "0");
        
        // Create a new soap session, and send the command
        // Log.i("merr", paramMap.toString());
        SoapObject responseObj = createSoapSession(WS_SOAP_ACTION_HEARTCOMM, paramMap);
        if (responseObj == null)
        {
            Logger.e("CPE [Heartbeat] no response.");
            return false;
        }
        
        // Determine the effectiveness of the response
        String strResponse = responseObj.getProperty(0).toString();
        if (strResponse == null)
        {
            Logger.e("CPE Heartbeat ERROR[null string]");
            return false;
        }
        else if (strResponse.equals(XmlCmdInfoRef.IS_STR_INVALIDTERMINAL) || strResponse.equals(XmlCmdInfoRef.IS_STR_UNKNOWNTERMINAL) || strResponse.equals(XmlCmdInfoRef.IS_STR_SERVEREXCEPTION)
                || strResponse.equals(XmlCmdInfoRef.IS_STR_AUTHFAIL) || strResponse.equals(XmlCmdInfoRef.IS_STR_UNKOWNCOMMAND) || strResponse.equals(XmlCmdInfoRef.IS_STR_BADCOMMAND))
        {
            Logger.e("CPE Heartbeat ERROR[Invalid Terminal]");
            return false;
        }
        
        // Parser & execute response command
        XmlCmdParse.getInstance().parseAndExecuteCmd(strResponse, mHandler);
        return true;
    }
    
    /**********************************************
     * CPE send the Emergency PlayList Download * request to ACS *
     **********************************************/
    protected int emergencyPlayListDownload()
    {
        // Build the parameter map for SOAP
        String certNum = SysParamManager.getInstance().getCertNum();
        HashMap<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put(WS_DLEGCYPLAYLIST_MAC, mLocalMac);
        paramMap.put(WS_DLEGCYPLAYLIST_CERNUM, certNum);
        
        // Create a new soap session, and send the command
        SoapObject responseObj = createSoapSession(WS_SOAP_ACTION_EMERGENCYPLAYLISTTASKDOWN, paramMap);
        if (responseObj == null)
        {
            Logger.e("CPE [EmergencyPlayListDownload] no response.");
            return 1;
        }
        
        // Determine the effectiveness of the response
        String strResponse = responseObj.getProperty(0).toString();
        if (strResponse == null)
        {
            Logger.e("CPE Emergency PlayList Download ERROR[null string]");
            return 2;
        }
        else if (strResponse.equals(XmlCmdInfoRef.IS_STR_INVALIDTERMINAL) || strResponse.equals(XmlCmdInfoRef.IS_STR_UNKNOWNTERMINAL) || strResponse.equals(XmlCmdInfoRef.IS_STR_SERVEREXCEPTION)
                || strResponse.equals(XmlCmdInfoRef.IS_STR_AUTHFAIL) || strResponse.equals(XmlCmdInfoRef.IS_STR_UNKOWNCOMMAND) || strResponse.equals(XmlCmdInfoRef.IS_STR_BADCOMMAND))
        {
            Logger.e("CPE Emergency PlayList Download ERROR[Invalid Terminal]");
            return 3;
        }
        
        // Check the playList whether is really changed
        String strLocalFilePath = getEmgcySchFileName();
        if (FileUtils.isExist(strLocalFilePath))
        {
            String strContent = FileUtils.readSDFileData(strLocalFilePath);
            String localMd5 = getSchListMd5Value(strContent.getBytes());
            String remoteMd5 = getSchListMd5Value(strResponse.getBytes());
            if (localMd5.equals(remoteMd5))
            {
                Logger.e("Emergency playlist already is newlest, didn't need to update.");
                return 0;
            }
        }
        
        // save playlist content to temp file
        wirteTempFile(PosterApplication.LOCAL_CASTE_FILENAME_T, strResponse);
        
        // parse Schedule List & download template
        XmlParser parser = new XmlParser();
        InputStream xmlIs = new ByteArrayInputStream(strResponse.getBytes());
        ScheduleLists scheduList = (ScheduleLists) parser.getXmlObject(xmlIs, ScheduleLists.class);
        if (scheduList != null && scheduList.Schedule != null)
        {
            // download template
            if (downloadProgramList(scheduList) == false)
            {
            	Logger.e("CPE Emergency Template Download Failed[Invalid Terminal]");
            	delTempFile();
            	return 4;
            }
        }
        
        // move temp file to program path
        if (true == moveTempFile())
        {
            // notify ScreenManager Emergency program file changed.
            ScreenManager.getInstance().mUrgentPgmFileHasChanged = true;
            Logger.i("WSClient: Emergency program File has been changed.");
        }
        else
        {
            delTempFile();
        }
        return 0;
    }
    
    /**********************************************
     * CPE send the Normal PlayList Download * request to ACS *
     **********************************************/
    protected int normalPlayListDownload()
    {
        // Build the parameter map for SOAP
        String certNum = SysParamManager.getInstance().getCertNum();
        HashMap<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put(WS_DLPLAYLIST_MAC, mLocalMac);
        paramMap.put(WS_DLPLAYLIST_CERNUM, certNum);
        
        // Create a new soap session, and send the command
        SoapObject responseObj = createSoapSession(WS_SOAP_ACTION_PLAYLISTTASKDOWN, paramMap);
        if (responseObj == null)
        {
            Logger.e("CPE [normalPlayListDownload] no response.");
            return 1;
        }
        
        // Determine the effectiveness of the response
        String strResponse = responseObj.getProperty(0).toString();
        if (strResponse == null)
        {
            Logger.e("CPE normal PlayList Download ERROR[null string]");
            return 2;
        }
        else if (strResponse.equals(XmlCmdInfoRef.IS_STR_INVALIDTERMINAL) || strResponse.equals(XmlCmdInfoRef.IS_STR_UNKNOWNTERMINAL) || strResponse.equals(XmlCmdInfoRef.IS_STR_SERVEREXCEPTION)
                || strResponse.equals(XmlCmdInfoRef.IS_STR_AUTHFAIL) || strResponse.equals(XmlCmdInfoRef.IS_STR_UNKOWNCOMMAND) || strResponse.equals(XmlCmdInfoRef.IS_STR_BADCOMMAND))
        {
            Logger.e("CPE normal PlayList Download ERROR[Invalid Terminal]");
            return 3;
        }
        
        // Check the playList whether is really changed
        String strLocalFilePath = getNormalSchFileName();
        if (FileUtils.isExist(strLocalFilePath))
        {
            String strContent = FileUtils.readSDFileData(strLocalFilePath);
            String localMd5 = getSchListMd5Value(strContent.getBytes());
            String remoteMd5 = getSchListMd5Value(strResponse.getBytes());
            if (localMd5.equals(remoteMd5))
            {
                Logger.e("Normal playlist already is newlest, didn't need to update.");
                return 0;
            }
        }
        
        // save playlist content to temp file
        wirteTempFile(PosterApplication.LOCAL_CAST_FILENAME_T, strResponse);
        
        // parse Schedule List & download template
        XmlParser parser = new XmlParser();
        InputStream xmlIs = new ByteArrayInputStream(strResponse.getBytes());
        ScheduleLists scheduList = (ScheduleLists) parser.getXmlObject(xmlIs, ScheduleLists.class);
        if (scheduList != null && scheduList.Schedule != null)
        {
            // download template
            if (downloadProgramList(scheduList) == false)
            {
            	Logger.e("CPE Normal Template Download Failed[Invalid Terminal]");
            	delTempFile();
            	return 4;
            }
        }
        
        // move temp file to program path
        if (true == moveTempFile())
        {
            // notify ScreenManager to change normal program
            ScreenManager.getInstance().mNormalPgmFileHasChanged = true;
            Logger.i("WSClient: normal program file has been changed.");
        }
        else
        {
            delTempFile();
        }
        return 0;
    }
    
    /**********************************************
     * CPE send the template request to ACS *
     **********************************************/
    protected boolean templateDownload(String nTemplateId, String fileName, String verifyCode)
    {
        // Build the parameter map for SOAP
        String certNum = SysParamManager.getInstance().getCertNum();
        HashMap<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put(WS_DLTEMPLATE_MAC, mLocalMac);
        paramMap.put(WS_DLTEMPLATE_CERNUM, certNum);
        paramMap.put(WS_DLTEMPLATE_ID, nTemplateId);
        
        // Create a new soap session, and send the command
        SoapObject responseObj = createSoapSession(WS_SOAP_ACTION_TEMPLATELDOWN, paramMap);
        if (responseObj == null)
        {
            Logger.e("CPE [templateDownload] no response.");
            return false;
        }
        
        // Determine the effectiveness of the response
        String strResponse = responseObj.getProperty(0).toString();
        if (strResponse == null)
        {
            Logger.e("CPE template Download ERROR[null string]");
            return false;
        }
        else if (strResponse.equals(XmlCmdInfoRef.IS_STR_INVALIDTERMINAL) || strResponse.equals(XmlCmdInfoRef.IS_STR_UNKNOWNTERMINAL) || strResponse.equals(XmlCmdInfoRef.IS_STR_SERVEREXCEPTION)
                || strResponse.equals(XmlCmdInfoRef.IS_STR_AUTHFAIL) || strResponse.equals(XmlCmdInfoRef.IS_STR_UNKOWNCOMMAND) || strResponse.equals(XmlCmdInfoRef.IS_STR_BADCOMMAND))
        {
            Logger.e("CPE template Download ERROR[Invalid Terminal]");
            return false;
        }
        
        // compare Md5 value
        String md5Value = getProgramMd5Value(strResponse.getBytes());
        if (md5Value.equals(verifyCode))
        {
            wirteTempFile(fileName, strResponse);
        }
        else
        {
            // <PARAM><ID>节目ID</ID><CURR>当前验证号</CURR><EXPECT>期望验证号</EXPECT></PARAM>
            String param = "<PARAM><ID>" + nTemplateId + "</ID><CURR>" + md5Value + "</CURR><EXPECT>" + verifyCode + "</EXPECT></PARAM>";
            LogUtils.getInstance().toAddSLog(Contants.WARN, Contants.ProgramVerifyFail, param);
            Logger.e("playbill Md5 is wrong, verifyCode is: " + verifyCode + " Md5 is: " + md5Value);
        }
        
        return true;
    }
    
    /**********************************************
     * CPE send the ResultBack request to ACS *
     **********************************************/
    public void postResultBack(int nCmdTag, int nCmdId, int nResult, String strAdd)
    {
        // Build the parameter map for SOAP
        String certNum = SysParamManager.getInstance().getCertNum();
        HashMap<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put(WS_RESULTBACK_MAC, mLocalMac);
        paramMap.put(WS_RESULTBACK_CERNUM, certNum);
        paramMap.put(WS_RESULTBACK_CMD, nCmdTag);
        paramMap.put(WS_RESULTBACK_ID, nCmdId);
        paramMap.put(WS_RESULTBACK_INT, nResult);
        paramMap.put(WS_RESULTBACK_STR, strAdd);
        
        // Create a new soap session, and send the command
        SoapObject responseObj = createSoapSession(WS_SOAP_ACTION_RESULTBACK, paramMap);
        if (responseObj == null)
        {
            Logger.e("CPE [ResultBack] no response.");
            return;
        }
        
        // Determine the effectiveness of the response
        String strResponse = responseObj.getProperty(0).toString();
        if (strResponse == null)
        {
            Logger.e("CPE ResultBack ERROR[null string]");
            return;
        }
        else if (strResponse.equals(XmlCmdInfoRef.IS_STR_INVALIDTERMINAL) || strResponse.equals(XmlCmdInfoRef.IS_STR_UNKNOWNTERMINAL) || strResponse.equals(XmlCmdInfoRef.IS_STR_SERVEREXCEPTION)
                || strResponse.equals(XmlCmdInfoRef.IS_STR_AUTHFAIL) || strResponse.equals(XmlCmdInfoRef.IS_STR_UNKOWNCOMMAND) || strResponse.equals(XmlCmdInfoRef.IS_STR_BADCOMMAND))
        {
            Logger.e("CPE ResultBack ERROR[Invalid Terminal]");
            return;
        }
        else
        {
            Logger.i("CPE ResultBack response is:" + strResponse);
        }
    }
    
    /**************************************************
     * download the XML file which is defined program *
     **************************************************/
    private boolean downloadProgramList(ScheduleLists scheduList)
    {
        if (scheduList == null || scheduList.Schedule == null)
        {
            return false;
        }
        
        Schedules schedule = null;
        Playbills playbill = null;
        Programs program = null;
        String strProgramId = null;
        String strProgramName = null;
        String strVerifyCode = null;
        
        // 遍历所有的Schedule
        for (int i = 0; i < scheduList.Schedule.size(); i++)
        {
            schedule = scheduList.Schedule.get(i);
            
            // 有效性验证
            if (schedule == null || schedule.Playbill == null)
            {
                continue;
            }
            
            // 遍历Schedule中所有的playbill
            for (int j = 0; j < schedule.Playbill.size(); j++)
            {
                playbill = schedule.Playbill.get(j);
                
                // 有效性验证
                if (playbill == null || playbill.Program == null)
                {
                    continue;
                }
                
                // 遍历playbill中所有的program, 并启动下载
                for (int k = 0; k < playbill.Program.size(); k++)
                {
                    // 获取program中的信息
                    program = playbill.Program.get(k);
                    
                    // 有效性验证
                    if (program == null || program.Program == null)
                    {
                        continue;
                    }
                    
                    // 下载节目文件
                    strProgramId = program.Program.get("id");
                    strProgramName = program.Program.get("name");
                    strVerifyCode = program.Program.get("verify");
                    if (strProgramId != null && strProgramName != null && strVerifyCode != null)
                    {
                        if (templateDownload(strProgramId, strProgramName, strVerifyCode) == false)
                        {
                        	return false;
                        }
                    }
                }
            }
        }
        
        return true;
    }
    
    /**************************************************
     * Save the temporary XML file *
     **************************************************/
    private void wirteTempFile(String strFileName, String message)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(FileUtils.getHardDiskPath());
        sb.append(File.separator);
        sb.append("tmpDowload");
        String strFilePath = sb.toString();
        
        if (!FileUtils.isExist(strFilePath))
        {
            FileUtils.createDir(strFilePath);
        }
        
        sb.setLength(0);
        sb.append(strFilePath);
        sb.append(File.separator);
        sb.append(strFileName);
        String strFilePathName = sb.toString();
        if (FileUtils.isExist(strFilePathName))
        {
            File file = new File(strFilePathName);
            FileUtils.delFile(file);
        }
        FileUtils.writeSDFileData(strFilePathName, message.getBytes(), false);
    }
    
    private void delTempFile()
    {
    	StringBuilder sb = new StringBuilder();
        sb.append(FileUtils.getHardDiskPath());
        sb.append(File.separator);
        sb.append("tmpDowload");
        
        FileUtils.delDir(sb.toString());
    }
    
    /**************************************************
     * Move the temporary XML file to player path *
     **************************************************/
    private boolean moveTempFile()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(FileUtils.getHardDiskPath());
        sb.append(File.separator);
        sb.append("tmpDowload");
        String strSrcFilePath = sb.toString();
        
        File file = new File(strSrcFilePath);
        File[] files = file.listFiles();
        if (files == null)
        {
            return false;
        }
        
        File srcFile = null;
        File dstFile = null;
        String name = null;
        String strDstFilePath = null;
        synchronized (ScreenManager.getInstance().mProgramFileLock)
        {
            for (int i = 0; i < files.length; i++)
            {
                if (files[i].isFile())
                {
                    String newPath = PosterApplication.getProgramPath();
                    name = files[i].getName();
                    sb.setLength(0);
                    if (name.equals(PosterApplication.LOCAL_CAST_FILENAME_T))
                    {
                        name = PosterApplication.LOCAL_NORMAL_PLAYLIST_FILENAME;
                        sb.append(newPath);
                        sb.append(File.separator);
                        sb.append("playlist");
                    }
                    else if (name.equals(PosterApplication.LOCAL_CASTE_FILENAME_T))
                    {
                        name = PosterApplication.LOCAL_EMGCY_PLAYLIST_FILENAME;
                        sb.append(newPath);
                        sb.append(File.separator);
                        sb.append("playlist");
                    }
                    else
                    {
                        sb.append(newPath);
                        sb.append(File.separator);
                        sb.append("template");
                    }
                    
                    strDstFilePath = sb.toString();
                    if (!FileUtils.isExist(strDstFilePath))
                    {
                        FileUtils.createDir(strDstFilePath);
                    }
                    
                    // Move file
                    if (FileUtils.getFileExtensionName(name).equals("xml"))
                    {
                        sb.setLength(0);
                        sb.append(strSrcFilePath);
                        sb.append(File.separator);
                        sb.append(files[i].getName());
                        srcFile = new File(sb.toString());
                        
                        sb.setLength(0);
                        sb.append(strDstFilePath);
                        sb.append(File.separator);
                        sb.append(name);
                        dstFile = new File(sb.toString());
                        
                        try
                        {
                            FileUtils.moveFileTo(srcFile, dstFile);
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        
        // delete the temp path
        if (FileUtils.isExist(strSrcFilePath))
        {
            FileUtils.delDir(strSrcFilePath);
        }
        
        return true;
    }
    
    /****************************************************
     * General WebService interface request method, * as long as it is with ksoap web side interaction * can make use of
     * this method *
     ****************************************************/
    @SuppressWarnings("rawtypes")
    private SoapObject createSoapSession(String methodName, HashMap<String, Object> paramMap)
    {
        SoapObject retSoapObj = null;
        String serverWebUrl = getServerURL();
        String soapAction = NAME_SPACE + methodName;
        KeepAliveHttpTransportSE transport = null;
        SoapSerializationEnvelope envelope = null;
        
        if (serverWebUrl == null)
        {
            Logger.d("createSoapSession(): server web url is null.");
            return null;
        }
        System.setProperty("http.keepAlive", "false");
        
        try
        {
            // 创建SoapObject对象, 并指定WebService的命名空间和调用的方法名
            SoapObject rpc = new SoapObject(NAME_SPACE, methodName);
            
            // 设置WebService方法的参数
            if (null != paramMap && paramMap.size() > 0)
            {
                Map.Entry entry = null;
                Object key = null;
                Object val = null;
                Iterator<?> iter = paramMap.entrySet().iterator();
                while (iter.hasNext())
                {
                    entry = (Map.Entry) iter.next();
                    key = entry.getKey();
                    val = entry.getValue();
                    rpc.addProperty(key.toString(), val.toString());
                }
            }
            
            // 生成调用WebService方法的SOAP请求信息,并指定SOAP的版本
            envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            
            // 设置Envelope属性
            envelope.dotNet = true; // 支持.Net开发的WebService
            envelope.bodyOut = rpc; // 设置bodyOut属性

            // 创建HttpTransportSE对象，并指定WSDL文档的URL
            transport = new KeepAliveHttpTransportSE(serverWebUrl, SOAP_SESSION_TIMEOUT);
            
            // 调用WebService与服务器通信
            transport.call(soapAction, envelope);
            
            // 获取返回的数据
            retSoapObj = (SoapObject) envelope.bodyIn;
            
            // debug code
            String debugCode=retSoapObj.getProperty(0).toString();
            Logger.d(debugCode);
            if(debugCode!=null)
            {
            	if (debugCode.equals("MacConflict")){
            	    changeLocalMac(); 	
                }
            	else if (debugCode.contains("<CMD>31</CMD>") || debugCode.contains("<CMD>99</CMD>"))
            	{
            		mIsRecvPgmCmd = true;
            	}
            }
        }
        catch (Exception e)
        {
        	Logger.e("createSoapSession() catch a exception, the error message is: " + e.toString());
            e.printStackTrace();
            
        	if (transport != null)
        	{
        		Logger.i("retry to createSoapSession()");
	            try {
					transport.call(soapAction, envelope);
					
					// 获取返回的数据
		            retSoapObj = (SoapObject) envelope.bodyIn;
		            
		            // debug code
		            Logger.d(((SoapObject) envelope.bodyIn).getProperty(0).toString());
				}
	            catch (Exception er)
	            {
					er.printStackTrace();
					retSoapObj = null;
		            Logger.e("retry createSoapSession() catch a exception, the error message is: " + er.toString());
		            er.printStackTrace();
				}
        	}
        }
        finally
        {
        	if (transport != null)
        	{
        		try {
					transport.getServiceConnection().disconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}
        	}
        }
        
        return retSoapObj;
    }
    
    /**********************************************
     * 获取心跳包所需的硬盘使用率 *
     **********************************************/
    private String getDiskRatio()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(FileUtils.getDiskUseSpace());
        sb.append("/");
        sb.append(FileUtils.getDiskSpace());
        return sb.toString();
    }
    
    /**********************************************
     * 获取心跳包所需的CPU利用率 *
     **********************************************/
    private String readCPUUsage()
    {
        RandomAccessFile reader = null;
        try
        {
            // Read CPU usage File
            reader = new RandomAccessFile("/proc/stat", "r");
            String[] tokCPU1 = reader.readLine().split("\\s+");
            reader.close();
            reader = null;
            if (tokCPU1.length < 8)
            {
                return "0%";
            }
            
            // Get the first CPU usage
            long idle1 = Long.parseLong(tokCPU1[4]);
            long cpu1 = Long.parseLong(tokCPU1[1]) + Long.parseLong(tokCPU1[2]) + Long.parseLong(tokCPU1[3]) + Long.parseLong(tokCPU1[4]) + Long.parseLong(tokCPU1[5]) + Long.parseLong(tokCPU1[6]) + Long.parseLong(tokCPU1[7]);
            
            Thread.sleep(360);
            
            // Read CPU usage File
            reader = new RandomAccessFile("/proc/stat", "r");
            String[] tokCPU2 = reader.readLine().split("\\s+");
            reader.close();
            reader = null;
            if (tokCPU2.length < 8)
            {
                return "0%";
            }
            
            // Get the second CPU usage
            long idle2 = Long.parseLong(tokCPU2[4]);
            long cpu2 = Long.parseLong(tokCPU2[1]) + Long.parseLong(tokCPU2[2]) + Long.parseLong(tokCPU2[3]) + Long.parseLong(tokCPU2[4]) + Long.parseLong(tokCPU2[5]) + Long.parseLong(tokCPU2[6]) + Long.parseLong(tokCPU2[7]);
            
            // Calculation of CPU usage
            double dbValue = (double) ((cpu2 - cpu1) - (idle2 - idle1)) / (cpu2 - cpu1) * 100;
            
            // Build string for Server
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%.0f", dbValue));
            sb.append("%");
            return sb.toString();
        }
        catch (Exception e)
        {
        }
        finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        
        return ("0%");
    }
    
    /**********************************************
     * 获取心跳包所需的内存利用率 *
     **********************************************/
    private String readMemUsage()
    {
        RandomAccessFile reader = null;
        try
        {
            reader = new RandomAccessFile("/proc/meminfo", "r");
            String[] totals = reader.readLine().split("\\s+");
            String[] frees = reader.readLine().split("\\s+");
            String[] buffers = reader.readLine().split("\\s+");
            String[] cached = reader.readLine().split("\\s+");
            reader.close();
            reader = null;
            
            if (totals.length < 3 || 
                frees.length < 3 || 
                buffers.length < 3 || 
                cached.length < 3)
            {
                return "0%";
            }

            // get total memory
            long totalMemory = Long.parseLong(totals[1]);
            if (totals[2].toLowerCase().equals("kb"))
            {
                totalMemory *= 1024;
            }
            else if (totals[2].toLowerCase().equals("mb"))
            {
                totalMemory *= 1024 * 1024;
            }
            else if (totals[2].toLowerCase().equals("gb"))
            {
                totalMemory *= 1024 * 1024 * 1024;
            }
            
            // get free memory
            long freeMemory = Long.parseLong(frees[1]);
            if (frees[2].toLowerCase().equals("kb"))
            {
                freeMemory *= 1024;
            }
            else if (frees[2].toLowerCase().equals("mb"))
            {
                freeMemory *= 1024 * 1024;
            }
            else if (frees[2].toLowerCase().equals("gb"))
            {
                freeMemory *= 1024 * 1024 * 1024;
            }

            // get buffers memory
            long bufMemory = Long.parseLong(buffers[1]);
            if (buffers[2].toLowerCase().equals("kb"))
            {
                bufMemory *= 1024;
            }
            else if (buffers[2].toLowerCase().equals("mb"))
            {
                bufMemory *= 1024 * 1024;
            }
            else if (buffers[2].toLowerCase().equals("gb"))
            {
                bufMemory *= 1024 * 1024 * 1024;
            }
            
            // get cached memory
            long cacheMemory = Long.parseLong(cached[1]);
            if (cached[2].toLowerCase().equals("kb"))
            {
                cacheMemory *= 1024;
            }
            else if (cached[2].toLowerCase().equals("mb"))
            {
                cacheMemory *= 1024 * 1024;
            }
            else if (cached[2].toLowerCase().equals("gb"))
            {
                cacheMemory *= 1024 * 1024 * 1024;
            }
            
            // Calculation of memory usage
            double dbValue = (double) (totalMemory - freeMemory - bufMemory - cacheMemory) / totalMemory * 100;
            
            // Build string for Server
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%.0f", dbValue));
            sb.append("%");
            return sb.toString();
        }
        catch (Exception e)
        {
        }
        finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        return ("0%");
    }
    
    /**********************************************
     * 获取心跳包所需的接收字节数 *
     **********************************************/
    private String getRxBytes()
    {
        ApplicationInfo info = PosterApplication.getInstance().getApplicationInfo();
        long rxBytes = TrafficStats.getUidRxBytes(info.uid);
        
        // Build string for Server
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%.0f", (double) (rxBytes / 1024)));
        return sb.toString();
    }
    
    /**********************************************
     * 获取心跳包所需的发送字节数 *
     **********************************************/
    private String getTxBytes()
    {
        ApplicationInfo info = PosterApplication.getInstance().getApplicationInfo();
        long txBytes = TrafficStats.getUidTxBytes(info.uid);
        
        // Build string for Server
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%.0f", (double) (txBytes / 1024)));
        return sb.toString();
    }
    
    /**********************************************
     * 心跳包状态信息内容编码 *
     **********************************************/
    @SuppressWarnings("rawtypes")
    private String EncodeCmdPages(HashMap<String, Object> paramMap)
    {
        StringWriter writer = new StringWriter();
        
        try
        {
            // 初始化
            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(writer);
            
            /** 设置内容 **/
            if (null != paramMap && paramMap.size() > 0)
            {
                Map.Entry entry = null;
                Object key = null;
                Object val = null;
                Iterator<?> iter = paramMap.entrySet().iterator();
                while (iter.hasNext())
                {
                    entry = (Map.Entry) iter.next();
                    key = entry.getKey();
                    val = entry.getValue();
                    
                    serializer.startTag("", key.toString());
                    serializer.text(val.toString());
                    serializer.endTag("", key.toString());
                }
            }
            
            /** 设置结束 **/
            serializer.endDocument();
        }
        catch (IllegalArgumentException e)
        {
            e.printStackTrace();
        }
        catch (IllegalStateException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        
        return writer.toString();
    }
    
    /**********************************************
     * 获取Web URL *
     **********************************************/
    private static String getServerURL()
    {
        ConcurrentHashMap<String, String>  serverSet = SysParamManager.getInstance().getServerParam();
        if (serverSet != null)
        {
        	String weburl = serverSet.get("weburl");
        	if (weburl != null)
        	{
        		return weburl;
        	}
        }
        
        return (PosterApplication.getInstance().getConfiguration().getDefualtServerUrl() + SERVICE_URL_SUFFIX);
    }
    
    public static String getServerURLPrefix()
    {
        String weburl = getServerURL();
        if (weburl != null && weburl.endsWith("asmx") && weburl.length() > WsClient.SERVICE_URL_SUFFIX.length())
        {
            return weburl.substring(0, (weburl.length() - WsClient.SERVICE_URL_SUFFIX.length()));
        }
        return weburl;
    }
    
    private String getSchListMd5Value(byte[] message)
    {
        int md5Key = 0x10325476;
        String md5Value = new Md5(md5Key).ComputeMd5(message);
        return md5Value;
    }
    
    private String getProgramMd5Value(byte[] message)
    {
        int md5Key = 0x10325476;
        String md5Value = new Md5(md5Key).ComputeMd5(message);
        return md5Value;
    }
    
    private String getNormalSchFileName()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(PosterApplication.getProgramPath());
        sb.append(File.separator);
        sb.append("playlist");
        sb.append(File.separator);
        sb.append(PosterApplication.LOCAL_NORMAL_PLAYLIST_FILENAME);
        return sb.toString();
    }
    
    private String getEmgcySchFileName()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(PosterApplication.getProgramPath());
        sb.append(File.separator);
        sb.append("playlist");
        sb.append(File.separator);
        sb.append(PosterApplication.LOCAL_EMGCY_PLAYLIST_FILENAME);
        return sb.toString();
    }
    
    private void startAPKUpdate(String xmlFile, String verifyKey, String verifyCode, int regCode) {
        APKUpdateManager apkupdatemanager = APKUpdateManager.getInstance();
        if (!apkupdatemanager.isInProgress()) {
			apkupdatemanager.startUpdate(xmlFile, verifyKey, verifyCode, regCode);
        } else {
            postResultBack(XmlCmdInfoRef.CMD_PTL_SYSUPDATE, regCode, 1, "");
        }
    }

    
    /**********************************************
     * Client state management thread *
     **********************************************/
    private final class ClientFSM extends Thread
    {
        private boolean mIsRun = false;
        
        public ClientFSM(String threadName)
        {
        	super(threadName);
            setRunFlag(true);
        }
        
        public void setRunFlag(boolean bIsRun)
        {
            mIsRun = bIsRun;
        }
        
        public boolean getRunFlag()
        {
            return mIsRun;
        }
        
        @SuppressLint("SimpleDateFormat")
		private void SyncTiming() {
    		NTPUDPClient timeClient = null;
        	try{
        		timeClient = new NTPUDPClient();
        		String timeServerUrl1= "time1.aliyun.com";
        		timeClient.setDefaultTimeout(30000);
        		
        		InetAddress timeServerAddress = InetAddress.getByName(timeServerUrl1);
        		TimeInfo timeInfo = timeClient.getTime(timeServerAddress);
        		TimeStamp timeStamp = timeInfo.getMessage().getTransmitTimeStamp();
        		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd.HHmmss");
        		String dateStr = dateFormat.format(timeStamp.getDate());
        		
        		Logger.i("System current time in mainboard: " + System.currentTimeMillis());
				Logger.i("Network check systemtime through NTP method by time1.aliyun.com , timeStamp: "  + timeStamp.toDateString());
				Logger.i("Network check systemtime through NTP method by time1.aliyun.com"  + dateStr);
				long timeinfo = timeStamp.getTime();
				Logger.i("Network check systemtime timestamp long info " + timeinfo);
				//如果时间相差一分钟则写入从网络获取的时间
				if(Math.abs(timeinfo - System.currentTimeMillis()) > 1000*60 ){
				    RuntimeExec.getInstance().runRootCmd("date -s " + dateStr);
				    RuntimeExec.getInstance().runRootCmd("clock -w");				
				}
				// 对时后重新检测定时开关机
				PowerOnOffManager.getInstance().checkAndSetOnOffTime(
                		PowerOnOffManager.AUTOSCREENOFF_IMMEDIATE);
        	}catch(UnknownHostException e){
        		e.printStackTrace();
        	}catch(IOException e){
        		e.printStackTrace();
        	}catch(Exception e){
        		e.printStackTrace();
        	}finally {
        		if (timeClient != null){
        	        timeClient.close();
        		}
        	}
    	}

        @Override
        public void run()
        {
            Logger.i("New ClientFSM thread, id is: " + currentThread().getId());
            int nInterval = 0;
            int nFristTimes = 0;
            boolean bIsSyncTime = false;
            long lastSyncTimeMillis = 0;
            mIsRecvPgmCmd = false;
            
            while (mIsRun)
            {
                try
                {
                    if (TextUtils.isEmpty(mLocalMac))
                    {
                        mLocalMac = PosterApplication.getEthMacStr();
                    }

                    if (mServerConfigChanged)
                    {
                        mState = STATE_IDLE;
                        FtpHelper.getInstance().stopDownloadPgmMaterials();
                        Logger.i("Server URL has been changed, retry to connect with new server.");
                    }
                    
                    if (getServerURL() == null)
                    {
                    	mState = STATE_IDLE;
                        Logger.w("server URL is null, please set the server URL firstly.");
                        Thread.sleep(1000);
                        continue;
                    }
                    else if (!PosterApplication.getInstance().isNetworkConnected())
                    {
                    	if (System.currentTimeMillis() - lastSyncTimeMillis > 60 * 60 * 1000)
                    	{
                             bIsSyncTime = false;
                    	}
                        mState = STATE_IDLE;
                        Logger.w("Net link is down, please connect the network firstly.");
                        Thread.sleep(3000);
                        continue;
                    }
                    else if (!bIsSyncTime)
                    {
                    	SyncTiming();
                    	bIsSyncTime = true;
                    	lastSyncTimeMillis = System.currentTimeMillis();
                    }
                    
                    switch (mState)
                    {
                    case STATE_IDLE:
                        if (true == authentication())
                        {
                            mState = STATE_ONLINE;
                            mHeartbeatFailTimes = 0;
                        }
                        break;
                    
                    case STATE_ONLINE:
                        if (false == heartbeat())
                        {
                            // 如果心跳失败次数超过3次，则转到IDLE状态
                        	if (++mHeartbeatFailTimes > HEARTBEAT_MAX_FAIL_TIMES)
                        	{
	                            mState = STATE_IDLE;
	                            Logger.w("Heart beat failed times reach the limit, go to IDLE state.");
                        	}
                        }
                        else
                        {
                        	mHeartbeatFailTimes = 0;
                        	if (nFristTimes <10 && !mIsRecvPgmCmd)
                        	{
                        		nFristTimes++;
                        		nInterval = 1000;
                        	}
                        	else
                        	{
                                nInterval = SysParamManager.getInstance().getCycleTime();
                                nInterval = (nInterval > 1) ? (nInterval * 1000) : 1000;
                        	}
                            Thread.sleep(nInterval);
                            continue;
                        }
                        break;
                    }
                    
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {
                    Logger.i("ClientFSM Thread sleep over, and safe exit, the Thread id is: " + currentThread().getId());
                    return;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            
            Logger.i("ClientFSM Thread is safe Terminate, id is: " + currentThread().getId());
        }
    }
    
    @SuppressLint("HandlerLeak")
	final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case XmlCmdInfoRef.CMD_PTL_PLAYFILE:
				if (mContext instanceof PosterMainActivity) {
					String duration = (String) msg.getData().getSerializable(
							"duration");
					String number = (String) msg.getData().getSerializable(
							"number");
					String fontColor = new StringBuilder(((String) msg
							.getData().getSerializable("fontColor"))).insert(2,
							"FF").toString();
					String fontName = (String) msg.getData().getSerializable(
							"fontName");
					String playSpeed = (String) msg.getData().getSerializable(
							"playSpeed");
					String text = (String) msg.getData()
							.getSerializable("text");

					((PosterMainActivity) mContext).startPopSub(text,
							Integer.parseInt(playSpeed),
							Integer.parseInt(duration),
							Integer.parseInt(number), fontName,
							PosterApplication.stringHexToInt(fontColor));
				}
				return;
			case XmlCmdInfoRef.CMD_PTL_SYSUPDATE:
				startAPKUpdate(msg.getData().getString("apkXmlFile"),
				/* msg.getData().getString("verifyKey"), */
				"0x10325476", msg.getData().getString("verifyCode"), msg
						.getData().getInt("regCode"));
				return;
			case XmlCmdInfoRef.CMD_PTL_SETONOFF:
				// 刷新OSD
                if (OsdSubMenuFragment.INSTANCE != null) {
                	OsdSubMenuFragment.INSTANCE.reflashOnOffTime();
                }
                // 计算开关机时间
	            PowerOnOffManager.getInstance().checkAndSetOnOffTime(PowerOnOffManager.AUTOSCREENOFF_COMMON);
				return;
			default:
				break;
			}
			super.handleMessage(msg);
		}
	};
}
