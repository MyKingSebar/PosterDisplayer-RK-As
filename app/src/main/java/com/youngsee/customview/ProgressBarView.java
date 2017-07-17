/*
 * Copyright (C) 2013 poster PCE
 * YoungSee Inc. All Rights Reserved
 * Proprietary and Confidential. 
 * @author LiLiang-Ping
 */

package com.youngsee.customview;

import com.youngsee.posterdisplayer.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

public class ProgressBarView extends LinearLayout
{
    private Context mContext = null;

    public ProgressBarView(Context context) {
        super(context);
        mContext = context;
        initVeiw(mContext);
    }

    public ProgressBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initVeiw(mContext);
    }

    public ProgressBarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        initVeiw(mContext);
    }
    
    private void initVeiw(Context context)
    {
        if (context != null)
        {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(R.layout.view_progressbar, this);
        }
    }
}
