
package de.madvertise.test;

import junit.framework.Assert;
import de.madvertise.android.sdk.mraid.MadvertiseMraidView;
import de.madvertise.android.sdk.mraid.MadvertiseMraidView.ExpandProperties;
import android.test.ActivityInstrumentationTestCase2;
import android.util.DisplayMetrics;
import android.util.Log;

public class MraidTestSuite extends ActivityInstrumentationTestCase2<MraidTestActivity> {

    private MraidTestActivity activity;

    private MadvertiseMraidView mraidView;

    protected String callback_data;

    public MraidTestSuite() {
        super("de.madvertise.test", MraidTestActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        activity = getActivity();
        mraidView = (MadvertiseMraidView)activity.findViewById(42);
        mraidView.addJavascriptInterface(new Object() {
            public void callback(String data) {
                callback_data = data;
                Log.d("Javascript", "called back: " + data);
            }
        }, "test");
        Thread.sleep(500); // somehow needed to finish initialization
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
        loadHtml("<html><head></head><body>testing initial state" +
                "<script type=\"text/javascript\">test.callback(mraid.getState());" +
                "</script></body></html>");
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
        mraidView.loadUrl("javascript:mraid.addEventListener('ready', function(event) { test.callback(event); });");
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
                assertEquals("{\"width\":" + metrics.widthPixels + ",\"height\":" + metrics.heightPixels +
                            ",\"useCustomClose\":false,\"isModal\":false}", properties);
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
            long timeout = 3000;
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
        } catch (InterruptedException e) {}
    }
}
