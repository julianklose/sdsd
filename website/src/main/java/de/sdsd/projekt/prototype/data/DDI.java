package de.sdsd.projekt.prototype.data;

import java.io.Serializable;
import java.util.HashSet;

/**
 * Represents a DDI with information from the wikinormia.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
 */
public class DDI implements Serializable, Comparable<DDI> {
	
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 994461721075761210L;
	
	/** The ddi. */
	private final int ddi;
	
	/** The description. */
	private final String name, description;
	
	/** The category. */
	private final DDICategory category;
	
	/**
	 * Instantiates a new ddi.
	 *
	 * @param ddi the ddi
	 * @param name the name
	 * @param description the description
	 * @param category the category
	 */
	public DDI(int ddi, String name, String description, DDICategory category) {
		this.ddi = ddi;
		this.name = name;
		this.description = description;
		this.category = category;
		category.add(this);
	}

	/**
	 * Gets the ddi.
	 *
	 * @return the ddi
	 */
	public int getDDI() {
		return ddi;
	}

	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the description.
	 *
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Gets the category.
	 *
	 * @return the category
	 */
	public DDICategory getCategory() {
		return category;
	}
	
	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return String.format("[%d] %s(%s)", ddi, name, category.getName());
	}

	/**
	 * Hash code.
	 *
	 * @return the int
	 */
	@Override
	public int hashCode() {
		return ddi;
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
		DDI other = (DDI) obj;
		if (ddi != other.ddi)
			return false;
		return true;
	}

	/**
	 * Compare to.
	 *
	 * @param o the o
	 * @return the int
	 */
	@Override
	public int compareTo(DDI o) {
		return Integer.compare(ddi, o.ddi);
	}
	
	
	/**
	 * Represents a DDI category with information from the wikinormia.
	 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian Klose</a>
	 */
	public static class DDICategory extends HashSet<DDI> implements Comparable<DDICategory> {
		
		/** The Constant serialVersionUID. */
		private static final long serialVersionUID = 4425653707070892688L;
		
		/** The index. */
		private final int index;
		
		/** The description. */
		private final String name, description;

		/**
		 * Instantiates a new DDI category.
		 *
		 * @param index the index
		 * @param name the name
		 * @param description the description
		 */
		public DDICategory(int index, String name, String description) {
			super();
			this.index = index;
			this.name = name;
			this.description = description;
		}
		
		/**
		 * Gets the index.
		 *
		 * @return the index
		 */
		public int getIndex() {
			return index;
		}

		/**
		 * Gets the name.
		 *
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Gets the description.
		 *
		 * @return the description
		 */
		public String getDescription() {
			return description;
		}
		
		/**
		 * To string.
		 *
		 * @return the string
		 */
		@Override
		public String toString() {
			return name;
		}

		/**
		 * Hash code.
		 *
		 * @return the int
		 */
		@Override
		public int hashCode() {
			return name.hashCode();
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
			DDICategory other = (DDICategory) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

		/**
		 * Compare to.
		 *
		 * @param o the o
		 * @return the int
		 */
		@Override
		public int compareTo(DDICategory o) {
			return Integer.compare(index, o.index);
		}
	}
}
