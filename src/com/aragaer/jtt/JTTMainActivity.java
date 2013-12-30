package com.aragaer.jtt;

import com.aragaer.jtt.core.Clockwork;
import com.aragaer.jtt.today.TodayAdapter;

import android.app.ActivityGroup;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Window;
import android.widget.ListView;

public class JTTMainActivity extends ActivityGroup implements
		SharedPreferences.OnSharedPreferenceChangeListener {
	private ClockView clock;
	private JTTPager pager;
	private TodayAdapter today;

	private final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (!intent.getAction().equals(Clockwork.ACTION_JTT_TICK))
				return;
			final int wrapped = intent.getIntExtra("jtt", 0);

			clock.setHour(wrapped);
			today.tick();
		}
	};

	private int settings_tab = 0;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(Settings.getTheme(this));
		super.onCreate(savedInstanceState);
		startService(new Intent(this, JttService.class));

		pager = new JTTPager(this);

		clock = new ClockView(this);
		pager.addTab(clock, R.string.clock);

		final ListView today_list = new ListView(this);
		today = new TodayAdapter(this, 0);
		today_list.setAdapter(today);
		today_list.setDividerHeight(-getResources().getDimensionPixelSize(R.dimen.today_divider_neg));
		pager.addTab(today_list, R.string.today);

		final Window sw = getLocalActivityManager().startActivity("settings",
				new Intent(this, Settings.class));
		settings_tab = pager.addTab(sw.getDecorView(), R.string.settings);

		setContentView(pager);

		registerReceiver(receiver, new IntentFilter(Clockwork.ACTION_JTT_TICK));
	}

	@Override
	protected void onStart() {
		super.onStart();
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (!settings.contains("jtt_loc")) // location is not set
			pager.selectScreen(settings_tab);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(receiver);
	}

	public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
		if (key.equals("jtt_theme") || key.equals(Settings.PREF_LOCALE)) {
			final int flags = Intent.FLAG_ACTIVITY_NO_ANIMATION;
			finish();
			startActivity(getIntent().addFlags(flags));
			// restart settings activity on top of this
			startActivity(new Intent(this, Settings.class).addFlags(flags));
		}
	}
}
