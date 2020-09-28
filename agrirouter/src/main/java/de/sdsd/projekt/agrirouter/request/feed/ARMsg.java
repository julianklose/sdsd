package de.sdsd.projekt.agrirouter.request.feed;

import static de.sdsd.projekt.agrirouter.ARConfig.USE_BASE64;

import java.util.Base64;
import java.util.regex.Pattern;

import com.google.protobuf.BytesValue;
import com.google.protobuf.InvalidProtocolBufferException;

import agrirouter.feed.push.notification.PushNotificationOuterClass.PushNotification;
import agrirouter.feed.response.FeedResponse.MessageQueryResponse;
import de.sdsd.projekt.agrirouter.ARException;

/**
 * Represents a complete file received from the agrirouter.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class ARMsg {
	
	/**
	 * The header of the message contains a lot of metadata.
	 */
	public final ARMsgHeader header;
	private final byte[][] content;
	
	/**
	 * Constructs a new message using the header. 
	 * You have to {@link #addMessage(FeedMessage) add the content} of the file later.
	 * 
	 * @param header agrirouter message header
	 */
	ARMsg(ARMsgHeader header) {
		this.header = header;
		this.content = new byte[header.getChunkCount()][];
	}
	
	private static final Pattern NEWLINE_REGEX = Pattern.compile("[\\r\\n ]");
	
	/**
	 * Add a part of the message content.
	 * 
	 * @param msg obtained agrirouter message
	 * @throws ARException if an empty message was received over the agrirouter
	 */
	void addMessage(MessageQueryResponse.FeedMessage msg) throws ARException {
		if(!msg.hasHeader() || msg.getHeader().getReceiverId().isEmpty())
			throw new ARException("Received empty message!");
		MessageQueryResponse.Header head = msg.getHeader();
		byte[] arr;
		
		if(head.getTechnicalMessageType().endsWith(":protobuf") || head.getTechnicalMessageType().equals("gps:info")) {
			arr = msg.getContent().getValue().toByteArray();
		}
		else {
			try {
				if(USE_BASE64) {
					String base64Content = msg.getContent().getValue().toStringUtf8();
					base64Content = NEWLINE_REGEX.matcher(base64Content).replaceAll("");
					arr = Base64.getDecoder().decode(base64Content);
				} else {
					arr = msg.getContent().unpack(BytesValue.class).getValue().toByteArray();
				}
			} catch (IllegalArgumentException | InvalidProtocolBufferException e) {
				String msgcontent = msg.getContent().getValue().toStringUtf8();
				throw new ARException("Could not read message content: (" + msg.getContent().getTypeUrl() + "): " 
						+ msgcontent.substring(0, Math.min(msgcontent.length(), 100)) + "...");
			}
		}
		
		if(head.hasChunkContext() && !head.getChunkContext().getContextId().isEmpty()) 
			this.content[(int) (head.getChunkContext().getCurrent() - 1)] = arr;
		else
			this.content[0] = arr;
	}
	
	/**
	 * Add a part of the message content.
	 * 
	 * @param msg obtained agrirouter message
	 * @throws ARException if an empty message was received over the agrirouter
	 */
	void addMessage(PushNotification.FeedMessage msg) throws ARException {
		if(!msg.hasHeader() || msg.getHeader().getReceiverId().isEmpty())
			throw new ARException("Received empty message!");
		PushNotification.Header head = msg.getHeader();
		byte[] arr;
		
		if(head.getTechnicalMessageType().endsWith(":protobuf") || head.getTechnicalMessageType().equals("gps:info")) {
			arr = msg.getContent().getValue().toByteArray();
		}
		else {
			try {
				if(USE_BASE64) {
					String base64Content = msg.getContent().getValue().toStringUtf8();
					base64Content = NEWLINE_REGEX.matcher(base64Content).replaceAll("");
					arr = Base64.getDecoder().decode(base64Content);
				} else {
					arr = msg.getContent().unpack(BytesValue.class).getValue().toByteArray();
				}
			} catch (IllegalArgumentException | InvalidProtocolBufferException e) {
				String msgcontent = msg.getContent().getValue().toStringUtf8();
				throw new ARException("Could not read message content: (" + msg.getContent().getTypeUrl() + "): " 
						+ msgcontent.substring(0, Math.min(msgcontent.length(), 100)) + "...");
			}
		}
		
		if(head.hasChunkContext() && !head.getChunkContext().getContextId().isEmpty()) 
			this.content[(int) (head.getChunkContext().getCurrent() - 1)] = arr;
		else
			this.content[0] = arr;
	}
	
	/**
	 * The header of the message contains a lot of metadata.
	 * @return message header
	 */
	public ARMsgHeader getHeader() {
		return header;
	}
	
	/**
	 * Returns whether all content for this message was received.
	 * @return whether content is complete
	 */
	public boolean isComplete() {
		for (int i = 0; i < content.length; ++i) {
			if(content[i] == null) return false;
		}
		return true;
	}
	
	/**
	 * Calculate the current content length of all added chunks. 
	 * For a complete message this should equal the result of {@link ARMsgHeader#getPayloadSize()}.
	 * @return current calculated content size
	 */
	public int getPayloadSize() {
		int size = 0;
		for (int i = 0; i < content.length; ++i) {
			if(content[i] != null) size += content[i].length;
		}
		return size;
	}
	
	/**
	 * Get the file content.
	 * @return aggregated file content
	 */
	public byte[] getContent() {
		byte[] arr = new byte[getPayloadSize()];
		int j = 0;
		for (int i = 0; i < content.length; ++i) {
			System.arraycopy(content[i], 0, arr, j, content[i].length);
			j += content[i].length;
		}
		return arr;
	}
	
	@Override
	public String toString() {
		return "Message of " + header.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((header == null) ? 0 : header.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ARMsg other = (ARMsg) obj;
		if (header == null) {
			if (other.header != null)
				return false;
		} else if (!header.equals(other.header))
			return false;
		return true;
	}
}
