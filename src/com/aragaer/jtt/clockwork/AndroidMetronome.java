package com.aragaer.jtt.clockwork;
// vim: et ts=4 sts=4 sw=4

import android.content.Context;


public class AndroidMetronome implements Metronome {
    private Context context;
    private Clockwork clockwork;

    public AndroidMetronome(Context context) {
        this.context = context;
    }

    public void attachTo(Clockwork clockwork) {
        this.clockwork = clockwork;
    }

	public void start(long start, long tickLength) {
		TickService.setCallback(new ClockworkTickCallback(clockwork, start, tickLength));
		TickService.start(context, start, tickLength);
	}

	public void stop() {
		TickService.stop(context);
	}
}
