package de.sdsd.projekt.api;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.sdsd.projekt.api.ServiceAPI.JsonRpcException;

/**
 * The Class WebsocketClient.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
@ClientEndpoint
public class WebsocketClient extends Client {
	private static final URI SDSD_WS = URI.create("wss://app.sdsd-projekt.de/websocket/sdsd"),
			SDSD_WS_LOCAL = URI.create("ws://localhost:8081/websocket/sdsd");
	
	private Session clientsession = null;
	private URI reopen;
	private final Map<String, CompletableFuture<JSONObject>> requests = new ConcurrentHashMap<>(3);
	private final Map<SDSDListenerKey, SDSDListener> eventListener = new ConcurrentHashMap<>();
	
	/**
	 * Instantiates a new websocket client.
	 *
	 * @param local if the SDSD website is accessible on localhost
	 * @throws DeploymentException the deployment exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public WebsocketClient(boolean local) throws DeploymentException, IOException {
		reopen = local ? SDSD_WS_LOCAL : SDSD_WS;
		open();
	}
	
	private void open() throws DeploymentException, IOException {
		if(reopen != null)
			ContainerProvider.getWebSocketContainer().connectToServer(this, reopen);
	}
	
	@OnOpen
	public void onOpen(Session session, EndpointConfig config) {
		System.out.println("onOpen: " + session.getId());
		if(clientsession != null && !clientsession.getId().equals(session.getId())) {
			try {
				clientsession.close();
			} catch (IOException e) {
				
			}
		}
		clientsession = session;
	}
	
	@OnClose
	public void onClose(Session session, CloseReason reason) {
		System.out.format("onClose: %s with reason: %s(%d)\n", session.getId(), reason.getReasonPhrase(), reason.getCloseCode().getCode());
		if(clientsession != null && clientsession.getId().equals(session.getId())) {
			clientsession = null;
			while(reopen != null) {
				try {
					open();
					for(SDSDListener l : eventListener.values()) {
						sendEventListener(l);
					}
					break;
				} catch (DeploymentException | IOException | JsonRpcException e) {
					System.err.println(e.getMessage());
				}
				try {
					Thread.sleep(60000);
				} catch (InterruptedException e1) {
					break;
				}
			}
		}
	}
	
	@OnError
	public void onError(Session session, Throwable error) {
		error.printStackTrace();
	}
	
	@OnMessage
	public void onMessage(String message, Session session) {
		try {
			JSONObject msg = new JSONObject(message);
			if(msg.has("endpoint")) {
				SDSDListener listener = eventListener.get(new SDSDListenerKey(msg));
				System.out.println("Call " + new SDSDListenerKey(msg) + ": " + (listener != null));
				if(listener != null)
					callListenerAsync(listener, msg.optJSONArray("params"));
			}
			else {
				CompletableFuture<JSONObject> future = requests.get(msg.optString("id"));
				if(future != null)
					future.complete(msg);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private void callListenerAsync(Consumer<JSONArray> listener, JSONArray params) {
		new Thread() {
			public void run() {
				listener.accept(params);
			}
		}.start();
	}
	
	/**
	 * Send message.
	 *
	 * @param message the message
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void sendMessage(String message) throws IOException {
		clientsession.getBasicRemote().sendText(message);
	}
	
	@Override
	public JSONObject execute(String endpoint, String method, String token, Object... parameters) throws JsonRpcException {
		String id = UUID.randomUUID().toString();
		try {
			JSONObject request = request(id, endpoint, method, parameters)
					.put("token", token);
			
			CompletableFuture<JSONObject> future = new CompletableFuture<>();
			requests.put(id, future);
			clientsession.getBasicRemote().sendText(request.toString());
			JSONObject resp = future.get(10, TimeUnit.SECONDS);
			
			if(resp.has("error"))
				throw new JsonRpcException(resp.getJSONObject("error"));

			if(resp.has("result"))
				return resp.getJSONObject("result");
			
			throw new JsonRpcException(new JSONObject()
					.put("code", -32000)
					.put("message", "Invalid Result"));
		} catch (Throwable e) {
			if(e instanceof JsonRpcException)
				throw (JsonRpcException)e;
			requests.remove(id);
			throw new JsonRpcException(new JSONObject()
					.put("code", -32603)
					.put("message", e.getMessage()));
		}
	}
	
	/**
	 * Class for unique identifiable listener.
	 *
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private static class SDSDListenerKey {
		final String endpoint, name, token, identifier;
		
		/**
		 * Instantiates a new SDSD listener key.
		 *
		 * @param endpoint the endpoint
		 * @param name the name
		 * @param token the token
		 * @param identifier the identifier
		 */
		public SDSDListenerKey(String endpoint, String name, @Nullable String token, @Nullable String identifier) {
			this.endpoint = endpoint;
			this.name = name;
			this.token = token != null ? token : "";
			this.identifier = identifier != null ? identifier : "";
		}
		
		/**
		 * Instantiates a new SDSD listener key.
		 *
		 * @param msg the JSON-RPC message
		 */
		SDSDListenerKey(JSONObject msg) {
			this.endpoint = msg.optString("endpoint");
			this.name = msg.optString("method");
			this.token = msg.optString("token");
			this.identifier = msg.optString("identifier");
		}
		
		@Override
		public String toString() {
			return String.format("SDSDListener(%s, %s, %s, %s)", endpoint, name, token, identifier);
		}

		@Override
		public int hashCode() {
			return Objects.hash(endpoint, identifier, name, token);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof SDSDListenerKey))
				return false;
			SDSDListenerKey other = (SDSDListenerKey) obj;
			return Objects.equals(endpoint, other.endpoint) && Objects.equals(identifier, other.identifier)
					&& Objects.equals(name, other.name) && Objects.equals(token, other.token);
		}
	}
	
	/**
	 * Base class for SDSD listener.
	 *
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static abstract class SDSDListener extends SDSDListenerKey implements Consumer<JSONArray> {
		
		/**
		 * Instantiates a new SDSD listener.
		 *
		 * @param endpoint the endpoint
		 * @param name the name
		 * @param token the token
		 * @param identifier the identifier
		 */
		public SDSDListener(String endpoint, String name, @Nullable String token, String identifier) {
			super(endpoint, name, token, identifier);
		}
	}
	
	private void sendEventListener(SDSDListener listener) throws JsonRpcException {
		execute(listener.endpoint, "set" + StringUtils.capitalize(listener.name) + "Listener", listener.token, listener.identifier);
	}
	
	/**
	 * Sets the event listener.
	 *
	 * @param listener the new event listener
	 * @throws JsonRpcException the json rpc exception
	 */
	public void setEventListener(SDSDListener listener) throws JsonRpcException {
		System.out.println("Set " + listener);
		sendEventListener(listener);
		eventListener.put(listener, listener);
	}
	
	/**
	 * Unset event listener.
	 *
	 * @param endpoint the endpoint
	 * @param name the name
	 * @param token the token
	 * @param identifier the identifier
	 * @throws JsonRpcException the json rpc exception
	 */
	public void unsetEventListener(String endpoint, String name, @Nullable String token, @Nullable String identifier) throws JsonRpcException {
		System.out.println("UnSet " + new SDSDListenerKey(endpoint, name, token, identifier));
		eventListener.remove(new SDSDListenerKey(endpoint, name, token, identifier));
		execute(endpoint, "unset" + StringUtils.capitalize(name) + "Listener", token, identifier);
	}
	
	/**
	 * Clear event listeners.
	 *
	 * @param token the token
	 */
	public void clearEventListeners(@Nullable String token) {
		if(token == null) eventListener.clear();
		else {
			Iterator<SDSDListenerKey> it = eventListener.keySet().iterator();
			while(it.hasNext()) {
				if(token.equals(it.next().token))
					it.remove();
			}
		}
	}

	@Override
	public void close() throws IOException {
		reopen = null;
		eventListener.clear();
		if(clientsession != null) 
			clientsession.close();
	}
	
}
