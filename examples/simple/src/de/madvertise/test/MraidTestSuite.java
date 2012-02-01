
package de.madvertise.test;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Instrumentation.ActivityMonitor;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import de.madvertise.android.sdk.MadvertiseMraidView;
import de.madvertise.android.sdk.MadvertiseMraidView.ExpandProperties;
import de.madvertise.android.sdk.MadvertiseUtil;

public class MraidTestSuite extends ActivityInstrumentationTestCase2<Activity> {

    private Activity activity;

    private String callback_data;

    private MadvertiseMraidView mraidView;

    public MraidTestSuite() {
        super("de.madvertise.test", Activity.class);
    }

    @Override
    protected void setUp() {
        try {
            super.setUp();
            activity = getActivity();
            runTestOnUiThread(new Runnable() {
                public void run() {
                    mraidView = new MadvertiseMraidView(activity);
                    FrameLayout layout = new FrameLayout(activity);
                    mraidView.setLayoutParams(new FrameLayout.LayoutParams(320, 53));
                    layout.addView(mraidView);
                    activity.setContentView(layout);
                }
            });
            getInstrumentation().waitForIdleSync();
            mraidView.addJavascriptInterface(new Object() {
                @SuppressWarnings("unused")
                public void callback(String data) {
                    callback_data = data;
                    Log.d("Javascript", "called back: " + data);
                }
            }, "test");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void testAdControllerExists() {
        loadHtml("testing Ad Controller exists");
        executeAsyncJs("typeof mraid", new JsCallback() {
            void done(String type) {
                assertEquals("object", type);
            }
        });
    }

    // getVersion should return "1.0"
    public void testGetVersion() {
        loadHtml("testing getVersion()");
        executeAsyncJs("mraid.getVersion()", new JsCallback() {
            void done(String version) {
                assertEquals("1.0", version);
            }
        });
    }

    // mraid conform Ads should identify themselves with a script tag
    public void testIdentification() {
        loadHtml("<html><head><script src=\"mraid.js\"></script></head><body>testing identification");
        executeAsyncJs("document.getElementsByTagName('script')[0].src", new JsCallback() {
            void done(String script_tag_src) {
                assertEquals("mraid.js", script_tag_src);
            }
        });
    }

    // initial state should be "loading"
    public void testInitialState() {
        mraidView.loadData("<html><head></head><body>testing initial state"
                + "<script type=\"text/javascript\">test.callback(mraid.getState());"
                + "</script></bod></html>", "text/html", "utf8");
        waitForJsCallback();
        assertEquals("loading", callback_data);
    }

    // state should be "default" when 'ready'
    public void testStateAfterInitialization() {
        loadHtml("testing state after initialization");
        executeAsyncJs("mraid.getState()", new JsCallback() {
            void done(String version) {
                assertEquals("default", version);
            }
        });
    }

    public void testAddEventListenerFunctionExists() {
        loadHtml("testing event listener function exists");
        executeAsyncJs("typeof mraid.addEventListener", new JsCallback() {
            void done(String type) {
                assertEquals("function", type);
            }
        });
    }

    public void testReadyEventListener() {
        loadHtml("testing 'ready' event listeners listen for events");
        mraidView.loadUrl("javascript:mraid.addEventListener('ready', function() {test.callback('works');});");
        mraidView.fireEvent("ready");
        waitForJsCallback();
        assertEquals("works", callback_data);
    }

    public void testStateChangeEventListener() {
        loadHtml("testing 'stateChange' event listeners listen for events");
        mraidView.loadUrl("javascript:mraid.addEventListener('stateChange', function(state) {test.callback(state);});");
        mraidView.fireEvent("stateChange");
        waitForJsCallback();
        assertEquals("default", callback_data);
    }

    public void testViewableChangeEventListener() {
        loadHtml("testing 'viewableChange' event listeners listen for events");
        mraidView.loadUrl("javascript:mraid.addEventListener('viewableChange', function(viewable) {test.callback(viewable);});");
        mraidView.fireEvent("viewableChange");
        waitForJsCallback();
        assertEquals("true", callback_data);
    }

    public void testErrorEventListener() {
        loadHtml("testing 'error' event listeners listen for events");
        mraidView.loadUrl("javascript:mraid.addEventListener('error', function(msg, action) {test.callback(msg+action);});");
        mraidView.fireErrorEvent("some message ", "with some action");
        waitForJsCallback();
        assertEquals("some message with some action", callback_data);
    }

    public void testRemoveEventListener() throws InterruptedException {
        loadHtml("testing removeEventListener removes one specific listener");
        mraidView.loadUrl("javascript:var listener = function() { test.callback('works'); }");
        mraidView.loadUrl("javascript:mraid.addEventListener('ready', listener);");
        mraidView.loadUrl("javascript:mraid.removeEventListener('ready', listener);");
        callback_data = null;
        mraidView.fireEvent("ready");
        Thread.sleep(3000);
        assertEquals(null, callback_data);
    }

    public void testRemoveAllEventListeners() throws InterruptedException {
        loadHtml("testing removeEventListener removes all listener for an event");
        mraidView.loadUrl("javascript:var listener = function() { test.callback(event); }");
        mraidView.loadUrl("javascript:var listener2 = function(event) { test.callback(event); }");
        mraidView.loadUrl("javascript:mraid.addEventListener('ready', listener);");
        mraidView.loadUrl("javascript:mraid.addEventListener('ready', listener2);");
        mraidView.loadUrl("javascript:mraid.removeEventListener('ready');"); // called without second argument
        callback_data = null;
        mraidView.fireEvent("ready");
        Thread.sleep(3000);
        assertEquals(null, callback_data);
    }

    public void testPlacementTypeAccessors() {
        loadHtml("testing placement type getter and setter ");
        executeAsyncJs("mraid.getPlacementType()", new JsCallback() {
            void done(String placementType) {
                assertEquals("inline", placementType); // default
            }
        });
        mraidView.setPlacementType(MadvertiseUtil.PLACEMENT_TYPE_INTERSTITIAL);
        executeAsyncJs("mraid.getPlacementType()", new JsCallback() {
            void done(String placementType) {
                assertEquals("interstitial", placementType); // default
            }
        });
    }

    @UiThreadTest
    public void testIsViewable() throws Throwable {
        loadHtml("testing viewability");
        Thread.sleep(1500);
        assertIsViewable();
        ((ViewGroup) mraidView.getParent()).removeView(mraidView); // detach from screen..
        Thread.sleep(1500); // should become non-viewable
        assertNotViewable(); 
        mraidView.setVisibility(View.INVISIBLE);
        assertNotViewable(); 
        activity.setContentView(mraidView); // re-attach to screen
        Thread.sleep(1500); // should still not be viewable
        assertNotViewable();
        mraidView.setVisibility(View.VISIBLE);
        assertIsViewable();
        mraidView.setVisibility(View.GONE);
        assertNotViewable();
    }
    // little helper
    private void assertNotViewable() {
        executeAsyncJs("mraid.isViewable()", new JsCallback() {
            void done(String viewable) {
                assertFalse(Boolean.parseBoolean(viewable));
            }
        });
    }
    // little helper
    private void assertIsViewable() {
        executeAsyncJs("mraid.isViewable()", new JsCallback() {
            void done(String viewable) {
                assertTrue(Boolean.parseBoolean(viewable));
            }
        });
    }

    // expand properties should be set to screen dimensions by default
    public void testDefaultExpandProperties() {
        loadHtml("testing initial default expandProperties ");
        final DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        executeAsyncJs("JSON.stringify(mraid.getExpandProperties())", new JsCallback() {
            void done(String properties) {
                assertEquals("{\"width\":" + metrics.widthPixels + ",\"height\":"
                        + metrics.heightPixels + ",\"useCustomClose\":false,\"isModal\":false}", properties);
            }
        });
    }

    public void testExpandPropertyAccessors() {
        loadHtml("testing getter and setter for expand properties ");
        mraidView.loadUrl("javascript:mraid.setExpandProperties({width:42,height:23,useCustomClose:true,isModal:true});");
        executeAsyncJs("JSON.stringify(mraid.getExpandProperties())", new JsCallback() {
            void done(String properties) {
                assertEquals("{\"width\":42,\"height\":23,\"useCustomClose\":true,\"isModal\":false}", properties);
            }
        });
        ExpandProperties props = mraidView.getExpandProperties();
        assertEquals(42, props.width);
        assertEquals(23, props.height);
        assertTrue(props.useCustomClose);
        assertFalse(props.isModal); // because this is read-only!
    }

    public void testExpandPropertiesCheckSize() {
        final ExpandProperties properties = mraidView.new ExpandProperties(480, 800);
        final String width = "width", height = "height";
        JSONObject json = new JSONObject();

        // both fit
        try {
            json.put(width, 480);
            json.put(height, 800);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        properties.readJson(json.toString());
        assertTrue(properties.height == 800 && properties.width == 480);

        // height fits, width doesn't
        try {
            json.put(width, 500);
            json.put(height, 800);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        properties.readJson(json.toString());
        assertTrue(properties.height == 768 && properties.width == 480);

        // width fits, height doesn't
        try {
            json.put(width, 480);
            json.put(height, 900);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        properties.readJson(json.toString());
        assertTrue(properties.height == 800 && properties.width == 426);

        // both don't fit
        try {
            json.put(width, 500);
            json.put(height, 900);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        properties.readJson(json.toString());
        assertTrue(properties.height == 800 && properties.width == 444);
    }

    public void testExpandAndClose() throws InterruptedException {
        loadHtml("testing expand</div>");
        mraidView.loadUrl("javascript:mraid.setExpandProperties({height:230});");
        mraidView.loadUrl("javascript:mraid.expand();");
        Thread.sleep(1000);
        assertEquals(230, mraidView.getHeight());
        mraidView.loadUrl("javascript:mraid.close();");
        Thread.sleep(1000);
        assertEquals(53, mraidView.getHeight());
        executeAsyncJs("mraid.getState()", new JsCallback() {
            void done(String properties) {
                assertEquals("default", properties);
            }
        });
    }

    public void testCloseButton() throws Throwable {
        loadHtml("testing close button ");
        mraidView.loadUrl("javascript:mraid.expand();");
        Thread.sleep(500);
        final ImageButton closeButton = (ImageButton)activity.findViewById(43);
        runTestOnUiThread(new Runnable() {
            public void run() {
                closeButton.performClick();
            }
        });
        getInstrumentation().waitForIdleSync();
        assertEquals(53, mraidView.getHeight());
        executeAsyncJs("mraid.getState()", new JsCallback() {
            void done(String properties) {
                assertEquals("default", properties);
            }
        });
    }

    public void testExpandWithUrl() throws InterruptedException {
        loadHtml("testing expand with</div>");
        mraidView.loadUrl("javascript:mraid.setExpandProperties({height:300});");
        mraidView.loadUrl("javascript:mraid.expand('http://andlabs.eu');");
        Thread.sleep(1000);
        assertEquals(300, mraidView.getHeight());
        Thread.sleep(9000);
    }

    public void testUseCustomClose() throws InterruptedException {
        loadHtml("testing custom close ");
        mraidView.loadUrl("javascript:mraid.useCustomClose(true);");
        executeAsyncJs("JSON.stringify(mraid.getExpandProperties().useCustomClose)",
                new JsCallback() {
                    void done(String properties) {
                        assertEquals("true", properties);
                    }
                });
        ExpandProperties props = mraidView.getExpandProperties();
        assertTrue(props.useCustomClose);
        
        mraidView.loadUrl("javascript:mraid.setExpandProperties({height:300});");
        mraidView.loadUrl("javascript:mraid.expand('http://andlabs.eu');");
        Thread.sleep(1000);
        assertEquals(300, mraidView.getHeight());
        
        final ImageButton closeButton = (ImageButton)activity.findViewById(43);
        assertTrue(closeButton.getDrawable() == null);
        Thread.sleep(9000);
    }

    public void testOpenBrowserActivity() {
        loadHtml("testing open external url</div>");
        mraidView.loadUrl("javascript:mraid.open('http://andlabs.eu');");
        ActivityMonitor monitor = getInstrumentation().addMonitor(
                "de.madvertise.android.sdk.MadvertiseBrowserActivity", null, true);
        monitor.waitForActivityWithTimeout(3000);
        assertEquals(1, monitor.getHits());
    }

    public void testMraidExample_static() throws InterruptedException {
        mraidView.loadDataWithBaseURL("file:///android_asset/MRAID_static/src/",
                        "<html><head><script type=\"text/javascript\" src=\"ad_loader.js\"/>" +
                        "</head><body></body></html>",
                        "text/html", "utf8", null);
        Thread.sleep(9000);
    }

    public void testMraidExample_expandable() throws InterruptedException {
        mraidView.loadDataWithBaseURL("file:///android_asset/MRAID_expandable/src/",
                        "<html><head><script type=\"text/javascript\" src=\"ad_loader.js\"/>" +
                        "</head><body></body></html>",
                        "text/html", "utf8", null);
        Thread.sleep(9000);
    }


    // ------------ Test util stuff ---------------------

    private void loadHtml(String html) {
        mraidView.loadData("<html><head></head><body onload=\"test.callback('Go!');\">" +
                            html + "</body></html>", "text/html", "utf8");
        waitForJsCallback();
    }

    private abstract class JsCallback {
        abstract void done(String arg);
    }

    private void executeAsyncJs(String javascript, JsCallback callback) {
        callback_data = null;
        mraidView.loadUrl("javascript:test.callback(" + javascript + ");");
        waitForJsCallback();
        callback.done(callback_data);
    }

    private void waitForJsCallback() {
        new WaitFor() {
            boolean check() {
                return callback_data != null;
            }
        }.run();
    }

    // wait for 'things' to happen...
    private abstract class WaitFor {

        abstract boolean check();

        public void run() {
            long timeout = 5000;
            while (timeout > 0) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Assert.fail("InterruptedException while waiting");
                }
                if (check())
                    return;
                timeout -= 300;
            }
            Assert.fail("Waiting timed out!");
        }
    }
}
