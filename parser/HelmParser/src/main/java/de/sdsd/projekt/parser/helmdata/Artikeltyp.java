package de.sdsd.projekt.parser.helmdata;

import com.opencsv.bean.CsvBindByName;

import de.sdsd.projekt.parser.annotations.HelmLabel;
import de.sdsd.projekt.parser.annotations.HelmTransient;
import de.sdsd.projekt.parser.interfaces.Identifiable;

/**
 * CSV mapping class for RAX entity "Artikeltyp".
 * 
 * @author <a href="mailto:andreas.schliebitz@hs-osnabrueck.de">Andreas
 *         Schliebitz</a>
 *
 */
public class Artikeltyp implements Identifiable {
	@CsvBindByName
	@HelmTransient
	private Long artikelTypId;
	@CsvBindByName
	@HelmLabel
	private String artikelTypName;

	@Override
	public Long getId() {
		return artikelTypId;
	}

	@Override
	public String toString() {
		return "Artikeltyp [artikelTypId=" + artikelTypId + ", artikelTypName=" + artikelTypName + "]";
	}
}