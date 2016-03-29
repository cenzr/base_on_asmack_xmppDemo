package com.cenzr.utils;

import opensource.jpinyin.PinyinFormat;
import opensource.jpinyin.PinyinHelper;

/*
 * @创建者     Administrator
 * @创建时间   2015/8/7 15:41
 * @描述	      ${TODO}
 *
 * @更新者     $Author$
 * @更新时间   $Date$
 * @更新描述   ${TODO}
 */
public class PinyinUtil {
	public static String getPinyin(String str) {
		return PinyinHelper.convertToPinyinString(str, "", PinyinFormat.WITHOUT_TONE);
	}
}
