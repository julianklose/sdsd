package de.sdsd.projekt.parser.isoxml;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ResourceFactory;
import org.json.JSONObject;

import de.sdsd.projekt.parser.isoxml.RefAttr.IDRef;
import de.sdsd.projekt.parser.isoxml.RefAttr.OIDRef;

/**
 * Represents an isoxml attribute.
 * 
 * @param <T> data type of the attribute value
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public interface Attribute<T> {
	
	public static enum AttrType {
		UNKNOWN("unknown"),
		STRING("string"),
		ID("id"),
		ID_REF("idref"),
		OID("oid"),
		OID_REF("oidref"),
		HEX("hex"),
		DDI("ddi"),
		INT("long"),
		ULONG("ulong"),
		USHORT("ushort"),
		BYTE("byte"),
		DOUBLE("double"),
		DECIMAL("decimal"),
		DATETIME("datetime"),
		ENUM("enum");
		
		public final String type;
		private AttrType(String type) {
			this.type = type;
		}
		
		private static Map<String, AttrType> VALUES;
		static {
			HashMap<String, AttrType> map = new HashMap<>();
			for (AttrType t : values()) {
				map.put(t.type, t);
			}
			VALUES = Collections.unmodifiableMap(map);
		}
		
		public static AttrType from(String type) {
			return VALUES.getOrDefault(type, AttrType.STRING);
		}
	}
	
	public static Attribute<?> parseAttribute(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
		switch(AttrType.from(format.getString("type"))) {
		case STRING:
			return new StringAttr(node, key, format, stringvalue);
		case ID:
			return new IDAttr(node, key, format, stringvalue);
		case ID_REF:
			return new IDRef(node, key, format, stringvalue);
		case OID:
			return new OIDAttr(node, key, format, stringvalue);
		case OID_REF:
			return new OIDRef(node, key, format, stringvalue);
		case HEX:
			return new HEXAttr(node, key, format, stringvalue);
		case DDI:
			return new DDIAttr(node, key, format, stringvalue);
		case INT:
			return new IntAttr(node, key, format, stringvalue);
		case ULONG:
			return new ULongAttr(node, key, format, stringvalue);
		case USHORT:
			return new UShortAttr(node, key, format, stringvalue);
		case BYTE:
			return new ByteAttr(node, key, format, stringvalue);
		case DOUBLE:
			return new DoubleAttr(node, key, format, stringvalue);
		case DECIMAL:
			return new FloatAttr(node, key, format, stringvalue);
		case DATETIME:
			return new DatetimeAttr(node, key, format, stringvalue);
		case ENUM:
			return new EnumAttr(node, key, format, stringvalue);
		default:
			return new UnknownAttr(node, key, format, stringvalue);
		}
	}
	
	public static UnknownAttr createUnknownAttribute(IsoXmlElement node, String name, @Nullable String stringvalue) {
		JSONObject format = new JSONObject()
				.put("name", name)
				.put("type", AttrType.UNKNOWN.type)
				.put("required", false);
		return new UnknownAttr(node, name, format, stringvalue);
	}
	
	String getKey();
	String getName();
	AttrType getType();
	String getStringValue();
	boolean hasValue();
	boolean hasError();
	String getError();
	T getValue();
	Literal toLiteral();
	String toString();
	int hashCode();
	boolean equals(Object obj);
	
	abstract class AbstractAttribute<T> implements Attribute<T> {
		protected static final String ERROR_MISSING = "Missing attribute", 
				ERROR_INVALID = "Invalid attribute type", 
				ERROR_RANGE = "Attribute value out of range",
				ERROR_TOO_LONG = "Attribute value is too long",
				ERROR_FORMAT = "Value format is invalid",
				ERROR_LENGTH = "Attribute value has not the right length";
		
		protected final IsoXmlElement node;
		protected final String key, name;
		protected final AttrType type;
		@CheckForNull
		protected final String stringvalue;
		@CheckForNull
		private String error = null;
	
		public AbstractAttribute(IsoXmlElement node, AttrType type, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			this.node = node;
			this.key = key;
			this.name = format != null ? format.getString("name") : key;
			this.type = type;
			this.stringvalue = stringvalue;
			if(format != null) {
				if(!type.type.equals(format.getString("type")))
					throw new IllegalArgumentException("Type is not correct");
				
				if(stringvalue == null && format.getBoolean("required"))
					error(ERROR_MISSING);
			}
		}
		
		public final String getKey() {
			return key;
		}
		
		public final String getName() {
			return name;
		}
		
		public final AttrType getType() {
			return type;
		}
		
		public final String getStringValue() {
			return stringvalue != null ? stringvalue : "";
		}
		
		public final boolean hasValue() {
			return stringvalue != null;
		}
		
		protected final String createError(String error) {
			return String.format("%s(%s): %s", name, type.type, error);
		}
		
		protected final void error(String error) {
			this.error = createError(error);
		}
		
		public final boolean hasError() {
			return error != null;
		}
		
		public final String getError() {
			return error != null ? error : "";
		}

		@Override
		public String toString() {
			return stringvalue != null ? stringvalue : "";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((stringvalue == null) ? 0 : stringvalue.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
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
			Attribute<?> other = (Attribute<?>) obj;
			return getValue().equals(other.getValue());
		}
	}

	public static class UnknownAttr extends AbstractAttribute<String> {
		
		public UnknownAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.UNKNOWN, key, format, stringvalue);
			error("Unknown attribute");
		}

		@Override
		public String getValue() {
			return stringvalue != null ? stringvalue : "";
		}
		
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createStringLiteral(getValue());
		}
	}
	
	public static class StringAttr extends AbstractAttribute<String> {

		public StringAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.STRING, key, format, stringvalue);
			if(stringvalue != null) {
				int length = format.optInt("length", -1);
				if(length >= 0 && stringvalue.length() != length)
					error(ERROR_LENGTH);
				if(stringvalue.length() > format.optInt("maxlength", Integer.MAX_VALUE))
					error(ERROR_TOO_LONG);
			}
		}

		@Override
		public String getValue() {
			return stringvalue != null ? stringvalue : "";
		}
		
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createStringLiteral(getValue());
		}
	}
	
	public static class IDAttr extends AbstractAttribute<String> {
		protected static final Pattern ID_REGEX = Pattern.compile("[A-Z]{3}-?\\d{1,10}");
		
		protected IDAttr(IsoXmlElement node, AttrType type, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, type, key, format, stringvalue);
			if(stringvalue != null) {
				if(!ID_REGEX.matcher(stringvalue).matches())
					error(ERROR_FORMAT);
			}
		}
		
		public IDAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			this(node, AttrType.ID, key, format, stringvalue);
		}

		@Override
		public String getValue() {
			return stringvalue != null ? stringvalue : "";
		}
		
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createStringLiteral(getValue());
		}
	}
	
	public static class OIDAttr extends AbstractAttribute<Integer> {
		protected int value = 0;
		
		protected OIDAttr(IsoXmlElement node, AttrType type, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, type, key, format, stringvalue);
			if(stringvalue != null) {
				try {
					value = Integer.parseInt(stringvalue);
					if(value < format.optInt("min", 1) || value > format.optInt("max", 65534))
						error(ERROR_RANGE);
				} catch (NumberFormatException e) {
					error(ERROR_INVALID);
				}
			}
		}
		
		public OIDAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			this(node, AttrType.OID, key, format, stringvalue);
		}

		@Override
		public Integer getValue() {
			return value;
		}
		
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createTypedLiteral(value);
		}
	}
	
	public static class HEXAttr extends AbstractAttribute<byte[]> {
		protected byte[] value = new byte[0];
		
		public HEXAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.HEX, key, format, stringvalue);
			if(stringvalue != null) {
				try {
					value = Hex.decodeHex(stringvalue);
					if(value.length > format.optInt("maxbytes", Integer.MAX_VALUE))
						error(ERROR_TOO_LONG);
					
				} catch (DecoderException e) {
					error(ERROR_INVALID);
				}
			}
		}

		@Override
		public byte[] getValue() {
			return value;
		}
		
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createTypedLiteral(Hex.encodeHexString(value, false), XSDDatatype.XSDhexBinary);
		}
	}
	
	public static class DDIAttr extends AbstractAttribute<Integer> {
		protected int value = 0;
		
		public DDIAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.DDI, key, format, stringvalue);
			if(stringvalue != null) {
				try {
					value = Integer.parseInt(stringvalue, 16);
					if(value < 0 || value > 65534)
						error(ERROR_RANGE);
				} catch (NumberFormatException e) {
					error(ERROR_INVALID);
				}
			}
		}

		@Override
		public Integer getValue() {
			return value;
		}
		
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createTypedLiteral(value);
		}
	}
	
	public static class IntAttr extends AbstractAttribute<Integer> {
		protected int value = 0;
		
		public IntAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.INT, key, format, stringvalue);
			if(stringvalue != null) {
				try {
					value = Integer.parseInt(stringvalue);
					if(value < format.optInt("min", Integer.MIN_VALUE)
							|| value > format.optInt("max", Integer.MAX_VALUE))
						error(ERROR_RANGE);
				} catch (NumberFormatException e) {
					error(ERROR_INVALID);
				}
			}
		}

		@Override
		public Integer getValue() {
			return value;
		}
		
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createTypedLiteral(value);
		}
	}
	
	public static class ULongAttr extends AbstractAttribute<Long> {
		protected long value = 0;
		
		public ULongAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.ULONG, key, format, stringvalue);
			if(stringvalue != null) {
				try {
					value = Long.parseLong(stringvalue);
					if(value < format.optLong("min", 0)
							|| value > format.optLong("max", 4294967294L))
						error(ERROR_RANGE);
				} catch (NumberFormatException e) {
					error(ERROR_INVALID);
				}
			}
		}

		@Override
		public Long getValue() {
			return value;
		}
		
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createTypedLiteral(value);
		}
	}
	
	public static class UShortAttr extends AbstractAttribute<Integer> {
		protected int value = 0;
		
		public UShortAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.USHORT, key, format, stringvalue);
			if(stringvalue != null) {
				try {
					value = Integer.parseInt(stringvalue);
					if(value < format.optInt("min", 0)
							|| value > format.optInt("max", 65535))
						error(ERROR_RANGE);
				} catch (NumberFormatException e) {
					error(ERROR_INVALID);
				}
			}
		}

		@Override
		public Integer getValue() {
			return value;
		}
		
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createTypedLiteral(value);
		}
	}
	
	public static class ByteAttr extends AbstractAttribute<Integer> {
		protected int value = 0;
		
		public ByteAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.BYTE, key, format, stringvalue);
			if(stringvalue != null) {
				try {
					value = Integer.parseInt(stringvalue);
					if(value < format.optInt("min", 0)
							|| value > format.optInt("max", 254))
						error(ERROR_RANGE);
				} catch (NumberFormatException e) {
					error(ERROR_INVALID);
				}
			}
		}

		@Override
		public Integer getValue() {
			return value;
		}
		
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createTypedLiteral(value);
		}
	}
	
	public static class DoubleAttr extends AbstractAttribute<Double> {
		protected double value = 0;
		
		public DoubleAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.DOUBLE, key, format, stringvalue);
			if(stringvalue != null) {
				try {
					value = Double.parseDouble(stringvalue);
					if(value < format.optDouble("min", Double.MIN_VALUE)
							|| value > format.optDouble("max", Double.MAX_VALUE))
						error(ERROR_RANGE);
				} catch (NumberFormatException e) {
					error(ERROR_INVALID);
				}
			}
		}

		@Override
		public Double getValue() {
			return value;
		}
		
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createTypedLiteral(value);
		}
	}
	
	public static class FloatAttr extends AbstractAttribute<Float> {
		protected float value = 0;
		
		public FloatAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.DECIMAL, key, format, stringvalue);
			if(stringvalue != null) {
				try {
					value = Float.parseFloat(stringvalue);
					if(value < format.optDouble("min", Float.MIN_VALUE)
							|| value > format.optDouble("max", Float.MAX_VALUE))
						error(ERROR_RANGE);
				} catch (NumberFormatException e) {
					error(ERROR_INVALID);
				}
			}
		}

		@Override
		public Float getValue() {
			return value;
		}
		
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createTypedLiteral(value);
		}
	}
	
	public static class DatetimeAttr extends AbstractAttribute<Instant> {
		protected Instant value = Instant.EPOCH;
		
		public DatetimeAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.DATETIME, key, format, stringvalue);
			if(stringvalue != null) {
				try {
					try {
						value = OffsetDateTime.parse(stringvalue).toInstant();
					} catch (DateTimeParseException e) {
						value = LocalDateTime.parse(stringvalue).atZone(ZoneOffset.systemDefault()).toInstant();
					}
				} catch (DateTimeParseException e) {
					error(ERROR_INVALID);
				}
			}
		}

		@Override
		public Instant getValue() {
			return value;
		}
		
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createTypedLiteral(GregorianCalendar.from(ZonedDateTime.ofInstant(value, ZoneOffset.UTC)));
		}
	}
	
	public static class EnumAttr extends AbstractAttribute<String> {
		protected byte number = 0;
		protected String value = "";
		
		public EnumAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.ENUM, key, format, stringvalue);
			if(stringvalue != null) {
				try {
					number = Byte.parseByte(stringvalue);
					if(number < 0)
						error(ERROR_RANGE);
					else {
						value = format.getJSONArray("values").optString(number);
						if(value.isEmpty())
							error("Unknown token " + number);
					}
				} catch (NumberFormatException e) {
					error(ERROR_INVALID);
				}
			}
		}
		
		public byte number() {
			return number;
		}

		@Override
		public String getValue() {
			return value;
		}
		
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createStringLiteral(value);
		}
	}

}
