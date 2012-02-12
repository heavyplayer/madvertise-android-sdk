<<<<<<< HEAD
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import de.madvertise.android.sdk.MadvertiseView.AnimationEndListener;
import de.madvertise.android.sdk.MadvertiseView.MadvertiseViewCallbackListener;

public class MadvertiseMraidView extends WebView {

    protected static final Pattern sUrlSplitter = Pattern.compile("((?:http|file):\\/\\/.*(?:\\.|_)+.*\\/)(.*\\.js)");

    private static final String TAG = MadvertiseMraidView.class.getCanonicalName();

    private static final int CLOSE_BUTTON_SIZE = 50;

    protected static final int STATE_LOADING = 0;

    protected static final int STATE_HIDDEN = 1;

    protected static final int STATE_DEFAULT = 2;

    protected static final int STATE_EXPANDED = 3;

    private int mState;

    private int mIndex;

    private boolean mOnScreen;

    private int mPlacementType;

    private FrameLayout mExpandLayout;

    private ViewGroup mOriginalParent;

    private Handler mLoadingCompletedHandler;

    private ExpandProperties mExpandProperties;

    private MadvertiseViewCallbackListener mListener;

    private AnimationEndListener mAnimationEndListener;


    public MadvertiseMraidView(Context context, MadvertiseViewCallbackListener listener,
            AnimationEndListener animationEndListener, Handler loadingCompletedHandler) {
        this(context);
        this.mLoadingCompletedHandler = loadingCompletedHandler;
        this.mAnimationEndListener = animationEndListener;
        this.mListener = listener;
    }

    public MadvertiseMraidView(Context context) {
                
        super(context);
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        //TODO: enable
        // setBackgroundColor(Color.TRANSPARENT);
        getSettings().setJavaScriptEnabled(true);
        addJavascriptInterface(mBridge, "mraid_bridge");
        loadMraidJs();
        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        mExpandProperties = new ExpandProperties(metrics.widthPixels, metrics.heightPixels);
        injectJs("mraid.setExpandProperties(" + mExpandProperties.toJson() + ");");

        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                setState(STATE_DEFAULT);
                fireEvent("ready");
                if (mLoadingCompletedHandler != null)
                    mLoadingCompletedHandler.sendEmptyMessage(MadvertiseView.MAKE_VISIBLE);
            }
           
            @Override
            public void onLoadResource(WebView view, String url) {
                if (!url.endsWith("mraid.js")) {
                   
                    MadvertiseUtil.logMessage(TAG, Log.DEBUG, "loading "+url);
                    view.stopLoading();
                    super.onLoadResource(view, url);
                } else {
                    MadvertiseUtil.logMessage(TAG, Log.DEBUG, "NOT loading "+url);
                    // ToDo identifcation
                }
            }           
        });
        setWebChromeClient(new WebChromeClient()); // enable js console.log
    }

    protected void loadAd(MadvertiseAd ad) {
        loadAd(ad.getBannerUrl());
    }

    protected void loadAd(String url) {
        Matcher m = sUrlSplitter.matcher(url);
        if (m.matches()) {
            String baseUrl = m.group(1);
            String jsFile = m.group(2);
            MadvertiseUtil.logMessage(null, Log.INFO, "loading javascript Ad: "
                                  + "baseUrl=" + baseUrl + " jsFile=" + jsFile);
            loadDataWithBaseURL(baseUrl, "<html><head><script type=\"text/javascript\" src=\""
                    + jsFile + "\"/></head><body></body></html>", "text/html", "utf8", null);
            
        } else {
            MadvertiseUtil.logMessage(null, Log.INFO, "loading html Ad: "+url);
            loadUrl(url);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        mOnScreen = true;
        setViewability();
    }
    
    @Override
    protected void onDetachedFromWindow() {
        mOnScreen = false;
        setViewability();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        setViewability();
    }

    private void setViewability() {
        if (mOnScreen && getVisibility() == View.VISIBLE) {
            injectJs("mraid.setViewable(true);");
        } else {
            injectJs("mraid.setViewable(false);");
        }
    }

    // to be called from the Ad (js side)
    Object mBridge = new Object() {

        public void expand() {
            post(new Runnable() {
                @Override
                public void run() {
                    MadvertiseMraidView.this.resize(
                            mExpandProperties.width,
                            mExpandProperties.height);
                }
            });
            setState(STATE_EXPANDED);
        }

        @SuppressWarnings("unused") // because it IS used from the js side
        public void expand(String url) {
            Log.d("TEST", "expand with url");
            loadUrl(url);
            expand();
        }

        @SuppressWarnings("unused") // because it IS used from the js side
        public void close() {
            post(new Runnable() {
                @Override
                public void run() {
                    MadvertiseMraidView.this.close();
                }
            });
        }

        @SuppressWarnings("unused") // because it IS used from the js side
        public void open(String url) {
            if(mListener != null) {
                mListener.onAdClicked();
            }
            getContext().startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(url), getContext(), MadvertiseBrowserActivity.class));
        }

        @SuppressWarnings("unused") // because it IS used from the js side
        public void setExpandProperties(String json) {
            mExpandProperties.readJson(json);
        }
    };


    // to be called from the App (java side)

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

    protected void setState(int state) {
        mState = state;
        injectJs("mraid.setState('" + state + "');");
    }

    void fireEvent(String event) {
        injectJs("mraid.fireEvent('" + event + "');");
    }
    
    protected void fireErrorEvent(String message, String action) {
        injectJs("mraid.fireErrorEvent('" + message + "', '"+action+"');");
    }

    protected ExpandProperties getExpandProperties() {
        return mExpandProperties;
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
        closeButton.setId(43);
        final FrameLayout.LayoutParams closeButtonParams = new FrameLayout.LayoutParams(
                CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE);
        closeButtonParams.gravity = Gravity.RIGHT;
        closeButton.setLayoutParams(closeButtonParams);
        closeButton.setBackgroundColor(Color.TRANSPARENT);
        closeButton.setOnClickListener(new OnClickListener() {            
            @Override
            public void onClick(View v) {
                close();
            }
        });

        if (!mExpandProperties.useCustomClose) {
            closeButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        }
        
        mExpandLayout.addView(closeButton);

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
                setState(STATE_DEFAULT);
                break;
            case STATE_DEFAULT:
                // Set MadvertiseView to GONE. Note: This will cause this view
                // to be GONE too.
                ((ViewGroup)getParent()).setVisibility(View.GONE);
                setState(STATE_HIDDEN);
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
    private void loadMraidJs() {
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
=======
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Picture;
import android.net.Uri;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.VideoView;
import de.madvertise.android.sdk.MadvertiseView.AnimationEndListener;
import de.madvertise.android.sdk.MadvertiseView.MadvertiseViewCallbackListener;

public class MadvertiseMraidView extends WebView {

    protected static final Pattern sUrlSplitter = Pattern.compile("((?:http|file):\\/\\/.*(?:\\.|_)+.*\\/)(.*\\.js)");

    private static final String TAG = MadvertiseMraidView.class.getCanonicalName();

    private static final int CLOSE_BUTTON_SIZE = 50;

    protected static final int STATE_LOADING = 0;

    protected static final int STATE_HIDDEN = 1;

    protected static final int STATE_DEFAULT = 2;

    protected static final int STATE_EXPANDED = 3;

    private int mState;

    private int mIndex;

    private boolean mOnScreen;

    private int mPlacementType;

    private FrameLayout mExpandLayout;

    private ViewGroup mOriginalParent;

    private Handler mLoadingCompletedHandler;

    private ExpandProperties mExpandProperties;

    private MadvertiseViewCallbackListener mListener;

    private AnimationEndListener mAnimationEndListener;

    private String mJsFile;


    public MadvertiseMraidView(Context context, MadvertiseViewCallbackListener listener,
            AnimationEndListener animationEndListener, Handler loadingCompletedHandler) {
        this(context);
        this.mLoadingCompletedHandler = loadingCompletedHandler;
        this.mAnimationEndListener = animationEndListener;
        this.mListener = listener;
    }

    public MadvertiseMraidView(Context context) {
        super(context);
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        //TODO: enable
        // setBackgroundColor(Color.TRANSPARENT);
        getSettings().setJavaScriptEnabled(true);
        addJavascriptInterface(mBridge, "mraid_bridge");
        
        loadMraidJs();
        
        getSettings().setPluginsEnabled(true);
        setWebChromeClient(new WebChromeClient() {

            @Override
            public Bitmap getDefaultVideoPoster() {
                Log.d("Video", "getDefVideoPoster");
                return super.getDefaultVideoPoster();
            }

            @Override
            public View getVideoLoadingProgressView() {
                Log.d("Video", "getVideoProg");
                return super.getVideoLoadingProgressView();
            }

            @Override
            public void onHideCustomView() {
                Log.d("Video", "onHide");
                super.onHideCustomView();
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                Log.d("Video", "onShow");
                super.onShowCustomView(view, callback);
                if (view instanceof FrameLayout){
                    Log.d("Video", "is Framelayout");
                    FrameLayout frame = (FrameLayout) view;
                    if (frame.getFocusedChild() instanceof VideoView){
                        VideoView video = (VideoView) frame.getFocusedChild();
                        frame.removeView(video);
                        ((Activity)getContext()).setContentView(video);
//                        video.setOnCompletionListener(this);
//                        video.setOnErrorListener(this);
                        video.start();
                    }
                }
            }
        });
        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                MadvertiseUtil.logMessage(TAG, Log.DEBUG, "finsished loading "+url);
                final DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
                mExpandProperties = new ExpandProperties(metrics.widthPixels, metrics.heightPixels);
                injectJs("mraid.setExpandProperties(" + mExpandProperties.toJson() + ");");
                injectJs("monkeyPatch();");
                if (mJsFile != null) {
                    injectJs("document.write(\"<body></body><script type=\\\"text/javascript\\\" src=\\\""+ mJsFile + "\\\"/>\");");
                }
                setViewability();
                fireEvent("ready");
                setState(STATE_DEFAULT);
                if (mLoadingCompletedHandler != null)
                    mLoadingCompletedHandler.sendEmptyMessage(MadvertiseView.MAKE_VISIBLE);
            }
        });
        setPictureListener(new PictureListener() {
            
            @Override // scroll to center
            public void onNewPicture(WebView wv, Picture pic) {
                int y = (pic.getHeight() - wv.getHeight()) / 2;
                int x = (pic.getWidth() - wv.getWidth()) / 2;
//                Log.d("Center", "scrollTo x="+x+", y="+y);
                wv.scrollTo(x, y);
            }
        });
    }

    protected void loadAd(MadvertiseAd ad) {
        loadAd(ad.getBannerUrl());
    }

    protected void loadAd(String url) {
        Matcher m = sUrlSplitter.matcher(url);
        if (m.matches()) {
            mJsFile = m.group(2);
            final String baseUrl = m.group(1);
            MadvertiseUtil.logMessage(TAG, Log.INFO, "loading javascript Ad: "
                                  + "baseUrl=" + baseUrl + " jsFile=" + mJsFile);
            loadDataWithBaseURL(baseUrl, "<html><head></head><body>MRAID Ad</body></html>", "text/html", "utf8", null);
        } else {
            MadvertiseUtil.logMessage(TAG, Log.INFO, "loading html Ad: "+url);
            loadUrl(url);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        mOnScreen = true;
        setViewability();
    }
    
    @Override
    protected void onDetachedFromWindow() {
        mOnScreen = false;
        setViewability();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        setViewability();
    }

    private void setViewability() {
        if (mOnScreen && getVisibility() == View.VISIBLE) {
            injectJs("mraid.setViewable(true);");
        } else {
            injectJs("mraid.setViewable(false);");
        }
    }


    // to be called from the Ad (js side)

    Object mBridge = new Object() {

        public void expand() {
            post(new Runnable() {
                @Override
                public void run() {
                    MadvertiseMraidView.this.resize(
                            mExpandProperties.width,
                            mExpandProperties.height);
                }
            });
            setState(STATE_EXPANDED);
        }

        @SuppressWarnings("unused") // because it IS used from the js side
        public void expand(String url) {
            Log.d("TEST", "expand with url");
            loadUrl(url);
            expand();
        }

        @SuppressWarnings("unused") // because it IS used from the js side
        public void close() {
            post(new Runnable() {
                @Override
                public void run() {
                    MadvertiseMraidView.this.close();
                }
            });
        }

        @SuppressWarnings("unused") // because it IS used from the js side
        public void open(String url) {
            if(mListener != null) {
                mListener.onAdClicked();
            }
            getContext().startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(url), getContext(), MadvertiseBrowserActivity.class));
        }

        @SuppressWarnings("unused") // because it IS used from the js side
        public void setExpandProperties(String json) {
            mExpandProperties.readJson(json);
        }
    };


    // to be called from the App (java side)

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

    protected void setState(int state) {
        mState = state;
        injectJs("mraid.setState('" + state + "');");
    }

    void fireEvent(String event) {
        injectJs("mraid.fireEvent('" + event + "');");
    }
    
    protected void fireErrorEvent(String message, String action) {
        injectJs("mraid.fireErrorEvent('" + message + "', '"+action+"');");
    }

    protected ExpandProperties getExpandProperties() {
        return mExpandProperties;
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
        closeButton.setId(43);
        final FrameLayout.LayoutParams closeButtonParams = new FrameLayout.LayoutParams(
                CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE);
        closeButtonParams.gravity = Gravity.RIGHT;
        closeButton.setLayoutParams(closeButtonParams);
        closeButton.setBackgroundColor(Color.TRANSPARENT);
        closeButton.setOnClickListener(new OnClickListener() {            
            @Override
            public void onClick(View v) {
                close();
            }
        });

        if (!mExpandProperties.useCustomClose) {
            closeButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        }
        
        mExpandLayout.addView(closeButton);

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
                setState(STATE_DEFAULT);
                break;
            case STATE_DEFAULT:
                // Set MadvertiseView to GONE. Note: This will cause this view
                // to be GONE too.
                ((ViewGroup)getParent()).setVisibility(View.GONE);
                setState(STATE_HIDDEN);
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
    private void loadMraidJs() {
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
>>>>>>> 19b525778a7fcba84282e8831d2adc0faccf4458
