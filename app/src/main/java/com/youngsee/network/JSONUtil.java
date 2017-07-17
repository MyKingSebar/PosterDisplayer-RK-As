package com.youngsee.network;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class JSONUtil{
    private final String TAG = "carrental_json";
    private JSONObject   mJSONObject;
    
    public JSONUtil(String str) throws JSONException{
        this.mJSONObject = new JSONObject(str);
    }
    
    // 根据键值获取JSON数组
    public JSONArray getJSONArray(String name){
        JSONArray object = null;
        try{
            object = mJSONObject.getJSONArray(name);
        }
        catch(JSONException e){
            Log.e(TAG, "getJSONArray by " + name + " failed");
            return null;
        }
        
        return object;
    }
    
    // 根据键值获取JSON对象
    public JSONObject getJSONObject(String name){
        JSONObject object = null;
        try{
            object = mJSONObject.getJSONObject(name);
        }
        catch(JSONException e){
            Log.e(TAG, "getJSONObject by " + name + " failed");
            return null;
        }
        
        return object;
    }

    // 根据键值获取布尔值，为返回异常情况，此处以整形做为返回值， -1表示异常
    public int getBoolean(String name){
        boolean value;
        try{
            value = mJSONObject.getBoolean(name);
        }
        catch(JSONException e){
            Log.e(TAG, "getBoolean by " + name + " failed");
            return -1;
        }
        
        return value ? 1 : 0;
    }

    // 根据键值获取浮点值， error用于出错的时候返回值
    public double getDouble(String name, double error){
        double value;
        try{
            value = mJSONObject.getDouble(name);
        }
        catch(JSONException e){
            Log.e(TAG, "getDouble by " + name + " failed");
            return error;
        }
        
        return value;
    }
    
    // 根据键值获取整形值，  error用于出错的时候返回值
    public int getInt(String name, int error){
        int value;
        try{
            value = mJSONObject.getInt(name);
        }
        catch(JSONException e){
            Log.e(TAG, "getInt by " + name + " failed");
            return error;
        }
        return value;
    }
    
    // 根据键值获取长整形值，  error用于出错的时候返回值
    public long getLong(String name, long error){
        long value;
        try{
            value = mJSONObject.getLong(name);
        }
        catch(JSONException e){
            Log.e(TAG, "getLong by " + name + " failed");
            return error;
        }
        return value;
    }
    
    // 根据键值获取字符串
    public String getString(String name){
        String value;
        try{
            value = mJSONObject.getString(name);
        }
        catch(JSONException e){
            Log.e(TAG, "getString by " + name + " failed");
            return "";
        }
        return value;
    }
}
