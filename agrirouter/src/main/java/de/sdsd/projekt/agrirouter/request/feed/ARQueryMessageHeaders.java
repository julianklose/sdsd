package de.sdsd.projekt.agrirouter.request.feed;

import static de.sdsd.projekt.agrirouter.ARConfig.DEBUG_MODE;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.Timestamps;

import agrirouter.commons.Chunk.ChunkComponent;
import agrirouter.feed.request.FeedRequests.MessageQuery;
import agrirouter.feed.request.FeedRequests.ValidityPeriod;
import agrirouter.feed.response.FeedResponse.FailedMessageQueryResponse;
import agrirouter.feed.response.FeedResponse.HeaderQueryResponse;
import agrirouter.request.Request.RequestEnvelope;
import agrirouter.response.Response.ResponseEnvelope;
import agrirouter.response.Response.ResponseEnvelope.ResponseBodyType;
import de.sdsd.projekt.agrirouter.ARException;
import de.sdsd.projekt.agrirouter.ARMessage;
import de.sdsd.projekt.agrirouter.ARRequest;
import de.sdsd.projekt.agrirouter.request.feed.ARMsgHeader.ARMsgHeaderResult;

/**
 * Retrieve a list of messages in the agrirouter message box.
 * This is always the first step for retrieving messages from the agrirouter.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class ARQueryMessageHeaders extends ARRequest<ARMsgHeaderResult> {
	
	private MessageQuery.Builder payload;

	/**
	 * Construct a new message header request.
	 */
	public ARQueryMessageHeaders() {
		super("dke:feed_header_query");
		this.payload = MessageQuery.newBuilder();
	}
	
	/**
	 * Filter messages from specific endpoints.
	 * 
	 * @param senderIds agrirouter endpoint id iterable
	 * @return this object for method chaining
	 */
	public ARQueryMessageHeaders addSenderFilters(Iterable<String> senderIds) {
		payload.addAllSenders(senderIds);
		return this;
	}
	
	/**
	 * Filter messages from a specific endpoint.
	 * 
	 * @param senderId agrirouter endpoint id
	 * @return this object for method chaining
	 */
	public ARQueryMessageHeaders addSenderFilter(String senderId) {
		payload.addSenders(senderId);
		return this;
	}
	
	/**
	 * Retrieve messages from all endpoints.
	 * 
	 * @return this object for method chaining
	 */
	public ARQueryMessageHeaders clearSenderFilter() {
		payload.clearSenders();
		return this;
	}
	
	/**
	 * Filter messages that were sent in a specific time range.
	 * 
	 * @param from first point in time
	 * @param to second point in time
	 * @return this object for method chaining
	 */
	public ARQueryMessageHeaders setValidityFilter(Instant from, Instant to) {
		payload.setValidityPeriod(ValidityPeriod.newBuilder()
				.setSentFrom(Timestamps.fromMillis(from.toEpochMilli()))
				.setSentTo(Timestamps.fromMillis(to.toEpochMilli()))
				.build());
		return this;
	}
	
	/**
	 * Filter to receive all messages.
	 * 
	 * @return this object for method chaining
	 */
	public ARQueryMessageHeaders setGetAllFilter() {
		payload.setValidityPeriod(ValidityPeriod.newBuilder()
				.setSentFrom(Timestamps.MIN_VALUE)
				.setSentTo(Timestamps.MAX_VALUE)
				.build());
		return this;
	}
	
	/**
	 * Retrieve messages regardless the time they were sent.
	 * 
	 * @return this object for method chaining
	 */
	public ARQueryMessageHeaders clearValidityFilter() {
		payload.clearValidityPeriod();
		return this;
	}
	
	/**
	 * One time use independent representation of the header query request.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private static class QueryMessageHeadersInstance extends RequestInstance<ARMsgHeaderResult> {

		private final MessageQuery query;
		private final Map<String, ARMsgHeader> chunks;
		private final ARMsgHeaderResult result;
		private int receivedPages = 0, totalPages = 1;
		
		/**
		 * Constructs a one time use independent message query request.
		 * 
		 * @param req request header
		 */
		public QueryMessageHeadersInstance(RequestEnvelope req, MessageQuery query) {
			super(req);
			this.query = query;
			this.chunks = new HashMap<>();
			this.result = new ARMsgHeaderResult();
		}

		@Override
		public boolean hasNext() {
			return receivedPages < totalPages;
		}

		@Override
		public ARMessage next() {
			return createMessage().setParams(query);
		}

		@Override
		public ARMsgHeaderResult getResponse() {
			return result;
		}

		@Override
		protected boolean readResponse(ResponseEnvelope header, Any params)
				throws InvalidProtocolBufferException, ARException {
			if(header.getType() == ResponseBodyType.ACK_FOR_FEED_HEADER_LIST) {
				HeaderQueryResponse resp = params.unpack(HeaderQueryResponse.class);
				if(DEBUG_MODE) System.out.println(resp.toString());
				totalPages = resp.getPage().getTotal();
				++receivedPages;
				
				result.setTotalMessagesInQuery(resp.getQueryMetrics().getTotalMessagesInQuery());
				result.addPendingMessageIds(resp.getPendingMessageIdsList());
				
				// create a map of chunk infos to find all messages that belong to the same file
				for(ChunkComponent chunk : resp.getChunkContextsList()) {
					String contextId = chunk.getContextId();
					if(contextId == null || contextId.isEmpty() || chunks.containsKey(contextId) || chunk.getTotal() <= 0)
						continue;
					ARMsgHeader msgHeader = new ARMsgHeader(chunk);
					result.add(msgHeader);
					chunks.put(contextId, msgHeader);
				}
				
				// read all message headers
				for(HeaderQueryResponse.Feed feed : resp.getFeedList()) {
					for(HeaderQueryResponse.Header head : feed.getHeadersList()) {
						if(head.getMessageId().isEmpty()) continue;
						ARMsgHeader msgHeader = chunks.get(head.getChunkContextId());
						if(msgHeader != null) msgHeader.addChunk(feed, head); // message is part of a chunked message
						else result.add(new ARMsgHeader(feed, head));
					}
				}
				return receivedPages >= totalPages;
			}
			else
				throw new ARException(params.unpack(FailedMessageQueryResponse.class).getReasonsList());
		}
	}

	@Override
	public RequestInstance<ARMsgHeaderResult> build() {
		return new QueryMessageHeadersInstance(req.build(), payload.build());
	}
	
}
