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

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import de.madvertise.android.sdk.MadvertiseUtil;
import de.madvertise.android.sdk.MadvertiseView.MadvertiseViewCallbackListener;

public class MadvertiseMraidView extends WebView implements IMraidBridge {

    private static final String JAVASCRIPT_INDICATOR = "javascript:";

    private static final int CLOSE_BUTTON_SIZE = 50;

    private static final String STATE_DEFAULT = "default";

    private static final String STATE_EXPANDED = "expanded";

    private static final String STATE_LOADING = "loading";

    private static final String STATE_HIDDEN = "hidden";

    private static final String EVENT_ERROR = "error";

    private static final String EVENT_READY = "ready";

    private static final String EVENT_STATE_CHANGE = "stateChange";

    private static final String EVENT_VIEWABLE_CHANGE = "viewableChange";

    private static final String MRAID_VERSION = "1.0";

    private static final String JS_INTERFACE_NAME = "mraid";

    private static final String PLACEMENT_TYPE_INLINE = "inline";

    private static final String PLACEMENT_TYPE_INTERSTITIAL = "interstitial";

    private String mCurrentState = STATE_DEFAULT;

    private boolean mEnlarged = false;

    private boolean mUseCustomClose = false;

    private ExpandProperties mExpandProperties;

    private FrameLayout mEnlargeLayout;
    
    private MadvertiseViewCallbackListener mCallbackListener;

    public MadvertiseMraidView(Context context, AttributeSet attrs, String url,
            MadvertiseViewCallbackListener listener) {
        super(context, attrs);
        
        this.mCallbackListener = listener;
        
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

        final IMraidBridge bridge = this;
        this.addJavascriptInterface(bridge, JS_INTERFACE_NAME);

        final WebViewClient webViewClient = new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                fireReadyEvent();
            }
        };
        setWebViewClient(webViewClient);

        final DisplayMetrics displayMetrics = getContext().getApplicationContext().getResources()
                .getDisplayMetrics();
        mExpandProperties = new ExpandProperties(displayMetrics.widthPixels,
                displayMetrics.heightPixels);
    }

    public void expand() {
        expand(300, 300);
        mCurrentState = STATE_EXPANDED;
        fireStateChangeEvent();
    }

    public void expand(final String url) {
        loadUrl(url);
        expand();
    }

    public void expand(final int width, final int height) {
        FrameLayout content = (FrameLayout)getRootView().findViewById(android.R.id.content);

        final FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(width, height);

        mEnlargeLayout = new FrameLayout(getContext());
        final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        mEnlargeLayout.setLayoutParams(layoutParams);

        this.setLayoutParams(adParams);
        ((ViewGroup)getParent()).removeView(this);
        mEnlargeLayout.addView(this);

        if (!mUseCustomClose) {
            final ImageView imageView = new ImageView(getContext());
            imageView.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);

            final FrameLayout.LayoutParams closeButtonParams = new FrameLayout.LayoutParams(
                    CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE);
            closeButtonParams.gravity = Gravity.RIGHT;
            imageView.setLayoutParams(closeButtonParams);
            mEnlargeLayout.addView(imageView);
        }

        content.addView(mEnlargeLayout);

        mEnlarged = true;
    }

    public void close() {
        if (mEnlargeLayout != null) {
            ((ViewGroup)mEnlargeLayout.getParent()).removeView(mEnlargeLayout);
        }
    }

    private void fireErrorEvent(final String message, final String action) {

    }

    private void fireReadyEvent() {

    }

    private void fireStateChangeEvent() {
        // for ()
        // String injection = function + "(" + mCurrentState + ")";
    }

    private void fireViewableChangeEvent() {

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        if (mEnlarged && getWidth() - event.getX() < CLOSE_BUTTON_SIZE
                && event.getY() < CLOSE_BUTTON_SIZE && event.getAction() == MotionEvent.ACTION_UP) {
            close();
        }

        return super.dispatchTouchEvent(event);

    }

    @Override
    public void addEventListener(final String event, final String listener) {
        // TODO Auto-generated method stub

    }

    //
    // @Override
    // public String getExpandProperties() {
    // return mExpandProperties.toJson();
    // }
    //
    // @Override
    // public String getPlacementType() {
    // // TODO Auto-generated method stub
    // return null;
    // }
    //
    // @Override
    // public String getState() {
    // return mCurrentState;
    // }

    // @Override
    // public String getVersion() {
    // return MRAID_VERSION;
    // }
    //
    // @Override
    // public boolean isViewable() {
    // // TODO Auto-generated method stub
    // return false;
    // }

    @Override
    public void open(final String url) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            getContext().startActivity(intent);
        } catch (Exception e) {
            MadvertiseUtil.logMessage(null, Log.DEBUG, "Failed to open URL : " + url);
            if (mCallbackListener != null) {
                mCallbackListener.onError(e);
                
            }
            e.printStackTrace();
        }
    }

    @Override
    public void removeEventListener(final String event, String listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setExpandProperties(final String json) {
        mExpandProperties.fromJson(json);
        mUseCustomClose = mExpandProperties.useCustomClose;
    }

    @Override
    public void useCustomClose(final boolean useCustomClose) {
        mUseCustomClose = useCustomClose;
    }

    private void inject(final String javaScript) {
        if (javaScript != null) {
            loadUrl(JAVASCRIPT_INDICATOR + javaScript);
        }
    }

    private class ExpandProperties {
        private static final String WIDTH = "width";

        private static final String HEIGHT = "height";

        private static final String USE_CUSTOM_CLOSE = "useCustomClose";

        private static final String IS_MODAL = "isModal";

        int width;

        int height;

        boolean useCustomClose = false;

        boolean isModal = true;

        ExpandProperties(final int width, final int height) {
            this.width = width;
            this.height = height;
        }

        String toJson() {
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

        void fromJson(final String json) {
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
}
