package com.cenzr.service;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.cenzr.activity.LoginActivity;
import com.cenzr.dbhelper.ContactOpenHelper;
import com.cenzr.dbhelper.SmsOpenHelper;
import com.cenzr.provider.ContactsProvider;
import com.cenzr.provider.SmsProvider;
import com.cenzr.utils.PinyinUtil;
import com.cenzr.utils.ThreadUtils;
import com.cenzr.utils.ToastUtils;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/*
 * @创建者     Administrator
 * @创建时间   2015/8/7 14:16
 * @描述	      ${TODO}
 *
 * @更新者     $Author$
 * @更新时间   $Date$
 * @更新描述   ${TODO}
 */
public class IMService extends Service {
	public static XMPPConnection	conn;
	public static String			mCurAccout;					// 当前登录用户的jid

	private Roster					mRoster;
	private MyRosterListener		mRosterListener;

	private ChatManager				mChatManager;
	private Chat					mCurChat;

	private Map<String, Chat>		mChatMap	= new HashMap<String, Chat>();

	@Override
	public IBinder onBind(Intent intent) {
		return new MyBinder();
	}

	public class MyBinder extends Binder {
		/**
		 * 返回service的实例
		 */
		public IMService getService() {
			return IMService.this;
		}
	}

	@Override
	public void onCreate() {
		System.out.println("--------------service onCreate--------------");
		ThreadUtils.runInThread(new Runnable() {
			@Override
			public void run() {
				/*=============== 同步花名册 begin ===============*/
				System.out.println("--------------同步花名册 begin--------------");
				// 需要连接对象
				// 得到花名册对象（获取联系人）
				mRoster = IMService.conn.getRoster();

				// 得到所有的联系人
				final Collection<RosterEntry> entries = mRoster.getEntries();

				// 监听联系人的改变
				mRosterListener = new MyRosterListener();
				mRoster.addRosterListener(mRosterListener);

				for (RosterEntry entry : entries) {
					saveOrUpdateEntry(entry);
				}
				System.out.println("--------------同步花名册 end--------------");
				/*=============== 同步花名册 end ===============*/

				/*=============== 创建消息的管理者  注册监听 begin ===============*/

				// 1.获取消息的管理者
				if (mChatManager == null) {
					mChatManager = IMService.conn.getChatManager();
				}
				mChatManager.addChatListener(mMyChatManagerListener);
				/*=============== 创建消息的管理者  注册监听 end ===============*/

			}
		});

		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		System.out.println("--------------service onStartCommand--------------");
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		System.out.println("--------------service onDestroy--------------");
		// 移除rosterListener
		if (mRoster != null && mRosterListener != null) {
			mRoster.removeRosterListener(mRosterListener);
		}

		// 移除messageListener
		if (mCurChat != null && mMyMessageListener != null) {
			mCurChat.removeMessageListener(mMyMessageListener);
		}
		super.onDestroy();
	}

	class MyRosterListener implements RosterListener {

		@Override
		public void entriesAdded(Collection<String> addresses) {// 联系人添加了
			System.out.println("--------------entriesAdded--------------");
			// 对应更新数据库
			for (String address : addresses) {
				RosterEntry entry = mRoster.getEntry(address);
				// 要么更新,要么插入
				saveOrUpdateEntry(entry);
			}
		}

		@Override
		public void entriesUpdated(Collection<String> addresses) {// 联系人修改了
			System.out.println("--------------entriesUpdated--------------");
			// 对应更新数据库
			for (String address : addresses) {
				RosterEntry entry = mRoster.getEntry(address);
				// 要么更新,要么插入
				saveOrUpdateEntry(entry);
			}
		}

		@Override
		public void entriesDeleted(Collection<String> addresses) {// 联系人删除了
			System.out.println("--------------entriesDeleted--------------");
			// 对应更新数据库
			for (String account : addresses) {
				// 执行删除操作
				getContentResolver().delete(ContactsProvider.URI_CONTACT,
						ContactOpenHelper.ContactTable.ACCOUNT + "=?", new String[] { account });
			}

		}

		@Override
		public void presenceChanged(Presence presence) {// 联系人状态改变
			System.out.println("--------------presenceChanged--------------");
		}
	}

	MyMessageListener	mMyMessageListener	= new MyMessageListener();

	class MyMessageListener implements MessageListener {

		@Override
		public void processMessage(Chat chat, Message message) {
			String body = message.getBody();
			/**
			 from: hm1@itheima.com/Spark 2.6.3
			 to: admin@itheima.com/Smack
			 */
			ToastUtils.showToastSafe(getApplicationContext(), body);
			System.out.println("body: " + message.getBody());
			System.out.println("type: " + message.getType());
			System.out.println("from: " + message.getFrom());
			System.out.println("to: " + message.getTo());

			// 收到消息,保存消息
			String participant = chat.getParticipant();
			System.out.println("participant: " + participant);
			saveMessage(participant, message);
		}
	}

	MyChatManagerListener	mMyChatManagerListener	= new MyChatManagerListener();

	class MyChatManagerListener implements ChatManagerListener {
		@Override
		public void chatCreated(Chat chat, boolean createdLocally) {
			System.out.println("--------------chatCreated--------------");

			// 判断chat是否存在map里面
			String participant = chat.getParticipant();// 和我聊天的那个人

			// 因为别人创建和我自己创建,参与者(和我聊天的人)对应的jid不同.所以需要统一处理
			participant = filterAccount(participant);

			if (!mChatMap.containsKey(participant)) {
				// 保存chat
				mChatMap.put(participant, chat);
				chat.addMessageListener(mMyMessageListener);
			}
			System.out.println("participant:" + participant);

			if (createdLocally) {// true
				System.out.println("-------------- 我创建了一个chat--------------");
				// participant:hm1@itheima.com admin@itheima.com hm1@itheima.com
			} else {// false
				System.out.println("-------------- 别人创建了一个chat--------------");
				// participant:hm1@itheima.com/Spark 2.6.3 admin@itheima.com hm1@itheima.com
			}
		}
	}

	private void saveOrUpdateEntry(RosterEntry entry) {
		ContentValues values = new ContentValues();
		String account = entry.getUser();

		// account = account.substring(0, account.indexOf("@")) + "@" + LoginActivity.SERVICENAME;

		// 处理昵称
		String nickname = entry.getName();
		if (nickname == null || "".equals(nickname)) {
			nickname = account.substring(0, account.indexOf("@"));// billy@itheima.com-->billy
		}

		values.put(ContactOpenHelper.ContactTable.ACCOUNT, account);
		values.put(ContactOpenHelper.ContactTable.NICKNAME, nickname);
		values.put(ContactOpenHelper.ContactTable.AVATAR, "0");
		values.put(ContactOpenHelper.ContactTable.PINYIN, PinyinUtil.getPinyin(account));

		// 先update,后插入-->重点
		int updateCount =
				getContentResolver().update(ContactsProvider.URI_CONTACT, values,
						ContactOpenHelper.ContactTable.ACCOUNT + "=?", new String[] { account });
		if (updateCount <= 0) {// 没有更新到任何记录
			getContentResolver().insert(ContactsProvider.URI_CONTACT, values);
		}
	}

	/**
	 * 发送消息
	 */
	public void sendMessage(final Message msg) {
		try {
			// 2.创建聊天对象
			// 我-->美女(jid)
			// chatManager.createChat("被发送对象jid",消息的监听者);
			// 判断chat对是否已经创建
			String toAccount = msg.getTo();

			if (mChatMap.containsKey(toAccount)) {
				mCurChat = mChatMap.get(toAccount);
			} else {
				mCurChat = mChatManager.createChat(toAccount, mMyMessageListener);
				mChatMap.put(toAccount, mCurChat);
			}
			// 发送消息
			mCurChat.sendMessage(msg);
			// 保存消息
			saveMessage(msg.getTo(), msg);
		} catch (XMPPException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 保存消息-->contentResolver-->contentProvider-->sqlite
	 *
	 * @param msg
	 */
	private void saveMessage(String sessionAccount, Message msg) {
		ContentValues values = new ContentValues();
		// 我(from)--->小蜜(to) ===>小蜜
		// 小蜜(from)--->我(to)====>小蜜
		sessionAccount = filterAccount(sessionAccount);
		String from = msg.getFrom();
		from = filterAccount(from);
		String to = msg.getTo();
		to = filterAccount(to);


		values.put(SmsOpenHelper.SmsTable.FROM_ACCOUNT, from);
		values.put(SmsOpenHelper.SmsTable.TO_ACCOUNT, to);
		values.put(SmsOpenHelper.SmsTable.BODY, msg.getBody());
		values.put(SmsOpenHelper.SmsTable.STATUS, "offline");
		values.put(SmsOpenHelper.SmsTable.TYPE, msg.getType().name());
		values.put(SmsOpenHelper.SmsTable.TIME, System.currentTimeMillis());
		values.put(SmsOpenHelper.SmsTable.SESSION_ACCOUNT, sessionAccount);
		getContentResolver().insert(SmsProvider.URI_SMS, values);
	}

	private String filterAccount(String accout) {
		return accout.substring(0, accout.indexOf("@")) + "@" + LoginActivity.SERVICENAME;
	}
}
