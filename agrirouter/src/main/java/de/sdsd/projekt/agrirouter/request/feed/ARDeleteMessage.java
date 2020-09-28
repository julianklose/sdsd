package de.sdsd.projekt.agrirouter.request.feed;

import static de.sdsd.projekt.agrirouter.ARConfig.DEBUG_MODE;

import java.time.Instant;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.Timestamps;

import agrirouter.commons.MessageOuterClass.Messages;
import agrirouter.feed.request.FeedRequests.MessageDelete;
import agrirouter.feed.request.FeedRequests.ValidityPeriod;
import agrirouter.response.Response.ResponseEnvelope;
import agrirouter.response.Response.ResponseEnvelope.ResponseBodyType;
import de.sdsd.projekt.agrirouter.ARException;
import de.sdsd.projekt.agrirouter.ARRequest.ARSingleRequest;

/**
 * Delete messages from the agrirouter message box.
 * You can only delete messages that you haven't {@link ARQueryMessages obtained} yet.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 * @see ARQueryMessageHeaders
 * @see ARQueryMessages
 */
public class ARDeleteMessage extends ARSingleRequest<Messages> {
	
	private final MessageDelete.Builder param;

	/**
	 * Construct a new delete message request.
	 */
	public ARDeleteMessage() {
		super("dke:feed_delete");
		this.param = MessageDelete.newBuilder();
	}
	
	/**
	 * Add a message to delete from the message box.
	 * You can only delete messages that you haven't {@link ARQueryMessages obtained} yet.
	 * 
	 * @param header message header from {@link ARQueryMessageHeaders}
	 * @return this object for method chaining
	 */
	public ARDeleteMessage addMessage(ARMsgHeader header) {
		this.param.addAllMessageIds(header.getIds());
		return this;
	}
	
	/**
	 * Add all messages to delete from the message box.
	 * You can only delete messages that you haven't {@link ARQueryMessages obtained} yet.
	 * 
	 * @param headers
	 * @return this object for method chaining
	 */
	public ARDeleteMessage addMessages(Iterable<ARMsgHeader> headers) {
		headers.forEach(this::addMessage);
		return this;
	}
	
	/**
	 * Clear the list of messages to delete.
	 * 
	 * @return this object for method chaining
	 */
	public ARDeleteMessage clearMessages() {
		this.param.clearMessageIds();
		return this;
	}
	
	/**
	 * Clear messages from specific endpoints.
	 * 
	 * @param senderIds agrirouter endpoint id iterable
	 * @return this object for method chaining
	 */
	public ARDeleteMessage addSenderFilters(Iterable<String> senderIds) {
		this.param.addAllSenders(senderIds);
		return this;
	}
	
	/**
	 * Clear messages from a specific endpoint.
	 * 
	 * @param senderId agrirouter endpoint id
	 * @return this object for method chaining
	 */
	public ARDeleteMessage addSenderFilter(String senderId) {
		this.param.addSenders(senderId);
		return this;
	}
	
	/**
	 * Clear messages from all endpoints.
	 * 
	 * @return this object for method chaining
	 */
	public ARDeleteMessage clearSenderFilter() {
		this.param.clearSenders();
		return this;
	}
	
	/**
	 * Clear messages that were sent in a specific time range.
	 * 
	 * @param from first point in time
	 * @param to second point in time
	 * @return this object for method chaining
	 */
	public ARDeleteMessage setValidityFilter(Instant from, Instant to) {
		this.param.setValidityPeriod(ValidityPeriod.newBuilder()
				.setSentFrom(Timestamps.fromMillis(from.toEpochMilli()))
				.setSentTo(Timestamps.fromMillis(to.toEpochMilli()))
				.build());
		return this;
	}
	
	/**
	 * Clear messages regardless the time they were sent.
	 * 
	 * @return this object for method chaining
	 */
	public ARDeleteMessage clearValidityFilter() {
		this.param.clearValidityPeriod();
		return this;
	}

	@Override
	protected Message getParams() {
		return param.build();
	}

	@Override
	protected Messages parseResponse(ResponseEnvelope header, Any params)
			throws InvalidProtocolBufferException, ARException {
		if(header.getType() == ResponseBodyType.ACK_WITH_MESSAGES) {
			Messages messages = params.unpack(Messages.class);
			if(DEBUG_MODE) System.out.println(messages.toString());
			return messages;
		}
		return Messages.getDefaultInstance();
	}

}
