package de.sdsd.projekt.parser.isoxml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.xml.sax.SAXException;

import de.sdsd.projekt.api.ParserAPI;
import de.sdsd.projekt.parser.isoxml.TimeLog.ValueDescription;

/**
 * Stores the required informations to read binary isoxml timelogs.
 * 
 * @author <a href="mailto:48514372+julianklose@users.noreply.github.com">Julian
 *         Klose</a>
 * 
 */
class TimeLogConfig {

	/** The headerlength. */
	private final int headerlength;

	/** The timelog. */
	private final Supplier<ParserAPI.TimeLog> timelog;

	/** The header. */
	private final List<Attribute<?>> header;

	/** The dlvs. */
	private final List<IsoXmlElement> dlvs;

	/** The value descriptions. */
	private List<ValueDescription> valueDescriptions = null;

	/**
	 * Instantiates a new time log config.
	 *
	 * @param tim     the tim
	 * @param timelog the timelog
	 * @throws IllegalArgumentException the illegal argument exception
	 */
	public TimeLogConfig(IsoXmlElement tim, Supplier<ParserAPI.TimeLog> timelog) throws IllegalArgumentException {
		this.timelog = timelog;
		IsoXmlElement ptn = tim.findChild("PTN");
		if (ptn == null)
			throw new IllegalArgumentException("TIM has no PTN element");

		List<Attribute<?>> header = new ArrayList<>(5);
		header.add(tim.getAttribute("start"));
		ptn.getAttributes().values().stream().filter(Attribute::hasValue).forEachOrdered(header::add);
		this.header = Collections.unmodifiableList(header);

		this.headerlength = header.stream().filter(TimeLog::isHeaderRead).mapToInt(TimeLog::headerByteCount).sum();

		this.dlvs = Collections.unmodifiableList(tim.findChildren("DLV"));
	}

	/**
	 * Gets the header.
	 *
	 * @return the header
	 */
	public List<Attribute<?>> getHeader() {
		return header;
	}

	/**
	 * Gets the header length.
	 *
	 * @return the header length
	 */
	public int getHeaderLength() {
		return headerlength;
	}

	/**
	 * Gets the dlvs.
	 *
	 * @return the dlvs
	 */
	public List<IsoXmlElement> getDlvs() {
		return dlvs;
	}

	/**
	 * Gets the value descriptions.
	 *
	 * @return the value descriptions
	 * @throws SAXException the SAX exception
	 */
	public List<ValueDescription> getValueDescriptions() throws SAXException {
		if (valueDescriptions == null)
			findReferences();
		return valueDescriptions;
	}

	/**
	 * Find references.
	 *
	 * @throws SAXException the SAX exception
	 */
	public void findReferences() throws SAXException {
		ParserAPI.TimeLog tlg = timelog.get();
		List<ValueDescription> valueDescriptions = new ArrayList<>(dlvs.size());
		for (IsoXmlElement dlv : dlvs) {
			valueDescriptions.add(ValueDescription.create(dlv, tlg, false));
		}
		this.valueDescriptions = Collections.unmodifiableList(valueDescriptions);
	}
}