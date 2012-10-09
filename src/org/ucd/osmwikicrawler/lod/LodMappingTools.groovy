/*
This file is part of the OSM Wiki Crawler.

   The OSM Wiki Crawler is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   The OSM Wiki Crawler is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with the OSM Wiki Crawler.  If not, see <http://www.gnu.org/licenses/>.
   
   Author: Andrea Ballatore
   Project home page: http://gforge.ucd.ie/projects/osm-similarity/
*/
package org.ucd.osmwikicrawler.lod

import org.apache.log4j.Logger
import org.ucd.osmwikicrawler.rdf.WikiRdf
import org.ucd.osmwikicrawler.utils.OntoUtils
import com.hp.hpl.jena.graph.impl.*
import com.hp.hpl.jena.query.*
import com.hp.hpl.jena.rdf.model.*
import com.hp.hpl.jena.shared.*
import com.hp.hpl.jena.sparql.resultset.ResultSetException
import com.hp.hpl.jena.query.ResultSetRewindable;

/**
 * 
 * @author Andrea Ballatore
 *
 */
class LodMappingTools {
	
	
	static def log = Logger.getLogger(LodMappingTools.class)
	
	static String _OSN_SPARQL_ENDPOINT = "http://spatial.ucd.ie/lod/sparql"
	static String _DBP_SPARQL_ENDPOINT = "http://dbpedia.org/sparql"
	
	static def getAllOsnConcepts(){
		log.info("Getting all concepts from OSN via SPARQL...")
		String q = "select ?c { ?c a <${OntoUtils.SKOS_CONCEPT}> }"
		
		q += " LIMIT 5"
		def conceptsUris = queryRemoteSparql(q, _OSN_SPARQL_ENDPOINT, "c")
		log.debug conceptsUris
		assert conceptsUris.size() > 0
		return conceptsUris
	}
	
	static Model createMappingRdfModel(){
		Model m = ModelFactory.createDefaultModel()
		m = WikiRdf.setPrefixes(m)
		return m
	}
	
	/**
	 * 
	 * @param query
	 * @param endpoint
	 * @param field
	 * @return
	 */
	static def queryRemoteSparql( String query, String endpoint, String field){
		assert query
		assert endpoint
		assert field
		log.debug "queryRemoteSparql: query=$query\nendpoint=$endpoint field=$field"
		
		try{
			def qexec = QueryExecutionFactory.sparqlService( endpoint, query )
			ResultSet res = qexec.execSelect()
			ResultSetRewindable rwRss = ResultSetFactory.makeRewindable( res )
			def fields = OntoUtils.getValuesFromJenaResultSet(rwRss, field)
			return fields
		} catch ( com.hp.hpl.jena.query.QueryParseException e ){
			log.error "error in '$query'"
			log.warn e
			return null
		} 
	}
	
	static def getOsnKey( String uri ){
		String q = "select ?k { <$uri> <$OntoUtils.SOSM_KEY_LABEL> ?k }"
		def keys = queryRemoteSparql(q, _OSN_SPARQL_ENDPOINT, "k")
		assert keys.size() == 1 | keys.size() == 0
		if (keys.size()==1) return keys[0]
		return null
	}
	
	static def getOsnValues( String uri ){
		String q = "select ?k { <$uri> <$OntoUtils.SOSM_VALUE_LABEL> ?k }"
		def vals = queryRemoteSparql(q, _OSN_SPARQL_ENDPOINT, "k")
		assert vals.size() >= 0
		return vals
	}
	
	static String getOsnDefinition( String uri, String lang = "en" ){
		String q = "select ?k { <$uri> <$OntoUtils.SKOS_DEFINITION> ?k FILTER ( lang(?k) = \"${lang}\" ) }"
		def defs = queryRemoteSparql(q, _OSN_SPARQL_ENDPOINT, "k")
		assert defs.size() == 1 || defs.size() == 0 || defs.size() == 2
		if (defs.size()>0) return defs[0]
		return null
	}

	/**
	 * 
	 * @param uri
	 * @param m
	 * @return
	 */
	static def processOsnUri( String uri, Model m ){
		assert uri
		assert m
		log.info "Processing '$uri'"
		
		findMappingOnDbpedia(uri,m)
		
	}
	
	static def findDbpediaOntoTerms( String search ){
		assert search
		if (search.length() > 100){
			log.warn "invalid key for dbdpia search $search"
			return null
		}
		log.info "findDbpediaOntoTerms '$search'"
		String q = "select distinct ?s from <http://dbpedia.org> where { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> FILTER ( regex(?s, \"${search}\",\"i\") ) }"
		log.debug q
		def uris = queryRemoteSparql(q, _DBP_SPARQL_ENDPOINT, "s")
		return uris
	}
	
	static def getLabelForDBpediaUri( String uri, String lang = "en"){
		String q = "select distinct ?l from <http://dbpedia.org> where { <$uri> <http://www.w3.org/2000/01/rdf-schema#label> ?l FILTER ( lang(?l) = \"$lang\" ) }"
		log.debug q
		def labels = queryRemoteSparql(q, _DBP_SPARQL_ENDPOINT, "l")
		return labels
	}
	
	/**
	 * 
	 * @param uri
	 * @param m
	 * @return
	 */
	static def findMappingOnDbpedia( String uri, Model m ){
		log.info "\tChecking DBpedia..."
		
		// fetch OSN uri info
		String k = getOsnKey(uri)
		log.info "k = $k"
		def vals = getOsnValues(uri)
		log.info "vals = $vals"
		String d = getOsnDefinition(uri)
		log.info "d = $d"
		
		
		def dbpOntoUris = findDbpediaOntoTerms(k)
		vals.each{
			def dbpOntoUrisV = findDbpediaOntoTerms(it)
			if (dbpOntoUrisV)
				dbpOntoUris.addAll(dbpOntoUrisV)
		}
		
		log.debug "dbpOntoUris = $dbpOntoUris"
		dbpOntoUris.each{ uri2->
			log.info("\t" + uri2 + " -> " + getLabelForDBpediaUri( uri2 ))
		}
		
		// find related resources on DBpedia
		
	}
	
}
