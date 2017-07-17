package com.youngsee.network;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

/**
 * Asynchronous HTTP connections 异步http连接
 * 
 */
public class HttpConnectionAsyn implements Runnable {

    public static final int GET = 0;
    public static final int POST = 1;
    private static final int UPLOADFILE = 2;
    private static final int BITMAP = 3;
    private static final int TIMEOUT = 50000;

    private static final String METHOD_POST = "POST";
    private static final String METHOD_GET = "GET";
    public static final String CONTENT_TYPE = "Content-type";
    public static final String CONTENT_TYPE_VALUE = "application/json; charset=utf-8";
    public static final String ACCEPT = "Accept";
    public static final String ACCEPT_VALUE = "application/json";
    public static final String ACCEPT_CHARSET = "Accept-Charset";
    public static final String ENCODING = "UTF-8";
    public static final String ACCEPT_ENCODING = "Accept-Encoding";
    public static final String ACCEPT_ENCODING_VALUE = "gzip, deflate";
    private static final String GZIP = "gzip";
    private static final String COOKIE = "Set-cookie";

    private int method;
    private OnResponseListener mListener;
    private String url;
    private String params;
    private int requestCode;
    private Map<String, String> headers;
    private File mFile;

    public HttpConnectionAsyn(OnResponseListener Listener) {
        mListener = Listener;
    }

    public void create(int method, String url, String params,
            Map<String, String> headers, int code, File file) {
        this.method = method;
        this.url = url;
        this.params = params;
        this.requestCode = code;
        this.headers = headers;
        this.mFile = file;
        ConnectionManager.getInstance().push(this);
    }

    public void connect(int method, String url, String params,
            Map<String, String> headers, int code) {
        create(method, url, params, headers, code, null);
    }

    public void upLoad(String url, String params, Map<String, String> headers, int code,
            File file) {
        create(UPLOADFILE, url, params, headers, code, file);
    }

    public void downLoad(String url, int code) {
        create(BITMAP, url, null, null, code, null);
    }

    public void run() {
        try {
            switch (method) {
            case GET:
                get_ResponseResult();
                break;
            case POST:
                post_ResponseResult();
                break;
            case UPLOADFILE:
                upLoadFile();
                break;
            case BITMAP:
                loadImg();
                break;
            }
            // 保存cookie
//            saveCookie(conn);
        } catch (Exception e) {
            e.printStackTrace();
            mListener.onError(e, requestCode);
        }
        ConnectionManager.getInstance().didComplete(this);
    }

    /**
     * GET请求
     * 
     * @param params
     * @throws Exception
     */
    private void get_ResponseResult() throws Exception {
        URL urlStr = new URL(url + params);
        HttpURLConnection conn = (HttpURLConnection) urlStr.openConnection();
        try{
            conn.setConnectTimeout(TIMEOUT);
            conn.setRequestMethod(METHOD_GET);
            setHeaders(conn);
            if (GZIP.equals(conn.getContentEncoding())) {
            	mListener.onResponse(new GZIPInputStream(conn.getInputStream()),
    					conn.getResponseCode(), requestCode);
            } else {
                mListener.onResponse(conn.getInputStream(), conn.getResponseCode(), requestCode);
            }
        }
        finally{
            conn.disconnect();
        }
    }

    /**
     * POST请求
     * 
     * @param params
     * @throws Exception
     */
    private void post_ResponseResult() throws Exception {
        URL urlStr = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urlStr.openConnection();
        try {
	        	conn.setDoOutput(true);
	        	conn.setDoInput(true);
	        	conn.setUseCaches(false);
//	            conn.setChunkedStreamingMode(0);
	        	byte[] buffer = params.getBytes(ENCODING);
	        	conn.setFixedLengthStreamingMode(buffer.length);
	            conn.setConnectTimeout(TIMEOUT);
	            conn.setRequestMethod(METHOD_POST);
	            setHeaders(conn);
	            conn.connect();
	
                OutputStream out = conn.getOutputStream();
	            out.write(buffer);
	            out.flush();
	            out.close();
	
	            int responseCode = conn.getResponseCode();  
	            if (responseCode == HttpURLConnection.HTTP_OK) {
		            if (GZIP.equals(conn.getContentEncoding())) {
		            	GZIPInputStream stream = new GZIPInputStream(conn.getInputStream());
		                mListener.onResponse(stream, conn.getResponseCode(), requestCode);
		                stream.close();
		            } else {
		                mListener.onResponse(conn.getInputStream(), conn.getResponseCode(), requestCode);
		            }
	            }
	            else{ 
	            	mListener.onResponse(null,200, requestCode);
	            }  
        	}
           finally {
        	   conn.disconnect();
          }
    }

    /**
     * 上传图片
     * 
     * @throws Exception
     */
    private void upLoadFile() throws Exception {
        URL urlStr = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urlStr.openConnection();
        try{
            conn.setConnectTimeout(TIMEOUT);
            conn.setRequestMethod(METHOD_POST);
            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
            FileInputStream fis = new FileInputStream(mFile);
            setHeaders(conn);
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int length = -1;
            /* 从文件读取数据至缓冲区 */
            while ((length = fis.read(buffer)) != -1) {
                /* 将资料写入DataOutputStream中 */
                dos.write(buffer, 0, length);
            }
            fis.close();
            if (params != null && !params.isEmpty()) {
                dos.write(params.getBytes(ENCODING));
                dos.flush();
                dos.close();
                conn.setDoOutput(true);
            }
            if (GZIP.equals(conn.getContentEncoding())) {
                mListener.onResponse(new GZIPInputStream(conn.getInputStream()),
                        conn.getResponseCode(), requestCode);
            } else {
                mListener.onResponse(conn.getInputStream(), conn.getResponseCode(), requestCode);
            }
        }
        finally{
            conn.disconnect();
        }
    }

    /**
     * 下载图片
     * 
     * @throws Exception
     */
    private void loadImg() throws Exception {
        URL urlStr = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urlStr.openConnection();
        try{
            InputStream in = conn.getInputStream();// 得到读取的内容
            mListener.onImgResponse(in, conn.getResponseCode(), requestCode);
        }
        finally{
            conn.disconnect();
        }
    }

    /**
     * 设置http请求头
     * 
     * @throws UnsupportedEncodingException
     */
    private void setHeaders(HttpURLConnection conn) throws UnsupportedEncodingException {
        if (headers != null && !headers.isEmpty()) {
            Set<Entry<String, String>> entries = headers.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                conn.setRequestProperty(entry.getKey(),entry.getValue());
            }
        }
    }

    /**
     * 保存cookie
     * 
     * @param connection
     */
//    private void saveCookie(HttpURLConnection connection) {
//        String key = null;
//        for (int i = 1; (key = connection.getHeaderFieldKey(i)) != null; i++) {
//            if (key.equalsIgnoreCase(COOKIE)) {
//                // PreferenceManager.getDefaultSharedPreferences(MyApplication.mContext).edit().putString("session",
//                // connection.getHeaderField(i).substring(0,
//                // connection.getHeaderField(i).indexOf(";")));
//            }
//        }
//    }

    public interface OnResponseListener {
        void onError(Exception e, int requestCode);

        void onResponse(InputStream in, int statusCode, int requestCode);

        void onImgResponse(InputStream in, int statusCode, int requestCode);
    }

    /**
     * 链接管理工具类
     * 
     * @author
     * 
     */
    private static class ConnectionManager {

        public static final int MAX_CONNECTIONS = 5;

        private ArrayList<Runnable> active = new ArrayList<Runnable>();
        private ArrayList<Runnable> queue = new ArrayList<Runnable>();
        private static ExecutorService mService = Executors
                .newCachedThreadPool();

        private static ConnectionManager instance;

        public static ConnectionManager getInstance() {
            if (instance == null) {
                instance = new ConnectionManager();
            }
            return instance;
        }

        public void push(Runnable runnable) {
            queue.add(runnable);
            if (active.size() < MAX_CONNECTIONS)
                startNext();
        }

        private void startNext() {
            if (!queue.isEmpty()) {
                Runnable next = queue.get(0);
                queue.remove(0);
                active.add(next);
                mService.execute(next);
            }
        }

        public void didComplete(Runnable runnable) {
            active.remove(runnable);
            startNext();
        }
    }
}