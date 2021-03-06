package de.sdsd.projekt.parser.wrapper;

/**
 * Wrapper class for ID attributes associated with their class type.
 * 
 * @author <a href="mailto:andreas.schliebitz@hs-osnabrueck.de">Andreas
 *         Schliebitz</a>
 *
 */
public class Id {
	
	/** The clazz. */
	private Class<?> clazz;
	
	/** The id. */
	private Long id;

	/**
	 * Instantiates a new id.
	 *
	 * @param clazz the clazz
	 * @param id the id
	 */
	public Id(Class<?> clazz, Long id) {
		this.clazz = clazz;
		this.id = id;
	}

	/**
	 * Gets the clazz.
	 *
	 * @return the clazz
	 */
	public Class<?> getClazz() {
		return clazz;
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public Long getId() {
		return id;
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
		result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		Id other = (Id) obj;
		if (clazz == null) {
			if (other.clazz != null)
				return false;
		} else if (!clazz.equals(other.clazz))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return "RowId [clazz=" + clazz + ", id=" + id + "]";
	}
}