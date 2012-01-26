package de.madvertise.test;

import de.madvertise.android.sdk.mraid.MadvertiseMraidView;
import android.app.Activity;
import android.os.Bundle;

public class MraidTestActivity extends Activity {

    /** Called when the activity is first created. */
    @Override 
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        MadvertiseMraidView mraidView = new MadvertiseMraidView(this);
        mraidView.setId(42); // TODO use xml layout with MadvertiseView container
        setContentView(mraidView);
    }

}
