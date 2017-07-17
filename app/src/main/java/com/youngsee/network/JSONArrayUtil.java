package com.youngsee.network;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class JSONArrayUtil{
    private final static String TAG = "carrental_json_array";
    
    // 根据键值获取JSON数组
    public static JSONArray getJSONArray(JSONArray array, int index){
        JSONArray object = null;
        try{
            object = array.getJSONArray(index);
        }
        catch(JSONException e){
            Log.e(TAG, "getJSONArray by " + String.valueOf(index) + "failed");
            return null;
        }
        
        return object;
    }
    
    // 根据键值获取JSON对象
    public static JSONObject getJSONObject(JSONArray array, int index){
        JSONObject object;
        try{
            object = array.getJSONObject(index);
        }
        catch(JSONException e){
            Log.e(TAG, "getJSONArray by " + String.valueOf(index) + "failed");
            return null;
        }
        
        return object;
    }

    // 根据键值获取布尔值，为返回异常情况，此处以整形做为返回值， -1表示异常
    public static int getBoolean(JSONArray array, int index){
        boolean value;
        try{
            value = array.getBoolean(index);
        }
        catch(JSONException e){
            Log.e(TAG, "getBoolean by " + String.valueOf(index) + "failed");
            return -1;
        }
        
        return value ? 1 : 0;
    }

    // 根据键值获取浮点值， error用于出错的时候返回值
    public static double getDouble(JSONArray array, int index, double error){
        double value;
        try{
            value = array.getDouble(index);
        }
        catch(JSONException e){
            Log.e(TAG, "getDouble by " + String.valueOf(index) + "failed");
            return error;
        }
        
        return value;
    }
    
    // 根据键值获取整形值，  error用于出错的时候返回值
    public static int getInt(JSONArray array, int index, int error){
        int value;
        try{
            value = array.getInt(index);
        }
        catch(JSONException e){
            Log.e(TAG, "getInt by " + String.valueOf(index) + "failed");
            return error;
        }
        return value;
    }
    
    // 根据键值获取长整形值，  error用于出错的时候返回值
    public static long getLong(JSONArray array, int index, long error){
        long value;
        try{
            value = array.getLong(index);
        }
        catch(JSONException e){
            Log.e(TAG, "getLong by " + String.valueOf(index) + "failed");
            return error;
        }
        return value;
    }
    
    // 根据键值获取字符串
    public static String getString(JSONArray array, int index){
        String value;
        try{
            value = array.getString(index);
        }
        catch(JSONException e){
            Log.e(TAG, "getString by " + String.valueOf(index) + "failed");
            return null;
        }
        return value;
    }
}
