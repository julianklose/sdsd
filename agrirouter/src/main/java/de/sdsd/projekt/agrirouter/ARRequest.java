package de.sdsd.projekt.agrirouter;

import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.Timestamps;

import agrirouter.commons.MessageOuterClass.Messages;
import agrirouter.request.Request.RequestEnvelope;
import agrirouter.response.Response.ResponseEnvelope;
import agrirouter.response.Response.ResponseEnvelope.ResponseBodyType;
import de.sdsd.projekt.agrirouter.ARMessage.Response;

/**
 * Base for all requests and file messages sent to the agrirouter.
 * Use {@link ARSingleRequest} for one part agrirouter requests.
 * 
 * @param <T> the response type of the request
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 * @see ARSingleRequest
 */
public abstract class ARRequest<T> {
	/**
	 * Editable request envelope.
	 */
	protected final RequestEnvelope.Builder req;

	/**
	 * Constructs a new request of the given type with a new header.
	 * @param technicalMessageType {@link ARMessageType#technicalMessageType() agrirouter message type}
	 */
	public ARRequest(String technicalMessageType) {
		this.req = RequestEnvelope.newBuilder()
			.setMode(RequestEnvelope.Mode.DIRECT)
			.setTechnicalMessageType(technicalMessageType);
	}
	
	/**
	 * Builds a request instance from this request. The request instance is independent from the request object.
	 * @return independent request instance
	 */
	public abstract RequestInstance<T> build();
	
	/**
	 * A request instance is a one time use independent representation of the request to send it to the agrirouter.
	 *
	 * @param <T> the response type of the request
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static abstract class RequestInstance<T> implements Iterator<ARMessage> {
		/**
		 * Editable request envelope. Make sure not to use the request envelope of the enveloping request.
		 */
		protected RequestEnvelope.Builder req;
		
		/**
		 * Creates a new request instance with a copy of the request envelope.
		 * @param req request envelope
		 */
		public RequestInstance(RequestEnvelope req) {
			this.req = req.toBuilder();
		}
		
		/**
		 * Creates a agrirouter message with the given sequence number, adding a random message id and the timestamp.
		 * 
		 * @param seqno ascending sequence number, starting with 1
		 * @return the created agrirouter message without body.
		 */
		protected ARMessage createMessage() {
			return new ARMessage(req
					.setApplicationMessageId(UUID.randomUUID().toString())
					.setApplicationMessageSeqNo(1)
					.setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
					.build());
		}
		
		/**
		 * Returns the result of the request. 
		 * Call this after every part of the request was processed by {@link #readResponse(ResponseEnvelope, Any)}.
		 * 
		 * @return the complete result of the request
		 */
		public abstract T getResponse();
		
		/**
		 * Process the result of the current part of the request.
		 * 
		 * @param header response header
		 * @param params response body, could contain the protobuf response message or binary message data
		 * @return whether the result could be read successfully and contained no errors
		 * @throws InvalidProtocolBufferException if it contains invalid protobuf data
		 * @throws ARException if the agrirouter returned an error
		 */
		protected abstract boolean readResponse(ResponseEnvelope header, Any params) throws InvalidProtocolBufferException, ARException;
		
		/**
		 * Process the result of the current part of the request.
		 * This function is intended to be called within a {@link CompletableFuture}-chain.
		 * 
		 * @param resp response of a request part
		 * @return whether the request is completed with this response
		 * @throws CompletionException encapsulated exceptions from reading the response
		 */
		final boolean addResponse(Response resp) throws CompletionException {
			try {
				if(resp.header.getType() == ResponseBodyType.ACK_WITH_FAILURE)
					throw new ARException(resp.payload.unpack(Messages.class).getMessagesList());
				return readResponse(resp.header, resp.payload);
			} catch (Throwable e) {
				throw new CompletionException(e);
			}
		}
	}

	/**
	 * Base for normal agrirouter requests that consists of only one part.
	 * 
	 * @param <T> the response type of the request
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static abstract class ARSingleRequest<T> extends ARRequest<T> {
		
		/**
		 * Constructs a new request of the given type with a new header.
		 * @param technicalMessageType {@link ARMessageType#technicalMessageType() agrirouter message type}
		 */
		public ARSingleRequest(String technicalMessageType) {
			super(technicalMessageType);
		}
		
		/**
		 * Returns the request body containing the parameters.
		 * @return request body
		 */
		protected abstract Message getParams();
		
		@Override
		public RequestInstance<T> build() {
			return new SingleRequestInstance(req.build(), getParams());
		}
		
		/**
		 * Process the result of the current part of the request.
		 * 
		 * @param header response header
		 * @param params response body, could contain the protobuf response message or binary message data
		 * @return whether the result could be read successfully and contained no errors
		 * @throws InvalidProtocolBufferException if it contains invalid protobuf data
		 * @throws ARException if the agrirouter returned an error
		 */
		protected abstract T parseResponse(ResponseEnvelope header, Any params) throws InvalidProtocolBufferException, ARException;
		
		/**
		 * A request instance is a one time use independent representation of the request to send it to the agrirouter.
		 * 
		 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
		 */
		protected class SingleRequestInstance extends RequestInstance<T> {
			private final Message params;
			private T result;
			private boolean unsent = true;

			/**
			 * Creates a new request instance with a copy of the request envelope.
			 * @param req request envelope
			 * @param params request body
			 */
			public SingleRequestInstance(RequestEnvelope req, Message params) {
				super(req);
				this.params = params;
			}

			@Override
			public boolean hasNext() {
				return unsent;
			}

			@Override
			public ARMessage next() {
				unsent = false;
				return createMessage().setParams(params);
			}

			@Override
			public T getResponse() {
				return result;
			}

			@Override
			protected synchronized boolean readResponse(ResponseEnvelope header, Any params)
					throws InvalidProtocolBufferException, ARException {
				result = parseResponse(header, params);
				return true;
			}
			
		}
	}
	
	/**
	 * Send the a request to the agrirouter asynchronously.
	 * 
	 * @param conn the agrirouter connection to use
	 * @param timeout the time from now to wait before aborting, 0 for infinite waiting
	 * @param unit the time unit of the delay parameter
	 * @return the future object to obtain results or exceptions
	 * @throws IOException error while processing the request
	 * @throws ARException error while sending the request to the agrirouter
	 * @see ARConnection#sendRequestAsync(ARRequest, long, TimeUnit)
	 * @see ScheduledExecutorService#schedule(Runnable, long, TimeUnit)
	 */
	public final CompletableFuture<T> sendAsync(ARConnection conn, long timeout, TimeUnit unit) throws IOException, ARException {
		return conn.sendRequestAsync(this, timeout, unit);
	}

	/**
	 * Send the a request to the agrirouter synchrounously.
	 * 
	 * @param conn the agrirouter connection to use
	 * @param timeout the time from now to wait before aborting, 0 for infinite waiting
	 * @param unit the time unit of the delay parameter
	 * @return the result of the request
	 * @throws IOException error while processing the request
	 * @throws InterruptedException if the current thread was interrupted while waiting
	 * @throws ARException agrirouter error
	 * @see ARConnection#sendRequest(ARRequest, long, TimeUnit)
	 */
	public final T send(ARConnection conn, long timeout, TimeUnit unit) throws IOException, InterruptedException, ARException {
		return conn.sendRequest(this, timeout, unit);
	}

}
