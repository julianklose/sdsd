package de.sdsd.projekt.agrirouter.request;

import java.util.Arrays;
import java.util.UUID;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import agrirouter.commons.Chunk.ChunkComponent;
import agrirouter.commons.MessageOuterClass.Metadata;
import agrirouter.request.Request.RequestEnvelope;
import agrirouter.request.Request.RequestEnvelope.Mode;
import agrirouter.response.Response.ResponseEnvelope;
import de.sdsd.projekt.agrirouter.ARException;
import de.sdsd.projekt.agrirouter.ARMessage;
import de.sdsd.projekt.agrirouter.ARMessageType;
import de.sdsd.projekt.agrirouter.ARRequest;

/**
 * Send or publish a file to other endpoints.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class ARSendMessage extends ARRequest<Boolean> {
	/**
	 * Maximum size of payload per file in bytes before chunking into several messages.
	 * The maximal message size is 1MB, but that includes overhead of the message header and Base64 encoding.
	 */
	public static final int MAX_MESSAGE_SIZE = 500000;

	/**
	 * Content of the file to send.
	 */
	protected byte[] payload = null;
	
	/**
	 * EFDI message to send.
	 */
	protected Message efdiPayload = null;

	/**
	 * Construct a new message to send.
	 */
	public ARSendMessage() {
		super(ARMessageType.OTHER.technicalMessageType());
		this.req.setTeamSetContextId("1");
	}
	
	/**
	 * Set the type of the containing file.
	 * 
	 * @param type agrirouter message type
	 * @return this object for method chaining
	 */
	public ARSendMessage setType(ARMessageType type) {
		this.req.setTechnicalMessageType(type.technicalMessageType());
		return this;
	}
	
	/**
	 * Set the team context id of the message.
	 * This is meant to label connected feeds of messages, but it could also be used to transfer the file name.
	 * 
	 * @param id string to send with the file
	 * @return this object for method chaining
	 */
	public ARSendMessage setTeamSetContextId(String id) {
		this.req.setTeamSetContextId(id);
		return this;
	}
	
	/**
	 * Set whether the file should be sent directly to the specified endpoints or published to all subscribed endpoints.
	 * You can still {@link #addRecipient(String) add recipients} to send the file directly to even though this is set to publish.
	 * 
	 * @param publish true to publish
	 * @return this object for method chaining
	 */
	public ARSendMessage setPublish(boolean publish) {
		if(publish) {
			if(req.getRecipientsCount() > 0)
				req.setMode(Mode.PUBLISH_WITH_DIRECT);
			else
				req.setMode(Mode.PUBLISH);
		} else 
			req.setMode(Mode.DIRECT);
		return this;
	}
	
	/**
	 * Add a recipient of the file.
	 * 
	 * @param targetId agrirouter endpoint id
	 * @return this object for method chaining
	 */
	public ARSendMessage addRecipient(String targetId) {
		this.req.addRecipients(targetId);
		if(req.getMode() == Mode.PUBLISH)
			req.setMode(Mode.PUBLISH_WITH_DIRECT);
		return this;
	}
	
	/**
	 * Add all the given recipents.
	 * 
	 * @param targetIds agrirouter endpoint id iterable
	 * @return this object for method chaining
	 */
	public ARSendMessage addAllRecipients(Iterable<String> targetIds) {
		this.req.addAllRecipients(targetIds);
		if(req.getMode() == Mode.PUBLISH && req.getRecipientsCount() > 0)
			req.setMode(Mode.PUBLISH_WITH_DIRECT);
		return this;
	}
	
	/**
	 * Remove all recipients.
	 * 
	 * @return this object for method chaining
	 */
	public ARSendMessage clearRecipients() {
		this.req.clearRecipients();
		if(req.getMode() == Mode.PUBLISH_WITH_DIRECT)
			req.setMode(Mode.PUBLISH);
		return this;
	}

	/**
	 * Set the content of the file to send.
	 * Files that are larger then {@value #MAX_MESSAGE_SIZE} bytes are automatically chunked in several messages.
	 * 
	 * @param payload file content
	 * @return this object for method chaining
	 */
	public ARSendMessage setPayload(byte[] payload) {
		this.payload = payload;
		return this;
	}
	
	/**
	 * Set the content of the EFDI message to send.
	 * EFDI messages do not get chunked.
	 * 
	 * @param payload EFDI message
	 * @return this object for method chaining
	 */
	public ARSendMessage setPayload(Message payload) {
		this.efdiPayload = payload;
		return this;
	}
	
	/**
	 * Sets the message metadata.
	 *
	 * @param metadata the metadata
	 * @return this object for method chaining
	 */
	public ARSendMessage setMetadata(Metadata metadata) {
		this.req.setMetadata(metadata);
		return this;
	}
	
	/**
	 * One time use independent representation of the request to send.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private static class SendMessageInstance extends RequestInstance<Boolean> {
		
		/** The chunkinfo. */
		protected ChunkComponent.Builder chunkinfo;
		
		/** The payload. */
		protected byte[] payload;
		
		/** The efdi payload. */
		protected Message efdiPayload;
		
		/** The part. */
		protected int part = 1;
		
		/**
		 * Constructs a one time use independent file send request.
		 *
		 * @param req request header
		 * @param payload file content
		 * @param efdiPayload the efdi payload
		 */
		public SendMessageInstance(RequestEnvelope req, byte[] payload, Message efdiPayload) {
			super(req);
			this.payload = payload != null ? Arrays.copyOf(payload, payload.length) : null;
			this.efdiPayload = efdiPayload != null ? efdiPayload.toBuilder().build() : null;
			
			// create info about chunks when needed
			this.chunkinfo = payload != null && payload.length > MAX_MESSAGE_SIZE ?
				ChunkComponent.newBuilder()
						.setContextId(UUID.randomUUID().toString())
						.setTotal((payload.length + MAX_MESSAGE_SIZE - 1) / MAX_MESSAGE_SIZE)
						.setTotalSize(payload.length)
				: null;
		}

		/**
		 * Checks for next.
		 *
		 * @return true, if successful
		 */
		@Override
		public boolean hasNext() {
			return part <= (chunkinfo != null ? chunkinfo.getTotal() : 1);
		}

		/**
		 * Next.
		 *
		 * @return the AR message
		 */
		@Override
		public ARMessage next() {
			if (!hasNext()) return null;
			if (chunkinfo != null)
				req.setChunkInfo(chunkinfo.setCurrent(part).build());

			ARMessage message = createMessage();
			if (chunkinfo != null) // calculate the content of the current chunk
				message.setPayload(Arrays.copyOfRange(payload, (part - 1) * MAX_MESSAGE_SIZE, Math.min(part * MAX_MESSAGE_SIZE, payload.length)));
			else if(payload != null)
				message.setPayload(payload);
			else if(efdiPayload != null)
				message.setParams(efdiPayload);
			++part;
			return message;
		}

		/**
		 * Gets the response.
		 *
		 * @return the response
		 */
		@Override
		public Boolean getResponse() {
			return !hasNext();
		}

		/**
		 * Read response.
		 *
		 * @param header the header
		 * @param params the params
		 * @return true, if successful
		 * @throws InvalidProtocolBufferException the invalid protocol buffer exception
		 * @throws ARException the AR exception
		 */
		@Override
		protected boolean readResponse(ResponseEnvelope header, Any params)
				throws InvalidProtocolBufferException, ARException {
			return true;
		}
		
	}

	/**
	 * Builds the.
	 *
	 * @return the request instance
	 */
	@Override
	public RequestInstance<Boolean> build() {
		return new SendMessageInstance(req.build(), payload, efdiPayload);
	}

}
