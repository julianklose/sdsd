package de.sdsd.projekt.agrirouter.request;

import java.util.Iterator;
import java.util.stream.IntStream;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import agrirouter.request.payload.endpoint.SubscriptionOuterClass.Subscription;
import agrirouter.request.payload.endpoint.SubscriptionOuterClass.Subscription.MessageTypeSubscriptionItem;
import agrirouter.request.payload.endpoint.SubscriptionOuterClass.Subscription.MessageTypeSubscriptionItem.Builder;
import agrirouter.response.Response.ResponseEnvelope;
import agrirouter.response.Response.ResponseEnvelope.ResponseBodyType;
import de.sdsd.projekt.agrirouter.ARMessageType;
import de.sdsd.projekt.agrirouter.ARRequest.ARSingleRequest;

/**
 * Subscription to receive messages of the specified type that are published by connected endpoints.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class ARSubscription extends ARSingleRequest<Boolean> {

	/** The m payload. */
	private final Subscription.Builder mPayload;

	/**
	 * Creates a new empty subscription list.
	 */
	public ARSubscription() {
		super("dke:subscription");
		mPayload = Subscription.newBuilder();
	}
	
	/**
	 * Creates a new subscription list of the given types.
	 * @param types types iterable
	 */
	public ARSubscription(Iterable<ARMessageType> types) {
		this();
		addSubscriptions(types);
	}
	
	/**
	 * Converts a message type to the required protobuf subscription item.
	 * @param type agrirouter message type to convert
	 * @return protobuf subscription item
	 */
	private static MessageTypeSubscriptionItem toSubscriptionItem(ARMessageType type) {
		Builder builder = MessageTypeSubscriptionItem.newBuilder()
				.setTechnicalMessageType(type.technicalMessageType());
		if(type == ARMessageType.TIME_LOG)
			builder.setPosition(true).addAllDdis(ALL_DDIS).addDdis(57342);
		return builder.build();
	}
	
	/**
	 * Iterable over all currently used DDIs.
	 */
	private static Iterable<Integer> ALL_DDIS = new Iterable<Integer>() {
		public Iterator<Integer> iterator() {
			return IntStream.rangeClosed(1, 600).boxed().iterator();
		}
	};
	
	/**
	 * Converts message types to protobuf subscription items on the fly.
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private static class SubscriptionItemIterable implements Iterable<MessageTypeSubscriptionItem> {
		
		/** The types. */
		private final Iterable<ARMessageType> types;
		
		/**
		 * Instantiates a new subscription item iterable.
		 *
		 * @param types the types
		 */
		public SubscriptionItemIterable(Iterable<ARMessageType> types) {
			this.types = types;
		}
		
		/**
		 * Iterator.
		 *
		 * @return the iterator
		 */
		@Override
		public Iterator<MessageTypeSubscriptionItem> iterator() {
			return new Iterator<MessageTypeSubscriptionItem>() {
				private final Iterator<ARMessageType> it = types.iterator();
				
				@Override
				public MessageTypeSubscriptionItem next() {
					return toSubscriptionItem(it.next());
				}
				
				@Override
				public boolean hasNext() {
					return it.hasNext();
				}
			};
		}
	}

	/**
	 * Add a message type to the subscription list.
	 * 
	 * @param type agrirouter message type
	 * @return this objecto for method chaining
	 */
	public ARSubscription addSubscription(ARMessageType type) {
		mPayload.addTechnicalMessageTypes(toSubscriptionItem(type));
		return this;
	}
	
	/**
	 * Add all message types to the subscription list.
	 * 
	 * @param types agrirouter message type iterable
	 * @return this object for method chaining
	 */
	public ARSubscription addSubscriptions(Iterable<ARMessageType> types) {
		mPayload.addAllTechnicalMessageTypes(new SubscriptionItemIterable(types));
		return this;
	}
	
	/**
	 * Remove all message types from the subscription list.
	 * 
	 * @return this object for method chaining
	 */
	public ARSubscription clearSubscriptions() {
		mPayload.clearTechnicalMessageTypes();
		return this;
	}

	/**
	 * Gets the params.
	 *
	 * @return the params
	 */
	@Override
	protected Message getParams() {
		return mPayload.build();
	}

	/**
	 * Parses the response.
	 *
	 * @param header the header
	 * @param payload the payload
	 * @return the boolean
	 * @throws InvalidProtocolBufferException the invalid protocol buffer exception
	 */
	@Override
	protected Boolean parseResponse(ResponseEnvelope header, Any payload) throws InvalidProtocolBufferException {
		return header.getType() == ResponseBodyType.ACK;
	}

}
