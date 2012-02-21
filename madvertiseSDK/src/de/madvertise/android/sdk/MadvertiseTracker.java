
package de.madvertise.android.sdk;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
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
            new Thread(new Runnable() {
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

                    // get uid (does not work in emulator)
                    String uid = MadvertiseUtil.getHashedAndroidID(mContext);

                    MadvertiseUtil.logMessage(null, Log.DEBUG, "uid = " + uid);

                    // create post request
                    HttpPost postRequest = new HttpPost(MadvertiseUtil.MAD_SERVER + "/action/"
                            + siteToken);
                    postRequest.setHeader("Content-Type",
                            "application/x-www-form-urlencoded; charset=utf-8");

                    List<NameValuePair> parameterList = new ArrayList<NameValuePair>();
                    parameterList.add(new BasicNameValuePair("ua", MadvertiseUtil.getUA()));
                    parameterList.add(new BasicNameValuePair("app", "true"));
                    parameterList.add(new BasicNameValuePair("debug", Boolean
                            .toString(mIsDebugMode)));
                    parameterList.add(new BasicNameValuePair("ip", MadvertiseUtil
                            .getLocalIpAddress(null)));
                    parameterList.add(new BasicNameValuePair("uid", uid));
                    final int labelId = mContext.getApplicationInfo().labelRes;
                    if (labelId != 0) {
                        parameterList.add(new BasicNameValuePair("app_name", mContext.getString(labelId)));
                    }
                    try {
                        parameterList.add(new BasicNameValuePair("app_version",
                                mContext.getPackageManager().getPackageInfo(
                                        mContext.getPackageName(), 0).versionName));
                    } catch (NameNotFoundException e) {
                        e.printStackTrace();
                    }
                    parameterList.add(new BasicNameValuePair("ts", Long.toString(System
                            .currentTimeMillis())));
                    parameterList.add(new BasicNameValuePair("at", actionType));
                    parameterList.add(new BasicNameValuePair("first_launch", Boolean
                            .toString(isFirstLaunch())));

                    UrlEncodedFormEntity urlEncodedEntity = null;
                    try {
                        urlEncodedEntity = new UrlEncodedFormEntity(parameterList);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    postRequest.setEntity(urlEncodedEntity);

                    MadvertiseUtil.logMessage(null, Log.DEBUG, "Post request created");
                    MadvertiseUtil.logMessage(null, Log.DEBUG, "Uri : "
                            + postRequest.getURI().toASCIIString());
                    MadvertiseUtil.logMessage(
                            null,
                            Log.DEBUG,
                            "All headers : "
                                    + MadvertiseUtil.getAllHeadersAsString(postRequest
                                            .getAllHeaders()));
                    MadvertiseUtil.logMessage(null, Log.DEBUG, "All request parameters :"
                            + MadvertiseUtil.printRequestParameters(parameterList));

                    synchronized (this) {
                        // send blocking request to ad server
                        HttpClient httpClient = new DefaultHttpClient();
                        HttpResponse httpResponse = null;

                        try {
                            HttpParams clientParams = httpClient.getParams();
                            HttpConnectionParams.setConnectionTimeout(clientParams,
                                    MadvertiseUtil.CONNECTION_TIMEOUT);
                            HttpConnectionParams.setSoTimeout(clientParams,
                                    MadvertiseUtil.CONNECTION_TIMEOUT);

                            MadvertiseUtil.logMessage(null, Log.DEBUG, "Sending request");
                            httpResponse = httpClient.execute(postRequest);

                            MadvertiseUtil.logMessage(null, Log.DEBUG, "Response Code => "
                                    + httpResponse.getStatusLine().getStatusCode());

                            int responseCode = httpResponse.getStatusLine().getStatusCode();

                            HttpEntity entity = httpResponse.getEntity();

                            if (responseCode == 200 && entity != null) {
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
            }, "MadvertiseTrackingThread").start();
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
