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

import org.json.JSONException;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import de.madvertise.android.sdk.MadvertiseView.AnimationEndListener;

/**
 * Displays a banner image in a {@link WebView}. The WebView handles scaling and
 * GIF animation for us.
 */
class MadvertiseImageView extends WebView {

    private MadvertiseAd mImageAd;

    private AnimationEndListener mAnimationListener;

    public MadvertiseImageView(final Context context, final int newWidth, final int newHeight,
            final MadvertiseAd ad, final Handler loadingCompletedHandler,
            final AnimationEndListener animationListener) {
        super(context);

        mAnimationListener = animationListener;

        mImageAd = ad;

        // Remove the ScrollBar so that we have no padding.
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);

        // No visible background
        setBackgroundColor(Color.TRANSPARENT);

        WebViewClient webViewClient = new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (loadingCompletedHandler != null) {
                    loadingCompletedHandler.sendEmptyMessage(MadvertiseView.MAKE_VISIBLE);
                }
            }
        };
        setWebViewClient(webViewClient);

        StringBuilder content = new StringBuilder();
        content.append(
                "<html><head><style>* {margin:0;padding:0;}</style></head><body>")
                .append("<img src=\"" + mImageAd.getBannerUrl() + "\" height=\"" + newHeight
                        + "\" width=\"" + newWidth + "\"/>" + getImpressionTrackingTag()).append("</body></html>");
        MadvertiseUtil.logMessage(null, Log.DEBUG, "Loading ad : " + content.toString());
        loadDataWithBaseURL(null, content.toString(), "text/html", "UTF-8", null);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mImageAd.handleClick();
            return true;
        }
        return super.dispatchTouchEvent(event);
    }

    /**
     * This is needed because of a sad Android-Bug: onAnimationEnd() will not be
     * called in the {@link Animation.AnimationListener}, so we have to listen
     * to this event in the views
     */
    @Override
    protected void onAnimationEnd() {
        super.onAnimationEnd();
        if (mAnimationListener != null) {
            mAnimationListener.onAnimationEnd();
        }
    }
    
    private String getImpressionTrackingTag() {
    	if (mImageAd == null || mImageAd.getImpressionTrackingArray() == null || mImageAd.getImpressionTrackingArray().length() == 0) {
    		return "";
    	}
    	
    	StringBuilder content = new StringBuilder("");
    	try {
	    	for (int i = 0; i < mImageAd.getImpressionTrackingArray().length(); i++) {
	    		content.append("<img src=\"" + mImageAd.getImpressionTrackingArray().get(i) + "\"/>");
	    	}
		} catch (JSONException e) {
			content = new StringBuilder("");
		}
    	
    	return content.toString();
    }
}
