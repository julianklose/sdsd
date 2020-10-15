package de.sdsd.projekt.prototype.websocket;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;
import javax.websocket.Session;

import org.json.JSONArray;
import org.json.JSONObject;

import de.sdsd.projekt.prototype.data.SDSDException;
import de.sdsd.projekt.prototype.data.Service;
import de.sdsd.projekt.prototype.data.ServiceInstance;
import de.sdsd.projekt.prototype.websocket.SDSDEvent.SDSDListener;

/**
 * Websocket connection for website client and service connections.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class WebsocketConnection implements SDSDListener<Service, ServiceInstance> {
	
	/** The session. */
	private final Session session;
	
	/**
	 * The Class ListenerCloser.
	 *
	 * @param <I> the generic type
	 * @param <R> the generic type
	 */
	private class ListenerCloser<I, R> {
		
		/** The event. */
		public SDSDEvent<I, R> event;
		
		/** The identifier. */
		public I identifier;
		
		/**
		 * Instantiates a new listener closer.
		 *
		 * @param event the event
		 * @param identifier the identifier
		 */
		public ListenerCloser(SDSDEvent<I, R> event, I identifier) {
			this.event = event;
			this.identifier = identifier;
		}
		
		/**
		 * Compute set.
		 *
		 * @param i the i
		 * @param ol the ol
		 * @return the sets the
		 */
		public Set<ListenerCloser<?,?>> computeSet(String i, Set<ListenerCloser<?,?>> ol) {
			if(ol == null) ol = new HashSet<>();
			ol.add(this);
			return ol;
		}
		
		/**
		 * Compute unset.
		 *
		 * @param i the i
		 * @param ol the ol
		 * @return the sets the
		 */
		public Set<ListenerCloser<?,?>> computeUnset(String i, Set<ListenerCloser<?,?>> ol) {
			if(ol != null) {
				ol.remove(this);
				if(ol.isEmpty())
					ol = null;
			}
			return ol;
		}
		
		/**
		 * Close.
		 */
		public void close() {
			event.unsetListener(identifier, session.getId());
		}
		
		/**
		 * Hash code.
		 *
		 * @return the int
		 */
		@Override
		public int hashCode() {
			return Objects.hash(event, identifier);
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
			if (!(obj instanceof ListenerCloser))
				return false;
			@SuppressWarnings("rawtypes")
			ListenerCloser other = (ListenerCloser) obj;
			return Objects.equals(event, other.event) && Objects.equals(identifier, other.identifier);
		}
	}
	
	/** The open listeners. */
	private final ConcurrentHashMap<String, Set<ListenerCloser<?, ?>>> openListeners = new ConcurrentHashMap<>();
	
	/** The instance stopped listener. */
	private ListenerCloser<Service, ServiceInstance> instanceStoppedListener = null;
	
	/**
	 * Instantiates a new websocket connection.
	 *
	 * @param session the session
	 */
	public WebsocketConnection(Session session) {
		this.session = session;
	}
	
	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public String getId() {
		return session.getId();
	}
	
	/**
	 * Observer id.
	 *
	 * @return the object
	 */
	@Override
	public Object observerId() {
		return session.getId();
	}
	
	/**
	 * Accept.
	 *
	 * @param t the t
	 */
	@Override
	public void accept(ServiceInstance t) {
		unsetInstanceListeners(t.getToken());
	}

	/**
	 * Sets the.
	 *
	 * @param event the event
	 * @param identifier the identifier
	 */
	@Override
	public void set(SDSDEvent<Service, ServiceInstance> event, Service identifier) {
		this.instanceStoppedListener = new ListenerCloser<Service, ServiceInstance>(event, identifier);
	}

	/**
	 * Unset.
	 *
	 * @param event the event
	 * @param identifier the identifier
	 */
	@Override
	public void unset(SDSDEvent<Service, ServiceInstance> event, Service identifier) {
		this.instanceStoppedListener = null;
	}
	
	/**
	 * Unset all listeners.
	 */
	public void unsetAllListeners() {
		Iterator<Entry<String, Set<ListenerCloser<?, ?>>>> it = openListeners.entrySet().iterator();
		while(it.hasNext()) {
			Set<ListenerCloser<?, ?>> remove = it.next().getValue();
			it.remove();
			if(remove != null) remove.forEach(ListenerCloser::close);
		}
		if(instanceStoppedListener != null)
			instanceStoppedListener.close();
	}
	
	/**
	 * Unset instance listeners.
	 *
	 * @param instanceToken the instance token
	 */
	public void unsetInstanceListeners(String instanceToken) {
		Set<ListenerCloser<?, ?>> remove = openListeners.remove(instanceToken);
		if(remove != null) remove.forEach(ListenerCloser::close);
	}
	
	/**
	 * On close.
	 */
	void onClose() {
		unsetAllListeners();
	}
	
	/**
	 * The Interface CheckedFunction.
	 *
	 * @param <T> the generic type
	 * @param <R> the generic type
	 */
	@FunctionalInterface
	public interface CheckedFunction<T, R> {
		
		/**
		 * Apply.
		 *
		 * @param t the t
		 * @return the r
		 * @throws Throwable the throwable
		 */
		R apply(T t) throws Throwable;
	}
	
	/**
	 * Listener.
	 *
	 * @param <I> the generic type
	 * @param <R> the generic type
	 * @param endpoint the endpoint
	 * @param method the method
	 * @param instanceToken the instance token
	 * @param identifier the identifier
	 * @param callback the callback
	 * @return the SDSD listener
	 * @throws SDSDException the SDSD exception
	 */
	public <I, R> SDSDListener<I, R> listener(String endpoint, String method, @Nullable String instanceToken, 
			@Nullable String identifier, CheckedFunction<R, ?> callback) throws SDSDException {
		return new WSListener<I, R>(endpoint, method, instanceToken, identifier, callback);
	}
	
	/**
	 * The listener interface for receiving WS events.
	 * The class that is interested in processing a WS
	 * event implements this interface, and the object created
	 * with that class is registered with a component using the
	 * component's <code>addWSListener<code> method. When
	 * the WS event occurs, that object's appropriate
	 * method is invoked.
	 *
	 * @param <I> the generic type
	 * @param <R> the generic type
	 * @see WSEvent
	 */
	public class WSListener<I, R> implements SDSDListener<I, R> {
		
		/** The identifier. */
		private String endpoint, method, instanceToken, identifier;
		
		/** The callback. */
		private CheckedFunction<R, ?> callback;
		
		/**
		 * Instantiates a new WS listener.
		 *
		 * @param endpoint the endpoint
		 * @param method the method
		 * @param instanceToken the instance token
		 * @param identifier the identifier
		 * @param callback the callback
		 * @throws SDSDException the SDSD exception
		 */
		public WSListener(String endpoint, String method, @Nullable String instanceToken, @Nullable String identifier, 
				CheckedFunction<R, ?> callback) throws SDSDException {
			if(instanceToken != null && instanceStoppedListener == null)
				throw new SDSDException("Set InstanceChanged listener before any other listeners");
			this.endpoint = endpoint;
			this.method = method;
			this.instanceToken = instanceToken;
			this.identifier = identifier;
			this.callback = callback;
		}
		
		/**
		 * Observer id.
		 *
		 * @return the object
		 */
		@Override
		public Object observerId() {
			return session.getId();
		}
		
		/**
		 * Accept.
		 *
		 * @param t the t
		 */
		@Override
		public void accept(R t) {
			try {
				Object params = callback.apply(t);
				JSONArray arr = params.getClass().isArray() ? new JSONArray(params) : new JSONArray().put(params);
				
				sendMessage(new JSONObject()
						.put("endpoint", endpoint)
						.put("method", method)
						.put("token", instanceToken)
						.put("identifier", identifier)
						.put("params", arr)
						.toString());
			} catch(Throwable e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * Sets the.
		 *
		 * @param event the event
		 * @param identifier the identifier
		 */
		@Override
		public void set(SDSDEvent<I, R> event, I identifier) {
			ListenerCloser<I, R> lc = new ListenerCloser<I, R>(event, identifier);
			openListeners.compute(instanceToken != null ? instanceToken : "", lc::computeSet);
		}
		
		/**
		 * Unset.
		 *
		 * @param event the event
		 * @param identifier the identifier
		 */
		@Override
		public void unset(SDSDEvent<I, R> event, I identifier) {
			ListenerCloser<I, R> lc = new ListenerCloser<I, R>(event, identifier);
			openListeners.compute(instanceToken != null ? instanceToken : "", lc::computeUnset);
		}
	}
	
	/**
	 * Send.
	 *
	 * @param endpoint the endpoint
	 * @param method the method
	 * @param params the params
	 */
	public void send(String endpoint, String method, Object...params) {
		JSONArray arr = new JSONArray();
		for(Object p : params) {
			arr.put(p);
		}
		sendMessage(new JSONObject().put("method", endpoint + '.' + method).put("params", arr).toString());
	}
	
	/**
	 * Send message.
	 *
	 * @param msg the msg
	 */
	synchronized void sendMessage(String msg) {
		try {
			session.getBasicRemote().sendText(msg);
		} catch (IOException e) {
			try {
				session.close();
			} catch (IOException e1) {
				// Ignore if already closed
			}
		} catch(IllegalStateException a) {
			a.printStackTrace();
		} 
	}

	/**
	 * Hash code.
	 *
	 * @return the int
	 */
	@Override
	public int hashCode() {
		return Objects.hash(session.getId());
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
		if (!(obj instanceof WebsocketConnection))
			return false;
		WebsocketConnection other = (WebsocketConnection) obj;
		return Objects.equals(session.getId(), other.session.getId());
	}
}
