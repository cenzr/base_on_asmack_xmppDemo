package com.cenzr.activity;

import java.sql.Date;
import java.text.SimpleDateFormat;

import org.jivesoftware.smack.packet.Message;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.cenzr.R;
import com.cenzr.dbhelper.SmsOpenHelper;
import com.cenzr.provider.SmsProvider;
import com.cenzr.service.IMService;
import com.cenzr.utils.ThreadUtils;


public class ChatActivity extends ActionBarActivity {

	public static final String	CLICKACCOUNT	= "clickAccount";
	public static final String	CLICKNICKNAME	= "clickNickName";

	TextView					mTitle;
	ListView					mListView;
	EditText					mEtBody;
	Button						mBtnSend;

	private String				mClickAccount;
	private String				mClickNickName;
	private CursorAdapter		mAdapter;

	private IMService			mImService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		
		mTitle=(TextView) findViewById(R.id.title);
		mListView=(ListView) findViewById(R.id.listView);
		mEtBody=(EditText) findViewById(R.id.et_body);
		mBtnSend=(Button) findViewById(R.id.btn_send);
		
		
		init();
		initView();
		initData();
		initListener();
	}

	private void init() {
		registerContentObserver();
		// 绑定服务
		Intent service = new Intent(ChatActivity.this, IMService.class);
		bindService(service, mMyServiceConnection, BIND_AUTO_CREATE);

		mClickAccount = getIntent().getStringExtra(ChatActivity.CLICKACCOUNT);
		mClickNickName = getIntent().getStringExtra(ChatActivity.CLICKNICKNAME);
	}

	private void initView() {
		// 设置title
		mTitle.setText("与 " + mClickNickName + " 聊天中");

	}

	private void initData() {

		setAdapterOrNotify();

	}

	private void setAdapterOrNotify() {
		// 1.首先判断是否存在adapter
		if (mAdapter != null) {
			// 刷新
			Cursor cursor = mAdapter.getCursor();
			cursor.requery();

			mListView.setSelection(cursor.getCount() - 1);
			return;
		}

		ThreadUtils.runInThread(new Runnable() {
			@Override
			public void run() {
				final Cursor c = getContentResolver().query(SmsProvider.URI_SMS,//
						null,//
						"(from_account = ? and to_account=?)or(from_account = ? and to_account= ? )",// where条件
						new String[] { IMService.mCurAccout, mClickAccount, mClickAccount, IMService.mCurAccout },// where条件的参数
						SmsOpenHelper.SmsTable.TIME + " ASC"// 根据时间升序排序
				);
				// 如果没有数据直接返回
				if (c.getCount() < 1) {
					return;
				}

				// desc
				ThreadUtils.runInUIThread(new Runnable() {
					@Override
					public void run() {
						// CursorAdapter :getview-->newView()-->bindView
						mAdapter = new CursorAdapter(ChatActivity.this, c) {

							public static final int	RECEIVE	= 1;
							public static final int	SEND	= 0;

							// 如果convertView == null的时候会调用-->返回根布局
							/*@Override
							public View newView(Context context, Cursor cursor, ViewGroup parent) {
							    TextView tv = new TextView(context);
							    return tv;
							}

							@Override
							public void bindView(View view, Context context, Cursor cursor) {
							    TextView tv = (TextView) view;

							    // 具体设置数据
							    String body = cursor.getString(cursor.getColumnIndex(SmsOpenHelper.SmsTable.BODY));
							    tv.setText(body);
							}*/

							@Override
							public int getItemViewType(int position) {
								c.moveToPosition(position);
								// 取出消息的创建者
								String fromAccount = c.getString(c.getColumnIndex(SmsOpenHelper.SmsTable.FROM_ACCOUNT));
								if (!IMService.mCurAccout.equals(fromAccount)) {// 接收
									return RECEIVE;
								} else {// 发送
									return SEND;
								}
								// return super.getItemViewType(position);// 0 1
								// 接收--->如果当前的账号 不等于 消息的创建者
								// 发送
							}

							@Override
							public int getViewTypeCount() {
								return super.getViewTypeCount() + 1;// 2
							}

							@Override
							public View getView(int position, View convertView, ViewGroup parent) {
								ViewHolder holder;
								if (getItemViewType(position) == RECEIVE) {
									if (convertView == null) {
										convertView = View.inflate(ChatActivity.this, R.layout.item_chat_receive, null);
										holder = new ViewHolder();
										convertView.setTag(holder);

										// holder赋值
										holder.time = (TextView) convertView.findViewById(R.id.time);
										holder.body = (TextView) convertView.findViewById(R.id.content);
										holder.head = (ImageView) convertView.findViewById(R.id.head);
									} else {
										holder = (ViewHolder) convertView.getTag();
									}

									// 得到数据,展示数据
								} else {// 发送
									if (convertView == null) {
										convertView = View.inflate(ChatActivity.this, R.layout.item_chat_send, null);
										holder = new ViewHolder();
										convertView.setTag(holder);

										// holder赋值
										holder.time = (TextView) convertView.findViewById(R.id.time);
										holder.body = (TextView) convertView.findViewById(R.id.content);
										holder.head = (ImageView) convertView.findViewById(R.id.head);
									} else {
										holder = (ViewHolder) convertView.getTag();
									}
									// 得到数据,展示数据

								}
								// 得到数据,展示数据
								c.moveToPosition(position);

								String time = c.getString(c.getColumnIndex(SmsOpenHelper.SmsTable.TIME));
								String body = c.getString(c.getColumnIndex(SmsOpenHelper.SmsTable.BODY));

								String formatTime =
										new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(Long
												.parseLong(time)));

								holder.time.setText(formatTime);
								holder.body.setText(body);

								return super.getView(position, convertView, parent);
							}

							@Override
							public View newView(Context context, Cursor cursor, ViewGroup parent) {
								return null;
							}

							@Override
							public void bindView(View view, Context context, Cursor cursor) {

							}

							class ViewHolder {
								TextView	body;
								TextView	time;
								ImageView	head;

							}
						};

						mListView.setAdapter(mAdapter);
						// 滚动到最后一行
						mListView.setSelection(mAdapter.getCount() - 1);
					}
				});

			}
		});
	}

	private void initListener() {

	}

	
	public void send(View v) {
		ThreadUtils.runInThread(new Runnable() {
			@Override
			public void run() {
				final String body = mEtBody.getText().toString();
				// 3.初始化了一个消息
				Message msg = new Message();
				msg.setFrom(IMService.mCurAccout);// 当前登录的用户
				msg.setTo(mClickAccount);
				msg.setBody(body);// 输入框里面的内容
				msg.setType(Message.Type.chat);// 类型就是chat
				msg.setProperty("key", "value");// 额外属性-->额外的信息,这里我们用不到

				// TODO 调用服务器里面的sendMessage这个方法
				mImService.sendMessage(msg);

				IMService service = new IMService();
				// 4.清空输入框
				ThreadUtils.runInUIThread(new Runnable() {
					@Override
					public void run() {
						mEtBody.setText("");
					}
				});
			}
		});

	}

	@Override
	protected void onDestroy() {
		unRegisterContentObserver();

		// 解绑服务
		if (mMyServiceConnection != null) {
			unbindService(mMyServiceConnection);
		}
		super.onDestroy();
	}

	/*=============== 使用contentObserver时刻监听记录的改变 ===============*/
	MyContentObserver	mMyContentObserver	= new MyContentObserver(new Handler());

	/**
	 * 注册监听
	 */
	public void registerContentObserver() {
		getContentResolver().registerContentObserver(SmsProvider.URI_SMS, true, mMyContentObserver);
	}

	/**
	 * 反注册监听
	 */
	public void unRegisterContentObserver() {
		getContentResolver().unregisterContentObserver(mMyContentObserver);
	}

	class MyContentObserver extends ContentObserver {

		public MyContentObserver(Handler handler) {
			super(handler);
		}

		/**
		 * 接收到数据记录的改变
		 */
		@Override
		public void onChange(boolean selfChange, Uri uri) {
			// 设置adapter或者notifyadapter
			setAdapterOrNotify();
			super.onChange(selfChange, uri);
		}
	}

	/*=============== 定义ServiceConnection调用服务里面的方法 ===============*/

	MyServiceConnection	mMyServiceConnection	= new MyServiceConnection();

	class MyServiceConnection implements ServiceConnection {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			System.out.println("--------------onServiceConnected--------------");
			IMService.MyBinder binder = (IMService.MyBinder) service;
			mImService = binder.getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			System.out.println("--------------onServiceDisconnected--------------");

		}
	}
}
