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
	
	private final Session session;
	
	private class ListenerCloser<I, R> {
		public SDSDEvent<I, R> event;
		public I identifier;
		
		public ListenerCloser(SDSDEvent<I, R> event, I identifier) {
			this.event = event;
			this.identifier = identifier;
		}
		
		public Set<ListenerCloser<?,?>> computeSet(String i, Set<ListenerCloser<?,?>> ol) {
			if(ol == null) ol = new HashSet<>();
			ol.add(this);
			return ol;
		}
		
		public Set<ListenerCloser<?,?>> computeUnset(String i, Set<ListenerCloser<?,?>> ol) {
			if(ol != null) {
				ol.remove(this);
				if(ol.isEmpty())
					ol = null;
			}
			return ol;
		}
		
		public void close() {
			event.unsetListener(identifier, session.getId());
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(event, identifier);
		}
		
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
	
	private final ConcurrentHashMap<String, Set<ListenerCloser<?, ?>>> openListeners = new ConcurrentHashMap<>();
	private ListenerCloser<Service, ServiceInstance> instanceStoppedListener = null;
	
	public WebsocketConnection(Session session) {
		this.session = session;
	}
	
	public String getId() {
		return session.getId();
	}
	
	@Override
	public Object observerId() {
		return session.getId();
	}
	
	@Override
	public void accept(ServiceInstance t) {
		unsetInstanceListeners(t.getToken());
	}

	@Override
	public void set(SDSDEvent<Service, ServiceInstance> event, Service identifier) {
		this.instanceStoppedListener = new ListenerCloser<Service, ServiceInstance>(event, identifier);
	}

	@Override
	public void unset(SDSDEvent<Service, ServiceInstance> event, Service identifier) {
		this.instanceStoppedListener = null;
	}
	
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
	public void unsetInstanceListeners(String instanceToken) {
		Set<ListenerCloser<?, ?>> remove = openListeners.remove(instanceToken);
		if(remove != null) remove.forEach(ListenerCloser::close);
	}
	
	void onClose() {
		unsetAllListeners();
	}
	
	@FunctionalInterface
	public interface CheckedFunction<T, R> {
		R apply(T t) throws Throwable;
	}
	
	public <I, R> SDSDListener<I, R> listener(String endpoint, String method, @Nullable String instanceToken, 
			@Nullable String identifier, CheckedFunction<R, ?> callback) throws SDSDException {
		return new WSListener<I, R>(endpoint, method, instanceToken, identifier, callback);
	}
	
	public class WSListener<I, R> implements SDSDListener<I, R> {
		private String endpoint, method, instanceToken, identifier;
		private CheckedFunction<R, ?> callback;
		
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
		
		@Override
		public Object observerId() {
			return session.getId();
		}
		
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
		
		@Override
		public void set(SDSDEvent<I, R> event, I identifier) {
			ListenerCloser<I, R> lc = new ListenerCloser<I, R>(event, identifier);
			openListeners.compute(instanceToken != null ? instanceToken : "", lc::computeSet);
		}
		
		@Override
		public void unset(SDSDEvent<I, R> event, I identifier) {
			ListenerCloser<I, R> lc = new ListenerCloser<I, R>(event, identifier);
			openListeners.compute(instanceToken != null ? instanceToken : "", lc::computeUnset);
		}
	}
	
	public void send(String endpoint, String method, Object...params) {
		JSONArray arr = new JSONArray();
		for(Object p : params) {
			arr.put(p);
		}
		sendMessage(new JSONObject().put("method", endpoint + '.' + method).put("params", arr).toString());
	}
	
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

	@Override
	public int hashCode() {
		return Objects.hash(session.getId());
	}

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
