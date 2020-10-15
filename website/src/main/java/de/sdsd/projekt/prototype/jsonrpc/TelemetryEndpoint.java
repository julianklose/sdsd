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

	/*
	 * Instantiates a new telemetry endpoint.
	 *
	 * @param application the application
	 */
	public TelemetryEndpoint(ApplicationLogic application) {
		super(application);
	}
	
	/**
	 * Gets the requested telemetry data
	 *
	 * @param req the current request
	 * @param fileid id of the file
	 * @param offset default is 0
	 * @param limit sets how many entries will be displayed. The default value is 50 entries.
	 * @return the JSON object telemetry data
	 * @throws JsonRpcException the json rpc exception
	 */
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
	
	/**
	 * Sets a listener that triggers when the watched file changes
	 *
	 * @param req the current request
	 * @param conn websocket connnection
	 * @param fileid the files id
	 * @return the JSON object changed telemetry data
	 * @throws JsonRpcException the json rpc exception
	 */
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

	/**
	 * Update the requested telemetry data.
	 *
	 * @param req the current request
	 * @param fileid the files id
	 * @param offset default is 0
	 * @param limit sets how many entries will be displayed. The default value is 50 entries.
	 * @return the JSON object updated telemetry data
	 * @throws JsonRpcException the json rpc exception
	 */
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
		
		/** The map. */
		private final HashMap<Key, TimelogValueInfo> map = new HashMap<>();
		
		/** The first. */
		private TimelogValueInfo first = null;

		/**
		 * Put - adds the new TimelogValueInfo to the map and sets the correct before and after attribute. Replaces any old entry with the same ddi and device element id.
		 *
		 * @param dvc the Device
		 * @param det the Device Element
		 * @param dpd the Device ProcessData 
		 * @param dvp the Device Value Presentation
		 */
		public void put(GrpcEfdi.Device dvc, GrpcEfdi.DeviceElement det, GrpcEfdi.DeviceProcessData dpd, GrpcEfdi.DeviceValuePresentation dvp) {
			put(new TimelogValueInfo(dvc, det, dpd, dvp));
		}

		/**
		 * Put - adds the new TimelogValueInfo to the map and sets the correct before and after attribute. Replaces any old entry with the same ddi and device element id.
		 *
		 * @param dvc the Device
		 * @param det the Device Element
		 * @param dpt the Device DeviceProperty 
		 * @param dvp the Device Value Presentation
		 */
		public void put(GrpcEfdi.Device dvc, GrpcEfdi.DeviceElement det, GrpcEfdi.DeviceProperty dpt, GrpcEfdi.DeviceValuePresentation dvp) {
			put(new TimelogValueInfo(dvc, det, dpt, dvp));
		}

		/**
		 * Put - adds the new TimelogValueInfo to the map and sets the correct before and after attribute. Replaces any old entry with the same ddi and device element id.
		 *
		 * @param tvi the Timelog Value Info
		 */
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

		/**
		 * Gets the timelog value info by device element id and ddi
		 *
		 * @param dlv the data log value
		 * @return the timelog value info
		 */
		public TimelogValueInfo get(GrpcEfdi.DataLogValue dlv) {
			return map.get(new Key(dlv.getDeviceElementIdRef(), dlv.getProcessDataDdi()));
		}

		/**
		 * The Class Key.
		 */
		private static final class Key {
			
			/** The det. */
			public final GrpcEfdi.UID det;
			
			/** The ddi. */
			public final int ddi;

			/**
			 * Instantiates a new key.
			 *
			 * @param det the det
			 * @param ddi the ddi
			 */
			Key(GrpcEfdi.UID det, int ddi) {
				this.det = det;
				this.ddi = ddi;
			}

			/**
			 * Hash code.
			 *
			 * @return the int
			 */
			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + ddi;
				result = prime * result + Long.hashCode(det.getNumber());
				return result;
			}

			/**
			 * Equals.
			 *
			 * @param obj the obj
			 * @return true, if successful
			 */
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

		/**
		 * Iterator.
		 *
		 * @return the iterator
		 */
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

		/** The dvc. */
		public final GrpcEfdi.Device dvc;
		
		/** The det. */
		public final GrpcEfdi.DeviceElement det;
		
		/** The dpd. */
		@CheckForNull
		public final GrpcEfdi.DeviceProcessData dpd;
		
		/** The dpt. */
		@CheckForNull
		public final GrpcEfdi.DeviceProperty dpt;
		
		/** The dvp. */
		@CheckForNull
		public final GrpcEfdi.DeviceValuePresentation dvp;
		
		/** The format. */
		private final NumberFormat format = DecimalFormat.getInstance();
		
		/** The index. */
		public int index = -1;
		
		/** The next. */
		public TimelogValueInfo next = null;
		
		/** The start value. */
		public long startValue;

		/**
		 * Instantiates a new timelog value info.
		 *
		 * @param dvc the dvc
		 * @param det the det
		 * @param dpd the dpd
		 * @param dvp the dvp
		 */
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

		/**
		 * Instantiates a new timelog value info.
		 *
		 * @param dvc the dvc
		 * @param det the det
		 * @param dpt the dpt
		 * @param dvp the dvp
		 */
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

		/**
		 * Gets the ddi.
		 *
		 * @return the ddi
		 */
		public int getDDI() {
			return dpd != null ? dpd.getDeviceProcessDataDdi() : dpt != null ? dpt.getDevicePropertyDdi() : 0;
		}

		/**
		 * Gets the designator.
		 *
		 * @return the designator
		 */
		public String getDesignator() {
			String designator = dpd != null ? dpd.getDeviceProcessDataDesignator() : dpt != null ? dpt.getDevicePropertyDesignator() : "";
			return designator.isEmpty() ? Integer.toString(getDDI()) : designator;
		}
		
		/**
		 * Format value.
		 *
		 * @param value the value
		 * @return the string
		 */
		public String formatValue(long value) {
			if(dvp != null) {
				double val = (value + dvp.getOffset()) * dvp.getScale();
				return format.format(val);
			}
			return Long.toString(value);
		}
		
		/**
		 * Gets the unit.
		 *
		 * @return the unit
		 */
		public String getUnit() {
			return dvp != null ? dvp.getUnitDesignator() : "";
		}

		/**
		 * Compare to.
		 *
		 * @param o the o
		 * @return the int
		 */
		@Override
		public int compareTo(TimelogValueInfo o) {
			int cmp = Long.compare(dvc.getDeviceId().getNumber(), o.dvc.getDeviceId().getNumber());
			if(cmp == 0) cmp = Long.compare(det.getDeviceElementId().getNumber(), o.det.getDeviceElementId().getNumber());
			if(cmp == 0) cmp = Integer.compare(getDDI(), o.getDDI());
			return cmp;
		}
	}

	/**
	 * Position.
	 *
	 * @param pos the pos
	 * @return the JSON object
	 */
	public static JSONObject position(GrpcEfdi.Position pos) {
		return new JSONObject()
				.put("lat", pos.getPositionNorth())
				.put("long", pos.getPositionEast());
	}

	/**
	 * Geo position.
	 *
	 * @param pos the pos
	 * @param time the time
	 * @return the JSON array
	 */
	public static JSONArray geoPosition(GrpcEfdi.Position pos, Timestamp time) {
		return new JSONArray()
				.put(pos.getPositionEast())
				.put(pos.getPositionNorth())
				.put(pos.getPositionUp())
				.put(TimeUnit.SECONDS.toMillis(time.getSeconds()));
	}

	/**
	 * Builds the info map.
	 *
	 * @param deviceDescription the device description
	 * @return the dlv info
	 */
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

	/**
	 * Creates the caption.
	 *
	 * @param dlvInfo the dlv info
	 * @return the JSON object
	 */
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

	/**
	 * Creates the efdi table.
	 *
	 * @param timeList the time list
	 * @param dlvInfo the dlv info
	 * @param startingTimeList the starting time list
	 * @return the JSON array
	 */
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

	/**
	 * Creates the efdi geo JSON.
	 *
	 * @param timeList the time list
	 * @return the JSON object
	 */
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

	/** The Constant timeComparator. */
	public static final Comparator<GrpcEfdi.Time> timeComparator = new Comparator<GrpcEfdi.Time>() {
		@Override
		public int compare(GrpcEfdi.Time o1, GrpcEfdi.Time o2) {
			Timestamp t1 = o1.getStart(), t2 = o2.getStart();
			int cmp = Long.compare(t1.getSeconds(), t2.getSeconds());
			if(cmp == 0) cmp = Integer.compare(t1.getNanos(), t2.getNanos());
			return cmp;
		}
	};
	
	/**
	 * To json.
	 *
	 * @param total the total
	 * @param dlvInfo the dlv info
	 * @param timeList the time list
	 * @param startingTimeList the starting time list
	 * @return the JSON object
	 */
	private JSONObject toJson(int total, DlvInfo dlvInfo, List<GrpcEfdi.Time> timeList, List<GrpcEfdi.Time> startingTimeList) {
		return new JSONObject()
				.put("total", total)
				.put("caption", createCaption(dlvInfo))
				.put("telemetry", createEfdiTable(timeList, dlvInfo, startingTimeList))
				.put("geoJSON", createEfdiGeoJSON(timeList));
	}

	/**
	 * Efdi telemetry.
	 *
	 * @param user the user
	 * @param file the file
	 * @param offset the offset
	 * @param limit the limit
	 * @return the JSON object
	 * @throws SDSDException the SDSD exception
	 * @throws FileNotFoundException the file not found exception
	 * @throws InvalidProtocolBufferException the invalid protocol buffer exception
	 */
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
	
	/**
	 * Sets the efdi update listener.
	 *
	 * @param user the user
	 * @param file the file
	 * @param conn the conn
	 * @param fileid the fileid
	 * @return the JSON object
	 * @throws SDSDException the SDSD exception
	 * @throws FileNotFoundException the file not found exception
	 * @throws InvalidProtocolBufferException the invalid protocol buffer exception
	 */
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
		
		/** The dlv info. */
		private final DlvInfo dlvInfo;
		
		/** The starting time list. */
		private final List<GrpcEfdi.Time> startingTimeList;
		
		/**
		 * Instantiates a new telemetry update listener.
		 *
		 * @param dlvInfo the dlv info
		 * @param startingTimeList the starting time list
		 */
		public TelemetryUpdateListener(DlvInfo dlvInfo, List<GrpcEfdi.Time> startingTimeList) {
			this.dlvInfo = dlvInfo;
			this.startingTimeList = startingTimeList;
		}

		/**
		 * Apply.
		 *
		 * @param content the content
		 * @return the JSON object
		 * @throws InvalidProtocolBufferException the invalid protocol buffer exception
		 */
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

	/**
	 * Update efdi telemetry.
	 *
	 * @param user the user
	 * @param file the file
	 * @param offset the offset
	 * @param limit the limit
	 * @return the JSON object
	 * @throws SDSDException the SDSD exception
	 * @throws ARException the AR exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws FileNotFoundException the file not found exception
	 * @throws InvalidProtocolBufferException the invalid protocol buffer exception
	 */
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

	/**
	 * Receive files.
	 *
	 * @param user the user
	 * @param headers the headers
	 * @return the int
	 */
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
	
	/** The Constant gpsComparator. */
	public static final Comparator<Gps.GPSList.GPSEntry> gpsComparator = new Comparator<Gps.GPSList.GPSEntry>() {
		@Override
		public int compare(Gps.GPSList.GPSEntry o1, Gps.GPSList.GPSEntry o2) {
			Timestamp t1 = o1.getGpsUtcTimestamp(), t2 = o2.getGpsUtcTimestamp();
			int cmp = Long.compare(t1.getSeconds(), t2.getSeconds());
			if(cmp == 0) cmp = Integer.compare(t1.getNanos(), t2.getNanos());
			return cmp;
		}
	};
	
	/** The Constant GPS_HEIGHT_FORMAT. */
	private static final NumberFormat GPS_HEIGHT_FORMAT = DecimalFormat.getInstance();
	static {
		GPS_HEIGHT_FORMAT.setMaximumFractionDigits(2);
	}
	
	/**
	 * Creates the gps table.
	 *
	 * @param timeList the time list
	 * @return the JSON array
	 */
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
	
	/**
	 * Creates the gps geo JSON.
	 *
	 * @param timeList the time list
	 * @return the JSON object
	 */
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
	
	/**
	 * To json.
	 *
	 * @param total the total
	 * @param timeList the time list
	 * @return the JSON object
	 */
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
	
	/**
	 * Gps telemetry.
	 *
	 * @param user the user
	 * @param file the file
	 * @param offset the offset
	 * @param limit the limit
	 * @return the JSON object
	 * @throws SDSDException the SDSD exception
	 * @throws FileNotFoundException the file not found exception
	 * @throws InvalidProtocolBufferException the invalid protocol buffer exception
	 */
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
	
	/**
	 * Sets the gps update listener.
	 *
	 * @param user the user
	 * @param file the file
	 * @param conn the conn
	 * @param fileid the fileid
	 * @return the JSON object
	 * @throws SDSDException the SDSD exception
	 * @throws FileNotFoundException the file not found exception
	 * @throws InvalidProtocolBufferException the invalid protocol buffer exception
	 */
	private JSONObject setGpsUpdateListener(User user, File file, WebsocketConnection conn, String fileid) 
			throws SDSDException, FileNotFoundException, InvalidProtocolBufferException {
		application.file.fileAppended.setListener(file, conn.listener("telemetry", "update", null, fileid, GPS_UPDATE_LISTENER));
		return success(true);
	}
	
	/** The Constant GPS_UPDATE_LISTENER. */
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

	/**
	 * Update gps telemetry.
	 *
	 * @param user the user
	 * @param file the file
	 * @param offset the offset
	 * @param limit the limit
	 * @return the JSON object
	 * @throws SDSDException the SDSD exception
	 * @throws ARException the AR exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws FileNotFoundException the file not found exception
	 * @throws InvalidProtocolBufferException the invalid protocol buffer exception
	 */
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
