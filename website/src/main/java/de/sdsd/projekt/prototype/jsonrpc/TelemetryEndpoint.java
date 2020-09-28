package de.sdsd.projekt.prototype.jsonrpc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;

import agrirouter.technicalmessagetype.Gps;
import de.sdsd.projekt.agrirouter.ARException;
import de.sdsd.projekt.agrirouter.ARMessageType;
import de.sdsd.projekt.agrirouter.request.feed.ARMsgHeader;
import de.sdsd.projekt.prototype.applogic.AgrirouterFunctions.ReceivedMessageResult;
import de.sdsd.projekt.prototype.applogic.ApplicationLogic;
import de.sdsd.projekt.prototype.data.EfdiTimeLog;
import de.sdsd.projekt.prototype.data.File;
import de.sdsd.projekt.prototype.data.SDSDException;
import de.sdsd.projekt.prototype.data.User;
import de.sdsd.projekt.prototype.websocket.WebsocketConnection;
import efdi.GrpcEfdi;

/**
 * JSONRPC-Endpoint for telemetry viewer functions.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class TelemetryEndpoint extends JsonRpcEndpoint {

	public TelemetryEndpoint(ApplicationLogic application) {
		super(application);
	}
	
	public JSONObject telemetry(HttpServletRequest req, String fileid, int offset, int limit) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("telemetry: user(" + (user != null ? user.getName() : "none") 
					+ ") file(" + fileid + ") offset(" + offset + ") limit(" + limit + ")");
			
			ObjectId fid = new ObjectId(fileid);
			if (user == null) 
				throw new NoLoginException();
			else if(!application.list.files.exists(user, fid))
				throw new SDSDException("File not found");
			else {
				File file = application.list.files.get(user, fid);
				if(file.isTimeLog())
					return efdiTelemetry(user, file, offset, limit);
				else if(file.isGpsInfo())
					return gpsTelemetry(user, file, offset, limit);
				else 
					throw new SDSDException("File is no telemetry format");
			}
		} catch (FileNotFoundException e) {
			throw createError(user, new SDSDException("File not found"));
		} catch (InvalidProtocolBufferException e) {
			throw createError(user, new SDSDException("File content invalid"));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}
	
	public JSONObject setUpdateListener(HttpServletRequest req, WebsocketConnection conn, String fileid) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("setUpdateListener: user(" + (user != null ? user.getName() : "none") 
					+ ") file(" + fileid + ")");
			
			ObjectId fid = new ObjectId(fileid);
			if (user == null)
				throw new NoLoginException();
			else if(!application.list.files.exists(user, fid))
				throw new SDSDException("File not found");
			else {
				File file = application.list.files.get(user, fid);
				if(file.isTimeLog())
					return setEfdiUpdateListener(user, file, conn, fileid);
				else if(file.isGpsInfo())
					return setGpsUpdateListener(user, file, conn, fileid);
				else 
					throw new SDSDException("File is no telemetry format");
			}
		} catch (FileNotFoundException e) {
			throw createError(user, new SDSDException("File not found"));
		} catch (InvalidProtocolBufferException e) {
			throw createError(user, new SDSDException("File content invalid"));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}

	public JSONObject updateTelemetry(HttpServletRequest req, String fileid, int offset, int limit) throws JsonRpcException {
		User user = null;
		try {
			user = application.getUser(getSessionId(req));
			System.out.println("updateTelemetry: user(" + (user != null ? user.getName() : "none") 
					+ ") file(" + fileid + ") offset(" + offset + ") limit(" + limit + ")");

			ObjectId fid = new ObjectId(fileid);
			if (user == null) 
				throw new NoLoginException();
			else if(!application.list.files.exists(user, fid))
				throw new SDSDException("File not found");
			else if(offset < 0)
				throw new SDSDException("offset must not be negative");
			else {
				File file = application.list.files.get(user, fid);
				if(file.isTimeLog())
					return updateEfdiTelemetry(user, file, offset, limit);
				else if(file.isGpsInfo())
					return updateGpsTelemetry(user, file, offset, limit);
				else 
					throw new SDSDException("File is no telemetry format");
			}
		} catch (FileNotFoundException e) {
			throw createError(user, new SDSDException("File not found"));
		} catch (InvalidProtocolBufferException e) {
			throw createError(user, new SDSDException("File content invalid"));
		} catch (Throwable e) {
			throw createError(user, e);
		}
	}

	/**
	 * List of timelog value infos.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private static class DlvInfo implements Iterable<TimelogValueInfo> {
		private final HashMap<Key, TimelogValueInfo> map = new HashMap<>();
		private TimelogValueInfo first = null;

		public void put(GrpcEfdi.Device dvc, GrpcEfdi.DeviceElement det, GrpcEfdi.DeviceProcessData dpd, GrpcEfdi.DeviceValuePresentation dvp) {
			put(new TimelogValueInfo(dvc, det, dpd, dvp));
		}

		public void put(GrpcEfdi.Device dvc, GrpcEfdi.DeviceElement det, GrpcEfdi.DeviceProperty dpt, GrpcEfdi.DeviceValuePresentation dvp) {
			put(new TimelogValueInfo(dvc, det, dpt, dvp));
		}

		private void put(TimelogValueInfo tvi) {
			TimelogValueInfo old = map.put(new Key(tvi.det.getDeviceElementId(), tvi.getDDI()), tvi);

			if(old != null) {
				tvi.next = old.next;
				if(old == first) first = tvi;
				else {
					TimelogValueInfo before = first;
					while(before.next != old) before = before.next;
					before.next = tvi;
				}
			}
			else {
				TimelogValueInfo before = null, after = first;
				while(after != null && tvi.compareTo(after) > 0) {
					before = after;
					after = after.next;
				}
				if(before != null) before.next = tvi;
				else first = tvi;
				tvi.next = after;
			}
		}

		public TimelogValueInfo get(GrpcEfdi.DataLogValue dlv) {
			return map.get(new Key(dlv.getDeviceElementIdRef(), dlv.getProcessDataDdi()));
		}

		private static final class Key {
			public final GrpcEfdi.UID det;
			public final int ddi;

			Key(GrpcEfdi.UID det, int ddi) {
				this.det = det;
				this.ddi = ddi;
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + ddi;
				result = prime * result + Long.hashCode(det.getNumber());
				return result;
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				Key other = (Key) obj;
				if (ddi != other.ddi)
					return false;
				if (det.getNumber() != other.det.getNumber())
					return false;
				return true;
			}
		}

		@Override
		public Iterator<TimelogValueInfo> iterator() {
			return new Iterator<TimelogValueInfo>() {
				private TimelogValueInfo next = first;

				@Override
				public TimelogValueInfo next() {
					if(next == null) return null;
					TimelogValueInfo out = next;
					next = next.next;
					return out;
				}

				@Override
				public boolean hasNext() {
					return next != null;
				}
			};
		}

	}

	/**
	 * Value information for timelog columns.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private static class TimelogValueInfo implements Comparable<TimelogValueInfo> {

		public final GrpcEfdi.Device dvc;
		public final GrpcEfdi.DeviceElement det;
		@CheckForNull
		public final GrpcEfdi.DeviceProcessData dpd;
		@CheckForNull
		public final GrpcEfdi.DeviceProperty dpt;
		@CheckForNull
		public final GrpcEfdi.DeviceValuePresentation dvp;
		private final NumberFormat format = DecimalFormat.getInstance();
		public int index = -1;
		public TimelogValueInfo next = null;
		public long startValue;

		public TimelogValueInfo(GrpcEfdi.Device dvc, GrpcEfdi.DeviceElement det, 
				GrpcEfdi.DeviceProcessData dpd, @Nullable GrpcEfdi.DeviceValuePresentation dvp) {
			this.dvc = dvc;
			this.det = det;
			this.dpd = dpd;
			this.dpt = null;
			this.dvp = dvp;
			this.startValue = 0;
			if(dvp != null) 
				format.setMaximumFractionDigits(dvp.getNumberOfDecimals());
		}

		public TimelogValueInfo(GrpcEfdi.Device dvc, GrpcEfdi.DeviceElement det, 
				GrpcEfdi.DeviceProperty dpt, @Nullable GrpcEfdi.DeviceValuePresentation dvp) {
			this.dvc = dvc;
			this.det = det;
			this.dpd = null;
			this.dpt = dpt;
			this.dvp = dvp;
			this.startValue = dpt.getDevicePropertyValue();
			if(dvp != null) 
				format.setMaximumFractionDigits(dvp.getNumberOfDecimals());
		}

		public int getDDI() {
			return dpd != null ? dpd.getDeviceProcessDataDdi() : dpt != null ? dpt.getDevicePropertyDdi() : 0;
		}

		public String getDesignator() {
			String designator = dpd != null ? dpd.getDeviceProcessDataDesignator() : dpt != null ? dpt.getDevicePropertyDesignator() : "";
			return designator.isEmpty() ? Integer.toString(getDDI()) : designator;
		}
		
		public String formatValue(long value) {
			if(dvp != null) {
				double val = (value + dvp.getOffset()) * dvp.getScale();
				return format.format(val);
			}
			return Long.toString(value);
		}
		
		public String getUnit() {
			return dvp != null ? dvp.getUnitDesignator() : "";
		}

		@Override
		public int compareTo(TimelogValueInfo o) {
			int cmp = Long.compare(dvc.getDeviceId().getNumber(), o.dvc.getDeviceId().getNumber());
			if(cmp == 0) cmp = Long.compare(det.getDeviceElementId().getNumber(), o.det.getDeviceElementId().getNumber());
			if(cmp == 0) cmp = Integer.compare(getDDI(), o.getDDI());
			return cmp;
		}
	}

	public static JSONObject position(GrpcEfdi.Position pos) {
		return new JSONObject()
				.put("lat", pos.getPositionNorth())
				.put("long", pos.getPositionEast());
	}

	public static JSONArray geoPosition(GrpcEfdi.Position pos, Timestamp time) {
		return new JSONArray()
				.put(pos.getPositionEast())
				.put(pos.getPositionNorth())
				.put(pos.getPositionUp())
				.put(TimeUnit.SECONDS.toMillis(time.getSeconds()));
	}

	DlvInfo buildInfoMap(GrpcEfdi.ISO11783_TaskData deviceDescription) {

		DlvInfo dlvInfo = new DlvInfo();
		HashMap<Integer, GrpcEfdi.DeviceValuePresentation> dvps = new HashMap<>();
		HashMap<Integer, GrpcEfdi.DeviceProcessData> dpds = new HashMap<>();
		HashMap<Integer, GrpcEfdi.DeviceProperty> dpts = new HashMap<>();
		for(GrpcEfdi.Device dvc: deviceDescription.getDeviceList()) {
			for (GrpcEfdi.DeviceValuePresentation dvp : dvc.getDeviceValuePresentationList())
				dvps.put(dvp.getDeviceValuePresentationObjectId(), dvp);
			for (GrpcEfdi.DeviceProcessData dpd : dvc.getDeviceProcessDataList())
				dpds.put(dpd.getDeviceProcessDataObjectId(), dpd);
			for (GrpcEfdi.DeviceProperty dpt : dvc.getDevicePropertyList())
				dpts.put(dpt.getDevicePropertyObjectId(), dpt);

			for (GrpcEfdi.DeviceElement det : dvc.getDeviceElementList()) {
				for (GrpcEfdi.DeviceObjectReference dor : det.getDeviceObjectReferenceList()) {
					GrpcEfdi.DeviceProcessData dpd;
					GrpcEfdi.DeviceProperty dpt;
					if((dpd = dpds.get(dor.getDeviceObjectId())) != null) {
						dlvInfo.put(dvc, det, dpd, dvps.get(dpd.getDeviceValuePresentationObjectId()));
					} 
					else if((dpt = dpts.get(dor.getDeviceObjectId())) != null) {
						dlvInfo.put(dvc, det, dpt, dvps.get(dpt.getDeviceValuePresentationObjectId()));
					}
				}
			}
			dvps.clear();
			dpds.clear();
		}
		return dlvInfo;
	}

	JSONObject createCaption(DlvInfo dlvInfo) {
		JSONObject empty = new JSONObject().put("span", 2).put("label", "");
		JSONArray dvcs = new JSONArray().put(empty);
		JSONArray dets = new JSONArray().put(empty);
		JSONArray dlvs = new JSONArray()
				.put(new JSONObject().put("label", "Time"))
				.put(new JSONObject().put("label", "Position"));
		GrpcEfdi.Device dvc = null;
		GrpcEfdi.DeviceElement det = null;
		int dvcc = 0, detc = 0;
		for(TimelogValueInfo tvi: dlvInfo) {
			if(dvc == null) {
				dvc = tvi.dvc;
				dvcc = 1;
			}
			else if(dvc.getDeviceId().getNumber() == tvi.dvc.getDeviceId().getNumber()) ++dvcc;
			else {
				dvcs.put(new JSONObject().put("span", dvcc).put("label", dvc.getDeviceDesignator()));
				dvc = tvi.dvc;
				dvcc = 1;
			}

			if(det == null) {
				det = tvi.det;
				detc = 1;
			}
			else if(det.getDeviceElementId().getNumber() == tvi.det.getDeviceElementId().getNumber()) ++detc;
			else {
				dets.put(new JSONObject().put("span", detc).put("label", det.getDeviceElementDesignator()));
				det = tvi.det;
				detc = 1;
			}

			dlvs.put(new JSONObject()
					.put("DDI", tvi.getDDI())
					.put("label", tvi.getDesignator()));
		}
		dvcs.put(new JSONObject().put("span", dvcc).put("label", dvc.getDeviceDesignator()));
		dets.put(new JSONObject().put("span", detc).put("label", det.getDeviceElementDesignator()));
		return new JSONObject().put("device", dvcs).put("deviceelement", dets).put("logvalues", dlvs);
	}

	JSONArray createEfdiTable(List<GrpcEfdi.Time> timeList, DlvInfo dlvInfo, List<GrpcEfdi.Time> startingTimeList) {
		for(GrpcEfdi.Time time : startingTimeList) {
			for (GrpcEfdi.DataLogValue dlv : time.getDataLogValueList()) {
				TimelogValueInfo tvi = dlvInfo.get(dlv);
				if(tvi != null)
					tvi.startValue = dlv.getProcessDataValue();
			}
		}
		JSONArray row = new JSONArray()
				.put(new JSONObject().put("value", 0).put("unit", ""))
				.put(new JSONObject().put("value", 0).put("unit", ""));
		int index = 1;
		for(TimelogValueInfo tvi: dlvInfo) {
			tvi.index = ++index;
			row.put(new JSONObject()
					.put("value", tvi.formatValue(tvi.startValue))
					.put("unit", tvi.getUnit()));
		}

		JSONArray out = new JSONArray();
		for (GrpcEfdi.Time time : timeList) {
			if(time.hasStart())
				row.getJSONObject(0).put("value", isoUTC(ARMsgHeader.timestampToInstant(time.getStart())));
			if(time.hasPositionStart())
				row.getJSONObject(1).put("value", position(time.getPositionStart()));
			for (GrpcEfdi.DataLogValue dlv : time.getDataLogValueList()) {
				TimelogValueInfo tvi = dlvInfo.get(dlv);
				if(tvi != null) {
					row.getJSONObject(tvi.index).put("value", tvi.formatValue(dlv.getProcessDataValue()));
				}
			}
			out.put(new JSONArray(row.toString()));
		}
		return out;
	}

	JSONObject createEfdiGeoJSON(List<GrpcEfdi.Time> timeList) {
		JSONArray lscoordinates = new JSONArray();
		JSONArray features = new JSONArray();
		features.put(new JSONObject()
				.put("type", "Feature")
				.put("geometry", new JSONObject()
						.put("type", "LineString")
						.put("coordinates", lscoordinates)));

		for (GrpcEfdi.Time time : timeList) {
			if(time.hasStart() && time.hasPositionStart()) {
				JSONArray coordinates = geoPosition(time.getPositionStart(), time.getStart());
				lscoordinates.put(coordinates);

				features.put(new JSONObject()
						.put("type", "Feature")
						.put("geometry", new JSONObject()
								.put("type", "Point")
								.put("coordinates", coordinates)));
			}
		}

		return new JSONObject()
				.put("type", "FeatureCollection")
				.put("features", features);
	}

	public static final Comparator<GrpcEfdi.Time> timeComparator = new Comparator<GrpcEfdi.Time>() {
		@Override
		public int compare(GrpcEfdi.Time o1, GrpcEfdi.Time o2) {
			Timestamp t1 = o1.getStart(), t2 = o2.getStart();
			int cmp = Long.compare(t1.getSeconds(), t2.getSeconds());
			if(cmp == 0) cmp = Integer.compare(t1.getNanos(), t2.getNanos());
			return cmp;
		}
	};
	
	private JSONObject toJson(int total, DlvInfo dlvInfo, List<GrpcEfdi.Time> timeList, List<GrpcEfdi.Time> startingTimeList) {
		return new JSONObject()
				.put("total", total)
				.put("caption", createCaption(dlvInfo))
				.put("telemetry", createEfdiTable(timeList, dlvInfo, startingTimeList))
				.put("geoJSON", createEfdiGeoJSON(timeList));
	}

	private JSONObject efdiTelemetry(User user, File file, int offset, int limit) 
			throws SDSDException, FileNotFoundException, InvalidProtocolBufferException {
		EfdiTimeLog efdiTimeLog = new EfdiTimeLog(application.file.downloadFile(user, file));
		GrpcEfdi.ISO11783_TaskData deviceDescription = efdiTimeLog.getDeviceDescription();
		Optional<String> tlgname = efdiTimeLog.getTimeLogNames().stream().findAny();
		GrpcEfdi.TimeLog timeLog = tlgname.isPresent() ? efdiTimeLog.getTimeLog(tlgname.get()) : null;
		
		if(timeLog == null) 
			throw new SDSDException("The time log is missing");
		if(deviceDescription == null) 
			throw new SDSDException("The corresponding device description is missing");

		DlvInfo dlvInfo = buildInfoMap(deviceDescription);

		if(offset < 0) offset = Math.max(timeLog.getTimeCount() + offset, 0);
		else if(offset > timeLog.getTimeCount()) offset = timeLog.getTimeCount();
		int end = limit < 0 ? timeLog.getTimeCount() : Math.min(offset + limit, timeLog.getTimeCount());
		List<GrpcEfdi.Time> sortedTimeList = timeLog.getTimeList().stream()
				.sorted(timeComparator)
				.limit(end)
				.collect(Collectors.toList());
		
		List<GrpcEfdi.Time> startingTimeList = sortedTimeList.subList(0, offset);
		List<GrpcEfdi.Time> timeList = sortedTimeList.subList(offset, end);

		return toJson(timeLog.getTimeCount(), dlvInfo, timeList, startingTimeList).put("offset", offset);
	}
	
	private JSONObject setEfdiUpdateListener(User user, File file, WebsocketConnection conn, String fileid) 
			throws SDSDException, FileNotFoundException, InvalidProtocolBufferException {
		EfdiTimeLog efdiTimeLog = new EfdiTimeLog(application.file.downloadFile(user, file));
		GrpcEfdi.ISO11783_TaskData deviceDescription = efdiTimeLog.getDeviceDescription();
		if(deviceDescription == null) 
			throw new SDSDException("The corresponding device description is missing");
		Optional<String> tlgname = efdiTimeLog.getTimeLogNames().stream().findAny();
		GrpcEfdi.TimeLog timeLog = tlgname.isPresent() ? efdiTimeLog.getTimeLog(tlgname.get()) : null;
		if(timeLog == null) 
			throw new SDSDException("The time log is missing");
		
		DlvInfo dlvInfo = buildInfoMap(deviceDescription);
		List<GrpcEfdi.Time> startingTimeList = timeLog.getTimeList().stream()
				.sorted(timeComparator)
				.collect(Collectors.toList());
		
		application.file.fileAppended.setListener(file, conn.listener("telemetry", "update", null, fileid, 
				new TelemetryUpdateListener(dlvInfo, startingTimeList)));
		
		return success(true);
	}
	
	/**
	 * Listener for telemetry updates, when connected over MQTT.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	private class TelemetryUpdateListener implements WebsocketConnection.CheckedFunction<byte[], JSONObject> {
		private final DlvInfo dlvInfo;
		private final List<GrpcEfdi.Time> startingTimeList;
		
		public TelemetryUpdateListener(DlvInfo dlvInfo, List<GrpcEfdi.Time> startingTimeList) {
			this.dlvInfo = dlvInfo;
			this.startingTimeList = startingTimeList;
		}

		@Override
		public JSONObject apply(byte[] content) throws InvalidProtocolBufferException {
			GrpcEfdi.TimeLog timeLog = GrpcEfdi.TimeLog.parseFrom(content);

			List<GrpcEfdi.Time> timeList = timeLog.getTimeList().stream()
					.sorted(timeComparator)
					.collect(Collectors.toList());

			JSONObject out = toJson(startingTimeList.size() + timeList.size(), dlvInfo, timeList, timeList);
			
			startingTimeList.addAll(timeList);
			
			return out;
		}
	}

	private JSONObject updateEfdiTelemetry(User user, File file, int offset, int limit) 
			throws SDSDException, ARException, IOException, FileNotFoundException, InvalidProtocolBufferException {
		final String contextId = application.file.getTimeLogContextId(file);
		if(contextId == null)
			throw new SDSDException("File is no TimeLog");
		Instant date = file.getCreated();
		if(date == null) date = Instant.now();
		CompletableFuture<Integer> arupdate = application.agrirouter.readMessageHeaders(user, 
				date.truncatedTo(ChronoUnit.DAYS), 
				date.plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS))
				.thenApply(headers -> headers.stream()
						.filter(h -> contextId.equals(h.getTeamSetContextId()) && !h.getIds().isEmpty() && h.isComplete())
						.collect(Collectors.toList()))
				.thenApply(headers -> receiveFiles(user, headers));

		// build the info map while waiting for new messages from agrirouter
		EfdiTimeLog efdiTimeLog = new EfdiTimeLog(application.file.downloadFile(user, file));
		GrpcEfdi.ISO11783_TaskData deviceDescription = efdiTimeLog.getDeviceDescription();
		if(deviceDescription == null) 
			throw new SDSDException("The corresponding device description is missing");
		DlvInfo dlvInfo = buildInfoMap(deviceDescription);

		try {
			int messagesReceived = arupdate.join();
			System.out.println("updateTelemetry: received " + messagesReceived + " new timelog messages");
		} catch (Throwable e) {
			if(e instanceof CompletionException) e = e.getCause();
			createError(user, e);
		}

		efdiTimeLog = new EfdiTimeLog(application.file.downloadFile(user, file));
		Optional<String> tlgname = efdiTimeLog.getTimeLogNames().stream().findAny();
		GrpcEfdi.TimeLog timeLog = tlgname.isPresent() ? efdiTimeLog.getTimeLog(tlgname.get()) : null;
		if(timeLog == null) 
			throw new SDSDException("The time log is missing");

		offset = Math.min(offset, timeLog.getTimeCount());
		int end = limit < 0 ? timeLog.getTimeCount() : Math.min(offset + limit, timeLog.getTimeCount());
		List<GrpcEfdi.Time> sortedTimeList = timeLog.getTimeList().stream()
				.sorted(timeComparator)
				.limit(end)
				.collect(Collectors.toList());
		
		List<GrpcEfdi.Time> startingTimeList = sortedTimeList.subList(0, offset);
		List<GrpcEfdi.Time> timeList = sortedTimeList.subList(offset, end);

		return toJson(timeLog.getTimeCount(), dlvInfo, timeList, startingTimeList).put("offset", offset);
	}

	private int receiveFiles(User user, List<ARMsgHeader> headers) {
		if(headers.isEmpty()) return 0;
		int received = 0;
		try {
			List<ReceivedMessageResult> results = application.agrirouter.receiveMessages(user, headers).join();
			for(int i = 0; i < results.size(); ++i) {
				ReceivedMessageResult res = results.get(i);
				if(res.isSaved()) {
					if(res.isNew())
						application.logInfo(user, "Received file: \"" + res.getName() + "\" from " 
								+ application.list.endpoints.get(user, headers.get(i).getSender()).getName());
					++received;
				}
				else if(res.isError()) {
					throw res.getError();
				}
				else {
					System.out.println("ARReceive: Discarded file because of missing storage task.");
					application.logInfo(user, "Discarded file because of missing storage task");
				}
			}
		} catch (Throwable e) {
			if(e instanceof CompletionException) e = e.getCause();
			createError(user, e);
		}
		return received;
	}
	
	public static final Comparator<Gps.GPSList.GPSEntry> gpsComparator = new Comparator<Gps.GPSList.GPSEntry>() {
		@Override
		public int compare(Gps.GPSList.GPSEntry o1, Gps.GPSList.GPSEntry o2) {
			Timestamp t1 = o1.getGpsUtcTimestamp(), t2 = o2.getGpsUtcTimestamp();
			int cmp = Long.compare(t1.getSeconds(), t2.getSeconds());
			if(cmp == 0) cmp = Integer.compare(t1.getNanos(), t2.getNanos());
			return cmp;
		}
	};
	
	private static final NumberFormat GPS_HEIGHT_FORMAT = DecimalFormat.getInstance();
	static {
		GPS_HEIGHT_FORMAT.setMaximumFractionDigits(2);
	}
	
	static JSONArray createGpsTable(List<Gps.GPSList.GPSEntry> timeList) {
		JSONArray row = new JSONArray()
				.put(new JSONObject().put("value", 0).put("unit", ""))
				.put(new JSONObject().put("value", 0).put("unit", ""))
				.put(new JSONObject().put("value", 0).put("unit", "m"))
				.put(new JSONObject().put("value", 0).put("unit", ""))
				.put(new JSONObject().put("value", 0).put("unit", ""))
				.put(new JSONObject().put("value", 0).put("unit", ""));

		JSONArray out = new JSONArray();
		for (Gps.GPSList.GPSEntry gps : timeList) {
			row.getJSONObject(0).put("value", gps.hasGpsUtcTimestamp() ? 
					isoUTC(ARMsgHeader.timestampToInstant(gps.getGpsUtcTimestamp())) : "");
			row.getJSONObject(1).put("value", new JSONObject()
					.put("lat", gps.getPositionNorth())
					.put("long", gps.getPositionEast()));
			row.getJSONObject(2).put("value", GPS_HEIGHT_FORMAT.format(gps.getPositionUp() / 1000.));
			row.getJSONObject(3).put("value", gps.getPositionStatus().name());
			row.getJSONObject(4).put("value", gps.getNumberOfSatellites());
			row.getJSONObject(5).put("value", gps.getFieldStatus().name());
			out.put(new JSONArray(row.toString()));
		}
		return out;
	}
	
	static JSONObject createGpsGeoJSON(List<Gps.GPSList.GPSEntry> timeList) {
		JSONArray lscoordinates = new JSONArray();
		JSONArray features = new JSONArray();
		features.put(new JSONObject()
				.put("type", "Feature")
				.put("geometry", new JSONObject()
						.put("type", "LineString")
						.put("coordinates", lscoordinates)));

		for (Gps.GPSList.GPSEntry gps : timeList) {
			JSONArray coordinates = new JSONArray()
					.put(gps.getPositionEast())
					.put(gps.getPositionNorth())
					.put(gps.getPositionUp());
			if(gps.hasGpsUtcTimestamp())
				coordinates.put(TimeUnit.SECONDS.toMillis(gps.getGpsUtcTimestamp().getSeconds()));
			lscoordinates.put(coordinates);

			features.put(new JSONObject()
					.put("type", "Feature")
					.put("geometry", new JSONObject()
							.put("type", "Point")
							.put("coordinates", coordinates)));
		}

		return new JSONObject()
				.put("type", "FeatureCollection")
				.put("features", features);
	}
	
	private static JSONObject toJson(int total, List<Gps.GPSList.GPSEntry> timeList) {
		return new JSONObject()
				.put("total", total)
				.put("caption", new JSONObject()
						.put("device", new JSONArray().put(new JSONObject().put("span", 5).put("label", "")))
						.put("deviceelement", new JSONArray().put(new JSONObject().put("span", 5).put("label", "")))
						.put("logvalues", new JSONArray()
								.put(new JSONObject().put("label", "Time"))
								.put(new JSONObject().put("label", "Position"))
								.put(new JSONObject().put("label", "Height"))
								.put(new JSONObject().put("label", "GPS State"))
								.put(new JSONObject().put("label", "Satellites"))
								.put(new JSONObject().put("label", "Working State"))))
				.put("telemetry", createGpsTable(timeList))
				.put("geoJSON", createGpsGeoJSON(timeList));
	}
	
	private JSONObject gpsTelemetry(User user, File file, int offset, int limit) 
			throws SDSDException, FileNotFoundException, InvalidProtocolBufferException {
		Gps.GPSList list = Gps.GPSList.parseFrom(application.file.downloadFile(user, file));

		if(offset < 0) offset = Math.max(list.getGpsEntriesCount() + offset, 0);
		else if(offset > list.getGpsEntriesCount()) offset = list.getGpsEntriesCount();
		int end = limit < 0 ? list.getGpsEntriesCount() : Math.min(offset + limit, list.getGpsEntriesCount());
		List<Gps.GPSList.GPSEntry> timeList = list.getGpsEntriesList().stream()
				.sorted(gpsComparator)
				.skip(offset)
				.limit(end)
				.collect(Collectors.toList());

		return toJson(list.getGpsEntriesCount(), timeList).put("offset", offset);
	}
	
	private JSONObject setGpsUpdateListener(User user, File file, WebsocketConnection conn, String fileid) 
			throws SDSDException, FileNotFoundException, InvalidProtocolBufferException {
		application.file.fileAppended.setListener(file, conn.listener("telemetry", "update", null, fileid, GPS_UPDATE_LISTENER));
		return success(true);
	}
	
	private static final WebsocketConnection.CheckedFunction<byte[], JSONObject> GPS_UPDATE_LISTENER = 
			new WebsocketConnection.CheckedFunction<byte[], JSONObject>() {
		
		@Override
		public JSONObject apply(byte[] content) throws Throwable {
			Gps.GPSList list = Gps.GPSList.parseFrom(content);

			List<Gps.GPSList.GPSEntry> timeList = list.getGpsEntriesList().stream()
					.sorted(gpsComparator)
					.collect(Collectors.toList());

			return toJson(list.getGpsEntriesCount(), timeList);
		}
	};

	private JSONObject updateGpsTelemetry(User user, File file, int offset, int limit) 
			throws SDSDException, ARException, IOException, FileNotFoundException, InvalidProtocolBufferException {
		Instant date = file.getCreated();
		if(date == null) date = Instant.now();
		CompletableFuture<Integer> arupdate = application.agrirouter.readMessageHeaders(user, 
				date.truncatedTo(ChronoUnit.DAYS), 
				date.plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS))
				.thenApply(headers -> headers.stream()
						.filter(h -> h.getType() == ARMessageType.GPS && h.getSender().equals(file.getSource()) && !h.getIds().isEmpty() && h.isComplete())
						.collect(Collectors.toList()))
				.thenApply(headers -> receiveFiles(user, headers));

		try {
			int messagesReceived = arupdate.join();
			System.out.println("updateTelemetry: received " + messagesReceived + " new timelog messages");
		} catch (Throwable e) {
			if(e instanceof CompletionException) e = e.getCause();
			createError(user, e);
		}

		Gps.GPSList list = Gps.GPSList.parseFrom(application.file.downloadFile(user, file));

		offset = Math.min(offset, list.getGpsEntriesCount());
		int end = limit < 0 ? list.getGpsEntriesCount() : Math.min(offset + limit, list.getGpsEntriesCount());
		List<Gps.GPSList.GPSEntry> timeList = list.getGpsEntriesList().stream()
				.sorted(gpsComparator)
				.skip(offset)
				.limit(end)
				.collect(Collectors.toList());

		return toJson(list.getGpsEntriesCount(), timeList).put("offset", offset);
	}

}
