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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.client.ClientProtocolException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

/**
 * Lets you track the access on your app to Madvertise. Use
 * <code>reportAction</code> to report an action. Use <code>getInstance()</code>
 * to retreive an instance of this class.
 */
public class MadvertiseTracker {

    private Context mContext;

    private SharedPreferences mPreferences;

    private boolean mIsDebugMode = false;

    private static final String IS_FIRST_LAUNCH = "hasBeenLaunched";

    /**
     * Action type for the {@link MadvertiseTracker}. Provide this type to the
     * method <code>reportAction</code> when your app is being launched.
     */
    private static final String ACTION_TYPE_LAUNCH = "launch";

    /**
     * Action type for the {@link MadvertiseTracker}. Provide this type to the
     * method <code>reportAction</code> when your app is being stopped.
     */
    private static final String ACTION_TYPE_STOP = "stop";

    /**
     * Action type for the {@link MadvertiseTracker}. Provide this type to the
     * method <code>reportAction</code> when your app is becoming active.
     */
    private static final String ACTION_TYPE_ACTIVE = "active";

    /**
     * Action type for the {@link MadvertiseTracker}. Provide this type to the
     * method <code>reportAction</code> when your app is becoming inactive.
     */
    private static final String ACTION_TYPE_INACTIVE = "inactive";

    private static MadvertiseTracker mInstance;

    /**
     * Retreive an instance of this class.
     * 
     * @return the instance
     */
    public static MadvertiseTracker getInstance(final Context context) {
        if (mInstance == null) {
            mInstance = new MadvertiseTracker();
        }
        mInstance.startNewSession(context);
        return mInstance;
    }

    /**
     * Start a new session for this Instance. This method has to be called only
     * once in an app's lifetime and is invoced by <code>getInstance()</code>
     * automatically.
     * 
     * @param context
     */
    public void startNewSession(final Context context) {
        mContext = context.getApplicationContext();
        mPreferences = mContext.getSharedPreferences("de.madvertise.android.sdk",
                Context.MODE_PRIVATE);
    }

    private MadvertiseTracker() {
    }

    /**
     * Reports an an app launch to madvertise.
     */
    public void reportLaunch() {
        reportAction(ACTION_TYPE_LAUNCH);
    }

    /**
     * Reports an an app stop to madvertise.
     */
    public void reportStop() {
        reportAction(ACTION_TYPE_STOP);
    }

    /**
     * Reports an an app actiation to madvertise. Should be called when an app
     * becomes active.
     */
    public void reportActive() {
        reportAction(ACTION_TYPE_ACTIVE);
    }

    /**
     * Reports an an app inactivation to madvertise. Should be called when an
     * app becomes inactive.
     */
    public void reportInactive() {
        reportAction(ACTION_TYPE_INACTIVE);
    }

    private boolean isSessionEnabled() {
        if (mContext == null) {
            MadvertiseUtil.logMessage(null, Log.DEBUG,
                    "Tracker is not enabled. Please call startNewSession() first");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Reports an action to madvertise. Provide
     * <code>ACTION_TYPE_LAUNCH</action>,
     * <code>ACTION_TYPE_STOP</action>,
     * <code>ACTION_TYPE_ACTIVE</action> or
     * <code>ACTION_TYPE_INACTIVE</action>.
     * 
     * @param actionType
     */
    private void reportAction(final String actionType) {
        if (
        		(
        			actionType.equals(ACTION_TYPE_ACTIVE) ||
        			actionType.equals(ACTION_TYPE_INACTIVE) ||
        			actionType.equals(ACTION_TYPE_LAUNCH) ||
        			actionType.equals(ACTION_TYPE_STOP)
        		)
        		&&
        		isSessionEnabled()
           ) {
            Thread trackerThread = new Thread(new Runnable() {
                public void run() {
                    MadvertiseUtil.logMessage(null, Log.DEBUG, "Reporting action " + actionType);

                    // read all parameters, that we need for the request
                    // get site token from manifest xml file
                    String siteToken = MadvertiseUtil.getToken(mContext, null);
                    if (siteToken == null) {
                        siteToken = "";
                        MadvertiseUtil.logMessage(null, Log.DEBUG,
                                "Cannot show ads, since the appID ist null");
                    } else {
                        MadvertiseUtil.logMessage(null, Log.DEBUG, "appID = " + siteToken);
                    }

                    HttpURLConnection urlConnection = null;
                    synchronized (this) {
            			try {
            				// create post request
            				URL url = new URL(MadvertiseUtil.MAD_SERVER + "/action/" + siteToken);
            				
            				urlConnection = (HttpURLConnection)url.openConnection();
            				urlConnection.setDoOutput(true);

            				urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
                    
            				StringBuilder entities = new StringBuilder();
            				MadvertiseUtil.addEntity(entities, "ua", MadvertiseUtil.getUA());
            				MadvertiseUtil.addEntity(entities, "app", "true");
            				MadvertiseUtil.addEntity(entities, "debug", Boolean.toString(mIsDebugMode));
            				MadvertiseUtil.addEntity(entities, "ip", MadvertiseUtil.getLocalIpAddress(null));
            				
            				MadvertiseUtil.addEntity(entities, "ts", Long.toString(System.currentTimeMillis()));
            				MadvertiseUtil.addEntity(entities, "at", actionType);
            				MadvertiseUtil.addEntity(entities, "first_launch", Boolean.toString(isFirstLaunch()));
            				
            				MadvertiseUtil.addEntity(entities, "app_name", MadvertiseUtil.getApplicationName(mContext.getApplicationContext()));
            				MadvertiseUtil.addEntity(entities, "app_version", MadvertiseUtil.getApplicationVersion(mContext.getApplicationContext()));

            				MadvertiseUtil.addEntity(entities, "udid_md5", MadvertiseUtil.getHashedAndroidID(mContext, MadvertiseUtil.HashType.MD5));
            				MadvertiseUtil.addEntity(entities, "udid_sha1", MadvertiseUtil.getHashedAndroidID(mContext, MadvertiseUtil.HashType.SHA1));

            				MadvertiseUtil.addEntity(entities, "mac_md5", MadvertiseUtil.getHashedMacAddress(mContext, MadvertiseUtil.HashType.MD5));
            				MadvertiseUtil.addEntity(entities, "mac_sha1", MadvertiseUtil.getHashedMacAddress(mContext, MadvertiseUtil.HashType.SHA1));

            				urlConnection.setConnectTimeout(MadvertiseUtil.CONNECTION_TIMEOUT);
            				urlConnection.setReadTimeout(MadvertiseUtil.CONNECTION_TIMEOUT);

            				MadvertiseUtil.logMessage(null, Log.DEBUG, "Post request created");
            				MadvertiseUtil.logMessage(null, Log.DEBUG, "Uri : "+ url.toString());
            				MadvertiseUtil.logMessage(
            						null,
            						Log.DEBUG,
            						"All headers : "+MadvertiseUtil.getAllHeadersAsString(urlConnection.getRequestProperties()));
            				MadvertiseUtil.logMessage(null, Log.DEBUG, "All request parameters : "+entities.toString());

            				// send blocking request to ad server
            				urlConnection.setConnectTimeout(MadvertiseUtil.CONNECTION_TIMEOUT);
            				urlConnection.setReadTimeout(MadvertiseUtil.CONNECTION_TIMEOUT);

            				MadvertiseUtil.logMessage(null, Log.DEBUG, "Sending request");
            				OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream(), MadvertiseUtil.ENCODING);
            				out.write(entities.toString());
            				out.flush();

            				int responseCode = urlConnection.getResponseCode();

                            MadvertiseUtil.logMessage(null, Log.DEBUG, "Response Code => "
                                    + responseCode);

            				if (responseCode == 200) {
            					if (isFirstLaunch()) {
                                    onFirstLaunch();
                                }
            				}
                        } catch (ClientProtocolException e) {
                            MadvertiseUtil.logMessage(null, Log.DEBUG,
                                    "Error in HTTP request / protocol");
                            e.printStackTrace();
                        } catch (IOException e) {
                            MadvertiseUtil.logMessage(null, Log.DEBUG,
                                    "Could not receive a http response on an report-request");
                            e.printStackTrace();
                        } catch (Exception e) {
                            MadvertiseUtil.logMessage(null, Log.DEBUG,
                                    "Could not receive a http response on an report-request");
                            e.printStackTrace();
                        }
                    }
                }
            }, "MadvertiseTrackingThread");
            trackerThread.setPriority(Thread.MIN_PRIORITY);
            trackerThread.start();
        }
    }

    private void onFirstLaunch() {
        final Editor editor = mPreferences.edit();
        editor.putBoolean(IS_FIRST_LAUNCH, false);
        editor.commit();
    }

    private boolean isFirstLaunch() {
        return mPreferences.getBoolean(IS_FIRST_LAUNCH, true);
    }

    /**
     * Sets whether this instance is in debug mode or not
     * 
     * @param isDebugMode
     */
    public void setDebugMode(boolean isDebugMode) {
        mIsDebugMode = isDebugMode;
    }
}
