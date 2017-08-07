package com.youngsee.webservices;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.youngsee.posterdisplayer.PosterMainActivity;
import com.youngsee.posterdisplayer.R;
import com.youngsee.socket.PatientQueue;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by WeiYanGeorge on 17/8/3.
 */

public class SocketServerMinor {
    private Context mContext = null;
    private static SocketServerMinor mSSInstance = null;
    public static ServerSocket serversocket = null;
    public static String TAG = "PD SocketServer";
    public static int Port = 20001;

    private static final int ERROR_TYPE = -1;
    private static final int PATIENT_TYPE = 0;
    public int InfoType = -1;

    public static boolean isLoadingPatientProgram = false;

    public static DatagramSocket socket = null;

    //Thread
    public ServerThread mServersocket = null;
    public LoadPatientThread mLoadPatientThread = null;

    private HashMap<String, String> infoHashMap = null;

    public ArrayList<PatientQueue> patientQueueArrayList = new ArrayList<PatientQueue>();

    //Define message ID For Handler
    private static final int EVENT_CHANGE_PATIENT = 0x8001;

    public String firstNumType = null;
    public String firstCurrentPatient = null;
    public String firstReadyPatient = null;
    public String firstRoom = null;

    public String SecondNumType = null;
    public String SecondCurrentPatient = null;
    public String SecondReadyPatient = null;
    public String SecondRoom = null;

    public LinearLayout ly_first_window = null;
    public TextView tv_first_num_type = null;
    public TextView tv_first_currentPatient = null;
    public TextView tv_first_readyPatient =  null;
    public TextView tv_first_room = null;

    public LinearLayout ly_second_window = null;
    public TextView tv_second_num_type = null;
    public TextView tv_second_currentPatient = null;
    public TextView tv_second_readyPatient =  null;
    public TextView tv_second_room = null;



    private SocketServerMinor(Context context) {
        mContext = context;
    }

    public static SocketServerMinor createInstance(Context context) {
        if (mSSInstance == null && context != null) {
            mSSInstance = new SocketServerMinor(context);
        }
        return mSSInstance;
    }

    public synchronized static SocketServerMinor getInstance() {
        return mSSInstance;
    }

    private void AddWindowInfo1(){
        LayoutInflater inflater = PosterMainActivity.INSTANCE.getLayoutInflater();
        ly_first_window = (LinearLayout) inflater.inflate(R.layout.minor_socket_view,null);
        tv_first_room = (TextView) ly_first_window.findViewById(R.id.tv_minor_first_room);
        tv_first_num_type = (TextView) ly_first_window.findViewById(R.id.tv_minor_first_type);
        tv_first_readyPatient = (TextView) ly_first_window.findViewById(R.id.tv_minor_first_ready_patient);
        tv_first_currentPatient = (TextView) ly_first_window.findViewById(R.id.tv_minor_first_current_patient);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        FrameLayout frame = new FrameLayout(mContext);
        frame.setX(67);
        frame.setY(320);
        frame.addView(ly_first_window);
        PosterMainActivity.INSTANCE.addContentView(frame,layoutParams);
    }

    private void AddWindowInfo2(){
        LayoutInflater inflater = PosterMainActivity.INSTANCE.getLayoutInflater();
        ly_second_window= (LinearLayout) inflater.inflate(R.layout.minor_socket_view2,null);
        tv_second_room = (TextView) ly_second_window.findViewById(R.id.tv_minor_second_room);
        tv_second_num_type = (TextView) ly_second_window.findViewById(R.id.tv_minor_second_type);
        tv_second_readyPatient = (TextView) ly_second_window.findViewById(R.id.tv_minor_second_ready_patient);
        tv_second_currentPatient = (TextView) ly_second_window.findViewById(R.id.tv_minor_second_current_patient);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        FrameLayout frame = new FrameLayout(mContext);
        frame.setX(67);
        frame.setY(1180);
        frame.addView(ly_second_window);
        PosterMainActivity.INSTANCE.addContentView(frame,layoutParams);
    }

    public void startRun() {
        stopRun();

        AddWindowInfo1();
        AddWindowInfo2();
        mServersocket = new ServerThread();
        mLoadPatientThread = new LoadPatientThread();

        mServersocket.start();
        mLoadPatientThread.start();
    }

    public void stopRun() {
        if (mServersocket != null) {
            mServersocket.interrupt();
            mServersocket = null;
        }

        if (mLoadPatientThread != null) {
            mLoadPatientThread.interrupt();
            mLoadPatientThread = null;
        }

        mHandler.removeMessages(EVENT_CHANGE_PATIENT);
    }

    private void ConvertMsg(String info) {
        String[] rawInfo = info.split("\n");
        for (int i = 0; i < rawInfo.length; i++) {
            Log.d("George", rawInfo[i]);
        }
    }

    private int getMsgLength(String info) {
        String[] rawInfo = info.split("\n");
        return rawInfo.length;
    }

    private int getDataType(String info) {
        String[] rawInfo = info.split("\n");
        int rawLength = rawInfo.length;
        if (rawLength == 10) {
            return PATIENT_TYPE;
        } else {
            return ERROR_TYPE;
        }
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

    private void GetSpliteRoomData(String info){
        String[] rawInfo = info.split("<br/>");
        firstRoom = rawInfo[1];
        SecondRoom = rawInfo[0];
    }

    private void GetSpliteData(String info) {
        String[] rawInfo = info.split("<br/>");
        int length = rawInfo.length;
        Log.d("GeorgeWin", rawInfo[length-1]+length);

        firstNumType = rawInfo[length - 2];
        firstCurrentPatient = rawInfo[length - 3];
        firstReadyPatient = rawInfo[length - 4];

        SecondNumType = rawInfo [length-5];
        SecondCurrentPatient = rawInfo [length-6];
        SecondReadyPatient = rawInfo[length - 7];
        Log.d("GeorgeWin", firstNumType + " " + firstCurrentPatient + firstReadyPatient);
        Log.d("GeorgeWin", SecondNumType + " " + SecondCurrentPatient + SecondReadyPatient);
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


    public class ServerThread extends Thread {
        private int count = 0;

        @Override
        public void run() {
            try {
                Log.d(TAG, "Create ServerThread");
                socket = new DatagramSocket(Port);
                serversocket = new ServerSocket(Port);
                while (true) {
                    Log.d(TAG, "Running count" + count++);
                    byte data[] = new byte[4 * 1024];
                    DatagramPacket packet = new DatagramPacket(data, data.length);
                    socket.receive(packet);
                    String msg = new String(packet.getData(), packet.getOffset(), packet.getLength(), "gb2312");
                    ConvertMsg(msg);
                    Log.d(TAG, "GET Message Length" + getMsgLength(msg));
                    InfoType = getDataType(msg);
                    switch (InfoType) {
                        case PATIENT_TYPE:
                            Log.d(TAG, "This type RawInfo from Socket code" + PATIENT_TYPE + "PATIENT_TYPE");
                            ConvertPatientQueueMsg(msg);
                            break;
                        case ERROR_TYPE:
                            Log.d(TAG, "This type RawInfo from Socket is error code" + ERROR_TYPE);
                            continue;
                    }
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
                    Log.d(TAG, "Create LoadPatientThread");
                    if (isLoadingPatientProgram) {
                        Log.d(TAG, "is Loading Program ; wait for the socket info");
                        Thread.sleep(1000);
                        continue;
                    }
                    if (patientQueueArrayList.size() > 0) {
                        Log.d(TAG, "Send Message to Handler");
                        isLoadingPatientProgram = true;
                        mHandler.sendEmptyMessage(EVENT_CHANGE_PATIENT);
                    } else {
                        Log.d(TAG, "");
                        Thread.sleep(1000);
                        continue;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_CHANGE_PATIENT:
                    Log.d(TAG, "GET CURRENT PATIENT LIST SIZE" + patientQueueArrayList.size());
                    Log.d(TAG, "GET THE QUEUE DATA" + patientQueueArrayList.get(0).data);
                    GetSpliteData(patientQueueArrayList.get(0).data);
                    GetSpliteRoomData(patientQueueArrayList.get(0).data1);
                    tv_first_room.setText(firstRoom);
                    tv_first_num_type.setText(firstNumType);
                    tv_first_currentPatient.setText(firstCurrentPatient);
                    tv_first_readyPatient.setText(firstCurrentPatient);

                    tv_second_room.setText(firstRoom);
                    tv_second_num_type.setText(firstNumType);
                    tv_second_currentPatient.setText(firstCurrentPatient);
                    tv_second_readyPatient.setText(firstCurrentPatient);

                    isLoadingPatientProgram = false;
                    patientQueueArrayList.remove(0);
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

}
