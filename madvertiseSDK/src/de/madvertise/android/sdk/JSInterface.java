package de.madvertise.android.sdk;

import android.util.Log;

public class JSInterface {
	
	public void ping(String str) {
		MadvertiseUtil.logMessage(null, Log.INFO, str);
	}

}
