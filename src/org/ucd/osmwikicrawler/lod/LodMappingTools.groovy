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

import javax.sound.midi.MidiDevice.Info;

import net.ricecode.similarity.JaroWinklerStrategy;

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
	static def _OSN_STOP_WORDS = ["yes","no","optional","any","*","ref","de","type"]
	
	static Double _SIMILARITY_TH = 0.98
	
	static def getAllOsnConcepts(){
		log.info("Getting all concepts from OSN via SPARQL...")
		String q = "select ?c { ?c a <${OntoUtils.SKOS_CONCEPT}> }"
		
		q += " LIMIT 150"
		def conceptsUris = queryRemoteSparql(q, _OSN_SPARQL_ENDPOINT, "c")
		
		log.debug conceptsUris
		assert conceptsUris.size() > 0
		return conceptsUris
	}
	
	static boolean isAStopWord( String w ){
		return w in _OSN_STOP_WORDS
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
			log.warn "error in '$query'"
			log.warn e
			return []
		} 
	}
	
	static def getOsnKey( String uri ){
		String q = "select ?k { <$uri> <$OntoUtils.SOSM_KEY_LABEL> ?k }"
		def keys = queryRemoteSparql(q, _OSN_SPARQL_ENDPOINT, "k")
		assert keys.size() == 1 || keys.size() == 0 || keys.size() == 2
		if (keys.size()>0) return keys[0]
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
	
	static String cleanStr( String s ){
		s = s.replaceAll("_"," ")
		s = s.replaceAll(":"," ")
		s = s.replaceAll("-"," ")
		return s
	}
	
	static String collapseString( String s ){
		s = s.replaceAll("_","")
		s = s.replaceAll(" ","")
		return s
	}
	
	/**
	 * 
	 * @param search
	 * @return
	 */
	static def findDbpediaOntoTerms( String search, boolean collapseStr = false ){
		if (isAStopWord(search)) return []
		if (!search || search.length() < 2) return []
		if (search.length() > 100){
			log.warn "invalid key for dbdpia search $search"
			return []
		}
		assert search
		log.info "findDbpediaOntoTerms '$search'"
		
		if (collapseStr) collapseString(search)
		search = cleanStr(search)
		boolean multiWords = search.split(" ").size() > 1
		
		String q = "select distinct ?s from <http://dbpedia.org> where { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> FILTER ( regex(?s, \"${search}\",\"i\") ) }"
		log.debug q
		def uris = queryRemoteSparql(q, _DBP_SPARQL_ENDPOINT, "s")
		
		if (!uris || uris.isEmpty()){
			if (multiWords){
				def multiUris = []
				log.debug "\thandle multiwords = $search"
				search.split(" ").each{
					def dbpTerms = findDbpediaOntoTerms( it )
					multiUris.addAll(dbpTerms)
				}
				return multiUris
			}
		}
		return uris
	}
	
	static String getLabelForDBpediaUri( String uri, String lang = "en"){
		String q = "select distinct ?l from <http://dbpedia.org> where { <$uri> <http://www.w3.org/2000/01/rdf-schema#label> ?l FILTER ( lang(?l) = \"$lang\" ) }"
		log.debug q
		def labels = queryRemoteSparql(q, _DBP_SPARQL_ENDPOINT, "l")
		if (labels.size() > 0) return labels[0]
		return null
	}
	
	static def findAllDbpediaOntoTerms( String key, def vals, boolean collapseStr ){
		
		def dbpOntoUris = findDbpediaOntoTerms( key, collapseStr)
		vals.each{
			def dbpOntoUrisV = findDbpediaOntoTerms(it, collapseStr)
			if (dbpOntoUrisV)
				dbpOntoUris.addAll(dbpOntoUrisV)
		}
		
		return dbpOntoUris
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
		
		boolean collapseStr = false
		def dbpOntoUris = findAllDbpediaOntoTerms( k, vals, collapseStr )
		collapseStr = true
		dbpOntoUris.addAll( findAllDbpediaOntoTerms( k, vals, collapseStr ) )
		
		log.debug "dbpOntoUris = $dbpOntoUris"
		log.info("TARGET: $uri")
		dbpOntoUris.unique().each{ uri2->
			assert uri2
			def dbpL = getLabelForDBpediaUri( uri2 )
			
			String osnStr = "$k ${vals.join(' ')}"
			def simAB = getStringSimilarityAsymm( osnStr, dbpL )
			def simBA = getStringSimilarityAsymm( dbpL, osnStr )
			
			if (!simAB || !simBA) return
			
			if (simAB >= _SIMILARITY_TH){
				if (simBA <= _SIMILARITY_TH){
					// detected partial match
					log.info("Found rel match: $uri === $uri2")
					addRdfStatement( uri, OntoUtils.SKOS_REL_MATCH, uri2, m)
				}
			}
			
			log.info("\t ==> " + uri2 + " simAB=" + simAB.round(2) + " simBA=" + simBA.round(2))
		}
		
		// find related resources on DBpedia
		
	}
	
	static void addRdfStatement(String sub, String pred, String obj, Model m ){
		Resource s = m.createResource( sub )
		Property p = m.createProperty( pred )
		def o = m.createResource( obj )
		Statement statement = m.createStatement( s, p, o )
		m.add( statement )
	}
	
	/**
	 * 
	 * @param base OSN uri
	 * @param target any other string (DBPedia, etc)
	 * @return
	 */
	static Double getStringSimilarityAsymm( String base, String target ){
		def wordsB = cleanStr(base).split(" ")
		//def wordsT = target.split(" ")
		
		Double maxSim = 0
		String curMax = null
		wordsB.each{wb->
			def s = getStringSimilarity( wb, target, false, true )
			log.info("\t\tgetStringSimilarityAsymm: '$wb' '$target' = ${s}")
			if (s>maxSim){
				maxSim = s
			}
			//sims.add(maxSim)
		}
		log.debug("getStringSimilarityAsymm: max=${maxSim.round(3)}")
		return maxSim
	}
	
	/**
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	static Double getStringSimilarity( String a, String b, boolean splitWords = false, boolean fuzzyMatch = true ){
		if (!a || !b) return null
		assert a
		assert b
		
		if (!fuzzyMatch){
			if (a.toLowerCase() == b.toLowerCase()) return 1.0
			else return 0.0
		}
		
		def simEngine = new JaroWinklerStrategy()
		
		if (splitWords){
			def sims = []
			def wordsA = a.split(" ")
			def wordsB = b.split(" ")
			wordsA.each{wa->
				wordsB.each{wb->
					def s = simEngine.score(wa,wb)
					sims.add(s)
				}
			}
			def mean = sims.sum()/sims.size()
			log.debug("similarity '$a' '$b' mean = $mean")
			return mean
		} else {
			def s = simEngine.score(a,b)
			log.debug("similarity '$a' '$b' = $s")
			return s
		}
	}
}
