-- Comment

-- DF_GLOBAL_RESET();

SPARQL CLEAR GRAPH <http://spatial.ucd.ie/lod/osn/>;
DB.DBA.RDF_LOAD_RDFXML (http_get('http://spatial.ucd.ie/osn/skos/osm_semantic_network.skos.rdf'), '', 'http://spatial.ucd.ie/lod/osn/');
