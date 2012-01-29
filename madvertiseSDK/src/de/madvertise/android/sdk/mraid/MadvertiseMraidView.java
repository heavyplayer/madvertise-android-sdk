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

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
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

    private FrameLayout mEnlargeLayout;

    protected static final int STATE_LOADING = 0;

    protected static final int STATE_HIDDEN = 1;

    protected static final int STATE_DEFAULT = 2;

    protected static final int STATE_EXPANDED = 3;

    // protected static final String STATE_HIDDEN = "hidden";
    // protected static final String STATE_LOADING = "loading";
    // protected static final String STATE_DEFAULT = "default";
    // protected static final String STATE_EXPANDED = "expanded";

    private String mPlacementType;

    private int mState = STATE_LOADING;

    private MadvertiseViewCallbackListener mListener;

    private AnimationEndListener mAnimationEndListener;
    
    private int mIndex;

    private boolean mUseCustomClose = false;
    
    private Handler mLoadingCompletedHandler;
    
    private MadvertiseAd mAd;
    

    public MadvertiseMraidView(Context context, AttributeSet attrs,
            MadvertiseViewCallbackListener listener, AnimationEndListener animationEndListener, Handler loadingCompletedHandler, MadvertiseAd ad) {
        this(context);
        String packageName = "http://schemas.android.com/apk/res/"
                + getContext().getApplicationContext().getPackageName();
        mPlacementType = attrs.getAttributeValue(packageName, "placement_type");
        if (mPlacementType == null || mPlacementType.equals("")) {
            mPlacementType = MadvertiseUtil.PLACEMENT_TYPE_INLINE;
        }
        this.mListener = listener;
        this.mAnimationEndListener = animationEndListener;
        this.mLoadingCompletedHandler = loadingCompletedHandler;
        this.mAd = ad;
        
//        loadUrl(mAd.getBannerUrl());
    }

    public MadvertiseMraidView(Context context) {
        super(context);
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);

        setBackgroundColor(Color.TRANSPARENT);

        final WebSettings webSettings = getSettings();
        webSettings.setJavaScriptEnabled(true);

        getSettings().setJavaScriptEnabled(true);
        addJavascriptInterface(mBridge, "mraid_bridge");
        loadJs();

        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d("TEST", "FINISHED loading");
                setState(STATE_DEFAULT);
                fireEvent("ready");
                mLoadingCompletedHandler.sendEmptyMessage(MadvertiseView.MAKE_VISIBLE);
            }
        });
    }

    // to be called from the App (java side)

    public void setState(int state) {
        mState = state;
        injectJs("mraid.setState('" + state + "');");
    }

    public void fireEvent(String event) {
        injectJs("mraid.fireEvent('" + event + "');");
    }

    private void injectJs(String jsCode) {
        loadUrl("javascript:" + jsCode);
    }

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

        public void setExpandProperties(final String json) {
            // mExpandProperties.fromJson(json);
            // mUseCustomClose = mExpandProperties.useCustomClose;
            // setExpandProperties(mExpandProperties);
            // TODO: Resize ad size
        }

        // we could just pass comma-separated values in a certain order?
        // or set Properties separately?
        // like e.g.:
        public void setUseCustomClose(String customClose) {
        }

        public void setExpandDimensions(String dimensions) {
        }
    };

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

    private ViewGroup mOriginalParent;

    public void expand(final int width, final int height) {
        final FrameLayout content = (FrameLayout)getRootView().findViewById(android.R.id.content);

        final FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(width, height);

        final View placeholderView = new View(getContext());
        placeholderView.setLayoutParams(getLayoutParams());

        mEnlargeLayout = new FrameLayout(getContext());
        final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        mEnlargeLayout.setLayoutParams(layoutParams);

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
        mEnlargeLayout.addView(this);

        final ImageButton closeButton = new ImageButton(getContext());
        final FrameLayout.LayoutParams closeButtonParams = new FrameLayout.LayoutParams(
                CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE);
        closeButtonParams.gravity = Gravity.RIGHT;
        closeButton.setLayoutParams(closeButtonParams);
        closeButton.setBackgroundColor(Color.TRANSPARENT);
        mEnlargeLayout.addView(closeButton);

        if (!mUseCustomClose) {
            closeButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        }

        content.addView(mEnlargeLayout);

        mOriginalParent.addView(placeholderView, mIndex);

        mState = STATE_EXPANDED;
    }

    public void close() {
        switch (mState) {
            case STATE_EXPANDED:
                if (mEnlargeLayout != null) {
                    ((ViewGroup)mEnlargeLayout.getParent()).removeView(mEnlargeLayout);
                    mEnlargeLayout.removeView(this);
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
