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
package org.ucd.osmwikicrawler.rdf

import java.io.StringWriter
import org.apache.log4j.Logger
import org.ucd.osmwikicrawler.crawler.Crawler
import org.ucd.osmwikicrawler.ontology.OsmOntology
import org.ucd.osmwikicrawler.utils.OntoUtils
import org.ucd.osmwikicrawler.utils.Utils

import com.hp.hpl.jena.query.*
import com.hp.hpl.jena.rdf.model.*

/**
 * Class that generates an RDF graph from an OsmOntology.
 * 
 * @author Andrea Ballatore
 *
 */
class WikiRdf {
	
	static def log = Logger.getLogger(WikiRdf)
	
	static Model osmWikiModel = null
	
	static final String RDF_LANG = "en"
													  
	static final String OSM_WIKI_NT_FILE_PREFIX = "OSM_Semantic_Network"
		
	/**
	 * 
	 * @param ontology
	 * @return fileName
	 */
    static String genNtFromOsmOntology( OsmOntology ontology, Model m ) {
		assert ontology
		assert m 
		String fn = getNtFileName( ontology )
		String content = getNtTextFromModel( m )
		Utils.outputFile( content, fn )
		
		return fn
    }
	
	/**
	 * 
	 * @param ontology
	 */
	static void genRdfFiles( OsmOntology ontology ){
		assert ontology
		Model m = buildJenaModelFromOntology( ontology )
		assert m
		// gen NT
		String fn = getNtFileName( ontology )
		String content = getNtTextFromModel( m, "N-TRIPLES" )
		Utils.outputFile( content, fn )
		// gen RDF
		fn = getRdfFileName( ontology )
		content = getNtTextFromModel( m, "RDF/XML" )
		content = '<?xml version="1.0" encoding="UTF-8"?>\n' + content
		Utils.outputFile( content, fn )
		
	}
	
	/**
	*
	* @param ontology
	* @return fileName
	*/
   static String genRdfFromOsmOntology( OsmOntology ontology, Model m ) {
	   assert ontology
	   assert m
	   String fn = getNtFileName( ontology )
	   String content = getNtTextFromModel( m )
	   Utils.outputFile( content, fn )
	   
	   return fn
   }
	
	/**
	 * 
	 * @param m
	 * @return UTF
	 */
	static private String getNtTextFromModel( Model m, String format ){
		assert m
		assert format
		log.info("Writing triples ...")
		RDFWriter w = m.getWriter( format )
		StringWriter sw = new StringWriter()
		w.write(m, sw, null)
		String ntText = sw.toString()
		def bytesArr = ntText.getBytes("ISO-8859-1")
		String utfStr = new String(bytesArr, "UTF-8")
		log.info("... Triples were wrote to string. sz=${utfStr.length()}")
		return utfStr
	}
	
	/**
	 * 
	 * @param ontology
	 * @return
	 */
	static private String getNtFileName( OsmOntology ontology ){
		assert ontology
		assert ontology.creationDate
		String timeStr = String.format( '%tF', ontology.creationDate )
		String fn = Utils.getSemantiNetworkOutputFolder() + OSM_WIKI_NT_FILE_PREFIX + "-${timeStr}.nt"
		return fn
	}
	
	/**
	 * 
	 * @param ontology
	 * @return
	 */
	static private String getRdfFileName( OsmOntology ontology ){
		assert ontology
		assert ontology.creationDate
		String timeStr = String.format( '%tF', ontology.creationDate )
		String fn = Utils.getSemantiNetworkOutputFolder() + OSM_WIKI_NT_FILE_PREFIX + "-${timeStr}.rdf"
		return fn
	}
	
	static private String translateOsnUri( String uri ){
		String osnUri = uri.replace("http://wiki.openstreetmap.org/wiki/",OntoUtils.OSN_RESOURCE)
		return osnUri
	}
	
	/**
	 * 
	 * @param sub
	 * @param pred
	 * @param obj
	 * @param m
	 */
	static private void addStatement( String sub, String pred, String obj, Model m, Boolean bTranslateObjOsnUri = true ){
		assert sub
		assert pred
		assert obj
		assert m
		
		sub = sub.replace("=",'%3D').trim() //.decodeURL()
		pred = pred.trim()
		obj = obj.replace("=",'%3D').trim()
		
		sub = translateOsnUri(sub)
		if ( bTranslateObjOsnUri ){
			// only used for sourceUri
			obj = translateOsnUri(obj)
		}
		
		if ( sub == obj ){
			// loop, skip it
			return
		}
		
		assert Utils.validateUrl(sub, false ), "invalid RDF subject = $sub"
		assert Utils.validateUrl(pred, false ), "invalid RDF subject = $pred"

		Resource s = m.createResource( sub )
		Property p = m.createProperty( pred )
		def o = null
		if (obj =~ /^http:\/\//){
			// url
			if (!Crawler.isWikipediaLink(obj)){
				if (!Utils.validateUrl(obj,false)){
					log.warn "skipping invalid uri in statement object="+obj
					return
				}
			}
			o = m.createResource( obj ) //.decodeURL() )
		} else {
			String str = clearStringForLiteral( obj )
			if (!str) return
			// text. add language
			o = m.createLiteral( clearStringForLiteral( str ), RDF_LANG )
		}
		Statement statement = m.createStatement( s, p, o )
		m.add( statement )
	}
	
	/**
	 * 
	 * @param text
	 * @return
	 */
	static private String clearStringForLiteral( String text ){
		if (!text) return ''
		
		text = text.replace("\n",' ')
		text = text.replaceAll("Undefined", ' ')
		text = text.replaceAll(/\s+/,' ')
		text = text.trim()
		return text
	}
	
	
	/**
	 * Add general meta data about the SKOS model.
	 * 
	 * Based on http://www.w3.org/TR/skos-primer/#secscheme
	 * @param m
	 * @return
	 */
	static private Model createSkosScheme( Model m ){
		
		// general meta-data
		addStatement( OntoUtils.SKOS_SCHEMA_NAME, OntoUtils.RDF_TYPE, OntoUtils.SKOS_CONCEPT_SCHEME, m )
		addStatement( OntoUtils.SKOS_SCHEMA_NAME, "http://purl.org/dc/elements/1.1/title", "OSM Semantic Network", m )
		addStatement( OntoUtils.SKOS_SCHEMA_NAME, "http://purl.org/dc/elements/1.1/language", "International English", m )
		addStatement( OntoUtils.SKOS_SCHEMA_NAME, "http://purl.org/dc/elements/1.1/description", "Conceptualisation of geographic terms extracted from the OpenStreetMap Wiki website, and used in the OpenStreetMap vector map.", m )
		addStatement( OntoUtils.SKOS_SCHEMA_NAME, "http://purl.org/dc/elements/1.1/creator", "http://sites.google.com/site/andreaballatore", m )
		addStatement( OntoUtils.SKOS_SCHEMA_NAME, "http://purl.org/dc/elements/1.1/publisher", "http://sites.google.com/site/andreaballatore", m )
		addStatement( OntoUtils.SKOS_SCHEMA_NAME, "http://purl.org/dc/elements/1.1/source", "http://wiki.openstreetmap.org", m )
		addStatement( OntoUtils.SKOS_SCHEMA_NAME, "http://purl.org/dc/elements/1.1/rights", "This material is Open Knowledge. http://opendefinition.org", m )
		addStatement( OntoUtils.SKOS_SCHEMA_NAME, "http://purl.org/dc/elements/1.1/date", new Date().toString(), m )
		addStatement( OntoUtils.SKOS_SCHEMA_NAME, OntoUtils.SKOS_PREFLABEL, "OSM Semantic Network", m )
		addStatement( OntoUtils.SKOS_TOP_CONCEPT, OntoUtils.SKOS_DEFINITION, "Dummy root concept. It does not carry any meaning.", m )
		addStatement( OntoUtils.SKOS_TOP_CONCEPT, OntoUtils.SKOS_PREFLABEL, "root concept", m )
		addStatement( OntoUtils.SKOS_TOP_CONCEPT, OntoUtils.RDF_TYPE, OntoUtils.SKOS_CONCEPT, m )
		addStatement( OntoUtils.SKOS_TOP_CONCEPT, "http://www.w3.org/2004/02/skos/core#topConceptOf", OntoUtils.SKOS_SCHEMA_NAME, m )
		
		
		
		// define new properties
		// TODO
		m = createNewProperties(m)
		return m
	}
	
	static private Model createNewProperties( Model m ){
		// LABELS
		// keyLabel
		addStatement( OntoUtils.SOSM_KEY_LABEL, OntoUtils.RDF_SUBPROPERTYOF, "http://www.w3.org/2000/01/rdf-schema#label", m )
		addStatement( OntoUtils.SOSM_KEY_LABEL, OntoUtils.RDF_LABEL, "has OSM key", m )
		addStatement( OntoUtils.SOSM_KEY_LABEL, OntoUtils.RDF_TYPE, "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property", m )
		addStatement( OntoUtils.SOSM_KEY_LABEL, OntoUtils.RDF_DEFINEDBY, OntoUtils.SKOS_SCHEMA_NAME, m )
		addStatement( OntoUtils.SOSM_KEY_LABEL, OntoUtils.SKOS_DEFINITION, "This concept belongs to the given key", m )

		// valueLabel
		addStatement( OntoUtils.SOSM_VALUE_LABEL, OntoUtils.RDF_SUBPROPERTYOF, "http://www.w3.org/2000/01/rdf-schema#label", m )
		addStatement( OntoUtils.SOSM_VALUE_LABEL, OntoUtils.RDF_LABEL, "has OSM value", m )
		addStatement( OntoUtils.SOSM_VALUE_LABEL, OntoUtils.RDF_TYPE, "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property", m )
		addStatement( OntoUtils.SOSM_VALUE_LABEL, OntoUtils.RDF_DEFINEDBY, OntoUtils.SKOS_SCHEMA_NAME, m )
		addStatement( OntoUtils.SOSM_VALUE_LABEL, OntoUtils.SKOS_DEFINITION, "This concept has the given value", m )
		// RELATIONSHIPS
		addStatement( OntoUtils.SOSM_KEY, OntoUtils.RDF_SUBPROPERTYOF, OntoUtils.SKOS_SEMREL, m )
		addStatement( OntoUtils.SOSM_KEY, OntoUtils.RDF_LABEL, "has OSM key", m )
		addStatement( OntoUtils.SOSM_KEY, OntoUtils.RDF_DEFINEDBY, OntoUtils.SKOS_SCHEMA_NAME, m )
		addStatement( OntoUtils.SOSM_KEY, OntoUtils.SKOS_DEFINITION, "This concept has the given key", m )
		
		addStatement( OntoUtils.SOSM_KEY, 			OntoUtils.RDF_SUBPROPERTYOF, OntoUtils.SKOS_SEMREL, m )
		addStatement( OntoUtils.SOSM_INTERNAL_LINK, OntoUtils.RDF_SUBPROPERTYOF, OntoUtils.SKOS_SEMREL, m )
		addStatement( OntoUtils.SOSM_REDIRECT, 		OntoUtils.RDF_SUBPROPERTYOF, OntoUtils.SKOS_SEMREL, m )
		addStatement( OntoUtils.SOSM_WIKIPEDIA_LINK,OntoUtils.RDF_SUBPROPERTYOF, OntoUtils.SKOS_SEMREL, m )
		addStatement( OntoUtils.SOSM_PHOTO,			OntoUtils.RDF_SUBPROPERTYOF, OntoUtils.SKOS_SEMREL, m )
		addStatement( OntoUtils.SOSM_IMPLIES,		OntoUtils.RDF_SUBPROPERTYOF, OntoUtils.SKOS_SEMREL, m )
		addStatement( OntoUtils.SOSM_COMBINATION,	OntoUtils.RDF_SUBPROPERTYOF, OntoUtils.SKOS_SEMREL, m )
		addStatement( OntoUtils.SOSM_APPLIES_TO,	OntoUtils.RDF_SUBPROPERTYOF, OntoUtils.SKOS_SEMREL, m )
		
		return m
	}
	
	/**
	 * 
	 * 
	 * @param ontology
	 * @return
	 */
	static private Model buildJenaModelFromOntology( OsmOntology ontology ){
		assert ontology
		log.info(Utils.LOG_ROW + "Building Jena Model From Ontology...")
		log.debug("buildJenaModelFromOntology...")
		
		Model m = getOsmWikiModel()
		m = createSkosScheme( m )
		assert m
		def termsToOutput = ontology.terms.findAll{ !it.bFailedToBuild }
		int i = 0
		int nTerms = termsToOutput.size()
		termsToOutput.each{ t->
			i++
			log.debug("buildJenaModelFromOntology: processing term: \n$t")
			int percent = (new Float(i) / nTerms)*100
			log.info("building RDF statements from term ${i}/${nTerms} (${percent}%)")
			Utils.validateUrl( t?.uri )
			//if ( i > 100 ) return
			if ( t.redirectionUri ){
				//log.debug(t)
				assert t?.uri
				addStatement( t?.uri, OntoUtils.SOSM_REDIRECT, t?.redirectionUri, m )
			} else {
				// handle LINKS
				if ( t.uri ){
					
					// add SKOS Concept and scheme info
					addStatement( t.uri, OntoUtils.SKOS_INSCHEME, OntoUtils.SKOS_SCHEMA_NAME, m )
					addStatement( t.uri, OntoUtils.RDF_TYPE, OntoUtils.SKOS_CONCEPT, m )
					
					if ( t.sourceUri ){
						addStatement( t.uri, OntoUtils.DC_SOURCE, t.sourceUri, m, false )
					}
					
					if ( t.key ){
						addStatement( t.uri, OntoUtils.SOSM_KEY_LABEL, t.key, m )
					}
					
					if (Crawler.isOsmKeyPage(t.uri)){
						// Key term
						addStatement( t.uri, OntoUtils.SKOS_BROADER, OntoUtils.SKOS_TOP_CONCEPT, m )
						
						
						String altLabel = "k_" + t.key
						addStatement( t.uri, OntoUtils.SKOS_ALTLABEL, t.key, m )
						addSkosPrefLabelToUri( t.uri, altLabel, m )
						
					}
					
					if ( t.value && t.value != t.key ){
						// Value term
						if (t.value != "*" || ( t.value == "*" && !t.multiValues)){
							addStatement( t.uri, OntoUtils.SOSM_VALUE_LABEL, t.value, m )
							
							// Skos
							if (Crawler.isOsmTagPage(t.uri)){
								String skosLabel = "k_" + t.key + " v_" + t.value
								addSkosPrefLabelToUri(t.uri,skosLabel, m)
								
								String altLabel = t.key + "=" + t.value
								addStatement( t.uri, OntoUtils.SKOS_ALTLABEL, altLabel, m )
								altLabel = t.key + "#" + t.value
								addStatement( t.uri, OntoUtils.SKOS_ALTLABEL, altLabel, m )
							}
						}
					}
					
					if ( t.multiValues ){
						def values = t.multiValues.split(' ')*.trim()
						values.each{
							String val = it.trim()
							addStatement( t.uri, OntoUtils.SOSM_VALUE_LABEL, val, m )
						}
					}
					
					if ( t.photoUris ){
						def values = t.photoUris.split(' ')*.trim()
						values.each{
							String val = it.trim()
							addStatement( t.uri, OntoUtils.SOSM_PHOTO, val, m, false )
						}
					}
					
					if ( t.wikiKeyUris ){
						def keyUris = t.wikiKeyUris.split(' ')*.trim()
						keyUris.each{
							assert it.trim()
							assert Crawler.isOsmKeyPage( it.trim() )
							Utils.validateUrl( it.trim() )
							addStatement( t.uri, OntoUtils.SOSM_KEY, it.trim(), m )
							// Skos
							addStatement( t.uri, OntoUtils.SKOS_BROADER, it.trim(), m )
							//addStatement( it.trim(), OntoUtils.SKOS_NARROWER, t.uri, m )
						}
					}
					
					// types of data supported by tag
					if (t.bNode) 		{addStatement( t.uri, OntoUtils.SOSM_APPLIES_TO , "http://wiki.openstreetmap.org/wiki/Node", m )}
					if (t.bWay) 		{addStatement( t.uri, OntoUtils.SOSM_APPLIES_TO , "http://wiki.openstreetmap.org/wiki/Way", m )}
					if (t.bRelation) 	{addStatement( t.uri, OntoUtils.SOSM_APPLIES_TO , "http://wiki.openstreetmap.org/wiki/Relation", m )}
					
					// description of concept
					if ( t?.description?.trim() ){
						assert t.description.trim() != '' && t.description.trim() != null
						//addStatement( t.uri, OntoUtils.COMMENT_PRED, t?.description.trim(), m )
						addSkosDefinitionToUri( t.uri, t?.description.trim(), m )
					}
					
					// add implies links
					def impliesLinks = t.impliesUris.split(" ")*.trim().findAll{it}
					impliesLinks.each{ link->
						assert link
						Utils.validateUrl( link )
						addStatement( t.uri, OntoUtils.SOSM_IMPLIES, link, m )
					}
					
					// add combination links
					def combinationLinks = t.combinationUris.split(" ")*.trim().findAll{it}
					combinationLinks.each{ link->
						assert link
						Utils.validateUrl( link )
						addStatement( t.uri, OntoUtils.SOSM_COMBINATION, link, m )
					}
					
					// add general links
					def generalLiks = t.descriptionUris.split(" ")*.trim()
					generalLiks.findAll{ it }.each{ link->
						assert link
						link = Utils.fixUri(link, false)
						if ( !Utils.validateUrl(link,false) ){
							log.warn("buildJenaModelFromOntology: invalid uri=\n$link \n skipping...")
							return
						}
						String predicate = null
						if ( Crawler.isOsmWikiUrl(link) ){
							predicate = OntoUtils.SOSM_INTERNAL_LINK
							Utils.validateUrl( link )
							// Skos
							addStatement( t.uri, OntoUtils.SKOS_RELATED, link, m )
						} else {
							if ( Crawler.isWikipediaLink( link ) ){
								// get real link
								String realLink = Utils.getLinkRedirection( link )
								if (realLink) link = realLink
								predicate = OntoUtils.SOSM_WIKIPEDIA_LINK
								addStatement( t.uri, OntoUtils.SKOS_REL_MATCH, link, m )
							}
						}
						addStatement( t.uri, predicate, link, m )
					}
					
					// LGD link
					if ( t.lgdUri ){
						//addStatement( t.uri, OntoUtils.PRED_EQUIVALENT_CLASS, t.lgdUri, m )
						addStatement( t.uri, OntoUtils.SKOS_EXACT_MATCH, t.lgdUri, m )
					}
					
					
				}
			}
		}
		
		m = fixSkosConsistency( m )
		log.debug("build ${m.size()} statements.")
		return m
	}
	
	/**
	 * 
	 * @param subject
	 * @param prop
	 * @param m
	 * @return
	 */
	static boolean statementExists( String subject, String prop, Model m ){
		assert subject
		assert prop
		assert m
		
		String sel = "ASK { <$subject> <$prop> ?a }"
		//log.debug(sel)
		try{
			boolean b = executeSparqlAskOnModel( sel, m )
			return b
		} catch (com.hp.hpl.jena.query.QueryParseException e) {
			// option in case of exception
			log.warn "invalid query '$sel' --- skipping."
			return false
		}
	}
	
	static void addSkosPrefLabelToUri( String subject, String label, Model m ){
		// check if prefLabel exists
		if (statementExists( subject, OntoUtils.SKOS_PREFLABEL, m)){
			log.warn("prefLabel for $subject found. Skipping.")
		} else {
			addStatement( subject, OntoUtils.SKOS_PREFLABEL, label, m )
		}
	}
	
	static void addSkosDefinitionToUri( String subject, String definition, Model m ){
		if (statementExists( subject, OntoUtils.SKOS_DEFINITION, m)){
			log.warn("skos:definition for $subject found. Skipping.")
		} else {
			addStatement( subject, OntoUtils.SKOS_DEFINITION, definition, m )
		}
	}
	
	
	/**
	 * Note: Validate SKOS with 
	 * http://demo.semantic-web.at:8080/SkosServices/index
	 * and 
	 * http://www.seco.tkk.fi/tools/skosify
	 * 
	 * Example: https://www.seegrid.csiro.au/subversion/CGI_CDTGVocabulary/tags/SKOSVocabularies/
	 * 
	 * @param m
	 * @return
	 */
	static Model fixSkosConsistency( Model m ){
		log.info(" Check consistency of SKOS vocabulary...")
		// remove skos:related when skos:broader or skos:narrower  
		String sel = "SELECT ?a ?b { ?a <${OntoUtils.SKOS_BROADER}> ?b . " +  
			 "?a <${OntoUtils.SKOS_RELATED}> ?b }"
		def res = executeSparqlSelectOnModel( sel, m )
		int sz = m.size()
		res.each{ r->
			String a = r.get( "a" ).toString()
			String b = r.get( "b" ).toString()
			// remove skos:related entry
			def s = m.createResource(a)
			def p = m.createProperty(OntoUtils.SKOS_RELATED)
			def o = m.createResource(b)
			
			m = m.remove( ResourceFactory.createStatement(s, p, o) )
			m = m.remove( ResourceFactory.createStatement(o, p, s) )
			
			//String rem = "DELETE {?a <${OntoUtils.SKOS_RELATED}> ?b}"
			//def res2 = executeSparqlUpdateOnModel( rem, m )
			//log.info( "issue found with $a $b")
			assert sz > m.size()
		}
		
		sel = "SELECT ?a ?b { ?a <${OntoUtils.SKOS_NARROWER}> ?b . " +
		"?a <${OntoUtils.SKOS_RELATED}> ?b }"
		res = executeSparqlSelectOnModel( sel, m )
		res.each{ r->
		   String a = r.get( "a" ).toString()
		   String b = r.get( "b" ).toString()
		   // remove skos:related entry
		   def s = m.createResource(a)
		   def p = m.createProperty(OntoUtils.SKOS_RELATED)
		   def o = m.createResource(b)
		   
		   m = m.remove( ResourceFactory.createStatement(s, p, o) )
		   m = m.remove( ResourceFactory.createStatement(o, p, s) )
		   
		   //String rem = "DELETE {?a <${OntoUtils.SKOS_RELATED}> ?b}"
		   //def res2 = executeSparqlUpdateOnModel( rem, m )
		   //log.info( "issue found with $a $b")
		   assert sz > m.size()
	   }
		
		// TODO: run the same test for NARROWER
		
		// TODO: fix multiple PREFLABELS and DEFINITIONS
		
		// TODO: fix missing skos:prefLabel
		
		
		// remove multiple definitions
		assert sz >= m.size()
		log.info(" SKOS vocabulary valid.")
		return m
	}
	
	/**
	 * 
	 * @return
	 */
	static Model getOsmWikiModel(){
		if (osmWikiModel){
			return osmWikiModel
		} else {
			Model m = ModelFactory.createDefaultModel()
			osmWikiModel = m
		}
		assert osmWikiModel != null
		return osmWikiModel
	}
	
	/**
	*
	* @param sparql
	* @return
	*/
   public static Boolean executeSparqlAskOnModel( String sparql, Model m ){
	   assert sparql
	   assert sparql.toLowerCase().trim() =~ "ask","sparql=${sparql}"
	   log.debug("executeSparqlAskOnModel: executing sparql=${sparql}")
	   Query query = QueryFactory.create( sparql )
	   QueryExecution qexec = QueryExecutionFactory.create( query, m )
	   Boolean b = qexec.execAsk()
	   assert b!= null
	   qexec.close()
	   return b
   }
	
	
	/**
	 *
	 * @param sparql
	 * @return
	 */
	Boolean executeSparqlAskOnOsmWiki( String sparql ){
		assert sparql
		assert sparql.toLowerCase().trim() =~ "ask","sparql=${sparql}"
		log.debug("executeSparqlAskOnOsmWiki: executing sparql=${sparql}")
		Model m = getOsmWikiModel()
		Query query = QueryFactory.create( sparql )
		QueryExecution qexec = QueryExecutionFactory.create( query, m )
		Boolean b = qexec.execAsk()
		assert b!= null
		qexec.close()
		return b 
	}
	
	/**
	*
	* @param sparql
	* @return
	*/
   public static ResultSetRewindable executeSparqlUpdateOnModel( String sparql, Model m ){
	   assert sparql
	   //assert sparql.toLowerCase().trim() =~ "select","$sparql"
	   log.debug("executeSparqlUpdateOnModel: executing sparql=${sparql}")
	   Query query = QueryFactory.create( sparql )
	   QueryExecution qexec = QueryExecutionFactory.create( query, m )
	   ResultSet r = qexec.execSelect()
	   ResultSetRewindable rwRs = ResultSetFactory.makeRewindable( r )
	   qexec.close()
	   return rwRs
   }
	
	/**
	 *
	 * @param sparql
	 * @return
	 */
	public static ResultSetRewindable executeSparqlSelectOnModel( String sparql, Model m ){
		assert sparql
		assert sparql.toLowerCase().trim() =~ "select","$sparql"
		log.debug("executeSparqlSelectOnModel: executing sparql=${sparql}")
		Query query = QueryFactory.create( sparql )
		QueryExecution qexec = QueryExecutionFactory.create( query, m )
		ResultSet r = qexec.execSelect()
		ResultSetRewindable rwRs = ResultSetFactory.makeRewindable( r )
		qexec.close()
		return rwRs
	}
	
	
	/**
	 * 
	 * @param sparql
	 * @return
	 */
	ResultSetRewindable executeSparqlSelectOnOsmWiki( String sparql ){
		assert sparql
		assert sparql.toLowerCase().trim() =~ "select","$sparql"
		log.debug("executeSparqlSelectOnOsmWiki: executing sparql=${sparql}")
		Query query = QueryFactory.create( sparql )
		Model m = getOsmWikiModel()
		QueryExecution qexec = QueryExecutionFactory.create( query, m )
		ResultSet r = qexec.execSelect()
		ResultSetRewindable rwRs = ResultSetFactory.makeRewindable( r )
		qexec.close()
		return rwRs
	}
	
	/**
	 * 
	 * @param uri
	 * @return
	 */
	String getOsmWikiDescriptionForUri( String uri ){
		assert uri
		String sparql = "SELECT ?obj { <$uri> <${OntoUtils.COMMENT_PRED}> ?obj }"
		log.debug("getOsmWikiDescriptionForUri: sparql=$sparql")
		def results = executeSparqlSelectOnOsmWiki( sparql )
		def objects = OntoUtils.getValuesFromJenaResultSet( results, "obj" )
		assert objects.size() == 0 || objects.size() == 1
		if (objects.size()>0){
			assert objects[0]
			return objects[0]
		}
		return null
	}
}
