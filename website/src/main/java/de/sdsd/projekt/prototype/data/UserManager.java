package de.sdsd.projekt.prototype.data;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.CheckForNull;

import org.bson.Document;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONException;
import org.json.JSONObject;

import de.sdsd.projekt.agrirouter.ARConnection;
import de.sdsd.projekt.agrirouter.ARConnection.MqttConnection;
import de.sdsd.projekt.agrirouter.ARConnection.RestConnection;
import de.sdsd.projekt.agrirouter.request.feed.ARMsg;
import de.sdsd.projekt.agrirouter.request.feed.ARPushNotificationReceiver;
import de.sdsd.projekt.prototype.applogic.ApplicationLogic;

/**
 * Manager of users and agrirouter connections.
 * If a user needs his agrirouter connection, it is created and stored here. 
 * The manager checks every {@value #CONNECTION_CHECK_INTERVAL} ms if a connection was not used for more 
 * than {@value #CONNECTION_TIMEOUT} ms and closes it automatically.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class UserManager implements AutoCloseable {
	
	private final ApplicationLogic app;
	private final ScheduledExecutorService executor;
	private LinkedHashMap<String, User> users;
	private final ConcurrentHashMap<String, ARConnection> connections = new ConcurrentHashMap<>();
	private final ScheduledFuture<?> connChecker;
	
	public UserManager(ApplicationLogic app, ScheduledExecutorService executor, Iterable<Document> users) {
		this.app = app;
		this.executor = executor;
		this.connChecker = executor.scheduleAtFixedRate(new ConnectionChecker(), 
				10000, RestConnection.CONNECTION_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
		updateList(users);
	}
	
	public void updateList(Iterable<Document> users) {
		LinkedHashMap<String, User> map = new LinkedHashMap<>();
		for(Document doc : users) {
			User user = new User(doc, this);
			map.put(user.getName(), user);
		}
		this.users = map;
	}
	
	public void updateUser(User user, Document userDoc) {
		user.updateFromDB(userDoc);
	}
	
	@CheckForNull
	public User getUser(String username) {
		return users.get(username);
	}
	
	public Collection<User> getAll() {
		return Collections.unmodifiableCollection(users.values());
	}
	
	ARConnection getARConnection(User user, JSONObject onboardingInfo) 
			throws IOException, JSONException, GeneralSecurityException, MqttException {
		ARConnection conn = connections.get(user.getName());
		if(conn != null) return conn;
		conn = ARConnection.create(onboardingInfo, executor);
		conn.setPushNotificationReceiver(new PushNotificationCallback(user));
		connections.put(user.getName(), conn);
		return conn;
	}
	
	ARConnection initARConnection(User user, JSONObject onboardingInfo) 
			throws IOException, JSONException, GeneralSecurityException, MqttException {
		closeARConnection(user);
		ARConnection conn = ARConnection.create(onboardingInfo, executor);
		conn.setPushNotificationReceiver(new PushNotificationCallback(user));
		connections.put(user.getName(), conn);
		return conn;
	}
	
	public void closeARConnection(User user) {
		ARConnection conn = connections.get(user.getName());
		if(conn != null) {
			connections.remove(user.getName());
			try {
				conn.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void connectAllMqtt() {
		for(User user : users.values()) {
			ARConn arconn = user.agrirouter();
			if(arconn != null && arconn.isMQTT()) {
				try {
					System.out.println(user.username + ": MQTT connect");
					arconn.conn();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void disconnectAllMqtt() {
		Iterator<ARConnection> it = connections.values().iterator();
		while(it.hasNext()) {
			ARConnection conn = it.next();
			if(conn instanceof MqttConnection) {
				try {
					conn.close();
					it.remove();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Callback container for agrirouter push notifications.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private class PushNotificationCallback extends ARPushNotificationReceiver {
		private final User user;
		
		public PushNotificationCallback(User user) {
			this.user = user;
			System.out.println(user.getName() + ": set PushNotificationReceiver");
		}

		@Override
		public void onReceive(ARMsg msg) {
			app.agrirouter.onPushNotificationMsg(user, msg);
		}

		@Override
		public void onError(Exception e) {
			app.agrirouter.onPushNotificationError(user, e);
		}
	}
	
	@Override
	public void close() throws Exception {
		connChecker.cancel(false);
		Iterator<ARConnection> it = connections.values().iterator();
		while(it.hasNext()) {
			it.next().close();
			it.remove();
		}
	}
	
	/**
	 * Runnable to close http connections that weren't used recently.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private class ConnectionChecker implements Runnable {

		@Override
		public void run() {
			long now = System.currentTimeMillis();
			Iterator<ARConnection> it = connections.values().iterator();
			while(it.hasNext()) {
				ARConnection conn = it.next();
				if(conn instanceof RestConnection) {
					if(now >= ((RestConnection)conn).getTimeout()) {
						it.remove();
						try {
							conn.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		
	}
	
}
