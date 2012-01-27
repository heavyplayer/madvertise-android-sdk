/*
 * Copyright 2011 madvertise Mobile Advertising GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Container for rich media ads using the MRAID version 1.0 - standard
 */

//TODO: 

package de.madvertise.android.sdk.mraid;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import de.madvertise.android.sdk.MadvertiseUtil;
import de.madvertise.android.sdk.MadvertiseView.MadvertiseViewCallbackListener;
import de.madvertise.android.sdk.R;

public class MadvertiseMraidView extends WebView {

    

//    private static final int CLOSE_BUTTON_SIZE = 50;

    private static final String JS_INTERFACE_NAME = "mraid";

    private static final String TAG = MadvertiseMraidView.class.getCanonicalName();

//    private FrameLayout mEnlargeLayout;
    
    private MadvertiseViewCallbackListener mListener;
    
    private MraidBridge mBridge;
    
    public MadvertiseMraidView(Context context) {
        super(context);
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        getSettings().setJavaScriptEnabled(true);
        init();        
    }

    public MadvertiseMraidView(Context context, AttributeSet attrs, String url,
            MadvertiseViewCallbackListener listener) {
        super(context, attrs);
        
        this.mListener = listener;
        
        init();

        loadUrl(url);
    }

    private void init() {
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);

        // setBackgroundColor(Color.TRANSPARENT);

        final WebSettings webSettings = getSettings();
        webSettings.setJavaScriptEnabled(true);

        mBridge = new MraidBridge(this, mListener);
        
        this.addJavascriptInterface(mBridge, JS_INTERFACE_NAME);

        final WebViewClient webViewClient = new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mBridge.onLoadingComplete();
            }
        };
        setWebViewClient(webViewClient);
        
        loadJs();
    }
    
    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        boolean isVisible;
        if(visibility == View.VISIBLE) {
            isVisible = true;
        } else {
            isVisible = false;
        }
        mBridge.setIsViewable(isVisible);
    }
    
    public void setPlacementType(final String type) {
        mBridge.setPlacementType(type);
    }

  

//    public void expand(final int width, final int height) {
//        FrameLayout content = (FrameLayout)getRootView().findViewById(android.R.id.content);
//
//        final FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(width, height);
//
//        mEnlargeLayout = new FrameLayout(getContext());
//        final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
//                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
//        layoutParams.gravity = Gravity.CENTER;
//        mEnlargeLayout.setLayoutParams(layoutParams);
//
//        this.setLayoutParams(adParams);
//        ((ViewGroup)getParent()).removeView(this);
//        mEnlargeLayout.addView(this);
//
//        if (!mUseCustomClose) {
//            final ImageView imageView = new ImageView(getContext());
//            imageView.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
//
//            final FrameLayout.LayoutParams closeButtonParams = new FrameLayout.LayoutParams(
//                    CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE);
//            closeButtonParams.gravity = Gravity.RIGHT;
//            imageView.setLayoutParams(closeButtonParams);
//            mEnlargeLayout.addView(imageView);
//        }
//
//        content.addView(mEnlargeLayout);
//
//        mEnlarged = true;
//    }

//    public void close() {
//        if (mEnlargeLayout != null) {
//            ((ViewGroup)mEnlargeLayout.getParent()).removeView(mEnlargeLayout);
//        }
//    }
 

//    @Override
//    public boolean dispatchTouchEvent(MotionEvent event) {
//        
//        if (mEnlarged && getWidth() - event.getX() < CLOSE_BUTTON_SIZE
//                && event.getY() < CLOSE_BUTTON_SIZE && event.getAction() == MotionEvent.ACTION_UP) {
//            close();
//        }
//
//        return super.dispatchTouchEvent(event);
//
//    }

    
    //Utility methods
    private void loadJs() {
        String script = "";
        try {
            InputStream is = getContext().getResources().openRawResource(R.raw.mraid);
            int size = is.available();
            byte[] buffer = new byte[size+11];
            byte[] js = "javascript:".getBytes();
            for (int i = 0; i < 11; i++) buffer[i] = js[i];
            is.read(buffer, 11, size);
            is.close();
            script = new String(buffer);
//            MadvertiseUtil.logMessage(TAG, Log.DEBUG, script);
        } catch (IOException e) {
            MadvertiseUtil.logMessage(TAG, Log.ERROR, "error reading mraid.js");
        }
        loadUrl(script);
    }
}
