package de.sdsd.projekt.parser.helmdata;

import java.util.Date;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvCustomBindByName;

import de.sdsd.projekt.parser.annotations.HelmLabel;

/**
 * CSV mapping class for RAX entity "Schnittstelle".
 * 
 * @author <a href="mailto:andreas.schliebitz@hs-osnabrueck.de">Andreas
 *         Schliebitz</a>
 *
 */
public class Schnittstelle {
	
	/** The software. */
	@CsvBindByName
	@HelmLabel
	private String software;
	
	/** The version. */
	@CsvBindByName
	private String version;
	
	/** The export typ. */
	@CsvBindByName
	private String exportTyp;
	
	/** The erstelldatum. */
	@CsvCustomBindByName(converter = HelmDateConverter.class)
	private Date erstelldatum;

	/**
	 * To string.
	 *
	 * @return the string
	 */
	@Override
	public String toString() {
		return "Schnittstelle [software=" + software + ", version=" + version + ", exportTyp=" + exportTyp
				+ ", erstelldatum=" + erstelldatum + "]";
	}
}