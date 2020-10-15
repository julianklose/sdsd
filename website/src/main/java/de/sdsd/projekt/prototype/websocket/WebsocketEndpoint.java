package de.sdsd.projekt.prototype.websocket;

import java.util.HashMap;
import java.util.Map;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.jabsorb.JSONRPCBridge;
import org.jabsorb.JSONRPCResult;
import org.jabsorb.localarg.LocalArgController;
import org.jabsorb.localarg.LocalArgResolveException;
import org.jabsorb.localarg.LocalArgResolver;
import org.json.JSONObject;

/**
 * Websocket endpoint to provide websocket connections to website clients and services for JSON-RPC calls.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
@ServerEndpoint("/sdsd")
public class WebsocketEndpoint {
	
	static {
		LocalArgController.registerLocalArgResolver(WebsocketConnection.class, WebsocketConnection.class, new LocalArgResolver() {
			public Object resolveArg(Object context) throws LocalArgResolveException {
				if (context instanceof WebsocketConnection) 
					return context;
				else
					throw new LocalArgResolveException("Couldn't resolve websocket connection.");
			}
		});
	}
	
	/** The Constant sessions. */
	private static final Map<String, String> sessions = new HashMap<>();
	
	/** The Constant connections. */
	private static final Map<String, WebsocketConnection> connections = new HashMap<>();
	
	/**
	 * On open.
	 *
	 * @param session the session
	 * @param config the config
	 */
	@OnOpen
	public void onOpen(Session session, EndpointConfig config) {
		connections.put(session.getId(), new WebsocketConnection(session));
		System.out.println("Websocket opened: " + session.getId());
	}

	/**
	 * On close.
	 *
	 * @param session the session
	 * @param reason the reason
	 */
	@OnClose
	public void onClose(Session session, CloseReason reason) {
		WebsocketConnection connection = connections.remove(session.getId());
		if(connection != null)
			connection.onClose();
		buffer.remove(session.getId());
		System.out.format("Websocket %s closed: %s(%d)\n", session.getId(), reason.getReasonPhrase(), reason.getCloseCode().getCode());
	}
	
	/**
	 * On error.
	 *
	 * @param session the session
	 * @param error the error
	 */
	@OnError
	public void onError(Session session, Throwable error) {
		System.err.format("Websocket %s error: (%s)%s\n", session.getId(), error.getClass().getName(), error.getMessage());
	}
	
	/** The buffer. */
	private final Map<String, StringBuffer> buffer = new HashMap<>();
	
	/**
	 * On message.
	 *
	 * @param msgString the msg string
	 * @param last the last
	 * @param session the session
	 */
	@OnMessage
	public void onMessage(String msgString, boolean last, Session session) {
		//System.out.format("onMessage from %s: length(%d) last(%b)\n", session.getId(), msgString.length(), last);
		WebsocketConnection conn = connections.get(session.getId());
		if(conn == null) connections.put(session.getId(), conn = new WebsocketConnection(session));
		
		if(last) {
			StringBuffer buf = buffer.remove(session.getId());
			if(buf != null && buf.length() > 0)
				msgString = buf.append(msgString).toString();
		} else {
			StringBuffer buf = buffer.get(session.getId());
			if(buf == null) buffer.put(session.getId(), buf = new StringBuffer());
			buf.append(msgString);
			return;
		}
		
		try {
			JSONObject msg = new JSONObject(msgString);
			
			String sdsdSessionId = (String) msg.remove("SDSDSESSION");
			if(sdsdSessionId != null)
				sessions.put(session.getId(), sdsdSessionId);
			else
				sdsdSessionId = sessions.get(session.getId());
			
			String token = (String) msg.remove("token");
			WebsocketServletRequest wsr = new WebsocketServletRequest(sdsdSessionId, token);
			
			if(msg.has("method")) {
				JSONRPCResult result = JSONRPCBridge.getGlobalBridge().call(new Object[] { wsr, conn }, msg);
				conn.sendMessage(result.toString());
			}
		} catch (Throwable e) {
			e.printStackTrace();
			conn.sendMessage(new JSONRPCResult(JSONRPCResult.CODE_REMOTE_EXCEPTION, null, e).toString());
		}
	}
	
}
