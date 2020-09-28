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
	@CsvBindByName
	@HelmLabel
	private String software;
	@CsvBindByName
	private String version;
	@CsvBindByName
	private String exportTyp;
	@CsvCustomBindByName(converter = HelmDateConverter.class)
	private Date erstelldatum;

	@Override
	public String toString() {
		return "Schnittstelle [software=" + software + ", version=" + version + ", exportTyp=" + exportTyp
				+ ", erstelldatum=" + erstelldatum + "]";
	}
}