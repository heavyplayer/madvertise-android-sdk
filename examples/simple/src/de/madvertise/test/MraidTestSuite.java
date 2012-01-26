package de.madvertise.test;


import junit.framework.Assert;
import de.madvertise.android.sdk.mraid.MadvertiseMraidView;
import android.test.ActivityInstrumentationTestCase2;
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
        mraidView = (MadvertiseMraidView) activity.findViewById(42);
        mraidView.addJavascriptInterface(new Object(){
            public void callback(String data) {
                callback_data = data;
                Log.d("Javascript", "called back: "+data);
            }
        }, "test");
    }



    // getVersion should return "1.0"
    public void testGetVersion() {
        mraidView.loadUrl("javascript:var mraid = {}; mraid.getVersion = function() { return '1.0'; }");
        executeAsyncJs("mraid.getVersion()", new JsCallback() {
            void done(String version) {
                assertEquals("1.0", version);
            }
        });
    }
    
    // mraid conform Ads should identify themselves with a script tag
    public void testIdentification() {
        loadHtml("<html><head><script src=\"mraid.js\"></script></head></html>");
        executeAsyncJs("document.getElementsByTagName('script')[0].src", new JsCallback() {
            void done(String script_tag_src) {
                assertEquals("mraid.js", script_tag_src);
            }
        });
    }
    
    // it should support static non-rich media Ads (plain HTML5/Css/Js)
    public void testFallback() {
        loadHtml("<html><body>testing HTML5 fallback</body></html>");
        executeAsyncJs("typeof mraid", new JsCallback() {
            void done(String type) {
                assertEquals("undefined", type);
            }
        });
    }



    // ------------ Test util stuff ---------------------
    
    private void loadHtml(String html) {
        mraidView.loadData(html, "text/html", "utf8");
        new WaitFor() { boolean check() { return mraidView.getProgress() == 100; }}.run();
    }
    
    private abstract class JsCallback {
        abstract void done(String arg);
    }
    
    private void executeAsyncJs(String javascript, JsCallback callback) {
        callback_data = null;
        mraidView.loadUrl("javascript:test.callback("+javascript+");");
        new WaitFor() { boolean check() { return callback_data != null; }}.run();
        callback.done(callback_data);
    }
    
    // wait for 'things' to happen...
    private abstract class WaitFor {

        abstract boolean check();

        public void run() {
            long timeout = 3000;
            while (timeout > 0) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Assert.fail("InterruptedException while waiting");
                }
                if (check()) return;
                timeout -= 200;
            }
            Assert.fail("Waiting timed out!");
        }
    }
}
