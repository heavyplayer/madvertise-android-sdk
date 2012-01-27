package de.madvertise.android.sdk.mraid;

import de.madvertise.android.sdk.mraid.MadvertiseMraidView.ExpandProperties;

/**
 * Used for the communication with the JavaScript used in the {@link MadvertiseMraidView}.
 */
public class MraidBridge implements IMraidBridge {
    
    private MadvertiseMraidView mMraidView;
    
    private static final String JAVASCRIPT_INDICATOR = "javascript:";
    
    public MraidBridge(MadvertiseMraidView mraidView) {
        mMraidView = mraidView;
    }

    @Override
    public void close() {
        
    }

    @Override
    public void expand() {
        
    }

    @Override
    public void setExpandProperties(String json) {
        
    }

    @Override
    public void addEventListener(String event, String listener) {
        
    }

    @Override
    public void removeEventListener(String event, String listener) {
        
    }

    @Override
    public void open(String url) {
        
    }

    @Override
    public void useCustomClose(boolean useCustomClose) {

    }
    
    public void setExpandProperties(final ExpandProperties properties) {
        final StringBuilder sb = new StringBuilder();
        sb.append("setCustomClose(").append(properties.toString()).append(");");
        inject(sb.toString());
    }
    
    public void setPlacementType(String type) {
        final StringBuilder sb = new StringBuilder();
        sb.append("setPlacementType(").append(type).append(");");
        inject(sb.toString());
    }
    
    public void setIsViewable(boolean isViewable) {
        final StringBuilder sb = new StringBuilder();
        sb.append("setIsViewable(").append(isViewable).append(");");
        inject(sb.toString());
    }
    
    // getVersion is by convention always '1.0' and hence doesn't need a callback method in java

    // Utility methods
    private void inject(final String javaScript) {
        if (javaScript != null) {
            mMraidView.loadUrl(JAVASCRIPT_INDICATOR + javaScript);
        }
    }
    

}
