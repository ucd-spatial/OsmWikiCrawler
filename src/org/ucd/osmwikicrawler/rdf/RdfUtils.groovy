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
import com.hp.hpl.jena.query.*
import com.hp.hpl.jena.db.*
import com.hp.hpl.jena.rdf.model.*

/**
 * Utils for the RDF format.
 * 
 * @author Andrea Ballatore
 *
 */
class RdfUtils {
	
	private IDBConnection jenaConnection = null
	private ModelMaker maker = null
	private Model globalModel = null
	private Model metaModel = null
	static def log = Logger.getLogger(RdfUtils)
	
	/**
	 * This function works out the rdf URI and returns the model
	 *
	 * @param uri URI
	 * @return model
	 */
	private Model getMetaModel(){
		if (metaModel) return metaModel
		
		def maker = getMaker()
		assert maker
		assert !metaModel
		metaModel = maker.openModel( JENA_MODEL_NAME_META )
		assert metaModel
		return metaModel
	}
	
	/**
	 *
	 * @param statements
	 * @return
	 */
	def addStatementsToGlobalModel( def statements ){
		log.debug("addStatementsToGlobalModel: importing " + statements + " statements...")
		getGlobalModel()?.add( statements )
		log.debug("...done.")
		
	}
	
	/**
	 * This function works out the rdf URI and returns the model
	 *
	 * @param uri URI
	 * @return model
	 */
	private Model getGlobalModel(){
		if (globalModel) return globalModel
		
		def maker = getMaker()
		assert maker
		assert !globalModel
		globalModel = maker.openModel( JENA_MODEL_NAME )
		assert globalModel
		return globalModel
	}
	
	
	/**
	 *
	 * @param rdf Jena Model
	 * @param ns e.g. "http://www.w3.org/2003/01/geo/wgs84_pos#geometry"
	 * @param prop e.g. ""
	 * @return set of statements matching predicateUri
	 */
	def getStatementsFromGlobalModel( String subject, String predicate, String object ){
		assert predicate
		assert subject || object
		
		Model m = getGlobalModel()
		assert m
		
		Property p = null
		Property s = null
		Property o = null
		//Property to filter the model
		if (subject)		s = m.createProperty( subject )
		if (predicate)		p = m.createProperty( predicate )
		if (object)			o = m.createProperty( object )
		
		def statements = []
		SimpleSelector sel = new SimpleSelector(s, p, o)
		
		StmtIterator iter = m.listStatements( sel )
		while (iter?.hasNext()) {
			Statement stm = iter.nextStatement()
			if (stm.getObject().isLiteral()) {
				Literal obj = (Literal) stm.getObject()
				String val = obj.getString()
				if (val){
					statements.add(val)
				}
			} else if (stm.getObject().isResource()) {
				Resource obj = (Resource) stm.getObject()
				String val = obj.toString()
				if (val){
					statements.add(val)
				}
			} else {
				assert false,"stm.getObject() should be handled"
			}
		}
		log.debug("getStatementsFromGlobalModel: s='${subject}', p='${predicate}', o='${object}': found=${statements.size()}")
		return statements
	}
	
	/**
	 *
	 * @return
	 */
	private ModelMaker getMaker(){
		if (maker) return maker
		
		def conn = getJenaConnection()
		assert conn
		maker = ModelFactory.createModelRDBMaker( conn )
		assert maker
		return maker
	}
	
	/**
	 * 
	 * @return connection to local Jena db 
	 */
	private IDBConnection getJenaConnection(){
		if (jenaConnection){
			return jenaConnection
		}
		
		// create connection
		// database URL
		String M_DB_URL         = "jdbc:postgresql://yeats.ucd.ie:5432/maps"
		// User name
		String M_DB_USER        = "aballatore"
		// Password
		String M_DB_PASSWD      = "andreab"
		// Database engine name
		String M_DB 			= "PostgreSQL"
		// JDBC driver
		String M_DBDRIVER_CLASS = "org.postgresql.Driver"
		// load the the driver class
		Class.forName(M_DBDRIVER_CLASS)
		
		// create a database connection
		jenaConnection = new DBConnection(M_DB_URL, M_DB_USER, M_DB_PASSWD, M_DB)
		assert jenaConnection
		log.debug("Jena connection opened: ${M_DB_URL} -"+jenaConnection.toString())
		return jenaConnection
	}
	
	
	/**
	 * @return Rdf Model containing manual mappings
	 */
	Model getManualMappingModel(){
		// load manual mappings
		String inputFile = Utils.getResourcePath() + ONTO_MANUAL_MAPPING_FILE_RDF
		assert inputFile
		Model m = ModelFactory.createDefaultModel()
		try{
			InputStream is = new FileInputStream( inputFile )
			assert is
			m.read( is, " " )
			is.close()
		} catch(IOException e) {
			log.error("getManualMappingModel: "+e)
			throw e
		}
		return m
	}
	
	/**
	 *
	 * @return all manual mappings
	 */
	def getAllManualMappings(){
		Model m = getManualMappingModel()
		assert m
		def statements = []
		StmtIterator iter = m.listStatements()
		while ( iter?.hasNext() ){
			Statement stm = iter.nextStatement()
			String predUri = stm.getPredicate().getURI().toString()
			String subUri = stm.getSubject().getURI().toString()
			
			assert predUri
			String objUri = ''
			// object
			if (stm.getObject().isLiteral()) {
				Literal obj = (Literal) stm.getObject()
				objUri = obj.getString()
			} else if (stm.getObject().isResource()) {
				Resource obj = (Resource) stm.getObject()
				objUri = obj.toString()
			} else {
				assert false,"stm.getObject() should be handled"
			}
			statements.add( [ subUri, predUri, objUri ] )
		}
		return statements
	}
	
	/**
	 *
	 * @param subject
	 * @param predicate
	 * @param object
	 * @param m
	 * @return
	 */
	Model addRdfStatementToModel( String subject, String predicate, String object, Model m ){
		assert subject
		assert predicate
		assert object
		assert m
		
		Resource s = m.createResource(subject)
		Property p = m.createProperty(predicate)
		Resource o = m.createResource(object)
		
		Statement statement = m.createStatement( s, p, o )
		m.add(statement)
		
		return m
	}
	
	/**
	 * 
	 * @param sparql
	 * @return
	 */
	String executeAnySparqlOnTdbAsHtml( String sparql ){
		if (!sparql) return "Empty query."
		sparql = sparql.trim()
		String html = ''
		if (sparql.toLowerCase() =~ /^select/){
			getAllRepositories().each{ rep->
				html += Utils.LOG_ROW + "Repository: ${rep}" + Utils.LOG_ROW
				def rs = executeSparqlSelectOnTdb( sparql, rep )
				ByteArrayOutputStream bao = new ByteArrayOutputStream()
				ResultSetFormatter.outputAsCSV( bao, rs )
				html += new String( bao.toByteArray() ) 
				html += "<br/>"
			}
			// execute on OSM wiki dataset
			html += Utils.LOG_ROW + "Repository OSMWiki: " + Utils.LOG_ROW
			def rs = wikiRdfService.executeSparqlSelectOnOsmWiki( sparql )
			ByteArrayOutputStream bao = new ByteArrayOutputStream()
			ResultSetFormatter.outputAsCSV( bao, rs )
			html += new String( bao.toByteArray() ) 
		}
		else 
		if (sparql.toLowerCase() =~ /^ask/){
			getAllRepositories().each{ rep->
				html += "Repository ${rep}: "
				def b = executeSparqlAskOnTdb( sparql, rep )
				html += "${b}<br/>"
			}
			// execute on OSM wiki dataset
			html += Utils.LOG_ROW + "Repository OSMWiki: " + Utils.LOG_ROW 
			def b = wikiRdfService.executeSparqlAskOnOsmWiki( sparql )
			html += "${b}<br/>"
		}
		else assert sparql,"invalid SPARQL: ${sparql}. Only 'ask' and 'select'."
		
		html = html.replaceAll("\n","<br/>")
		return html
	}
}
