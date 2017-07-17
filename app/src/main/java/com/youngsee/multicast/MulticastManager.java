package com.youngsee.multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import com.youngsee.common.DbHelper;
import com.youngsee.logmanager.Logger;
import com.youngsee.posterdisplayer.PosterApplication;
import com.youngsee.screenmanager.ScreenManager;

import android.os.SystemClock;
import android.text.TextUtils;

public class MulticastManager
{
    private static MulticastManager INSTANCE                             = null;
    
    private static int              MULTICAST_DEFAULT_PORT               = 4999;                   // 组播地址port
    private static int              MULTICAST_DEFAULT_LOCAL_PORT         = 5888;                   // 本地socket port
    private static String           MULTICAST_DEFAULT_GROUP_IP           = "239.1.1.8";            // 组播地址 ip
                                                                                                    
    private static final int        DEFAULT_BUFFER_SIZE                  = 1024;   
    
    private InetAddress             mGroupAddress                        = null;
    private MulticastSocket         multicastSocket                      = null;
    private MulticastThread         mulThread                            = null;

	private int    mCurrentPort      = 0;
	private int    mCurrentLocalPort = 0;
	private String mCurrentGroupIp   = null;
	private int    mCurrentProgsync  = MulticastCommon.MC_VALUE_PROGSYNC_CLOSE; // 当前同步指示
    
	private MulticastSyncInfoRef     mSyncInfo                          = null;
	private long mRecvSyncInfoTime   = 0;
	
    private MulticastManager()
    {
    	obtainMulticastParam();
    }

    public static MulticastManager getInstance()
    {
    	if (INSTANCE == null)
        {
            INSTANCE = new MulticastManager();
        }
        return INSTANCE;
    }

    public int getCurrentSynIdicate()
    {
    	return mCurrentProgsync;
    }
    
    public int getCurrentPort()
    {
    	return mCurrentPort;
    }
    
    public int getCurrentLocalPort()
    {
    	return mCurrentLocalPort;
    }
    
    public String getCurrentGroupIp()
    {
    	return mCurrentGroupIp;
    }
    
    public MulticastSyncInfoRef getSyncInfo()
    {
    	return mSyncInfo;
    }
    
    public boolean isSyncPlay()
    {
    	return (mCurrentProgsync != MulticastCommon.MC_VALUE_PROGSYNC_CLOSE);
    }
    
    public boolean leaderIsValid()
    {
    	return (mCurrentProgsync == MulticastCommon.MC_VALUE_PROGSYNC_OPEN_GROUP_LEADER &&
    			PosterApplication.getInstance().isNetworkConnected());
    }
    
    public boolean memberIsValid()
    {
    	return (mCurrentProgsync == MulticastCommon.MC_VALUE_PROGSYNC_OPEN_GROUP_MEMBERS && 
    			PosterApplication.getInstance().isNetworkConnected() &&
    			!recvSyncInfoIsTimeOut());
    }
    
    private boolean isMultilGroupIp(String strIp)
    {
    	String strNum = strIp.substring(0, strIp.indexOf("."));
    	int num = Integer.valueOf(strNum);
    	return ((num & 0xf0) == 0xe0);
    }
    
    private void obtainMulticastParam()
    {
    	mCurrentProgsync = DbHelper.getInstance().getPgmSyncFlagFromDB();
    	
    	mCurrentGroupIp = DbHelper.getInstance().getBcastIpFromDB();
    	if (TextUtils.isEmpty(mCurrentGroupIp) || !isMultilGroupIp(mCurrentGroupIp))
        {
        	mCurrentGroupIp = MULTICAST_DEFAULT_GROUP_IP;
        }
    	
        mCurrentPort = DbHelper.getInstance().getBcastPortFromDB();
        if (mCurrentPort == 0)
        {
        	mCurrentPort = MULTICAST_DEFAULT_PORT;
        }
        
        mCurrentLocalPort = DbHelper.getInstance().getBcastLocalPortFromDB();
        if (mCurrentLocalPort == 0)
        {
        	mCurrentLocalPort = MULTICAST_DEFAULT_LOCAL_PORT;
        }  
    }
    
    private void createMulticastSocket()
    {
    	try 
    	{
    		if (multicastSocket != null)
            {
                multicastSocket.close();
                multicastSocket = null;
            }
    		
			mGroupAddress = InetAddress.getByName(mCurrentGroupIp);
			if (mCurrentProgsync == MulticastCommon.MC_VALUE_PROGSYNC_OPEN_GROUP_LEADER)
			{
			    multicastSocket = new MulticastSocket(mCurrentLocalPort);
	    	    multicastSocket.setLoopbackMode(true);
	    	    multicastSocket.setTimeToLive(128);
	            multicastSocket.joinGroup(mGroupAddress);
			}
			else if (mCurrentProgsync == MulticastCommon.MC_VALUE_PROGSYNC_OPEN_GROUP_MEMBERS)
			{
				multicastSocket = new MulticastSocket(mCurrentPort);
				multicastSocket.joinGroup(mGroupAddress);
			}
			else
			{
				Logger.i("Current sync indicate (" + mCurrentProgsync + ") error, didn't create socket.");
			}
		} 
    	catch (Exception e) 
    	{
    		if (multicastSocket != null)
            {
                multicastSocket.close();
                multicastSocket = null;
            }
			e.printStackTrace();
		}
    }
    
    public void restartWork()
    {
    	if (PosterApplication.getInstance().isNetworkConnected())
    	{
    	    Logger.i("------ restart sync play program --------");
    	    stopWork();
    	    startWork();
    	}
    	else
    	{
    		Logger.i("------ Ether Link is down, restart sync play program failed --------");
    	}
    }
    
    public void startWork()
    {
    	if (!PosterApplication.getInstance().isNetworkConnected())
    	{
    		Logger.i("------ Ether Link is down, start sync play program failed --------");
    		return;
    	}
    	
    	obtainMulticastParam();
    	createMulticastSocket();
    	
        if (mCurrentProgsync == MulticastCommon.MC_VALUE_PROGSYNC_OPEN_GROUP_MEMBERS)
        {
        	if (mulThread != null)
            {
                mulThread.setmRunflag(false);
                mulThread.interrupt();
                mulThread = null;
            }
        	
            mulThread = new MulticastThread();
            mulThread.start();
        }
        
        Logger.i("------ Start sync play program --------");
    }
    
    public void stopWork()
    {
        if (mulThread != null)
        {
            mulThread.setmRunflag(false);
            mulThread.interrupt();
            mulThread = null;
        }
        
        if (multicastSocket != null)
        {
            multicastSocket.close();
            multicastSocket = null;
        }
        
        mSyncInfo         = null;
    	mCurrentPort      = 0;
    	mCurrentLocalPort = 0;
    	mCurrentGroupIp   = null;
    	mCurrentProgsync  = MulticastCommon.MC_VALUE_PROGSYNC_CLOSE; 
    	
    	Logger.i("------ Stop sync play program --------");
    }
    
    public boolean recvSyncInfoIsTimeOut()
    {
    	if ((mCurrentProgsync == MulticastCommon.MC_VALUE_PROGSYNC_OPEN_GROUP_MEMBERS) &&
    		((SystemClock.uptimeMillis() - mRecvSyncInfoTime) > 60 * 1000))		
    	{
    		if (mulThread == null || !mulThread.isAlive() || multicastSocket == null || !multicastSocket.isConnected())
    		{
    			Logger.i("The receive thread or Socket is null, sync info is time out.");
    			restartWork();
    		}
    		return true;
    	}
    	return false;
    }
    
    public void sendSyncInfo(MulticastSyncInfoRef syncInfo)
    {
        try 
        {
        	// 获取同步信息包内容
        	byte syncFlag = syncInfo.SyncFlag;
        	byte[] pgmTypeBuff = intToByteArray(syncInfo.ProgramState);
        	byte[] pgmIdBuff = syncInfo.ProgramId.getBytes();
        	byte[] pgmCodeBuff = syncInfo.PgmVerifyCode.getBytes();
        	byte[] wndNameBuff = syncInfo.WndName.getBytes();
        	byte[] mediaNameBuff = syncInfo.MediaFullName.getBytes();
        	byte[] mediaSrcBuff = syncInfo.MediaSource.getBytes();
        	byte[] mediaCodeBuff = syncInfo.MediaVerifyCode.getBytes();
        	byte[] mediaPostionBuff = intToByteArray(syncInfo.MediaPosition);
        	
        	// 封装信息包
        	int idx = 0;
        	int bInfoLen = MulticastCommon.MC_BYTES_BEFORE_LEN + pgmTypeBuff.length + pgmIdBuff.length + pgmCodeBuff.length + wndNameBuff.length 
        			       + mediaNameBuff.length + mediaSrcBuff.length + mediaCodeBuff.length + mediaPostionBuff.length;
        	byte[] binfo = new byte[bInfoLen];
        	
        	if (idx < bInfoLen)
        	{
        	    binfo[idx++] = syncFlag;
        	}
        	
        	if ((idx + pgmTypeBuff.length + 2) < bInfoLen)
        	{
        	    binfo[idx++] = MulticastCommon.MC_FILED_PROGRAMTYPE_ID;
        	    binfo[idx++] = (byte)pgmTypeBuff.length;
        	    System.arraycopy(pgmTypeBuff, 0, binfo, idx, pgmTypeBuff.length);
        	    idx += pgmTypeBuff.length;
        	}
        	
        	if ((idx + pgmIdBuff.length + 2) < bInfoLen)
        	{
        	    binfo[idx++] = MulticastCommon.MC_FILED_PROGRAMID_ID;
        	    binfo[idx++] = (byte)pgmIdBuff.length;
        	    System.arraycopy(pgmIdBuff, 0, binfo, idx, pgmIdBuff.length);
        	    idx += pgmIdBuff.length;
        	}
        	
        	if ((idx + pgmCodeBuff.length + 2) < bInfoLen)
        	{
        	    binfo[idx++] = MulticastCommon.MC_FILED_PGM_VERIFYCODE_ID;
        	    binfo[idx++] = (byte)pgmCodeBuff.length;
        	    System.arraycopy(pgmCodeBuff, 0, binfo, idx, pgmCodeBuff.length);
        	    idx += pgmCodeBuff.length;
        	}
        	
        	if ((idx + wndNameBuff.length + 2) < bInfoLen)
        	{
        	    binfo[idx++] = MulticastCommon.MC_FILED_WND_NAME_ID;
        	    binfo[idx++] = (byte)wndNameBuff.length;
        	    System.arraycopy(wndNameBuff, 0, binfo, idx, wndNameBuff.length);
        	    idx += wndNameBuff.length;
        	}
        	
        	if ((idx + mediaNameBuff.length + 2) < bInfoLen)
        	{
        	    binfo[idx++] = MulticastCommon.MC_FILED_MEDIA_FULLNAME_ID;
        	    binfo[idx++] = (byte)mediaNameBuff.length;
        	    System.arraycopy(mediaNameBuff, 0, binfo, idx, mediaNameBuff.length);
        	    idx += mediaNameBuff.length;
        	}
        	
        	if ((idx + mediaSrcBuff.length + 2) < bInfoLen)
        	{
        	    binfo[idx++] = MulticastCommon.MC_FILED_MEDIA_SOURCE_ID;
        	    binfo[idx++] = (byte)mediaSrcBuff.length;
        	    System.arraycopy(mediaSrcBuff, 0, binfo, idx, mediaSrcBuff.length);
        	    idx += mediaSrcBuff.length;
        	}
        	
        	if ((idx + mediaCodeBuff.length + 2) < bInfoLen)
        	{
        	    binfo[idx++] = MulticastCommon.MC_FILED_MEDIA_CODE_ID;
        	    binfo[idx++] = (byte)mediaCodeBuff.length;
        	    System.arraycopy(mediaCodeBuff, 0, binfo, idx, mediaCodeBuff.length);
        	    idx += mediaCodeBuff.length;
        	}
        	
        	if ((idx + mediaPostionBuff.length + 2) < bInfoLen)
        	{
        	    binfo[idx++] = MulticastCommon.MC_FILED_MEDIA_POSITION_ID;
        	    binfo[idx++] = (byte)mediaPostionBuff.length;
        	    System.arraycopy(mediaPostionBuff, 0, binfo, idx, mediaPostionBuff.length);
        	    idx += mediaPostionBuff.length;
        	}
        	Logger.i("sync leader send sync info: " + binfo.toString());
        	
        	if (multicastSocket == null)
        	{
        		obtainMulticastParam();
            	createMulticastSocket();
            	Logger.i("------ sendSyncInfo(), socket is null, create it.  --------");
        	}
        	
        	// 发送同步信息包
        	if (multicastSocket != null)
        	{
        	    DatagramPacket packet = new DatagramPacket(binfo, binfo.length, mGroupAddress, mCurrentPort);
			    multicastSocket.send(packet);
        	}
		} 
        catch (IOException e) 
        {
			e.printStackTrace();
			restartWork();
		}
    }

    public class MulticastThread extends Thread
    {
        private boolean         mRunflag        = true;
        
        public MulticastThread()
        {
        }
        
        private void handleSyncInfo(byte[] buffer, int length)
        {
        	if (length <= 0)
        	{
        		return;
        	}

        	if (mSyncInfo == null)
        	{
        		mSyncInfo = new MulticastSyncInfoRef();
        	}
        	
        	// 解析数据
        	int idx = 0;
        	byte syncFieldId = 0;
        	byte syncFieldLen = 0;
        	mSyncInfo.SyncFlag = buffer[idx++];
        	while (idx < length)
        	{
        		syncFieldId = buffer[idx++];
        		syncFieldLen = buffer[idx++];
        		switch (syncFieldId)
        		{
        		case MulticastCommon.MC_FILED_PROGRAMTYPE_ID:
        			mSyncInfo.ProgramState = byteArrayToInt(buffer, syncFieldLen, idx);
        			break;
        			
        		case MulticastCommon.MC_FILED_PROGRAMID_ID:
        			mSyncInfo.ProgramId = byteArrayToString(buffer, syncFieldLen, idx);
        			break;
        			
        		case MulticastCommon.MC_FILED_PGM_VERIFYCODE_ID:
        			mSyncInfo.PgmVerifyCode = byteArrayToString(buffer, syncFieldLen, idx);
        			break;
        			
        		case MulticastCommon.MC_FILED_WND_NAME_ID:
        			mSyncInfo.WndName = byteArrayToString(buffer, syncFieldLen, idx);
        			break;
        			
        		case MulticastCommon.MC_FILED_MEDIA_FULLNAME_ID:
        			mSyncInfo.MediaFullName = byteArrayToString(buffer, syncFieldLen, idx);
        			break;
        			
        		case MulticastCommon.MC_FILED_MEDIA_SOURCE_ID:
        			mSyncInfo.MediaSource = byteArrayToString(buffer, syncFieldLen, idx);
        			break;
        			
        		case MulticastCommon.MC_FILED_MEDIA_CODE_ID:
        			mSyncInfo.MediaVerifyCode = byteArrayToString(buffer, syncFieldLen, idx);
        			break;
        			
        		case MulticastCommon.MC_FILED_MEDIA_POSITION_ID:
        			mSyncInfo.MediaPosition = byteArrayToInt(buffer, syncFieldLen, idx);
        			break;
        		}
        		idx += syncFieldLen;
        	}
        	
        	// 通知同步操作
        	ScreenManager.getInstance().syncPlayProgram(mSyncInfo);
        }
        
        public void setmRunflag(boolean isrun)
        {
            mRunflag = isrun;
        }
        
        @Override
        public void run()
        {
            Logger.i("New Multicast sync receive thread, id is: " + currentThread().getId());
            
            try 
			{
                DatagramPacket recvDataPacket = null;
        	    byte[] receiveData = new byte[DEFAULT_BUFFER_SIZE];
			    while (mRunflag && (mCurrentProgsync == MulticastCommon.MC_VALUE_PROGSYNC_OPEN_GROUP_MEMBERS)) 
			    {
			    	if (multicastSocket == null)
	            	{
	            		obtainMulticastParam();
	                	createMulticastSocket();
	                	Logger.i("------ MulticastThread(), socket is null, create it.  --------");
	            	}

			    	if (multicastSocket != null)
			    	{
						Arrays.fill(receiveData, (byte) 0);
						recvDataPacket = new DatagramPacket(receiveData, receiveData.length);
						multicastSocket.receive(recvDataPacket); // 接收数据，进入阻塞状态

						Logger.i("sync group member recv sync info: " + receiveData.toString());
						
					    mRecvSyncInfoTime = SystemClock.uptimeMillis();
					    handleSyncInfo(receiveData, recvDataPacket.getLength());
			    	}
			    	
					Thread.sleep(100);
				} 
			}
			catch (InterruptedException ite) 
			{
				Logger.i("sync receive thread sleep over, and safe exit, the Thread id is: " + currentThread().getId());					
			} 
			catch (Exception ex) 
			{
				Logger.e("mulThread  Catch a error.");
				ex.printStackTrace();
			} 
        }
    }

    /**
     * 字节数组转换为字符串
     * 
     * @param buffer
     * @param length
     *            长度
     * @param start
     *            起始下标
     * @return
     */
    private String byteArrayToString(byte[] buffer, int length, int start)
    {
        byte[] bs = new byte[length];
        for (int a = 0; a < length; a++)
        {
            bs[a] = buffer[a + start];
        }
        return new String(bs);
    }

    /**
     * byte array to int
     * 
     * @param buffer
     * @param length
     *            指定长度
     * @param start
     *            数组开始下标位
     * @return
     */
    private int byteArrayToInt(byte[] buffer, int length, int start)
    {
        byte[] bs = new byte[length];
        for (int a = 0; a < length; a++)
        {
            bs[a] = buffer[a + start];
        }
        return byteArrayToInt(bs);
    }
    
    /**
     * byte[]转int
     * 
     * @param bytes
     * @return
     */
    private int byteArrayToInt(byte[] bytes)
    {
        int value = 0;
        int shift = 0;
        
        // 由高位到低位
        for (int i = 0; i < 4; i++)
        {
            shift = (4 - 1 - i) * 8;
            value += (bytes[i] & 0x000000FF) << shift;// 往高位游
        }
        return value;
    }
    
    /**
     * int到byte[]
     * 
     * @param i
     * @return
     */
    private byte[] intToByteArray(int i)
    {
        byte[] result = new byte[4];
        // 由高位到低位
        result[0] = (byte) ((i >> 24) & 0xFF);
        result[1] = (byte) ((i >> 16) & 0xFF);
        result[2] = (byte) ((i >> 8) & 0xFF);
        result[3] = (byte) (i & 0xFF);
        return result;
    }
    
    /**
     * byte array to long
     * 
     * @param buffer
     * @param length
     *            指定长度
     * @param start
     *            数组开始下标位
     * @return
     */
    @SuppressWarnings("unused")
	private long byteArrayToLong(byte[] buffer, int length, int start)
    {
        byte[] bs = new byte[length];
        for (int a = 0; a < length; a++)
        {
            bs[a] = buffer[a + start];
        }
        return byteArrayToLong(bs);
    }
    
    // long类型转成byte数组
    @SuppressWarnings("unused")
	private byte[] longToByteArray(long number)
    {
        long temp = number;
        byte[] b = new byte[8];
        for (int i = 0; i < b.length; i++)
        {
            b[i] = Long.valueOf(temp & 0xff).byteValue(); // 将最低位保存在最低位
            temp = temp >> 8; // 向右移8位
        }
        return b;
    }
    
    // byte数组转成long
    public static long byteArrayToLong(byte[] b)
    {
        long s = 0;
        long s0 = b[0] & 0xff;// 最低位
        long s1 = b[1] & 0xff;
        long s2 = b[2] & 0xff;
        long s3 = b[3] & 0xff;
        long s4 = b[4] & 0xff;// 最低位
        long s5 = b[5] & 0xff;
        long s6 = b[6] & 0xff;
        long s7 = b[7] & 0xff;
        
        // s0不变
        s1 <<= 8;
        s2 <<= 16;
        s3 <<= 24;
        s4 <<= 8 * 4;
        s5 <<= 8 * 5;
        s6 <<= 8 * 6;
        s7 <<= 8 * 7;
        s = s0 | s1 | s2 | s3 | s4 | s5 | s6 | s7;
        return s;
    }
}