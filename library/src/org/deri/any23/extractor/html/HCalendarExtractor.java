package org.deri.any23.extractor.html;

import java.util.Arrays;
import java.util.List;

import org.deri.any23.extractor.ExtractionContext;
import org.deri.any23.extractor.ExtractorDescription;
import org.deri.any23.extractor.ExtractorFactory;
import org.deri.any23.extractor.SimpleExtractorFactory;
import org.deri.any23.rdf.PopularPrefixes;
import org.deri.any23.vocab.ICAL;
import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.w3c.dom.Node;

/**
 * Extractor for the <a href="http://microformats.org/wiki/hcalendar">hCalendar</a>
 * microformat.
 * 
 * @author Gabriele Renzi
 */
public class HCalendarExtractor extends MicroformatExtractor {
	private ExtractionContext context;

	@Override
	protected boolean extract(ExtractionContext context) {
		this.context = context;
		List<Node> calendars = document.findAllByClassName("vcalendar");
		if (calendars.size() == 0)
			// vcal allows to avoid top name, in which case whole document is
			// the calendar, let's try
			if (document.findAllByClassName("vevent").size() > 0)
				calendars.add(document.getDocument());

		boolean foundAny = false;
		for (Node node : calendars)
			foundAny |= extractCalendar(node);

		return foundAny;
	}

	private boolean extractCalendar(Node node) {
		URI cal = valueFactory.createURI(baseURI.toString());
		out.writeTriple(cal, RDF.TYPE, ICAL.Vcalendar, context);
		return addComponents(node, cal);
	}

	private static final String[] Components = { "Vevent", "Vtodo", "Vjournal",
			"Vfreebusy" };

	private boolean addComponents(Node node, Resource cal) {
		boolean foundAny = false;
		for (String component : Components) {
			List<Node> events = DomUtils.findAllByClassName(node, component);
			if (events.size() == 0)
				continue;
			for (Node evtNode : events)
				foundAny |= extractComponent(evtNode, cal, component);
		}
		return foundAny;
	}

	private boolean extractComponent(Node node, Resource cal, String component) {
		HTMLDocument compoNode = new HTMLDocument(node);
		Resource evt = valueFactory.createBNode();
		out.writeTriple(evt, RDF.TYPE, ICAL.getResource(component), context);
		addTextProps(compoNode, evt);
		addUrl(compoNode, evt);
		addRRule(compoNode, evt);
		addOrganizer(compoNode, evt);
		addUid(compoNode,evt);
		out.writeTriple(cal, ICAL.component, evt, context);
		return true;
	}

	private void addUid(HTMLDocument compoNode, Resource evt) {
		String url = compoNode.getSingularUrlField("uid");
		conditionallyAddStringProperty(evt, ICAL.uid, url);
	}

	private void addUrl(HTMLDocument compoNode, Resource evt) {
		String url = compoNode.getSingularUrlField("url");
		if ("".equals(url)) return;
		out.writeTriple(evt, ICAL.url, valueFactory.createURI(absolutizeURI(url)), context);
	}

	private void addRRule(HTMLDocument compoNode, Resource evt) {
		for (Node rule : compoNode.findAllByClassName("rrule")) {
			BNode rrule = valueFactory.createBNode();
			out.writeTriple(rrule, RDF.TYPE, ICAL.DomainOf_rrule, context);
			String freq = new HTMLDocument(rule).getSingularTextField("freq");
			conditionallyAddStringProperty(rrule, ICAL.freq, freq);
			out.writeTriple(evt, ICAL.rrule, rrule, context);
		}
	}

	private void addOrganizer(HTMLDocument compoNode, Resource evt) {
		for (Node organizer : compoNode.findAllByClassName("organizer")) {
			//untyped
			BNode blank = valueFactory.createBNode();
			String mail = new HTMLDocument(organizer).getSingularUrlField("organizer");
			conditionallyAddStringProperty(blank, ICAL.calAddress, mail);
			out.writeTriple(evt, ICAL.organizer, blank, context);
		}
	}
	
	private String[] textSingularProps = { "dtstart", "dtstamp", "dtend", "summary", "class", "transp", "description", "status","location" };

	private void addTextProps(HTMLDocument node, Resource evt) {

		for (String date : textSingularProps) {
			String val = node.getSingularTextField(date);
			conditionallyAddStringProperty(evt, ICAL.getProperty(date), val);
		}
		String[] values = node.getPluralTextField("category");
		for (String val : values) {
			conditionallyAddStringProperty(evt, ICAL.categories, val);
		}
	}

	public ExtractorDescription getDescription() {
		return factory;
	}
	
	public final static ExtractorFactory<HCalendarExtractor> factory = 
		SimpleExtractorFactory.create(
				"html-mf-hcalendar",
				PopularPrefixes.createSubset("rdf", "ical"),
				Arrays.asList("text/html;q=0.1", "application/xhtml+xml;q=0.1"),
				null,
				HCalendarExtractor.class);
}