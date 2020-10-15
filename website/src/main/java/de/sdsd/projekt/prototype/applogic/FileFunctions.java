package de.sdsd.projekt.prototype.applogic;

import agrirouter.technicalmessagetype.Gps;
import agrirouter.technicalmessagetype.Gps.GPSList;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOptions;
import de.sdsd.projekt.agrirouter.ARException;
import de.sdsd.projekt.agrirouter.ARMessageType;
import de.sdsd.projekt.agrirouter.ARMessageType.ARDirection;
import de.sdsd.projekt.agrirouter.request.AREndpoint;
import de.sdsd.projekt.agrirouter.request.ARListEndpointsRequest;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.QueryResult;
import de.sdsd.projekt.prototype.applogic.TripleFunctions.UtilQuerySolution;
import de.sdsd.projekt.prototype.data.ARConn;
import de.sdsd.projekt.prototype.data.DeviceDescription;
import de.sdsd.projekt.prototype.data.EfdiTimeLog;
import de.sdsd.projekt.prototype.data.File;
import de.sdsd.projekt.prototype.data.FileContent;
import de.sdsd.projekt.prototype.data.SDSDType;
import de.sdsd.projekt.prototype.data.StorageTask;
import de.sdsd.projekt.prototype.data.User;
import de.sdsd.projekt.prototype.jsonrpc.JsonRpcEndpoint;
import de.sdsd.projekt.prototype.websocket.SDSDEvent;
import efdi.GrpcEfdi;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.mime.MimeTypeException;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;

/**
 * Functions for storing and optaining files.
 *
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class FileFunctions {
	
	/** The device descriptions. */
	final DeviceDescriptions deviceDescriptions;
	
	/** The app. */
	private final ApplicationLogic app;
	
	/** The mongo file. */
	final MongoCollection<Document> mongoFile;
	
	/** The mongo content. */
	final MongoCollection<Document> mongoContent;
	
	/** The data added. */
	public final SDSDEvent<User, File> dataAdded = new SDSDEvent<>();
	
	/** The parser finished. */
	public final SDSDEvent<User, File> parserFinished = new SDSDEvent<>();
	
	/** The file appended. */
	public final SDSDEvent<File, byte[]> fileAppended = new SDSDEvent<>();
	
	/** The file deleted. */
	public final SDSDEvent<User, File> fileDeleted = new SDSDEvent<>();

	/**
	 * Instantiates a new file functions.
	 *
	 * @param app the app
	 */
	FileFunctions(ApplicationLogic app) {
		this.app = app;
		this.mongoFile = app.mongo.sdsd.getCollection("fileUploads");
		this.mongoContent = app.mongo.sdsd.getCollection("fileContents");
		this.deviceDescriptions = new DeviceDescriptions();
		mongoContent.createIndex(Indexes.ascending(FileContent.FILEID), new IndexOptions().unique(true));
		
		ZonedDateTime now = ZonedDateTime.now();
		ZonedDateTime start = now.truncatedTo(ChronoUnit.DAYS).plusDays(1);
		app.executor.scheduleAtFixedRate(new ExpireDaemon(), 
				Duration.between(now, start).getSeconds(), 24*60*60, TimeUnit.SECONDS);
	}
	
	/**
	 * Store file.
	 *
	 * @param user the user
	 * @param filename the filename
	 * @param content the content
	 * @param created the created
	 * @param source the source
	 * @param artype the artype
	 * @return the file
	 */
	@CheckForNull
	public File storeFile(User user, String filename, byte[] content, Instant created, String source, @Nullable ARMessageType artype) {
		SDSDType type = app.parser.determineType(content, filename, artype);
		
		try {
			if(!type.getMimeType().isEmpty() && FilenameUtils.getExtension(filename).isEmpty()) {
				String ext = TikaConfig.getDefaultConfig().getMimeRepository().forName(type.getMimeType()).getExtension();
				if(!ext.isEmpty())
					filename += ext;
			}
		} catch(MimeTypeException e) {
			System.err.println(e.getMessage());
		}
		
		Instant expires = checkStorageTasks(user, type.getUri(), source, created);
		if(expires == null) return null; // no storage task
		
		Document create = File.create(user, filename, content.length, type.getUri(), created, source, expires);
		File file = app.list.files.add(user, create);
		mongoContent.insertOne(FileContent.create(file, content, false));

		app.parser.parseFileAsync(user, file, content, true);
		dataAdded.trigger(user, file);

		return file;
	}
	
	/**
	 * Check storage tasks.
	 *
	 * @param user the user
	 * @param type the type
	 * @param source the source
	 * @param created the created
	 * @return the instant
	 */
	@CheckForNull
	protected Instant checkStorageTasks(User user, String type, String source, Instant created) {
		boolean discard = true;
		Instant expires = Instant.MIN;
		for(StorageTask st : app.list.storageTasks.getList(user)) {
			if(st.check(type, source, created)) {
				discard = false;
				Instant exp = st.getExpire();
				if(exp.isAfter(expires)) expires = exp;
			}
		}
		return discard ? null : expires;
	}
	
	
	
	/**
	 * Gets the content.
	 *
	 * @param user the user
	 * @param file the file
	 * @return the content
	 * @throws FileNotFoundException the file not found exception
	 */
	@Nonnull
	protected FileContent getContent(User user, File file) throws FileNotFoundException {
		Document doc = mongoContent.find(FileContent.filter(user, file.getId())).first();
		if(doc == null) 
			throw new FileNotFoundException("No content of file \"" + file.getFilename() + "\" found");
		return new FileContent(doc);
	}
	
	/**
	 * Download file.
	 *
	 * @param user the user
	 * @param file the file
	 * @return the byte[]
	 * @throws FileNotFoundException the file not found exception
	 */
	@Nonnull
	public byte[] downloadFile(User user, File file) throws FileNotFoundException {
		return getContent(user, file).getContent();
	}

	/**
	 * Delete file.
	 *
	 * @param user the user
	 * @param file the file
	 * @return true, if successful
	 */
	public boolean deleteFile(User user, File file) {
		app.parser.removeFileDataAsync(user, file.getId().toHexString(), true);
		
		mongoContent.deleteOne(FileContent.filter(user, file.getId()));
		boolean ok = mongoFile.deleteOne(file.filter()).wasAcknowledged();
		if(ok) {
			fileAppended.unsetAllListener(file);
			fileDeleted.trigger(user, file);
		}
		return ok;
	}
	
	/**
	 * List all file I ds.
	 *
	 * @return the sets the
	 */
	Set<ObjectId> listAllFileIDs() {
		Set<ObjectId> files = new HashSet<>();
		for(Document doc : mongoFile.find()) {
			files.add(doc.getObjectId(File.ID));
		}
		return files;
	}
	
	/**
	 * Tidy up.
	 *
	 * @param fileIds the file ids
	 */
	void tidyUp(Set<ObjectId> fileIds) {
		Set<ObjectId> delete = new HashSet<>();
		for(Document doc : mongoContent.find(Filters.nin(FileContent.FILEID, fileIds))) {
			delete.add(doc.getObjectId(FileContent.FILEID));
		}
		
		System.out.println("Delete " + delete.size() + " file contents: " + delete.stream()
				.map(ObjectId::toHexString).collect(Collectors.joining(", ")));
		
		if(delete.size() > 0)
			System.out.println("Success: " + mongoContent.deleteMany(Filters.in(FileContent.FILEID, delete)).wasAcknowledged());
	}
	
	/**
	 * Reparse file.
	 *
	 * @param user the user
	 * @param file the file
	 * @return the future
	 * @throws FileNotFoundException the file not found exception
	 */
	public Future<Boolean> reparseFile(User user, File file) throws FileNotFoundException {
		byte[] content = downloadFile(user, file);
		return app.parser.deleteAndParseFileAsync(user, file, content, true);
	}
	
	/**
	 * Daemon for automatic deletion of expired files.
	 *
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private class ExpireDaemon implements Runnable {
		
		/**
		 * Run.
		 */
		@Override
		public void run() {
			try {
				System.out.println("Running expire daemon...");
				for(Document d : mongoFile.find(Filters.lt(File.EXPIRES, Date.from(Instant.now())))) {
					File file = new File(d);
					User user = app.user.getUser(file.getUser());
					if(user == null) {
						System.err.format("Found file(%s) from unknown user \"%s\"\n", file.getId().toHexString(), file.getUser());
						continue;
					}
					System.out.println("File expired: user(" + user.getName() + ") file(" + file.getFilename() + ")");

					deleteFile(user, file);
					app.logInfo(user, "File expired: " + file.getFilename());
				}
			} catch(Throwable e) {
				e.printStackTrace();
			}
		}
	};
	
	/**
	 * Helper for storing and obtaining EFDI device descriptions.
	 *
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public class DeviceDescriptions {
		
		/** The mongo. */
		private final MongoCollection<Document> mongo = app.mongo.sdsd.getCollection("deviceDescriptions");
		
		/**
		 * Instantiates a new device descriptions.
		 */
		public DeviceDescriptions() {
			mongo.createIndex(Indexes.ascending(DeviceDescription.CONTEXTID), new IndexOptions().unique(true));
			
			ZonedDateTime now = ZonedDateTime.now();
			ZonedDateTime start = now.truncatedTo(ChronoUnit.DAYS).plusDays(1).plusHours(1);
			app.executor.scheduleAtFixedRate(new RemoveDaemon(), 
					Duration.between(now, start).getSeconds(), 24*60*60, TimeUnit.SECONDS);
//			app.executor.scheduleAtFixedRate(new RemoveDaemon(), 
//					15, 24*60*60, TimeUnit.SECONDS);
		}
		
		/**
		 * Store.
		 *
		 * @param user the user
		 * @param contextId the context id
		 * @param content the content
		 * @throws InvalidProtocolBufferException the invalid protocol buffer exception
		 */
		public void store(User user, String contextId, byte[] content) 
				throws InvalidProtocolBufferException {
			GrpcEfdi.ISO11783_TaskData.parseFrom(content);
			
			mongo.updateOne(DeviceDescription.filter(contextId), 
					DeviceDescription.create(user, content), 
					new UpdateOptions().upsert(true));
		}
		
		/**
		 * Gets the.
		 *
		 * @param contextId the context id
		 * @return the device description
		 */
		@CheckForNull
		public DeviceDescription get(String contextId) {
			Document doc = mongo.find(DeviceDescription.filter(contextId)).first();
			if(doc == null) return null;
			return new DeviceDescription(doc);
		}
		
		/**
		 * The Class RemoveDaemon.
		 */
		private class RemoveDaemon implements Runnable {
			
			/**
			 * Run.
			 */
			@Override
			public void run() {
				try {
					List<DeviceDescription> list = new LinkedList<>();
					HashMap<String, Set<ByteString>> routes = new HashMap<>();
					
					for(Document doc : mongo.find()) {
						DeviceDescription desc = new DeviceDescription(doc);
						if(!mongoFile.find(desc.filter()).iterator().hasNext()) {
							list.add(desc);
							if(desc.getUser() != null)
								routes.put(desc.getUser(), Collections.emptySet());
						}
					}
					
					ARListEndpointsRequest endpointRequest = new ARListEndpointsRequest()
							.considerRoutingRules(true)
							.setDirection(ARDirection.RECEIVE)
							.setMessageTypeFilter(ARMessageType.TIME_LOG);
					
					for(String username : routes.keySet()) {
						try {
							User user = app.user.getUser(username);
							if(user == null) continue;
							ARConn arconn = user.agrirouter();
							if(arconn == null) continue;
							HashSet<ByteString> set = new HashSet<>();
							for(AREndpoint endpoint : endpointRequest.send(arconn.conn(), 
									AgrirouterFunctions.TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
								try {
									String clientname = endpoint.getExternalId();
									set.add(ByteString.copyFrom(Hex.decodeHex(clientname)));
								} catch (DecoderException e) {}
							}
							routes.put(username, set);
						} catch (InterruptedException | ARException e) {}
					}
					
					ListIterator<DeviceDescription> it = list.listIterator();
					while(it.hasNext()) {
						DeviceDescription desc = it.next();
						if(desc.getUser() != null) {
							Set<ByteString> set = routes.get(desc.getUser());
							if(!desc.getContent().getDeviceList().stream()
									.map(GrpcEfdi.Device::getClientName)
									.filter(set::contains)
									.findAny().isPresent())
								it.remove();
						}
					}

					if(list.isEmpty())
						System.out.println("Nothing to delete");
					else {
						mongo.deleteMany(Filters.or(list.stream().map(DeviceDescription::filter).collect(Collectors.toList())));
						System.out.println("Deleting : \n" + list.stream().map(DeviceDescription::toString).collect(Collectors.joining("\n")));
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		};
	}
	
	/** The parse timer. */
	private final ConcurrentMap<ObjectId, DelayedParser> parseTimer = new ConcurrentHashMap<>();
	/**
	 * Base class for delayed calling of parsers.
	 * Delayed parsers are used for appended file types to prevent the parser from running very often.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private abstract class DelayedParser implements Runnable {
		
		/** The user. */
		public final User user;
		
		/** The file. */
		public final File file;
		
		/** The log. */
		public final boolean log;
		
		/** The future. */
		private ScheduledFuture<?> future;
		
		/**
		 * Instantiates a new delayed parser.
		 *
		 * @param user the user
		 * @param file the file
		 * @param delay the delay
		 * @param unit the unit
		 * @param log the log
		 */
		public DelayedParser(User user, File file, long delay, TimeUnit unit, boolean log) {
			this.user = user;
			this.file = file;
			this.log = log;
			this.future = app.executor.schedule(this, delay, unit);
		}
		
		/**
		 * Adds the.
		 *
		 * @param delay the delay
		 * @param unit the unit
		 */
		protected void add(long delay, TimeUnit unit) {
			this.future.cancel(false);
			this.future = app.executor.schedule(this, delay, unit);
		}
	}
	
	/**
	 * Delayed EFDI timelog parser.
	 *
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 * @see DelayedParser
	 */
	private class DelayedTimelogParser extends DelayedParser {
		
		/** The tlg. */
		private final GrpcEfdi.TimeLog.Builder tlg;
		
		/**
		 * Instantiates a new delayed timelog parser.
		 *
		 * @param user the user
		 * @param file the file
		 * @param delay the delay
		 * @param unit the unit
		 * @param log the log
		 * @param tlg the tlg
		 */
		public DelayedTimelogParser(User user, File file, long delay, TimeUnit unit, boolean log, GrpcEfdi.TimeLog tlg) {
			super(user, file, delay, unit, log);
			this.tlg = tlg.toBuilder();
		}
		
		/**
		 * Adds the.
		 *
		 * @param delay the delay
		 * @param unit the unit
		 * @param tlg the tlg
		 * @return the delayed timelog parser
		 */
		public DelayedTimelogParser add(long delay, TimeUnit unit, GrpcEfdi.TimeLog tlg) {
			this.tlg.addAllTime(tlg.getTimeList());
			super.add(delay, unit);
			return this;
		}

		/**
		 * Run.
		 */
		@Override
		public void run() {
			if(parseTimer.remove(file.getId()) == null) return;
			try {
				Var DLV=Var.alloc("dlv"), TLG=Var.alloc("tlg"), TLGNAME=Var.alloc("tlgname"),
						DET=Var.alloc("det"), DETNUM=Var.alloc("detnum"), DDI=Var.alloc("ddi");
				Query query = new SelectBuilder()
						.addVar(TLG).addVar(TLGNAME).addVar(DLV)
						.from(file.getURI())
						.addWhere(TLG, TripleFunctions.TIMELOG.prop("name"), TLGNAME)
						.addWhere(DLV, DCTerms.isPartOf, TLG)
						.addWhere(DLV, JsonRpcEndpoint.T_ISOXML.res("DLV").prop("A"), DDI)
						.addWhere(DLV, JsonRpcEndpoint.T_ISOXML.res("DLV").prop("C"), DET)
						.addWhere(DET, JsonRpcEndpoint.T_ISOXML.res("DET").prop("A"), DETNUM)
						.build();
				
				JSONObject tlguris = new JSONObject();
				JSONObject vuris = new JSONObject();
				try(QueryResult qr = app.triple.query(query)) {
					for(UtilQuerySolution qs : qr.iterate()) {
						tlguris.put(qs.getString(TLGNAME), qs.getUri(TLG));
						JsonRpcEndpoint.getOrCreate(vuris, qs.getLiteralValue(DETNUM).toString())
								.put(qs.get(DDI).visitWith(JsonRpcEndpoint.DDI_INT).toString(), qs.getUri(DLV));
					}
				}
				if(tlguris.length() == 0) throw new IOException("No timelogs found for file " + file.getURI());
				
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				try(ZipOutputStream zip = new ZipOutputStream(out, Charset.forName("Cp437"))) {
					zip.putNextEntry(new ZipEntry("tlginfo.json"));
					zip.write(new JSONObject()
							.put("tlguris", tlguris)
							.put("vuris", vuris)
							.toString().getBytes(StandardCharsets.UTF_8));
					zip.closeEntry();
					
					String tlgname = tlguris.keys().next();
					zip.putNextEntry(new ZipEntry(tlgname + ".bin"));
					tlg.build().writeTo(zip);
					zip.closeEntry();
				}
				
				app.parser.parseFileAsync(user, file, out.toByteArray(), log);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Delayed GPS Info parser.
	 *
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 * @see DelayedParser
	 */
	private class DelayedGpsParser extends DelayedParser {
		
		/** The gps. */
		private final GPSList.Builder gps;
		
		/**
		 * Instantiates a new delayed gps parser.
		 *
		 * @param user the user
		 * @param file the file
		 * @param delay the delay
		 * @param unit the unit
		 * @param log the log
		 * @param gps the gps
		 */
		public DelayedGpsParser(User user, File file, long delay, TimeUnit unit, boolean log, GPSList gps) {
			super(user, file, delay, unit, log);
			this.gps = gps.toBuilder();
		}
		
		/**
		 * Adds the.
		 *
		 * @param delay the delay
		 * @param unit the unit
		 * @param gps the gps
		 * @return the delayed gps parser
		 */
		public DelayedGpsParser add(long delay, TimeUnit unit, GPSList gps) {
			this.gps.addAllGpsEntries(gps.getGpsEntriesList());
			super.add(delay, unit);
			return this;
		}

		/**
		 * Run.
		 */
		@Override
		public void run() {
			if(parseTimer.remove(file.getId()) == null) return;
			app.parser.parseFileAsync(user, file, gps.build().toByteArray(), log);
		}
	}
	
	/**
	 * Parses the delayed.
	 *
	 * @param user the user
	 * @param file the file
	 * @param delay the delay
	 * @param unit the unit
	 * @param log the log
	 * @param tlg the tlg
	 */
	private void parseDelayed(User user, File file, long delay, TimeUnit unit, boolean log, GrpcEfdi.TimeLog tlg) {
		DelayedParser dp = parseTimer.get(file.getId());
		if(dp == null) parseTimer.put(file.getId(), new DelayedTimelogParser(user, file, delay, unit, log, tlg));
		else ((DelayedTimelogParser)dp).add(delay, unit, tlg);
	}
	
	/**
	 * Parses the delayed.
	 *
	 * @param user the user
	 * @param file the file
	 * @param delay the delay
	 * @param unit the unit
	 * @param log the log
	 * @param gps the gps
	 */
	private void parseDelayed(User user, File file, long delay, TimeUnit unit, boolean log, GPSList gps) {
		DelayedParser dp = parseTimer.get(file.getId());
		if(dp == null) parseTimer.put(file.getId(), new DelayedGpsParser(user, file, delay, unit, log, gps));
		else ((DelayedGpsParser)dp).add(delay, unit, gps);
	}
	
	/** The Constant TIMELOG_PARSE_DELAY. */
	private static final long TIMELOG_PARSE_DELAY = 60; // seconds
	
	/**
	 * Store time log.
	 *
	 * @param user the user
	 * @param contextId the context id
	 * @param content the content
	 * @param created the created
	 * @param source the source
	 * @return the file
	 * @throws InvalidProtocolBufferException the invalid protocol buffer exception
	 * @throws ARException the AR exception
	 */
	@CheckForNull
	public File storeTimeLog(User user, String contextId, byte[] content, Instant created, String source) throws InvalidProtocolBufferException, ARException {
		GrpcEfdi.TimeLog log = GrpcEfdi.TimeLog.parseFrom(content);

		Document doc = mongoFile.find(Filters.and(File.filter(user), DeviceDescription.filter(contextId), 
				Filters.gte(File.CREATED, Date.from(created.truncatedTo(ChronoUnit.DAYS))), 
				Filters.lt(File.CREATED, Date.from(created.truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS))))).first();
		
		if(doc != null) {
			File file = new File(doc);
			try {
				FileContent fileContent = getContent(user, file);
				EfdiTimeLog tlg = new EfdiTimeLog(fileContent.getContent());
				
				Optional<String> tlgname = tlg.getTimeLogNames().stream().findAny();
				if(tlgname.isPresent()) {
					GrpcEfdi.TimeLog oldLog = tlg.getTimeLog(tlgname.get());
					if(oldLog != null) {
						log = oldLog.toBuilder().addAllTime(log.getTimeList()).build();
						tlg.setTimeLog(tlgname.get(), log);
					} else
						tlg.setTimeLog(tlgname.get(), content);
				} else {
					tlgname = Optional.of(tlg.getFreeTimeLogName());
					tlg.setTimeLog(tlgname.get(), content);
				}
				
				byte[] newcontent = tlg.toZipByteArray();
				if(!mongoContent.updateOne(fileContent.filter(), 
						fileContent.setContent(newcontent, true)).wasAcknowledged()) return null;
				app.list.files.update(user, file, file.setSize(newcontent.length));
			} catch(FileNotFoundException e) {
				DeviceDescription deviceDescription = deviceDescriptions.get(contextId);
				if(deviceDescription == null)
					throw new ARException("Missing DeviceDescription for context " + contextId);
				byte[] newcontent = new EfdiTimeLog(deviceDescription)
						.addTimeLog(content)
						.toZipByteArray();
				mongoContent.insertOne(FileContent.create(file, newcontent, false));
				app.list.files.update(user, file, file.setSize(newcontent.length));
			}
			parseDelayed(user, file, TIMELOG_PARSE_DELAY, TimeUnit.SECONDS, false, log);
			fileAppended.trigger(file, content);
			dataAdded.trigger(user, file);
			return file;
		} 
		else {
			Instant expires = checkStorageTasks(user, File.TYPE_TIMELOG, source, created);
			if(expires == null) return null; // no storage task
			
			String filename = DateTimeFormatter.ISO_LOCAL_DATE.format(created.atOffset(ZoneOffset.UTC)) + "_";
			DeviceDescription deviceDescription = deviceDescriptions.get(contextId);
			if(deviceDescription == null)
				throw new ARException("Missing DeviceDescription for context " + contextId);
				
			filename += deviceDescription.getContent().getDeviceList().stream()
					.map(dvc -> String.format("%s(%s)", dvc.getDeviceDesignator(), dvc.getDeviceSerialNumber()))
					.collect(Collectors.joining("_"));
			filename += ".zip";
			
			byte[] newcontent = new EfdiTimeLog(deviceDescription)
					.addTimeLog(content)
					.toZipByteArray();
			
			Document create = File.create(user, filename, newcontent.length, 
					File.TYPE_TIMELOG, created, source, expires);
			create.put(DeviceDescription.CONTEXTID, contextId);
			File file = app.list.files.add(user, create);
			mongoContent.insertOne(FileContent.create(file, newcontent, false));
			app.parser.parseFileAsync(user, file, newcontent, false);
			dataAdded.trigger(user, file);
			return file;
		}
	}
	
	/**
	 * Gets the time log context id.
	 *
	 * @param file the file
	 * @return the time log context id
	 */
	@CheckForNull
	public String getTimeLogContextId(File file) {
		Document doc = mongoFile.find(file.filter()).first();
		if(doc == null) throw new RuntimeException("File not found: " + file.getId().toHexString());
		return doc.getString(DeviceDescription.CONTEXTID);
	}
	
	/**
	 * Store gps info.
	 *
	 * @param user the user
	 * @param content the content
	 * @param created the created
	 * @param source the source
	 * @return the file
	 * @throws InvalidProtocolBufferException the invalid protocol buffer exception
	 * @throws ARException the AR exception
	 */
	public File storeGpsInfo(User user, byte[] content, Instant created, String source) throws InvalidProtocolBufferException, ARException {
		GPSList gps = Gps.GPSList.parseFrom(content);

		Document doc = mongoFile.find(Filters.and(File.filter(user), File.filterType(File.TYPE_GPSINFO), File.filterSource(source), 
				Filters.gte(File.CREATED, Date.from(created.truncatedTo(ChronoUnit.DAYS))), 
				Filters.lt(File.CREATED, Date.from(created.truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS))))).first();
		
		if(doc != null) {
			File file = new File(doc);
			try {
				FileContent fileContent = getContent(user, file);
				GPSList list = GPSList.parseFrom(fileContent.getContent());
				
				list = list.toBuilder().addAllGpsEntries(gps.getGpsEntriesList()).build();
				
				byte[] newcontent = list.toByteArray();
				if(!mongoContent.updateOne(fileContent.filter(), 
						fileContent.setContent(newcontent, true)).wasAcknowledged()) return null;
				app.list.files.update(user, file, file.setSize(newcontent.length));
			} catch(FileNotFoundException e) {
				mongoContent.insertOne(FileContent.create(file, content, false));
				app.list.files.update(user, file, file.setSize(content.length));
			}
			parseDelayed(user, file, TIMELOG_PARSE_DELAY, TimeUnit.SECONDS, false, gps);
			fileAppended.trigger(file, content);
			dataAdded.trigger(user, file);
			return file;
		} 
		else {
			Instant expires = checkStorageTasks(user, File.TYPE_TIMELOG, source, created);
			if(expires == null) return null; // no storage task
			
			String filename = "GPS_" + DateTimeFormatter.ISO_LOCAL_DATE.format(created.atOffset(ZoneOffset.UTC)) + ".bin";
			
			Document create = File.create(user, filename, content.length, 
					File.TYPE_GPSINFO, created, source, expires);
			File file = app.list.files.add(user, create);
			mongoContent.insertOne(FileContent.create(file, content, true));
			app.parser.parseFileAsync(user, file, content, false);
			dataAdded.trigger(user, file);
			return file;
		}
	}
	
	/**
	 * Append file.
	 *
	 * @param user the user
	 * @param file the file
	 * @param newContent the new content
	 * @return true, if successful
	 */
	@CheckForNull
	public boolean appendFile(User user, File file, byte[] newContent) {
		try {
			FileContent fileContent = getContent(user, file);
			byte[] existing = fileContent.getContent();
			byte[] content = new byte[existing.length + newContent.length];
			System.arraycopy(existing, 0, content, 0, existing.length);
			System.arraycopy(newContent, 0, content, existing.length, newContent.length);
			
			if(!mongoContent.updateOne(fileContent.filter(), 
					fileContent.setContent(content, fileContent.isCompressed())).wasAcknowledged()) return false;
			app.list.files.update(user, file, file.setSize(content.length));
		} catch (FileNotFoundException e) {
			mongoContent.insertOne(FileContent.create(file, newContent, true));
			app.list.files.update(user, file, file.setSize(newContent.length));
		}
		
		app.parser.parseFileAsync(user, file, newContent, false);
		fileAppended.trigger(file, newContent);
		dataAdded.trigger(user, file);
		
		return true;
	}
}
