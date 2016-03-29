package com.cenzr.test;

import opensource.jpinyin.PinyinFormat;
import opensource.jpinyin.PinyinHelper;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import android.test.InstrumentationTestCase;

import com.cenzr.dbhelper.ContactOpenHelper;
import com.cenzr.provider.ContactsProvider;

public class TestContactsProvider extends AndroidTestCase{

	
	public void test() {
		
		ContactOpenHelper contactOpenHelper=new ContactOpenHelper(getContext());
		SQLiteDatabase writableDatabase = contactOpenHelper.getWritableDatabase();
		if(writableDatabase==null)
			System.out.println("111111111111111111111111");
		System.out.println("222222222222222222222222");
		
	}
	
	
	public void testInsert() {
		/**
		 public static final String ACCOUNT = "account";//账号
		 public static final String NICKNAME = "nickname";//昵称
		 public static final String AVATAR = "avatar";//头像
		 public static final String PINYIN = "pinyin";//账号拼音
		 */
		ContentValues values = new ContentValues();
		values.put(ContactOpenHelper.ContactTable.ACCOUNT, "billy@itheima.com");
		values.put(ContactOpenHelper.ContactTable.NICKNAME, "老伍");
		values.put(ContactOpenHelper.ContactTable.AVATAR, "0");
		values.put(ContactOpenHelper.ContactTable.PINYIN, "laowu");
		System.out.println("-----------------------"+ContactsProvider.URI_CONTACT);
		getContext().getContentResolver().insert(ContactsProvider.URI_CONTACT, values);
	}

	public void testDelete() {
		getContext().getContentResolver().delete(ContactsProvider.URI_CONTACT,
				ContactOpenHelper.ContactTable.ACCOUNT + "=?", new String[] { "billy@itheima.com" });
	}

	public void testUpdate() {
		ContentValues values = new ContentValues();
		values.put(ContactOpenHelper.ContactTable.ACCOUNT, "billy@itheima.com");
		values.put(ContactOpenHelper.ContactTable.NICKNAME, "我是老伍");
		values.put(ContactOpenHelper.ContactTable.AVATAR, "0");
		values.put(ContactOpenHelper.ContactTable.PINYIN, "woshilaowu");
		getContext().getContentResolver().update(ContactsProvider.URI_CONTACT, values,
				ContactOpenHelper.ContactTable.ACCOUNT + "=?", new String[] { "billy@itheima.com" });
	}

	public void testQuery() {
		Cursor c = getContext().getContentResolver().query(ContactsProvider.URI_CONTACT, null, null, null, null);
		int columnCount = c.getColumnCount();// 一共多少列
		while (c.moveToNext()) {
			// 循环打印列
			for (int i = 0; i < columnCount; i++) {
				System.out.print(c.getString(i) + "    ");
			}
			System.out.println("");
		}
	}

	public void testPinyin() {
		// String pinyinString = PinyinHelper.convertToPinyinString("内容", "分隔符", 拼音的格式);
		String pinyinString = PinyinHelper.convertToPinyinString("黑马程序员", "", PinyinFormat.WITHOUT_TONE);
		System.out.println(pinyinString);
	}
}
