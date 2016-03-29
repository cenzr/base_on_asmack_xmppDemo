package com.cenzr.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.cenzr.utils.ToastUtils;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

/*
 * @创建者     Administrator
 * @创建时间   2015/8/8 16:34
 * @描述	      ${TODO}
 *
 * @更新者     $Author$
 * @更新时间   $Date$
 * @更新描述   ${TODO}
 */
public class PushService extends Service {
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		System.out.println("--------------PushService- onCreate-------------");

		IMService.conn.addPacketListener(new PacketListener() {
			@Override
			public void processPacket(Packet packet) {
				Message message = (Message) packet;
				String body = message.getBody();
				ToastUtils.showToastSafe(getApplicationContext(), body);
			}
		}, null);
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		System.out.println("--------------PushService- onDestroy-------------");
		super.onDestroy();
	}
}
