package com.youngsee.network;

import org.json.JSONException;

import android.content.Context;
import android.widget.Toast;

import com.youngsee.posterdisplayer.R;

public class HttpResultHandler{
	Context mContext;
	
	public HttpResultHandler(Context context){
		mContext = context;
	}
	
    public int getErrorCode(String jsonStr){

        String toast = null;
        int result = -999;
        
        try{
            JSONUtil json = new JSONUtil(jsonStr);
            result = json.getInt(NetWorkUtil.KEY_ERROR_CODE, -999);
            if(result == -51){
            	toast = mContext.getResources().getString(R.string.toast_exceed_register_count);
            }
            else if(result == -52){
            	toast = mContext.getResources().getString(R.string.unauthorized_msg);
            }
            else if(result < NetWorkUtil.RESULT_OK){
                toast = mContext.getResources().getString(R.string.toast_wrong_register_code);
            }
        }
        catch(JSONException e){
            toast = mContext.getResources().getString(R.string.toast_network_failed);
        }
        if(toast != null){
        	Toast.makeText(mContext, toast, Toast.LENGTH_LONG).show();
        }
    	
        return result;
    }
    
    public JSONUtil getJSONUtilObjec(String jsonStr){
        JSONUtil json = null;
        String toast = null;
        
        try{
            json = new JSONUtil(jsonStr);
        }
        catch(JSONException e){
            toast = mContext.getResources().getString(R.string.toast_wrong_service_param);
        }
        
        if(toast != null){
            Toast.makeText(mContext, toast, Toast.LENGTH_LONG).show();
        }
        
        return json;
    }
}
