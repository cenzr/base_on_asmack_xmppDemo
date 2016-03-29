package com.cenzr.fragment;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.cenzr.R;
import com.cenzr.activity.ChatActivity;
import com.cenzr.dbhelper.ContactOpenHelper;
import com.cenzr.provider.ContactsProvider;
import com.cenzr.utils.ThreadUtils;

/*
 * @创建者     Administrator
 * @创建时间   2015/8/7 11:08
 * @描述	      联系人的Fragment
 *
 * @更新者     $Author$
 * @更新时间   $Date$
 * @更新描述   ${TODO}$
 */
public class ContactsFragment extends Fragment {

	private ListView		mListView;
	private CursorAdapter	mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		init();
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_contacts, container, false);
		initView(view);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		initData();
		initListener();
		super.onActivityCreated(savedInstanceState);
	}

	private void init() {
		registerContentObserver();
	}

	private void initView(View view) {
		mListView = (ListView) view.findViewById(R.id.listView);
	}

	private void initListener() {
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				Cursor c = mAdapter.getCursor();
				c.moveToPosition(position);

				// 拿到jid(账号)-->发送消息的时候需要
				String account = c.getString(c.getColumnIndex(ContactOpenHelper.ContactTable.ACCOUNT));
				// 拿到nickName-->显示效果
				String nickname = c.getString(c.getColumnIndex(ContactOpenHelper.ContactTable.NICKNAME));

				Intent intent = new Intent(getActivity(), ChatActivity.class);

				intent.putExtra(ChatActivity.CLICKACCOUNT, account);
				intent.putExtra(ChatActivity.CLICKNICKNAME, nickname);

				startActivity(intent);
			}
		});
	}

	private void initData() {

		setOrNotifyAdapter();

		/**
		 * JID=[ node”@” ] domain [ “/” resource ]
		 我是hm4: hm4@itheima.com [Friends]  	hm4@itheima.com  我是hm4  subscribe  none
		 hm2: hm2@itheima.com [Friends]  	 	hm2@itheima.com  hm2  null  both
		 hm1: hm1@itheima.com [Friends]  	 	hm1@itheima.com  hm1  null  both
		 我是苍老师: hm3@itheima.com [Friends]	hm3@itheima.com  我是苍老师  subscribe  none
		 */
	}

	/**
	 * 设置或者更新adapter
	 */
	private void setOrNotifyAdapter() {
		// 判断adapter是否存在
		if (mAdapter != null) {
			// 刷新adapter就行了
			mAdapter.getCursor().requery();
			return;
		}
		ThreadUtils.runInThread(new Runnable() {
			@Override
			public void run() {
				// 对应查询记录
				final Cursor c =
						getActivity().getContentResolver().query(ContactsProvider.URI_CONTACT, null, null, null, null);

				// 假如没有数据的时候
				if (c.getCount() <= 0) {
					return;
				}
				// 设置adapter,然后显示数据
				ThreadUtils.runInUIThread(new Runnable() {
					@Override
					public void run() {
						/**
						 BaseAdapter    -->getView
						 |-CursorAdapter
						 */
						mAdapter = new CursorAdapter(getActivity(), c) {
							// 如果convertView==null,返回一个具体的根视图
							@Override
							public View newView(Context context, Cursor cursor, ViewGroup parent) {
								View view = View.inflate(context, R.layout.item_contact, null);
								return view;
							}

							// 设置数据显示数据
							@Override
							public void bindView(View view, Context context, Cursor cursor) {
								ImageView ivHead = (ImageView) view.findViewById(R.id.head);
								TextView tvAccount = (TextView) view.findViewById(R.id.account);
								TextView tvNickName = (TextView) view.findViewById(R.id.nickname);

								String acccount =
										cursor.getString(c.getColumnIndex(ContactOpenHelper.ContactTable.ACCOUNT));

								String nickName =
										cursor.getString(c.getColumnIndex(ContactOpenHelper.ContactTable.NICKNAME));

								tvAccount.setText(acccount);
								tvNickName.setText(nickName);
							}
						};

						mListView.setAdapter(mAdapter);
					}
				});
			}
		});
	}

	@Override
	public void onDestroy() {
		// 按照常理,我们Fragment销毁了.那么我们就不应该去继续去监听
		// 但是,实际,我们是需要一直监听对应roster的改变
		// 所以,我们把联系人的监听和同步操作放到Service去
		unRegisterContentObserver();
		super.onDestroy();
	}

	/*=============== 监听数据库记录的改变 ===============*/

	MyContentObserver	mMyContentObserver	= new MyContentObserver(new Handler());

	/**注册监听*/
	public void registerContentObserver() {
		// content://xxxx/contact
		// content://xxxx/contact/i
		getActivity().getContentResolver().registerContentObserver(ContactsProvider.URI_CONTACT, true,
				mMyContentObserver);
	}

	public void unRegisterContentObserver() {
		getActivity().getContentResolver().unregisterContentObserver(mMyContentObserver);
	}

	/**反注册监听*/

	class MyContentObserver extends ContentObserver {

		public MyContentObserver(Handler handler) {
			super(handler);
		}

		/**
		 * 如果数据库数据改变会在这个方法收到通知
		 */
		@Override
		public void onChange(boolean selfChange, Uri uri) {
			super.onChange(selfChange, uri);
			// 更新adapter或者刷新adapter
			setOrNotifyAdapter();
		}
	}
}
