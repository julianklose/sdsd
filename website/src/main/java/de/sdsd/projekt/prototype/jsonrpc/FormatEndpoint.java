package de.sdsd.projekt.prototype.jsonrpc;

import java.util.Arrays;
import java.util.NoSuchElementException;

import javax.servlet.http.HttpServletRequest;

import org.bson.types.ObjectId;
import org.json.JSONException;
import org.json.JSONObject;

import de.sdsd.projekt.agrirouter.ARMessageType;
import de.sdsd.projekt.prototype.applogic.ApplicationLogic;
import de.sdsd.projekt.prototype.applogic.TripleFunctions;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.WikiFormat;
import de.sdsd.projekt.prototype.data.DraftFormat;
import de.sdsd.projekt.prototype.data.DraftItem;
import de.sdsd.projekt.prototype.data.SDSDException;
import de.sdsd.projekt.prototype.data.SDSDType;
import de.sdsd.projekt.prototype.data.User;
import de.sdsd.projekt.prototype.data.Util;
import de.sdsd.projekt.prototype.data.WikiClass;
import de.sdsd.projekt.prototype.data.WikiEntry;
import de.sdsd.projekt.prototype.data.WikiInstance;

/**
 * JSONRPC-Endpoint for format functions.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class FormatEndpoint extends JsonRpcEndpoint {

	public FormatEndpoint(ApplicationLogic application) {
		super(application);
	}
	
	public JSONObject listFormats(HttpServletRequest req) throws JsonRpcException {
		return listFormats(req, false);
	}
	
	public JSONObject listFormats(HttpServletRequest req, boolean draft) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.format("listFormats: user(%s) draft(%b)\n", user != null ? user.getName() : "none", draft);
			
			if (user == null) 
				throw new NoLoginException();
			else {
				JSONObject out = new JSONObject();
				
				out.put("formats", application.list.types.getList(null).stream()
						.map(type -> new JSONObject()
								.put("value", type.getUri())
								.put("label", type.getName())
								.put("artype", type.getARType().technicalMessageType())
								.put("author", type.getAuthor()))
						.collect(Util.toJSONArray()));
				
				if(draft) {
					out.put("drafts", application.list.draftFormats.getList(user).stream()
							.map(dr -> new Res(dr.getId().toHexString(), dr.getLabel()))
							.collect(Util.toJSONArray()));
				}
				
				return out;
			}
		} catch (JSONException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject listARTypes(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("format listARTypes: user(" + (user != null ? user.getName() : "none") + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				JSONObject out = new JSONObject();
				
				out.put("artypes", Arrays.stream(ARMessageType.values())
						.filter(t -> !t.technicalMessageType().isEmpty())
						.map(JsonRpcEndpoint::artype)
						.collect(Util.toJSONArray()));
				
				return out;
			}
		} catch (JSONException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}

	private static final String JS_CLASS="class", JS_COMMENT="description", 
			JS_MIME="mimetype", JS_ARTYPE="artype", 
			JS_PARSER="parser", JS_PARSE_COMMAND="parseCommand", JS_TEST_COMMAND="testCommand";
	
	public JSONObject get(HttpServletRequest req, String identifier) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("format get: user(" + (user != null ? user.getName() : "none") + ") identifier(" + identifier + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				WikiFormat res = new WikiFormat(identifier);
				if(!application.list.types.exists(null, res.getURI()))
					throw new SDSDException("Format '" + identifier + "' not found");
				
				SDSDType format = application.list.types.get(user, res.getURI());
				
				WikiEntry entry = application.wiki.get(res, false);
				if(entry instanceof WikiInstance)
					throw new SDSDException("The given identifier belongs to an instance");
				WikiClass wiki = (WikiClass) entry;
				
				String parser = format.getParser().orElse(null);
				String parseCommand = format.getParseCommand().orElse(null);
				if(parseCommand != null && parser != null && parser.length() > 0)
					parseCommand = parseCommand.replace(parser, "%path%");
				String testCommand = format.getTestCommand().orElse(null);
				if(testCommand != null && parser != null && parser.length() > 0)
					testCommand = testCommand.replace(parser, "%path%");
				
				return new JSONObject()
						.put(JS_CLASS, WikinormiaEndpoint.wikiHeadToJson(wiki))
						.put(JS_COMMENT, wiki.getComment().orElse(null))
						.put(JS_MIME, format.getMimeType())
						.put(JS_ARTYPE, format.getARType().technicalMessageType())
						.put(JS_PARSER, parser)
						.put(JS_PARSE_COMMAND, parseCommand)
						.put(JS_TEST_COMMAND, testCommand);
			}
		} catch (JSONException | NoSuchElementException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject getDraft(HttpServletRequest req, String formatID) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.format("format getDraft: user(%s) formatID(%s)\n", 
					user != null ? user.getName() : "none", formatID);
			
			if (user == null) 
				throw new NoLoginException();
			else {
				return application.list.draftFormats.get(user, formatID).getJson();
			}
		} catch (JSONException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject setDraft(HttpServletRequest req, String formatID, JSONObject input) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.format("format setDraft: user(%s) formatID(%s) title(%s)\n", 
					user != null ? user.getName() : "none", formatID, input.optString("label"));
			
			if (user == null) 
				throw new NoLoginException();
			else {
				ObjectId id = formatID != null ? new ObjectId(formatID) : null;
				
				String identifier = input.getString(DraftFormat.IDENTIFIER);
				if(identifier.isBlank()) throw new SDSDException("Identifier must not be empty");
				if(input.getString(DraftFormat.LABEL).isBlank()) throw new SDSDException("Label must not be empty");
				if(application.list.types.exists(null, SDSDType.filter(TripleFunctions.createWikiResourceUri(identifier))))
					throw new SDSDException("This format already exists");
				if(application.list.draftFormats.exists(user, DraftFormat.filter(user, identifier, id)))
					throw new SDSDException("Another draft with this identifier already exists");
				
				boolean ok;
				DraftFormat format;
				if(formatID == null) {
					format = application.list.draftFormats.add(user, DraftFormat.create(user, input));
					ok = true;
				} else if(!application.list.draftFormats.exists(user, id)) {
					throw new SDSDException("Given formatID doesn't exist");
				} else {
					format = application.list.draftFormats.get(user, id);
					ok = application.list.draftFormats.update(user, format, format.setContent(input));
				}

				return success(ok).put("id", format.getId().toHexString());
			}
		} catch (JSONException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject dropDraft(HttpServletRequest req, String formatID) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.format("format dropDraft: user(%s) formatID(%s)\n", 
					user != null ? user.getName() : "none", formatID);
			
			if (user == null) 
				throw new NoLoginException();
			else {
				ObjectId format = new ObjectId(formatID);
				boolean ok = application.list.draftItems.delete(user, DraftItem.filterFormat(format));
				if(ok) ok = application.list.draftFormats.delete(user, format);
				return success(ok);
			}
		} catch (JSONException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
}
