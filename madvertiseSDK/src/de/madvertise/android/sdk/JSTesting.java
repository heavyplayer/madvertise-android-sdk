package de.madvertise.android.sdk;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class JSTesting extends WebView {
	
	private MadvertiseAd mImageAd;
	
	public JSTesting(final Context context, final MadvertiseAd ad, final Handler loadingCompletedHandler) {
        super(context);
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setPluginsEnabled(true);
        addJavascriptInterface(new JSInterface(), "mraid_bridge");
        MadvertiseUtil.logMessage(null, Log.INFO, "mraid_bridge added");
        mImageAd = ad;
        

        
        WebViewClient webViewClient = new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
            	MadvertiseUtil.logMessage(null, Log.INFO, "onPageFinished called");
                if (loadingCompletedHandler != null) {
                    loadingCompletedHandler.sendEmptyMessage(MadvertiseView.MAKE_VISIBLE);
                }
                loadUrl("javascript:pingCallOutside();");
                loadUrl("javascript:mraid_bridge.ping('muchtel');");
            }
        };
        setWebViewClient(webViewClient);

        String pingerJS = MadvertiseUtil.convertStreamToString(getContext().getResources().openRawResource(de.madvertise.android.sdk.R.raw.pinger));

	    loadUrl(mImageAd.getBannerUrl());
	    MadvertiseUtil.logMessage(null, Log.INFO, "banner added : " + mImageAd.getBannerUrl());
	    
    	loadUrl("javascript:" + pingerJS);
    	MadvertiseUtil.logMessage(null, Log.INFO, "javascript added : " + pingerJS);
    	
    }
}