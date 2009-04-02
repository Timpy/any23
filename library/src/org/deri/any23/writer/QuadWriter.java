package org.deri.any23.writer;

import org.deri.any23.extractor.ExtractionContext;
import org.deri.any23.vocab.ANY23;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDFS;

/**
 * A triple handler that converts triples to quads by using the
 * document URI of each triple's context as the graph name.
 * Optionally, a metadata graph can be specified; for each
 * document URI, it will record which extractors were used on
 * it, and the document title if any.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class QuadWriter implements TripleHandler {
	
	public static interface QuadHandler {
		void writeQuad(Resource s, URI p, Value o, URI g);
		void close();
	}

	private final QuadHandler quadHandler;
	private final URI metaGraph;
	private final ValueFactory factory = ValueFactoryImpl.getInstance();
	
	public QuadWriter(QuadHandler quadHandler) {
		this(quadHandler, null);
	}
	
	public QuadWriter(QuadHandler quadHandler, String metadataGraphURI) {
		this.quadHandler = quadHandler;
		this.metaGraph = (metadataGraphURI == null) 
				? null : factory.createURI(metadataGraphURI);
	}
	
	public void openContext(ExtractionContext context) {
		if (metaGraph == null) return;
		quadHandler.writeQuad(
				factory.createURI(context.getDocumentURI()), 
				ANY23.EXTRACTOR, 
				ANY23.getExtractorResource(context.getExtractorName()), 
				metaGraph);
	}
	
	public void closeContext(ExtractionContext context) {
		// do nothing
	}

	public void receiveTriple(Resource s, URI p, Value o, ExtractionContext context) {
		quadHandler.writeQuad(s, p, o, 
				ValueFactoryImpl.getInstance().createURI(context.getDocumentURI()));
	}
	
	public void receiveLabel(String label, ExtractionContext context) {
		if (metaGraph == null || !context.isDocumentContext()) return;
		quadHandler.writeQuad(
				factory.createURI(context.getDocumentURI()), 
				RDFS.LABEL, 
				factory.createLiteral(label), 
				metaGraph);
	}

	public void close() {
		quadHandler.close();
	}
}