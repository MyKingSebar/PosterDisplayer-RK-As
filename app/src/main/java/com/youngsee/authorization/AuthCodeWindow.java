package com.youngsee.authorization;

import com.youngsee.posterdisplayer.R;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;

public class AuthCodeWindow extends PopupWindow implements OnClickListener{
	
	private View mView;
    private OnCodeListener mListener;
    
    public AuthCodeWindow(View view, OnCodeListener listener){
        super(view, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

    	mView = view;
    	mListener = listener;
    	
    	Button button = (Button)mView.findViewById(R.id.BtnConfirm);
    	button.setOnClickListener(this);
    	button = (Button)mView.findViewById(R.id.BtnCancel);
    	button.setOnClickListener(this);
    }

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.BtnConfirm:
			String code = ((EditText)mView.findViewById(R.id.ETCode)).getText().toString();
			code = code.replace(" ", "");
			code = code.replace("-", "");
			String company = ((EditText)mView.findViewById(R.id.ETCompany)).getText().toString();
			if(mListener != null){
				mListener.onCode(code, company);
			}
			break;
		case R.id.BtnCancel:
			break;
		}
		dismiss();
	}

	public interface OnCodeListener{
		public void onCode(String code, String company);
	}
}
