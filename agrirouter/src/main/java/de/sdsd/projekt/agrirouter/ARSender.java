package de.sdsd.projekt.agrirouter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.sdsd.projekt.agrirouter.ARMessage.Response;
import de.sdsd.projekt.agrirouter.ARRequest.RequestInstance;

/**
 * Intern class for sending request to the agrirouter and receiving the results.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
abstract class ARSender implements AutoCloseable {

	/**
	 * Mapping of message IDs for connecting responses to the correct request.
	 */
	protected final ConcurrentMap<String, ResponseFuture> responseMap = new ConcurrentHashMap<>();
	/**
	 * Queue of all currently pending requests.
	 * A request can have multiple parts.
	 */
	protected final ConcurrentLinkedDeque<RequestFuture> pendingRequests = new ConcurrentLinkedDeque<>();
	/**
	 * Executor service to wait for responses asynchronously.
	 * @see #startExecutorIfNotRunning()
	 * @see #stopExecutorIfDone()
	 * @see #responseGetter
	 */
	protected final ScheduledExecutorService executor;

	/**
	 * Constructor
	 * @param executor executor service with thread pool to use for receiving responses from the agrirouter. Doesn't need more than one thread.
	 */
	public ARSender(ScheduledExecutorService executor) {
		this.executor = executor;
	}
	
	/**
	 * Represents a pending response.
	 * Call {@link #partComplete(Response)} for each received response. It completes when all responses are received.
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	protected class ResponseFuture extends CompletableFuture<RequestInstance<?>> {
		private final RequestInstance<?> req;
		
		/**
		 * Creates a new ResponseFuture.
		 * @param req the corresponding request instance
		 */
		public ResponseFuture(RequestInstance<?> req) {
			this.req = req;
		}
		
		/**
		 * This calls {@link RequestInstance#addResponse(Response)} and completes this future when all responses are received.
		 * @param resp received response
		 * @return true if this invocation caused this CompletableFutureto transition to a completed state, else false
		 */
		public boolean partComplete(Response resp) {
			try {
				if(req.addResponse(resp)) {
					super.complete(req);
					responseMap.remove(resp.header.getApplicationMessageId());
					return true;
				} else
					return false;
			} catch (Throwable e) {
				responseMap.remove(resp.header.getApplicationMessageId());
				return completeExceptionally(e);
			}
		}
		
		/**
		 * The asynchronous version of {@link #partComplete(Response)}.
		 * @param resp received response
		 * @return Future with the result of {@link #partComplete(Response)}
		 */
		public Future<Boolean> partCompleteAsync(final Response resp) {
			return executor.submit(() -> partComplete(resp));
		}
	}
	
	/**
	 * Represents a pending request.
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	protected class RequestFuture {
		public final RequestInstance<?> req;
		private String currentMessage = null;
		
		/**
		 * Creates a new RequestFuture.
		 * @param req the corresponding request instance
		 */
		public RequestFuture(RequestInstance<?> req) {
			this.req = req;
		}
		
		/**
		 * Set the request completed.
		 * Currently waiting responses are interrupted and completed exceptionally.
		 */
		public void setCompleted() {
			setCurrentMessage(null);
			pendingRequests.remove(this);
		}
		
		/**
		 * Set an currently waiting response to abort it if the request gets aborted.
		 * @param currentMessage message ID of the waiting response
		 * @see #responseMap
		 */
		public synchronized void setCurrentMessage(String currentMessage) {
			if(this.currentMessage != null) {
				ResponseFuture future = responseMap.remove(this.currentMessage);
				if(future != null && !future.isDone())
					future.completeExceptionally(new InterruptedException());
			}
			this.currentMessage = currentMessage;
		}
	}

	/**
	 * Send the a request to the agrirouter asynchronously.
	 * 
	 * @param req {@link ARRequest#build() built} request instance
	 * @param timeout the time from now to wait before aborting, 0 for infinite waiting
	 * @param unit the time unit of the delay parameter
	 * @return the future object to obtain results or exceptions
	 * @throws IOException error while processing the request
	 * @throws ARException error while sending the request to the agrirouter
	 * @see ScheduledExecutorService#schedule(Runnable, long, TimeUnit)
	 */
	public <T> CompletableFuture<T> send(RequestInstance<T> req, long timeout, TimeUnit unit) throws IOException, ARException {
		final RequestFuture rf = new RequestFuture(req);
		pendingRequests.add(rf);
		
		CompletableFuture<T> future = sendNext(req, rf);
		if(timeout > 0)
			executor.schedule(() -> future.completeExceptionally(new ARException("No answer received from agrirouter.")), timeout, unit);
		return future.whenComplete((r,e) -> rf.setCompleted());
	}
	
	/**
	 * Iterate over every message (request part) of the request and send it.
	 * This function calls itself recursively for every message in the request.
	 * 
	 * @param it request instance to process
	 * @param rf the created request future for the current request
	 * @return future object to obtain exceptions or a boolean value, if every part was sent successfully
	 * @throws IOException error while processing the request
	 * @throws ARException error while sending the request to the agrirouter
	 */
	private <T> CompletableFuture<T> sendNext(RequestInstance<T> it, RequestFuture rf) throws IOException, ARException {
		return it.hasNext() ? sendMsg(it.next(), rf).thenCompose(r -> {
			try {
				return sendNext(it, rf);
			} catch (IOException | ARException e) {
				throw new CompletionException(e);
			}
		}) : CompletableFuture.completedFuture(it.getResponse());
	}
	
	/**
	 * Send a single message (request part) to the agrirouter.
	 * 
	 * @param message request part to send
	 * @param rf the created request future for the current request
	 * @return future object that completes when the complete response was received
	 * @throws IOException error while processing the request
	 * @throws ARException error while sending the request to the agrirouter
	 */
	protected abstract ResponseFuture sendMsg(ARMessage message, RequestFuture rf) throws IOException, ARException;

	@Override
	public void close() {
		ARException e = new ARException("Connection closed");
		for(ResponseFuture future : responseMap.values()) {
			if(!future.isDone()) future.completeExceptionally(e);
		}
		responseMap.clear();
		pendingRequests.clear();
	}
}
