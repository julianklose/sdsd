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
	private final SDSDEvent<User, TelemetrySimulator> onSended = new SDSDEvent<>();
	
	private static final boolean SEND = true;
	
	private final ConcurrentHashMap<User, TelemetrySimulator> runningSimulators = new ConcurrentHashMap<>();

	public SimulatorEndpoint(ApplicationLogic application) {
		super(application);
	}
	
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
				
				Map<Long, Device> deviceMap = new HashMap<>();
				for(Device device : deviceDescription.getDeviceList()) {
					for(DeviceElement det : device.getDeviceElementList()) {
						deviceMap.put(det.getDeviceElementId().getNumber(), device);
					}
				}
				
				JSONObject result = new JSONObject();
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
					
					result.put(name, info);
				}
				
				return result;
			}
		} catch (IOException e) {
			throw createError(user, new SDSDException(e.getMessage()));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
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
		
		public final String contextId = UUID.randomUUID().toString();
		public final AtomicReference<String> lastError = new AtomicReference<>();
		
		private final User user;
		private final ISO11783_TaskData deviceDescription;
		private final List<TimeLog> timelogs;
		private final int interval;
		private final double scale;
		private final boolean endless;
		
		private int skip, index;
		private long remaining;
		@CheckForNull
		private Timestamp replaceTime;
		@CheckForNull
		private Timestamp started = null;
		
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
		
		public long getRemaining() {
			return remaining;
		}
		
		@CheckForNull
		public Timestamp getStarted() {
			return started;
		}
		
		public void setStarted() throws ARException, IOException, SAXException {
			if(started == null) {
				if(SEND) application.agrirouter.sendDeviceDescription(user, deviceDescription, contextId).exceptionally(this::onError);
				else System.out.println("DeviceDescription...");
				
				started = Timestamps.fromMillis(System.currentTimeMillis());
			}
		}

		@Override
		public boolean hasNext() {
			if(remaining > 0) return true;
			if(!endless) return false;
			for(TimeLog tlg : timelogs) {
				if(tlg.getTimeCount() > 0) return true;
			}
			return false;
		}

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
		
		@CheckForNull
		public String nextName() {
			return hasNext() ? timelogs.get(index % timelogs.size()).getFilename() : null;
		}
		
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
		private final TimelogIterator chain;
		public final User user;
		public final TimeLog timelog;
		
		public final int skip, interval;
		public final double scale;
		public final Timestamp replaceTime;
		
		private int index = 0;
		private double timeDiff = 0.;
		private Instant eta = Instant.EPOCH;
		private ScheduledFuture<?> schedule = null;
		
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
		
		public void start() throws ARException, IOException, SAXException {
			chain.setStarted();
			resume();
		}
		
		public Timestamp getCurTimestamp() {
			return timelog.getTime(Math.max(index-1, 0)).getStart();
		}
		
		public Timestamp getEndTimestamp() {
			return timelog.getTime(timelog.getTimeCount()-1).getStart();
		}
		
		public void resume() {
			if(schedule == null || schedule.isDone()) {
				
				this.timeDiff = Timestamps.toMillis(getCurTimestamp()) - scale * System.currentTimeMillis();
				this.schedule = application.executor.scheduleAtFixedRate(sender, interval, interval, TimeUnit.SECONDS);
				this.eta = Instant.ofEpochMilli(Math.round((Timestamps.toMillis(getEndTimestamp()) - timeDiff + chain.getRemaining()) / scale));
			}
		}
		
		public void cancel() {
			if(schedule != null) {
				schedule.cancel(false);
				schedule = null;
			}
		}
		
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
		
		public boolean isRunning() {
			return schedule != null && !schedule.isDone();
		}
		
		public boolean isDone() {
			return  index >= timelog.getTimeCount();
		}
		
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
	
	public static EfdiTimeLog isoxmlToEfdi(byte[] content) throws IOException {
		Process process = new ProcessBuilder("java", "-jar", "parser/isoxml.jar", "efdi").start();
		try (OutputStream processIn = process.getOutputStream()) {
			IOUtils.copy(new ByteArrayInputStream(content), processIn);
		}
		return new EfdiTimeLog(process.getInputStream());
	}
	
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
	
	public static GrpcEfdi.Time replaceTime(GrpcEfdi.TimeLog timelog, int skip, GrpcEfdi.Time entry, @Nullable Timestamp replacedStart) {
		if(replacedStart == null) return entry;
		Timestamp tlStart = timelog.getTime(skip).getStart();
		Timestamp time = entry.getStart();
		return entry.toBuilder()
				.setStart(Timestamps.add(replacedStart, Timestamps.between(tlStart, time)))
				.build();
	}

}
