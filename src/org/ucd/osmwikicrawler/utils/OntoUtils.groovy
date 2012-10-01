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
package org.ucd.osmwikicrawler.utils

import java.awt.Color
import java.io.*
import java.util.SortedSet
import com.hp.hpl.jena.graph.impl.*
import com.hp.hpl.jena.query.*
import com.hp.hpl.jena.rdf.model.*
import com.hp.hpl.jena.shared.*
import com.hp.hpl.jena.sparql.resultset.ResultSetException;
import javax.xml.ws.*
import org.apache.log4j.Logger;
import org.ucd.osmwikicrawler.exceptions.RemoteServiceException;

/**
 * Utils for semantic stuff
 * 
 * @author Andrea Ballatore
 *
 */
class OntoUtils {
	
	static def log = Logger.getLogger(OntoUtils.class)
	
	public enum FileFormat{XML, CSV, HTML}
	
	/** NAMESPACES */
	static final String SEMOSM_ROOT_URL = "http://wiki.openstreetmap.org/"
	static final String OUR_LINKED_DATA_SERVER = "http://spatial.ucd.ie"
	static final String OSN_ROOT_NAME = "OSMSemanticNetwork"
	
	static final def NAMESPACES = [
	    skos:"http://www.w3.org/2004/02/skos/core#",
	    owl:"http://www.w3.org/2002/07/owl#",
	    dbp:"http://dbpedia.org/",
	    dbpo:"http://dbpedia.org/ontology/",
	    dbpr:"http://dbpedia.org/resource/",
	    dbpy:"http://dbpedia.org/class/yago/",
	    dbpc:"http://dbpedia.org/resource/Category:",
	    dbpt:"http://dbpedia.org/resource/Template",
	    dbpp:"http://dbpedia.org/property/",
	    lgd:"http://linkedgeodata.org/",
	    lgdo:"http://linkedgeodata.org/ontology/",
	    lgdtw:"http://linkedgeodata.org/triplify/way",
	    lgdtn:"http://linkedgeodata.org/triplify/node",
	    lgdt:"http://linkedgeodata.org/triplify/",
	    geo:"http://www.w3.org/2003/01/geo/wgs84_pos#",
	    rdfsy:"http://www.w3.org/1999/02/22-rdf-syntax-ns#",
		rdfsc:"http://www.w3.org/2000/01/rdf-schema#",
		dct:"http://purl.org/dc/terms/",
		osn:"$OUR_LINKED_DATA_SERVER/$OSN_ROOT_NAME/",
		osnp:"$OUR_LINKED_DATA_SERVER/$OSN_ROOT_NAME/property/",
		osnt:"$OUR_LINKED_DATA_SERVER/$OSN_ROOT_NAME/term/",
		osnpt:"$OUR_LINKED_DATA_SERVER/$OSN_ROOT_NAME/proposed_term/",
		sosm: SEMOSM_ROOT_URL,
		sosmv: SEMOSM_ROOT_URL + "wiki/OSMSemanticNetwork#",
		sosmt: SEMOSM_ROOT_URL+"terms/",
		sosmf: SEMOSM_ROOT_URL+"osm_feat/",
		sosmk: SEMOSM_ROOT_URL+"keyword/",
		dbplookup: SEMOSM_ROOT_URL+"dbp_lookup/",
		purl:"http://purl.org/goodrelations/",
		geonm:"http://sws.geonames.org/",
		opgs:"http://www.opengis.net/",
		foaf:"http://xmlns.com/foaf/0.1/",
		osmw:"http://wiki.openstreetmap.org/wiki/"
	]

	/* RDF predicates used to expand semantics */
	
	/** Semantic OSM ontological predicates */
	static final String OSN_RESOURCE = NAMESPACES['osn']
	static final String MANUALLY_MAPPED = NAMESPACES['sosmt'] + "manualMapping"
	static final String SOSM_WIKIPEDIA_LINK = NAMESPACES['osnp'] + "wikipediaLink"
	static final String SOSM_INTERNAL_LINK = NAMESPACES['osnp'] + "link"
	static final String SOSM_KEY = NAMESPACES['osnp'] + "key"
	static final String SOSM_KEY_LABEL = NAMESPACES['osnp'] + "keyLabel"
	static final String SOSM_VALUE = NAMESPACES['osnp'] + "value"
	static final String SOSM_VALUE_LABEL = NAMESPACES['osnp'] + "valueLabel"
	static final String SOSM_PHOTO = NAMESPACES['osnp'] + "image"
	static final String SOSM_REDIRECT = NAMESPACES['osnp'] + "redirect"
	static final String SOSM_IMPLIES = NAMESPACES['osnp'] + "implies"
	static final String SOSM_APPLIES_TO = NAMESPACES['osnp'] + "appliesTo"
	static final String SOSM_COMBINATION = NAMESPACES['osnp'] + "combinedWith"
	static final String SOSM_TAGINFO = NAMESPACES['osnp'] + "tagInfo"
	static final String LGD_MAPPED = NAMESPACES['osnp'] + "lgdMapping"
	
	// obsolete
	static final String SEMOSM_SCORE_URI = NAMESPACES['sosm'] + "score#"
	static final String GRAPH_ROOT_NODE = NAMESPACES['sosm'] + "ROOT"
	static final String SEMOSM_HAS_KEYWORD = NAMESPACES['sosmt'] + "hasKeyword"
	static final String DBP_LOOKUP_MAPPED = NAMESPACES['sosmt'] + "dbpLookup"
	static final String KEYWORD_MAPPING_PREFIX = NAMESPACES['sosmk']
	static final String KEYWORD_MAPPING_IGNORE = KEYWORD_MAPPING_PREFIX + "ignore"

	
	/** Other ontological terms used in the system */
	static final String RDF_TYPE = NAMESPACES['rdfsy'] + "type"
	static final String OWL_CLASS = NAMESPACES['owl'] + "Class"
	static final String PRED_SUBCLASS = NAMESPACES['rdfsc'] + "subClassOf"
	static final String PRED_ONTO_TYPE = NAMESPACES['rdfsy'] + "type"
	static final String PRED_SUBJECT = NAMESPACES['dct'] + "subject"
	static final String DC_SOURCE = "http://purl.org/dc/elements/1.1/source"
	static final String PRED_SAME_AS = NAMESPACES['owl'] + "sameAs"
	static final String PRED_EQUIVALENT_CLASS = NAMESPACES['owl'] + "equivalentClass"
	static final String DBPO_ABSTRACT = NAMESPACES['dbpo'] + "abstract"
	static final String RDF_LABEL = NAMESPACES['rdfsc'] + "label"
	static final String COMMENT_PRED = NAMESPACES['rdfsc']+"comment"
	static final String RDF_SUBPROPERTYOF = NAMESPACES['rdfsc']+"subPropertyOf"
	static final String RDF_DEFINEDBY =  NAMESPACES['rdfsc']+"isDefinedBy"
	
	static final String GEOLOC_PRED = NAMESPACES['geo']+"geometry"
	static final String DBP_ONTO_PLACE = NAMESPACES['dbpo']+"Place"
	static final String DBP_ONTO_DISAMBIGUATE = NAMESPACES['dbpo']+"wikiPageDisambiguates"
	
	/** SKOS vocabulary */
	
	static final String SKOS_SCHEMA_NAME = NAMESPACES['osn'] + "Scheme"
	static final String SKOS_BROADER = NAMESPACES['skos'] + "broader"
	static final String SKOS_CONCEPT_SCHEME = NAMESPACES['skos'] + "ConceptScheme"
	static final String SKOS_TOP_CONCEPT = NAMESPACES['osnt'] + "rootConcept"
	static final String SKOS_EXACT_MATCH = NAMESPACES['skos'] + "exactMatch"
	static final String SKOS_REL_MATCH = NAMESPACES['skos'] + "relatedMatch"
	static final String SKOS_INSCHEME = NAMESPACES['skos'] + "inScheme"
	static final String SKOS_NARROWER = NAMESPACES['skos'] + "narrower"
	static final String SKOS_DEFINITION = NAMESPACES['skos'] + "definition"
	static final String SKOS_RELATED = NAMESPACES['skos'] + "related"
	static final String SKOS_CONCEPT = NAMESPACES['skos'] + "Concept"
	static final String SKOS_PREFLABEL = NAMESPACES['skos'] + "prefLabel"
	static final String SKOS_ALTLABEL = NAMESPACES['skos'] + "altLabel"
	static final String SKOS_SEMREL = NAMESPACES['skos'] + "semanticRelation"
	
	static final def VECTOR_MERGE_MODES = ["skip","sum","max"]

	static final Integer SPARQL_MAX_RETRIALS = 3
	static final Integer SPARQL_RETRIAL_SLEEP_MS = 100
	
	static final String MAPPING_TABLE_HEADER = '''
				<th title='Order of retrieval from DBPedia lookup'>u#</th>
				<th title='URI pointing to the DBPedia node and lookup keyword'>Keyword/DBP Node</th>
				<th title='abstract of the DBPedia node describing what it is'>DBP Description</th>
				<th title='Distance score'>DiSc</th>
				<th title='Keyword matching score'>KwSc</th>
				<th title='Ontological matching score based on DBPedia ontology'>OntoSc</th>
				<th title='Overall score (mean of the other scores)'>OverSc</th>
				<th title='Node selected as valid mapping or not'>Valid</th>
	
	'''

	static final Map DBP_MAPPING_FEEDBACK_OPTIONS = [
	   0:"Unset",
	   1:"Wrong",
	   2:"Poor",
	   3:"OK",
	   4:"Good",
	   5:"Excellent"
	]
	
	static final Map DBP_MAPPING_FEEDBACK_OPTIONS_COLORS = [
  	   0:"#AAA",
  	   1:"white",
  	   2:"white",
  	   3:"white",
  	   4:"white",
  	   5:"white"
	]
	
	/**
	 * 
	 * @param res
	 * @param field
	 * @return
	 */
	static def getValuesFromJenaResultSet( ResultSetRewindable res, String field, String debugQuery = null ){
		assert res != null
		assert field
		res.reset()
		def results = []
		try{
			//res.each{ soln->
			for ( ; res.hasNext() ;  ){
				QuerySolution soln = res.next()
				assert soln
				def f = soln.get( field )
				if (f){
					String result = null
					if (f instanceof Resource) result = f.toString()
					if (f instanceof Literal) result = f.getString()
					assert result
					results.add( result )
				}
			}
		} catch( ResultSetException e ){
			String msg = "getValuesFromJenaResultSet: failed to read SPARQL results. res=${results} query=\n'${debugQuery}'\n ${e}"
			log.error(msg)
			throw new Exception(msg)
		}
		return results
    }
	
	/**
	 * 
	 * @param keyword
	 * @return URI
	 */
	String getKeywordMappingURI( String keyword ){
		assert keyword
		String uri = KEYWORD_MAPPING_PREFIX + keyword.toLowerCase().replace(" ", "_")
		return uri
	}
	                                    
	/**
	 * 
	 * @param uri
	 * @return
	 */
	static String getNameFromUri( String uri, boolean validate = true ){
		assert uri
		String type = getNamespaceFromUri(uri)
		if (validate){
			assert type,"getNameFromUri: type null for uri=${uri}"
		}
		if (type){
			String name = uri.replace(NAMESPACES[type],"")
			name = URLDecoder.decode( name )
			return name
		}
		//assert name,"getNameFromUri: null name for <${uri}>"
		return uri
	}
	
	/**
	 * 
	 * @param uri
	 * @return name with prefix
	 */
	String getNameFromUriWithNS( String uri ){
		assert uri
		String name = getNameFromUri( uri )
		if (!name){
			return getNamespaceFromUri( uri ) 
		}
		name = URLDecoder.decode( name )
		name = getNamespaceFromUri( uri ) + ":" + name
		assert name
		return name
	}
	
	/**
	 * 
	 * @param uri
	 * @return namespace short code or null
	 */
    static String getNamespaceFromUri( String uri, boolean validate = false ) {
		assert uri
		//assert uri.length() > 0
		def matches = []
		NAMESPACES.each{ abbr,ns->
			if(uri =~ ns){
				matches.add( ns )
			}
		}
		String bestMatch = ''
		matches.each{ ns->
			if (ns.length() > bestMatch.length()){
				bestMatch = ns
			}
		}
		if (!bestMatch){
			if ( validate ){
				// unhandled URIs
				String msg = "getNamespaceFromUri: URI type unknown: '${uri}'"
				log.warn(msg)
				throw new IllegalArgumentException(msg)
			} else {
				// return empty type
				return null
			}
		}
		
		String type = ''
		NAMESPACES.each{ abbr,ns->
			if(bestMatch == ns){
				type = abbr
				return
			}
		}
		assert isNamespaceValid( type )
		return type
    }

	/**
	 * 
	 * @param type
	 */
	static boolean isNamespaceValid( String ns ){
		assert ns
		boolean found = false
		NAMESPACES.each{ k,v ->
			if ( ns == k ) found = true
		}
		return found
	}
	
	/**
	 * 
	 * @param uri
	 * @return RDF link
	 */
	String getRdfUriFromUri( String uri ){
		assert false
		/*
		assert uri
		String type = getNamespaceFromUri( uri )
		String name = getNameFromUri( uri )
		assert name
		
		String pageName = name.replace(' ','_').encodeAsURL()
		
		String rdfUrl = ''
		if (type == "owl" || type =~ "rdf"){
			// no need to redefine URI
			rdfUrl = uri
		} else if (type == "dbpo"){
			// DBPEDIA ontology
			rdfUrl = "http://dbpedia.org/data3/${pageName}.rdf"
		} else if (type == "dbp" || type == "dbpr"){
			// all other DBPEDIA node types
			rdfUrl = "http://dbpedia.org/data/${pageName}.rdf"
		} else if (type == null){
			rdfUrl = uri
		}
		assert rdfUrl, "rdfUrl null for uri=${uri}"
		assert !(rdfUrl =~ "%20")
		assert !(rdfUrl =~ " ")
		//log.debug("getRdfUriFromUri: uri=${uri}, rdfUrl=${rdfUrl}")
		return rdfUrl	
		*/
	}
	
	/**
	 * 
	 * @param rawKeyword
	 * @return formatted keyword
	 */
	String formatKeyword( String rawKeyword ){
		assert rawKeyword
		String cleanKeyword = rawKeyword.trim() //.replace(" ","_")
		cleanKeyword = cleanKeyword.replaceAll("\\s","%20")
		cleanKeyword = cleanKeyword.replaceAll("_","%20")
		return cleanKeyword
	}
	
	/**
	 * 
	 * @param sub
	 * @param pred
	 * @param obj
	 * @return a Jena Statement
	 */
	Statement createStatement( String sub, String pred, String obj ){
		assert sub
		assert pred
		assert obj
		
		log.debug("createStatement: <${sub}> <${pred}> <${obj}>")
		
		def nsub = createJenaProperty( sub )
		def npred = createJenaProperty( pred )
		def nobj = createJenaProperty( obj )

		if (!nsub || nsub instanceof com.hp.hpl.jena.rdf.model.Literal || !npred || !nobj){
			 log.warn "createStatement: skipping <${sub}> <${pred}> <${obj}>"
			 return null
		}
		
		Statement s = ResourceFactory.createStatement( nsub, npred, nobj )
		assert s
		return s
	}

	/**
	 * 
	 * @param uri
	 * @return whether the uri is any DBpedia node
	 */
	boolean isUriDbp( String uri ){
		return isUriDbpr( uri ) || isUriDbpo( uri ) || isUriDbpc( uri )
	}
	
	/**
	 * 
	 * @param uri
	 * @return whether the uri is any DBpedia node
	 */
	boolean isUriSosm( String uri ){
		assert uri
		String type = getNamespaceFromUri( uri )
		return isUriSosmk(uri) || type == "sosm" || type == "sosmt" || type == "sosmf" 
	}
	
	/**
	*
	* @param uri
	* @return whether the uri is any DBpedia node
	*/
   boolean isUriSosmk( String uri ){
	   assert uri
	   String type = getNamespaceFromUri( uri )
	   return type == "sosmk"
   }
	
	/**
	 * 
	 * @param uri
	 * @return whether the uri is a DBPedia category (concept)
	 */
	boolean isUriDbpc( String uri ){
		assert uri
		String type = getNamespaceFromUri( uri )
		return type == "dbpc" || type == "dbpy"
	}
	
	/**
	 * 
	 * @param uri
	 * @return whether the uri is a YAGO concept
	 */
	boolean isUriLgd( String uri ){
		assert uri
		return isUriLgdt( uri ) || isUriLgdo( uri ) || isUriLgdFeature( uri )  
	}
	
	/**
	 * 
	 * @param uri
	 * @return whether the uri is a lgd triplify link
	 */
	boolean isUriLgdt( String uri ){
		assert uri
		String type = getNamespaceFromUri( uri )
		return type == "lgdt"
	}
	
	/**
	 * 
	 * @param uri
	 * @return whether the uri is a lgd triplify link
	 */
	boolean isUriLgdFeature( String uri ){
		assert uri
		String type = getNamespaceFromUri( uri )
		return type == "lgdtw" || type == "lgdtn"
	}
	
	boolean isUriLgdo( String uri ){
		assert uri
		String type = getNamespaceFromUri( uri )
		return type == "lgdo"
	}
	
	/**
	 * 
	 * @param uri
	 * @return whether the uri is a DBPedia resource
	 */
	boolean isUriDbpr( String uri ){
		assert uri
		String type = getNamespaceFromUri( uri )
		return type == "dbpr"
	}
	
	/**
	 * 
	 * @param uri
	 * @return whether the uri is a DBPedia ontology term
	 */
	boolean isUriDbpo( String uri ){
		assert uri
		String type = getNamespaceFromUri( uri )
		return type == "dbpo"
	}
	
	/**
	 * 
	 * @param uri
	 * @return
	 */
	String getNodeShortLinkHtml( String uri ){
		if (uri){
			if (uri=~/^http:\/\/.*/){
				// it's a URI
				String link = getNameFromUriWithNS(uri)
				link = utilsService.cutString( link, 37 )
				link = link.encodeAsHTML()
				String html = "<a target='blank' href='${uri}' title='${uri}'>${link}</a>"
				return html
			} else {
				// not a uri, return simple text
				return uri
			}
		} else {
			return "Not set"
		}
	}

	
	/**
	 * 
	 * @param text
	 * @return
	 */
	def createJenaProperty( String text ){
		def nobj = null
		if ( utilsService.isUrl( text ) ){
			try {
				nobj = ResourceFactory.createProperty( text )
			} catch(InvalidPropertyURIException e){
				log.warn( "createJenaProperty: skipping $text ..." + e )
				return null
			}
		} else {
			nobj = ResourceFactory.createPlainLiteral( text )
		}
		assert nobj
		return nobj
	}
	
	/**
	 * 
	 * @param uri
	 * @return
	 */
	Color getColorForUri( String uri ){
		assert uri
		def color = Color.GRAY
		if ( isUriDbp( uri ) ){
			color = Color.GREEN
			color = color.darker()
			color = color.darker()
			color = color.darker()
			
			if (isUriDbpc(uri)){
				color = color.brighter()
			}
			if (isUriDbpr(uri)){
				
			}
			if (isUriDbpo(uri)){
				color = color.brighter()
				color = color.brighter()
			}
		}
		if (isUriLgd(uri)){
			color = Color.RED
			color = color.darker()
			//color = color.darker()
			if (isUriLgdt(uri)){
				
			}
			if (isUriLgdo(uri)){
				color = color.brighter()
			}
		}
		if (isUriSosm(uri)){
			color = Color.BLUE
			color = color.darker()
			//color = color.darker()
			if (isUriSosmk(uri)){
				color = color.brighter()
			}
		}
		return color
	}
	
	/**
	 * 
	 * @param askSparql
	 * @param endpoint
	 * @return
	 */
	static public Boolean sparqlAskRemote( String askSparql, String endpoint, Boolean useCache = true, Boolean useDirectUrl = false ){
		assert askSparql
		assert endpoint
		assert askSparql.toLowerCase() =~ "ask","sparqlAskRemote invalid query='${askSparql}'"
		Integer QUERY_HTTP_TIMEOUT_MS = 2000
		log.debug("sparqlAskRemote: endpoint='${endpoint}' query='${askSparql}' useCache=${useCache} useDirectUrl=${useDirectUrl} ")
		
		// FIX for specific issues with endpoints
		if (endpoint =~ "linkedgeodata"){
			assert !( !useDirectUrl && !useCache)
			//assert useCache == true
		}
		
		com.hp.hpl.jena.query.QueryExecution qexec = null
		//ARQ.getContext().setTrue(ARQ.useSAX)
		//com.hp.hpl.jena.query.Query q = null
		Integer trialCounter = 0
		while ( trialCounter < SPARQL_MAX_RETRIALS ){
			trialCounter++
			try {
				//q = QueryFactory.create( askSparql )
				
				qexec = QueryExecutionFactory.sparqlService( endpoint, askSparql )
				boolean res = null 
				if ( useDirectUrl ){
					// FIX for strange JENA bug
					String innerQuery = qexec.toString()
					assert innerQuery[0..3] == "GET "
					innerQuery = innerQuery[4..-1].trim()
					assert innerQuery
					String cacheEntry = null
					if (useCache){
						cacheEntry = null//.getSparqlCacheEntryByUri( innerQuery )
						if (!cacheEntry){
							WebPage wp = Utils.getWebPageByURI( innerQuery, true )
							assert wp.content,"sparqlAskRemote: Query \n"+innerQuery+"\n returned empty response"
							cacheEntry = wp.content
							//utilsService.storeSparqlCacheEntry( innerQuery, res.toString() )
						}
						res = new Boolean( cacheEntry )
					} else {
						WebPage wp = Utils.getWebPageByURI( innerQuery, true )
						assert wp.content,"sparqlAskRemote: Query \n"+innerQuery+"\n returned empty response"
						res = new Boolean( wp.content )
					}
					// END FIX
					//normal version that doesn't work: boolean res = qexec.execAsk()
					qexec.close()
					log.debug( "sparqlAskRemote:"+askSparql+" = "+res )
				} else {
					// do it the normal way (buggy on DBPEDIA)
					qexec.addParam("timeout","${QUERY_HTTP_TIMEOUT_MS}")
					res = qexec.execAsk()
				}
				assert res != null,"empty response from qexec.execAsk()"
				
				if (useCache){
					// store result in cache
					//utilsService.storeSparqlCacheEntry( cacheUri, res.toString() )
				}
				return res
			} catch (e){
				String msg = "sparqlAskRemote: failed to execute '${askSparql}'\non '${endpoint}' useDirectUrl=${useDirectUrl},useCache=${useCache}:\n"+e
				if ( trialCounter >= SPARQL_MAX_RETRIALS ){
					String stackTr = Utils.stack2string(e)
					log.error( stackTr )
					// no retrials left, throw error
					throw new RemoteServiceException( msg )
				}
				log.warn( msg + "\n\tRetrying..." )
				Thread.currentThread().sleep( SPARQL_RETRIAL_SLEEP_MS )
			}
		}
		return false
	}
	
	/**
	 * Run a SELECT sparql query on a remote endpoint
	 * 
	 * @param selSparql
	 * @param endpoint
	 * @return
	 */
	static public ResultSetRewindable sparqlSelectRemote( String selSparql, String endpoint, boolean useCache = true, boolean useDirectUrl = false ){
		assert selSparql
		assert endpoint
		assert selSparql.trim().toLowerCase() =~ "select"
		com.hp.hpl.jena.query.QueryExecution qexec = null
		//// new code
		//def rs = rdfService.executeSparqlOnTdb( selSparql )
		//ResultSetRewindable rwRs = ResultSetFactory.makeRewindable( rs )
		//return rwRs
		// fetch cached results. Prevent hammering SPARQL endpoints
		
		
		// === Fix for JENA bug. Very important. ===
		ARQ.getContext().setTrue(ARQ.useSAX)
		qexec = QueryExecutionFactory.sparqlService( endpoint, selSparql )
		
		int trialCounter = 0
		while ( trialCounter < SPARQL_MAX_RETRIALS ){
			trialCounter++
			try {
				String innerQuery = null
				String xmlResults = null
				if ( useDirectUrl ){
					// BEGIN FIX for another strange JENA bug. 
					// Avoid "" as sometimes it returns empty values. 
					innerQuery = qexec.toString()
					assert innerQuery[0..3] == "GET "
					innerQuery = innerQuery[4..-1].trim() + "&format=xml"
					assert innerQuery
					boolean downloadPage = true
		
					if (downloadPage){
						WebPage wp = Utils.getWebPageByURI( innerQuery, true )
						assert !wp.downloadFailed,"sparqlSelectRemote: Failed query: "+innerQuery
						assert wp.content,"sparqlSelectRemote: Query "+innerQuery+" returned empty response"
						xmlResults = wp.content
						if (useCache){
							//utilsService.storeSparqlCacheEntry( innerQuery, xmlResults )
						}
					}
				} else {
					// use normal way
					ResultSet r2 = qexec.execSelect()
					xmlResults = ResultSetFormatter.asXMLString( r2 )
				}
				
				assert xmlResults

				InputStream is2 = new ByteArrayInputStream( xmlResults.getBytes("UTF-8") )
				
				ResultSet res = ResultSetFactory.fromXML( is2 )

				// END FIX
				qexec.close()
		
				// extract XML
				ResultSetRewindable rwRss = ResultSetFactory.makeRewindable( res )
				String xmlStr = ResultSetFormatter.asXMLString( rwRss )
				assert xmlStr
				res = null
				
				if (useCache){
					//utilsService.storeSparqlCacheEntry( cacheUri, xmlStr )
				}
				rwRss.reset()
				return rwRss
			} catch (e){
				qexec.close()
				String msg = "sparqlSelectRemote: failed to execute '${selSparql}'\non '${endpoint}' useDirectUrl=${useDirectUrl},useCache=${useCache}:\n"+e
				if ( trialCounter >= SPARQL_MAX_RETRIALS ){
					// no retrials left, throw error
					String stackTr = Utils.stack2string(e)
					log.error( stackTr )
					throw new RemoteServiceException( msg )
				}
				log.warn( msg + "\n\tRetrying..." )
				Thread.currentThread().sleep( SPARQL_RETRIAL_SLEEP_MS )
			}
		}
	}

	/**
	 * 
	 * @param uri
	 * @return
	 */
	def getKeyWordsFromUri( String uri ){
		assert uri
		String ns = getNamespaceFromUri( uri )
		def words = []
		if (ns){
			String name = null
			if ( isUriLgdo( uri ) ){
				name = lgdService.getLgdLabel( uri )
			}
			if (!name){
				name = getNameFromUri( uri )
				name = name.decodeURL()
			}
			name = utilsService.clearTextFromSpecialChars( name )
			log.debug("uri=<${uri}> name=${name}")
			if (name){
				"empty name for uri=${uri}"
				String nameSpl = utilsService.splitCamelCaseString( name )
				//if (words.size()==0){
				def splWords = nameSpl.split(" ")*.trim()
				splWords.each{ splW->
					splW = splW.trim() 
					if (splW){
						if ( !utilsService.isStopWord( splW ) )
							words.add( splW )
					}
				}
			}
		}
		if (words.size()==0){
			words.add( uri )
		}
		return words
	}
}
