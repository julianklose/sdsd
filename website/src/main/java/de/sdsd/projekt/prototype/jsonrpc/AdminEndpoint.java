package de.sdsd.projekt.prototype.jsonrpc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import de.sdsd.projekt.prototype.applogic.ApplicationLogic;
import de.sdsd.projekt.prototype.data.ARConn;
import de.sdsd.projekt.prototype.data.SDSDException;
import de.sdsd.projekt.prototype.data.User;

/**
 * JSONRPC-Endpoint for admin functions.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class AdminEndpoint extends JsonRpcEndpoint {

	public AdminEndpoint(ApplicationLogic application) {
		super(application);
	}

	public JSONObject adminLogin(HttpServletRequest req, HttpServletResponse res, String password) throws JsonRpcException {
		try {
			boolean success = application.user.adminLogin(getSetSessionId(req, res), password);
			System.out.println("adminLogin: " + (success ? "success" : "failed"));
			return success(success);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	public JSONObject adminStatus(HttpServletRequest req) throws JsonRpcException {
		try {
			return new JSONObject().put("isLogin", application.user.isAdmin(getSessionId(req)));
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	public JSONObject adminLogout(HttpServletRequest req, HttpServletResponse res) throws JsonRpcException {
		try {
			boolean success = application.user.adminLogout(getSessionId(req));
			System.out.println("adminLogout: " + (success ? "success" : "failed"));
			return success(success);
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	public JSONObject adminCreateUser(HttpServletRequest req, HttpServletResponse res, JSONObject regForm) throws JsonRpcException {
		try {
			if(!application.user.isAdmin(getSessionId(req)))
				throw new NoLoginException();
			else {
				User user = application.user.register(getSetSessionId(req, res), 
						regForm.getString("username"), regForm.getString("password"), regForm.getString("email"));
				System.out.println("adminCreateUser: user(" + user.getName() + ") email(" + user.getEmail() + ")");
				return new JSONObject().put("success", true);
			}
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	public JSONObject adminListUsers(HttpServletRequest req, boolean update) throws JsonRpcException {
		try {
			String sessionId = getSessionId(req);
			if(!application.user.isAdmin(sessionId))
				throw new NoLoginException();
			else {
				User login = application.getUser(sessionId);
				System.out.println("adminListUsers: update(" + update + ")");
				JSONArray arr = new JSONArray();
				int num = 0;
				for(User user : application.user.listUsers(update)) {
					ARConn arConn = user.agrirouter();
					arr.put(new JSONObject()
							.put("num", num++)
							.put("username", user.getName())
							.put("created", isoUTC(user.getCreated()))
							.put("email", user.getEmail())
							.put("agrirouterId", arConn != null ? arConn.getOwnEndpointId() : "")
							.put("adminLogin", user.equals(login)));
				}
				return new JSONObject().put("users", arr);
			}
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	public JSONObject adminUserLogin(HttpServletRequest req, String username) throws JsonRpcException {
		try {
			String sessionId = getSessionId(req);
			if(!application.user.isAdmin(sessionId))
				throw new NoLoginException();
			else {
				User user = application.user.adminUserLogin(sessionId, username);
				System.out.println("adminUserLogin: user(" + user.getName() + ")");
				return success(true);
			}
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	public JSONObject adminUserResetPassword(HttpServletRequest req, String username, String password) throws JsonRpcException {
		try {
			String sessionId = getSessionId(req);
			User user = application.user.getUser(username);
			if(!application.user.isAdmin(sessionId))
				throw new NoLoginException();
			else if(user == null)
				throw new SDSDException("User not found: " + username);
			else {
				System.out.println("adminUserResetPassword: user(" + user.getName() + ")");
				return success(application.user.adminUserResetPassword(user, password));
			}
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	public JSONObject adminConnectAllMqtt(HttpServletRequest req) throws JsonRpcException {
		try {
			String sessionId = getSessionId(req);
			if(!application.user.isAdmin(sessionId))
				throw new NoLoginException();
			else {
				System.out.println("adminConnectAllMqtt");
				application.user.connectAllMqtt();
				return success(true);
			}
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	public JSONObject adminDisconnectAllMqtt(HttpServletRequest req) throws JsonRpcException {
		try {
			String sessionId = getSessionId(req);
			if(!application.user.isAdmin(sessionId))
				throw new NoLoginException();
			else {
				System.out.println("adminDisconnectAllMqtt");
				application.user.disconnectAllMqtt();
				return success(true);
			}
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
}
