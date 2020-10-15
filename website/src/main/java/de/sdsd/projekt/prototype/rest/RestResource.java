package de.sdsd.projekt.prototype.rest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;

import de.sdsd.projekt.agrirouter.ARException;
import de.sdsd.projekt.prototype.applogic.ApplicationLogic;
import de.sdsd.projekt.prototype.applogic.TripleFunctions;
import de.sdsd.projekt.prototype.data.ARConn;
import de.sdsd.projekt.prototype.data.EfdiTimeLog;
import de.sdsd.projekt.prototype.data.File;
import de.sdsd.projekt.prototype.data.SDSDException;
import de.sdsd.projekt.prototype.data.SDSDType;
import de.sdsd.projekt.prototype.data.User;
import de.sdsd.projekt.prototype.jsonrpc.SimulatorEndpoint;
import efdi.GrpcEfdi.ISO11783_TaskData;
import efdi.GrpcEfdi.TimeLog;

/**
 * Implements the webserver rest interface, accessible over /rest/.
 * 
 * @author Markus Schr&ouml;der
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
@Path("/")
public class RestResource {

	/** The application. */
	@Inject private ApplicationLogic application;
	
	/**
	 * App error.
	 *
	 * @param user the user
	 * @param e the e
	 * @return the response
	 */
	private Response appError(User user, Exception e) {
		System.err.println(e.getMessage());
		if(user != null) application.logError(user, e.getLocalizedMessage());
		return Response.seeOther(URI.create("/")).build();
	}
	
	/**
	 * Internal error.
	 *
	 * @param user the user
	 * @param e the e
	 * @return the response
	 */
	private Response internalError(User user, Throwable e) {
		e.printStackTrace();
		if(user != null) application.logError(user, "Internal Server Error... Please report.");
		return Response.serverError().entity("Internal Server Error... Please report.").build();
	}
	
	/**
	 * No login error.
	 *
	 * @return the response
	 */
	private Response noLoginError() {
		return Response.serverError().entity("Not logged in!").build();
	}
	
	/**
	 * Gets the session id.
	 *
	 * @param req the req
	 * @return the session id
	 */
	private String getSessionId(HttpServletRequest req) {
		Cookie[] cookies = req.getCookies();
		if (cookies != null) {
			for (Cookie c : req.getCookies()) {
				if (c.getName().equals("SDSDSESSION")) {
					return c.getValue();
				}
			}
		}
		return null;
	}

	/**
	 * Upload file.
	 *
	 * @param bodyParts the body parts
	 * @param request the request
	 * @return the response
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	//curl -X POST -F "file=@/path/to/test.png" http://sdsd:sdsd@localhost:8081/rest/upload
	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(
			@FormDataParam("files") List<FormDataBodyPart> bodyParts,
			@Context HttpServletRequest request) throws IOException {
		User user = null;
		try {
			user = application.getUser(getSessionId(request));
			if(user == null)
				return noLoginError();
			
			List<String> errors = new ArrayList<>();
			for(FormDataBodyPart part : bodyParts) {
				ContentDisposition fileDetail = part.getContentDisposition();
				String filename = new String(fileDetail.getFileName().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
				Date date = fileDetail.getCreationDate();
				if(date == null)
					date = new Date();
				
				byte[] content = IOUtils.toByteArray(part.getEntityAs(InputStream.class));
				
				System.out.println("storeFile: user(" + user.getName() + ") filename(" + filename + ")");
				if(content.length == 0) {
					errors.add(filename + ": Uploaded file is empty.");
					continue;
				}
				if(content.length > 10485760) {
					errors.add(filename + ": Uploaded file is too big (max. 10 MB)");
					continue;
				}

				File file = application.file.storeFile(user, filename, content, 
						date.toInstant(), File.SOURCE_USER_UPLOAD, null);
				
				if(file == null) {
					errors.add(filename + ": No storage task for this file");
					continue;
				}
				
				application.logInfo(user, String.format("Uploaded file \"%s\" (%s)", file.getFilename(), FileUtils.byteCountToDisplaySize(file.getSize())));
			}
			
			return errors.isEmpty() 
					? Response.seeOther(URI.create("/")).build() 
					: appError(user, new SDSDException(String.join("\n", errors)));
			
		} catch (Throwable e) {
			return internalError(user, e);
		}
	}
	
	/**
	 * Download file.
	 *
	 * @param fileid the fileid
	 * @param request the request
	 * @return the response
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@GET
	@Path("/download/{fileid : [a-fA-F0-9]+}")
	public Response downloadFile(
			@PathParam("fileid") String fileid, 
			@Context HttpServletRequest request) throws IOException {
		User user = null;
		try {
			user = application.getUser(getSessionId(request));
			if(user == null)
				return noLoginError();
			
			ObjectId fid = new ObjectId(fileid);
			if(!application.list.files.exists(user, fid)) 
				return Response.status(Status.NOT_FOUND).entity("File not found").build();
			
			File file = application.list.files.get(user, fid);
			System.out.println("downloadFile: user(" + user.getName() + ") file(" + file.getFilename() + ")");
			
			application.logInfo(user, "Downloaded file \"" + file.getFilename() + "\"");
			SDSDType type = application.list.types.get(null, file.getType());
			
			return Response.ok(application.file.downloadFile(user, file), type.getMimeType())
					.header("Content-Disposition", "attachment; filename=\"" + file.getFilename() + "\"")
					.header("Content-Length", file.getSize())
					.build();
		} catch (Throwable e) {
			return internalError(user, e);
		}
	}
	
	/**
	 * Agrirouter connection information.
	 *
	 * @param username the username
	 * @param request the request
	 * @return the response
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@GET
	@Path("/arconn/{user : [A-Za-z0-9-_.!~*'()%]+}")
	public Response agrirouterConnectionInformation(
			@PathParam("user") String username, 
			@Context HttpServletRequest request) throws IOException {
		try {
			if(!application.user.isAdmin(getSessionId(request)))
				return noLoginError();
			
			User user = application.user.getUser(username);
			if(user == null) 
				return Response.status(Status.NOT_FOUND).entity("User not found").build();
			ARConn ar = user.agrirouter();
			if(ar == null) 
				return Response.status(Status.NOT_FOUND).entity("User is not onboarded to the agrirouter").build();
			
			System.out.println("arconn: user(" + user.getName() + ")");
			
			byte[] bytes = ar.getOnboardingInfo().getBytes(StandardCharsets.UTF_8);
			return Response.ok(bytes, MediaType.APPLICATION_JSON)
					.header("Content-Disposition", "attachment; filename=\"arconn_" + username + ".json\"")
					.header("Content-Length", bytes.length)
					.build();
			
		} catch (Throwable e) {
			return internalError(null, e);
		}
	}
	
	/**
	 * Onboarded.
	 *
	 * @param state the state
	 * @param token the token
	 * @param signature the signature
	 * @param error the error
	 * @param request the request
	 * @return the response
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@GET
	@Path("/onboard")
	public Response onboarded(
			@QueryParam("state") String state, 
			@QueryParam("token") String token,
			@QueryParam("signature") String signature,
			@QueryParam("error") String error,
			@Context HttpServletRequest request) throws IOException {
		User user = null;
		try {
			user = application.getUser(getSessionId(request));
			if(user == null) {
				System.out.println("/onboard: Not logged in!");
				return noLoginError();
			}
			
			if(error != null) {
				System.err.println("SecureOnboarding: " + error);
				application.logError(user, "Agrirouter onboarding failed: " + error);
				user.setSecureOnboardingContext(null);
			}
			else {
				application.agrirouter.secureOnboard(user, state, token, signature);
			}
			
			return Response.seeOther(URI.create("/")).build();
		} catch (ARException e) {
			return appError(user, e);
		} catch (Throwable e) {
			return internalError(user, e);
		}
	}
	
	/**
	 * Onboarded.
	 *
	 * @param state the state
	 * @param token the token
	 * @param signature the signature
	 * @param error the error
	 * @param statesig the statesig
	 * @param request the request
	 * @return the response
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@GET
	@Path("/onboard/{statesig : [a-fA-F0-9]+}")
	public Response onboarded(
			@QueryParam("state") String state, 
			@QueryParam("token") String token, 
			@QueryParam("signature") String signature, 
			@QueryParam("error") String error, 
			@PathParam("statesig") String statesig, 
			@Context HttpServletRequest request) throws IOException {
		
		try {
			if(!application.agrirouter.checkStateSignature(state, statesig)) 
				return Response.serverError().entity("Invalid redirect signature!").build();
			URIBuilder uriBuilder = new URIBuilder("http://localhost:8081/rest/onboard")
					.addParameter("state", state);
			if(error != null)
				uriBuilder.addParameter("error", error);
			else
				uriBuilder.addParameter("token", token).addParameter("signature", signature);
			System.out.println("Redirecting onboarding answer to localhost...");
			return Response.seeOther(uriBuilder.build()).build();
		} catch (Throwable e) {
			return internalError(null, e);
		}
	}
	
	/**
	 * Download efdi.
	 *
	 * @param fileid the fileid
	 * @param name the name
	 * @param skip the skip
	 * @param replaceTime the replace time
	 * @param request the request
	 * @return the response
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@GET
	@Path("/efdi/{fileid : [a-fA-F0-9]+}")
	public Response downloadEfdi(
			@PathParam("fileid") String fileid, 
			@QueryParam("timelog") String name, 
			@QueryParam("skip") int skip,
			@QueryParam("replaceTime") String replaceTime,
			@Context HttpServletRequest request) throws IOException {
		User user = null;
		if(name == null) name = "";
		try {
			user = application.getUser(getSessionId(request));
			if(user == null)
				return noLoginError();
			
			ObjectId fid = new ObjectId(fileid);
			if(!application.list.files.exists(user, fid)) 
				return Response.status(Status.NOT_FOUND).entity("File not found").build();
			
			File file = application.list.files.get(user, fid);
			System.out.println("efdi: user(" + user.getName() + ") file(" + file.getFilename() + ") timelog(" + name + ") skip(" + skip + ") replaceTime(" + replaceTime + ")");
			
			Timestamp rTime = replaceTime == null || replaceTime.isEmpty() ? null : Timestamps.parse(replaceTime);
			if(skip < 0) skip = 0;
			
			byte[] content = application.file.downloadFile(user, file);
			EfdiTimeLog efdi = file.isTimeLog() ? new EfdiTimeLog(content) : SimulatorEndpoint.isoxmlToEfdi(content);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			
			if(rTime == null && name.isEmpty()) {
				out.write(efdi.toZipByteArray());
			} 
			else {
				ISO11783_TaskData deviceDescription = efdi.getDeviceDescription();
				if(deviceDescription == null) throw new FileNotFoundException("No DeviceDescription found");
				List<TimeLog> timelogs = new ArrayList<>();
				if(name.isEmpty()) {
					for(String tlg : efdi.getTimeLogNames()) {
						timelogs.add(efdi.getTimeLog(tlg));
					}
					if(timelogs.isEmpty())
						new SDSDException("Couldn't find any timelogs in this file");
					if(timelogs.size() > 1)
						timelogs.sort((a,b) -> a.getFilename().compareTo(b.getFilename()));
				} else {
					TimeLog tlg = efdi.getTimeLog(name);
					if(tlg != null)
						timelogs.add(tlg);
					else
						throw new FileNotFoundException("Couldn't find timelog " + name);
					SimulatorEndpoint.trimDeviceDescription(deviceDescription, timelogs.get(0));
				}
				
				try(ZipOutputStream zip = new ZipOutputStream(out, Charset.forName("Cp437"))) {
					zip.putNextEntry(new ZipEntry("DeviceDescription.bin"));
					deviceDescription.writeTo(zip);
					zip.closeEntry();
					
					for(TimeLog log : timelogs) {
						if(skip < log.getTimeCount()) {
							if(rTime != null) {
								TimeLog.Builder times = TimeLog.newBuilder();
								for(int i = skip; i < log.getTimeCount(); ++i) {
									times.addTime(SimulatorEndpoint.replaceTime(log, skip, log.getTime(i), rTime));
								}
								log = times.build();
								rTime = log.getTime(log.getTimeCount()-1).getStart();
							}
	
							zip.putNextEntry(new ZipEntry(log.getFilename() + ".bin"));
							log.writeTo(zip);
							zip.closeEntry();
						}
						skip = Math.max(skip - log.getTimeCount(), 0);
					}
				}
			}
			
			return Response.ok(out.toByteArray(), "application/zip")
					.header("Content-Disposition", "attachment; filename=\"" + file.getFilename() + "\"")
					.header("Content-Length", out.size())
					.build();
			
		} catch (IOException | ParseException e) {
			return appError(user, e);
		} catch (Throwable e) {
			return internalError(user, e);
		}
	}
	
	/**
	 * Download efdi as isoxml.
	 *
	 * @param fileid the fileid
	 * @param request the request
	 * @return the response
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@GET
	@Path("/isoxml/{fileid : [a-fA-F0-9]+}")
	public Response downloadEfdiAsIsoxml(
			@PathParam("fileid") String fileid, 
			@Context HttpServletRequest request) throws IOException {
		User user = null;
		try {
			user = application.getUser(getSessionId(request));
			if(user == null)
				return noLoginError();
			
			ObjectId fid = new ObjectId(fileid);
			if(!application.list.files.exists(user, fid)) 
				return Response.status(Status.NOT_FOUND).entity("File not found").build();
			
			File file = application.list.files.get(user, fid);
			System.out.println("efdiToIsoxml: user(" + user.getName() + ") file(" + file.getFilename() + ")");
			if(!file.isTimeLog()) throw new IOException("File is no EFDI TimeLog");
			
			byte[] content = application.file.downloadFile(user, file);
			Process process = new ProcessBuilder("java", "-jar", "parser/efdiTimelog.jar", "isoxml").start();
			try (OutputStream processIn = process.getOutputStream()) {
				IOUtils.copy(new ByteArrayInputStream(content), processIn);
			}
			byte[] out = IOUtils.toByteArray(process.getInputStream());
			
			application.logInfo(user, "Downloaded file \"" + file.getFilename() + "\" as ISOXML TaskData");
			
			return Response.ok(out, "application/zip")
					.header("Content-Disposition", "attachment; filename=\"taskdata_" + file.getFilename() + "\"")
					.header("Content-Length", out.length)
					.build();
			
		} catch (IOException e) {
			e.printStackTrace();
			return appError(user, e);
		} catch (Throwable e) {
			e.printStackTrace();
			return internalError(user, e);
		}
	}
	
	/**
	 * Upload parser.
	 *
	 * @param uri the uri
	 * @param parser the parser
	 * @param parseCommand the parse command
	 * @param testCommand the test command
	 * @param req the req
	 * @return the response
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@POST
	@Path("/parser")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadParser(
			@FormDataParam("uri") String uri,
			@FormDataParam("parser") FormDataBodyPart parser,
			@FormDataParam("parseCommand") String parseCommand,
			@FormDataParam("testCommand") String testCommand,
			@Context HttpServletRequest req) throws IOException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			if(user == null)
				return noLoginError();
			if(!uri.startsWith(TripleFunctions.NS_WIKI))
				return appError(user, new SDSDException("Given URI is no Wikinormia URI"));
			if(!application.list.types.exists(user, uri))
				return appError(user, new SDSDException("Given format doesn't exist or doesn't belong to you"));
			ContentDisposition fileDetail = parser.getContentDisposition();
			
			String identifier = uri.substring(TripleFunctions.NS_WIKI.length());
			System.out.println("parser: user(" + user.getName() + ") identifier(" + identifier + ") parser(" + fileDetail.getFileName() 
					+ ") parseCommand(" + parseCommand + ") testCommand(" + testCommand + ")");

			String path = "parser/" + identifier;
			String extension = FilenameUtils.getExtension(fileDetail.getFileName());
			if(extension.length() > 0) path += "." + extension;
			
			try (FileOutputStream out = new FileOutputStream(path)) {
				IOUtils.copy(parser.getEntityAs(InputStream.class), out);
			} catch(IOException e) {
				e.printStackTrace();
				return appError(user, new SDSDException("Error while writing the parser to server"));
			}
			
			parseCommand = parseCommand.replace("%path%", path);
			testCommand = testCommand.replace("%path%", path);
			
			SDSDType type = application.list.types.get(user, uri);
			if(!application.list.types.update(user, type, type.setParser(path, parseCommand, testCommand)))
				return appError(user, new SDSDException("Couldn't modify format information"));
			
			return Response.seeOther(URI.create("/wikinormia.html")).build();
			
		} catch (Throwable e) {
			return internalError(user, e);
		}
	}
}
