package de.sdsd.projekt.parser.interfaces;

/**
 * Interface to be implemented by CSV mapping classes being clearly identifiable
 * by an ID attribute (surrogate primary key).
 * 
 * @author <a href="mailto:andreas.schliebitz@hs-osnabrueck.de">Andreas
 *         Schliebitz</a>
 *
 */
public interface Identifiable {
	public Long getId();
}