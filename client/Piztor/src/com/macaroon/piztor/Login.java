package com.macaroon.piztor;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class Login extends PiztorAct {

	ActMgr actMgr;
	Button btnLogin;
	EditText edtUser, edtPass;

	int loginButtonClick = 1, retryButtonClick = 2, loginFailed = 3;

	@SuppressLint("HandlerLeak")
	Handler hand = new Handler() {
		@Override
		public void handleMessage(Message m) {
			System.out.println("receive what : " + m.what);
			if (m.what == -1) {
				((Exception) m.obj).printStackTrace();
				actMgr.trigger(loginFailed);
				return;
			}
			if (m.what == 0) {
				ResLogin res = (ResLogin) m.obj;
				UserInfo.token = res.t;
				UserInfo.id = res.uid;
				UserInfo.username = edtUser.getText().toString();
				System.out.println(res.s + "   :!!!    " + res.t);
				actMgr.trigger(AppMgr.loginSuccess);
			} else {
				actMgr.trigger(loginFailed);
			}
		}
	};

	class StartStatus extends ActStatus {

		@Override
		void enter(int e) {
		}

		@Override
		void leave(int e) {
		}
	}

	class LoginStatus extends ActStatus {

		@Override
		void enter(int e) {
			String user = edtUser.getText().toString();
			String pass = edtPass.getText().toString();
			long nowtime = System.currentTimeMillis();
			System.out.println(user + " : " + pass + "\n");
			AppMgr.transam.send(new ReqLogin(user, pass, nowtime, 5000));
		}

		@Override
		void leave(int e) {

		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		id = "login";
		super.onCreate(savedInstanceState);
		ActStatus[] r = new ActStatus[2];
		AppMgr.transam.setHandler(hand);
		r[0] = new StartStatus();
		r[1] = new LoginStatus();
		actMgr = new ActMgr(this, r[0], r);
		actMgr.add(r[0], loginButtonClick, r[1]);
		actMgr.add(r[1], loginFailed, r[0]);
		setContentView(R.layout.activity_login);
	}

	@Override
	protected void onStart() {
		super.onStart();
		btnLogin = (Button) findViewById(R.id.login_btn_login);
		edtUser = (EditText) findViewById(R.id.user_id);
		edtPass = (EditText) findViewById(R.id.user_pass);
		btnLogin.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				actMgr.trigger(loginButtonClick);
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		 if (keyCode == KeyEvent.KEYCODE_BACK) {
			 AppMgr.exit();
			 return true;
		 }
		 return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.login, menu);
		return true;
	}

}
