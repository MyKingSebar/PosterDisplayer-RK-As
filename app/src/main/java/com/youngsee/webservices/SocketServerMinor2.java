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

import com.youngsee.common.BaseUtils;
import com.youngsee.posterdisplayer.PosterMainActivity;
import com.youngsee.posterdisplayer.R;
import com.youngsee.socket.MinorScreenInfo;
import com.youngsee.socket.PatientQueue;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;

/**
 * Created by WeiYanGeorge on 17/8/21.
 */

public class SocketServerMinor2 {
    private Context mContext = null;
    private static SocketServerMinor2 mSSInstance = null;
    public static ServerSocket serversocket = null;
    public static String TAG = "PD SocketServer";
    public static int Port = 20001;

    private static final int ERROR_TYPE = -1;
    private static final int PATIENT_TYPE = 0;
    public int InfoType = -1;

    public static boolean isLoadingPatientProgram = false;
    public static boolean isShowScreenInfo = false;

    public static DatagramSocket socket = null;

    //Thread
    public SocketServerMinor2.ServerThread mServersocket = null;
    public SocketServerMinor2.LoadPatientThread mLoadPatientThread = null;
    public SocketServerMinor2.ShowScreenInfoThread showScreenInfoThread = null;

    private HashMap<String, String> infoHashMap = null;

    public ArrayList<PatientQueue> patientQueueArrayList = new ArrayList<PatientQueue>();
    public ArrayList<MinorScreenInfo> minorScreenInfoArrayList = new ArrayList<MinorScreenInfo>();

    //Define message ID For Handler
    private static final int EVENT_CHANGE_PATIENT = 0x8001;
    private static final int EVENT_CHANGE_PATIENT1 = 0x8002;

    public String firstNumType = null;
    public String firstCurrentPatient = null;
    public String firstReadyPatient = null;
    public String firstRoom = null;
    public String firstReadyPatient1 = null;

    public String SecondNumType = null;
    public String SecondCurrentPatient = null;
    public String SecondReadyPatient = null;
    public String SecondRoom = null;
    public String SecondReadyPatient1 = null;

    public FrameLayout ly_first_window = null;
    public TextView tv_first_num_type = null;
    public TextView tv_first_currentPatient = null;
    public TextView tv_first_readyPatient =  null;
    public TextView tv_first_room = null;
    public TextView tv_first_readyPatient1 = null;

    public LinearLayout ly_second_window = null;
    public TextView tv_second_num_type = null;
    public TextView tv_second_currentPatient = null;
    public TextView tv_second_readyPatient =  null;
    public TextView tv_second_room = null;


    private Timer showInfoTask = null;

    public static String roominfo = null;
    public static String doctorinfo = null;
    public static String currentPatient = null;
    public static String waitingPatient = null;
    public static String waitingPatient1= null;

    private SocketServerMinor2(Context context) {
        mContext = context;
    }

    public static SocketServerMinor2 createInstance(Context context) {
        if (mSSInstance == null && context != null) {
            mSSInstance = new SocketServerMinor2(context);
        }
        return mSSInstance;
    }

    public synchronized static SocketServerMinor2 getInstance() {
        return mSSInstance;
    }

    private void AddWindowInfo1(){
        LayoutInflater inflater = PosterMainActivity.INSTANCE.getLayoutInflater();
        ly_first_window = (FrameLayout) inflater.inflate(R.layout.minor_socket_view_landscape,null);
        tv_first_room = (TextView) ly_first_window.findViewById(R.id.tv_minor_landscape_room_title);
        tv_first_num_type = (TextView) ly_first_window.findViewById(R.id.tv_minor_landscape_num_type);
        tv_first_readyPatient = (TextView) ly_first_window.findViewById(R.id.tv_minor_landscape_waiting_patient);
        tv_first_currentPatient = (TextView) ly_first_window.findViewById(R.id.tv_minor_landscape_current_patient);
        tv_first_readyPatient1 = (TextView)  ly_first_window.findViewById(R.id.tv_minor_landscape_waiting_patient1);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        FrameLayout frame = new FrameLayout(mContext);
        frame.setX(0);
        frame.setY(0);
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
        //AddWindowInfo2();
        mServersocket = new SocketServerMinor2.ServerThread();
        mLoadPatientThread = new SocketServerMinor2.LoadPatientThread();
        showScreenInfoThread = new SocketServerMinor2.ShowScreenInfoThread();

        mServersocket.start();
        mLoadPatientThread.start();
        showScreenInfoThread.start();
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

    private boolean isRawInfoTwo(String info){
        String[] rawinfo = info.split("<br/>");
        firstRoom = rawinfo[1];
        SecondRoom = rawinfo[0];
        SecondRoom.trim();
        //判断传输的数据是否为两个
        Log.d("GeorgeWin","the second room info is"+ SecondRoom+"the second room size is "+SecondRoom.length());
        if(BaseUtils.isEmptyString(SecondRoom)){
            return false;
        }else {
            return true;
        }
    }

    private void GetSpliteRoomData(String info){
        String[] rawInfo = info.split("<br/>");
        firstRoom = rawInfo[1];
        SecondRoom = rawInfo[0];
        if(SecondRoom.trim().isEmpty()){
            Log.d("GeorgeWin","if Second Room is Empty");
        }
    }

    private void GetSpliteData(String info,String info1) {
        if(isRawInfoTwo(info1)){
            Log.d("GeorgeWin","This is Double Info");
            MinorScreenInfo minorScreenInfo1 = new MinorScreenInfo();
            MinorScreenInfo minorScreenInfo2 = new MinorScreenInfo();

            String[] rawInfo = info1.split("<br/>");
            String[] rawInfo1 = info.split("<br/>");
            int length = rawInfo1.length;
            Log.d("GeorgeWin", rawInfo1[length-1]+length);
            firstRoom = rawInfo[1];
            SecondRoom = rawInfo[0];

            firstNumType = rawInfo1[length-2];
            firstCurrentPatient = rawInfo1[length -3];
            firstReadyPatient = rawInfo1[length -4];
            firstReadyPatient1 = rawInfo1[length-5];

            SecondNumType = rawInfo1[length-7];
            SecondCurrentPatient = rawInfo1[length-8];
            SecondReadyPatient = rawInfo1[length-9];
            SecondReadyPatient1 = rawInfo1[length-10];


            minorScreenInfo1.roominfo = firstRoom;
            minorScreenInfo1.doctorinfo = firstNumType;
            minorScreenInfo1.currentPatient = firstCurrentPatient;
            minorScreenInfo1.waitingPatient = firstReadyPatient;
            minorScreenInfo1.waitingPatient1 = firstReadyPatient1;

            minorScreenInfo2.roominfo = SecondRoom;
            minorScreenInfo2.doctorinfo = SecondNumType;
            minorScreenInfo2.currentPatient = SecondCurrentPatient;
            minorScreenInfo2.waitingPatient = SecondReadyPatient;
            minorScreenInfo2.waitingPatient1 = SecondReadyPatient1;
            Log.d("GeorgeWin","The Second WaitingPatient"+SecondReadyPatient);

            minorScreenInfoArrayList.add(minorScreenInfo1);
            minorScreenInfoArrayList.add(minorScreenInfo2);
        }else {
            Log.d("GeorgeWin","This is Single Info");
            MinorScreenInfo minorScreenInfo1 = new MinorScreenInfo();

            Log.d("GeorgeWin","This is Single Info"+info1);
            String[] rawInfo = info1.split("<br/>");
            String[] rawInfo1 = info.split("<br/>");

            int length = rawInfo1.length;

            Log.d("GeorgeWin", "The First Room is "+rawInfo[1]+length);
            firstRoom = rawInfo[1];


            firstNumType = rawInfo1[length-2];
            firstCurrentPatient = rawInfo1[length -3];
            firstReadyPatient = rawInfo1[length -4];
            firstReadyPatient1 = rawInfo1[length -5];

            minorScreenInfo1.roominfo = firstRoom;
            minorScreenInfo1.doctorinfo = firstNumType;
            minorScreenInfo1.currentPatient = firstCurrentPatient;
            minorScreenInfo1.waitingPatient = firstReadyPatient;
            minorScreenInfo1.waitingPatient1 = firstReadyPatient1;


            minorScreenInfoArrayList.add(minorScreenInfo1);
        }
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
                    //Log.d(TAG, "Running count" + count++);
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

    public class ShowScreenInfoThread extends Thread{
        @Override
        public void run() {
            while (true){
                try{
                    Log.d(TAG,"Create ShowPatientThread");
                    if(isShowScreenInfo){
                        Log.d(TAG,"is Showing Program; wait for the next round");
                        Thread.sleep(1000);
                        continue;
                    }
                    if (minorScreenInfoArrayList.size()>0){
                        isShowScreenInfo = true;
                        int infolength = minorScreenInfoArrayList.size();
                        for(int i = 0; i < infolength; i++){
                            Log.d("GeorgeWin","is loading info"+i);
                            Log.d("GeorgeWin","set patient info"+minorScreenInfoArrayList.get(i).roominfo+","+minorScreenInfoArrayList.get(i).doctorinfo
                                    +","+minorScreenInfoArrayList.get(i).currentPatient+","+minorScreenInfoArrayList.get(i).waitingPatient);
                            roominfo = minorScreenInfoArrayList.get(i).roominfo;
                            doctorinfo = minorScreenInfoArrayList.get(i).doctorinfo;
                            currentPatient = minorScreenInfoArrayList.get(i).currentPatient;
                            waitingPatient = minorScreenInfoArrayList.get(i).waitingPatient;
                            try{
                                mHandler.sendEmptyMessage(EVENT_CHANGE_PATIENT1);
                                Thread.sleep(6000);
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                        Log.d("GeorgeWin","Finish the round info ");
                        minorScreenInfoArrayList.clear();
                        isLoadingPatientProgram = false;
                        patientQueueArrayList.remove(0);
                        isShowScreenInfo=false;
                    }else {
                        Thread.sleep(1000);
                        continue;
                    }

                }catch (Exception e){

                }
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
                    if (patientQueueArrayList.size()>5){
                        patientQueueArrayList.clear();
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
                case EVENT_CHANGE_PATIENT1:
                    Log.d("GeorgeWin","Event Change Patient"+ roominfo+doctorinfo+currentPatient+waitingPatient);
                    tv_first_room.setText(roominfo);
                    tv_first_num_type.setText(doctorinfo);
                    //当前就诊
                    if(currentPatient.length()>7){
                        tv_first_currentPatient.setTextSize(90);
                        tv_first_currentPatient.setText(currentPatient);
                    }else {
                        tv_first_currentPatient.setTextSize(110);
                        tv_first_currentPatient.setText(currentPatient);
                    }

                    //候诊1
                    if(waitingPatient.length()>7){
                        tv_first_readyPatient.setTextSize(90);
                        tv_first_readyPatient.setText(waitingPatient);
                    }else {
                        tv_first_readyPatient.setTextSize(110);
                        tv_first_readyPatient.setText(waitingPatient);
                    }

                    //候诊2
                    if(!BaseUtils.isEmptyString(waitingPatient1)){
                        if(waitingPatient1.length()>7){
                            tv_first_readyPatient1.setTextSize(90);
                            tv_first_readyPatient1.setText(waitingPatient1);
                        }else {
                            tv_first_readyPatient1.setTextSize(110);
                            tv_first_readyPatient1.setText(waitingPatient1);
                        }
                    }else {
                        tv_first_readyPatient1.setText("");
                    }
                    break;
                case EVENT_CHANGE_PATIENT:
                    ArrayList<MinorScreenInfo> TempScreenInfoArrayList = new ArrayList<MinorScreenInfo>();
                    TempScreenInfoArrayList =  minorScreenInfoArrayList;
                    Log.d(TAG, "Get Minor info size and voice queue size");
                    Log.d(TAG, "GET CURRENT PATIENT LIST SIZE" + patientQueueArrayList.size());
                    Log.d(TAG, "GET THE QUEUE DATA" + patientQueueArrayList.get(0).data);
                    Log.d(TAG, "GET THE QUEUE DATA1" + patientQueueArrayList.get(0).data1);
                    GetSpliteData(patientQueueArrayList.get(0).data,patientQueueArrayList.get(0).data1);
                    isShowScreenInfo = false;
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };
}
