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
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * @param <T> data type of the attribute value
 */
public interface Attribute<T> {

	/**
	 * The Enum AttrType.
	 */
	public static enum AttrType {

		/** The unknown. */
		UNKNOWN("unknown"),

		/** The string. */
		STRING("string"),

		/** The id. */
		ID("id"),

		/** The id ref. */
		ID_REF("idref"),

		/** The oid. */
		OID("oid"),

		/** The oid ref. */
		OID_REF("oidref"),

		/** The hex. */
		HEX("hex"),

		/** The ddi. */
		DDI("ddi"),

		/** The int. */
		INT("long"),

		/** The ulong. */
		ULONG("ulong"),

		/** The ushort. */
		USHORT("ushort"),

		/** The byte. */
		BYTE("byte"),

		/** The double. */
		DOUBLE("double"),

		/** The decimal. */
		DECIMAL("decimal"),

		/** The datetime. */
		DATETIME("datetime"),

		/** The enum. */
		ENUM("enum");

		/** The type. */
		public final String type;

		/**
		 * Instantiates a new attr type.
		 *
		 * @param type the type
		 */
		private AttrType(String type) {
			this.type = type;
		}

		/** The values. */
		private static Map<String, AttrType> VALUES;
		static {
			HashMap<String, AttrType> map = new HashMap<>();
			for (AttrType t : values()) {
				map.put(t.type, t);
			}
			VALUES = Collections.unmodifiableMap(map);
		}

		/**
		 * From.
		 *
		 * @param type the type
		 * @return the attr type
		 */
		public static AttrType from(String type) {
			return VALUES.getOrDefault(type, AttrType.STRING);
		}
	}

	/**
	 * Parses the attribute.
	 *
	 * @param node        the node
	 * @param key         the key
	 * @param format      the format
	 * @param stringvalue the stringvalue
	 * @return the attribute
	 */
	public static Attribute<?> parseAttribute(IsoXmlElement node, String key, @Nullable JSONObject format,
			@Nullable String stringvalue) {
		switch (AttrType.from(format.getString("type"))) {
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

	/**
	 * Creates the unknown attribute.
	 *
	 * @param node        the node
	 * @param name        the name
	 * @param stringvalue the stringvalue
	 * @return the unknown attr
	 */
	public static UnknownAttr createUnknownAttribute(IsoXmlElement node, String name, @Nullable String stringvalue) {
		JSONObject format = new JSONObject().put("name", name).put("type", AttrType.UNKNOWN.type).put("required",
				false);
		return new UnknownAttr(node, name, format, stringvalue);
	}

	/**
	 * Gets the key.
	 *
	 * @return the key
	 */
	String getKey();

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	AttrType getType();

	/**
	 * Gets the string value.
	 *
	 * @return the string value
	 */
	String getStringValue();

	/**
	 * Checks for value.
	 *
	 * @return true, if successful
	 */
	boolean hasValue();

	/**
	 * Checks for error.
	 *
	 * @return true, if successful
	 */
	boolean hasError();

	/**
	 * Gets the error.
	 *
	 * @return the error
	 */
	String getError();

	/**
	 * Checks for warning.
	 *
	 * @return true, if successful
	 */
	boolean hasWarning();

	/**
	 * Gets the warning.
	 *
	 * @return the warning
	 */
	String getWarning();

	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	T getValue();

	/**
	 * To literal.
	 *
	 * @return the literal
	 */
	Literal toLiteral();

	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	String toString();

	/**
	 * Hash code.
	 *
	 * @return the int
	 */
	@Override
	int hashCode();

	/**
	 * Equals.
	 *
	 * @param obj the obj
	 * @return true, if successful
	 */
	@Override
	boolean equals(Object obj);

	/**
	 * The Class AbstractAttribute.
	 *
	 * @param <T> the generic type
	 */
	abstract class AbstractAttribute<T> implements Attribute<T> {

		/** The Constant ERROR_LENGTH. */
		protected static final String ERROR_MISSING = "Missing attribute", ERROR_INVALID = "Invalid attribute type",
				ERROR_RANGE = "Attribute value out of range", ERROR_TOO_LONG = "Attribute value is too long",
				ERROR_FORMAT = "Value format is invalid", ERROR_LENGTH = "Attribute value has not the right length";

		/** The node. */
		protected final IsoXmlElement node;

		/** The name. */
		protected final String key, name;

		/** The type. */
		protected final AttrType type;

		/** The stringvalue. */
		@CheckForNull
		protected final String stringvalue;

		/** The error. */
		@CheckForNull
		private String warning = null, error = null;

		/**
		 * Instantiates a new abstract attribute.
		 *
		 * @param node        the node
		 * @param type        the type
		 * @param key         the key
		 * @param format      the format
		 * @param stringvalue the stringvalue
		 */
		public AbstractAttribute(IsoXmlElement node, AttrType type, String key, @Nullable JSONObject format,
				@Nullable String stringvalue) {
			this.node = node;
			this.key = key;
			this.name = format != null ? format.getString("name") : key;
			this.type = type;
			this.stringvalue = stringvalue;
			if (format != null) {
				if (!type.type.equals(format.getString("type")))
					throw new IllegalArgumentException("Type is not correct");

				if (stringvalue == null && format.getBoolean("required"))
					error(ERROR_MISSING);
			}
		}

		/**
		 * Gets the key.
		 *
		 * @return the key
		 */
		@Override
		public final String getKey() {
			return key;
		}

		/**
		 * Gets the name.
		 *
		 * @return the name
		 */
		@Override
		public final String getName() {
			return name;
		}

		/**
		 * Gets the type.
		 *
		 * @return the type
		 */
		@Override
		public final AttrType getType() {
			return type;
		}

		/**
		 * Gets the string value.
		 *
		 * @return the string value
		 */
		@Override
		public final String getStringValue() {
			return stringvalue != null ? stringvalue : "";
		}

		/**
		 * Checks for value.
		 *
		 * @return true, if successful
		 */
		@Override
		public final boolean hasValue() {
			return stringvalue != null;
		}

		/**
		 * Creates the error.
		 *
		 * @param error the error
		 * @return the string
		 */
		protected final String createError(String error) {
			return String.format("%s(%s): %s", name, type.type, error);
		}

		/**
		 * Error.
		 *
		 * @param error the error
		 */
		protected final void error(String error) {
			this.error = createError(error);
		}

		/**
		 * Checks for error.
		 *
		 * @return true, if successful
		 */
		@Override
		public final boolean hasError() {
			return error != null;
		}

		/**
		 * Gets the error.
		 *
		 * @return the error
		 */
		@Override
		public final String getError() {
			return error != null ? error : "";
		}

		/**
		 * Warn.
		 *
		 * @param error the error
		 */
		protected final void warn(String error) {
			this.warning = createError(error);
		}

		/**
		 * Checks for warning.
		 *
		 * @return true, if successful
		 */
		@Override
		public final boolean hasWarning() {
			return warning != null;
		}

		/**
		 * Gets the warning.
		 *
		 * @return the warning
		 */
		@Override
		public final String getWarning() {
			return warning != null ? warning : "";
		}

		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return stringvalue != null ? stringvalue : "";
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
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((stringvalue == null) ? 0 : stringvalue.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
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
			Attribute<?> other = (Attribute<?>) obj;
			return getValue().equals(other.getValue());
		}
	}

	/**
	 * The Class UnknownAttr.
	 */
	public static class UnknownAttr extends AbstractAttribute<String> {

		/**
		 * Instantiates a new unknown attr.
		 *
		 * @param node        the node
		 * @param key         the key
		 * @param format      the format
		 * @param stringvalue the stringvalue
		 */
		public UnknownAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.UNKNOWN, key, format, stringvalue);
			warn("Unknown attribute");
		}

		/**
		 * Gets the value.
		 *
		 * @return the value
		 */
		@Override
		public String getValue() {
			return stringvalue != null ? stringvalue : "";
		}

		/**
		 * To literal.
		 *
		 * @return the literal
		 */
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createStringLiteral(getValue());
		}
	}

	/**
	 * The Class StringAttr.
	 */
	public static class StringAttr extends AbstractAttribute<String> {

		/**
		 * Instantiates a new string attr.
		 *
		 * @param node        the node
		 * @param key         the key
		 * @param format      the format
		 * @param stringvalue the stringvalue
		 */
		public StringAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.STRING, key, format, stringvalue);
			if (stringvalue != null) {
				int length = format.optInt("length", -1);
				if (length >= 0 && stringvalue.length() != length)
					warn(ERROR_LENGTH);
				if (stringvalue.length() > format.optInt("maxlength", Integer.MAX_VALUE))
					warn(ERROR_TOO_LONG);
			}
		}

		/**
		 * Gets the value.
		 *
		 * @return the value
		 */
		@Override
		public String getValue() {
			return stringvalue != null ? stringvalue : "";
		}

		/**
		 * To literal.
		 *
		 * @return the literal
		 */
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createStringLiteral(getValue());
		}
	}

	/**
	 * The Class IDAttr.
	 */
	public static class IDAttr extends AbstractAttribute<String> {

		/** The Constant ID_REGEX. */
		protected static final Pattern ID_REGEX = Pattern.compile("[A-Z]{3}-?\\d{1,10}");

		/**
		 * Instantiates a new ID attr.
		 *
		 * @param node        the node
		 * @param type        the type
		 * @param key         the key
		 * @param format      the format
		 * @param stringvalue the stringvalue
		 */
		protected IDAttr(IsoXmlElement node, AttrType type, String key, @Nullable JSONObject format,
				@Nullable String stringvalue) {
			super(node, type, key, format, stringvalue);
			if (stringvalue != null) {
				if (!ID_REGEX.matcher(stringvalue).matches())
					warn(ERROR_FORMAT);
			}
		}

		/**
		 * Instantiates a new ID attr.
		 *
		 * @param node        the node
		 * @param key         the key
		 * @param format      the format
		 * @param stringvalue the stringvalue
		 */
		public IDAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			this(node, AttrType.ID, key, format, stringvalue);
		}

		/**
		 * Gets the value.
		 *
		 * @return the value
		 */
		@Override
		public String getValue() {
			return stringvalue != null ? stringvalue : "";
		}

		/**
		 * To literal.
		 *
		 * @return the literal
		 */
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createStringLiteral(getValue());
		}
	}

	/**
	 * The Class OIDAttr.
	 */
	public static class OIDAttr extends AbstractAttribute<Integer> {

		/** The value. */
		protected int value = 0;

		/**
		 * Instantiates a new OID attr.
		 *
		 * @param node        the node
		 * @param type        the type
		 * @param key         the key
		 * @param format      the format
		 * @param stringvalue the stringvalue
		 */
		protected OIDAttr(IsoXmlElement node, AttrType type, String key, @Nullable JSONObject format,
				@Nullable String stringvalue) {
			super(node, type, key, format, stringvalue);
			if (stringvalue != null) {
				try {
					value = Integer.parseInt(stringvalue);
					if (value < format.optInt("min", 1) || value > format.optInt("max", 65534))
						warn(ERROR_RANGE);
				} catch (NumberFormatException e) {
					error(ERROR_INVALID);
				}
			}
		}

		/**
		 * Instantiates a new OID attr.
		 *
		 * @param node        the node
		 * @param key         the key
		 * @param format      the format
		 * @param stringvalue the stringvalue
		 */
		public OIDAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			this(node, AttrType.OID, key, format, stringvalue);
		}

		/**
		 * Gets the value.
		 *
		 * @return the value
		 */
		@Override
		public Integer getValue() {
			return value;
		}

		/**
		 * To literal.
		 *
		 * @return the literal
		 */
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createTypedLiteral(value);
		}
	}

	/**
	 * The Class HEXAttr.
	 */
	public static class HEXAttr extends AbstractAttribute<byte[]> {

		/** The value. */
		protected byte[] value = new byte[0];

		/**
		 * Instantiates a new HEX attr.
		 *
		 * @param node        the node
		 * @param key         the key
		 * @param format      the format
		 * @param stringvalue the stringvalue
		 */
		public HEXAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.HEX, key, format, stringvalue);
			if (stringvalue != null) {
				try {
					value = Hex.decodeHex(stringvalue);
					if (value.length > format.optInt("maxbytes", Integer.MAX_VALUE))
						warn(ERROR_TOO_LONG);

				} catch (DecoderException e) {
					error(ERROR_INVALID);
				}
			}
		}

		/**
		 * Gets the value.
		 *
		 * @return the value
		 */
		@Override
		public byte[] getValue() {
			return value;
		}

		/**
		 * To literal.
		 *
		 * @return the literal
		 */
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createTypedLiteral(Hex.encodeHexString(value, false), XSDDatatype.XSDhexBinary);
		}
	}

	/**
	 * The Class DDIAttr.
	 */
	public static class DDIAttr extends AbstractAttribute<Integer> {

		/** The value. */
		protected int value = 0;

		/**
		 * Instantiates a new DDI attr.
		 *
		 * @param node        the node
		 * @param key         the key
		 * @param format      the format
		 * @param stringvalue the stringvalue
		 */
		public DDIAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.DDI, key, format, stringvalue);
			if (stringvalue != null) {
				try {
					value = Integer.parseInt(stringvalue, 16);
					if (value < 0 || value > 65534)
						error(ERROR_RANGE);
				} catch (NumberFormatException e) {
					error(ERROR_INVALID);
				}
			}
		}

		/**
		 * Gets the value.
		 *
		 * @return the value
		 */
		@Override
		public Integer getValue() {
			return value;
		}

		/**
		 * To literal.
		 *
		 * @return the literal
		 */
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createTypedLiteral(value);
		}
	}

	/**
	 * The Class IntAttr.
	 */
	public static class IntAttr extends AbstractAttribute<Integer> {

		/** The value. */
		protected int value = 0;

		/**
		 * Instantiates a new int attr.
		 *
		 * @param node        the node
		 * @param key         the key
		 * @param format      the format
		 * @param stringvalue the stringvalue
		 */
		public IntAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.INT, key, format, stringvalue);
			if (stringvalue != null) {
				try {
					value = Integer.parseInt(stringvalue);
					if (value < format.optInt("min", Integer.MIN_VALUE)
							|| value > format.optInt("max", Integer.MAX_VALUE))
						warn(ERROR_RANGE);
				} catch (NumberFormatException e) {
					error(ERROR_INVALID);
				}
			}
		}

		/**
		 * Gets the value.
		 *
		 * @return the value
		 */
		@Override
		public Integer getValue() {
			return value;
		}

		/**
		 * To literal.
		 *
		 * @return the literal
		 */
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createTypedLiteral(value);
		}
	}

	/**
	 * The Class ULongAttr.
	 */
	public static class ULongAttr extends AbstractAttribute<Long> {

		/** The value. */
		protected long value = 0;

		/**
		 * Instantiates a new u long attr.
		 *
		 * @param node        the node
		 * @param key         the key
		 * @param format      the format
		 * @param stringvalue the stringvalue
		 */
		public ULongAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.ULONG, key, format, stringvalue);
			if (stringvalue != null) {
				try {
					value = Long.parseLong(stringvalue);
					if (value < format.optLong("min", 0) || value > format.optLong("max", 4294967294L))
						warn(ERROR_RANGE);
				} catch (NumberFormatException e) {
					error(ERROR_INVALID);
				}
			}
		}

		/**
		 * Gets the value.
		 *
		 * @return the value
		 */
		@Override
		public Long getValue() {
			return value;
		}

		/**
		 * To literal.
		 *
		 * @return the literal
		 */
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createTypedLiteral(value);
		}
	}

	/**
	 * The Class UShortAttr.
	 */
	public static class UShortAttr extends AbstractAttribute<Integer> {

		/** The value. */
		protected int value = 0;

		/**
		 * Instantiates a new u short attr.
		 *
		 * @param node        the node
		 * @param key         the key
		 * @param format      the format
		 * @param stringvalue the stringvalue
		 */
		public UShortAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.USHORT, key, format, stringvalue);
			if (stringvalue != null) {
				try {
					value = Integer.parseInt(stringvalue);
					if (value < format.optInt("min", 0) || value > format.optInt("max", 65535))
						warn(ERROR_RANGE);
				} catch (NumberFormatException e) {
					error(ERROR_INVALID);
				}
			}
		}

		/**
		 * Gets the value.
		 *
		 * @return the value
		 */
		@Override
		public Integer getValue() {
			return value;
		}

		/**
		 * To literal.
		 *
		 * @return the literal
		 */
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createTypedLiteral(value);
		}
	}

	/**
	 * The Class ByteAttr.
	 */
	public static class ByteAttr extends AbstractAttribute<Integer> {

		/** The value. */
		protected int value = 0;

		/**
		 * Instantiates a new byte attr.
		 *
		 * @param node        the node
		 * @param key         the key
		 * @param format      the format
		 * @param stringvalue the stringvalue
		 */
		public ByteAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.BYTE, key, format, stringvalue);
			if (stringvalue != null) {
				try {
					value = Integer.parseInt(stringvalue);
					if (value < format.optInt("min", 0) || value > format.optInt("max", 254))
						warn(ERROR_RANGE);
				} catch (NumberFormatException e) {
					error(ERROR_INVALID);
				}
			}
		}

		/**
		 * Gets the value.
		 *
		 * @return the value
		 */
		@Override
		public Integer getValue() {
			return value;
		}

		/**
		 * To literal.
		 *
		 * @return the literal
		 */
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createTypedLiteral(value);
		}
	}

	/**
	 * The Class DoubleAttr.
	 */
	public static class DoubleAttr extends AbstractAttribute<Double> {

		/** The value. */
		protected double value = 0;

		/**
		 * Instantiates a new double attr.
		 *
		 * @param node        the node
		 * @param key         the key
		 * @param format      the format
		 * @param stringvalue the stringvalue
		 */
		public DoubleAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.DOUBLE, key, format, stringvalue);
			if (stringvalue != null) {
				try {
					value = Double.parseDouble(stringvalue);
					if (value < format.optDouble("min", Double.MIN_VALUE)
							|| value > format.optDouble("max", Double.MAX_VALUE))
						warn(ERROR_RANGE);
				} catch (NumberFormatException e) {
					error(ERROR_INVALID);
				}
			}
		}

		/**
		 * Gets the value.
		 *
		 * @return the value
		 */
		@Override
		public Double getValue() {
			return value;
		}

		/**
		 * To literal.
		 *
		 * @return the literal
		 */
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createTypedLiteral(value);
		}
	}

	/**
	 * The Class FloatAttr.
	 */
	public static class FloatAttr extends AbstractAttribute<Float> {

		/** The value. */
		protected float value = 0;

		/**
		 * Instantiates a new float attr.
		 *
		 * @param node        the node
		 * @param key         the key
		 * @param format      the format
		 * @param stringvalue the stringvalue
		 */
		public FloatAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.DECIMAL, key, format, stringvalue);
			if (stringvalue != null) {
				try {
					value = Float.parseFloat(stringvalue);
					if (value < format.optDouble("min", Float.MIN_VALUE)
							|| value > format.optDouble("max", Float.MAX_VALUE))
						warn(ERROR_RANGE);
				} catch (NumberFormatException e) {
					error(ERROR_INVALID);
				}
			}
		}

		/**
		 * Gets the value.
		 *
		 * @return the value
		 */
		@Override
		public Float getValue() {
			return value;
		}

		/**
		 * To literal.
		 *
		 * @return the literal
		 */
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createTypedLiteral(value);
		}
	}

	/**
	 * The Class DatetimeAttr.
	 */
	public static class DatetimeAttr extends AbstractAttribute<Instant> {

		/** The value. */
		protected Instant value = Instant.EPOCH;

		/**
		 * Instantiates a new datetime attr.
		 *
		 * @param node        the node
		 * @param key         the key
		 * @param format      the format
		 * @param stringvalue the stringvalue
		 */
		public DatetimeAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.DATETIME, key, format, stringvalue);
			if (stringvalue != null) {
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

		/**
		 * Gets the value.
		 *
		 * @return the value
		 */
		@Override
		public Instant getValue() {
			return value;
		}

		/**
		 * To literal.
		 *
		 * @return the literal
		 */
		@Override
		public Literal toLiteral() {
			return ResourceFactory
					.createTypedLiteral(GregorianCalendar.from(ZonedDateTime.ofInstant(value, ZoneOffset.UTC)));
		}
	}

	/**
	 * The Class EnumAttr.
	 */
	public static class EnumAttr extends AbstractAttribute<String> {

		/** The number. */
		protected byte number = 0;

		/** The value. */
		protected String value = "";

		/**
		 * Instantiates a new enum attr.
		 *
		 * @param node        the node
		 * @param key         the key
		 * @param format      the format
		 * @param stringvalue the stringvalue
		 */
		public EnumAttr(IsoXmlElement node, String key, @Nullable JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.ENUM, key, format, stringvalue);
			if (stringvalue != null) {
				try {
					number = Byte.parseByte(stringvalue);
					if (number < 0)
						error(ERROR_RANGE);
					else {
						value = format.getJSONArray("values").optString(number);
						if (value.isEmpty())
							error("Unknown token " + number);
					}
				} catch (NumberFormatException e) {
					error(ERROR_INVALID);
				}
			}
		}

		/**
		 * Number.
		 *
		 * @return the byte
		 */
		public byte number() {
			return number;
		}

		/**
		 * Gets the value.
		 *
		 * @return the value
		 */
		@Override
		public String getValue() {
			return value;
		}

		/**
		 * To literal.
		 *
		 * @return the literal
		 */
		@Override
		public Literal toLiteral() {
			return ResourceFactory.createStringLiteral(value);
		}
	}

}
