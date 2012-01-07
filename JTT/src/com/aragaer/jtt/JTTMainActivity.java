package com.aragaer.jtt;

import java.util.Date;
import java.util.TimeZone;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

public class JTTMainActivity extends Activity {
	private static final int btn_ids[] = { R.id.clockbtn, R.id.alarmbtn,
			R.id.settingsbtn };

	private JTT calculator;
	private final Runnable mUpdateUITimerTask = new Runnable() {
		public void run() {
			JTTClockView hh = (JTTClockView) findViewById(R.id.hour);
			float latitude, longitude;
			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(getBaseContext());
			latitude = Float.parseFloat(settings.getString("posLat", "0.0"));
			longitude = Float.parseFloat(settings.getString("posLong", "0.0"));

			calculator = new JTT(latitude, longitude, TimeZone.getDefault());

			JTTHour hour = calculator.time_to_jtt(new Date());

			hh.setJTTHour(hour);
		}
	};
	private final Handler mHandler = new Handler();

	private JTTPager pager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Button tabs[] = new Button[btn_ids.length];
		for (int i = 0; i < btn_ids.length; i++)
			tabs[i] = (Button) findViewById(btn_ids[i]);

		pager = (JTTPager) findViewById(R.id.tabcontent);
		pager.setTabs(tabs);

		startService(new Intent(JTTService.class.getName()));
		mHandler.postDelayed(mUpdateUITimerTask, 100);
	}

	public void onToggle(View view) {
		pager.btnToggle((Button) view);
	}

	static public class JTTPager extends ViewGroup {
		private static final int SNAP_VELOCITY = 1000;

		private boolean mFirstLayout = true;
		private VelocityTracker mVelocityTracker;
		private int mMaximumVelocity;

		private int mCurrentScreen;

		private float mLastMotionX;
		private float mLastMotionY;
		private int mScrollX;
		private int mScrollY;

		private final static int TOUCH_STATE_REST = 0;
		private final static int TOUCH_STATE_SCROLLING = 1;
		private int mTouchState = TOUCH_STATE_REST;

		private Button[] tabs;

		public JTTPager(Context ctx, AttributeSet attrs) {
			super(ctx, attrs);

			setHapticFeedbackEnabled(false);

			final ViewConfiguration cfg = ViewConfiguration.get(getContext());
			mMaximumVelocity = cfg.getScaledMaximumFlingVelocity();
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);

			final int width = MeasureSpec.getSize(widthMeasureSpec);

			// The children are given the same width and height as the workspace
			final int count = getChildCount();
			for (int i = 0; i < count; i++)
				getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);

			if (mFirstLayout) {
				setHorizontalScrollBarEnabled(false);
				scrollTo(mCurrentScreen * width, 0);
				mFirstLayout = false;
			}
		}

		@Override
		protected void onLayout(boolean changed, int left, int top, int right,
				int bottom) {
			int childLeft = 0;

			final int count = getChildCount();
			for (int i = 0; i < count; i++) {
				final View child = getChildAt(i);
				final int childWidth = child.getMeasuredWidth();
				child.layout(childLeft, 0, childLeft + childWidth,
						child.getMeasuredHeight());
				childLeft += childWidth;
			}
		}

		protected void setTabs(Button[] newTabs) {
			tabs = newTabs;
			tabs[mCurrentScreen].setSelected(true);
		}

		void show() {
			setVisibility(VISIBLE);
		}

		@Override
		public boolean onTouchEvent(MotionEvent ev) {
			if (mVelocityTracker == null)
				mVelocityTracker = VelocityTracker.obtain();
			mVelocityTracker.addMovement(ev);

			final int action = ev.getAction();
			final float x = ev.getX();

			switch (action) {
			case MotionEvent.ACTION_DOWN:
				// Remember where the motion event started
				mLastMotionX = x;
				break;
			case MotionEvent.ACTION_MOVE:
				int deltaX = (int) (mLastMotionX - x);
				mTouchState = TOUCH_STATE_SCROLLING;
				// Scroll to follow the motion event
				mLastMotionX = x;

				final int maxWidth = (getChildCount() - 1) * getWidth();

				if (mScrollX + deltaX < 0)
					deltaX = -mScrollX;
				else if (mScrollX + deltaX > maxWidth)
					deltaX = maxWidth - mScrollX;

				scrollBy(deltaX, 0);
				break;
			case MotionEvent.ACTION_UP:
				if (mTouchState == TOUCH_STATE_SCROLLING) {
					final int bump = getWidth() / 2 + 1;
					final VelocityTracker velocityTracker = mVelocityTracker;
					velocityTracker.computeCurrentVelocity(1000,
							mMaximumVelocity);
					int velocityX = (int) velocityTracker.getXVelocity();

					if (velocityX > SNAP_VELOCITY && mCurrentScreen > 0)
						// Fling hard enough to move left
						scrollBy(-bump, 0);
					else if (velocityX < -SNAP_VELOCITY
							&& mCurrentScreen < getChildCount() - 1)
						// Fling hard enough to move right
						scrollBy(bump, 0);

					snapToScreen(mCurrentScreen);

					mVelocityTracker.recycle();
					mVelocityTracker = null;
				}
				mTouchState = TOUCH_STATE_REST;
				break;
			case MotionEvent.ACTION_CANCEL:
				mTouchState = TOUCH_STATE_REST;
			}

			return true;
		}

		@Override
		public void scrollBy(int scrollX, int scrollY) {
			mScrollX += scrollX;
			mScrollY += scrollY;
			super.scrollBy(scrollX, scrollY);
			selectDestination();
		}

		@Override
		public void scrollTo(int scrollX, int scrollY) {
			mScrollX = scrollX;
			mScrollY = scrollY;
			super.scrollTo(scrollX, scrollY);
			selectDestination();
		}

		private void selectDestination() {
			final int w = getWidth();
			if (w == 0)
				return;
			final int whichScreen = (int) (mScrollX + w / 2) / w;

			selectScreen(whichScreen);
		}

		private void selectScreen(int whichScreen) {
			tabs[mCurrentScreen].setSelected(false);
			mCurrentScreen = whichScreen;
			tabs[mCurrentScreen].setSelected(true);
		}

		public void snapToScreen(int whichScreen) {
			selectScreen(whichScreen);
			super.scrollTo(whichScreen * getWidth(), 0);
			mScrollX = whichScreen * getWidth();
		}

		protected void btnToggle(Button btn) {
			if (btn == tabs[mCurrentScreen])
				return;
			for (int i = 0; i < tabs.length; i++)
				if (tabs[i] == btn) {
					snapToScreen(i);
					return;
				}
		}
	}
}
