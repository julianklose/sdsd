package de.sdsd.projekt.agrirouter.request.feed;

import static de.sdsd.projekt.agrirouter.ARConfig.DEBUG_MODE;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import agrirouter.commons.MessageOuterClass.Message;
import agrirouter.commons.MessageOuterClass.Messages;
import agrirouter.feed.request.FeedRequests.MessageConfirm;
import agrirouter.feed.request.FeedRequests.MessageQuery;
import agrirouter.feed.response.FeedResponse.FailedMessageQueryResponse;
import agrirouter.feed.response.FeedResponse.MessageQueryResponse;
import agrirouter.feed.response.FeedResponse.MessageQueryResponse.Builder;
import agrirouter.feed.response.FeedResponse.MessageQueryResponse.FeedMessage;
import agrirouter.request.Request.RequestEnvelope;
import agrirouter.response.Response.ResponseEnvelope;
import de.sdsd.projekt.agrirouter.ARException;
import de.sdsd.projekt.agrirouter.ARMessage;
import de.sdsd.projekt.agrirouter.ARRequest;

/**
 * Obtain messages from the agrirouter message box.
 * After the messages have been retrieved successfully they are confirmed and deleted from the message box.
 * Note that when there was an error during the retrieval of a message, this message cannot be 
 * {@link ARDeleteMessage deleted} anymore and must be retrieved and confirmed.
 * If a message consists of several chunks, this request retrieves all chunks.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 * @see ARQueryMessageHeaders
 */
public class ARQueryMessages extends ARRequest<List<ARMsg>> {
	
	/** The Constant TYPE_CONFIRM. */
	private static final String TYPE_QUERY = "dke:feed_message_query", TYPE_CONFIRM = "dke:feed_confirm";
	
	/** The header. */
	private List<ARMsgHeader> header = new ArrayList<>();

	/**
	 * Construct a new message query request.
	 */
	public ARQueryMessages() {
		super(TYPE_QUERY);
	}
	
	/**
	 * Add a message to obtain.
	 * Check {@link ARMsgHeader#isComplete()} to see if every part of a chunked message is ready to obtain.
	 * Don't use for incomplete messages.
	 * 
	 * @param head message header from {@link ARQueryMessageHeaders}
	 * @return this object for method chaining
	 */
	public ARQueryMessages addMessageFilter(ARMsgHeader head) {
		this.header.add(head);
		return this;
	}
	
	/**
	 * Add messages to obtain.
	 * Check {@link ARMsgHeader#isComplete()} to see if every part of a chunked message is ready to obtain.
	 * Don't use for incomplete messages.
	 *
	 * @param header the header
	 * @return this object for method chaining
	 */
	public ARQueryMessages addMessageFilter(Collection<ARMsgHeader> header) {
		this.header.addAll(header);
		return this;
	}
	
	/**
	 * Unset the message to obtain.
	 * 
	 * @return this object for method chaining
	 */
	public ARQueryMessages clearMessageIdFilter() {
		header.clear();
		return this;
	}
	
	/**
	 * One time use independent representation of the query request.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private static class QueryMessageInstance extends RequestInstance<List<ARMsg>> {

		/** The results. */
		private final Map<String, ARMsg> results;
		
		/** The query. */
		private final MessageQuery.Builder query;
		
		/** The confirm. */
		private final MessageConfirm.Builder confirm;
		
		/** The total pages. */
		private int receivedPages = 0, totalPages = 1;
		
		/**
		 * Constructs a one time use independent message query request.
		 *
		 * @param req request header
		 * @param header the header
		 */
		public QueryMessageInstance(RequestEnvelope req, List<ARMsgHeader> header) {
			super(req);
			this.query = MessageQuery.newBuilder();
			this.confirm = MessageConfirm.newBuilder();
			this.results = new LinkedHashMap<>(); // preserve order of the header list
			for(ARMsgHeader head : header) {
				if(Collections.disjoint(results.keySet(), head.getIds())) {
					ARMsg arMsg = new ARMsg(head);
					head.getIds().forEach(id -> results.put(id, arMsg));
				}
			}
			query.addAllMessageIds(results.keySet());
		}

		/**
		 * Checks for next.
		 *
		 * @return true, if successful
		 */
		@Override
		public boolean hasNext() {
			return receivedPages <= totalPages;
		}

		/**
		 * Next.
		 *
		 * @return the AR message
		 */
		@Override
		public ARMessage next() {
			if(receivedPages < totalPages) {
				req.setTechnicalMessageType(TYPE_QUERY);
				return createMessage().setParams(query.build());
			} 
			else if(receivedPages == totalPages) {
				req.setTechnicalMessageType(TYPE_CONFIRM);
				return createMessage().setParams(confirm.build());
			} 
			else 
				return null;
		}
		
		/**
		 * Gets the response.
		 *
		 * @return the response
		 */
		@Override
		public List<ARMsg> getResponse() {
			return results.values().stream()
					.filter(new OrderedDistinct())
					.filter(ARMsg::isComplete)
					.collect(Collectors.toList());
		}
		
		/**
		 * A filter that only returns true to the first equal item in an ordered stream.
		 * This is a stateful filter and performs only object reference comparison with the last checked object.
		 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
		 */
		private static class OrderedDistinct implements Predicate<Object> {
			
			/** The last. */
			private Object last = this;
			
			/**
			 * Test.
			 *
			 * @param t the t
			 * @return true, if successful
			 */
			@Override
			public boolean test(Object t) {
				boolean ok = t != last;
				last = t;
				return ok;
			}
		}
		
		/** The error. */
		private ARException error = null;

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
		protected synchronized boolean readResponse(ResponseEnvelope header, Any params)
				throws InvalidProtocolBufferException, ARException {
			switch(header.getType()) {
			case ACK_FOR_FEED_MESSAGE: { // positive result of message retrieval
				MessageQueryResponse resp = params.unpack(MessageQueryResponse.class);
				if(DEBUG_MODE) {
					Builder builder = resp.toBuilder();
					Any content = Any.newBuilder().setValue(ByteString.copyFrom("omitted", StandardCharsets.UTF_8)).build();
					builder.getMessagesBuilderList().forEach(feedmsg -> feedmsg.setContent(content));
					System.out.println(builder.toString());
				}
				totalPages = resp.getPage().getTotal();
				++receivedPages;
				for(FeedMessage feedmsg : resp.getMessagesList()) {
					ARMsg arMsg = results.get(feedmsg.getHeader().getMessageId());
					try {
						if(arMsg != null) // check if the message is one of the requested messages, otherwise there are unconfirmed messages in the outbox
							arMsg.addMessage(feedmsg);
					} catch(ARException e) {
						error = e;
					}
					confirm.addMessageIds(feedmsg.getHeader().getMessageId());
				}
				return receivedPages >= totalPages;
			}
			case ACK_FOR_FEED_FAILED_MESSAGE: // error result of message retrieval
				FailedMessageQueryResponse failureResponse = params.unpack(FailedMessageQueryResponse.class);
				if(DEBUG_MODE) System.out.println(failureResponse.toString());
				throw new ARException(failureResponse.getReasonsList());
			case ACK_WITH_MESSAGES: { // positive result of message confirmation
				Messages msgs = params.unpack(Messages.class);
				if(msgs != null)
					msgs.getMessagesList().stream()
							.filter(m -> !m.getMessageCode().equals("VAL_000206"))
							.map(Message::getMessage).forEachOrdered(System.out::println);
			}
			case ACK: // positive result of message confirmation
				if(receivedPages == totalPages) {
					++receivedPages; // confirmation successful
					if(error != null) throw error;
					return true;
				}
			default:
				return false;
			}
		}
		
	}
	
	/**
	 * Builds the.
	 *
	 * @return the request instance
	 */
	@Override
	public RequestInstance<List<ARMsg>> build() {
		return new QueryMessageInstance(req.build(), header);
	}

}
