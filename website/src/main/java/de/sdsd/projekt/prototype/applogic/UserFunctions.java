package de.sdsd.projekt.prototype.applogic;

import java.io.IOException;
import java.util.Collection;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import de.sdsd.projekt.prototype.data.SDSDException;
import de.sdsd.projekt.prototype.data.User;
import de.sdsd.projekt.prototype.data.UserManager;

/**
 * Provides functions regarding users.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class UserFunctions {
	private final ApplicationLogic app;
	private final MongoCollection<Document> mongo;
	private final UserManager manager;

	UserFunctions(ApplicationLogic app) {
		this.app = app;
		this.mongo = app.mongo.sdsd.getCollection("user");
		mongo.createIndex(Indexes.ascending(User.NAME), new IndexOptions().unique(true));
		this.manager = new UserManager(app, app.executor, mongo.find());
	}
	
	@CheckForNull
	public User getUser(String username) {
		return manager.getUser(username);
	}
	
	public boolean updateUser(User user, Bson update) {
		return mongo.updateOne(user.filter(), update).wasAcknowledged();
	}
	
	public User tryLogin(String username, String password) throws SDSDException, IOException {
		if(username == null || username.isEmpty()) throw new SDSDException("login: No username specified");
		if(password == null || password.isEmpty()) throw new SDSDException("login: No password specified");
		User user = manager.getUser(username);
		if(user == null) {
			manager.updateList(mongo.find());
			user = manager.getUser(username);
		}
		if(user == null)
			throw new SDSDException("login: user(" + username + ") not found");
		
		if(!user.checkPassword(password))
			throw new SDSDException("login: incorrect password for user(" + user.getName() + ")");
		return user;
	}
	
	public User login(String sessionId, String username, String password) throws SDSDException, IOException {
		if(sessionId == null) throw new NullPointerException("sessionId is null");
		User user = tryLogin(username, password);
		app.sessions.setUser(sessionId, user);
		return user;
	}

	public User register(String sessionId, String username, String password, String email) throws SDSDException, IOException {
		if(sessionId == null) throw new NullPointerException("sessionId is null");
		if(username == null || username.isEmpty()) throw new SDSDException("Username must not be empty");
		if(password == null || password.isEmpty()) throw new SDSDException("Password must not be empty");
		if(email == null || email.isEmpty()) throw new SDSDException("Email must not be empty");
		//check if user already exists
		if (mongo.countDocuments(User.filter(username)) >= 1) {
			throw new SDSDException("Account name already exists.");
		}

		//add user to database
		Document doc = User.create(username, password, email);
		mongo.insertOne(doc);
		manager.updateList(mongo.find());
		
		return login(sessionId, username, password);
	}
	
	public boolean changePassword(User user, String oldpw, String newpw) throws SDSDException {
		if(newpw == null || newpw.isEmpty()) throw new SDSDException("Password must not be empty");
		Bson update = user.changePassword(oldpw, newpw);
		if(update == null) throw new SDSDException("changePassword: incorrect password for user(" + user.getName() + ")");
		return mongo.updateOne(user.filter(), update).wasAcknowledged();
	}

	public boolean logout(String sessionId, User user) {
		if(sessionId == null) throw new NullPointerException("sessionId is null");
		app.sessions.setUser(sessionId, null);
		return true;
	}
	
	public boolean unregister(@Nullable String sessionId, User user) throws IOException {
		app.service.clearAll(user);
		app.list.clearAll(user);
		app.agrirouter.offboard(user);
		app.triple.clearAll(user);
		if(sessionId != null) logout(sessionId, user);
		if(mongo.deleteOne(user.filter()).wasAcknowledged()) {
			manager.updateList(mongo.find());
			return true;
		}
		return false;
	}
	
	public boolean adminLogin(String sessionId, String password) throws SDSDException {
		if(sessionId == null) throw new NullPointerException("sessionId is null");
		if(!app.settings.getString("adminPassword").equals(password))
			throw new SDSDException("login: incorrect admin password");
		app.sessions.setAdmin(sessionId, true);
		return true;
	}
	
	public boolean isAdmin(String sessionId) {
		return sessionId != null ? app.sessions.isAdmin(sessionId) : false;
	}
	
	public boolean adminLogout(String sessionId) {
		if(sessionId == null) throw new NullPointerException("sessionId is null");
		app.sessions.setAdmin(sessionId, false);
		return true;
	}
	
	public User adminUserLogin(String sessionId, String username) throws IOException, SDSDException {
		if(sessionId == null) throw new NullPointerException("sessionId is null");
		User user = manager.getUser(username);
		if(user == null) 
			throw new SDSDException("adminUserLogin: user(" + username + ") not found");
		
		app.sessions.setUser(sessionId, user);
		return user;
	}
	
	public boolean adminUserResetPassword(User user, String newpw) throws SDSDException {
		if(newpw.isEmpty()) throw new SDSDException("Password must not be empty");
		Bson update = user.resetPassword(newpw);
		return mongo.updateOne(user.filter(), update).wasAcknowledged();
	}
	
	public Collection<User> listUsers(boolean update) {
		if(update)
			manager.updateList(mongo.find());
		return manager.getAll();
	}
	
	public void connectAllMqtt() {
		manager.connectAllMqtt();
	}
	
	public void disconnectAllMqtt() {
		manager.disconnectAllMqtt();
	}
}
