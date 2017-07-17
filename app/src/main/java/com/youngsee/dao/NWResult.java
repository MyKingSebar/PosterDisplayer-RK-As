package com.youngsee.dao;

import org.json.JSONException;

import android.os.Parcel;
import android.os.Parcelable;
import com.youngsee.network.JSONUtil;

public class NWResult implements Parcelable{
    // Error code of success.
    public static final int     RESULT_OK        = 0;
    public static final int     ERR_INVALID_JSON = -1000;
    
    // key of error code
    private static final String KEY_ERROR_CODE   = "errorcode";
    
    //The error code of server. Larger than 0 or equal to 0 for success result.
    private int                 mErrorCode;
    protected String            mJson;
    
    public NWResult(){
    }
    
    public NWResult(String json){
        mJson = json;
        parseJson(json);
    }
    
    public NWResult(Parcel in){
        mErrorCode = in.readInt();
        mJson = in.readString();
    }
    
    /**
     * The error code from server.
     * 
     * @return
     */
    public int getErrorCode(){
        return mErrorCode;
    }
    
    /**
     * Parse the error code from JSON String.
     * 
     * @param jsonStr
     * @return
     */
    protected boolean parseJson(String jsonStr){
        mErrorCode = ERR_INVALID_JSON;
        
        try{
            JSONUtil json = new JSONUtil(jsonStr);
            mErrorCode = json.getInt(KEY_ERROR_CODE, ERR_INVALID_JSON);
        }
        catch(JSONException e){
            e.printStackTrace();
            return false;
        }
        
        return true;
    }
    
    /**
     * Get JSONUtil object
     * @return
     */
    protected JSONUtil getJSONUtil(){
        JSONUtil json = null;
        
        try{
            json = new JSONUtil(mJson);
        }
        catch(JSONException e){
            e.printStackTrace();
        }
        
        return json;
    }

    @Override
    public int describeContents(){
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags){
        dest.writeInt(mErrorCode);
        dest.writeString(mJson);
    }
    
    public static final Parcelable.Creator<NWResult> CREATOR = new Parcelable.Creator<NWResult>() {
        public NWResult createFromParcel(Parcel in) {
            NWResult driver = new NWResult(in);
            return driver;
        }
        
        public NWResult[] newArray(int size) {
            return new NWResult[size];
        }
   };
}
