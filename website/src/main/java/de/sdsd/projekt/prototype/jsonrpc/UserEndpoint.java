package de.sdsd.projekt.prototype.jsonrpc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import de.sdsd.projekt.prototype.applogic.ApplicationLogic;
import de.sdsd.projekt.prototype.data.LogEntry;
import de.sdsd.projekt.prototype.data.SDSDException;
import de.sdsd.projekt.prototype.data.User;
import de.sdsd.projekt.prototype.data.Util;
import de.sdsd.projekt.prototype.websocket.WebsocketConnection;

/**
 * JSONRPC-Endpoint for user functions.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class UserEndpoint extends JsonRpcEndpoint {

	/**
	 * Instantiates a new user endpoint.
	 *
	 * @param application the application
	 */
	public UserEndpoint(ApplicationLogic application) {
		super(application);
	}

	/**
	 * @deprecated
	 * Reg.
	 *
	 * @param req the current request
	 * @param res the res
	 * @param regForm the reg form
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject reg(HttpServletRequest req, HttpServletResponse res, JSONObject regForm) throws JsonRpcException {
		User user = null;
		try {
			throw new SDSDException("Function disabled!");
//			user = application.reg(getSetSessionId(req, res), 
//					regForm.getString("username"), regForm.getString("password"), regForm.getString("email"));
//			System.out.println("register: user(" + user.getName() + ") email(" + user.getEmail() + ")");
//			return new JSONObject().put("success", true);
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}

	/**
	 * Creates a JSON object with the users name as an attribute and returns it.
	 *
	 * @param user the user
	 * @return JSON object with the current user status
	 */
	private JSONObject createStatus(User user) {
		return new JSONObject().put("username", user.getName());
	}
	
	/**
	 * If there is a user associated with the session id a JSON object with the users name is returned, else an empty JSON Object is returned. 
	 *
	 * @param req the current request
	 * @return JSON object with the current user status
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject status(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			if(user != null) return createStatus(user);
			else return new JSONObject();
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * User login. Starts the users session and returns a JSON object with the current user status.
	 *
	 * @param req http servlet request including userdata
	 * @param res http servlet response
	 * @param username the users name
	 * @param password the users password
	 * @return the JSON object with the current user status
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject login(HttpServletRequest req, HttpServletResponse res, String username, String password) throws JsonRpcException {
		User user = null;
		try {
			user = application.user.login(getSetSessionId(req, res), username, password);
			String remoteIP = req.getHeader("x-forwarded-for");
			System.out.println("login: user(" + user.getName() + ") ip(" + remoteIP + ")");
			application.logInfo(user, "Login from " + remoteIP);
			return createStatus(user);
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * User logout. Ends the users session and returns a JSON object with the current user status.
	 *
	 * @param req http servlet request including userdata
	 * @param res http servlet response
	 * @return the JSON object with success attribute
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject logout(HttpServletRequest req, HttpServletResponse res) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("logout: user(" + (user != null ? user.getName() : "none") + ")");
			
			if (user == null)
				throw new NoLoginException();
			else {
				boolean ok = application.user.logout(getSessionId(req), user);
				return new JSONObject().put("success", ok);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Change the users password.
	 *
	 * @param req http servlet request including userdata
	 * @param oldpw the users current password
	 * @param newpw the users new password
	 * @return the JSON object with success attribute
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject changePassword(HttpServletRequest req, String oldpw, String newpw) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			String remoteIP = req.getHeader("x-forwarded-for");
			System.out.println("changePassword: user(" + (user != null ? user.getName() : "none") + ") ip(" + remoteIP + ")");
			
			if (user == null)
				throw new NoLoginException();
			else {
				boolean ok = application.user.changePassword(user, oldpw, newpw);
				if(ok) application.logInfo(user, "Password changed from " + remoteIP);
				return new JSONObject().put("success", ok);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Unregisters the user associated with the current session id.
	 *
	 * @param req http servlet request including userdata
	 * @param res http servlet response
	 * @return the JSON object with success attribute
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject unregister(HttpServletRequest req, HttpServletResponse res) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("unregister: user(" + (user != null ? user.getName() : "none") + ")");
			
			if(user == null)
				throw new NoLoginException();
			else
				return new JSONObject().put("success", application.user.unregister(getSessionId(req), user));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * creates a JSON Obect from a LogEntry and returns it. Includes the time, type and text of the entry.
	 *
	 * @param log the log which data is saved as a JSON object
	 * @return the JSON object with the log data
	 */
	private static JSONObject printLog(LogEntry log) {
		return new JSONObject()
				.put("time", isoUTC(log.getTime()))
				.put("type", log.getLevel())
				.put("text", log.getText());
	}
	
	/**
	 * Returns a list of the users logs.
	 *
	 * @param req http servlet request including userdata
	 * @return the JSON object including the users log data
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject listLogs(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("listLogs: user(" + (user != null ? user.getName() : "none") + ")");

			if (user == null)
				throw new NoLoginException();
			else {
				JSONArray array = application.list.logs.getList(user).stream()
						.map(UserEndpoint::printLog)
						.collect(Util.toJSONArray());
				return new JSONObject().put("logs", array);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Sets a listener that triggers when new logs are added
	 *
	 * @param req http servlet request including userdata
	 * @param conn websocket connnection
	 * @param identifier the identifier
	 * @return the JSON object including a success attribute
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject setLogListener(HttpServletRequest req, WebsocketConnection conn, String identifier) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.format("setLogListener: user(%s) connection(%s)\n", (user != null ? user.getName() : "none"), conn.getId());
			
			if (user == null)
				throw new NoLoginException();
			else {
				application.list.logs.setListener(user, conn.listener("user", "log", null, identifier, UserEndpoint::printLog));
				return success(true);
			}
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Deletes the users logs.
	 *
	 * @param req http servlet request including userdata
	 * @return the JSON object including a success attribute
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject clearLogs(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("clearLogs: user(" + (user != null ? user.getName() : "none") + ")");

			if (user == null)
				throw new NoLoginException();
			else {
				return success(application.list.logs.clear(user));
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
}
