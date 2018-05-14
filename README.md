solr-synonyms-query-parser-plugin
=======================

SolrCloud Synonyms Query Parser Plugin

This is an example that explain how to extend Edismax and Dismax query parser plugins.

solrconfig.xml queryParser entry example:

    <queryParser name="autophrasingParser" class="it.apache.solr.search.SynonymsEdismaxQParserPlugin">
        <str name="synonyms">synonyms-file1.txt,synonyms-split-on-case-change.txt,synonyms-manufacturer.txt</str>
        <str name="ignoreCase">true</str>
        <str name="expand">false</str>
        <str name="tokenizerFactory">solr.WhitespaceTokenizerFactory</str>
    </queryParser> 

OR

    <queryParser name="autophrasingParser" class="it.apache.solr.search.SynonymsDismaxQParserPlugin">
        <str name="synonyms">synonyms-file1.txt,synonyms-split-on-case-change.txt,synonyms-manufacturer.txt</str>
        <str name="ignoreCase">true</str>
        <str name="expand">false</str>
        <str name="tokenizerFactory">solr.WhitespaceTokenizerFactory</str>
    </queryParser> 

