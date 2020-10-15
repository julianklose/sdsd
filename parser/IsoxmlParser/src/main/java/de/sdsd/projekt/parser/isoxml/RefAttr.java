package de.sdsd.projekt.parser.isoxml;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.json.JSONObject;

/**
 * Interface for attributes that contain a reference to other isoxml elements.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 */
public interface RefAttr {

	/**
	 * Gets the ref.
	 *
	 * @return the ref
	 */
	@CheckForNull
	public IsoXmlElement getRef();

	/**
	 * Try de ref.
	 *
	 * @return true, if successful
	 */
	public boolean tryDeRef();

	/**
	 * Reset.
	 *
	 * @return the ref attr
	 */
	public RefAttr reset();

	/**
	 * Gets the ref error.
	 *
	 * @return the ref error
	 */
	@CheckForNull
	public String getRefError();

	/**
	 * The Class IDRef.
	 */
	public static class IDRef extends Attribute.IDAttr implements RefAttr {

		/** The ref. */
		@CheckForNull
		private IsoXmlElement ref = null;

		/**
		 * Instantiates a new ID ref.
		 *
		 * @param node        the node
		 * @param key         the key
		 * @param format      the format
		 * @param stringvalue the stringvalue
		 */
		public IDRef(IsoXmlElement node, String key, JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.ID_REF, key, format, stringvalue);
		}

		/**
		 * Gets the ref.
		 *
		 * @return the ref
		 */
		@CheckForNull
		@Override
		public IsoXmlElement getRef() {
			if (ref == null)
				tryDeRef();
			return ref;
		}

		/**
		 * Try de ref.
		 *
		 * @return true, if successful
		 */
		@Override
		public boolean tryDeRef() {
			if (ref != null)
				return true;
			if (stringvalue == null || stringvalue.isEmpty())
				return false;
			ref = node.getRoot().getNodeById(getValue());
			return ref != null;
		}

		/**
		 * Reset.
		 *
		 * @return the ref attr
		 */
		@Override
		public RefAttr reset() {
			ref = null;
			return this;
		}

		/**
		 * Gets the ref error.
		 *
		 * @return the ref error
		 */
		@CheckForNull
		@Override
		public String getRefError() {
			if (ref != null)
				return null;
			return createError(String.format("Couldn't find global reference '%s'", getStringValue()));
		}
	}

	/**
	 * The Class OIDRef.
	 */
	public static class OIDRef extends Attribute.OIDAttr implements RefAttr {

		/** The ref. */
		@CheckForNull
		private IsoXmlElement ref = null;

		/**
		 * Instantiates a new OID ref.
		 *
		 * @param node        the node
		 * @param key         the key
		 * @param format      the format
		 * @param stringvalue the stringvalue
		 */
		public OIDRef(IsoXmlElement node, String key, JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.OID_REF, key, format, stringvalue);
		}

		/**
		 * Gets the ref.
		 *
		 * @return the ref
		 */
		@CheckForNull
		@Override
		public IsoXmlElement getRef() {
			if (ref == null)
				tryDeRef();
			return ref;
		}

		/**
		 * Try de ref.
		 *
		 * @return true, if successful
		 */
		@Override
		public boolean tryDeRef() {
			if (ref != null)
				return true;
			if (!hasValue() || hasError())
				return false;
			IsoXmlElement parent = node;
			while (parent != null) {
				ref = parent.getNodeByOId(getValue());
				if (ref != null)
					return true;
				parent = parent.getParent();
			}
			return false;
		}

		/**
		 * Reset.
		 *
		 * @return the ref attr
		 */
		@Override
		public RefAttr reset() {
			ref = null;
			return this;
		}

		/**
		 * Gets the ref error.
		 *
		 * @return the ref error
		 */
		@CheckForNull
		@Override
		public String getRefError() {
			if (ref != null)
				return null;
			return createError(String.format("Couldn't find object reference '%d'", getValue()));
		}
	}
}
