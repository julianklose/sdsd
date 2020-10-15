package de.sdsd.projekt.prototype.jsonrpc;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import com.mongodb.client.model.Filters;

import de.sdsd.projekt.agrirouter.ARException;
import de.sdsd.projekt.prototype.applogic.ApplicationLogic;
import de.sdsd.projekt.prototype.data.EfdiTimeLog;
import de.sdsd.projekt.prototype.data.File;
import de.sdsd.projekt.prototype.data.SDSDException;
import de.sdsd.projekt.prototype.data.User;
import de.sdsd.projekt.prototype.data.Util;
import de.sdsd.projekt.prototype.websocket.SDSDEvent;
import de.sdsd.projekt.prototype.websocket.WebsocketConnection;
import efdi.GrpcEfdi;
import efdi.GrpcEfdi.DataLogValue;
import efdi.GrpcEfdi.Device;
import efdi.GrpcEfdi.DeviceElement;
import efdi.GrpcEfdi.DeviceObjectReference;
import efdi.GrpcEfdi.DeviceProcessData;
import efdi.GrpcEfdi.ISO11783_TaskData;
import efdi.GrpcEfdi.Time;
import efdi.GrpcEfdi.TimeLog;
import efdi.GrpcEfdi.UID;

/**
 * JSONRPC-Endpoint for simulator functions.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class SimulatorEndpoint extends JsonRpcEndpoint {
	
	/** The on sended. */
	private final SDSDEvent<User, TelemetrySimulator> onSended = new SDSDEvent<>();
	
	/** The Constant SEND. */
	private static final boolean SEND = true;
	
	/** The running simulators. */
	private final ConcurrentHashMap<User, TelemetrySimulator> runningSimulators = new ConcurrentHashMap<>();

	/**
	 * Instantiates a new simulator endpoint.
	 *
	 * @param application the application
	 */
	public SimulatorEndpoint(ApplicationLogic application) {
		super(application);
	}
	
	/**
	 * List the users files.
	 *
	 * @param req the current request
	 * @return the JSON object list of files
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
				Bson filter = Filters.or(File.filterType("https://app.sdsd-projekt.de/wikinormia.html?page=isoxml"), File.filterType(File.TYPE_TIMELOG));
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
	 * gets timelog and ddi infos
	 *
	 * @param req the current request
	 * @param fileid the parent files id
	 * @return the JSON object timelog and ddi data. Includes the timelogs count, from and until timestamps and device name.
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject timelogInfo(HttpServletRequest req, String fileid) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("timelogInfo: user(" + (user != null ? user.getName() : "none") + ") file(" + fileid + ")");
			
			ObjectId fid = new ObjectId(fileid);
			if (user == null) 
				throw new NoLoginException();
			else if (!application.list.files.exists(user, fid))
				throw new SDSDException("File not found");
			else {
				File file = application.list.files.get(user, fid);
				byte[] content = application.file.downloadFile(user, file);
				EfdiTimeLog efdi = file.isTimeLog() ? new EfdiTimeLog(content) : isoxmlToEfdi(content);

				GrpcEfdi.ISO11783_TaskData deviceDescription = efdi.getDeviceDescription();
				if(deviceDescription == null) throw new FileNotFoundException("DeviceDescription is missing");
				
				JSONArray ddis = new JSONArray();
				Map<Long, Device> deviceMap = new HashMap<>();
				for(Device device : deviceDescription.getDeviceList()) {
					Map<Integer, DeviceProcessData> dpds = device.getDeviceProcessDataList().stream()
							.collect(Collectors.toMap(DeviceProcessData::getDeviceProcessDataObjectId, Function.identity()));
					
					JSONArray dets = new JSONArray();
					for(DeviceElement det : device.getDeviceElementList()) {
						deviceMap.put(det.getDeviceElementId().getNumber(), device);
						JSONArray detddis = new JSONArray();
						for(DeviceObjectReference dor : det.getDeviceObjectReferenceList()) {
							DeviceProcessData dpd = dpds.get(dor.getDeviceObjectId());
							if(dpd == null) continue;
							detddis.put(new JSONObject()
									.put("ddi", dpd.getDeviceProcessDataDdi())
									.put("name", dpd.getDeviceProcessDataDesignator()));
						}
						dets.put(new JSONObject()
								.put("element", det.getDeviceElementDesignator())
								.put("ddis", detddis));
					}
					ddis.put(new JSONObject()
							.put("device", device.getDeviceDesignator())
							.put("elements", dets));
				}
				
				JSONObject timelogs = new JSONObject();
				for(String name : efdi.getTimeLogNames()) {
					TimeLog timelog = efdi.getTimeLog(name);
					if(timelog == null) continue;
					
					JSONObject info = new JSONObject()
							.put("count", timelog.getTimeCount());
					if(timelog.getTimeCount() > 0) {
						info.put("from", Timestamps.toString(timelog.getTime(0).getStart()))
							.put("until", Timestamps.toString(timelog.getTime(timelog.getTimeCount()-1).getStart()));
					}
					
					Optional<Device> device = timelog.getTimeList().stream()
							.flatMap(time -> time.getDataLogValueList().stream())
							.map(DataLogValue::getDeviceElementIdRef)
							.mapToLong(UID::getNumber)
							.mapToObj(deviceMap::get)
							.filter(Objects::nonNull)
							.findAny();
					if(device.isPresent())
						info.put("device", device.get().getDeviceDesignator());
					
					timelogs.put(name, info);
				}
				
				return new JSONObject()
						.put("ddis", ddis)
						.put("timelogs", timelogs);
			}
		} catch (IOException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Sets a Listener that triggers on the SDSDEvent onSended.
	 *
	 * @param req http servlet request including userdata
	 * @param conn websocket connnection
	 * @param identifier the identifier
	 * @return the JSON object including a success attribute
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject setProgressListener(HttpServletRequest req, WebsocketConnection conn, String identifier) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.format("setProgressListener: user(%s) connection(%s)\n", (user != null ? user.getName() : "none"), conn.getId());
			
			if (user == null)
				throw new NoLoginException();
			else {
				onSended.setListener(user, conn.listener("simulator", "progress", null, identifier, TelemetrySimulator::progress));
				return success(true);
			}
		} catch (Throwable e) {
			throw createError(null, e);
		}
	}
	
	/**
	 * Progress.
	 *
	 * @param req the req
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject progress(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("progress: user(" + (user != null ? user.getName() : "none") + ")");

			if (user == null) 
				throw new NoLoginException();
			else {
				TelemetrySimulator sim = runningSimulators.get(user);
				return sim != null ? sim.progress() : new JSONObject();
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Pause.
	 *
	 * @param req the req
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject pause(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("pause: user(" + (user != null ? user.getName() : "none") + ")");

			if (user == null) 
				throw new NoLoginException();
			else {
				TelemetrySimulator sim = runningSimulators.get(user);
				if(sim != null) {
					sim.cancel();
					return sim.progress();
				}
				return success(false);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Resume.
	 *
	 * @param req the req
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject resume(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("resume: user(" + (user != null ? user.getName() : "none") + ")");

			if (user == null) 
				throw new NoLoginException();
			else {
				TelemetrySimulator sim = runningSimulators.get(user);
				if(sim != null) {
					sim.resume();
					return sim.progress();
				}
				return success(false);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Forward.
	 *
	 * @param req the req
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject forward(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("forward: user(" + (user != null ? user.getName() : "none") + ")");

			if (user == null) 
				throw new NoLoginException();
			else {
				TelemetrySimulator sim = runningSimulators.get(user);
				if(sim != null) {
					TelemetrySimulator next = sim.startNext();
					if(next != null)
						return next.progress();
				}
				return success(false);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Stop.
	 *
	 * @param req the req
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject stop(HttpServletRequest req) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("stop: user(" + (user != null ? user.getName() : "none") + ")");

			if (user == null) 
				throw new NoLoginException();
			else {
				TelemetrySimulator sim = runningSimulators.remove(user);
				if(sim != null) {
					sim.cancel();
					return success(true);
				}
				return success(false);
			}
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Send entire.
	 *
	 * @param req the req
	 * @param fileid the fileid
	 * @param name the name
	 * @param skip the skip
	 * @param replaceTime the replace time
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject sendEntire(HttpServletRequest req, String fileid, String name, int skip, String replaceTime) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			if(name == null) name = "";
			System.out.format("sendEntire: user(%s) file(%s) name(%s) skip(%d) replaceTime(%s)\n",
					(user != null ? user.getName() : "none"), fileid, name.isEmpty() ? "all" : name, skip, replaceTime != null ? replaceTime : "none");
			
			ObjectId fid = new ObjectId(fileid);
			if (user == null) 
				throw new NoLoginException();
			else if (user.agrirouter() == null)
				throw new NoOnboardingException();
			else if (!application.list.files.exists(user, fid))
				throw new SDSDException("File not found");
			else {
				Timestamp rTime = replaceTime == null || replaceTime.isEmpty() ? null : Timestamps.parse(replaceTime);
				if(skip < 0) skip = 0;
				
				File file = application.list.files.get(user, fid);
				byte[] content = application.file.downloadFile(user, file);
				EfdiTimeLog efdi = file.isTimeLog() ? new EfdiTimeLog(content) : isoxmlToEfdi(content);
				
				ISO11783_TaskData deviceDescription = efdi.getDeviceDescription();
				if(deviceDescription == null) throw new FileNotFoundException("No DeviceDescription found");
				List<TimeLog> timelogs = new ArrayList<>();
				if(name.isEmpty()) {
					for(String tlg : efdi.getTimeLogNames()) {
						timelogs.add(efdi.getTimeLog(tlg));
					}
					if(timelogs.isEmpty())
						throw new SDSDException("Couldn't find any timelogs in this file");
					if(timelogs.size() > 1)
						timelogs.sort((a,b) -> a.getFilename().compareTo(b.getFilename()));
				} else {
					TimeLog tlg = efdi.getTimeLog(name);
					if(tlg != null)
						timelogs.add(tlg);
					else
						throw new FileNotFoundException("Couldn't find timelog " + name);
					trimDeviceDescription(deviceDescription, timelogs.get(0));
				}
				
				if(deviceDescription.getDeviceCount() == 0)
					throw new SDSDException("No devices in device description");
				
				final String contextId = UUID.randomUUID().toString();
				
				try {
					if(SEND) application.agrirouter.sendDeviceDescription(user, deviceDescription, contextId).get();
					else System.out.println("DeviceDescription...");
					
					for(TimeLog timelog : timelogs) {
						if(skip < timelog.getTimeCount()) {
							if(rTime != null) {
								GrpcEfdi.TimeLog.Builder times = GrpcEfdi.TimeLog.newBuilder();
								for(int i = skip; i < timelog.getTimeCount(); ++i) {
									times.addTime(replaceTime(timelog, skip, timelog.getTime(i), rTime));
								}
								timelog = times.build();
							}
							
							if(timelog.getTimeCount() > 0) {
								if(SEND) application.agrirouter.sendTimelog(user, timelog, contextId).get();
								else System.out.println(timelog.getTimeCount() + " timelog entries...");
							}
							
							if(rTime != null)
								rTime = timelog.getTime(timelog.getTimeCount() - 1).getStart();
						}
						skip = Math.max(skip - timelog.getTimeCount(), 0);
					}
					
					application.logInfo(user, "Sent %d timelogs from %s", timelogs.size(), file.getFilename());
				} catch (ExecutionException e) {
					throw e.getCause();
				}
				return success(true);
			}
		} catch (IOException | ParseException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	/**
	 * Simulate.
	 *
	 * @param req the req
	 * @param fileid the fileid
	 * @param name the name
	 * @param skip the skip
	 * @param interval the interval
	 * @param scale the scale
	 * @param replaceTime the replace time
	 * @param endless the endless
	 * @return the JSON object
	 * @throws JsonRpcException the json rpc exception
	 */
	public JSONObject simulate(HttpServletRequest req, String fileid, String name, 
			int skip, int interval, float scale, String replaceTime, boolean endless) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			if(name == null) name = "";
			System.out.format("simulate: user(%s) file(%s) name(%s) skip(%d) interval(%d) scale(%f) replaceTime(%s) endless(%b)\n",
					(user != null ? user.getName() : "none"), fileid, name.isEmpty() ? "all" : name, 
							skip, interval, scale, replaceTime != null ? replaceTime : "none", endless);
			
			ObjectId fid = new ObjectId(fileid);
			if (user == null) 
				throw new NoLoginException();
			else if (user.agrirouter() == null)
				throw new NoOnboardingException();
			else if (!application.list.files.exists(user, fid))
				throw new SDSDException("File not found");
			else if (interval < 5)
				throw new SDSDException("Interval should by at least 5 seconds");
			else if (scale < 0.1 || scale > 1000000)
				throw new SDSDException("Invalid time scale");
			else {
				Timestamp rTime = replaceTime == null || replaceTime.isEmpty() ? null : Timestamps.parse(replaceTime);
				if(skip < 0) skip = 0;
				
				File file = application.list.files.get(user, fid);
				byte[] content = application.file.downloadFile(user, file);
				EfdiTimeLog efdi = file.isTimeLog() ? new EfdiTimeLog(content) : isoxmlToEfdi(content);
				
				ISO11783_TaskData deviceDescription = efdi.getDeviceDescription();
				if(deviceDescription == null) throw new FileNotFoundException("No DeviceDescription found");
				List<TimeLog> timelogs = new ArrayList<>();
				if(name.isEmpty()) {
					for(String tlg : efdi.getTimeLogNames()) {
						timelogs.add(efdi.getTimeLog(tlg));
					}
					if(timelogs.isEmpty())
						throw new SDSDException("Couldn't find any timelogs in this file");
					if(timelogs.size() > 1)
						timelogs.sort((a,b) -> a.getFilename().compareTo(b.getFilename()));
				} else {
					TimeLog tlg = efdi.getTimeLog(name);
					if(tlg != null)
						timelogs.add(tlg);
					else
						throw new FileNotFoundException("Couldn't find timelog " + name);
					trimDeviceDescription(deviceDescription, timelogs.get(0));
				}
				
				if(deviceDescription.getDeviceCount() == 0)
					throw new SDSDException("No devices in device description");

				TimelogIterator tlgIt = new TimelogIterator(user, deviceDescription, timelogs, skip, interval, scale, rTime, endless);
				if(!tlgIt.hasNext())
					throw new SDSDException(name.isEmpty() 
							? "All Timelogs in this file are empty" 
							: ("Timelog " + name + " is empty"));
				
				TelemetrySimulator simulator = tlgIt.next();
				TelemetrySimulator old = runningSimulators.put(user, simulator);
				if(old != null) {
					try {
						System.err.println("Already running simulation: " + old.progress().toString(2));
					} catch (Exception e) {
						e.printStackTrace();
					}
					old.cancel();
				}
				
				simulator.start();
				application.logInfo(user, "Started simulation of %s: %s", file.getFilename(), name.isEmpty() ? "all" : name);
				
				return simulator.progress();
			}
		} catch (IOException | ParseException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	
	/**
	 * Gets the duration.
	 *
	 * @param timelog the timelog
	 * @param skip the skip
	 * @return the duration
	 */
	public static long getDuration(TimeLog timelog, int skip) {
		Timestamp start = timelog.getTime(skip).getStart();
		Timestamp end = timelog.getTime(timelog.getTimeCount()-1).getStart();
		return Timestamps.toMillis(end) - Timestamps.toMillis(start);
	}
	
	/**
	 * Iterator for timelogs to sequentially simulate multiple timelogs.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private class TimelogIterator implements Iterator<TelemetrySimulator> {
		
		/** The context id. */
		public final String contextId = UUID.randomUUID().toString();
		
		/** The last error. */
		public final AtomicReference<String> lastError = new AtomicReference<>();
		
		/** The user. */
		private final User user;
		
		/** The device description. */
		private final ISO11783_TaskData deviceDescription;
		
		/** The timelogs. */
		private final List<TimeLog> timelogs;
		
		/** The interval. */
		private final int interval;
		
		/** The scale. */
		private final double scale;
		
		/** The endless. */
		private final boolean endless;
		
		/** The index. */
		private int skip, index;
		
		/** The remaining. */
		private long remaining;
		
		/** The replace time. */
		@CheckForNull
		private Timestamp replaceTime;
		
		/** The started. */
		@CheckForNull
		private Timestamp started = null;
		
		/**
		 * Instantiates a new timelog iterator.
		 *
		 * @param user the user
		 * @param deviceDescription the device description
		 * @param timelogs the timelogs
		 * @param skip the skip
		 * @param interval the interval
		 * @param scale the scale
		 * @param replaceTime the replace time
		 * @param endless the endless
		 * @throws FileNotFoundException the file not found exception
		 * @throws SAXException the SAX exception
		 * @throws IOException Signals that an I/O exception has occurred.
		 */
		public TimelogIterator(User user, ISO11783_TaskData deviceDescription, List<TimeLog> timelogs, 
				int skip, int interval, float scale, @Nullable Timestamp replaceTime, boolean endless) 
				throws FileNotFoundException, SAXException, IOException {
			this.user = user;
			this.deviceDescription = deviceDescription;
			this.skip = skip;
			this.interval = interval;
			this.scale = scale;
			this.replaceTime = replaceTime;
			this.endless = endless;
			this.timelogs = timelogs;
			this.skip = skip;
			reset();
		}
		
		/**
		 * Reset.
		 */
		public void reset() {
			index = 0;
			remaining = 0;
			int skip = this.skip;
			for(TimeLog tlg : timelogs) {
				if(skip < tlg.getTimeCount())
					remaining += getDuration(tlg, skip);
				skip = Math.max(skip - tlg.getTimeCount(), 0);
			}
		}
		
		/**
		 * Gets the remaining.
		 *
		 * @return the remaining
		 */
		public long getRemaining() {
			return remaining;
		}
		
		/**
		 * Gets the started.
		 *
		 * @return the started
		 */
		@CheckForNull
		public Timestamp getStarted() {
			return started;
		}
		
		/**
		 * Sets the started.
		 *
		 * @throws ARException the AR exception
		 * @throws IOException Signals that an I/O exception has occurred.
		 * @throws SAXException the SAX exception
		 */
		public void setStarted() throws ARException, IOException, SAXException {
			if(started == null) {
				if(SEND) application.agrirouter.sendDeviceDescription(user, deviceDescription, contextId).exceptionally(this::onError);
				else System.out.println("DeviceDescription...");
				
				started = Timestamps.fromMillis(System.currentTimeMillis());
			}
		}

		/**
		 * Checks for next.
		 *
		 * @return true, if successful
		 */
		@Override
		public boolean hasNext() {
			if(remaining > 0) return true;
			if(!endless) return false;
			for(TimeLog tlg : timelogs) {
				if(tlg.getTimeCount() > 0) return true;
			}
			return false;
		}

		/**
		 * Next.
		 *
		 * @return the telemetry simulator
		 */
		@Override
		public TelemetrySimulator next() {
			TelemetrySimulator simulator = null;
			while(simulator == null) {
				if(endless && index >= timelogs.size()) reset();
				TimeLog timelog = timelogs.get(index++);
				if(skip < timelog.getTimeCount()) {
					long duration = getDuration(timelog, skip);
					remaining -= duration;
					simulator = new TelemetrySimulator(user, timelog, skip, interval, scale, replaceTime, this);
					if(replaceTime != null)
						replaceTime = Timestamps.fromMillis(Timestamps.toMillis(replaceTime) + duration);
				}
				skip = Math.max(skip - timelog.getTimeCount(), 0);
			}
			return simulator;
		}
		
		/**
		 * Next name.
		 *
		 * @return the string
		 */
		@CheckForNull
		public String nextName() {
			return hasNext() ? timelogs.get(index % timelogs.size()).getFilename() : null;
		}
		
		/**
		 * On error.
		 *
		 * @param e the e
		 * @return true, if successful
		 */
		public boolean onError(Throwable e) {
			if(e != null) {
				lastError.set(e.getLocalizedMessage());
				application.logError(user, "Simulation error: " + e.getLocalizedMessage());
				System.err.println(user.getName() + ": Simulation error: " + e.getMessage());
			}
			return false;
		}
		
	}
	
	/**
	 * Simulates a driving machine by sending messages from a recorded timelog.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private class TelemetrySimulator {
		
		/** The chain. */
		private final TimelogIterator chain;
		
		/** The user. */
		public final User user;
		
		/** The timelog. */
		public final TimeLog timelog;
		
		/** The interval. */
		public final int skip, interval;
		
		/** The scale. */
		public final double scale;
		
		/** The replace time. */
		public final Timestamp replaceTime;
		
		/** The index. */
		private int index = 0;
		
		/** The time diff. */
		private double timeDiff = 0.;
		
		/** The eta. */
		private Instant eta = Instant.EPOCH;
		
		/** The schedule. */
		private ScheduledFuture<?> schedule = null;
		
		/**
		 * Instantiates a new telemetry simulator.
		 *
		 * @param user the user
		 * @param timelog the timelog
		 * @param skip the skip
		 * @param interval the interval
		 * @param scale the scale
		 * @param replaceTime the replace time
		 * @param chain the chain
		 */
		public TelemetrySimulator(User user, TimeLog timelog, int skip,
				int interval, double scale, @Nullable Timestamp replaceTime, 
				TimelogIterator chain) {
			this.chain = chain;
			this.user = user;
			this.timelog = timelog;
			skip = Math.min(skip, timelog.getTimeCount()-1);
			this.skip = skip;
			this.interval = interval;
			this.scale = scale;
			this.replaceTime = replaceTime;
			this.index = skip;
		}
		
		/**
		 * Start.
		 *
		 * @throws ARException the AR exception
		 * @throws IOException Signals that an I/O exception has occurred.
		 * @throws SAXException the SAX exception
		 */
		public void start() throws ARException, IOException, SAXException {
			chain.setStarted();
			resume();
		}
		
		/**
		 * Gets the cur timestamp.
		 *
		 * @return the cur timestamp
		 */
		public Timestamp getCurTimestamp() {
			return timelog.getTime(Math.max(index-1, 0)).getStart();
		}
		
		/**
		 * Gets the end timestamp.
		 *
		 * @return the end timestamp
		 */
		public Timestamp getEndTimestamp() {
			return timelog.getTime(timelog.getTimeCount()-1).getStart();
		}
		
		/**
		 * Resume.
		 */
		public void resume() {
			if(schedule == null || schedule.isDone()) {
				
				this.timeDiff = Timestamps.toMillis(getCurTimestamp()) - scale * System.currentTimeMillis();
				this.schedule = application.executor.scheduleAtFixedRate(sender, interval, interval, TimeUnit.SECONDS);
				this.eta = Instant.ofEpochMilli(Math.round((Timestamps.toMillis(getEndTimestamp()) - timeDiff + chain.getRemaining()) / scale));
			}
		}
		
		/**
		 * Cancel.
		 */
		public void cancel() {
			if(schedule != null) {
				schedule.cancel(false);
				schedule = null;
			}
		}
		
		/**
		 * Progress.
		 *
		 * @return the JSON object
		 * @throws Throwable the throwable
		 */
		public JSONObject progress() throws Throwable {
			Timestamp tlStart = timelog.getTime(skip).getStart();
			Timestamp tlCur = isRunning() 
							? Timestamps.fromMillis(Math.round(scale * System.currentTimeMillis() + timeDiff))
							: getCurTimestamp();
			Timestamp tlEnd = getEndTimestamp();
			if(replaceTime != null) {
				tlCur = Timestamps.add(replaceTime, Timestamps.between(tlStart, tlCur));
				tlEnd = Timestamps.add(replaceTime, Timestamps.between(tlStart, tlEnd));
				tlStart = replaceTime;
			}
			return new JSONObject()
					.put("name", timelog.getFilename())
					.put("interval", interval)
					.put("scale", scale)
					.put("replaceTime", replaceTime != null ? Timestamps.toString(replaceTime) : null)
					.put("state", isRunning() ? "running" : isDone() ? "done" : "paused")
					.put("error", chain.lastError.getAndSet(null))
					.put("position", new JSONObject()
							.put("current", index)
							.put("size", timelog.getTimeCount()))
					.put("timeLog", new JSONObject()
							.put("start", Timestamps.toString(tlStart))
							.put("current", Timestamps.toString(tlCur))
							.put("end",	Timestamps.toString(tlEnd)))
					.put("simulation", new JSONObject()
							.put("start", chain.getStarted() != null ? Timestamps.toString(chain.getStarted()) : null)
							.put("current", isoUTC(Instant.now()))
							.put("end",	isoUTC(eta)))
					.put("following", chain.nextName());
		}
		
		/**
		 * Checks if is running.
		 *
		 * @return true, if is running
		 */
		public boolean isRunning() {
			return schedule != null && !schedule.isDone();
		}
		
		/**
		 * Checks if is done.
		 *
		 * @return true, if is done
		 */
		public boolean isDone() {
			return  index >= timelog.getTimeCount();
		}
		
		/** The sender. */
		private final Runnable sender = new Runnable() {
			@Override
			public void run() {
				try {
					GrpcEfdi.TimeLog.Builder times = GrpcEfdi.TimeLog.newBuilder()
							.setFilename(timelog.getFilename());
					while(index < timelog.getTimeCount()) {
						Timestamp time = timelog.getTime(index).getStart();
						if(Timestamps.toMillis(time) > scale * System.currentTimeMillis() + timeDiff)
							break;
						Time entry = timelog.getTime(index);
						times.addTime(replaceTime != null ? replaceTime(timelog, skip, entry, replaceTime) : entry);
						++index;
					}
					
					if(times.getTimeCount() > 0) {
						if(SEND) {
							try {
								application.agrirouter.sendTimelog(user, times.build(), chain.contextId).exceptionally(chain::onError);
							} catch (ARException e) {
								chain.onError(e);
							}
						} else System.out.println(times.getTimeCount() + " timelog entries...");
						onSended.trigger(user, TelemetrySimulator.this);
					}
				} catch(Throwable e) {
					chain.lastError.set(INTERNAL_ERROR);
					application.logError(user, INTERNAL_ERROR);
					e.printStackTrace();
				} finally {
					if(index >= timelog.getTimeCount()) {
						startNext();
					}
				}
			}
		};
		
		/**
		 * Start next.
		 *
		 * @return the telemetry simulator
		 */
		@CheckForNull
		public TelemetrySimulator startNext() {
			try {
				cancel();
				if(chain.hasNext()) {
					TelemetrySimulator sim = chain.next();
					if(runningSimulators.replace(user, this, sim)) {
						sim.start();
						return sim;
					}
				}
			} catch (Throwable e) {
				chain.onError(e);
			}
			return null;
		}
	}
	
	/**
	 * Isoxml to efdi.
	 *
	 * @param content the content
	 * @return the efdi time log
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static EfdiTimeLog isoxmlToEfdi(byte[] content) throws IOException {
		Process process = new ProcessBuilder("java", "-jar", "parser/isoxml.jar", "efdi").start();
		try (OutputStream processIn = process.getOutputStream()) {
			IOUtils.copy(new ByteArrayInputStream(content), processIn);
		}
		return new EfdiTimeLog(process.getInputStream());
	}
	
	/**
	 * Trim device description.
	 *
	 * @param deviceDescription the device description
	 * @param timelog the timelog
	 * @return the grpc efdi. ISO 11783 task data
	 */
	public static GrpcEfdi.ISO11783_TaskData trimDeviceDescription(GrpcEfdi.ISO11783_TaskData deviceDescription, GrpcEfdi.TimeLog timelog) {
		Set<Long> deviceElements = timelog.getTimeList().stream()
				.flatMap(time -> time.getDataLogValueList().stream())
				.map(GrpcEfdi.DataLogValue::getDeviceElementIdRef)
				.mapToLong(GrpcEfdi.UID::getNumber)
				.collect(HashSet::new, HashSet::add, HashSet::addAll);
		
		GrpcEfdi.ISO11783_TaskData.Builder ddb = deviceDescription.toBuilder();
		for(int i = ddb.getDeviceCount()-1; i >= 0; --i) {
			if(!ddb.getDevice(i).getDeviceElementList().stream()
					.map(GrpcEfdi.DeviceElement::getDeviceElementId)
					.map(GrpcEfdi.UID::getNumber)
					.anyMatch(deviceElements::contains)) {
				ddb.removeDevice(i);
			}
		}
		
		return ddb.build();
	}
	
	/**
	 * Replace time.
	 *
	 * @param timelog the timelog
	 * @param skip the skip
	 * @param entry the entry
	 * @param replacedStart the replaced start
	 * @return the grpc efdi. time
	 */
	public static GrpcEfdi.Time replaceTime(GrpcEfdi.TimeLog timelog, int skip, GrpcEfdi.Time entry, @Nullable Timestamp replacedStart) {
		if(replacedStart == null) return entry;
		Timestamp tlStart = timelog.getTime(skip).getStart();
		Timestamp time = entry.getStart();
		return entry.toBuilder()
				.setStart(Timestamps.add(replacedStart, Timestamps.between(tlStart, time)))
				.build();
	}

}
