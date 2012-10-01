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

import java.io.InputStream
import org.apache.log4j.Logger
import groovy.json.JsonSlurper
import org.ucd.osmwikicrawler.crawler.Crawler;
import org.ucd.osmwikicrawler.ontology.OsmOntology;
import org.ucd.osmwikicrawler.utils.OntoUtils;
import org.ucd.osmwikicrawler.utils.Utils;

import com.hp.hpl.jena.query.*
import com.hp.hpl.jena.db.*
import com.hp.hpl.jena.rdf.model.*
import groovy.json.*

/**
 * Utils to get meta-data from TagInfo (taginfo.openstreetmap.org)
 * 
 * API: http://taginfo.openstreetmap.org/taginfo/apidoc
 * 
 * @author Andrea Ballatore
 *
 */
class TagInfoUtils {
	
	
	static def log = Logger.getLogger(TagInfoUtils)
	
	/**
	*
	* @param Rdf model
	* @return the same model with meta data from TagInfo taginfo.openstreetmap.org
	*/
   public static Model matchOsnTermsWithTagInfo( Model m ){
	   assert !m.isEmpty()
	   int termsFound = 0
	   log.info(" Adding meta-data from taginfo.openstreetmap.org (model sz=${m.size()})...")
	   
	   def allConcs = WikiRdf.executeSparqlSelectOnModel( "select ?c where { ?c <${OntoUtils.RDF_TYPE}> <${OntoUtils.SKOS_CONCEPT}> . } ", m )
	   //log.info(allConcs.size())
	   def termsUris = OntoUtils.getValuesFromJenaResultSet( allConcs, "c" )
	   //log.info(termsUris)
	   
	   termsUris.each{ uri->
		   //uri = Crawler.truncateUriFromLocalLink(uri)
		   log.debug("matchOsnTermsWithTagInfo: "+uri)
		    
		   //try {
			   def rk = WikiRdf.executeSparqlSelectOnModel( "select ?k where { <$uri> <${OntoUtils.SOSM_KEY_LABEL}> ?k . } ", m )
			   def keys = OntoUtils.getValuesFromJenaResultSet( rk, "k" )
			   
			   def rv = WikiRdf.executeSparqlSelectOnModel( "select ?k where { <$uri> <${OntoUtils.SOSM_VALUE_LABEL}> ?k . } ", m )
			   def values = OntoUtils.getValuesFromJenaResultSet( rv, "k" )
			   
			   log.debug("$keys === $values")
			   
			   termsFound += addTagInfoStatements( uri, keys, values, m )
		   //} catch( RuntimeException e ){
		   	//   log.warn( "Issue while matchOsnTermsWithTagInfo() on uri=$uri\n" + e )
		   //}
	   }
	   log.info(" Found $termsFound term(s) in taginfo.openstreetmap.org.")
	   return m
   }
   
   static String getTagInfoUri( String k, String v ){
	   assert k
	   k = k.trim()
	   
	   if (!validKeyValueOnTagInfo(k,v)){
		   return null
	   }
	   
	   if (!v){
		   String uri = "http://taginfo.openstreetmap.org/keys/$k"
		   uri = Crawler.truncateUriFromLocalLink(uri)
		   return uri
	   } else {
	   	   assert v
		   String uri = "http://taginfo.openstreetmap.org/tags/$k=$v"
		   return uri
	   }
	   
	   return null
   }
   
   static def queryTagInfoKey( String key ){
	   assert key
	   String apiCall = "http://taginfo.openstreetmap.org/api/2/db/keys/keys?key=$key"
	   def json = getJsonFromUri( apiCall )
	   //log.info(json)
	   return json
	   
   }
   
   /**
    * 
    * @param uri
    * @return
    */
   static def getJsonFromUri( String uri ){
	   assert uri
	   String callRes = Utils.downloadURL(uri, 5)
	   def json = new JsonSlurper().parseText(callRes)
	   return json
   }
   
   /**
    * 
    * @param key
    * @param value
    * @return
    */
   static def queryTagInfoKeyValue( String key, String value ){
	   assert key
	   assert value
	   String apiCall = "http://taginfo.openstreetmap.org/api/2/db/tags/overview?value=$value&key=$key"
	   
	   def json = getJsonFromUri(apiCall)
	   //log.info(json)
	   return json
   }
   
   static boolean validKeyValueOnTagInfo( String key, String value ){
	   assert key
	   boolean valid = false
	   if (!value){
		   def json = queryTagInfoKey( key )
		   //log.info("json.total="+ json.total)
		   if (json.total > 0) valid = true
	   } else {
	   		assert value
			def json = queryTagInfoKeyValue(key, value)
			if (json.all.count > 0) valid = true
			
	   }
	   log.debug("validKeyValueOnTagInfo $key=$value => $valid")
	   return valid
   }
   
   /**
    * 
    * @param k
    * @param v
    * @param m
    * @return number of new statements
    */
   static int addTagInfoStatements( String uri, def k, def v, Model m ){
	   
	   if (!k || k.size() == 0) return 0
	   
	   String key = k[0]
	   assert key
	   String tagInfoUri = null
	   
	   int newStm = 0
	   
	   if (!v){
		   tagInfoUri = getTagInfoUri( key, null )
	   }
	   if (v.size() == 1){
		   tagInfoUri = getTagInfoUri( key, v[0] )
	   }
	   
	   if (tagInfoUri){
		   newStm++
		   WikiRdf.addStatement( uri, OntoUtils.SOSM_TAGINFO, tagInfoUri, m)
	   }
	   
	   // multi-values
	   if (v.size() > 1){
		   v.each{ value->
			   tagInfoUri = getTagInfoUri( key, value )
			   if (tagInfoUri){
				   newStm++
				   WikiRdf.addStatement( uri, OntoUtils.SOSM_TAGINFO, tagInfoUri, m)
			   }
		   }
	   }
	   return newStm
   }
}
