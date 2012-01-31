
package de.madvertise.test;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.FrameLayout;
import de.madvertise.android.sdk.MadvertiseMraidView;
import de.madvertise.android.sdk.MadvertiseMraidView.ExpandProperties;

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
                public void callback(String data) {
                    callback_data = data;
                    Log.d("Javascript", "called back: " + data);
                }
            }, "test");
            Thread.sleep(1200); // somehow needed to finish initialization
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void testAdControllerExists() {
        loadHtml("<html><head></head><body>testing Ad Controller exists</body></html>");
        executeAsyncJs("typeof mraid", new JsCallback() {
            void done(String type) {
                assertEquals("object", type);
            }
        });
    }

    // getVersion should return "1.0"
    public void testGetVersion() {
        loadHtml("<html><head></head><body>testing getVersion()</body></html>");
        executeAsyncJs("mraid.getVersion()", new JsCallback() {
            void done(String version) {
                assertEquals("1.0", version);
            }
        });
    }

    // mraid conform Ads should identify themselves with a script tag
    public void testIdentification() {
        loadHtml("<html><head><script src=\"mraid.js\"></script></head><body>testing identification</body></html>");
        executeAsyncJs("document.getElementsByTagName('script')[0].src", new JsCallback() {
            void done(String script_tag_src) {
                assertEquals("mraid.js", script_tag_src);
            }
        });
    }

    // initial state should be "loading"
    public void testInitialState() {
        loadHtml("<html><head></head><body>testing initial state"
                + "<script type=\"text/javascript\">test.callback(mraid.getState());"
                + "</script></body></html>");
        waitForJsCallback();
        assertEquals("loading", callback_data);
    }

    // state should be "default" when 'ready'
    public void testStateAfterInitialization() {
        loadHtml("<html><head></head><body>testing state after initialization</body></html>");
        Log.d("TEST", "before later State check");
        executeAsyncJs("mraid.getState()", new JsCallback() {
            void done(String version) {
                assertEquals("default", version);
            }
        });
    }

    public void testAddEventListenerFunctionExists() {
        loadHtml("<html><head></head><body>testing event listener function exists</body></html>");
        executeAsyncJs("typeof mraid.addEventListener", new JsCallback() {
            void done(String type) {
                assertEquals("function", type);
            }
        });
    }

    public void testEventListenersListenForEvents() {
        loadHtml("<html><head></head><body>testing event listeners listen for events</body></html>");
        mraidView.loadUrl("javascript:mraid.addEventListener('ready', function(event) {test.callback(event);});");
        mraidView.fireEvent("ready");
        waitForJsCallback();
        assertEquals("ready", callback_data);
    }

    public void testRemoveEventListener() {
        loadHtml("<html><head></head><body>testing removeEventListener removes one specific listener</body></html>");
        mraidView.loadUrl("javascript:var listener = function(event) { test.callback(event); }");
        mraidView.loadUrl("javascript:mraid.addEventListener('ready', listener);");
        mraidView.loadUrl("javascript:mraid.removeEventListener('ready', listener);");
        mraidView.fireEvent("ready");
        waitForTimeOut();
        assertEquals(null, callback_data);
    }

    public void testRemoveAllEventListeners() {
        loadHtml("<html><head></head><body>testing removeEventListener removes all listener for an event</body></html>");
        mraidView.loadUrl("javascript:var listener = function(event) { test.callback(event); }");
        mraidView.loadUrl("javascript:var listener2 = function(event) { test.callback(event); }");
        mraidView.loadUrl("javascript:mraid.addEventListener('ready', listener);");
        mraidView.loadUrl("javascript:mraid.addEventListener('ready', listener2);");
        mraidView.loadUrl("javascript:mraid.removeEventListener('ready');"); // called without second argument
        mraidView.fireEvent("ready");
        waitForTimeOut();
        assertEquals(null, callback_data);
    }

    // expand properties should be set to screen dimensions by default
    public void testInitialExpandProperties() {
        loadHtml("<html><head></head><body>testing initial expandProperties </body></html>");
        final DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        executeAsyncJs("JSON.stringify(mraid.getExpandProperties())", new JsCallback() {
            void done(String properties) {
                assertEquals("{\"width\":" + metrics.widthPixels + ",\"height\":"
                        + metrics.heightPixels + ",\"useCustomClose\":false,\"isModal\":false}", properties);
            }
        });
    }

    public void testExpandPropertyAccessors() {
        loadHtml("<html><head></head><body>testing getter and setter for expand properties </body></html>");
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

    public void testUseCustomClose() {
        loadHtml("<html><head></head><body>testing convenience setter for custom close handle </body></html>");
        mraidView.loadUrl("javascript:mraid.useCustomClose(true);");
        executeAsyncJs("JSON.stringify(mraid.getExpandProperties().useCustomClose)", new JsCallback() {
            void done(String properties) {
                assertEquals("true", properties);
            }
        });
        ExpandProperties props = mraidView.getExpandProperties();
        assertTrue(props.useCustomClose);
    }

    public void testPlacementTypeAccessor() {
        loadHtml("<html><head></head><body>testing placement type getter and setter </body></html>");
        executeAsyncJs("mraid.getPlacementType()", new JsCallback() {
            void done(String placementType) {
                assertEquals("inline", placementType); // default
            }
        });
        mraidView.setPlacementType("interstitial");
        executeAsyncJs("mraid.getPlacementType()", new JsCallback() {
            void done(String placementType) {
                assertEquals("interstitial", placementType); // default
            }
        });
    }

    public void testIsViewable() throws Throwable {
        runTestOnUiThread(new Runnable() {
            public void run() {
                mraidView = new MadvertiseMraidView(activity);
                // mraidView.setVisibility(View.GONE);
            }
        });
        getInstrumentation().waitForIdleSync();
        mraidView.addJavascriptInterface(new Object() {
            public void callback(String data) {
                callback_data = data;
                Log.d("Javascript", "called back: " + data);
            }
        }, "test");
        loadHtml("<html><head></head><body>this view is not visible (yet)</body></html>");
        Thread.sleep(1500);
        executeAsyncJs("mraid.isViewable()", new JsCallback() {
            void done(String viewable) {
                assertFalse(Boolean.parseBoolean(viewable));
            }
        });
        runTestOnUiThread(new Runnable() {
            public void run() { // make it visible
            // mraidView.setVisibility(View.VISIBLE);
                activity.setContentView(mraidView);
            }
        });
        Thread.sleep(1500);
        // loadHtml("<html><head></head><body>this view becomes now visible!</body></html>");
        executeAsyncJs("mraid.isViewable()", new JsCallback() {
            void done(String viewable) {
                assertTrue(Boolean.parseBoolean(viewable));
            }
        });
    }
    
    public void testExpandAndClose() throws InterruptedException {
        loadHtml("<html><head></head><body>testing expand</div></body></html>");
        mraidView.loadUrl("javascript:mraid.setExpandProperties({height:230});");
        mraidView.loadUrl("javascript:mraid.expand();");
        Thread.sleep(1000);
        assertEquals(230, mraidView.getHeight());
        mraidView.loadUrl("javascript:mraid.close();");
        Thread.sleep(1000);
        assertEquals(53, mraidView.getHeight());
    }

    public void testExpandWithUrl() throws InterruptedException {
        loadHtml("<html><head></head><body>testing expand with</div></body></html>");
        mraidView.loadUrl("javascript:mraid.setExpandProperties({height:450});");
        mraidView.loadUrl("javascript:mraid.expand('http://andlabs.eu');");
        Thread.sleep(1000);
        assertEquals(450, mraidView.getHeight());
        Thread.sleep(9000);
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

    // ------------ Test util stuff ---------------------

    private void loadHtml(String html) {
        mraidView.loadData(html, "text/html", "utf8");
        new WaitFor() {
            boolean check() {
                return mraidView.getProgress() == 100;
            }
        }.run();
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

    private void waitForTimeOut() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }
    }
}
