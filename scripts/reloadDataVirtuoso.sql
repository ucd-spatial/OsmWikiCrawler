-- Comment

LOAD rdfbulkloader.sql;

-- DF_GLOBAL_RESET();

SPARQL CLEAR GRAPH <http://www.w3.org/2006/03/wn/wn20/>;

ld_dir('/usr/share/virtuoso/vad/', 'wordnet*.rdf', 'http://www.w3.org/2006/03/wn/wn20/');

rdf_loader_run();

SPARQL CLEAR GRAPH <http://spatial.ucd.ie/lod/osn/>;

DB.DBA.RDF_LOAD_RDFXML (http_get('http://spatial.ucd.ie/osn/skos/osm_semantic_network.skos.rdf'), '', 'http://spatial.ucd.ie/lod/osn/');
