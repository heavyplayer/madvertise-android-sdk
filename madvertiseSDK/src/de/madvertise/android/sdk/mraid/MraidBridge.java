
package de.madvertise.android.sdk.mraid;

import org.json.JSONException;
import org.json.JSONObject;

import de.madvertise.android.sdk.MadvertiseUtil;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

/**
 * Used for the communication with the JavaScript used in the
 * {@link MadvertiseMraidView}.
 */
public class MraidBridge implements IMraidBridge {

    private MadvertiseMraidView mMraidView;

    private static final String JAVASCRIPT_INDICATOR = "javascript:";

    private static final String STATE_DEFAULT = "default";

    private static final String STATE_EXPANDED = "expanded";

    private static final String STATE_LOADING = "loading";

    private static final String STATE_HIDDEN = "hidden";

    private static final String EVENT_ERROR = "error";

    private static final String EVENT_READY = "ready";

    private static final String EVENT_STATE_CHANGE = "stateChange";

    private static final String EVENT_VIEWABLE_CHANGE = "viewableChange";

    private boolean mUseCustomClose = false;

    private String mCurrentState = STATE_DEFAULT;

    private ExpandProperties mExpandProperties;

    private Context mContext;

    public MraidBridge(MadvertiseMraidView mraidView) {
        mMraidView = mraidView;
        mContext = mraidView.getContext();

        final DisplayMetrics displayMetrics = mContext.getApplicationContext().getResources()
                .getDisplayMetrics();
        mExpandProperties = new ExpandProperties(displayMetrics.widthPixels,
                displayMetrics.heightPixels);

        mCurrentState = STATE_LOADING;
        setState(mCurrentState);
        triggerEvent(EVENT_STATE_CHANGE);
    }

    @Override
    public void close() {
        if (mCurrentState.equals(STATE_DEFAULT)) {
            hide();
            mCurrentState = STATE_HIDDEN;
            setState(mCurrentState);
        } else if (mCurrentState.equals(STATE_EXPANDED)) {
            downsize();
            mCurrentState = STATE_DEFAULT;
            setState(mCurrentState);
        }
        triggerEvent(EVENT_STATE_CHANGE);
    }

    private void hide() {
        // TODO: Set MadvertiseView to gone
        mMraidView.setVisibility(View.GONE);
    }

    private void downsize() {
        // TODO: Downsize
    }

    @Override
    public void expand(final String url) {
        // final Intent intent = new Intent(mContext,
        // MraidFullScreenView.class);
        // intent.putExtra("width", mExpandProperties.width);
        // intent.putExtra("height", mExpandProperties.height);
        // intent.putExtra("url", url);
        // intent.putExtra("useCustomClose", mUseCustomClose);
        // mContext.startActivity(intent);
        mCurrentState = STATE_EXPANDED;
        setState(mCurrentState);
        triggerEvent(EVENT_STATE_CHANGE);
    }

    @Override
    public void setExpandProperties(final String json) {
        mExpandProperties.fromJson(json);
        mUseCustomClose = mExpandProperties.useCustomClose;
    }

    @Override
    public void open(final String url) {
        // TODO: Browser Activity
    }

    @Override
    public void useCustomClose(final boolean useCustomClose) {
        mUseCustomClose = useCustomClose;
    }

    public void setState(final String state) {
        final StringBuilder sb = new StringBuilder();
        sb.append("setState(").append(state).append(");");
        inject(sb.toString());
    }

    public void setExpandProperties(final ExpandProperties properties) {
        final StringBuilder sb = new StringBuilder();
        sb.append("setCustomClose(").append(properties.toString()).append(");");
        inject(sb.toString());
    }

    public void setPlacementType(final String type) {
        if (type == null || !type.equals(MadvertiseUtil.PLACEMENT_TYPE_INLINE)
                || !type.equals(MadvertiseUtil.PLACEMENT_TYPE_INTERSTITIAL)) {
            MadvertiseUtil
                    .logMessage(
                            null,
                            Log.WARN,
                            "Invalid placement type. You must use one of MadvertiseUtil.PLACEMENT_TYPE_INLINE and MadvertiseUtil.PLACEMENT_TYPE_INTERSTITIAL.");
        } else {
            final StringBuilder sb = new StringBuilder();
            sb.append("setPlacementType(").append(type).append(");");
            inject(sb.toString());
        }
    }

    public void setIsViewable(final boolean isViewable) {
        final StringBuilder sb = new StringBuilder();
        sb.append("setIsViewable(").append(isViewable).append(");");
        inject(sb.toString());

        triggerEvent(EVENT_VIEWABLE_CHANGE);
    }

    public void triggerEvent(final String type) {
        triggerEvent(type, null, null);
    }

    public void triggerEvent(final String type, final String message, final String action) {
        final StringBuilder sb = new StringBuilder();
        sb.append("sendEvent(").append(type);
        if (message != null && action != null) {
            sb.append(", ").append(message).append(", ").append(action);
        }
        sb.append(");");
        inject(sb.toString());
    }

    public void onLoadingComplete() {
        triggerEvent(EVENT_READY);
        // per specification, the ready-event has to be fired before the state
        // is set to default.
        mCurrentState = STATE_DEFAULT;
        setState(mCurrentState);
    }

    // getVersion is by convention always '1.0' and hence doesn't need a
    // callback method in java
    // removeEventListener() and addEventListener() is JavaScript-only
    // functionality.

    private void inject(final String javaScript) {
        if (javaScript != null) {
            mMraidView.loadUrl(JAVASCRIPT_INDICATOR + javaScript);
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

        @Override
        public String toString() {
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
