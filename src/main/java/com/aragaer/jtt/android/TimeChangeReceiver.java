// -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; -*-
// vim: et ts=4 sts=4 sw=4 syntax=java
package com.aragaer.jtt.android;

import com.aragaer.jtt.astronomy.*;
import com.aragaer.jtt.core.*;

import android.content.*;
import android.util.Log;


public class TimeChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_TIME_CHANGED)
            || action.equals(Intent.ACTION_DATE_CHANGED))
            try {
                SolarEventCalculator calculator = SscAdapter.getInstance();
                IntervalProvider provider = new SscCalculator(calculator);
                Clockwork clockwork = new Clockwork(provider);
                new AndroidTicker(context, clockwork).start();
            } catch (IllegalStateException e) {
                Log.i("JTT CLOCKWORK", "Time change while service is not running, ignore");
            }
    }
}
