package de.sdsd.projekt.parser.isoxml;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.json.JSONObject;

/**
 * Interface for attributes that contain a reference to other isoxml elements.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public interface RefAttr {
	@CheckForNull
	public IsoXmlElement getRef();
	public boolean tryDeRef();
	public RefAttr reset();
	@CheckForNull
	public String getRefError();

	public static class IDRef extends Attribute.IDAttr implements RefAttr {
		@CheckForNull
		private IsoXmlElement ref = null;

		public IDRef(IsoXmlElement node, String key, JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.ID_REF, key, format, stringvalue);
		}
		
		@CheckForNull
		@Override
		public IsoXmlElement getRef() {
			if(ref == null) tryDeRef(); 
			return ref;
		}

		@Override
		public boolean tryDeRef() {
			if(ref != null) return true;
			if(stringvalue == null || stringvalue.isEmpty()) return false;
			ref = node.getRoot().getNodeById(getValue());
			return ref != null;
		}

		@Override
		public RefAttr reset() {
			ref = null;
			return this;
		}

		@CheckForNull
		@Override
		public String getRefError() {
			if(ref != null) return null;
			return createError(String.format("Couldn't find global reference '%s'", getStringValue()));
		}
	}
	
	public static class OIDRef extends Attribute.OIDAttr implements RefAttr {
		@CheckForNull
		private IsoXmlElement ref = null;

		public OIDRef(IsoXmlElement node, String key, JSONObject format, @Nullable String stringvalue) {
			super(node, AttrType.OID_REF, key, format, stringvalue);
		}

		@CheckForNull
		@Override
		public IsoXmlElement getRef() {
			if(ref == null) tryDeRef();
			return ref;
		}

		@Override
		public boolean tryDeRef() {
			if(ref != null) return true;
			if(!hasValue() || hasError()) return false;
			IsoXmlElement parent = node;
			while(parent != null) {
				ref = parent.getNodeByOId(getValue());
				if(ref != null) return true;
				parent = parent.getParent();
			}
			return false;
		}

		@Override
		public RefAttr reset() {
			ref = null;
			return this;
		}
		
		@CheckForNull
		@Override
		public String getRefError() {
			if(ref != null) return null;
			return createError(String.format("Couldn't find object reference '%d'", getValue()));
		}
	}
}
