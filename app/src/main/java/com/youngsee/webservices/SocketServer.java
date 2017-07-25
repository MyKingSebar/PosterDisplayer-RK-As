package com.youngsee.webservices;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;

import com.youngsee.socket.NotifyInfo;
import com.youngsee.socket.PatientQueue;
import com.youngsee.socket.VoiceQueue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

    //Thread

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
    }

    public synchronized static SocketServer getInstance() {
        return mSSInstance;
    }

    private void ConvertMsg(String info) {
        String[] rawInfo = info.split("&");
        for (int i = 0; i < rawInfo.length; i++) {
            Log.d(TAG, rawInfo[i]);
        }
    }

    //将socket传输的数据转换为HashMap格式
    private HashMap<String, String> ConvertRaw2HashMap(String[] raw) {
        HashMap<String, String> hmInfo = new HashMap<String, String>();
        for (int i = 0; i < raw.length; i++) {
            String[] convertInfo = raw[i].split("=");
            //判断Key值是否已经存在
            if (hmInfo.containsKey(convertInfo[0])) {
                hmInfo.put(convertInfo[0] + 1, convertInfo[1]);
            } else {
                hmInfo.put(convertInfo[0], convertInfo[1]);
            }
        }
        return hmInfo;
    }

    private int getDataType(String info) {
        String[] rawInfo = info.split("&");
        int rawLength = rawInfo.length;
        if (rawLength == 4) {
            return VOICE_TYPE;
        } else if (rawLength == 9) {
            return PATIENT_TYPE;
        } else if (rawLength == 10) {
            return NOTIFY_TYPE;
        } else {
            return ERROR_TYPE;
        }
    }

    private void ConvertNotifyInfoQueue(String info) {
        String[] rawInfo = info.split("&");
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
        notifyInfo.notifyData1 = infoHashMap.get("data1");

        if (notifyInfo != null) {
            infoHashMap = null;
        }

        notifyInfoArrayList.add(notifyInfo);

    }

    private void ConvertVoiceQueue(String info) {
        String[] rawInfo = info.split("&");
        infoHashMap = new HashMap<String, String>();
        VoiceQueue voiceQueue = new VoiceQueue();

        infoHashMap = ConvertRaw2HashMap(rawInfo);

        //HashMap 转成VoiceQueue实体类
        voiceQueue.msg = infoHashMap.get("msg");
        voiceQueue.count = infoHashMap.get("count");
        voiceQueue.url = infoHashMap.get("url");
        voiceQueue.session_id = infoHashMap.get("session_id");

        if (voiceQueue != null) {
            Log.d(TAG, infoHashMap.size() + "HashMap size");
            Log.d(TAG, voiceQueue.url + "");
            infoHashMap = null;
        }
        voiceQueueArrayList.add(voiceQueue);
    }


    private void ConvertPatientQueueMsg(String info) {
        String[] rawInfo = info.split("&");
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

        if (patientQueue != null) {
            Log.d(TAG, infoHashMap.size() + "HashMap size");
            Log.d(TAG, patientQueue.msg + "");
            infoHashMap = null;
        }
        patientQueueArrayList.add(patientQueue);
    }

    private int getMsgLength(String info) {
        String[] rawInfo = info.split("&");
        return rawInfo.length;
    }

    public class ServerThread extends Thread {
        private int count = 0;

        @Override
        public void run() {

            try {
                serversocket = new ServerSocket(Port);
                while (true) {
                    Log.d(TAG, "Running count" + count++);
                    Socket socket = serversocket.accept();
                    BufferedReader buffer = new BufferedReader(
                            new InputStreamReader(socket.getInputStream())
                    );
                    String msg = buffer.readLine();
                    //Log.d("TextDemo", "my msg"+msg);
                    ConvertMsg(msg);
                    Log.d(TAG, " Get Message Length" + getMsgLength(msg));
                    InfoType = getDataType(msg);
                    switch (InfoType) {
                        case VOICE_TYPE:
                            Log.d(TAG, "This type RawInfo from Socket code" + VOICE_TYPE + "VOICE_TYPE");
                            ConvertVoiceQueue(msg);
                            break;
                        case PATIENT_TYPE:
                            Log.d(TAG, "This type RawInfo from Socket code" + PATIENT_TYPE + "PATIENT_TYPE");
                            ConvertPatientQueueMsg(msg);
                            break;
                        case NOTIFY_TYPE:
                            Log.d(TAG, "This type RawInfo from Socket code" + NOTIFY_TYPE + "NOTIFY_TYPE");
                            ConvertNotifyInfoQueue(msg);
                            break;
                        case ERROR_TYPE:
                            Log.d(TAG, "This type RawInfo from Socket is error code" + ERROR_TYPE);
                            continue;
                    }
                    //ConvertPatientQueueMsg(msg);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public class LoadPatientThread extends Thread {

        @Override
        public void run() {
            while (true) {
                try {
                    if (isLoadingPatientProgram) {
                        Log.d(TAG, "is Loading Program ; wait for the socket info");
                        Thread.sleep(1000);
                        continue;
                    }
                    if (patientQueueArrayList.size() > 0) {
                        isLoadingPatientProgram = true;
                        mHandler.sendEmptyMessage(EVENT_CHANGE_PATIENT);
                    } else {
                        Log.d(TAG, "All list is null ; wait for the socket info");
                        Thread.sleep(1000);
                        isLoadingPatientProgram = true;
                        continue;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public class LoadNotifyThread extends Thread {

        @Override
        public void run() {
            while (true) {
                try {
                    if (isLoadingNotifyProgram) {
                        Log.d(TAG, "is Loading Program ; wait for the socket info");
                        Thread.sleep(1000);
                        continue;
                    }
                    if (notifyInfoArrayList.size() > 0) {
                        isLoadingNotifyProgram = true;
                        mHandler.sendEmptyMessage(EVENT_CHANGE_NOTIFY);
                    } else {
                        Log.d(TAG, "All Notify list is null ; wait for the socket info");
                        Thread.sleep(1000);
                        continue;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public class LoadVoiceThread extends Thread {

        @Override
        public void run() {
            while (true) {
                try {
                    if (isLoadingPatientProgram) {
                        Log.d(TAG, "is Loading Program ; wait for the socket info");
                        Thread.sleep(1000);
                        continue;
                    }
                    if (patientQueueArrayList.size() > 0) {
                        isLoadingPatientProgram = true;
                        mHandler.sendEmptyMessage(EVENT_CHANGE_PATIENT);
                    } else {
                        Log.d(TAG, "All list is null ; wait for the socket info");
                        Thread.sleep(1000);
                        isLoadingPatientProgram = true;
                        continue;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }



    //消息处理变动UI
    @SuppressLint("HandlerLeak")
    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_CHANGE_PATIENT:
                    Log.d(TAG, "GET Current Patient List Size" + patientQueueArrayList.size());
                    patientQueueArrayList.remove(0);
                    isLoadingPatientProgram = false;
                    break;
                case EVENT_CHANGE_NOTIFY:
                    Log.d(TAG, "Get Current Notify List Size" + notifyInfoArrayList.size());
                    notifyInfoArrayList.remove(0);
                    isLoadingNotifyProgram  = false;
                    break;
                case EVENT_CHANGE_VOICE:
                    Log.d(TAG, "Get Current Voice List Size" + voiceQueueArrayList.size());
                    voiceQueueArrayList.remove(0);
                    isLoadingVoiceProgram   = false;
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

}
