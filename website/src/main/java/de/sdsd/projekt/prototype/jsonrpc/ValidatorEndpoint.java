package de.sdsd.projekt.prototype.jsonrpc;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mongodb.client.model.Filters;

import de.sdsd.projekt.api.ParserAPI.Validation;
import de.sdsd.projekt.prototype.applogic.ApplicationLogic;
import de.sdsd.projekt.prototype.data.File;
import de.sdsd.projekt.prototype.data.SDSDException;
import de.sdsd.projekt.prototype.data.User;
import de.sdsd.projekt.prototype.data.Util;

/**
 * JSONRPC-Endpoint for validator functions.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class ValidatorEndpoint extends JsonRpcEndpoint {

	/**
	 * Instantiates a new validator endpoint.
	 *
	 * @param application the application
	 */
	public ValidatorEndpoint(ApplicationLogic application) {
		super(application);
	}
	
	/**
	 * List iso xml files.
	 *
	 * @param req the req
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject listIsoXmlFiles(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("listIsoXmlFiles: user(" + (user != null ? user.getName() : "none") + ")");
			
			if (user == null) 
				throw new NoLoginException();
			else {
				Bson filter = Filters.in(File.TYPE, "https://app.sdsd-projekt.de/wikinormia.html?page=isoxml", File.TYPE_TIMELOG);
				JSONArray array = application.list.files.get(user, filter).stream()
						.sorted(File.CMP_RECENT)
						.map(file -> new JSONObject()
								.put("id", file.getId().toHexString())
								.put("filename", file.getFilename()))
						.collect(Util.toJSONArray());
				return new JSONObject().put("files", array);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Validate.
	 *
	 * @param req the req
	 * @param fileid the fileid
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject validate(HttpServletRequest req, String fileid) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("validate: user(" + (user != null ? user.getName() : "none") + ") file(" + fileid + ")");
			
			ObjectId fid = new ObjectId(fileid);
			if (user == null) 
				throw new NoLoginException();
			else if (!application.list.files.exists(user, fid))
				throw new SDSDException("File not found");
			else {
				File file = application.list.files.get(user, fid);
				String parser = null;
				if(file.getType().endsWith("=isoxml"))
					parser = "parser/isoxml.jar";
				else if(file.getType().endsWith("=efdiTimelog") || file.getType().endsWith("=efdiDeviceDescription"))
					parser = "parser/efdiTimelog.jar";
				else
					throw new SDSDException("File type doesn't support validation");
				
				byte[] content = application.file.downloadFile(user, file);
				Process process = new ProcessBuilder("java", "-jar", parser, "validate").start();
				try (OutputStream processIn = process.getOutputStream()) {
					IOUtils.copy(new ByteArrayInputStream(content), processIn);
				} catch(IOException e) {
					e.printStackTrace();
				}
				
				JSONArray errors = new JSONArray();
				try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
					String line;
					while((line = in.readLine()) != null) {
						errors.put(line);
					}
				}
				
				if(file.getValidation() == File.Validation.UNVALIDATED) {
					File.Validation vali;
					if(errors.length() == 0) vali = File.Validation.NO_ERROR;
					else {
						String err = errors.getString(0);
						if(err.startsWith(Validation.FATAL)) vali = File.Validation.FATAL_ERRORS;
						else if(err.startsWith(Validation.ERROR)) vali = File.Validation.ERRORS;
						else if(err.startsWith(Validation.WARN)) vali = File.Validation.WARNINGS;
						else vali = File.Validation.NO_ERROR;
					}
					application.list.files.update(user, file, file.setValidation(vali));
				}
				
				return new JSONObject()
						.put("filename", file.getFilename())
						.put("id", file.getId().toHexString())
						.put("output", errors.length() == 0 
								? new JSONArray().put("No Errors found") 
								: errors);
			}
		} catch (IOException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}

}
