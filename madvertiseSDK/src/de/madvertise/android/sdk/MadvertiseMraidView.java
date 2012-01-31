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

package de.madvertise.android.sdk;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Color;
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
import de.madvertise.android.sdk.MadvertiseView.AnimationEndListener;
import de.madvertise.android.sdk.MadvertiseView.MadvertiseViewCallbackListener;

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

    private int mPlacementType;

    private ViewGroup mOriginalParent;

    private Handler mLoadingCompletedHandler;

    private ExpandProperties mExpandProperties;

    private MadvertiseViewCallbackListener mListener;

    private AnimationEndListener mAnimationEndListener;

    public MadvertiseMraidView(Context context, AttributeSet attrs,
            MadvertiseViewCallbackListener listener, AnimationEndListener animationEndListener,
            Handler loadingCompletedHandler, MadvertiseAd ad) {
        this(context);

        int placementType = -1;
        if (attrs != null) {
//             TODO: this throws an exception
//             final String packageName = "http://schemas.android.com/apk/res/"
//             + getContext().getApplicationContext().getPackageName();
//             final String placementTypeStr = attrs.getAttributeValue(packageName,
//             "placement_type");
//             if(placementTypeStr.equalsIgnoreCase("inline")) {
//                 placementType = MadvertiseUtil.PLACEMENT_TYPE_INLINE;
//             } else if(placementTypeStr.equalsIgnoreCase("interstitial")) {
//                 placementType = MadvertiseUtil.PLACEMENT_TYPE_INTERSTITIAL;
//             }
        }
        
        if (placementType == -1) {
            placementType = MadvertiseUtil.PLACEMENT_TYPE_INLINE;
        }        
        setPlacementType(placementType);

        this.mLoadingCompletedHandler = loadingCompletedHandler;
        this.mAnimationEndListener = animationEndListener;
        this.mListener = listener;

        if (ad.getBannerUrl().endsWith(".js")) {
            // TODO: Make this work!!!

            char[] start = new char[128], end = new char[32];
            final int sep = ad.getBannerUrl().lastIndexOf("/");
            ad.getBannerUrl().getChars(0, sep + 1, start, 0);
            ad.getBannerUrl().getChars(sep + 1, ad.getBannerUrl().length(), end, 0);

            final String bannerStart = new String(start);
            final String bannerEnd = new String(end);

            loadDataWithBaseURL(bannerStart, "<html><head><script type=\"text/javascript\" src=\""
                    + bannerEnd + "\"/></head><body></body></html>", "text/html", "utf8", null);
        } else {
            loadUrl(ad.getBannerUrl());
        }

    }

    public MadvertiseMraidView(Context context) {
        super(context);
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);

        // setBackgroundColor(Color.TRANSPARENT);

        getSettings().setJavaScriptEnabled(true);
        addJavascriptInterface(mBridge, "mraid_bridge");
        loadJs();

        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        mExpandProperties = new ExpandProperties(metrics.widthPixels, metrics.heightPixels);
        injectJs("mraid.setExpandProperties(" + mExpandProperties.toJson() + ");");

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

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        // When the window is invisible, our view is too
        if(visibility != View.VISIBLE) {            
            setViewable(false);
        }
        super.onWindowVisibilityChanged(visibility);
    };

    @Override
    protected void onDetachedFromWindow() {
        // When not attached to a window, we are invisible
        setViewable(false);
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
        setViewable(isVisible);
    }
    

    // to be called from the Ad (js side)

    Object mBridge = new Object() {

        public void expand() {
            post(new Runnable() {
                @Override
                public void run() {
                    MadvertiseMraidView.this.resize(mExpandProperties.width,
                            mExpandProperties.height);
                }
            });
            setState(STATE_EXPANDED);
        }

        public void expand(String url) {
            Log.d("TEST", "expand with url");
            loadUrl(url);
            expand();
        }

        public void close() {
            post(new Runnable() {
                @Override
                public void run() {
                    MadvertiseMraidView.this.close();
                }
            });
            setState(mState--);
        }

        public void open(String url) {
            // TODO start (ORMMA?) BrowseActivity
            if (mListener != null) {
                mListener.onAdClicked();
            }
        }

        public void setExpandProperties(String json) {
            mExpandProperties.readJson(json);
        }
    };

    // to be called from the App (java side)

    private void setState(int state) {
        mState = state;
        injectJs("mraid.setState('" + state + "');");
    }

    private void fireEvent(String event) {
        injectJs("mraid.fireEvent('" + event + "');");
    }

    public String getPlacementType() {
        switch (mPlacementType) {
            case MadvertiseUtil.PLACEMENT_TYPE_INTERSTITIAL:
                return "interstitial";
            default:
                return "inline";
        }
    }

    public void setPlacementType(int placementType) { 
        if(placementType != MadvertiseUtil.PLACEMENT_TYPE_INTERSTITIAL && placementType != MadvertiseUtil.PLACEMENT_TYPE_INLINE) {
            MadvertiseUtil.logMessage(null, Log.WARN, "Placement type must be one of MadvertiseUtil.PLACEMENT_TYPE_INLINE or MadvertiseUtil.PLACEMENT_TYPE_INTERSTITIAL");
        } else {
            mPlacementType = placementType;
            injectJs("mraid.setPlacementType(" + mPlacementType + ");");
        }
    }
    
    private void setViewable(final boolean isViewable) {
        injectJs("mraid.setViewable(" + isViewable + ");");
    }

    private void injectJs(String jsCode) {
        loadUrl("javascript:" + jsCode);
    }

    private void resize(final int width, final int height) {
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

    private void close() {
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
                // TODO: set MadvertiseView to GONE.
                setVisibility(View.GONE);
                break;
        }
    }

    public class ExpandProperties {

        private static final String WIDTH = "width";

        private static final String HEIGHT = "height";

        private static final String USE_CUSTOM_CLOSE = "useCustomClose";

        private static final String IS_MODAL = "isModal";

        private int mMaxWidth;

        private int mMaxHeight;

        public int width;

        public int height;

        public boolean useCustomClose;

        public boolean isModal;

        public ExpandProperties(final int width, final int height) {
            this.width = width;
            this.height = height;
            this.mMaxWidth = width;
            this.mMaxHeight = height;
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

        public void readJson(final String json) {
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

            checkSizeParams();
        }

        private void checkSizeParams() {
            if (width > mMaxWidth || height > mMaxHeight) {
                final float ratio = height / (float)width;
                // respect the ratio
                final int diffWidth = (int)((float)(width - mMaxWidth) * ratio);
                final int diffHeight = (int)((float)(height - mMaxHeight) * ratio);

                if (diffWidth > diffHeight) {
                    width = mMaxWidth;
                    height = (int)(width * ratio);
                } else {
                    height = mMaxHeight;
                    width = (int)(height / ratio);
                }
                // TODO: Center the view (ScrollTo?)
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
        if (mAnimationEndListener != null) {
            mAnimationEndListener.onAnimationEnd();
        }
    }
}
