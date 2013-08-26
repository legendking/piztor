package com.macaroon.piztor;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;

public class Main extends PiztorAct {
	final static int SearchButtonPress = 1;
	final static int FetchButtonPress = 2;
	final static int FocuseButtonPress = 3;
	final static int SuccessFetch = 4;
	final static int FailedFetch = 5;
	final static int TimerFlush = 6;
	ActMgr actMgr;
	ImageButton btnSearch, btnFetch, btnFocus, btnSettings;
	Timer autodate;
	@SuppressLint("HandlerLeak")
	Handler fromGPS = new Handler() {
		@Override
		public void handleMessage(Message m) {
			if (m.what != 0) {
				Location l = (Location) m.obj;
				if (l == null)
					System.out.println("fuck!!!");
				else {
					ReqUpdate r = new ReqUpdate(UserInfo.token,
							UserInfo.username, l.getLatitude(),
							l.getLongitude(), System.currentTimeMillis(), 1000);
					AppMgr.transam.send(r);
				}
			}
		}
	};

	Handler fromTransam = new Handler() {
		@Override
		public void handleMessage(Message m) {
			switch (m.what) {
			case 1:
				ResUpdate update = (ResUpdate) m.obj;
				if (update.t == 0)
					System.out.println("update success");
				else
					System.out.println("update failed");
				break;
			case 2:
				ResLocation location = (ResLocation) m.obj;
				if (location.s == 0) {
					for (int i = 0; i < location.n; i++) {
						System.out.println(location.l.get(i).i + " : "
								+ location.l.get(i).lat + " "
								+ location.l.get(i).lot);
					}
					actMgr.trigger(SuccessFetch);
				} else {
					System.out
							.println("resquest for location must be wrong!!!");
				}
				break;
			case 3:
				ResUserinfo r = (ResUserinfo) m.obj;
				if (r.s == 0) {
					System.out.println(r.id + " " + r.sex + " " + r.groupId);
				} else {
					System.out.println("reqest for userInfo must be wrong!!!");
				}
				break;
			default:
				break;
			}

		}
	};

	String cause(int t) {
		switch (t) {
		case SearchButtonPress:
			return "Search Button Press";
		case FetchButtonPress:
			return "Fetch Button Press";
		case FocuseButtonPress:
			return "Focuse Button Press";
		case SuccessFetch:
			return "Success Fetch";
		case FailedFetch:
			return "Failed Fetch";
		case TimerFlush:
			return "TimerFlush";
		default:
			return "Fuck!!!";
		}
	}

	// TODO flush map view
	void flushMap() {

	}

	class StartStatus extends ActStatus {

		@Override
		void enter(int e) {
			System.out.println("enter start status!!!!");
			if (e == TimerFlush) {
				ReqLocation r = new ReqLocation(UserInfo.token,
						UserInfo.username, 1, System.currentTimeMillis(), 1000);
				AppMgr.transam.send(r);
			}
			if (e == SuccessFetch)
				flushMap();
		}

		@Override
		void leave(int e) {
			System.out.println("leave start status!!!! because" + cause(e));
		}

	}

	class FetchStatus extends ActStatus {

		@Override
		void enter(int e) {
			System.out.println("enter Fetch status!!!!");
			if (e == FetchButtonPress) {
				ReqLocation r = new ReqLocation(UserInfo.token,
						UserInfo.username, 1, System.currentTimeMillis(), 1000);
				AppMgr.transam.send(r);
			}
		}

		@Override
		void leave(int e) {
			System.out.println("leave fetch status!!!! because" + cause(e));
		}

	}

	class FocusStatus extends ActStatus {

		@Override
		void enter(int e) {
			System.out.println("enter focus status!!!!");

		}

		@Override
		void leave(int e) {
			System.out.println("leave focus status!!!! because" + cause(e));

		}

	}

	class AutoUpdate extends TimerTask {

		@Override
		public void run() {
			actMgr.trigger(Main.TimerFlush);
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		id = "Main";
		super.onCreate(savedInstanceState);
		AppMgr.tracker.setHandler(fromGPS);
		ActStatus[] r = new ActStatus[3];
		r[0] = new StartStatus();
		r[1] = new FetchStatus();
		r[2] = new FocusStatus();
		actMgr = new ActMgr(this, r[0], r);
		actMgr.add(r[0], FocuseButtonPress, r[2]);
		actMgr.add(r[0], FetchButtonPress, r[1]);
		actMgr.add(r[0], SuccessFetch, r[0]);
		actMgr.add(r[1], FetchButtonPress, r[0]);
		actMgr.add(r[1], FailedFetch, r[0]);
		actMgr.add(r[1], SuccessFetch, r[0]);
		actMgr.add(r[2], FocuseButtonPress, r[0]);
		actMgr.add(r[0], TimerFlush, r[0]);
		actMgr.add(r[2], TimerFlush, r[2]);
		autodate = new Timer();
		AppMgr.transam.setHandler(fromTransam);
		setContentView(R.layout.activity_main);
	}

	@Override
	protected void onStart() {
		super.onStart();
		btnFetch = (ImageButton) findViewById(R.id.footbar_btn_fetch);
		btnFocus = (ImageButton) findViewById(R.id.footbar_btn_focus);
		btnSearch = (ImageButton) findViewById(R.id.footbar_btn_search);
		btnSettings = (ImageButton) findViewById(R.id.footbar_btn_settings);
		btnFetch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				actMgr.trigger(FetchButtonPress);
			}
		});
		btnFocus.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				actMgr.trigger(FocuseButtonPress);
			}
		});
		autodate.schedule(new AutoUpdate(), 0, 5000);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}