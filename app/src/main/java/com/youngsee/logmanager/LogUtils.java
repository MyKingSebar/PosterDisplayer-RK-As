package com.youngsee.logmanager;

import java.util.LinkedList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.youngsee.common.FileUtils;
import com.youngsee.posterdisplayer.PosterApplication;

/**
 * 日志输出工具类
 * 
 * @author liuhuining
 * @date 2014年7月18日
 * 
 */
@SuppressLint("SimpleDateFormat")
public class LogUtils
{
    private Context            mContext          = null;
    private static LogUtils    mLogUtilsInstance = null;
    
    private LinkedList<String> mPlogList         = new LinkedList<String>();
    private LinkedList<String> mSlogList         = new LinkedList<String>();
    
    private LogThread          mlogthread        = null;
    private boolean            mSysLogIsOpen     = false;
    private boolean            mPlayLogIsOpen    = false;
    
    private LogUtils(Context context)
    {
        /*
         * This Class is a single instance mode, and define a private constructor to avoid external use the 'new'
         * keyword to instantiate a objects directly.
         */
        mContext = context;
        SharedPreferences spf = mContext.getSharedPreferences("logset", Activity.MODE_PRIVATE);
        mPlayLogIsOpen = spf.getBoolean("play", true);
        mSysLogIsOpen = spf.getBoolean("system", true);
    }
    
    public static LogUtils createInstance(Context context)
    {
        if (mLogUtilsInstance == null && context != null)
        {
            mLogUtilsInstance = new LogUtils(context);
        }
        return mLogUtilsInstance;
    }
    
    public static LogUtils getInstance()
    {
        return mLogUtilsInstance;
    }
    
    public void setSysLogFlag(boolean isOpen)
    {
        mSysLogIsOpen = isOpen;
    }
    
    public void setPlayLogFlag(boolean isOpen)
    {
        mPlayLogIsOpen = isOpen;
    }
    
    /**
     * 添加播放日志
     * 
     * @param level
     * @param type
     * @param pid
     *            素材id
     * @param area
     *            素材窗口名称
     * @param remark
     *            备注
     */
    public void toAddPLog(int level, int type, String pid, String area, String remark)
    { 
        if (mPlayLogIsOpen)
        {
            StringBuilder text = new StringBuilder();
            text.append("<ID>").append(System.currentTimeMillis()).append("</ID>").append("<TIME>").append(PosterApplication.getCurrentTime()).append("</TIME>").append("<LEVEL>").append(level)
                    .append("</LEVEL>").append("<TYPE>").append(type).append("</TYPE>").append("<PARAM>").append("<AREA>").append(area).append("</AREA>").append("<ID>").append(pid).append("</ID>")
                    .append("</PARAM>").append("\r\n");
            synchronized (mPlogList)
            {
                mPlogList.addLast(text.toString());
            }
        }
    }
    
    /**
     * 添加系统操作日志
     * 
     * @param level
     * @param type
     * @param param
     *            随着type的变化而变化
     */
    public void toAddSLog(int level, int type, String param)
    {
        if (mSysLogIsOpen)
        {
            StringBuilder text = new StringBuilder();
            text.append("<ID>").append(System.currentTimeMillis()).append("</ID>").append("<TIME>").append(PosterApplication.getCurrentTime()).append("</TIME>").append("<LEVEL>").append(level)
                    .append("</LEVEL>").append("<TYPE>").append(type).append("</TYPE>").append("<PARAM>").append(param).append("</PARAM>").append("\r\n");
            synchronized (mSlogList)
            {
                mSlogList.addLast(text.toString());
            }
        }
    }
    
    public void startRun()
    {
        stopRun();
        if (mlogthread == null)
        {
            mlogthread = new LogThread(true);
        }
        mlogthread.start();
    }
    
    public void stopRun()
    {
        if (mlogthread != null)
        {
            mlogthread.setRunFlag(false);
            mlogthread.interrupt();
            mlogthread = null;
        }
    }
    
    private final class LogThread extends Thread
    {
        private boolean mIsRun    = true;
        private String mTodayDate = null;
        private String  mpLogPath = null;
        private String  msLogPath = null;
        
        public LogThread(boolean bIsRun)
        {
            setRunFlag(bIsRun);
        }
        
        public void setRunFlag(boolean bIsRun)
        {
            mIsRun = bIsRun;
        }
        
        @Override
        public void run()
        {
            Logger.i("New LogThread thread, id is: " + currentThread().getId());
            while (mIsRun)
            {
                try
                {
                    synchronized (mPlogList)
                    {
                        if (!mPlogList.isEmpty())
                        {
                            if (!PosterApplication.getCurrentDate().equals(mTodayDate))
                            {
                                mpLogPath = PosterApplication.getLogFileFullPath(0, 0);
                                msLogPath = PosterApplication.getLogFileFullPath(1, 0);
                                mTodayDate = PosterApplication.getCurrentDate();
                            }
                            
                            FileUtils.writeSDFileData(mpLogPath, mPlogList.removeFirst(), true);
                        }
                    }
                    
                    synchronized (mSlogList)
                    {
                        if (!mSlogList.isEmpty())
                        {
                            if (!PosterApplication.getCurrentDate().equals(mTodayDate))
                            {
                                mpLogPath = PosterApplication.getLogFileFullPath(0, 0);
                                msLogPath = PosterApplication.getLogFileFullPath(1, 0);
                                mTodayDate = PosterApplication.getCurrentDate();
                            }
                            
                            FileUtils.writeSDFileData(msLogPath, mSlogList.removeFirst(), true);
                        }
                    }
                    
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }
}
