package de.sdsd.projekt.agrirouter.request.feed;

import static de.sdsd.projekt.agrirouter.ARConfig.DEBUG_MODE;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import agrirouter.commons.MessageOuterClass.Messages;
import agrirouter.feed.request.FeedRequests.MessageConfirm;
import agrirouter.response.Response.ResponseEnvelope;
import agrirouter.response.Response.ResponseEnvelope.ResponseBodyType;
import de.sdsd.projekt.agrirouter.ARException;
import de.sdsd.projekt.agrirouter.ARRequest.ARSingleRequest;
import de.sdsd.projekt.agrirouter.request.feed.ARMsgHeader.ARMsgHeaderResult;

/**
 * Confirm messages from the agrirouter message box.
 * Usually messages are confirmed automatically at receiving. 
 * You have to manually confirm messages that had errors or that are {@link ARMsgHeaderResult#getPendingMessageIds() pending}.
 * Confirmed messages are no longer in the agrirouter message box.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 * @see ARQueryMessageHeaders
 * @see ARQueryMessages
 */
public class ARConfirmMessage extends ARSingleRequest<Messages> {
	
	private final MessageConfirm.Builder param;

	/**
	 * Construct a new delete message request.
	 */
	public ARConfirmMessage() {
		super("dke:feed_confirm");
		this.param = MessageConfirm.newBuilder();
	}
	
	/**
	 * Add a message to confirm.
	 * 
	 * @param messageId message id from {@link ARMsgHeaderResult#getPendingMessageIds()}
	 * @return this object for method chaining
	 */
	public ARConfirmMessage addMessage(String messageId) {
		this.param.addMessageIds(messageId);
		return this;
	}
	
	/**
	 * Add all messages to confirm.
	 * 
	 * @param messageIds message ids from {@link ARMsgHeaderResult#getPendingMessageIds()}
	 * @return this object for method chaining
	 */
	public ARConfirmMessage addMessages(Iterable<String> messageIds) {
		this.param.addAllMessageIds(messageIds);
		return this;
	}
	
	/**
	 * @return The count of messages to confirm
	 */
	public int count() {
		return this.param.getMessageIdsCount();
	}
	
	/**
	 * Clear the list of messages to confirm.
	 * 
	 * @return this object for method chaining
	 */
	public ARConfirmMessage clearMessages() {
		this.param.clearMessageIds();
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
