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

package de.madvertise.android.sdk.mraid;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Picture;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import de.madvertise.android.sdk.MadvertiseAd;
import de.madvertise.android.sdk.MadvertiseUtil;
import de.madvertise.android.sdk.MadvertiseView;
import de.madvertise.android.sdk.MadvertiseView.AnimationEndListener;
import de.madvertise.android.sdk.MadvertiseView.MadvertiseViewCallbackListener;
import de.madvertise.android.sdk.R;

public class MadvertiseMraidView extends WebView {

    private static final int CLOSE_BUTTON_SIZE = 50;

    private static final String JS_INTERFACE_NAME = "mraid";

    private static final String TAG = MadvertiseMraidView.class.getCanonicalName();

    private FrameLayout mExpandLayout;

    protected static final int STATE_LOADING = 0;

    protected static final int STATE_HIDDEN = 1;

    protected static final int STATE_DEFAULT = 2;

    protected static final int STATE_EXPANDED = 3;

    // protected static final String STATE_HIDDEN = "hidden";
    // protected static final String STATE_LOADING = "loading";
    // protected static final String STATE_DEFAULT = "default";
    // protected static final String STATE_EXPANDED = "expanded";


    private int mState;
    private int mIndex;
    private MadvertiseAd mAd;
    private int mPlacementType;
    private ViewGroup mOriginalParent;
    private Handler mLoadingCompletedHandler;
    private ExpandProperties mExpandProperties;
    private MadvertiseViewCallbackListener mListener;
    private AnimationEndListener mAnimationEndListener;



    public MadvertiseMraidView(Context context, AttributeSet attrs,
            MadvertiseViewCallbackListener listener, AnimationEndListener animationEndListener, Handler loadingCompletedHandler, MadvertiseAd ad) {
        this(context);
        String packageName = "http://schemas.android.com/apk/res/" + getContext().getPackageName();
        setPlacementType(attrs.getAttributeValue(packageName, "placement_type"));
        this.mLoadingCompletedHandler = loadingCompletedHandler;
        this.mAnimationEndListener = animationEndListener;
        this.mListener = listener;
        this.mAd = ad;
        
//        loadUrl(mAd.getBannerUrl());
    }

    public MadvertiseMraidView(Context context) {
        super(context);
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);

//        setBackgroundColor(Color.TRANSPARENT);

        getSettings().setJavaScriptEnabled(true);
        addJavascriptInterface(mBridge, "mraid_bridge");
        loadJs();
        
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        mExpandProperties = new ExpandProperties(metrics.widthPixels, metrics.heightPixels);
        injectJs("mraid.setExpandProperties("+mExpandProperties.toJson()+");");
        
        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d("TEST", "onPageFinished");
                setState(STATE_DEFAULT);
                fireEvent("ready");
                if (mLoadingCompletedHandler != null)
                    mLoadingCompletedHandler.sendEmptyMessage(MadvertiseView.MAKE_VISIBLE);
            }
        });
    }
    
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Log.d("TEST", "onLayout (changed="+changed+")");
        injectJs("mraid.setViewable(true);");
        super.onLayout(changed, l, t, r, b);
    };

//    protected void onWindowVisibilityChanged(int visibility) {
//        Log.d("TEST", "onWindowVisibilityChanged "+visibility);
//        super.onWindowVisibilityChanged(visibility);
//    };
//
//    protected void onAttachedToWindow() {
//        Log.d("TEST", "onAttachedToWindow");
//        super.onAttachedToWindow();
//    };
//    
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        Log.d("TEST", "onMeasure");
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//    };



    // to be called from the Ad (js side)
    
    Object mBridge = new Object() {

        public void expand(final String url) {
            // TODO expand on the screen..
            setState(STATE_EXPANDED);
        }

        public void close() {
            // TODO close :-)
            setState(mState--);
        }

        public void open(String url) {
            // TODO start (ORMMA?) BrowseActivity
            mListener.onAdClicked();
        }
        
        public void setExpandProperties(String json) {
            mExpandProperties.readJson(json);
        }
    };



    // to be called from the App (java side)

    public void setState(int state) {
        mState = state;
        injectJs("mraid.setState('" + state + "');");
    }

    public void fireEvent(String event) {
        injectJs("mraid.fireEvent('" + event + "');");
    }
    
    public ExpandProperties getExpandProperties() {
        return mExpandProperties;
    }
    
    public String getPlacementType() {
        switch (mPlacementType) {
        case MadvertiseUtil.PLACEMENT_TYPE_INTERSTITIAL:
            return "interstitial";
        default:
            return "inline";
        }
    }
    
    public void setPlacementType(String placementType) { // use enum here?
        if (placementType.equals("interstitial")) {
            mPlacementType = MadvertiseUtil.PLACEMENT_TYPE_INTERSTITIAL;
        } else {
            mPlacementType = MadvertiseUtil.PLACEMENT_TYPE_INLINE;
        }
        injectJs("mraid.setPlacementType("+mPlacementType+");");
    }
    
    private void injectJs(String jsCode) {
        loadUrl("javascript:" + jsCode);
    }



    public void expand(final int width, final int height) {
        final FrameLayout content = (FrameLayout)getRootView().findViewById(android.R.id.content);

        final FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(width, height);

        final View placeholderView = new View(getContext());
        placeholderView.setLayoutParams(getLayoutParams());

        mExpandLayout = new FrameLayout(getContext());
        final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        mExpandLayout.setLayoutParams(layoutParams);

        mOriginalParent = (ViewGroup)getParent();

        int index = 0;
        int count = mOriginalParent.getChildCount();
        for (index = 0; index < count; index++) {
            if (mOriginalParent.getChildAt(index) == this)
                break;
        }

        mIndex = index;

        this.setLayoutParams(adParams);
        mOriginalParent.removeView(this);
        mExpandLayout.addView(this);

        final ImageButton closeButton = new ImageButton(getContext());
        final FrameLayout.LayoutParams closeButtonParams = new FrameLayout.LayoutParams(
                CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE);
        closeButtonParams.gravity = Gravity.RIGHT;
        closeButton.setLayoutParams(closeButtonParams);
        closeButton.setBackgroundColor(Color.TRANSPARENT);
        mExpandLayout.addView(closeButton);

        if (!mExpandProperties.useCustomClose) {
            closeButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        }

        content.addView(mExpandLayout);
        mOriginalParent.addView(placeholderView, mIndex);
        mState = STATE_EXPANDED;
    }

    public void close() {
        switch (mState) {
            case STATE_EXPANDED:
                if (mExpandLayout != null) {
                    ((ViewGroup)mExpandLayout.getParent()).removeView(mExpandLayout);
                    mExpandLayout.removeView(this);
                    this.setLayoutParams(mOriginalParent.getChildAt(mIndex).getLayoutParams());
                    mOriginalParent.removeViewAt(mIndex);
                    mOriginalParent.addView(this, mIndex);
                }
                break;
            case STATE_DEFAULT:
                //TODO: set MadvertiseView to GONE.
                setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        boolean isVisible;
        if (visibility == View.VISIBLE) {
            isVisible = true;
        } else {
            isVisible = false;
        }
        // mBridge.setIsViewable(isVisible);
    }
    
//    public void setPlacementType(final String type) {
//        mBridge.setPlacementType(type);
//    }




    public class ExpandProperties {

        private static final String WIDTH = "width";
        private static final String HEIGHT = "height";
        private static final String USE_CUSTOM_CLOSE = "useCustomClose";
        private static final String IS_MODAL = "isModal";

        public int width;
        public int height;
        public boolean useCustomClose;
        public boolean isModal;

        ExpandProperties(final int width, final int height) {
            this.width = width;
            this.height = height;
        }

        public String toJson() {
            final JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put(WIDTH, width);
                jsonObject.put(HEIGHT, height);
                jsonObject.put(USE_CUSTOM_CLOSE, useCustomClose);
                jsonObject.put(IS_MODAL, isModal);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObject.toString();
        }

        void readJson(final String json) {
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(json);

                if (jsonObject.has(HEIGHT)) {
                    width = jsonObject.getInt(WIDTH);
                }
                if (jsonObject.has(WIDTH)) {
                    height = jsonObject.getInt(HEIGHT);
                }
                if (jsonObject.has(USE_CUSTOM_CLOSE)) {
                    useCustomClose = jsonObject.getBoolean(USE_CUSTOM_CLOSE);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }



    // Utility methods
    private void loadJs() {
        String script = "";
        try {
            InputStream is = getContext().getResources().openRawResource(R.raw.mraid);
            int size = is.available();
            byte[] buffer = new byte[size + 11];
            byte[] js = "javascript:".getBytes();
            for (int i = 0; i < 11; i++)
                buffer[i] = js[i];
            is.read(buffer, 11, size);
            is.close();
            script = new String(buffer);
            // MadvertiseUtil.logMessage(TAG, Log.DEBUG, script);
        } catch (IOException e) {
            MadvertiseUtil.logMessage(TAG, Log.ERROR, "error reading mraid.js");
        }
        loadUrl(script);
    }
    
    @Override
    protected void onAnimationEnd() {
        super.onAnimationEnd();
        mAnimationEndListener.onAnimationEnd();
    }
}
