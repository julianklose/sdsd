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

	public UserEndpoint(ApplicationLogic application) {
		super(application);
	}

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

	private JSONObject createStatus(User user) {
		return new JSONObject().put("username", user.getName());
	}
	
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
	
	private static JSONObject printLog(LogEntry log) {
		return new JSONObject()
				.put("time", isoUTC(log.getTime()))
				.put("type", log.getLevel())
				.put("text", log.getText());
	}
	
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
