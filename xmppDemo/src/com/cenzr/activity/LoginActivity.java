package com.cenzr.activity;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.cenzr.R;
import com.cenzr.service.IMService;
import com.cenzr.service.PushService;
import com.cenzr.utils.ThreadUtils;
import com.cenzr.utils.ToastUtils;


public class LoginActivity extends ActionBarActivity {

//	public static final String	HOST		= "192.168.1.100";	// 主机ip
	public static final String	HOST		= "10.10.156.27";	// 主机ip
//	public static final String	HOST		= "192.168.155.1";	// 主机ip
	public static final int		PORT		= 5222;			// 对应的端口号
	public static final String	SERVICENAME	= "cenzr.com";
	private TextView			mEtUserName;
	private TextView			mEtPassWord;
	private Button				mBtnLogin;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		initView();
		initListener();
	}

	private void initListener() {
		mBtnLogin.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String userName = mEtUserName.getText().toString();
				final String passWord = mEtPassWord.getText().toString();
				// 判断用户名是否为空
				if (TextUtils.isEmpty(userName)) {// 用户名为空
					mEtUserName.setError("用户名不能为空");
					return;
				}
				// 判断密码是否为空
				if (TextUtils.isEmpty(passWord)) {// 用户名为空
					mEtPassWord.setError("密码不能为空");
					return;
				}

				ThreadUtils.runInThread(new Runnable() {
					@Override
					public void run() {
						try {
							// 1.创建连接配置对象
							ConnectionConfiguration config = new ConnectionConfiguration(HOST, PORT);

							// 额外的配置(方面我们开发,上线的时候,可以改回来)
							config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);// 明文传输
							config.setDebuggerEnabled(true);// 开启调试模式,方便我们查看具体发送的内容

							// 2.开始创建连接对象
							XMPPConnection conn = new XMPPConnection(config);

							// 开始连接
							conn.connect();

							// 连接成功了
							// 3.开始登录
							conn.login(userName, passWord);
							// 已经成功成功
							ToastUtils.showToastSafe(LoginActivity.this, "登录成功");

							finish();
							// 跳到主界面
							Intent intent = new Intent(LoginActivity.this, MainActivity.class);
							startActivity(intent);

							// 需要保存连接对象
							IMService.conn = conn;

							// 保存当前登录的账户
							String account = userName + "@" + LoginActivity.SERVICENAME;
							IMService.mCurAccout = account;// admin--->admin@itheima.com

							// 启动IMService
							Intent service = new Intent(LoginActivity.this, IMService.class);
							startService(service);
							// 启动PushService
							Intent pushService = new Intent(LoginActivity.this, PushService.class);
							startService(pushService);

						} catch (XMPPException e) {
							e.printStackTrace();
							ToastUtils.showToastSafe(LoginActivity.this, "登录失败");
						}
					}
				});
			}
		});
	}

	private void initView() {
		mEtUserName = (TextView) findViewById(R.id.et_username);
		mEtPassWord = (TextView) findViewById(R.id.et_password);

		mBtnLogin = (Button) findViewById(R.id.btn_login);
	}
}
