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
package org.ucd.osmwikicrawler.ontology


import java.net.URLDecoder
import java.net.URL
import java.net.URLEncoder;

import javax.xml.ws.WebServiceException

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.ucd.osmwikicrawler.crawler.Crawler;
import org.ucd.osmwikicrawler.rdf.RdfUtils;
import org.ucd.osmwikicrawler.utils.OntoUtils;
import com.hp.hpl.jena.query.*
import com.hp.hpl.jena.rdf.model.*
import com.hp.hpl.jena.shared.*

/**
 * 
 * @author Paul, Andrea
 *
 */
class Lgd {
	
	static def log = Logger.getLogger(Lgd)
	
	static final String LGD_SPARQL_PREFIX = '''PREFIX owl: <http://www.w3.org/2002/07/owl#>
		PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
		PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
		PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
		PREFIX foaf: <http://xmlns.com/foaf/0.1/>
		PREFIX dc: <http://purl.org/dc/elements/1.1/>
		'''
	//static final String LGD_SPARQL_SERVICE = "http://lod.openlinksw.com/sparql"
	static final String LGD_SPARQL_SERVICE = "http://linkedgeodata.org/sparql"
	static final String LGD_GRAPH = "http://linkedgeodata.org/"
	static final String LGD_CLASS = "http://www.w3.org/2002/07/owl#Class"
	
	static final String LGD_TYPE = OntoUtils.RDF_TYPE
	static final String LGD_LABEL = "http://www.w3.org/2000/01/rdf-schema#label"
	static final String LGD_LANGUAGE = "en"
	static final String OWL_SAME_AS = "http://www.w3.org/2002/07/owl#sameAs"
	
    static final def STOP_NODES_IN_GRAPH = [LGD_CLASS, "http://linkedgeodata.org/ontology/Way", "http://linkedgeodata.org/ontology/Node" ]
    static final LGD_TRIPLIFY_URI_REGEX = /^http:\/\/linkedgeodata.org\/triplify\/([a-z]+)(-?[0-9]+)/

	
	/**
	 * 
	 * @param sparqlQuery
	 * @return
	 */
	static private ResultSet executeLgdSparqlSelect( String sparqlQuery ){
		//return ontoUtilsService.sparqlSelectRemote( sparqlQuery, LGD_SPARQL_SERVICE )
		assert sparqlQuery
		return OntoUtils.sparqlSelectRemote( sparqlQuery, LGD_SPARQL_SERVICE )
	}

	/**
	 * 
	 * @return all LGD ontology terms
	 */
	static Set getAllLgdOntologicalTerms(){
		String q = "select ?sub { ?sub <${OntoUtils.RDF_TYPE}> <${OntoUtils.OWL_CLASS}> FILTER ( " +
			"regex( str(?sub), \"^${OntoUtils.NAMESPACES['lgdo']}\" ) ) } LIMIT 50000"
		ResultSet rs = executeLgdSparqlSelect( q )
		def terms = OntoUtils.getValuesFromJenaResultSet( rs, "sub", q ) as Set
		assert terms.size() > 0
		def filteredTerms = terms //.findAll{ t->
		//	/*t.matches(/.*\+.* /) == false && */
		//	!(isPseudoLgdUri(t))
		//}
		assert filteredTerms.size() > 0
		return filteredTerms
	}
	
	/**
	 * 
	 * @return
	 */
	String getLgdClassSummaryCsv(){
		
		def ontoTerms = getAllLgdOntologicalTerms()

		
		final String HEADER  = "URI,LABEL,PARENTS,NODE COUNT,WAY COUNT,TOTAL COUNT"
		String csv = "LGD DATASET - ONTOLOGY SUMMARY\n\n${HEADER}\n\n"
		
		//ontoTerms = [ "http://linkedgeodata.org/ontology/Aeroway" ]//,
		 //           "http://linkedgeodata.org/ontology/Restaurant",
		  //          "http://linkedgeodata.org/ontology/Graveyard" ]
		
		ontoTerms.sort{it}.each{ uri->
			// prepare data
			assert uri
			String label = getLgdLabel( uri )
			def parentClasses = getParentClasses(uri)
			String parentClassStr = ''
			if (parentClasses.size()>0){
				parentClassStr = parentClasses.join(" ")
				parentClassStr = parentClassStr.trim()
			} else {
				parentClassStr = "(NO)"
			}
			long nodeCount = countNodesForClass(uri)
			long wayCount = countWaysForClass(uri)
			long totalCount = countAllForClass(uri)
		 	
			// build csv line
			String csvLine = "${uri},${label},${parentClassStr},${nodeCount},${wayCount},${totalCount}"
			
			csv += "${csvLine}\n"
		}
		csv = csv.replaceAll(OntoUtilsService.NAMESPACES['lgdo'],"lgdo:")
		return csv
	}
	
	/**
	 * 
	 * @return
	 */
	static def getParentClasses( String uri ){
		String q = "select ?sup { <${uri}> <${OntoUtils.PRED_SUBCLASS}> ?sup }" +
				" LIMIT 50000"
		def rs = OntoUtils.sparqlSelectRemote( q, LGD_SPARQL_SERVICE )
		def labels = OntoUtils.getValuesFromJenaResultSet( rs, "sup", q )
		return labels
	}
	
	/**
	 * 
	 * @param uri
	 * @return
	 */
	long countNodesForClass( String uri ){
		String q = "select (count(?s) as ?c) { ?s <${OntoUtilsService.RDF_TYPE}> <${uri}> . "+
											" ?s <${OntoUtilsService.RDF_TYPE}> <http://linkedgeodata.org/ontology/Node> }"
		def rs = rdfService.executeSparqlSelectOnTdb( q, Repository.LGD )
		def counts = ontoUtilsService.getValuesFromJenaResultSet( rs, "c", q )
		assert counts.size() == 1
		long c = new Long( counts[0] )
		assert c >= 0
		return c
	}
	
	/**
	 * 
	 * @param uri
	 * @return
	 */
	long countAllForClass(  String uri  ){
		String q = "select (count(?s) as ?c) { ?s <${OntoUtilsService.RDF_TYPE}> <${uri}> . }"
		def rs = rdfService.executeSparqlSelectOnTdb( q, Repository.LGD )
		def counts = ontoUtilsService.getValuesFromJenaResultSet( rs, "c", q )
		assert counts.size() == 1
		long c = new Long( counts[0] )
		assert c >= 0
		return c
	}
	
	/**
	 * 
	 * @param uri
	 * @return
	 */
	long countWaysForClass( String uri ){
		String q = "select (count(?s) as ?c) { ?s <${OntoUtilsService.RDF_TYPE}> <${uri}> . "+
		" ?s <${OntoUtilsService.RDF_TYPE}> <http://linkedgeodata.org/ontology/Way> }"
		def rs = rdfService.executeSparqlSelectOnTdb( q, Repository.LGD )
		def counts = ontoUtilsService.getValuesFromJenaResultSet( rs, "c", q )
		assert counts.size() == 1
		long c = new Long( counts[0] )
		assert c >= 0
		return c
	}
	
	/**
	 * 
	 * @param text
	 * @return
	 */
	boolean isPseudoLgdUri( String text ){
		if (!text) return false
		String name = ontoUtilsService.getNameFromUri( text )
		if (name.length() < 3){
			return true
		}
		// 75d664d0:1300863666e:-7ff4
		if (name.toLowerCase().matches( /.*[a-z0-9]+:[a-z0-9]+:.*/ )){
			return true
		}
		// numbers
		if (name.toLowerCase().matches( /[0-9]+[A-Z]?/ )){
			return true
		}
		if (name.matches( /^<.+>$/ )){
			return true
		}
		
		name = name.toLowerCase()
		
		// ad hoc filters
		if (name =~ /11c.*11e/ ) return true
		if (name == "20b" ) return true
		if (name == "11c" ) return true
		if (name == "other") return true
		//log.debug("detectPseudoUri: false for text=${text}")
		return false
	}
	
	/**
	 * 
	 * @param uri
	 * @return
	 */
	String getModeFromTriplifyUri( String uri ){
		assert ontoUtilsService.isUriLgdFeature( uri )
		assert uri
		assert (uri =~ LGD_TRIPLIFY_URI_REGEX)
		def matcher = (uri =~ LGD_TRIPLIFY_URI_REGEX)
		assert matcher.size()>0,"matcher: ${matcher}"
		assert matcher[0].size() == 3
		assert matcher[0][1] in OsmService.OSM_FEAT_MODES,"getModeFromTriplifyUri: failed for <${uri}>: ${matcher[0][1]}"
		return matcher[0][1].trim()
	}
	
	/**
	 * 
	 * @param uri
	 * @return
	 */
	Long getOsmIdFromTriplifyUri( String uri ){
		assert ontoUtilsService.isUriLgdFeature( uri )
		assert uri
		assert (uri =~ LGD_TRIPLIFY_URI_REGEX)
		def matcher = (uri =~ LGD_TRIPLIFY_URI_REGEX)
		assert matcher.size()>0,"matcher: ${matcher}"
		assert matcher[0].size() == 3
		assert matcher[0][2] != null
		return new Long( matcher[0][2] )
	}
	
	/**
	 * 
	 * @param triplifyUri
	 * @return
	 */
	boolean isOsmFeatureInLgd( long osmId, String mode ){
		assert osmId != null
		String triplifyUri = getLgdFeatureUri(osmId, mode)
		String sparql = "ASK { <${triplifyUri}> ?p ?o . }"
		boolean found = askLgd( sparql )
		log.debug("isOsmFeatureInLgd: osmId=${osmId} mode=${mode} found=${found}")
		return found
	}
	
	/**
	 * 
	 * @param any uri
	 * @return
	 */
	boolean isUriInLgd( String uri ){
		assert uri
		String q = "ASK { <${uri}> ?p ?o .}"
		return askLgd( q )
	}
	
	/**
	 * 
	 * @param osmId
	 */
	void importLdgDataForOsmFeature( long osmId ){
		assert osmId != null
		
		assert false
	}
	
	/**
	 * 
	 * @param uri
	 * @return
	 */
	String getLgdLabel( String uri ){
		assert uri
		String query = " SELECT ?label { <${uri}> <${OntoUtilsService.LABEL_PRED}> ?label FILTER (lang(?label) = \"en\") . }"
		
		def res = executeLgdSparqlSelect( query )
		def labels = ontoUtilsService.getValuesFromJenaResultSet( res, "label", query )
		assert labels.size()==0 || labels.size()==1
		
		String label = labels.join(' ').trim()
		return label
	}	
	
	/**
	 * 
	 * @param askQuery ASK sparql query on LinkedGeoData
	 * @return true or false
	 */
	static public boolean askLgd( String askQuery ){
		assert askQuery
		boolean exists = false
		try {
			exists = OntoUtils.sparqlAskRemote( askQuery, LGD_SPARQL_SERVICE, true, true )
		} catch( e ){
			log.warn("askLgd: failed on " + e + "\n\t trying again with different settings...")
			exists = OntoUtils.sparqlAskRemote( askQuery, LGD_SPARQL_SERVICE, true, false )
		}
		assert exists != null
		return exists
	}
	
	/**
	 * 
	 * @param s subjectUri
	 * @param p predicateUri
	 * @param o objectUri
	 * @return array of Uris
	 */
	def queryLgdGraph( String s, String p, String o ){
		assert p
		assert (s && p && !o) || (!s && p && o)
		
		String sparql = null
		if ((s && p && !o)){
			sparql = "SELECT * { <${s}> <${p}>  ?target . }"
		}
		if ((!s && p && o)){
			sparql = "SELECT * { ?target <${p}> <${o}> . }"
		}
		assert sparql
		
		ResultSet res = executeLgdSparqlSelect( sparql )
		def results = []
		
		if (res){
			for ( ; res.hasNext() ; ) {
				QuerySolution soln = res.next()
				assert soln
				String object = soln.get("target")
				if (object){
					if (!(object in results)){
						results.add( object )
					}
				}
			}
		}
		//log.debug("queryLgdGraph: '${sparql}'\n\t-> ${results}")
		return results
	}
	
	/**
	 * Get all neighbouring nodes starting with filter 
	 * @param uri
	 * @return Rdf Model
	 */
	private Model getLgdNodeBySubjectAndPredicate( String subject, String predicate, Model m ){
		assert subject
		assert predicate
		assert m
		String query = "select * where { <${subject}> <${predicate}> ?obj } LIMIT 50000"
		ResultSetRewindable res = executeLgdSparqlSelect( query )
		res.each{ r->
			String obj = r.get( "obj" ).toString()
			assert obj
			m = rdfService.addRdfStatementToModel( subject, predicate, obj, m )
		}
		return m
	}
	
	/**
	 * 
	 * @param keyword
	 * @return suitableKeyword for Ldg Ontology
	 */
	static private String formatLdgOntologyKeyword( String keyword ){
		//log.debug("formatLdgOntologyKeyword1: "+keyword)
		assert keyword
		String kw = keyword?.trim()?.replace("_"," ")?.trim()
		assert kw
		kw = URLDecoder.decode( kw )
		def array = kw.split(" ")
		kw = ''
		array.each{
			if (it){
				assert it,"formatLdgOntologyKeyword: entry null for kw='${keyword}'"
				kw += StringUtils.capitalize( it.toLowerCase() )
			}
		}
		kw = kw?.trim()
		kw = kw?.replace(" ","")
		kw = URLEncoder.encode( kw )
		//log.debug("formatLdgOntologyKeyword2: "+kw)
		return kw
	}
	
	/**
	 * 
	 * @param osmId
	 * @return e.g. http://linkedgeodata.org/triplify/node83211617
	 */
	private String getLgdFeatureUri( Long osmId, String mode ){
		assert osmId != null
		assert mode in OsmService.OSM_FEAT_MODES
		return "http://linkedgeodata.org/triplify/${mode}${osmId}"
	}

	
	/**
	 * Used on DBpedia direct mapping
	 * @param osmId
	 * @return e.g. http://linkedgeodata.org/triplify/node/83211617#id
	 */
	String formatLgdUriForDbp( Long osmId, String mode ){
		assert osmId != null
		assert mode in OsmService.OSM_FEAT_MODES
		return "http://linkedgeodata.org/triplify/${mode}/${osmId}#id"
	}
	
	/**
	 * Used on DBpedia direct mapping
	 * @param osmId
	 * @return e.g. http://linkedgeodata.org/triplify/node/83211617#id
	 */
	String formatLgdUriForDbp( String triplifyUri ){
		assert triplifyUri
		long osmId = getOsmIdFromTriplifyUri( triplifyUri )
		assert osmId != null
		String mode = getModeFromTriplifyUri( triplifyUri )
		assert mode
		return formatLgdUriForDbp(osmId, mode)
	}
	
	/**
	 * 
	 * @param term
	 * @return
	 */
	private String getOntologyUri( String term ){
		assert term
		assert !(term =~ " ")
		String uri = "http://linkedgeodata.org/ontology/${term}"
		return uri
	}
	
	/**
	 * 
	 * @param kw keyword
	 * @return remapped keyword
	 */
	private String remapKeywordWithLgdOntology( String kw ){
		String ontoKw = formatLdgOntologyKeyword( kw )
		assert ontoKw
		String ontoUri = getOntologyUri( ontoKw )
		String sparql = "ASK { <${ontoUri}> <${LGD_TYPE}> <${LGD_CLASS}> . }" 

		boolean exists = askLgd( sparql )
		if (!exists){
			// don't do anything
			return kw
		}
		// find label
		
		sparql = "SELECT ?label { <${ontoUri}> <${LGD_LABEL}> ?label . FILTER( lang(?label) = \"${LGD_LANGUAGE}\") }"
		assert sparql
		ResultSet res = executeLgdSparqlSelect( sparql )
		def results = ontoUtilsService.getValuesFromJenaResultSet( res, "label" )
		assert results.size() == 0 || results.size() == 1
		if (results.size()>0){
			ontoKw = results[0]
			log.debug("remapKeywordWithLgdOntology: label for '${kw}' found: '${ontoKw}'")
		} else {
			// no label found, re-separate words
			ontoKw = kw
		}
		ontoKw = ontoKw.replace(" ","_")
		return ontoKw
	}
	
	
	/**
	 * 
	 * @param ontology
	 * @param algo
	 * @return
	 */
	static OsmOntology matchOsmOntoTermsWithLgd( OsmOntology ontology ){
		assert ontology
		assert ontology.terms
		
		ontology.lgdClassCount = getAllLgdOntologicalTerms().size()
		ontology.osmKeyCount = -1
		ontology.osmValueCount = -1
		int nTerms = ontology.terms.findAll{ !it.bFailedToBuild }.size()
		int i = 0
		int termsFound = 0
		ontology.terms.findAll{ !it.bFailedToBuild }.each{ term->
			i++
			int pc = new Float(i)/nTerms * 100
			if ( !Crawler.isRedirectionTerm(term) ){
				log.debug("getLgdClassFromKeyValue: $term")
				log.info("Mapping OSM term on LinkedGeoData [$i/$nTerms] ${pc}% ...")
				String lgdUri = getLgdClassFromKeyValue( term?.key, term?.value )
				if (lgdUri){
					termsFound++
					term.lgdUri = lgdUri
				}
			}
		}
		//ontology = addInfoFromCsvFile( ontology )
		log.info( "LinkedGeoData mapping complete. $termsFound term(s) found." )
		return ontology
	}
	
	/**
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	static private String getLgdClassFromKeyValue( String key, String value, boolean checkParents = true ){
		//assert key
		//assert value
		
		if (!key && !value) return null
		
		if (!key) key = ''
		if (!value) value = ''
		String uri = ''
		String ontoName1 = formatLdgOntologyKeyword( value )
		String ontoName2 = ''
		boolean isKey = key == value
		if (!isKey){
			ontoName2 = formatLdgOntologyKeyword( key ) + formatLdgOntologyKeyword( value )
		}
		
		[ontoName1,ontoName2].each{ ontoName->
			if (ontoName){
				String uriCandidate = LGD_GRAPH + "ontology/" + ontoName
				String query = "ASK { <${uriCandidate}> <${LGD_TYPE}> <${LGD_CLASS}> }"
				boolean found = askLgd( query )
				if (found){
					boolean parentMatch = true
					if (!isKey){
						if (checkParents){
							// find parents
							def parents = getParentClasses( uriCandidate )
							def pp = []
							parents.each{p->
								String name = OntoUtils.getNameFromUri( p )
								name = name.toLowerCase()
								log.debug("getLgdClassFromKeyValue: "+name)
								pp.add(name)
							}
							parentMatch = pp.contains( key )
						}
					}
					// exit after first one found
					if (uriCandidate.length() > uri.length() && parentMatch){
						uri = uriCandidate
					}
				}
			}
		}
		return uri
	}
	
	
	/**
	 * 
	 * @param ontology
	 * @return
	 */
	private OsmOntology addInfoFromCsvFile( OsmOntology ontology ){
		assert ontology
		// build map from CSV		
		String outfile = utilsService.getOntoDataFolder() + "lgd_classes_summary.csv"
		String content = utilsService.readFile(outfile)
		assert content
		def lines = content.split("\n").findAll{
			it
		}
		Map info = [:]
		lines.each{ line->
			def elems = line.split(",")*.trim()
			assert elems.size() == 6
			String uri = elems[0].replace("lgdo:","http://linkedgeodata.org/ontology/")
			info[ uri ] = elems
		}
		//log.debug("addInfoFromCsvFile: "+info)
		ontology.terms.each{ term->
			if (term.lgdUri){
				assert info[ term.lgdUri ] != null,term.lgdUri
				term.lgdTotalCount = new Long( info[ term.lgdUri ][5] )
				term.lgdWayCount = new Long( info[ term.lgdUri ][4] )
				term.lgdNodeCount = new Long( info[ term.lgdUri ][3] )
			}
		}
		
		return ontology
	}
	
	/**
	 * 
	 * @param ontology
	 * @return
	 */
	Set getUnmappedLgdClasses( OsmOntology ontology ){
		Set classes = getAllLgdOntologicalTerms()
		
		ontology.terms.each{
			if (it.lgdUri) classes.remove( it.lgdUri ) 
		}
		return classes
	}
	
	/**
	 * 
	 * @param ontology
	 * @param unmappedClasses
	 * @return
	 */
	OsmOntology mapUnmappedLgdClasses( OsmOntology ontology, Set unmappedClasses ){
		assert ontology
		assert unmappedClasses != null
		def mapped = []
		def pairs = []
		
		int debug = 0
		unmappedClasses.each{ cl->
			debug++
			if (debug > 4) return
			if (mapped.contains(cl)) return
			double maxSim = -1
			OsmOntoTerm candidateTerm = null
			assert cl
			ontology.terms.each{ t->
				double sim = similarityService.getSimilarityOsmTermAndLgdClass( t, cl )
				if (sim > maxSim ){
					candidateTerm = t
					maxSim = sim
				}
			}
			mapped.add(cl)
			pairs.add( cl, maxSim, candidateTerm )
		}
		return ontology
	}
}
