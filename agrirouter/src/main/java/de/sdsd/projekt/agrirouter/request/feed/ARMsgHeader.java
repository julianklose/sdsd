package de.sdsd.projekt.agrirouter.request.feed;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.CheckForNull;

import com.google.protobuf.Timestamp;

import agrirouter.commons.Chunk.ChunkComponent;
import agrirouter.commons.MessageOuterClass.Metadata;
import agrirouter.feed.push.notification.PushNotificationOuterClass.PushNotification;
import agrirouter.feed.response.FeedResponse.HeaderQueryResponse;
import de.sdsd.projekt.agrirouter.ARMessageType;

/**
 * Represents the header of a message in the agrirouter message box.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 * @see ARQueryMessageHeaders
 */
public class ARMsgHeader {
	
	/** The chunk context id. */
	private final String chunkContextId;
	
	/** The chunk length. */
	private final int chunkLength;
	
	/** The msgids. */
	private final HashSet<String> msgids;
	
	/** The receiver. */
	private String sender = null, receiver = null;
	
	/** The type. */
	private ARMessageType type = null;
	
	/** The team set context id. */
	private String teamSetContextId = null;
	
	/** The sent time. */
	private Instant sentTime = null;
	
	/** The content size. */
	private final int contentSize;
	
	/** The meta. */
	@CheckForNull
	private Metadata meta = null;
	
	/**
	 * Converts a protobuf timestamp to an java time instant.
	 * 
	 * @param ts protobuf timestamp
	 * @return java time instant
	 */
	public static Instant timestampToInstant(Timestamp ts) {
		return Instant.ofEpochMilli(TimeUnit.SECONDS.toMillis(ts.getSeconds()) 
				+ TimeUnit.NANOSECONDS.toMillis(ts.getNanos()));
	}
	
	/**
	 * Construct a not-chunked message header.
	 * 
	 * @param feed the feed this message belongs to
	 * @param head the protobuf message header
	 */
	ARMsgHeader(HeaderQueryResponse.Feed feed, HeaderQueryResponse.Header head) {
		this.chunkContextId = null;
		this.chunkLength = 1;
		this.msgids = new HashSet<>(1);
		this.msgids.add(head.getMessageId());
		this.sender = feed.getSenderId();
		this.receiver = feed.getReceiverId();
		this.type = ARMessageType.from(head.getTechnicalMessageType());
		this.teamSetContextId = head.getTeamSetContextId();
		this.sentTime = timestampToInstant(head.getSentTimestamp());
		this.contentSize = (int) head.getPayloadSize();
		this.meta = head.getMetadata();
	}
	
	/**
	 * Construct a message header.
	 * 
	 * @param head the push notification protobuf message header
	 */
	ARMsgHeader(PushNotification.Header head) {
		if(head.hasChunkContext() && head.getChunkContext().getTotal() > 0) {
			ChunkComponent chunk = head.getChunkContext();
			this.chunkContextId = chunk.getContextId();
			this.chunkLength = (int) chunk.getTotal();
			this.msgids = new HashSet<>(chunkLength);
			this.contentSize = (int) chunk.getTotalSize();
		} else {
			this.chunkContextId = null;
			this.chunkLength = 1;
			this.msgids = new HashSet<>(1);
			this.contentSize = (int) head.getPayloadSize();
		}
		this.msgids.add(head.getMessageId());
		this.sender = head.getSenderId();
		this.receiver = head.getReceiverId();
		this.type = ARMessageType.from(head.getTechnicalMessageType());
		this.teamSetContextId = head.getTeamSetContextId();
		this.sentTime = timestampToInstant(head.getSentTimestamp());
		this.meta = head.getMetadata();
	}
	
	/**
	 * Construct a cunked message header.
	 * 
	 * @param chunk chunk info of one part of the message
	 */
	ARMsgHeader(ChunkComponent chunk) {
		this.chunkContextId = chunk.getContextId();
		this.chunkLength = (int) chunk.getTotal();
		this.msgids = new HashSet<>(chunkLength);
		this.contentSize = (int) chunk.getTotalSize();
	}
	
	/**
	 * Add a part of a chunked message.
	 * 
	 * @param feed the feed this message belongs to
	 * @param head the protobuf message header
	 * @return this object for method chaining
	 */
	ARMsgHeader addChunk(HeaderQueryResponse.Feed feed, HeaderQueryResponse.Header head) {
		if(chunkContextId != null && chunkContextId.equals(head.getChunkContextId())) {
			this.msgids.add(head.getMessageId());
			this.sender = feed.getSenderId();
			this.receiver = feed.getReceiverId();
			this.type = ARMessageType.from(head.getTechnicalMessageType());
			this.teamSetContextId = head.getTeamSetContextId();
			this.sentTime = timestampToInstant(head.getSentTimestamp());
			this.meta = head.getMetadata();
		}
		return this;
	}
	
	/**
	 * Add a part of a chunked message.
	 * 
	 * @param head the push notification protobuf message header
	 * @return this object for method chaining
	 */
	ARMsgHeader addChunk(PushNotification.Header head) {
		if(chunkContextId != null && chunkContextId.equals(head.getChunkContext().getContextId())) {
			this.msgids.add(head.getMessageId());
			this.sender = head.getSenderId();
			this.receiver = head.getReceiverId();
			this.type = ARMessageType.from(head.getTechnicalMessageType());
			this.teamSetContextId = head.getTeamSetContextId();
			this.sentTime = timestampToInstant(head.getSentTimestamp());
			this.meta = head.getMetadata();
		}
		return this;
	}
	
	/**
	 * Return the chunk context id.
	 * This is used to identify all messages that belong to the same chunked file.
	 * @return chunk context id or null if not chunked
	 */
	String getChunkContextId() {
		return chunkContextId;
	}
	
	/**
	 * Return the message ids of the file.
	 * This could be more than one for chunked messages.
	 * @return unordered set of message ids
	 */
	public Set<String> getIds() {
		return Collections.unmodifiableSet(msgids);
	}
	
	/**
	 * Return the sender endpoint id.
	 * @return agrirouter endpoint id
	 */
	public String getSender() {
		return sender;
	}
	
	/**
	 * Return the receiver endpoint id.
	 * @return agrirouter endpoint id
	 */
	public String getReceiver() {
		return receiver;
	}
	
	/**
	 * Return the type of the message.
	 * @return agrirouter message type
	 */
	public ARMessageType getType() {
		return type;
	}
	
	/**
	 * Return the team context id of the message.
	 * This is meant to label connected feeds of messages, but it could also be used to transfer the file name.
	 * @return string that was sent along with the file
	 */
	public String getTeamSetContextId() {
		return teamSetContextId;
	}
	
	/**
	 * Return the total size of the containing file.
	 * @return file size in bytes
	 */
	public int getPayloadSize() {
		return contentSize;
	}
	
	/**
	 * Return the date time when the message was sent.
	 * @return java time instant
	 */
	public Instant getSentTime() {
		return sentTime;
	}
	
	/**
	 * Return the count of single agrirouter messages to retrieve whole the file.
	 * @return Count of single chunks.
	 */
	public int getChunkCount() {
		return chunkLength;
	}
	
	/**
	 * Check if all parts of the message are ready to be obtained.
	 * @return true if all parts are available, always true for not-chunked messages
	 */
	public boolean isComplete() {
		return msgids.size() == chunkLength;
	}
	
	/**
	 * Return the meta data sent along the message.
	 * @return the metadata of the message
	 */
	@CheckForNull
	public Metadata getMetadata() {
		return meta;
	}
	
//	@Override
//	public String toString() {
//		StringBuilder sb = new StringBuilder((isComplete() ? "Complete" : "Incomplete") + " message header");
//		sb.append("\n  IDs: ").append(String.join(", ", msgids));
//		if(chunkContextId != null) sb.append("\n  ChunkContextId: ").append(chunkContextId);
//		sb.append("\n  Sender: ").append(sender);
//		sb.append("\n  Receiver: ").append(receiver);
//		sb.append("\n  Type: ").append(type);
//		sb.append("\n  TeamSetContextId: ").append(teamSetContextId);
//		sb.append("\n  ContentSize (Bytes): ").append(contentSize);
//		sb.append("\n  SentTime: ").append(sentTime);
//		return sb.toString();
//	}

	/**
 * Hash code.
 *
 * @return the int
 */
@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		if(chunkContextId != null) return prime * result + chunkContextId.hashCode();
		for(String id: msgids) {
			result = prime * result + id.hashCode();
		}
		return result;
	}

	/**
	 * Equals.
	 *
	 * @param obj the obj
	 * @return true, if successful
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ARMsgHeader other = (ARMsgHeader) obj;
		if (chunkContextId == null) {
			if (other.chunkContextId != null)
				return false;
		} else if (!chunkContextId.equals(other.chunkContextId))
			return false;
		if (msgids.size() != other.msgids.size())
			return false;
		else if (!msgids.containsAll(other.msgids))
			return false;
		return true;
	}
	
	/**
	 * The Class ARMsgHeaderResult.
	 */
	public static class ARMsgHeaderResult extends ArrayList<ARMsgHeader> {
		
		/** The Constant serialVersionUID. */
		private static final long serialVersionUID = 8935412756454473233L;
		
		/** The total messages in query. */
		private int totalMessagesInQuery = 0;
		
		/** The pending message ids. */
		private final List<String> pendingMessageIds = new ArrayList<>();
		
		/**
		 * Gets the single message count.
		 *
		 * @return the single message count
		 */
		public int getSingleMessageCount() {
			return this.stream()
					.map(ARMsgHeader::getIds)
					.mapToInt(Set::size)
					.sum();
		}

		/**
		 * Gets the total messages in query.
		 *
		 * @return the total messages in query
		 */
		public int getTotalMessagesInQuery() {
			return totalMessagesInQuery;
		}

		/**
		 * Sets the total messages in query.
		 *
		 * @param totalMessagesInQuery the new total messages in query
		 */
		public void setTotalMessagesInQuery(int totalMessagesInQuery) {
			this.totalMessagesInQuery = totalMessagesInQuery;
		}

		/**
		 * Gets the pending message ids.
		 *
		 * @return the pending message ids
		 */
		public List<String> getPendingMessageIds() {
			return pendingMessageIds;
		}

		/**
		 * Adds the pending message ids.
		 *
		 * @param pendingMessageIds the pending message ids
		 */
		public void addPendingMessageIds(List<String> pendingMessageIds) {
			this.pendingMessageIds.addAll(pendingMessageIds);
		}
	}
}
