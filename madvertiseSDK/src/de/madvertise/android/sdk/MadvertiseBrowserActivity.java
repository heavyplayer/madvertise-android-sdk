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

// Notice: inspired by The ORMMA.org project reference android implementation

package de.madvertise.android.sdk;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;


public class MadvertiseBrowserActivity extends Activity {

    /** Extra Constants **/
    public static final String SHOW_BACK_EXTRA = "open_show_back";
    public static final String SHOW_FORWARD_EXTRA = "open_show_forward";
    public static final String SHOW_REFRESH_EXTRA = "open_show_refresh";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Build the layout
        RelativeLayout rl = new RelativeLayout(this);
        final WebView webview = new WebView(this);

        this.getWindow().requestFeature(Window.FEATURE_PROGRESS);
        getWindow().setFeatureInt(Window.FEATURE_PROGRESS,
                Window.PROGRESS_VISIBILITY_ON);

        // Build the button bar
        LinearLayout bll = new LinearLayout(this);
        bll.setOrientation(LinearLayout.HORIZONTAL);
        bll.setId(42);
        bll.setWeightSum(100);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.FILL_PARENT,
                RelativeLayout.LayoutParams.FILL_PARENT);
        lp.addRule(RelativeLayout.ABOVE, 42);
        rl.addView(webview, lp);

        lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.FILL_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        rl.addView(bll, lp);

        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.FILL_PARENT);
        lp2.weight = 25;
        lp2.gravity = Gravity.CENTER_VERTICAL;

        ImageButton backButton = new ImageButton(this);

        bll.addView(backButton, lp2);
        if (!getIntent().getBooleanExtra(SHOW_BACK_EXTRA, true))
            backButton.setVisibility(ViewGroup.GONE);
        
//        backButton.setImageBitmap(bitmapFromJar("bitmaps/leftarrow.png"));
        backButton.setImageResource(R.drawable.ic_menu_back);

        backButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                if (webview.canGoBack()) webview.goBack();
                else MadvertiseBrowserActivity.this.finish();
            }
        });

        final ImageButton forwardButton = new ImageButton(this);
        lp2 = new LinearLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.FILL_PARENT);
        lp2.weight = 25;
        lp2.gravity = Gravity.CENTER_VERTICAL;

        bll.addView(forwardButton, lp2);
        if (!getIntent().getBooleanExtra(SHOW_FORWARD_EXTRA, true))
            forwardButton.setVisibility(ViewGroup.GONE);
        forwardButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(android.view.View arg0) {
                webview.goForward();
            }
        });

        ImageButton refreshButton = new ImageButton(this);
//        refreshButton.setImageBitmap(bitmapFromJar("bitmaps/refresh.png"));
        refreshButton.setImageResource(R.drawable.ic_menu_refresh);
        lp2 = new LinearLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp2.weight = 25;
        lp2.gravity = Gravity.CENTER_VERTICAL;

        bll.addView(refreshButton, lp2);
        if (!getIntent().getBooleanExtra(SHOW_REFRESH_EXTRA, true))

            refreshButton.setVisibility(ViewGroup.GONE);
        refreshButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(android.view.View arg0) {
                webview.reload();
            }
        });

        ImageButton closeButton = new ImageButton(this);
//        closeButton.setImageBitmap(bitmapFromJar("bitmaps/close.png"));
        closeButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        lp2 = new LinearLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp2.weight = 25;
        lp2.gravity = Gravity.CENTER_VERTICAL;

        bll.addView(closeButton, lp2);
        closeButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(android.view.View arg0) {
                MadvertiseBrowserActivity.this.finish();
            }
        });

        // Show progress bar
        getWindow().requestFeature(Window.FEATURE_PROGRESS);

        // Enable cookies
        CookieSyncManager.createInstance(this);
        CookieSyncManager.getInstance().startSync();
        webview.getSettings().setJavaScriptEnabled(true);
        webview.loadUrl(getIntent().getDataString());
        Log.d("MadvertiseBrowser", "loading "+getIntent().getDataString());

        webview.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode,
                    String description, String failingUrl) {
                Activity a = (Activity) view.getContext();
                Toast.makeText(a, "Ormma Error:" + description,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                forwardButton.setImageResource(R.drawable.ic_menu_forward);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (view.canGoForward()) {
                    forwardButton.setEnabled(true);
                } else {
                    forwardButton.setEnabled(false);
                }

            }
        });
        setContentView(rl);

        webview.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                // show progress bar while loading, url when loaded
                Activity a = (Activity) view.getContext();
                a.setTitle("Loading...");
                a.setProgress(progress * 100);
                if (progress == 100)
                    a.setTitle(view.getUrl());
            }
        });

    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();
        CookieSyncManager.getInstance().stopSync();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
        CookieSyncManager.getInstance().startSync();
    }
}
