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

import com.youngsee.dao.NWResult;
import com.youngsee.network.HttpConnectionAsyn.OnResponseListener;

public abstract class HttpRequest{
    // HTTP层返回的错误码映射到应用
    /** 网络异常 , 服务器不可用 */
    public static final int    NETWORK_ERROR  = 503;
    /** 网络正常 */
    public static final int    NETWORK_OK     = 200;

    protected Handler          mHandler;

    private final int          INPUT_BUF_SIZE = 1024;
    private boolean            mHandlerExited = false;
    
    /**
     * 所有响应统一处理
     */
    private OnResponseListener mListener = new OnResponseListener(){
                                             @Override
                                             public void onError(Exception e, int code){
                                                 Log.d("HttpObject", "request code: " + String.valueOf(code));
                                                 
                                                 if(mHandlerExited){
                                                     return;
                                                 }
                                                 onNetworkError(code);
                                             }
                                             
                                             @Override
                                             public void onResponse(InputStream in, int statusCode, int code){
                                                 if(mHandlerExited){
                                                     return;
                                                 }
                                                 
                                                 onNetworkResponse(in, statusCode, code);
                                             }
                                             
                                             @Override
                                             public void onImgResponse(InputStream in, int statusCode, int code){
                                                 if(mHandlerExited){
                                                     return;
                                                 }
                                             }
                                         };

                                         
    public HttpRequest(Handler handler){
        this.mHandler = handler;
    }
    
    protected String getHost(){
        return "http://123.56.146.48:8001";
    }
    /**
     * 对网络错误的处理
     * 
     * @param code
     */
    protected void onNetworkError(int code){
        Message msg = Message.obtain();
        msg.what = code;
        msg.arg1 = NETWORK_ERROR;
        mHandler.sendMessage(msg);
    }
    
    /**
     * Get the NWResult by JSON String, The subclass should override this function if it's need.
     * @return
     */
    protected NWResult getNWResult(String json){
        return new NWResult(json);
    }
    
    /**
     * 对服务器结果的处理
     * 
     * @param in
     * @param statusCode
     * @param code
     */
    protected void onNetworkResponse(InputStream in, int statusCode, int code){
        String result = "";
        if(in != null){
            result = read2String(in);
        }
        Log.d("HttpObject", "statusCode: " + String.valueOf(statusCode) + " json: " + result);
        
        Message msg = Message.obtain();
        // arg1表示HTTP层消息的结果，目前所有非200的结果都转为网络错误
        if(statusCode == NETWORK_OK){
            msg.arg1 = NETWORK_OK;
            msg.obj = getNWResult(result);
        }
        else{
            msg.arg1 = NETWORK_ERROR;
            msg.obj = null;
        }
        msg.what = code;

        mHandler.sendMessage(msg);
    }
    
    /**
     * Send http GET to server.
     * 
     * @param params
     * @param code
     * @return
     */
    protected boolean get(String url, HashMap<String, String> params, int code){
        try{
            (new HttpConnectionAsyn(mListener)).connect(HttpConnectionAsyn.GET, getHost() + url, setURLParams(HttpConnectionAsyn.GET, params),
                    setHeaderParams(), code);
        }
        catch(Exception e){
            e.printStackTrace();
            return false;
        }
        
        return true;
    }
    
    /**
     * Send Http POST to server.
     * 
     * @param params
     * @param code
     * @return
     */
    protected boolean post(String url, HashMap<String, String> params, int code){
        try{
            Log.d("HttpObject", "post url: " + url + params.toString());
            (new HttpConnectionAsyn(mListener)).connect(HttpConnectionAsyn.POST, getHost() + url, setURLParams(HttpConnectionAsyn.POST, params),
                    setHeaderParams(), code);
        }
        catch(Exception e){
            e.printStackTrace();
            return false;
        }
        
        return true;
    }
    
    public void close(){
        mHandlerExited = true;
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
    protected String setURLParams(int method, Map<String, String> params) throws UnsupportedEncodingException{
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
    protected Map<String, String> setHeaderParams(){
        Map<String, String> map = new HashMap<String, String>();
        map.put(HttpConnectionAsyn.CONTENT_TYPE, HttpConnectionAsyn.CONTENT_TYPE_VALUE);
        map.put(HttpConnectionAsyn.ACCEPT, HttpConnectionAsyn.ACCEPT_VALUE);
        map.put(HttpConnectionAsyn.ACCEPT_CHARSET, HttpConnectionAsyn.ENCODING);
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
    protected String read2String(InputStream inStream){
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
    protected byte[] read2Byte(InputStream in) throws Exception{
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
