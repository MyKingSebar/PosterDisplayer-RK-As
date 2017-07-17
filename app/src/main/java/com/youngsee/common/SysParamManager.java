package com.youngsee.common;

import java.io.StringWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.xmlpull.v1.XmlSerializer;

import android.text.TextUtils;
import android.util.Xml;

import com.youngsee.logmanager.Logger;
import com.youngsee.posterdisplayer.PosterApplication;
import com.youngsee.webservices.SysParam;
import com.youngsee.webservices.XmlCmdInfoRef;

public class SysParamManager 
{
    private SysParam mSysParam = null;
	private ReadWriteLock mReadWriteLock = new ReentrantReadWriteLock();
	
    private SysParamManager()
    {   
    }
    
    private static class SysParamHolder
    {
        static final SysParamManager INSTANCE = new SysParamManager();
    }
    
    public static SysParamManager getInstance()
    {
        return SysParamHolder.INSTANCE;
    }
    
    public void initSysParam()
    {
        mReadWriteLock.writeLock().lock();
        mSysParam = DbHelper.getInstance().getSysParamFromDB();
        if (mSysParam == null)
        {
            mSysParam = PosterApplication.getInstance().factoryRest();
            DbHelper.getInstance().saveSysParamToDB(mSysParam);
        }
        else
        {
            mSysParam.cfevervalue = android.os.Build.VERSION.RELEASE;
            mSysParam.swVervalue = PosterApplication.getInstance().getVerName();
            mSysParam.kernelvervalue = PosterApplication.getInstance().getKernelVersion();
        }
        mReadWriteLock.writeLock().unlock();
    }

    public int getStatus()
    {
        int status = -1;
        
        if (mSysParam != null)
        {
            mReadWriteLock.readLock().lock();
            status = mSysParam.setBit;
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
        
        return status;
    }
    
    public void setStatus(int status)
    {
        if (mSysParam != null)
        {
            mReadWriteLock.writeLock().lock();
            mSysParam.setBit = status;
            DbHelper.getInstance().updateSetBit(status);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
    }
    
    public int getOsdLang()
    {
        int lang = -1;
        
        if (mSysParam != null)
        {
            mReadWriteLock.readLock().lock();
            lang = mSysParam.osdLangSetosd_lang;
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
        
        return lang;
    }
    
    public void setOsdLang(int osdLang)
    {
        if (mSysParam != null)
        {
            mReadWriteLock.writeLock().lock();
            mSysParam.osdLangSetosd_lang = osdLang;
            DbHelper.getInstance().updateOsdLang(osdLang);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
    }
    
    public int getTaskPeriodTime()
    {
        int time = -1;
        
        if (mSysParam != null)
        {
            mReadWriteLock.readLock().lock();
            time = mSysParam.getTaskPeriodtime;
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
        
        return time;
    }
    
    public void setTaskPeriodTime(int time)
    {
        if (mSysParam != null)
        {
            mReadWriteLock.writeLock().lock();
            mSysParam.getTaskPeriodtime = time;
            DbHelper.getInstance().updateGetTaskPeriodtime(time);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
    }
    
    public int getDelFilePeriodTime()
    {
        int time = -1;
        
        if (mSysParam != null)
        {
            mReadWriteLock.readLock().lock();
            time = mSysParam.delFilePeriodtime;
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
        
        return time;
    }
    
    public void setDelFilePeriodTime(int time)
    {
        if (mSysParam != null)
        {
            mReadWriteLock.writeLock().lock();
            mSysParam.delFilePeriodtime = time;
            DbHelper.getInstance().updateDelFilePeriodtime(time);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
    }
    
    public String getTimeZone()
    {
        String zone = "";
        
        if (mSysParam != null)
        {
            mReadWriteLock.readLock().lock();
            zone = mSysParam.timeZonevalue;
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
        
        return zone;
    }
    
    public void setTimeZone(String zone)
    {
        if (mSysParam != null)
        {
            mReadWriteLock.writeLock().lock();
            mSysParam.timeZonevalue = zone;
            DbHelper.getInstance().updateTimeZone(zone);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
    }
    
    public int getScrnRotate()
    {
        int rotate = -1;
        
        if (mSysParam != null)
        {
            mReadWriteLock.readLock().lock();
            rotate = mSysParam.scrnRotatevalue;
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
        
        return rotate;
    }
    
    public void setScrnRotate(int rotate)
    {
        if (mSysParam != null)
        {
            mReadWriteLock.writeLock().lock();
            mSysParam.scrnRotatevalue = rotate;
            DbHelper.getInstance().updateScrnRotate(rotate);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
    }
    
    public String getPasswd()
    {
        String pwd = "";
        
        if (mSysParam != null)
        {
            mReadWriteLock.readLock().lock();
            pwd = mSysParam.passwdvalue;
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
        
        return pwd;
    }
    
    public void setPasswd(String pwd)
    {
        if (mSysParam != null)
        {
            mReadWriteLock.writeLock().lock();
            mSysParam.passwdvalue = pwd;
            DbHelper.getInstance().updatePasswd(pwd);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
    }
    
    public String getSysPasswd()
    {
        String pwd = "";
        
        if (mSysParam != null)
        {
            mReadWriteLock.readLock().lock();
            pwd = mSysParam.syspasswdvalue;
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
        
        return pwd;
    }
    
    public void setSysPasswd(String pwd)
    {
        if (mSysParam != null)
        {
            mReadWriteLock.writeLock().lock();
            mSysParam.syspasswdvalue = pwd;
            DbHelper.getInstance().updateSysPasswd(pwd);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
    }
    
    public int getDevSel()
    {
        int value = -1;
        
        if (mSysParam != null)
        {
            mReadWriteLock.readLock().lock();
            value = mSysParam.devSelvalue;
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
        
        return value;
    }
    
    public void setDevSel(int value)
    {
        if (mSysParam != null)
        {
            mReadWriteLock.writeLock().lock();
            mSysParam.devSelvalue = value;
            DbHelper.getInstance().updateDevSel(value);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
    }
    
    public int getBrightness()
    {
        int value = -1;
        
        if (mSysParam != null)
        {
            mReadWriteLock.readLock().lock();
            value = mSysParam.brightnessvalue;
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
        
        return value;
    }
    
    public void setBrightness(int value)
    {
        if (mSysParam != null)
        {
            mReadWriteLock.writeLock().lock();
            mSysParam.brightnessvalue = value;
            DbHelper.getInstance().updateBrightness(value);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
    }
    
    public int getVolume()
    {
        int value = -1;
        
        if (mSysParam != null)
        {
            mReadWriteLock.readLock().lock();
            value = mSysParam.volumevalue;
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
        
        return value;
    }
    
    public void setVolume(int value)
    {
        if (mSysParam != null)
        {
            mReadWriteLock.writeLock().lock();
            mSysParam.volumevalue = value;
            DbHelper.getInstance().updateVolume(value);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
    }
    
    public String getSwVer()
    {
        String version = "";
        
        if (mSysParam != null)
        {
            mReadWriteLock.readLock().lock();
            version = mSysParam.swVervalue;
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
        
        return version;
    }
    
    public void setSwVer(String version)
    {
        if (mSysParam != null)
        {
            mReadWriteLock.writeLock().lock();
            mSysParam.swVervalue = version;
            DbHelper.getInstance().updateSwVer(version);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
    }
    
    public String getHwVer()
    {
        String version = "";
        
        if (mSysParam != null)
        {
            mReadWriteLock.readLock().lock();
            version = mSysParam.hwVervalue;
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
        
        return version;
    }
    
    public void setHwVer(String version)
    {
        if (mSysParam != null)
        {
            mReadWriteLock.writeLock().lock();
            mSysParam.hwVervalue = version;
            DbHelper.getInstance().updateHwVer(version);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
    }
    
    public String getKernelVer()
    {
        String version = "";
        
        if (mSysParam != null)
        {
            mReadWriteLock.readLock().lock();
            version = mSysParam.kernelvervalue;
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
        
        return version;
    }
    
    public void setKernelVer(String version)
    {
        if (mSysParam != null)
        {
            mReadWriteLock.writeLock().lock();
            mSysParam.kernelvervalue = version;
            DbHelper.getInstance().updateKernelVer(version);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
    }
    
    public String getCfeVer()
    {
        String version = "";
        
        if (mSysParam != null)
        {
            mReadWriteLock.readLock().lock();
            version = mSysParam.cfevervalue;
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
        
        return version;
    }
    
    public void setCfeVer(String version)
    {
        if (mSysParam != null)
        {
            mReadWriteLock.writeLock().lock();
            mSysParam.cfevervalue = version;
            DbHelper.getInstance().updateCfeVer(version);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
    }
    
    public String getCertNum()
    {
        String certnum = "";
        
        if (mSysParam != null)
        {
            mReadWriteLock.readLock().lock();
            certnum = mSysParam.certNumvalue;
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
        
        return certnum;
    }
    
    public void setCertNum(String certnum)
    {
        if (mSysParam != null)
        {
            mReadWriteLock.writeLock().lock();
            mSysParam.certNumvalue = certnum;
            DbHelper.getInstance().updateCertNum(certnum);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
    }
    
    public String getTermMode()
    {
        String mode = "";
        
        if (mSysParam != null)
        {
            mReadWriteLock.readLock().lock();
            mode = mSysParam.termmodelvalue;
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
        
        return mode;
    }
    
    public void setTermMode(String mode)
    {
        if (mSysParam != null)
        {
            mReadWriteLock.writeLock().lock();
            mSysParam.termmodelvalue = mode;
            DbHelper.getInstance().updateTermMode(mode);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
    }
    
    public String getTerm()
    {
        String name = "";
        
        if (mSysParam != null)
        {
            mReadWriteLock.readLock().lock();
            name = mSysParam.termname;
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
        
        return name;
    }
    
    public void setTerm(String name)
    {
        if (mSysParam != null)
        {
            mReadWriteLock.writeLock().lock();
            mSysParam.termname = name;
            DbHelper.getInstance().updateTermName(name);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
    }
    
    public String getTermGrp()
    {
        String grpname = "";
        
        if (mSysParam != null)
        {
            mReadWriteLock.readLock().lock();
            grpname = mSysParam.termGrpvalue;
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
        
        return grpname;
    }
    
    public void setTermGrp(String name)
    {
        if (mSysParam != null)
        {
            mReadWriteLock.writeLock().lock();
            mSysParam.termGrpvalue = name;
            DbHelper.getInstance().updateTermGrp(name);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
    }
    
    public int getDwnLdrSpd()
    {
        int dwnspd = -1;
        
        if (mSysParam != null)
        {
            mReadWriteLock.readLock().lock();
            dwnspd = mSysParam.dwnLdrSpdvalue;
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
        
        return dwnspd;
    }
    
    public void setDwnLdrSpd(int dwnspd)
    {
        if (mSysParam != null)
        {
            mReadWriteLock.writeLock().lock();
            mSysParam.dwnLdrSpdvalue = dwnspd;
            DbHelper.getInstance().updateDwnLdrSpd(dwnspd);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
    }
    
    public int getCycleTime()
    {
        int time = -1;
        
        if (mSysParam != null)
        {
            mReadWriteLock.readLock().lock();
            time = mSysParam.cycleTimevalue;
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
        
        return time;
    }
    
    public void setCycleTime(int time)
    {
        if (mSysParam != null)
        {
            mReadWriteLock.writeLock().lock();
            mSysParam.cycleTimevalue = time;
            DbHelper.getInstance().updateCycleTime(time);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
    }
    
    public int getDispScale()
    {
        int value = -1;
        
        if (mSysParam != null)
        {
            mReadWriteLock.readLock().lock();
            value = mSysParam.dispScalevalue;
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
        
        return value;
    }
    
    public void setDispScale(int value)
    {
        if (mSysParam != null)
        {
            mReadWriteLock.writeLock().lock();
            mSysParam.dispScalevalue = value;
            DbHelper.getInstance().updateDispScale(value);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
    }
    
    public int getAutoUpgrade()
    {
        int value = -1;
        
        if (mSysParam != null)
        {
            mReadWriteLock.readLock().lock();
            value = mSysParam.autoupgradevalue;
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
        
        return value;
    }
    
    public void setAutoUpgrade(int value)
    {
        if (mSysParam != null)
        {
            mReadWriteLock.writeLock().lock();
            mSysParam.autoupgradevalue = value;
            DbHelper.getInstance().updateAutoUpgrade(value);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
    }
    
    public int getRunMode()
    {
        int value = -1;
        
        if (mSysParam != null)
        {
            mReadWriteLock.readLock().lock();
            value = mSysParam.runmodevalue;
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
        
        return value;
    }
    
    public void setRunMode(int value)
    {
        if (mSysParam != null)
        {
            mReadWriteLock.writeLock().lock();
            mSysParam.runmodevalue = value;
            DbHelper.getInstance().updateRunMode(value);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("system param is null.");
        }
    }
    
    public ConcurrentHashMap<String, String> getNetConnParam()
    {
        ConcurrentHashMap<String, String> netcon = null;
        
        if (mSysParam != null && mSysParam.netConn != null)
        {
            mReadWriteLock.readLock().lock();
            netcon = new ConcurrentHashMap<String, String>(mSysParam.netConn);
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("Net connection param is null.");
        }
        
        return netcon;
    }
    
    public void setNetConnParam(final ConcurrentHashMap<String, String> netcon)
    {
        if (mSysParam != null && netcon != null)
        {
            mReadWriteLock.writeLock().lock();
            if (mSysParam.netConn != null)
            {
                String mode = netcon.get("mode");
                String ip = netcon.get("ip");
                String gateway = netcon.get("gateway");
                String mask = netcon.get("mask");
                String dns1 = netcon.get("dns1");
                String dns2 = netcon.get("dns2");
                String module = netcon.get("module");
                String time = netcon.get("time");
                
                mSysParam.netConn.put("mode", TextUtils.isEmpty(mode) ? "" : mode);
                mSysParam.netConn.put("ip", TextUtils.isEmpty(ip) ? "0.0.0.0" : ip);
                if (!TextUtils.isEmpty(gateway))
                {
                    mSysParam.netConn.put("gateway", gateway);
                }
                if (!TextUtils.isEmpty(mask))
                {
                    mSysParam.netConn.put("mask", mask);
                }
                if (!TextUtils.isEmpty(dns1))
                {
                    mSysParam.netConn.put("dns1", dns1);
                }
                if (!TextUtils.isEmpty(dns2))
                {
                    mSysParam.netConn.put("dns2", dns2);
                }
                if (!TextUtils.isEmpty(module))
                {
                    mSysParam.netConn.put("module", module);
                }
                if (!TextUtils.isEmpty(time))
                {
                    mSysParam.netConn.put("time", time);
                }
            }
            else
            {
                mSysParam.netConn = new ConcurrentHashMap<String, String>(netcon);
            }
            DbHelper.getInstance().updateNetConn(mSysParam.netConn);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("Net connection param is null.");
        }
    }
    
    public ConcurrentHashMap<String, String> getServerParam()
    {
        ConcurrentHashMap<String, String> serverInfo = null;
        
        if (mSysParam != null && mSysParam.serverSet != null)
        {
            mReadWriteLock.readLock().lock();
            serverInfo = new ConcurrentHashMap<String, String>(mSysParam.serverSet);
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("server param is null.");
        }
        
        return serverInfo;
    }
    
    public void setServerParam(final ConcurrentHashMap<String, String> serverInfo)
    {
        if (mSysParam != null && serverInfo != null)
        {
            mReadWriteLock.writeLock().lock();
            if (mSysParam.serverSet != null)
            {
                String webUrl = serverInfo.get("weburl");
                String ftpIp = serverInfo.get("ftpip");
                String ftpName = serverInfo.get("ftpname");
                String ftpPort = serverInfo.get("ftpport");
                String ftpPwd = serverInfo.get("ftppasswd");
                String ntpIp = serverInfo.get("ntpip");
                String ntpPort = serverInfo.get("ntpport");
                
                mSysParam.serverSet.put("weburl", TextUtils.isEmpty(webUrl) ? "" : webUrl);
                mSysParam.serverSet.put("ftpip", TextUtils.isEmpty(ftpIp) ? "0.0.0.0" : ftpIp);
                mSysParam.serverSet.put("ftpport", TextUtils.isEmpty(ftpPort) ? "" : ftpPort);
                mSysParam.serverSet.put("ftpname", TextUtils.isEmpty(ftpName) ? "" : ftpName);
                mSysParam.serverSet.put("ftppasswd", TextUtils.isEmpty(ftpPwd) ? "" : ftpPwd);
                mSysParam.serverSet.put("ntpip", TextUtils.isEmpty(ntpIp) ? "" : ntpIp);
                mSysParam.serverSet.put("ntpport", TextUtils.isEmpty(ntpPort) ? "" : ntpPort);
            }
            else
            {
                mSysParam.serverSet = new ConcurrentHashMap<String, String>(serverInfo);
            }
            DbHelper.getInstance().updateServerSet(mSysParam.serverSet);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("server param is null.");
        }
    }
    
    public ConcurrentHashMap<String, String> getSigOutParam()
    {
        ConcurrentHashMap<String, String> sigout = null;
        
        if (mSysParam != null && mSysParam.sigOutSet != null)
        {
            mReadWriteLock.readLock().lock();
            sigout = new ConcurrentHashMap<String, String>(mSysParam.sigOutSet);
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("Signal out param is null.");
        }
        
        return sigout;
    }
    
    public void setSigOutParam(final ConcurrentHashMap<String, String> sigout)
    {
        if (mSysParam != null && sigout != null)
        {
            mReadWriteLock.writeLock().lock();
            if (mSysParam.sigOutSet != null)
            {
                String mode = sigout.get("mode");
                String value = sigout.get("value");
                String rpt = sigout.get("repratio");
                
                mSysParam.sigOutSet.put("mode", TextUtils.isEmpty(mode) ? "" : mode);
                mSysParam.sigOutSet.put("value", TextUtils.isEmpty(value) ? "" : value);
                mSysParam.sigOutSet.put("repratio", TextUtils.isEmpty(rpt) ? "" : rpt);
            }
            else
            {
                mSysParam.sigOutSet = new ConcurrentHashMap<String, String>(sigout);
            }
            DbHelper.getInstance().updateSigOutSet(mSysParam.sigOutSet);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("Signal out param is null.");
        }
    }
    
    public ConcurrentHashMap<String, String> getWifiParam()
    {
        ConcurrentHashMap<String, String> wifi = null;
        
        if (mSysParam != null && mSysParam.wifiSet != null)
        {
            mReadWriteLock.readLock().lock();
            wifi = new ConcurrentHashMap<String, String>(mSysParam.wifiSet);
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("wifi configure param is null.");
        }
        
        return wifi;
    }
    
    public void setWifiParam(final ConcurrentHashMap<String, String> wifi)
    {
        if (mSysParam != null && wifi != null)
        {
            mReadWriteLock.writeLock().lock();
            if (mSysParam.wifiSet != null)
            {
                String ssid = wifi.get("ssid");
                String wpapsk = wifi.get("wpapsk");
                String auth = wifi.get("authmode");
                String type = wifi.get("encryptype");
                
                mSysParam.wifiSet.put("ssid", TextUtils.isEmpty(ssid) ? "" : ssid);
                mSysParam.wifiSet.put("wpapsk", TextUtils.isEmpty(wpapsk) ? "" : wpapsk);
                mSysParam.wifiSet.put("authmode", TextUtils.isEmpty(auth) ? "" : auth);
                mSysParam.wifiSet.put("encryptype", TextUtils.isEmpty(type) ? "" : type);
            }
            else
            {
                mSysParam.wifiSet = new ConcurrentHashMap<String, String>(wifi);
            }
            DbHelper.getInstance().updateWifiSet(mSysParam.wifiSet);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("Wifi config param is null.");
        }
    }
    
    public ConcurrentHashMap<String, String> getOnOffTimeParam()
    {
        ConcurrentHashMap<String, String> onofftime = null;
        
        if (mSysParam != null && mSysParam.onOffTime != null)
        {
            mReadWriteLock.readLock().lock();
            onofftime = new ConcurrentHashMap<String, String>(mSysParam.onOffTime);
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("On&Off time param is null.");
        }
        
        return onofftime;
    }
    
    public void setOnOffTimeParam(final ConcurrentHashMap<String, String> onofftime)
    {
        if (mSysParam != null && onofftime != null)
        {
            mReadWriteLock.writeLock().lock();
            if (mSysParam.onOffTime != null)
            {
                String week = null;
                String onTime = null;
                String offTime = null;
                int group = Integer.parseInt(onofftime.get("group"));
                for (int i = 0; i < group; i++)
                {
                    week = onofftime.get("week" + (i + 1));
                    onTime = onofftime.get("on_time" + (i + 1));
                    offTime = onofftime.get("off_time" + (i + 1));
                    
                    mSysParam.onOffTime.put(("week" + (i + 1)), TextUtils.isEmpty(week) ? "" : week);
                    mSysParam.onOffTime.put(("on_time" + (i + 1)), TextUtils.isEmpty(onTime) ? "" : onTime);
                    mSysParam.onOffTime.put(("off_time" + (i + 1)), TextUtils.isEmpty(offTime) ? "" : offTime);
                }
                mSysParam.onOffTime.put("group", String.valueOf(group));
            }
            else
            {
                mSysParam.onOffTime = new ConcurrentHashMap<String, String>(onofftime);
            }
            DbHelper.getInstance().updateOnOffTime(mSysParam.onOffTime);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("On&Off time param is null.");
        }
    }
    
    public ConcurrentHashMap<String, String> getOffDlTimeParam()
    {
        ConcurrentHashMap<String, String> offDlTime = null;
        
        if (mSysParam != null && mSysParam.offdlTime != null)
        {
            mReadWriteLock.readLock().lock();
            offDlTime = new ConcurrentHashMap<String, String>(mSysParam.offdlTime);
            mReadWriteLock.readLock().unlock();
        }
        else
        {
            Logger.i("Off DL time param is null.");
        }
        
        return offDlTime;
    }
    
    public void setOffDlTimeParam(final ConcurrentHashMap<String, String> offDlTime)
    {
        if (mSysParam != null && offDlTime != null)
        {
            mReadWriteLock.writeLock().lock();
            if (mSysParam.offdlTime != null)
            {
                String week = null;
                String onTime = null;
                String offTime = null;
                int group = Integer.parseInt(offDlTime.get("group"));
                for (int i = 0; i < group; i++)
                {
                    week = offDlTime.get("week" + (i + 1));
                    onTime = offDlTime.get("on_time" + (i + 1));
                    offTime = offDlTime.get("off_time" + (i + 1));
                    
                    mSysParam.offdlTime.put(("week" + (i + 1)), TextUtils.isEmpty(week) ? "" : week);
                    mSysParam.offdlTime.put(("on_time" + (i + 1)), TextUtils.isEmpty(onTime) ? "" : onTime);
                    mSysParam.offdlTime.put(("off_time" + (i + 1)), TextUtils.isEmpty(offTime) ? "" : offTime);
                }
                mSysParam.offdlTime.put("group", String.valueOf(group));
            }
            else
            {
                mSysParam.offdlTime = new ConcurrentHashMap<String, String>(offDlTime);
            }
            DbHelper.getInstance().updateOffdlTime(mSysParam.offdlTime);
            mReadWriteLock.writeLock().unlock();
        }
        else
        {
            Logger.i("Off DL time param is null.");
        }
    }
    
    public String getFormatXmlParam()
    {
        String xmlParam = "0";
        if (mSysParam == null)
        {
            initSysParam();
        }
        
        mReadWriteLock.readLock().lock();
        
        try
        {
            // 初始化引擎
            XmlSerializer serializer = Xml.newSerializer();
            
            // 创建一个字符串输出流对象
            StringWriter stringWriter = new StringWriter();
            
            // 设置输出流
            serializer.setOutput(stringWriter);
            
            serializer.startDocument(FileUtils.ENCODING, true);
            serializer.startTag(null, XmlCmdInfoRef.SYSSETDATAFILE);
            
            serializer.startTag(null, XmlCmdInfoRef.SETBIT);
            serializer.text(Integer.toString(mSysParam.setBit));
            serializer.endTag(null, XmlCmdInfoRef.SETBIT);
            
            if (mSysParam.netConn != null)
            {
                serializer.startTag(null, XmlCmdInfoRef.NETCONN);
                for (String key : mSysParam.netConn.keySet())
                {
                    serializer.attribute(null, key, mSysParam.netConn.get(key));
                }
                serializer.endTag(null, XmlCmdInfoRef.NETCONN);
            }
            
            if (mSysParam.serverSet != null)
            {
                serializer.startTag(null, XmlCmdInfoRef.SRVERSET);
                for (String key : mSysParam.serverSet.keySet())
                {
                    serializer.attribute(null, key, mSysParam.serverSet.get(key));
                }
                serializer.endTag(null, XmlCmdInfoRef.SRVERSET);
            }
            
            serializer.startTag(null, XmlCmdInfoRef.SIGOUTSET);
            serializer.attribute(null, "mode", "3"); // HDMI
            serializer.attribute(null, "value", "10"); // 1080P
            if (mSysParam.sigOutSet != null)
            {
                serializer.attribute(null, "repratio", (mSysParam.sigOutSet.get("repratio") == null) ? "" : mSysParam.sigOutSet.get("repratio"));
            }
            serializer.endTag(null, XmlCmdInfoRef.SIGOUTSET);
            
            if (mSysParam.wifiSet != null)
            {
                serializer.startTag(null, XmlCmdInfoRef.WIFISET);
                for (String key : mSysParam.wifiSet.keySet())
                {
                    serializer.attribute(null, key, mSysParam.wifiSet.get(key));
                }
                serializer.endTag(null, XmlCmdInfoRef.WIFISET);
            }
            
            serializer.startTag(null, XmlCmdInfoRef.OSDLANGSET);
            serializer.attribute(null, XmlCmdInfoRef.OSDLANG, Integer.toString(mSysParam.osdLangSetosd_lang));
            serializer.endTag(null, XmlCmdInfoRef.OSDLANGSET);
            
            serializer.startTag(null, XmlCmdInfoRef.GETTASKPERIOD);
            serializer.attribute(null, XmlCmdInfoRef.TIME, Integer.toString(mSysParam.getTaskPeriodtime));
            serializer.endTag(null, XmlCmdInfoRef.GETTASKPERIOD);
            
            serializer.startTag(null, XmlCmdInfoRef.DELFILEPERIOD);
            serializer.attribute(null, XmlCmdInfoRef.TIME, Integer.toString(mSysParam.delFilePeriodtime));
            serializer.endTag(null, XmlCmdInfoRef.DELFILEPERIOD);
            
            if (mSysParam.timeZonevalue != null)
            {
                serializer.startTag(null, XmlCmdInfoRef.TIMEZONE);
                serializer.attribute(null, XmlCmdInfoRef.VALUE, mSysParam.timeZonevalue);
                serializer.endTag(null, XmlCmdInfoRef.TIMEZONE);
            }
            
            serializer.startTag(null, XmlCmdInfoRef.SCRNROTATE);
            serializer.attribute(null, XmlCmdInfoRef.VALUE, Integer.toString(mSysParam.scrnRotatevalue));
            serializer.endTag(null, XmlCmdInfoRef.SCRNROTATE);
            
            if (mSysParam.passwdvalue != null)
            {
                serializer.startTag(null, XmlCmdInfoRef.PASSWD);
                serializer.attribute(null, XmlCmdInfoRef.VALUE, mSysParam.passwdvalue);
                serializer.endTag(null, XmlCmdInfoRef.PASSWD);
            }
            
            if (mSysParam.syspasswdvalue != null)
            {
                serializer.startTag(null, XmlCmdInfoRef.SYSPASSWD);
                serializer.attribute(null, XmlCmdInfoRef.VALUE, mSysParam.syspasswdvalue);
                serializer.endTag(null, XmlCmdInfoRef.SYSPASSWD);
            }
            
            if (mSysParam.onOffTime != null)
            {
                serializer.startTag(null, XmlCmdInfoRef.ONOFFTIME);
                for (String key : mSysParam.onOffTime.keySet())
                {
                    serializer.attribute(null, key, mSysParam.onOffTime.get(key));
                }
                serializer.endTag(null, XmlCmdInfoRef.ONOFFTIME);
            }
            
            serializer.startTag(null, XmlCmdInfoRef.DEVSEL);
            serializer.attribute(null, XmlCmdInfoRef.VALUE, Integer.toString(mSysParam.devSelvalue));
            serializer.endTag(null, XmlCmdInfoRef.DEVSEL);
            
            serializer.startTag(null, XmlCmdInfoRef.BRIGHTNESS);
            serializer.attribute(null, XmlCmdInfoRef.VALUE, Integer.toString(mSysParam.brightnessvalue));
            serializer.endTag(null, XmlCmdInfoRef.BRIGHTNESS);
            
            serializer.startTag(null, XmlCmdInfoRef.VOLUME);
            serializer.attribute(null, XmlCmdInfoRef.VALUE, Integer.toString(mSysParam.volumevalue));
            serializer.endTag(null, XmlCmdInfoRef.VOLUME);
            
            if (mSysParam.swVervalue != null)
            {
                serializer.startTag(null, XmlCmdInfoRef.SWVER);
                serializer.attribute(null, XmlCmdInfoRef.VALUE, mSysParam.swVervalue);
                serializer.endTag(null, XmlCmdInfoRef.SWVER);
            }
            
            if (mSysParam.hwVervalue != null)
            {
                serializer.startTag(null, XmlCmdInfoRef.HWVER);
                serializer.attribute(null, XmlCmdInfoRef.VALUE, mSysParam.hwVervalue);
                serializer.endTag(null, XmlCmdInfoRef.HWVER);
            }
            
            if (mSysParam.kernelvervalue != null)
            {
                serializer.startTag(null, XmlCmdInfoRef.KERNELVER);
                serializer.attribute(null, XmlCmdInfoRef.VALUE, mSysParam.kernelvervalue);
                serializer.endTag(null, XmlCmdInfoRef.KERNELVER);
            }
            
            if (mSysParam.cfevervalue != null)
            {
                serializer.startTag(null, XmlCmdInfoRef.CFEVER);
                serializer.attribute(null, XmlCmdInfoRef.VALUE, mSysParam.cfevervalue);
                serializer.endTag(null, XmlCmdInfoRef.CFEVER);
            }
            
            if (mSysParam.certNumvalue != null)
            {
                serializer.startTag(null, XmlCmdInfoRef.CERTNUM);
                serializer.attribute(null, XmlCmdInfoRef.VALUE, mSysParam.certNumvalue);
                serializer.endTag(null, XmlCmdInfoRef.CERTNUM);
            }
            
            if (mSysParam.termmodelvalue != null)
            {
                serializer.startTag(null, XmlCmdInfoRef.TERMMDL);
                serializer.attribute(null, XmlCmdInfoRef.VALUE, mSysParam.termmodelvalue);
                serializer.endTag(null, XmlCmdInfoRef.TERMMDL);
            }
            
            if (mSysParam.termname != null)
            {
                serializer.startTag(null, XmlCmdInfoRef.TERM);
                serializer.attribute(null, XmlCmdInfoRef.VALUE, mSysParam.termname);
                serializer.endTag(null, XmlCmdInfoRef.TERM);
            }
                  
            if (mSysParam.termGrpvalue != null)
            {
                serializer.startTag(null, XmlCmdInfoRef.TERMGRP);
                serializer.attribute(null, XmlCmdInfoRef.VALUE, mSysParam.termGrpvalue);
                serializer.endTag(null, XmlCmdInfoRef.TERMGRP);
            }
            
            serializer.startTag(null, XmlCmdInfoRef.DWNLDRSPD);
            serializer.attribute(null, XmlCmdInfoRef.VALUE, Integer.toString(mSysParam.dwnLdrSpdvalue));
            serializer.endTag(null, XmlCmdInfoRef.DWNLDRSPD);
            
            serializer.startTag(null, XmlCmdInfoRef.CYCLETIME);
            serializer.attribute(null, XmlCmdInfoRef.VALUE, Integer.toString(mSysParam.cycleTimevalue));
            serializer.endTag(null, XmlCmdInfoRef.CYCLETIME);
            
            serializer.startTag(null, XmlCmdInfoRef.DISPSCALE);
            serializer.attribute(null, XmlCmdInfoRef.VALUE, Integer.toString(mSysParam.dispScalevalue));
            serializer.endTag(null, XmlCmdInfoRef.DISPSCALE);
            
            serializer.startTag(null, XmlCmdInfoRef.AUTOGRADE);
            serializer.attribute(null, XmlCmdInfoRef.VALUE, Integer.toString(mSysParam.autoupgradevalue));
            serializer.endTag(null, XmlCmdInfoRef.AUTOGRADE);
            
            serializer.startTag(null, XmlCmdInfoRef.RUNMODER);
            serializer.attribute(null, XmlCmdInfoRef.VALUE, Integer.toString(mSysParam.runmodevalue));
            serializer.endTag(null, XmlCmdInfoRef.RUNMODER);
            
            if (mSysParam.offdlTime != null)
            {
                serializer.startTag(null, XmlCmdInfoRef.OFFDLTIME);
                for (String key : mSysParam.offdlTime.keySet())
                {
                    serializer.attribute(null, key, mSysParam.offdlTime.get(key));
                }
                serializer.endTag(null, XmlCmdInfoRef.OFFDLTIME);
            }
            
            serializer.endTag(null, XmlCmdInfoRef.SYSSETDATAFILE);
            serializer.endDocument();
            
            xmlParam = stringWriter.toString();
        }
        catch (Exception e)
        {
            Logger.e("transfer sysparam has error.");
            e.printStackTrace();
        }

        mReadWriteLock.readLock().unlock();
        
        return xmlParam;
    }
}
