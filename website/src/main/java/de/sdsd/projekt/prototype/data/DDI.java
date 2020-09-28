package de.sdsd.projekt.prototype.data;

import java.io.Serializable;
import java.util.HashSet;

/**
 * Represents a DDI with information from the wikinormia.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class DDI implements Serializable, Comparable<DDI> {
	private static final long serialVersionUID = 994461721075761210L;
	
	private final int ddi;
	private final String name, description;
	private final DDICategory category;
	
	public DDI(int ddi, String name, String description, DDICategory category) {
		this.ddi = ddi;
		this.name = name;
		this.description = description;
		this.category = category;
		category.add(this);
	}

	public int getDDI() {
		return ddi;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public DDICategory getCategory() {
		return category;
	}
	
	@Override
	public String toString() {
		return String.format("[%d] %s(%s)", ddi, name, category.getName());
	}

	@Override
	public int hashCode() {
		return ddi;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DDI other = (DDI) obj;
		if (ddi != other.ddi)
			return false;
		return true;
	}

	@Override
	public int compareTo(DDI o) {
		return Integer.compare(ddi, o.ddi);
	}
	
	
	/**
	 * Represents a DDI category with information from the wikinormia.
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class DDICategory extends HashSet<DDI> implements Comparable<DDICategory> {
		private static final long serialVersionUID = 4425653707070892688L;
		
		private final int index;
		private final String name, description;

		public DDICategory(int index, String name, String description) {
			super();
			this.index = index;
			this.name = name;
			this.description = description;
		}
		
		public int getIndex() {
			return index;
		}

		public String getName() {
			return name;
		}

		public String getDescription() {
			return description;
		}
		
		@Override
		public String toString() {
			return name;
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DDICategory other = (DDICategory) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

		@Override
		public int compareTo(DDICategory o) {
			return Integer.compare(index, o.index);
		}
	}
}
