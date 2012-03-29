/*
 * Copyright 2012 madvertise Mobile Advertising GmbH
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

package de.madvertise.android.sdk;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
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

public class MadvertiseActivity extends Activity {

    public static final String SHOW_BACK_EXTRA = "open_show_back";
    public static final String SHOW_FORWARD_EXTRA = "open_show_forward";
    public static final String SHOW_REFRESH_EXTRA = "open_show_refresh";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final RelativeLayout relativeLayout = new RelativeLayout(this);
        final WebView webView = new WebView(this);

        this.getWindow().requestFeature(Window.FEATURE_PROGRESS);
        getWindow().setFeatureInt(Window.FEATURE_PROGRESS,
                Window.PROGRESS_VISIBILITY_ON);

        final LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setId(42);
        linearLayout.setWeightSum(100);
        RelativeLayout.LayoutParams rlParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.FILL_PARENT,
                RelativeLayout.LayoutParams.FILL_PARENT);
        rlParams.addRule(RelativeLayout.ABOVE, 42);
        relativeLayout.addView(webView, rlParams);

        rlParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.FILL_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        rlParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        relativeLayout.addView(linearLayout, rlParams);

        LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.FILL_PARENT);
        llParams.weight = 25;
        llParams.gravity = Gravity.CENTER_VERTICAL;

        final ImageButton back = new ImageButton(this);

        linearLayout.addView(back, llParams);
        if (!getIntent().getBooleanExtra(SHOW_BACK_EXTRA, true))
            back.setVisibility(ViewGroup.GONE);

        back.setImageResource(R.drawable.ic_menu_back);

        back.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                if (webView.canGoBack())
                    webView.goBack();
                else
                    MadvertiseActivity.this.finish();
            }
        });

        final ImageButton forward = new ImageButton(this);
        llParams = new LinearLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.FILL_PARENT);
        llParams.weight = 25;
        llParams.gravity = Gravity.CENTER_VERTICAL;

        linearLayout.addView(forward, llParams);
        if (!getIntent().getBooleanExtra(SHOW_FORWARD_EXTRA, true))
            forward.setVisibility(ViewGroup.GONE);
        forward.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(android.view.View arg0) {
                webView.goForward();
            }
        });

        final ImageButton refresh = new ImageButton(this);
        refresh.setImageResource(R.drawable.ic_menu_refresh);
        llParams = new LinearLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        llParams.weight = 25;
        llParams.gravity = Gravity.CENTER_VERTICAL;

        linearLayout.addView(refresh, llParams);
        if (!getIntent().getBooleanExtra(SHOW_REFRESH_EXTRA, true)) {
            refresh.setVisibility(ViewGroup.GONE);
        }
        refresh.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(android.view.View arg0) {
                webView.reload();
            }
        });

        final ImageButton close = new ImageButton(this);
        close.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        llParams = new LinearLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        llParams.weight = 25;
        llParams.gravity = Gravity.CENTER_VERTICAL;

        linearLayout.addView(close, llParams);
        close.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(android.view.View arg0) {
                MadvertiseActivity.this.finish();
            }
        });

        // Show progress bar
        getWindow().requestFeature(Window.FEATURE_PROGRESS);

        // Enable cookies
        CookieSyncManager.createInstance(this);
        CookieSyncManager.getInstance().startSync();
        webView.getSettings().setJavaScriptEnabled(true);
        
        final String dataString = getIntent().getDataString();
        // check for other formats like tel:, geo:, and so on
        if (!dataString.startsWith("http") || dataString.startsWith("https://market.")
                || dataString.startsWith("https://play.")) {
            try {
                final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(dataString));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } catch (ActivityNotFoundException e) { // in case there is no activity, we show the data in the browser
                webView.loadUrl(getIntent().getDataString());
            }
        } else {
            webView.loadUrl(getIntent().getDataString());
        }

        webView.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode,
                    String description, String failingUrl) {
                final Activity activity = (Activity) view.getContext();
                Toast.makeText(activity, "Error:" + description,
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
                forward.setImageResource(R.drawable.ic_menu_forward);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (view.canGoForward()) {
                    forward.setEnabled(true);
                } else {
                    forward.setEnabled(false);
                }

            }
        });
        setContentView(relativeLayout);

        webView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                // show progress bar while loading, url when loaded
                final Activity activity = (Activity) view.getContext();
                activity.setTitle("Loading...");
                activity.setProgress(progress * 100);
                if (progress == 100)
                    activity.setTitle(view.getUrl());
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieSyncManager.getInstance().stopSync();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CookieSyncManager.getInstance().startSync();
    }
}
