package de.sdsd.projekt.prototype.jsonrpc;

import de.sdsd.projekt.prototype.applogic.ApplicationLogic;
import de.sdsd.projekt.prototype.data.File;
import de.sdsd.projekt.prototype.data.SDSDException;
import de.sdsd.projekt.prototype.data.SDSDType;
import de.sdsd.projekt.prototype.data.StorageTask;
import de.sdsd.projekt.prototype.data.StorageTask.StorageTaskBuilder;
import de.sdsd.projekt.prototype.data.User;
import de.sdsd.projekt.prototype.data.Util;
import de.sdsd.projekt.prototype.util.FileSizeUtil;
import de.sdsd.projekt.prototype.websocket.WebsocketConnection;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import javax.servlet.http.HttpServletRequest;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * JSONRPC-Endpoint for file functions.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class FileEndpoint extends JsonRpcEndpoint {

	/**
	 * Instantiates a new file endpoint.
	 *
	 * @param application the application
	 */
	public FileEndpoint(ApplicationLogic application) {
		super(application);
	}
	
	/**
	 * Prints the file.
	 *
	 * @param file the file
	 * @return the JSON object
	 */
	private JSONObject printFile(File file) {
		User user = application.user.getUser(file.getUser());
		return new JSONObject()
				.put("id", file.getId().toHexString())
				.put("filename", file.getFilename())
				.put("source", file.getSource() != null ? application.list.endpoints.get(user, file.getSource()).getName() : null)
				.put("created", file.getCreated() != null ? isoUTC(file.getCreated()) : null)
				.put("lastmodified", file.getModified() != null ? isoUTC(file.getModified()) : null)
				.put("leveraged", file.getLeveraged() != null ? isoUTC(file.getLeveraged()) : null)
				.put("expires", file.getExpires() != null ? isoUTC(file.getExpires()) : null)
				.put("size", FileSizeUtil.humanReadableByteCount(file.getSize()))
				.put("type", file.getType())
				.put("validation", file.getValidation() != File.Validation.UNVALIDATED ? file.getValidation().name() : null)
				.put("coredata", file.isCoreData());
	}
	
	/**
	 * List files.
	 *
	 * @param req the req
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject listFiles(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("listFiles: user(" + (user != null ? user.getName() : "none") + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				JSONArray array = application.list.files.getList(user).stream()
						.sorted(File.CMP_RECENT)
						.map(this::printFile)
						.collect(Util.toJSONArray());
				return new JSONObject().put("files", array);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Sets the file listener.
	 *
	 * @param req the req
	 * @param conn the conn
	 * @param identifier the identifier
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject setFileListener(HttpServletRequest req, WebsocketConnection conn, String identifier) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.format("setFileListener: user(%s) connection(%s)\n", (user != null ? user.getName() : "none"), conn.getId());
			
			if (user == null)
				throw new NoLoginException();
			else {
				application.file.dataAdded.setListener(user, conn.listener("file", "file", null, identifier, this::printFile));
				return success(true);
			}
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Rename file.
	 *
	 * @param req the req
	 * @param fileid the fileid
	 * @param newName the new name
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject renameFile(HttpServletRequest req, String fileid, String newName) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("renameFile: user(" + (user != null ? user.getName() : "none") + ") file("
					+ fileid + ") newName(" + newName + ")");
			ObjectId fid = new ObjectId(fileid);
			
			if (user == null) 
				throw new NoLoginException();
			else if (!application.list.files.exists(user, fid))
				throw new SDSDException("File not found");
			else {
				File file = application.list.files.get(user, fid);
				String oldName = file.getFilename();
				if(oldName.equals(newName)) return success(true);
				if(application.list.files.update(user, file, file.rename(newName))) {
					application.triple.updateFile(user, file);
					application.logInfo(user, "Changed file name from \"" + oldName + "\" to " + newName);
					return success(true);
				}
				return success(false);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Change file type.
	 *
	 * @param req the req
	 * @param fileid the fileid
	 * @param type the type
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject changeFileType(HttpServletRequest req, String fileid, String type) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("changeFileType: user(" + (user != null ? user.getName() : "none") + ") file("
					+ fileid + ") type(" + type + ")");
			ObjectId fid = new ObjectId(fileid);
			
			if (user == null)
				throw new NoLoginException();
			else if (!application.list.files.exists(user, fid))
				throw new SDSDException("File not found");
			else if (!application.list.types.exists(null, type))
				throw new SDSDException("Type doesn't exist");
			else {
				SDSDType sdsdType = application.list.types.get(null, type);
				File file = application.list.files.get(user, fid);
				if(file.getType().equals(sdsdType.getUri())) return success(true);
				if(application.list.files.update(user, file, file.setType(type))) {
					application.file.reparseFile(user, file);
					application.logInfo(user, "Changed file type of \"" + file.getFilename() + "\" to " + sdsdType.getName());
					return success(true);
				}
				return success(false);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Sets the core data.
	 *
	 * @param req the req
	 * @param fileid the fileid
	 * @param coredata the coredata
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject setCoreData(HttpServletRequest req, String fileid, boolean coredata) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("setCoreData: user(" + (user != null ? user.getName() : "none") + ") file("
					+ fileid + ") coredata(" + coredata + ")");
			ObjectId fid = new ObjectId(fileid);
			
			if (user == null) 
				throw new NoLoginException();
			else if (!application.list.files.exists(user, fid))
				throw new SDSDException("File not found");
			else {
				File file = application.list.files.get(user, fid);
				boolean ok = application.list.files.update(user, file, file.setCoreData(coredata));
				if(ok) application.logInfo(user, (coredata ? "Set" : "Unset") + " \"" + file.getFilename() + "\" as core data");
				return success(ok);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Delete files.
	 *
	 * @param req the req
	 * @param files the files
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject deleteFiles(HttpServletRequest req, String[] files) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("deleteFiles: user(" + (user != null ? user.getName() : "none") + ") files("
					+ String.join(", ", files) + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				int count = 0;
				for(String fileid : files) {
					File deletedFile = application.list.files.get(user, fileid);
					if(application.file.deleteFile(user, deletedFile)) {
						application.logInfo(user, "File deleted: " + deletedFile.getFilename());
						++count;
					}
				}
				return new JSONObject().put("count", count);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * List storage tasks.
	 *
	 * @param req the req
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject listStorageTasks(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("listStorageTasks: user(" + (user != null ? user.getName() : "none") + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				JSONArray array = new JSONArray();
				for (StorageTask st : application.list.storageTasks.getList(user)) {
					JSONObject obj = new JSONObject()
							.put("id", st.getId().toHexString())
							.put("label", st.getLabel());
					
					if(st.getType() != null) obj.put("type", new JSONObject()
							.put("value", st.getType())
							.put("equals", st.isTypeEquals()));
					if(st.getSource() != null) obj.put("source", new JSONObject()
							.put("value", st.getSource())
							.put("equals", st.isSourceEquals()));
					if(st.getFrom() != null || st.getUntil() != null) {
						JSONObject created = new JSONObject();
						if(st.getFrom() != null) created.put("from", isoUTC(st.getFrom()));
						if(st.getUntil() != null) created.put("until", isoUTC(st.getUntil()));
						obj.put("created", created);
					}
					
					if(st.getStoreUntil() != null) obj.put("storeUntil", isoUTC(st.getStoreUntil()));
					else obj.put("storeFor", st.getStoreFor().toDays());
					array.put(obj);
				}
				return new JSONObject().put("storageTasks", array);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Delete storage tasks.
	 *
	 * @param req the req
	 * @param tasks the tasks
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject deleteStorageTasks(HttpServletRequest req, String[] tasks) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("deleteStorageTasks: user(" + (user != null ? user.getName() : "none") + ") tasks("
					+ String.join(", ", tasks) + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				int count = 0;
				for(String taskid : tasks) {
					StorageTask task = application.list.storageTasks.get(user, taskid);
					if(application.list.storageTasks.delete(user, taskid)) {
						++count;
						application.logInfo(user, "Storage task deleted: " + task.getLabel());
					}
				}
				return new JSONObject().put("count", count);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Clear storage tasks.
	 *
	 * @param req the req
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject clearStorageTasks(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("clearStorageTasks: user(" + (user != null ? user.getName() : "none") + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				boolean ok = application.list.storageTasks.clear(user);
				if(ok) application.logInfo(user, "Deleted all storage tasks");
				return new JSONObject().put("success", ok);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Adds the storage task.
	 *
	 * @param req the req
	 * @param task the task
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject addStorageTask(HttpServletRequest req, JSONObject task) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("addStorageTask: user(" + (user != null ? user.getName() : "none") + ") task("
					+ task.optString("label") + ")");

			if (user == null) 
				throw new NoLoginException();
			else {
				String s = task.optString("label");
				if(s == null || s.isEmpty()) throw new JSONException("StorageTask: label is required");
				StorageTaskBuilder builder = StorageTask.create(user, s);
				
				JSONObject o = task.optJSONObject("type");
				if(o != null && o.has("value")) {
					s = o.optString("value");
					if(s == null) throw new JSONException("StorageTask: value is required in type");
					if(!s.isEmpty())
						builder.setType(s, o.optBoolean("equals", true));
				}
				
				o = task.optJSONObject("source");
				if(o != null && o.has("value")) {
					s = o.optString("value");
					if(s == null) throw new JSONException("StorageTask: value is required in source");
					
					if(!s.isEmpty())
						builder.setSource(s, o.optBoolean("equals", true));
				}
				
				o = task.optJSONObject("created");
				if(o != null) {
					s = o.optString("from");
					if(s != null && !s.isEmpty()) builder.setFrom(Instant.parse(s));
					s = o.optString("until");
					if(s != null && !s.isEmpty()) builder.setUntil(Instant.parse(s));
				}
				
				Document d = null;
				int days = task.optInt("storeFor", -1);
				if(days > 0)
					d = builder.build(Duration.ofDays(days));
				
				s = task.optString("storeUntil");
				if(s != null && !s.isEmpty())
					d = builder.build(Instant.parse(s));
				
				if(d == null) throw new JSONException("StorageTask: set storeFor or storeUntil");
				StorageTask storageTask = application.list.storageTasks.add(user, d);
				application.logInfo(user, "Storage task added: " + storageTask.getLabel());
				return new JSONObject().put("success", true);
			}
		} catch (JSONException | DateTimeParseException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
}
