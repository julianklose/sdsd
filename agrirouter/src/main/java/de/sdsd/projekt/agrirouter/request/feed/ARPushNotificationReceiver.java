package de.sdsd.projekt.agrirouter.request.feed;

import static de.sdsd.projekt.agrirouter.ARConfig.DEBUG_MODE;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import agrirouter.feed.push.notification.PushNotificationOuterClass.PushNotification;
import agrirouter.response.Response.ResponseEnvelope;
import de.sdsd.projekt.agrirouter.ARConnection;
import de.sdsd.projekt.agrirouter.request.ARCapabilities;

/**
 * Agrirouter Push Notification Receiver.
 * Create and set this to the {@link ARConnection#setPushNotificationReceiver(ARPushNotificationReceiver) ARConnection} 
 * to receive push notifications. Make sure push notifications were enabled in 
 * {@link ARCapabilities#enablePushNotifications(boolean) the capability declaration}.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 * @see ARCapabilities#enablePushNotifications(boolean)
 * @see ARConnection#setPushNotificationReceiver(ARPushNotificationReceiver)
 */
public abstract class ARPushNotificationReceiver {
	private ConcurrentHashMap<String, ARMsg> chunks = new ConcurrentHashMap<>();
	
	public final ARConfirmMessage readResponse(ResponseEnvelope header, Any params) {
		ARConfirmMessage confirm = new ARConfirmMessage();
		try {
			PushNotification resp = params.unpack(PushNotification.class);
			if(DEBUG_MODE) {
				PushNotification.Builder builder = resp.toBuilder();
				Any content = Any.newBuilder().setValue(ByteString.copyFrom("omitted", StandardCharsets.UTF_8)).build();
				builder.getMessagesBuilderList().forEach(feedmsg -> feedmsg.setContent(content));
				System.out.println(builder.toString());
			}
			
			for(PushNotification.FeedMessage feedmsg : resp.getMessagesList()) {
				try {
					if(feedmsg.getHeader().hasChunkContext() && !feedmsg.getHeader().getChunkContext().getContextId().isEmpty()) {
						ARMsg arMsg = chunks.get(feedmsg.getHeader().getChunkContext().getContextId());
						if(arMsg == null)
							chunks.put(feedmsg.getHeader().getChunkContext().getContextId(), arMsg = new ARMsg(new ARMsgHeader(feedmsg.getHeader())));
						arMsg.addMessage(feedmsg);
						
						if(arMsg.isComplete()) {
							chunks.remove(arMsg.getHeader().getChunkContextId());
							onReceive(arMsg);
						}
					} else {
						ARMsg arMsg = new ARMsg(new ARMsgHeader(feedmsg.getHeader()));
						arMsg.addMessage(feedmsg);
						
						if(arMsg.isComplete()) {
							onReceive(arMsg);
						}
					}
				} catch(Exception e) {
					onError(e);
				}
				confirm.addMessage(feedmsg.getHeader().getMessageId());
			}
		} catch (InvalidProtocolBufferException e) {
			onError(e);
		}
		return confirm;
	}
	
	public abstract void onReceive(ARMsg msg);
	public abstract void onError(Exception e);
}
