
package de.madvertise.android.sdk;

import java.io.File;
import java.util.regex.Matcher;

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
import android.widget.Toast;
import de.madvertise.android.sdk.MadvertiseMraidView.ExpandProperties;

public class MraidTestSuite extends ActivityInstrumentationTestCase2<Activity> {

    private MadvertiseMraidView mraidView;
    private Activity testActivity;
    private String callback_data;
    private boolean go;
    private float scale;

    public MraidTestSuite() {
        super("de.madvertise.android.sdk", Activity.class);
    }

    @Override
    protected void setUp() {
        try {
            super.setUp();
            testActivity = getActivity();
            runTestOnUiThread(new Runnable() {
                public void run() {
                    mraidView = new MadvertiseMraidView(testActivity);
                    FrameLayout layout = new FrameLayout(testActivity);
                    scale = testActivity.getResources().getDisplayMetrics().density;
                    mraidView.setLayoutParams(new FrameLayout.LayoutParams((int) (300 * scale), (int) (53 * scale)));
                    layout.addView(mraidView);
                    testActivity.setContentView(layout);
                }
            });
            getInstrumentation().waitForIdleSync();
            mraidView.addJavascriptInterface(new Object() {
                @SuppressWarnings("unused")
                public void callback(String data) {
                    callback_data = data;
                    Log.d("Javascript", "called back: " + data);
                }

                @SuppressWarnings("unused")
                public void go() {
                    go = true;
                }
            }, "test");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void testGlobalVariableScope() {
        loadHtml("mraid should not polute the global scope with variables");
        mraidView.injectJs("var state = 'Ad state';"); // Ad-Dev uses same variable name
        executeAsyncJs("mraid.getState()", new JsCallback() {
            void done(String state) {
                assertEquals("default", state); // should not interfere  
            }
        });
    }

    public void testCacheHack() {
        loadHtml("mraid.js should be loaded out of cache");
        executeAsyncJs("typeof mraid", new JsCallback() {
            void done(String type) {
                assertEquals("object", type);
            }
        });
    }

    public void testMraidCacheFileCopy() {
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
            File mraid = new File("/data/data/" + testActivity.getPackageName()
                    + "/cache/webviewCache/mraid");
            mraid.delete();
            loadHtml("mraid.js should be copied to cache directory");
            assertTrue(mraid.exists());
            // Log.d("cache",
            // ""+CacheManager.getCacheFile("http://foo.bar/mraid.js",
            // headers));
        }

    }

    public void testGetVersion() {
        loadHtml("getVersion() should return '1.0'");
        executeAsyncJs("mraid.getVersion()", new JsCallback() {
            void done(String version) {
                assertEquals("1.0", version);
            }
        });
    }

    public void testInitialState() {
        loadHtml("getState() should initially return 'loading' " +
                "<script type=\"text/javascript\"> test.callback(mraid.getState()); </script>");
        waitForJsCallback();
        assertEquals("loading", callback_data);
    }

    public void testStateAfterInitialization() throws InterruptedException {
        loadHtml("getState() should return 'default' after mraid is ready");
        Thread.sleep(1000);
        executeAsyncJs("mraid.getState()", new JsCallback() {
            void done(String version) {
                assertEquals("default", version);
            }
        });
    }

    public void testAddEventListenerFunctionExists() {
        loadHtml("addEventListener() function should exists");
        executeAsyncJs("typeof mraid.addEventListener", new JsCallback() {
            void done(String type) {
                assertEquals("function", type);
            }
        });
    }

    public void testReadyEventListener() {
        loadHtml("event listeners should be able to listen for 'ready' events");
        mraidView.injectJs("mraid.addEventListener('ready', function() {test.callback('works');});");
        mraidView.fireEvent("ready");
        waitForJsCallback();
        assertEquals("works", callback_data);
    }

    public void testStateChangeEventListener() {
        loadHtml("event listeners should be able to listen for 'stateChange' events");
        mraidView.injectJs("mraid.addEventListener('stateChange', function(state) {test.callback(state);});");
        mraidView.setState(MadvertiseMraidView.STATE_HIDDEN);
        waitForJsCallback();
        assertEquals("hidden", callback_data);
    }

    public void testViewableChangeEventListener() throws Throwable {
        loadHtml("event listeners should be able to listen for 'viewableChange' events");
        mraidView.injectJs("mraid.addEventListener('viewableChange', function(viewable) {test.callback(viewable);});");
        runTestOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                mraidView.setVisibility(View.INVISIBLE);
            }
        });
        waitForJsCallback();
        assertEquals("false", callback_data);
    }

    public void testErrorEventListener() {
        loadHtml("event listeners should be able to listen for 'error' events");
        mraidView.injectJs("mraid.addEventListener('error', function(msg, action) {test.callback(msg+action);});");
        mraidView.fireErrorEvent("some message ", "with some action");
        waitForJsCallback();
        assertEquals("some message with some action", callback_data);
    }

    public void testRemoveEventListener() throws InterruptedException {
        loadHtml("removeEventListener('ready', aListener) should remove aListener");
        mraidView.injectJs("var listener = function() { test.callback('works'); }" +
                "mraid.addEventListener('ready', listener);" +
                "mraid.removeEventListener('ready', listener);");
        callback_data = null;
        mraidView.fireEvent("ready");
        Thread.sleep(3000);
        assertEquals(null, callback_data);
    }

    public void testRemoveAllEventListeners() throws InterruptedException {
        loadHtml("removeEventListener('ready') should remove all listeners");
        mraidView.injectJs("var listener = function() { test.callback(event); } " +
                "var listener2 = function(event) { test.callback(event); } " +
                "mraid.addEventListener('ready', listener1);" +
                "mraid.addEventListener('ready', listener2);");
        mraidView.injectJs("mraid.removeEventListener('ready');"); // called
                                                                   // without
                                                                   // second
                                                                   // argument
        callback_data = null;
        mraidView.fireEvent("ready");
        Thread.sleep(3000);
        assertEquals(null, callback_data);
    }

    public void testPlacementTypeAccessors() {
        loadHtml("getPlacementType should be accessible");
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

    public void testIsViewable() throws Throwable {
        loadHtml("isViewable() should tell if the ad is visible");
        Thread.sleep(1500);
        assertIsViewable();
        runTestOnUiThread(new Runnable() {
            public void run() {
//                ((ViewGroup) mraidView.getParent()).removeView(mraidView); // detach from screen..
                Toast.makeText(testActivity, "now it should NOT be 'viewable'", Toast.LENGTH_LONG)
                        .show();
            }
        });
        Thread.sleep(1500); // should become non-viewable
        assertNotViewable();
        runTestOnUiThread(new Runnable() {
            public void run() {
                mraidView.setVisibility(View.INVISIBLE);
            }
        });
        Thread.sleep(1500); // should still not be viewable
        assertNotViewable();
        runTestOnUiThread(new Runnable() {
            public void run() {
//                testActivity.setContentView(mraidView); // re-attach to screen
            }
        });
        Thread.sleep(1500); // should still not be viewable
        assertNotViewable();
        runTestOnUiThread(new Runnable() {
            public void run() {
                mraidView.setVisibility(View.VISIBLE);
            }
        });
        assertIsViewable();
        runTestOnUiThread(new Runnable() {
            public void run() {
                mraidView.setVisibility(View.GONE);
            }
        });
        assertNotViewable();
    }

    // helper
    private void assertNotViewable() {
        executeAsyncJs("mraid.isViewable()", new JsCallback() {
            void done(String viewable) {
                assertFalse(Boolean.parseBoolean(viewable));
            }
        });
    }

    // helper
    private void assertIsViewable() {
        executeAsyncJs("mraid.isViewable()", new JsCallback() {
            void done(String viewable) {
                assertTrue(Boolean.parseBoolean(viewable));
            }
        });
    }

    public void testDefaultExpandProperties() {
        loadHtml("expand properties should be set to screen dimensions by default");
        final DisplayMetrics metrics = testActivity.getResources().getDisplayMetrics();
        executeAsyncJs("JSON.stringify(mraid.getExpandProperties())", new JsCallback() {
            void done(String properties) {
                assertEquals("{\"width\":" + metrics.widthPixels + ",\"height\":"
                        + metrics.heightPixels + ",\"useCustomClose\":false,\"isModal\":false}",
                        properties);
            }
        });
    }

    public void testExpandPropertyAccessors() {
        loadHtml("expand properties should be accessible");
        mraidView.injectJs("mraid.setExpandProperties({width:42,height:23,useCustomClose:true,isModal:true});");
        executeAsyncJs("JSON.stringify(mraid.getExpandProperties())", new JsCallback() {
            void done(String properties) {
                assertEquals(
                        "{\"width\":42,\"height\":23,\"useCustomClose\":true,\"isModal\":false}",
                        properties);
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
        loadHtml("expand() and close() should do what their names imply");
        mraidView.injectJs("mraid.setExpandProperties({height:230});");
        mraidView.injectJs("mraid.expand();");
        Thread.sleep(1000);
        assertEquals(230, mraidView.getHeight());
        mraidView.injectJs("mraid.close();");
        Thread.sleep(1000);
        assertEquals((int) (53 * scale), mraidView.getHeight());
        executeAsyncJs("mraid.getState()", new JsCallback() {
            void done(String properties) {
                assertEquals("default", properties);
            }
        });
    }

    public void testExpandWithUrl() throws Throwable {
        loadHtml("testing expand with url");
        mraidView.injectJs("mraid.setExpandProperties({height:300});");
        mraidView.injectJs("mraid.expand('http://andlabs.eu');");
        Thread.sleep(1000);
        assertEquals(300, mraidView.getHeight());
        Thread.sleep(9000);
    }

    public void testCloseButton() throws Throwable {
        loadHtml("a close button should always be present");
        mraidView.injectJs("mraid.expand();");
        Thread.sleep(500);
        final ImageButton closeButton = (ImageButton) testActivity.findViewById(43);
        runTestOnUiThread(new Runnable() {
            public void run() {
                closeButton.performClick();
            }
        });
        getInstrumentation().waitForIdleSync();
        assertEquals((int) (53 * scale), mraidView.getHeight());
        executeAsyncJs("mraid.getState()", new JsCallback() {
            void done(String properties) {
                assertEquals("default", properties);
            }
        });
    }

    public void testUseCustomClose() throws Throwable {
        loadHtml("useCustomClose(true) should make the close button invisible (but still clickable)");
        mraidView.injectJs("mraid.useCustomClose(true);");
        executeAsyncJs(
                "JSON.stringify(mraid.getExpandProperties().useCustomClose)",
                new JsCallback() {
                    void done(String properties) {
                        assertEquals("true", properties);
                    }
                });
        ExpandProperties props = mraidView.getExpandProperties();
        assertTrue(props.useCustomClose);
        mraidView.injectJs("mraid.setExpandProperties({height:300});");
        mraidView.injectJs("mraid.expand('http://andlabs.eu');");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        assertEquals(300, mraidView.getHeight());
        final ImageButton closeButton = (ImageButton) testActivity
                .findViewById(43);
        assertTrue(closeButton.getDrawable() == null);
        runTestOnUiThread(new Runnable() {
            public void run() {
                closeButton.performClick();
            }
        });
        getInstrumentation().waitForIdleSync();
        // TODO close button doesn't close if custom close is set true
        // assertEquals(53, mraidView.getHeight());
        Thread.sleep(3000);
    }

    public void testOpenBrowserActivity() throws InterruptedException {
        loadHtml("open(url) should start browser activity");
        mraidView.injectJs("mraid.open('http://andlabs.eu');");
        ActivityMonitor monitor = getInstrumentation().addMonitor(
                "de.madvertise.android.sdk.MadvertiseBrowserActivity", null, true);
        monitor.waitForActivityWithTimeout(3000);
        // assertEquals(1, monitor.getHits());
    }

    public void testJsUrlSplitter() {
        Matcher m = MadvertiseMraidView.sUrlSplitter.matcher("http://domain.com/ad.html");
        assertFalse(m.matches());
        m = MadvertiseMraidView.sUrlSplitter.matcher("wrong://domain.com/some_ad.js");
        assertFalse(m.matches());
        m = MadvertiseMraidView.sUrlSplitter.matcher("http://no/domain/some_ad.js");
        assertFalse(m.matches());
        m = MadvertiseMraidView.sUrlSplitter.matcher("http://domain.com/some_ad.js");
        assertTrue(m.matches());
        assertEquals("http://domain.com/", m.group(1));
        assertEquals("some_ad.js", m.group(2));
        m = MadvertiseMraidView.sUrlSplitter.matcher("file:///android_asset/path/ad.js");
        assertTrue(m.matches());
        assertEquals("file:///android_asset/path/", m.group(1));
        assertEquals("ad.js", m.group(2));
        m = MadvertiseMraidView.sUrlSplitter.matcher("http://sub.domain.com/ad.js");
        assertTrue(m.matches());
        assertEquals("http://sub.domain.com/", m.group(1));
        assertEquals("ad.js", m.group(2));
        m = MadvertiseMraidView.sUrlSplitter.matcher("http://domain.de/with/long/path/ad.js");
        assertTrue(m.matches());
        assertEquals("http://domain.de/with/long/path/", m.group(1));
        assertEquals("ad.js", m.group(2));
    }

    public void testXample_static() throws InterruptedException {
        // mraidView.loadAd("http://andlabs.info/jobs/MRAID_static/src/ad_loader.js");
        mraidView.loadAd("file:///android_asset/MRAID_static/src/ad_loader.js");
        Thread.sleep(23000);
    }

    public void testXample_expandable() throws InterruptedException {
        // mraidView.loadAd("http://andlabs.info/jobs/MRAID_expandable/src/ad_loader.js");
        mraidView.loadAd("file:///android_asset/MRAID_expandable/src/ad_loader.js");
        Thread.sleep(23000);
    }

    public void testXample_video() throws InterruptedException {
        mraidView.loadAd("file:///android_asset/MRAID_video/video.html");
        Thread.sleep(42000);
    }

    public void testXample_xHTML5_Ad() throws InterruptedException {
        mraidView.loadAd("file:///android_asset/MRAID_andlabs_html_ad/banner.html");
        Thread.sleep(42000);
    }

    // ------------ Test util stuff ---------------------

    private void loadHtml(String html) {
        mraidView.loadDataWithBaseURL("http://foo.bar/", "<html><head>" +
                "<script src=\"mraid.js\"></script>" +
                "<style type=\"text/css\">" +
                "body { color: white; }" +
                "</style></head>" +
                "<body onload=\"test.go();\">" + html + "</body>" +
                "</html>", "text/html", "utf8", null);
        new WaitFor() {
            boolean check() {
                return go;
            }
        }.run();
    }

    private abstract class JsCallback {
        abstract void done(String arg);
    }

    private void executeAsyncJs(String javascript, JsCallback callback) {
        callback_data = null;
        mraidView.injectJs("test.callback(" + javascript + ");");
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
