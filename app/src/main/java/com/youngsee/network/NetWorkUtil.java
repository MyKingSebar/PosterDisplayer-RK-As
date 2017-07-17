package com.youngsee.network;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONObject;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.youngsee.network.HttpConnectionAsyn.OnResponseListener;

public class NetWorkUtil{
    public static final String  HOST                    = "http://123.56.146.48:8001";
    // URL of resource, 注意各个索引值和url字符串数组的索引要对应
    public static final int     CMD_REGISTER_CODE         = 0;
    public static final int     CMD_VERIFY_CODE           = 1;
    
    private static final String mUrls[]           = new String[]{"/Devices", "/Devices"};
    
    // key of json
    public static final String  KEY_ERROR_CODE    = "errorcode";
    
    // 服务器返回json串中对应的结果映射
    public static final int     RESULT_OK         = 0;
    
    // HTTP层返回的错误码映射到应用
    /** 网络异常 , 服务器不可用 */
    public static final int     NETWORK_ERROR     = 503;
    /** 网络正常 */
    public static final int     NETWORK_OK        = 200;
    
    private final int           INPUT_BUF_SIZE    = 1024;
    
    private boolean             mHandlerExited    = false;
    private HttpConnectionAsyn  conn;
    private Handler             mHandler;
    
    public NetWorkUtil(Handler handler){
        this.mHandler = handler;
        if(conn == null)
            conn = new HttpConnectionAsyn(mListener);
    }
    
    /**
     * 所有响应统一处理
     */
    private OnResponseListener mListener = new OnResponseListener(){
                                             @Override
                                             public void onError(Exception e, int code){
                                                 Log.d("carrental_network", "error code: " + String.valueOf(code));
                                                 
                                                 if(mHandlerExited){
                                                     return;
                                                 }
                                                 
                                                 Message msg = Message.obtain();
                                                 msg.what = code;
                                                 msg.arg1 = NETWORK_ERROR;
                                                 mHandler.sendMessage(msg);
                                             }
                                             
                                             @Override
                                             public void onResponse(InputStream in, int statusCode, int code){
                                                 if(mHandlerExited){
                                                     return;
                                                 }
                                                 
                                                 String result = "";
                                                 if(in != null){
                                                     result = read2String(in);
                                                 }
                                                 Message msg = Message.obtain();
                                                 // arg1表示HTTP层消息的结果，目前所有非200的结果都转为网络错误
                                                 if(statusCode == NETWORK_OK){
                                                     msg.arg1 = NETWORK_OK;
                                                 }
                                                 else{
                                                     msg.arg1 = NETWORK_ERROR;
                                                 }
                                                 msg.what = code;
                                                 msg.obj = result;
                                                 Log.d("carrental_network", "statusCode: " + String.valueOf(statusCode)
                                                         + " json: " + result);
                                                 mHandler.sendMessage(msg);
                                             }
                                             
                                             @Override
                                             public void onImgResponse(InputStream in, int statusCode, int code){
                                                 // TODO Auto-generated method
                                                 // stub
                                                 
                                             }
                                         };
    
    public void close(){
        mHandlerExited = true;
    }
    /**
     * 获取验证码
     * 
     * @param phoneNumber
     */
    public void register(String cpuid, String code, String group){
        try{
            Map<String, String> params = new HashMap<String, String>();
            params.put("command", "register");
            params.put("code", code);
            params.put("group", group);
            params.put("company_name", group);
            params.put("cpuid_macaddr", cpuid);
            params.put("type", String.valueOf(1));
            
            conn.connect(HttpConnectionAsyn.POST, HOST + mUrls[CMD_REGISTER_CODE],
                    setURLParams(HttpConnectionAsyn.POST, params), setHeaderParams(), CMD_REGISTER_CODE);
        }
        catch(Exception e){
            mListener.onError(e, CMD_REGISTER_CODE);
            e.printStackTrace();
        }
    }
    
    /**
     * 验证注册
     * 
     * @param phoneNumber
     */
    public void verifyAuthorization(String cpuid){
        try{
            Map<String, String> params = new HashMap<String, String>();
            params.put("command", "verifyAuthorization");
            params.put("cpuid_macaddr", cpuid);
            
            conn.connect(HttpConnectionAsyn.POST, HOST + mUrls[CMD_VERIFY_CODE],
                    setURLParams(HttpConnectionAsyn.POST, params), setHeaderParams(), CMD_VERIFY_CODE);
        }
        catch(Exception e){
            mListener.onError(e, CMD_VERIFY_CODE);
            e.printStackTrace();
        }
    }
    
    /**
     * 设置http请求URL携带的参数
     * 
     * for get method the special string will append to the request url, for
     * post method the special string will append to the request body
     * 
     * @param method
     *            GET or Post
     * @param params
     * @return return the special string with request
     * 
     * @throws UnsupportedEncodingException
     */
    private String setURLParams(int method, Map<String, String> params) throws UnsupportedEncodingException{
        StringBuilder buf = new StringBuilder();
        if(params != null && !params.isEmpty()){
            Set<Entry<String, String>> entries = params.entrySet();
            if(method == HttpConnectionAsyn.GET){
                buf.append("?");
                for(Map.Entry<String, String> entry : entries){
                    buf.append(entry.getKey()).append("=")
                            .append(URLEncoder.encode(entry.getValue(), HttpConnectionAsyn.ENCODING)).append("&");
                }
                buf.deleteCharAt(buf.length() - 1);
            }
            else{
                JSONObject json = new JSONObject(params);
                buf.append(json.toString());
            }
            return buf.toString();
        }
        else{
            return "";
        }
    }
    
    /**
     * 设置http请求头相关内容
     * 
     * @return
     */
    private Map<String, String> setHeaderParams(){
        Map<String, String> map = new HashMap<String, String>();
        map.put(HttpConnectionAsyn.CONTENT_TYPE, HttpConnectionAsyn.CONTENT_TYPE_VALUE);
        map.put(HttpConnectionAsyn.ACCEPT, HttpConnectionAsyn.ACCEPT_VALUE);
        map.put(HttpConnectionAsyn.ACCEPT_ENCODING, HttpConnectionAsyn.ACCEPT_ENCODING_VALUE);
        return map;
    }
    
    /**
     * 将输入流转为字符串
     * 
     * @param inStream
     *            输入流对象
     * @return
     * @throws Exception
     */
    private String read2String(InputStream inStream){
        try{
            ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
            byte[] buffer = new byte[INPUT_BUF_SIZE];
            int len = 0;
            while((len = inStream.read(buffer, 0, INPUT_BUF_SIZE)) != -1){
                outSteam.write(buffer, 0, len);
            }
            outSteam.close();
            inStream.close();
            return new String(outSteam.toByteArray(), HttpConnectionAsyn.ENCODING);
        }
        catch(Exception e){
            e.printStackTrace();
            return "";
        }
    }
    
    /**
     * 将输入流转换成字节数组
     * 
     * @param in
     * @return
     * @throws Exception
     */
    private byte[] read2Byte(InputStream in) throws Exception{
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buff = new byte[INPUT_BUF_SIZE];
        int len = 0;
        while((len = in.read(buff, 0, INPUT_BUF_SIZE)) != -1){
            os.write(buff, 0, len);
        }
        os.close();
        in.close();
        return os.toByteArray();
    }
    
}
