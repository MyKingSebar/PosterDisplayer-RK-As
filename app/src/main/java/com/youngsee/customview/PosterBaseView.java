/*
 * Copyright (C) 2013 poster PCE YoungSee Inc. All Rights Reserved Proprietary and Confidential.
 * 
 * @author LiLiang-Ping
 */

package com.youngsee.customview;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.youngsee.common.FileUtils;
import com.youngsee.common.Md5;
import com.youngsee.common.MediaInfoRef;
import com.youngsee.common.SysParamManager;
import com.youngsee.common.TypefaceManager;
import com.youngsee.ftpoperation.FtpFileInfo;
import com.youngsee.ftpoperation.FtpHelper;
import com.youngsee.logmanager.Logger;
import com.youngsee.multicast.MulticastCommon;
import com.youngsee.multicast.MulticastManager;
import com.youngsee.multicast.MulticastSyncInfoRef;
import com.youngsee.posterdisplayer.PosterApplication;
import com.youngsee.screenmanager.ScreenManager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

public abstract class PosterBaseView extends FrameLayout {
    protected Context            mContext      = null;
    
    // View attributes
    protected int                mXPos         = 0;
    protected int                mYPos         = 0;
    protected int                mWidth        = 0;
    protected int                mHeight       = 0;
    protected String             mViewName     = "";
    protected String             mViewType     = "";
    
    // The Medias need to play for the View
    protected int                mCurrentIdx   = -1;
    protected MediaInfoRef       mCurrentMedia = null;
    protected List<MediaInfoRef> mMediaList    = null;

    protected long  DEFAULT_THREAD_PERIOD      = 1000;
    protected long  DEFAULT_THREAD_QUICKPERIOD = 100;

    protected boolean mIsSyncLoadMedia      = false;
    protected boolean mIsSyncPlayMedia      = false;
    protected boolean mIsSyncStartPlayVideo = false;
    
    protected String mSyncMediaName    = null;
    protected String mSyncMediaSrc     = null;
    protected String mSyncMediaVryCode = null;
    protected int    mSyncMediaPos     = 0;
    
    public PosterBaseView(Context context) {
        super(context);
        mContext = context;
    }

    public PosterBaseView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public PosterBaseView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    public void setViewName(String viewName)
    {
        mViewName = viewName;
    }
    
    public void setViewType(String viewType)
    {
        mViewType = viewType;
    }
    
    public void setViewPosition(int xPos, int yPos)
    {
        mXPos = xPos;
        mYPos = yPos;
        this.setX(xPos);
        this.setY(yPos);
    }
    
    public void setViewSize(int nWidth, int nHeight)
    {
        mWidth = nWidth;
        mHeight = nHeight;
        this.setLayoutParams(new FrameLayout.LayoutParams(nWidth, nHeight));
    }
    
    public void setMediaList(List<MediaInfoRef> lst) 
    {
        mMediaList = lst;
    }
    
    public String getViewName()
    {
        return mViewName;
    }
    
    public String getViewType()
    {
        return mViewType;
    }
    
    public int getXPos()
    {
        return mXPos;
    }
    
    public int getYPos()
    {
        return mYPos;
    }
    
    public int getViewWidth()
    {
        return mWidth;
    }
    
    public int getViewHeight()
    {
        return mHeight;
    }
    
    public List<MediaInfoRef> getMediaList() 
    {
        return mMediaList;
    }
    
    public static boolean md5IsCorrect(MediaInfoRef media)
    {
    	if (media.filePath.equals(PosterApplication.getStandbyScreenImgPath()))
    	{
    		// 待机画面不做MD5校验
    		return true;
    	}
    	
        String md5Value = new Md5(media.md5Key).ComputeFileMd5(media.filePath);
        
        if (md5Value != null && md5Value.equals(media.verifyCode))
        {
            return true;
        }
        
        return false;
    }
    
    public static void downloadMedia(MediaInfoRef media)
    {
        if (media != null)
        {
            if (FileUtils.isExist(media.filePath) && md5IsCorrect(media))
            {
                Log.i("downloadMedia()", "Media has been existing. No need to download.");
                return;
            }
            
            FtpFileInfo ftpFileInfo = new FtpFileInfo();
            ftpFileInfo.setRemotePath(media.remotePath);
            ftpFileInfo.setVerifyCode(media.verifyCode);
            ftpFileInfo.setVerifyKey(media.md5Key);
            ftpFileInfo.setLocalPath(FileUtils.getFileAbsolutePath(media.filePath));
            if (FtpHelper.getInstance().materialIsOnDownload(ftpFileInfo))
            {
                Log.i("downloadMedia()", "Media is being downloaded. Skip it...");
                return;
            }

            LinkedList<FtpFileInfo> downloadlst = new LinkedList<FtpFileInfo>();
            downloadlst.add(ftpFileInfo);
            if (FtpHelper.getInstance().ftpDownloadIsWorking())
            {
                FtpHelper.getInstance().addMaterialsToDlQueue(downloadlst, media.isIgnoreDlLimit);
            }
            else
            {
                FtpHelper.getInstance().startDownloadPgmMaterials(downloadlst, media.isIgnoreDlLimit, null);
            }
        }
    }
    
    public static BitmapFactory.Options setBitmapOption(final MediaInfoRef picInfo)
    {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        
        InputStream in = createImgInputStream(picInfo);
        if (in != null)
        {
            // 获取图片实际大小
            opt.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, opt);
            
            int outWidth = opt.outWidth;
            int outHeight = opt.outHeight;
            int nWndWidth = picInfo.containerwidth;
            int nWndHeight = picInfo.containerheight;
            
            // 设置压缩比例
            opt.inSampleSize = 1;
            if (outWidth > nWndWidth || outHeight > nWndHeight)
            {
                opt.inSampleSize = (outWidth / nWndWidth + outHeight / nWndHeight) / 2;
            }
            opt.inPreferredConfig = Bitmap.Config.RGB_565;
            opt.inDither = false;
            opt.inJustDecodeBounds = false;
            
            // 关闭输入�?
            try
            {
                in.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        
        return opt;
    }
    
    public static InputStream createImgInputStream(final MediaInfoRef picInfo)
    {
        InputStream is = null;
        
        try
        {
            if (FileUtils.mediaIsGifFile(picInfo) || FileUtils.mediaIsPicFromFile(picInfo) || FileUtils.mediaIsTextFromFile(picInfo))
            {
                String strFileName = picInfo.filePath;
                if (FileUtils.isExist(strFileName))
                {
                    is = new FileInputStream(strFileName);
                    FileUtils.updateFileLastTime(picInfo.filePath);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        return is;
    }
    
    // Abstract functions
    public abstract void onViewDestroy();
    public abstract void onViewPause();
    public abstract void onViewResume();
    public abstract void startWork();
    public abstract void stopWork();

    protected boolean mediaTimeIsValid(MediaInfoRef media)
    {
        if (media.starttime == null || media.endtime == null)
        {
            return true;
        }
        
        int startSeconds = PosterApplication.getSecondsInDayByTime(media.starttime);
        int endSeconds = PosterApplication.getSecondsInDayByTime(media.endtime);
        if ((startSeconds == -1) || (endSeconds == -1))
        {
            return true;
        }
        
        int currentSeconds = PosterApplication.getCurrentSecondsInDay();
        
        return (currentSeconds >= startSeconds) && (currentSeconds <= endSeconds);
    }
    
    protected String getText(final MediaInfoRef txtInfo)
    {
        String strText = null;
        if (FileUtils.mediaIsTextFromFile(txtInfo))
        {
            strText = readTextFile(txtInfo.filePath);
        }
        else if (FileUtils.mediaIsTextFromNet(txtInfo))
        {
            strText = downloadText(txtInfo.filePath);
        }
        return strText;
    }
    
    protected String downloadText(String urlPath)
    {
        if (!PosterApplication.getInstance().isNetworkConnected())
        {
            Logger.i("Net link is down, can't dowload the Text.");
            return "";
        }
        
        InputStream in = null;
        BufferedReader br = null;
        StringBuffer sb = new StringBuffer();
        try
        {
            String line = "";
            URL url = new URL(urlPath);
            in = url.openStream();
            br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            while ((line = br.readLine()) != null)
            {
                sb.append(line + "\n");
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (in != null)
                {
                    in.close();
                }
                
                if (br != null)
                {
                    br.close();
                }
            }
            catch (Exception e)
            {
                
            }
        }
        
        return sb.toString();
    }
    
    protected String readTextFile(String filePath)
    {
        FileUtils.updateFileLastTime(filePath);
        String dest = "";
        InputStream is = null;
        BufferedReader reader = null;
        try
        {
            String str = "";
            StringBuffer sb = new StringBuffer();
            is = new FileInputStream(filePath);
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            while ((str = reader.readLine()) != null)
            {
                sb.append(str + "\n");
            }
            
            // 去掉非法字符
            Pattern p = Pattern.compile("(\ufeff)");
            Matcher m = p.matcher(sb.toString());
            dest = m.replaceAll("");
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (is != null)
                {
                    is.close();
                }
                
                if (reader != null)
                {
                    reader.close();
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        
        return dest;
    }
    
    /**
     * 自动分割文本
     * 
     * @param content
     *            需要分割的文本
     * @param p
     *            画笔，用来根据字体测量文本的宽度
     * @param width
     *            最大的可显示像素（一般为控件的宽度）
     * @return 一个字符串数组，保存每行的文本
     */
    protected ArrayList<String> autoSplit(String content, Paint p, float width)
    {
        String strText = StringFilter(content);
        ArrayList<String> retList = new ArrayList<String>();
        if (strText == null)
        {
            return retList;
        }
        
        char ch = 0;
        int w = 0;
        int istart = 0;
        int length = strText.length();
        float[] widths = new float[1];
        for (int i = 0; i < length; i++)
        {
            ch = strText.charAt(i);
            p.getTextWidths(String.valueOf(ch), widths);
            if (ch == '\n')
            {
                retList.add(strText.substring(istart, i));
                istart = i + 1;
                w = 0;
            }
            else
            {
                w += (int) Math.ceil(widths[0]);
                if (w > width)
                {
                    retList.add(strText.substring(istart, i));
                    istart = i;
                    i--;
                    w = 0;
                }
                else
                {
                    if (i == length - 1)
                    {
                        retList.add(strText.substring(istart));
                    }
                }
            }
        }
        
        return retList;
    }
    
    // 替换、过滤特殊字符
    protected String StringFilter(String str)
    {
        if (str == null)
        {
            return null;
        }
        
        str = str.replaceAll("【", "[").replaceAll("】", "]").replaceAll("！", "!");// 替换中文标号
        String regEx = "[『』]"; // 清除掉特殊字符
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.replaceAll("").trim();
    }

    protected int getFontSize(final MediaInfoRef txtInfo)
    {
        int nFontSize = 0;
        if (txtInfo.fontSize != null)
        {
            nFontSize = Integer.parseInt(txtInfo.fontSize);
        }
        else
        {
            nFontSize = 50;
        }
        return nFontSize;
    }
    
    protected int getFontColor(final MediaInfoRef txtInfo)
    {
        int nFontColor = 0;
        if (txtInfo.fontColor != null)
        {
            nFontColor = PosterApplication.stringHexToInt(txtInfo.fontColor);
        }
        else
        {
            nFontColor = Color.WHITE;
        }
        return (nFontColor | 0xFF000000);
    }
    
    protected Typeface getFont(final MediaInfoRef txtInfo)
    {
        Typeface typeface = null;
        TypefaceManager typefaceManager = new TypefaceManager(mContext);
        if (txtInfo.fontName != null)
        {
            typeface = typefaceManager.getTypeface(txtInfo.fontName);
        }
        else
        {
            typeface = typefaceManager.getTypeface(TypefaceManager.DEFAULT);
        }
        return typeface;
    }
    
    protected Bitmap getBitMap(final MediaInfoRef picInfo, boolean isUseCacheForNetPic)
    {
        Bitmap retBmp = null;
        if (!isUseCacheForNetPic && FileUtils.mediaIsPicFromNet(picInfo))
        {
            retBmp = loadNetPicture(picInfo, isUseCacheForNetPic);
        }
        else
        {
            String strKey = getImgCacheKey(picInfo);
            if ((retBmp = PosterApplication.getBitmapFromMemoryCache(strKey)) == null)
            {
                if (FileUtils.mediaIsPicFromNet(picInfo))
                {
                    // Check whether the picture save in disk cache.
                    // if so, then load the picture to memory cache and show it.
                    if ((retBmp = PosterApplication.getBitmapFromDiskCache(strKey)) != null)
                    {
                        PosterApplication.addBitmapToMemoryCache(strKey, retBmp);
                    }
                    else
                    {
                        retBmp = loadNetPicture(picInfo, isUseCacheForNetPic);
                    }
                }
                else if (FileUtils.mediaIsPicFromFile(picInfo))
                {
                    retBmp = loadLocalPicture(picInfo);
                }
            }
        }
        
        return retBmp;
    }
    
    protected Bitmap loadNetPicture(final MediaInfoRef picInfo, boolean isUseCache)
    {
        String newFilePath;
        if (picInfo.filePath.contains("$MAC$"))
        {
            newFilePath = picInfo.filePath.replace("$MAC$", PosterApplication.getEthMacStr());
        }
        else if (picInfo.filePath.contains("$TERMNAME$"))
        {
            String termNames = SysParamManager.getInstance().getTerm();
            newFilePath = picInfo.filePath.replace("$TERMNAME$", termNames);
        }
        else if (picInfo.filePath.contains("$TERMGRP$"))
        {
            String termGrps = SysParamManager.getInstance().getTermGrp();
            newFilePath = picInfo.filePath.replace("$TERMGRP$", termGrps);
        }
        else
        {
            newFilePath = picInfo.filePath;
        }
        Bitmap srcBmp = downloadBitmap(newFilePath);
        
        // Save the bitmap to cache
        if (isUseCache && srcBmp != null)
        {
            String strKey = getImgCacheKey(picInfo);
            PosterApplication.addBitmapToMemoryCache(strKey, srcBmp);
            PosterApplication.addBitmapToDiskCache(strKey, srcBmp);
        }
        
        return srcBmp;
    }
    
    protected Bitmap loadLocalPicture(final MediaInfoRef picInfo)
    {
        Bitmap srcBmp = null;
        
        try
        {
            if (picInfo == null || FileUtils.mediaIsPicFromNet(picInfo))
            {
                Log.e("load picture error", "picture info is error");
                return null;
            }
            
            // Create the Stream
            InputStream isImgBuff = createImgInputStream(picInfo);
            if (isImgBuff == null)
            {
                return null;
            }
            
            try
            {
                // Create the bitmap for BitmapFactory
                srcBmp = BitmapFactory.decodeStream(isImgBuff, null, setBitmapOption(picInfo));
            }
            catch (java.lang.OutOfMemoryError e)
            {
                Logger.e("picture is too big, out of memory!");
                
                if (srcBmp != null && !srcBmp.isRecycled())
                {
                    srcBmp.recycle();
                    srcBmp = null;
                }
                
                System.gc();
            }
            finally
            {
                if (isImgBuff != null)
                {
                    isImgBuff.close();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        // Will be stored the new image to LruCache
        if (srcBmp != null)
        {
            PosterApplication.addBitmapToMemoryCache(getImgCacheKey(picInfo), srcBmp);
        }

        return srcBmp;
    }
    
    protected Bitmap downloadBitmap(String imageUrl)
    {
        if (!PosterApplication.getInstance().isNetworkConnected())
        {
            Logger.i("Net link is down, can't dowload the bitmap.");
            return null;
        }
        
        Bitmap bitmap = null;
        HttpURLConnection con = null;
        InputStream in = null;
        System.setProperty("http.keepAlive", "false");
        try
        {
            URL url = new URL(imageUrl);
            con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(60 * 1000);
            con.setReadTimeout(90 * 1000);
            con.setDoInput(true);
            in = con.getInputStream();
            bitmap = BitmapFactory.decodeStream(in);
        }
        catch (Exception e)
        {
            Logger.e("load net picture fail");
            e.printStackTrace();
        }
        finally
        {
            if (in != null)
            {
                try
                {
                    in.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            
            if (con != null)
            {
                con.disconnect();
            }
        }
        
        return bitmap;
    }
    
    protected String getImgCacheKey(MediaInfoRef pInfo)
    {
        String retKey = null;
        if (TextUtils.isEmpty(mViewName))
        {
            retKey = pInfo.filePath;
        }
        else
        {
            StringBuilder sb = new StringBuilder();
            sb.append(mViewName).append(File.separator).append(pInfo.filePath);
            retKey = sb.toString();
        }
        return retKey;
    }
    
    protected boolean noMediaValid()
    {
        if (mMediaList != null)
        {
            for (MediaInfoRef mediaInfo : mMediaList)
            {
            	if (mediaInfo.filePath.equals(PosterApplication.getStandbyScreenImgPath()))
            	{
            		// 加载待机画面不需要显示ProgressBar
            		return false;
            	}
            	else if (FileUtils.mediaIsFile(mediaInfo) && 
                    FileUtils.isExist(mediaInfo.filePath) && 
                    md5IsCorrect(mediaInfo) && 
                    mediaTimeIsValid(mediaInfo))
                {              
                    return false;
                }
            }
        }
        return true;
    }
    
    protected MediaInfoRef findNextMedia()
    {
        if (mMediaList == null)
        {
            Logger.i("View [" + mViewName + "] Media list is null.");
            return null;
        }
        
        if (mMediaList.isEmpty())
        {
            Logger.i("View [" + mViewName + "] No media in the list.");
            return null;
        }
        
        if (++mCurrentIdx >= mMediaList.size())
        {
            mCurrentIdx = 0;
            if (mViewType.contains("Main") && materaialsIsAllShow())
            {
                ScreenManager.getInstance().setPrgFinishedFlag(true);
            }
        }
        
        return mMediaList.get(mCurrentIdx);
    }
    
    protected MediaInfoRef findSyncMedia()
    {
    	if (mMediaList == null)
        {
            Logger.i("View [" + mViewName + "] Media list is null.");
            return null;
        }
        
        if (mMediaList.isEmpty())
        {
            Logger.i("View [" + mViewName + "] No media in the list.");
            return null;
        }
        
        if (this.mSyncMediaPos < mMediaList.size())
        {
        	MediaInfoRef media = mMediaList.get(mSyncMediaPos);
        	if ("File".equals(this.mSyncMediaSrc))
        	{
        		if (media.verifyCode.equals(this.mSyncMediaVryCode))
        		{
        			mCurrentIdx = mSyncMediaPos;
        			return media;
        		}
        	}
        	else
        	{
        		if (media.filePath.equals(this.mSyncMediaName))
        		{
        			mCurrentIdx = mSyncMediaPos;
        			return media;
        		}
        	}
        }
        
        mCurrentIdx = -1;
        for (MediaInfoRef mediaInfo : mMediaList)
        {
        	if ("File".equals(this.mSyncMediaSrc))
        	{
        		if (mediaInfo.verifyCode.equals(this.mSyncMediaVryCode))
        		{
        			return mediaInfo;
        		}
        	}
        	else
        	{
        		if (mediaInfo.filePath.equals(this.mSyncMediaName))
        		{
        			return mediaInfo;
        		}
        	}
        	mCurrentIdx++;
        }
        
        if (mCurrentIdx >= mMediaList.size())
        {
        	mCurrentIdx = 0;
        }
        
        return null;
    }
    
    protected MediaInfoRef findNextOrSyncMedia() throws InterruptedException
    {
    	MediaInfoRef retMedia = null;
    	
    	// 若是同步播放，且该终端为组员，则等待组长通知加载素材
    	if (MulticastManager.getInstance().memberIsValid())
    	{
    		while (!mIsSyncLoadMedia && !mIsSyncPlayMedia && mIsSyncStartPlayVideo && !MulticastManager.getInstance().recvSyncInfoIsTimeOut())
    		{
    			Thread.sleep(100);
    		}
    		retMedia = findSyncMedia();
    	}
    	else
    	{
    		retMedia = findNextMedia();
    	}
    	
    	return retMedia;
    }
    
    protected void syncLoadMedia() throws InterruptedException
    {
    	if (MulticastManager.getInstance().leaderIsValid()) 
		{
			// 该终端为组长，则通知其他组员开始加载素材
			sendMediaSyncInfo(MulticastCommon.MC_SYNCFLAG_LOAD_MEDIA, mCurrentIdx);
			// 发送同步命令网络时延
			if (MulticastCommon.DEFAULT_SYNC_TIMES_DIFFERENCE > 0) 
			{
				Thread.sleep(MulticastCommon.DEFAULT_SYNC_TIMES_DIFFERENCE);
			}
		} 
		else if (MulticastManager.getInstance().memberIsValid()) 
		{
			// 组员本次加载完成, 复位标志
			mIsSyncLoadMedia = false;
		}
    }
    
    protected void syncPlayMedia() throws InterruptedException
    {
    	if (MulticastManager.getInstance().leaderIsValid())
        {
        	// 该终端为组长，则通知其他组员开始播放素材
        	sendMediaSyncInfo(MulticastCommon.MC_SYNCFLAG_PLAY_MEDIA, mCurrentIdx);
        	// 发送同步命令网络时延
        	if (MulticastCommon.DEFAULT_SYNC_TIMES_DIFFERENCE > 0)
    	    {
    	        Thread.sleep(MulticastCommon.DEFAULT_SYNC_TIMES_DIFFERENCE);
    	    } 
        }
        else if (MulticastManager.getInstance().memberIsValid())
        {
        	// 该终端为组员，则等待组长通知, 超过1秒后播放
        	int i  = 0;
    	    while (!mIsSyncPlayMedia && i++ < 10)
    	    {
    	    	Thread.sleep(100);
    	    }
    	    mIsSyncPlayMedia = false;
        }
    }
    
    protected void sendMediaSyncInfo(byte syncFlag, int postion)
    {
    	if (mCurrentMedia == null)
    	{
    		return;
    	}
    	
        MulticastSyncInfoRef syncInfo = new MulticastSyncInfoRef();
        syncInfo.SyncFlag = syncFlag;
        syncInfo.ProgramState = ScreenManager.getInstance().getStatus();
        syncInfo.ProgramId = ScreenManager.getInstance().getPlayingPgmId();
        syncInfo.PgmVerifyCode = ScreenManager.getInstance().getPlayingPgmVerifyCode();
        syncInfo.WndName = mViewName;
        syncInfo.MediaFullName = mCurrentMedia.filePath;
        syncInfo.MediaSource = mCurrentMedia.source;
        syncInfo.MediaVerifyCode = (mCurrentMedia.verifyCode == null) ? "0" : mCurrentMedia.verifyCode;
        syncInfo.MediaPosition = postion;
        MulticastManager.getInstance().sendSyncInfo(syncInfo);
    }
    
    public void recvSyncPlayInfo(MulticastSyncInfoRef syncInfo)
    {
    	this.mSyncMediaName = syncInfo.MediaFullName;
	    this.mSyncMediaPos = syncInfo.MediaPosition;
	    this.mSyncMediaSrc = syncInfo.MediaSource;
	    this.mSyncMediaVryCode = syncInfo.MediaVerifyCode;
	    
    	switch (syncInfo.SyncFlag)
		{
	    case MulticastCommon.MC_SYNCFLAG_LOAD_MEDIA:
		    this.mIsSyncLoadMedia = true;
		    break;
		
	    case MulticastCommon.MC_SYNCFLAG_PLAY_MEDIA:    	
		    this.mIsSyncPlayMedia = true;
		    break;
		    
	    case MulticastCommon.MC_SYNCFLAG_START_PLAY_VIDEO:
	    	this.mIsSyncStartPlayVideo = true;
	    	break;
		}
    }
    
    protected boolean syncMediaIsSame(MediaInfoRef mediaInfo)
    {
    	if ("File".equals(this.mSyncMediaSrc))
    	{
    		if (mediaInfo.verifyCode.equals(this.mSyncMediaVryCode))
    		{
    			return true;
    		}
    	}
    	else
    	{
    		if (mediaInfo.filePath.equals(this.mSyncMediaName))
    		{
    			return true;
    		}
    	}
    	
    	return false;
    }
    
    private boolean materaialsIsAllShow()
    {
        for (MediaInfoRef media : mMediaList)
        {
            if (media.playedtimes <= 0)
            {
                return false;
            }
        }
        return true;
    }
}
