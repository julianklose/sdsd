package de.sdsd.projekt.agrirouter;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Enum of all supported agrirouter message types with a mapping to mimetypes.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public enum ARMessageType implements Serializable {
	AMBIGUOUS("Ambiguous", "", ""), 
	OTHER("Other", "dke:other", ""), 
	LMIS("LMIS Other", "lmis:other", ""),
	SHAPE("Shape Zip Folder", "shp:shape:zip", "application/zip"), 
	TASKDATA("ISOXML Taskdata", "iso:11783:-10:taskdata:zip", "application/zip"), 
	DEVICE_DESCRIPTION("EFDI Device Description", "iso:11783:-10:device_description:protobuf", ""), 
	TIME_LOG("EFDI TimeLog", "iso:11783:-10:time_log:protobuf", ""), 
	IMG_BMP("Bitmap Image", "img:bmp", "image/bmp"), 
	IMG_JPEG("JPEG Image", "img:jpeg", "image/jpeg"), 
	IMG_PNG("PNG Image", "img:png", "image/png"), 
	VID_AVI("AVI Video", "vid:avi", "video/x-msvideo"), 
	VID_MP4("MP4 Video", "vid:mp4", "video/mp4"), 
	VID_WMV("WMV Video", "vid:wmv", "video/x-ms-wmv"), 
	DOC_PDF("PDF Document", "doc:pdf", "application/pdf"),
	GPS("GPS Info", "gps:info", "");
	
	private static Map<String, ARMessageType> VALUES;
	static {
		HashMap<String, ARMessageType> map = new HashMap<>();
		for (ARMessageType t : values()) {
			if(t.technicalMessageType != null && !t.technicalMessageType.isEmpty())
				map.put(t.technicalMessageType, t);
		}
		VALUES = Collections.unmodifiableMap(map);
	}
	
	/**
	 * Get the message type enum value for the given technical message type from the agrirouter.
	 * If the technical message type is unknown, {@link OTHER} is returned.
	 * 
	 * @param technicalMessageType agrirouter string representation of message type or {@link OTHER}
	 * @return corresponding message type enum value
	 */
	public static ARMessageType from(String technicalMessageType) {
		ARMessageType type = VALUES.get(technicalMessageType);
		if(type == null) {
			System.err.println("Unknown Message Type: " + technicalMessageType);
			return OTHER;
		}
		return type;
	}
	
	/**
	 * Get the message type enum value for the given technical message type from the agrirouter.
	 * 
	 * @param technicalMessageType agrirouter string representation of message type
	 * @return corresponding message type enum value
	 * @throws ARException if the given technical message type is unknown
	 */
	public static ARMessageType fromOrThrow(String technicalMessageType) throws ARException {
		ARMessageType type = VALUES.get(technicalMessageType);
		if(type == null)
			throw new ARException("Unknown Message Type: " + technicalMessageType);
		return type;
	}
	
	private static ARMessageType[] EMPTY = new ARMessageType[0];
	private static Map<String, ARMessageType[]> MIMETYPES;
	static {
		HashMap<String, ARMessageType[]> map = new HashMap<>();
		for (ARMessageType t : values()) {
			if(t.mimetype != null && !t.mimetype.isEmpty()) {
				ARMessageType[] types = map.get(t.mimetype);
				if(types == null) 
					map.put(t.mimetype, new ARMessageType[] {t});
				else {
					types = Arrays.copyOf(types, types.length + 1);
					types[types.length - 1] = t;
					map.put(t.mimetype, types);
				}
			}
		}
		MIMETYPES = Collections.unmodifiableMap(map);
	}
	
	/**
	 * Get all message type enum values for the given mime type.
	 * For not mapped mimetypes an empty array is returned.
	 * 
	 * @param mimetype mapped mimetype for a message type
	 * @return array of corresponding message type enum values
	 */
	public static ARMessageType[] fromMimetypeAll(String mimetype) {
		ARMessageType[] types = MIMETYPES.get(mimetype);
		return types != null ? types : EMPTY;
	}
	
	/**
	 * Get the first message type enum value for the given mime type.
	 * For not mapped mimetypes {@link #OTHER} is returned.
	 * If there are more than one corresponding message type {@link #AMBIGUOUS} is returned.
	 * 
	 * @param mimetype mapped mimetype for a message type
	 * @return first corresponding message type enum value
	 */
	public static ARMessageType fromMimetype(String mimetype) {
		ARMessageType[] types = MIMETYPES.get(mimetype);
		if(types == null) return ARMessageType.OTHER;
		else if(types.length == 1) return types[0];
		else return ARMessageType.AMBIGUOUS;
	}
	
	private final String mimetype;
	private final String technicalMessageType;
	private final String name;
	
	/**
	 * Hidden constructor. Is only called once for every enum value.
	 * 
	 * @param name display name, returned by {@link #toString()}
	 * @param technicalMessageType agrirouter message type
	 * @param mime mapped mimetype
	 */
	private ARMessageType(String name, String technicalMessageType, String mime) {
		this.name = name;
		this.technicalMessageType = technicalMessageType;
		this.mimetype = mime;
	}
	
	/**
	 * Returns the corresponding mimetype to this message type.
	 * @return mapped mimetype or null
	 */
	public String mimetype() {
		return mimetype;
	}
	
	/**
	 * Returns the agrirouter string representation of this message type.
	 * @return agrirouter message type
	 */
	public String technicalMessageType() {
		return technicalMessageType;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	/**
	 * Universal agrirouter direction enum.
	 * 
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static enum ARDirection implements Serializable {
		SEND(0), RECEIVE(1), SEND_RECEIVE(2);
		
		public static ARDirection from(int number) {
			switch (number) {
			  case 0: return SEND;
			  case 1: return RECEIVE;
			  case 2: return SEND_RECEIVE;
			  default: return null;
			}
		}
		
		private int number;
		private ARDirection(int number) {
			this.number = number;
		}
		
		/**
		 * Returns the number of the direction, that is the same in all protobuf direction enums.
		 * @return SEND: 0, RECEIVE: 1, SEND_RECEIVE: 2
		 */
		public int number() {
			return number;
		}
	}
}
