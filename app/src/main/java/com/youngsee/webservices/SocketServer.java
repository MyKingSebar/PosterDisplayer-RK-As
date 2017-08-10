package com.youngsee.webservices;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.youngsee.common.Contants;
import com.youngsee.common.FileUtils;
import com.youngsee.posterdisplayer.PosterApplication;
import com.youngsee.posterdisplayer.PosterMainActivity;
import com.youngsee.posterdisplayer.R;
import com.youngsee.socket.FtpSocket;
import com.youngsee.socket.MusicServiceHelper;
import com.youngsee.socket.NotifyInfo;
import com.youngsee.socket.PatientQueue;
import com.youngsee.socket.VoiceQueue;


import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Administrator on 2017/7/19.
 */

public class SocketServer {

    private Context mContext = null;
    private static SocketServer mSSInstance = null;
    public static ServerSocket serversocket = null;
    public static String TAG = "PD SocketServer";
    public static int Port = 20001;
    private static final int ERROR_TYPE = -1;
    private static final int VOICE_TYPE = 0;
    private static final int PATIENT_TYPE = 1;
    private static final int NOTIFY_TYPE = 2;
    public int InfoType = -1;

    public static boolean isLoadingPatientProgram = false;
    public static boolean isLoadingNotifyProgram  = false;
    public static boolean isLoadingVoiceProgram   = false;

    public static DatagramSocket socket = null;

    //Thread
    public  ServerThread mServersocket = null;
    public  LoadPatientThread mLoadPatientThread = null;
    public  LoadNotifyThread  mLoadNotifyThread  = null;
    public  LoadVoiceThread   mLoadVoiceThread   = null;

    private HashMap<String, String> infoHashMap = null;

    public ArrayList<PatientQueue>     patientQueueArrayList = new ArrayList<PatientQueue>();
    public ArrayList<VoiceQueue>       voiceQueueArrayList = new ArrayList<VoiceQueue>();
    public ArrayList<NotifyInfo>       notifyInfoArrayList = new ArrayList<NotifyInfo>();

    // Define message ID For Handler
    private static final int EVENT_CHANGE_PATIENT = 0x8001;
    private static final int EVENT_CHANGE_NOTIFY = 0x8002;
    private static final int EVENT_CHANGE_VOICE = 0x8003;

    //WindowInfo
    TextView tv_main = null;
    TextView tv_title = null;

    public String path = null;
    public String voicename = null;

    public String myCurrentFileName = null;
    public String myLastFileName = null;

    /*

     */
    //public String minor_

    private SocketServer(Context context) {
        mContext = context;
    }

    public static SocketServer createInstance(Context context) {
        if (mSSInstance == null && context != null) {
            mSSInstance = new SocketServer(context);
        }
        return mSSInstance;
    }

    public void startRun(){
        // Add Window Info
        AddWindowInfo();
        AddWindowTitle();

        stopRun();
        mServersocket      =    new ServerThread();
        mLoadPatientThread =    new LoadPatientThread();
        mLoadNotifyThread  =    new LoadNotifyThread();
        mLoadVoiceThread   =    new LoadVoiceThread();

        mServersocket.start();
        mLoadPatientThread.start();
        mLoadNotifyThread.start();
        mLoadVoiceThread.start();

    }

    public void stopRun(){
        if (mServersocket != null){
            mServersocket.interrupt();
            mServersocket = null;
        }

        if (mLoadPatientThread != null){
            mLoadPatientThread.interrupt();
            mLoadPatientThread = null;
        }

        if (mLoadNotifyThread != null){
            mLoadNotifyThread.interrupt();
            mLoadNotifyThread = null;
        }

        if (mLoadVoiceThread != null){
            mLoadVoiceThread.interrupt();
            mLoadVoiceThread = null;
        }

        mHandler.removeMessages(EVENT_CHANGE_PATIENT);
        mHandler.removeMessages(EVENT_CHANGE_NOTIFY);
        mHandler.removeMessages(EVENT_CHANGE_VOICE);
    }

    public synchronized static SocketServer getInstance() {
        return mSSInstance;
    }


    private void AddWindowInfo(){
        LayoutInflater inflater = PosterMainActivity.INSTANCE.getLayoutInflater();
        tv_main = (TextView) inflater.inflate(R.layout.define_textview,null);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(1828,601);
        FrameLayout frame = new FrameLayout(mContext);
        frame.setX(67);
        frame.setY(348);
        frame.addView(tv_main);
        PosterMainActivity.INSTANCE.addContentView(frame,layoutParams);
    }

    private void AddWindowTitle(){
        LayoutInflater inflater = PosterMainActivity.INSTANCE.getLayoutInflater();
        tv_title = (TextView) inflater.inflate(R.layout.title_textview,null);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(480,95);
        FrameLayout frame = new FrameLayout(mContext);
        frame.setX(660);
        frame.setY(70);
        frame.addView(tv_title);
        PosterMainActivity.INSTANCE.addContentView(frame,layoutParams);
    }

    private void ConvertMsg(String info){
        String[] rawInfo = info.split("\n");
        for(int i = 0; i < rawInfo.length;i++){
            Log.d("George",rawInfo[i]);
        }
    }

    private String GetSpliteData(String info){
        String[] rawInfo = info.split("<br/>");
        int length = rawInfo.length;
        String textInfo ="";
        for(int i = 0; i < rawInfo.length-1;i++){
            Log.d("GeorgeWin", rawInfo[length-i-2]);
            textInfo += rawInfo[length-i-2]+"\n";
        }
        return textInfo;
    }

    private String GetMinorSpliteData(String info){
        String[] rawInfo = info.split("<br/>");

        return "";
    }



    private String XmlParse(String raw) {
        Log.d("GeorgeWin", "yanwei"+raw);
        String rawInfo = raw.substring(raw.lastIndexOf("PatInfo=")+9,raw.indexOf("\";<"));
        return rawInfo;
    }

    //将socket传输的数据转换为HashMap格式
    private HashMap<String,String> ConvertRaw2HashMap(String[] raw){
        HashMap<String,String>  hmInfo = new HashMap<String, String>();
        for(int i = 1; i < raw.length; i++){
            String[] convertInfo = raw[i].split("=");
            //判断Key值是否已经存在
            if(hmInfo.containsKey(convertInfo[0])){
                hmInfo.put(convertInfo[0]+1,convertInfo[1]);
            }else {
                hmInfo.put(convertInfo[0],convertInfo[1]);
            }
        }
        return hmInfo;
    }



    private int getDataType(String info){
        String[] rawInfo = info.split("\n");
        int rawLength = rawInfo.length;
        if (rawLength == 5){
            return VOICE_TYPE;
        }else if (rawLength == 10){
            return PATIENT_TYPE;
        }else if (rawLength == 11){
            return NOTIFY_TYPE;
        }else {
            return ERROR_TYPE;
        }
    }

    private void ConvertNotifyInfoQueue(String info){
        Log.d(TAG,info);
        String[] rawInfo = info.split("\n");
        infoHashMap = new HashMap<String, String>();
        NotifyInfo notifyInfo = new NotifyInfo();

        infoHashMap = ConvertRaw2HashMap(rawInfo);

        notifyInfo.msg = infoHashMap.get("msg");
        notifyInfo.areaId = infoHashMap.get("areaid");
        notifyInfo.areaType = infoHashMap.get("areatype");
        notifyInfo.dataId = infoHashMap.get("dataid");
        notifyInfo.notifyData = infoHashMap.get("data");
        notifyInfo.areaId1 = infoHashMap.get("areaid1");
        notifyInfo.areaType1 = infoHashMap.get("areatype1");
        notifyInfo.dataId1 = infoHashMap.get("dataid1");
        notifyInfo.action = infoHashMap.get("action");
        notifyInfo.notifyData1 = rawInfo[10];

        if (notifyInfo != null){
            infoHashMap = null;
        }

        notifyInfoArrayList.add(notifyInfo);

    }

    private void ConvertVoiceQueue(String info){
        Log.d(TAG,info);
        String[] rawInfo = info.split("\n");
        infoHashMap = new HashMap<String, String>();
        VoiceQueue voiceQueue = new VoiceQueue();

        infoHashMap = ConvertRaw2HashMap(rawInfo);

        //HashMap 转成VoiceQueue实体类
        voiceQueue.msg = infoHashMap.get("msg");
        voiceQueue.count = infoHashMap.get("count");
        voiceQueue.url = infoHashMap.get("url");
        voiceQueue.session_id = infoHashMap.get("session_id");

        if(voiceQueue != null){
            Log.d(TAG,infoHashMap.size()+"HashMap size");
            Log.d(TAG,voiceQueue.url+"");
            infoHashMap = null;
        }
        voiceQueueArrayList.add(voiceQueue);
    }



    private void ConvertPatientQueueMsg(String info){
        Log.d(TAG,info);
        String[] rawInfo = info.split("\n");
        Log.d("GeorgeWin", "Get PatientQUeueMsg length"+rawInfo.length+rawInfo[0]);
        infoHashMap = new HashMap<String, String>();
        PatientQueue patientQueue = new PatientQueue();

        infoHashMap = ConvertRaw2HashMap(rawInfo);

        //HashMap 转成PatientQueue实体类
        patientQueue.msg = infoHashMap.get("msg");
        patientQueue.areaid = infoHashMap.get("areaid");
        patientQueue.areatype = infoHashMap.get("areatype");
        patientQueue.dataid = infoHashMap.get("dataid");
        patientQueue.data = infoHashMap.get("data");
        patientQueue.areaid1 = infoHashMap.get("areaid1");
        patientQueue.areatype1 = infoHashMap.get("areatype1");
        patientQueue.dataid1 = infoHashMap.get("dataid1");
        patientQueue.data1 = infoHashMap.get("data1");

        if(patientQueue != null){
            Log.d(TAG,infoHashMap.size()+"HashMap size");
            Log.d(TAG,patientQueue.msg+"");
            infoHashMap = null;
        }
        patientQueueArrayList.add(patientQueue);
    }

    private int getMsgLength(String info){
        String[] rawInfo = info.split("\n");
        return rawInfo.length;
    }

    public class ServerThread extends Thread{
        private int count = 0;
        @Override
        public void run() {

            try{
                Log.d(TAG,"Create ServerThread");
                socket = new DatagramSocket(Port);
                serversocket = new ServerSocket(Port);
                while (true){
                    //Log.d(TAG,"Running count"+count++);

                    byte data[] = new byte[4*1024];
                    DatagramPacket packet = new DatagramPacket(data,data.length);
                    socket.receive(packet);
                    String msg = new String (packet.getData(),packet.getOffset(),packet.getLength(),"gb2312");
                    ConvertMsg(msg);
                    Log.d(TAG," Get Message Length"+getMsgLength(msg));
                    InfoType = getDataType(msg);
                    switch (InfoType){
                        case VOICE_TYPE:
                            Log.d(TAG,"This type RawInfo from Socket code" + VOICE_TYPE + "VOICE_TYPE");
                            ConvertVoiceQueue(msg);
                            break;
                        case PATIENT_TYPE:
                            Log.d(TAG,"This type RawInfo from Socket code" + PATIENT_TYPE + "PATIENT_TYPE");
                            ConvertPatientQueueMsg(msg);
                            break;
                        case NOTIFY_TYPE:
                            Log.d(TAG,"This type RawInfo from Socket code" + NOTIFY_TYPE + "NOTIFY_TYPE");
                            ConvertNotifyInfoQueue(msg);
                            break;
                        case ERROR_TYPE:
                            Log.d(TAG,"This type RawInfo from Socket is error code" + ERROR_TYPE);
                            continue;
                    }
                    //ConvertPatientQueueMsg(msg);
                }

            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public class LoadPatientThread extends Thread{

        @Override
        public void run() {
            while (true){
                try {
                    if (isLoadingPatientProgram) {
                        Log.d(TAG,"is Loading Program ; wait for the socket info");
                        Thread.sleep(1000);
                        continue;
                    }
                    if (patientQueueArrayList.size()> 0){
                        Log.d(TAG,"Send Message to Handler");
                        isLoadingPatientProgram =true;
                        mHandler.sendEmptyMessage(EVENT_CHANGE_PATIENT);
                    }else {
                        Log.d(TAG,"All list is null ; wait for the socket info");
                        Thread.sleep(1000*1);
                        continue;
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        }
    }

    public class LoadNotifyThread extends Thread{

        @Override
        public void run() {
            while (true){
                try {
                    if (isLoadingNotifyProgram) {
                        Log.d(TAG,"is Loading Notify ; wait for the socket info");
                        Thread.sleep(1000);
                        continue;
                    }
                    if (notifyInfoArrayList.size()> 0){
                        isLoadingNotifyProgram =true;
                        mHandler.sendEmptyMessage(EVENT_CHANGE_NOTIFY);;
                    }else {
                        Log.d(TAG,"Notify list is null ; wait for the socket info");
                        Thread.sleep(1000*1);
                        continue;
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        }
    }

    public class LoadVoiceThread extends Thread{

        @Override
        public void run() {
            while (true){
                try {
                    if (isLoadingVoiceProgram) {
                        Log.d(TAG,"is Loading Voice ; wait for the socket info");
                        Thread.sleep(1000);
                        continue;
                    }
                    if (voiceQueueArrayList.size()> 0){
                        isLoadingVoiceProgram =true;
                        Log.d(TAG,"Begin to Loading Voice Program");
                        mHandler.sendEmptyMessage(EVENT_CHANGE_VOICE);
                    }else {
                        Log.d(TAG,"Voice list is null ; wait for the socket info");
                        Thread.sleep(1000*1);
                        continue;
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        }
    }

    @SuppressLint("HandlerLeak")
    final  Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case EVENT_CHANGE_PATIENT:
                    Log.d(TAG,"GET CURRENT PATIENT LIST SIZE"+ patientQueueArrayList.size());
                    tv_main.setText(GetSpliteData(patientQueueArrayList.get(0).data));
                    tv_title.setText(patientQueueArrayList.get(0).data1);
                    patientQueueArrayList.remove(0);
                    isLoadingPatientProgram = false;
                    break;
                case EVENT_CHANGE_NOTIFY:
                    Log.d(TAG, "Get Current Notify List Size"+ notifyInfoArrayList.size());
                    Log.d(TAG, "Get Notify data " + notifyInfoArrayList.get(0).notifyData1);
                    PosterMainActivity.INSTANCE.initTongRenNotifyPw(notifyInfoArrayList.get(0).notifyData , XmlParse(notifyInfoArrayList.get(0).notifyData1));
                    notifyInfoArrayList.remove(0);
                    break;
                case EVENT_CHANGE_VOICE:
                    Log.d(TAG, "Get Current Voice List Size" + voiceQueueArrayList.size());
                    Log.d(TAG, "Get Current Voice Url " + voiceQueueArrayList.get(0).url );
                    String[] convert = voiceQueueArrayList.get(0).url.split("/");
                    path =voiceQueueArrayList.get(0).url.substring(1,voiceQueueArrayList.get(0).url.length()-1);
                    voicename = convert[convert.length-1].substring(0,25);
                    path.trim();
                    try{
                        path = new String(path.getBytes(),"utf-8");
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000);
                                Log.d(TAG,"FTP 下载地址"+path);
                                new FtpSocket().downloadSingleFile(path, "/mnt/sdcard/TRDownload/",voicename, new FtpSocket.DownLoadProgressListener() {
                                    @Override
                                    public void onDownLoadProgress(String currentStep, long downProcess, File file) {
                                        Log.d(TAG, "FTP Status" + currentStep+voicename);
                                        if(currentStep.equals(Contants.FTP_DOWNLOAD_SUCCESS)){
                                            Log.d(TAG,"开始播放声音");
                                            voiceQueueArrayList.remove(0);
                                            try{
                                                Thread.sleep(500);
                                            }catch (InterruptedException e){
                                                e.printStackTrace();
                                            }
                                            MusicServiceHelper.doBindMusicService("/sdcard/TRDownload/"+voicename);
                                        }else if (currentStep.equals(Contants.FTP_FILE_NOEXIST)){
                                            Log.d(TAG,"FTP 文件不存在 重新开始载入");
                                            isLoadingVoiceProgram =false;
                                        }
                                    }
                                });
                            }catch (Exception e){
                                e.printStackTrace();
                                Log.d(TAG,"FTP 异常清除数据");
                                voiceQueueArrayList.clear();
                                isLoadingVoiceProgram = false;
                            }

                        }
                    }).start();
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };


}
