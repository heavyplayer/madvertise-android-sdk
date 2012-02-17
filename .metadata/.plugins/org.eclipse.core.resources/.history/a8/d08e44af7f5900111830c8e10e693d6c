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

package de.madvertise.android.sdk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import de.madvertise.android.sdk.MadvertiseView.MadvertiseViewCallbackListener;

/**
 * Defines an ad from a JSON object, that contains all necessary information
 * which is provided by the madvertise ad server. Icons and banners are
 * synchronously fetched from the madvertise server and stored on the device.
 * Click action is handled asynchronously.
 */
public class MadvertiseAd {

    private final String CLICK_URL_CODE = "click_url";

    private final String BANNER_URL_CODE = "banner_url";

    private final String TEXT_CODE = "text";

    private final String HAS_BANNER_CODE = "has_banner";

    private String mClickUrl;

    private String mBannerUrl;

    private String mText;

    private String mBannerType;

    private boolean mDownloadBanner;

    private boolean mHasBanner;

    private JSONArray mJsonNames;

    private JSONArray mJsonValues;

    private byte[] mImageByteArray;

    private Context mContext;

    private MadvertiseViewCallbackListener mCallbackListener;

    /**
     * Constructor, blocking due to http request, should be called in a thread
     * pool, a request queue, a network thread
     * 
     * @param context the applications context
     * @param json json object containing all ad information
     */
    protected MadvertiseAd(final Context context, final JSONObject json,
            final MadvertiseViewCallbackListener listener, final String bannerType) {
        this.mContext = context;
        this.mCallbackListener = listener;
        this.mBannerType = bannerType;

        MadvertiseUtil.logMessage(null, Log.DEBUG, "Creating ad");

        // init json arrays and print all keys / values
        mJsonNames = json.names();
        try {
            mJsonValues = json.toJSONArray(mJsonNames);

            if (MadvertiseUtil.PRINT_LOG) {
                for (int i = 0; i < mJsonNames.length(); i++) {
                    MadvertiseUtil.logMessage(null, Log.DEBUG, "Key => " + mJsonNames.getString(i)
                            + " Value => " + mJsonValues.getString(i));
                }
            }
            mClickUrl = json.isNull(CLICK_URL_CODE) ? "" : json.getString(CLICK_URL_CODE);
            
            mBannerUrl = "file:///android_asset/MRAID_andlabs_html_ad/banner.html";
//            mBannerUrl = json.isNull(BANNER_URL_CODE) ? "" : json.getString(BANNER_URL_CODE);
            
            mText = json.isNull(TEXT_CODE) ? "" : json.getString(TEXT_CODE);
            mDownloadBanner = Boolean.parseBoolean(json.isNull(HAS_BANNER_CODE) ? "true" : json
                    .getString(HAS_BANNER_CODE));

            if (mDownloadBanner) {
                mHasBanner = mBannerUrl != null && !mBannerUrl.equals("");
            } else {
                mHasBanner = false;
                MadvertiseUtil.logMessage(null, Log.DEBUG, "No banner link in json found");
            }
        } catch (JSONException e) {
            MadvertiseUtil.logMessage(null, Log.DEBUG, "Error in json string");
            if (mCallbackListener != null) {
                mCallbackListener.onError(e);
            }
            e.printStackTrace();
        }
    }

    /**
     * Handles the click action (opens the click url)
     */
    protected void handleClick() {
        if (mClickUrl != null && mClickUrl.equals("")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mClickUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            try {
                mContext.startActivity(intent);
                if (mCallbackListener != null) {
                    mCallbackListener.onAdClicked();
                }
            } catch (Exception e) {
                MadvertiseUtil.logMessage(null, Log.DEBUG, "Failed to open URL : " + mClickUrl);
                if (mCallbackListener != null) {
                    mCallbackListener.onError(e);
                }
                e.printStackTrace();
            }
        }
    }

    protected String getClickURL() {
        return mClickUrl;
    }

    public String getBannerUrl() {
        return mBannerUrl;
    }

    protected String getText() {
        return mText;
    }

    protected boolean hasBanner() {
        return mHasBanner;
    }

    protected byte[] getImageByteArray() {
        return mImageByteArray;
    }

    protected String getBannerType() {
        return mBannerType;
    }
}