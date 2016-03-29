package com.cenzr.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.cenzr.dbhelper.SmsOpenHelper;

/*
 * @创建者     Administrator
 * @创建时间   2015/8/8 09:43
 * @描述	      ${TODO}
 *
 * @更新者     $Author$
 * @更新时间   $Date$
 * @更新描述   ${TODO}
 */
public class SmsProvider extends ContentProvider {
	private static final String	AUTHORITIES	= SmsProvider.class.getCanonicalName();

	static UriMatcher			mUriMatcher;

	public static Uri			URI_SESSION	= Uri.parse("content://" + AUTHORITIES + "/session");
	public static Uri			URI_SMS		= Uri.parse("content://" + AUTHORITIES + "/sms");

	public static final int		SMS			= 1;
	public static final int		SESSION		= 2;

	static {
		mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		// 添加匹配规则
		mUriMatcher.addURI(AUTHORITIES, "sms", SMS);
		mUriMatcher.addURI(AUTHORITIES, "session", SESSION);
	}

	private SmsOpenHelper		mHelper;

	@Override
	public boolean onCreate() {
		mHelper = new SmsOpenHelper(getContext());
		if (mHelper != null) {
			return true;
		}
		return false;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	/*=============== CRUD begin ===============*/
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		switch (mUriMatcher.match(uri)) {
		case SMS:
			// 插入之后对于的id
			long id = mHelper.getWritableDatabase().insert(SmsOpenHelper.T_SMS, "", values);
			if (id > 0) {
				System.out.println("--------------SmsProvider insertSuccess--------------");
				uri = ContentUris.withAppendedId(uri, id);
				// 发送数据改变的信号
				getContext().getContentResolver().notifyChange(SmsProvider.URI_SMS, null);
			}
			break;

		default:
			break;
		}
		return uri;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int deleteCount = 0;
		switch (mUriMatcher.match(uri)) {
		case SMS:
			// 具体删除了几条数据
			deleteCount = mHelper.getWritableDatabase().delete(SmsOpenHelper.T_SMS, selection, selectionArgs);
			if (deleteCount > 0) {
				System.out.println("--------------SmsProvider deleteSuccess--------------");
				// 发送数据改变的信号
				getContext().getContentResolver().notifyChange(SmsProvider.URI_SMS, null);
			}
			break;

		default:
			break;
		}
		return deleteCount;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int updateCount = 0;
		switch (mUriMatcher.match(uri)) {
		case SMS:
			// 更新了几条数据
			updateCount = mHelper.getWritableDatabase().update(SmsOpenHelper.T_SMS, values, selection, selectionArgs);
			if (updateCount > 0) {
				System.out.println("--------------SmsProvider updateSuccess--------------");
				// 发送数据改变的信号
				getContext().getContentResolver().notifyChange(SmsProvider.URI_SMS, null);
			}
			break;

		default:
			break;
		}
		return updateCount;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		Cursor cursor = null;
		switch (mUriMatcher.match(uri)) {
		case SMS:
			cursor =
					mHelper.getReadableDatabase().query(SmsOpenHelper.T_SMS, projection, selection, selectionArgs,
							null, null, sortOrder);
			System.out.println("--------------SmsProvider querySuccess--------------");
			break;
		case SESSION:
			SQLiteDatabase db = mHelper.getReadableDatabase();
			cursor = db.rawQuery("SELECT * FROM "//
					+ "(SELECT * FROM t_sms WHERE from_account = ? or to_account = ? ORDER BY time ASC)" //
					+ " GROUP BY session_account", selectionArgs);//
		default:
			break;
		}
		return cursor;
	}
	/*=============== CRUD begin ===============*/
}
