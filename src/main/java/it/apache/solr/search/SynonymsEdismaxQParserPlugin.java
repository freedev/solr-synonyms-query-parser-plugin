package it.apache.solr.search;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.synonym.SynonymFilterFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.util.Version;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.ExtendedDismaxQParserPlugin;
import org.apache.solr.search.QParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SynonymsEdismaxQParserPlugin extends ExtendedDismaxQParserPlugin implements ResourceLoaderAware {

	private static final Logger logger = LoggerFactory.getLogger(SynonymsEdismaxQParserPlugin.class);
	private String synonyms;
	private String tokenizerFactory;

	private ResourceLoader loader;
	private SynonymFilterFactory syf;

	private boolean ignoreCase = true;
	private boolean expand = false;

	@Override
	@SuppressWarnings("rawtypes") 
	public void init(NamedList initArgs) {
		logger.info("init ...");
		SolrParams params = SolrParams.toSolrParams(initArgs);
		synonyms = params.get("synonyms");
		tokenizerFactory = params.get("tokenizerFactory");

		if (synonyms != null) {

			String ignoreCaseSt = params.get("ignoreCase");
			if (ignoreCaseSt != null && ignoreCaseSt.equalsIgnoreCase("false")) {
				ignoreCase = false;
			}

			String expandSt = params.get("expand");
			if (expandSt != null && expandSt.equalsIgnoreCase("false")) {
				expand = false;
			}
			
			if (tokenizerFactory == null)
				tokenizerFactory = "solr.WhitespaceTokenizerFactory";

			logger.info("synonyms= " + synonyms);
			logger.info("ignoreCase= " + ignoreCase);
			logger.info("expand= " + expand);
			logger.info("tokenizerFactory= " + tokenizerFactory);

			Map<String, String> args = new HashMap<>();
			args.put("synonyms", synonyms);
			args.put("luceneMatchVersion", org.apache.lucene.util.Version.LUCENE_48.toString());
			args.put("ignoreCase", Boolean.toString(ignoreCase));
			args.put("expand", Boolean.toString(expand));
			args.put("tokenizerFactory", tokenizerFactory);
			try {
				logger.info("creating new SynonymFilterFactory");
				syf = new SynonymFilterFactory(args);
				logger.info("created new SynonymFilterFactory");
			} catch (SolrException e) {
				logger.error("Error creating SynonymFilterFactory "+ e.getMessage());
				syf = null;
			}
			logger.info("init complete.");
		}

	}

	@Override
	public QParser createParser(String qStr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {

		ModifiableSolrParams modparams = new ModifiableSolrParams(params);
		String modQ = null;
		try {
			logger.debug("evaluating: "+qStr);
			modQ = applySynonyms(qStr);
			if (!modQ.equals(qStr))
				logger.info(qStr + " -> " + modQ);
			modparams.set("q", modQ);
		} catch (IOException ioe) {
			logger.error(ioe.getMessage());
		}

		return super.createParser(modQ, localParams, modparams, req);

	}

	private String applySynonyms(String input) throws IOException {
		if (syf == null) {
			logger.warn("syf is null exiting!!!");
			return input;
		}
		StringBuffer strbuf = new StringBuffer();
		try (WhitespaceTokenizer wt = new WhitespaceTokenizer(Version.LUCENE_48, new StringReader(input))) {
			try (TokenStream syn = syf.create(wt)) {
				CharTermAttribute term = syn.addAttribute(CharTermAttribute.class);
				syn.reset();
				String t = null;
				while (syn.incrementToken()) {
					if (t != null)
						strbuf.append(" ");
					t = term.toString();
					strbuf.append(t);
				}
			}
		}
		return strbuf.toString();
	}

	@Override
	public void inform(ResourceLoader loader) throws IOException {
		this.loader = loader;
		if (synonyms != null) {
			try {
				if (syf != null) {
					logger.debug("loading synonyms " + synonyms);
					syf.inform(this.loader);
				} else {
					logger.warn("syf = null, unable to load synonyms...");
				}
			} catch (IOException e) {
				logger.error("Failed to create parser " + e.getMessage());
				syf = null;
			}
		}
	}

}
