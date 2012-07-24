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
package org.ucd.osmwikicrawler.crawler

import java.net.URLEncoder
import java.util.regex.*
import org.ucd.osmwikicrawler.ontology.*
import org.ucd.osmwikicrawler.utils.OntoUtils;
import org.ucd.osmwikicrawler.utils.Utils
import org.apache.log4j.*

/**
 * Main Class containing the crawler logic.
 * Class to parse OSM wiki and extract tag info.
 *
 * @author Andrea Ballatore
 */
class Crawler {
	
	static final String RAW_DATA_OPTION = "?&action=raw"

	static def log = Logger.getLogger(Crawler.class.name)
	
	static final String OSM_WIKI_BASE_URL = "http://wiki.openstreetmap.org/wiki/"
	static final String OSM_MAP_FEATURES_PAGE = OSM_WIKI_BASE_URL + "Map_Features"
	static final String OSM_WIKI_ALL_PAGES = "http://wiki.openstreetmap.org/w/index.php?title=Special:AllPages"
	static final String OSM_WIKI_ALL_PAGES_REGEX = "http://wiki.openstreetmap.org/w/index.php\\?title=Special:AllPages"
	/*
	static final def OSM_WIKI_ALL_PAGES_CLUSTERS = ["${OSM_WIKI_ALL_PAGES}&from=Karlovy_Vary&to=Key%3Afuel%3A1_50",
		"${OSM_WIKI_ALL_PAGES}&from=Key%3Afuel%3AGTL_diesel&to=Key%3Arecycling_type",
		"${OSM_WIKI_ALL_PAGES}&from=Key%3Aref&to=Kolkata",
		"${OSM_WIKI_ALL_PAGES}&from=Princeton&to=Proposed_features%2FDirectional_Prefix_%26_Suffix_Indication",
		"${OSM_WIKI_ALL_PAGES}&from=Proposed_features%2FDisabilityDescription&to=Proposed_features%2FRailway",
		"${OSM_WIKI_ALL_PAGES}&from=Proposed_features%2FRailway_Signals&to=Proposed_features%2Femergency_phone",
		"${OSM_WIKI_ALL_PAGES}&from=Proposed_features%2Femergency_vehicle_access&to=Pt-br%3AKey%3Aaddr%3Afloor",
		"${OSM_WIKI_ALL_PAGES}&from=TMC%2FTMC_Import_Germany%2FRoads%2F53400_to_53500&to=Tag%3Aaeroway%3Dterminal",
		"${OSM_WIKI_ALL_PAGES}&from=Tag%3Aaeroway%3Dwindsock&to=Tag%3Adock%3Dfloating",
		"${OSM_WIKI_ALL_PAGES}&from=Tag%3Adock%3Dtidal&to=Tag%3Anatural%3Dgrassland",
		"${OSM_WIKI_ALL_PAGES}&from=Tag%3Anatural%3Dheath&to=Tag%3Ashop%3Dvacuum_cleaner",
		"${OSM_WIKI_ALL_PAGES}&from=Tag%3Ashop%3Dvariety_store&to=Talca%2C_Chile"
		]*/
	
	static final String OSM_KEY_BASE_URL = OSM_WIKI_BASE_URL + "Key:"
	static final String OSM_TAG_BASE_URL = OSM_WIKI_BASE_URL + "Tag:"
	static final String OSM_TAG_INFO_URL = "http://taginfo.openstreetmap.de/api/2/db/tags/overview"
	
	static final String OSM_MAP_PROPOSED_FEATURES_PAGE = OSM_WIKI_BASE_URL + "Proposed_features"
	static final String OSM_PROPOSED_TAG_URL = OSM_WIKI_BASE_URL + "Proposed_features/"
	static final String OSM_PROPOSED_REL_URL = OSM_WIKI_BASE_URL + "Relations/Proposed/"
	
	/** OSM TYPES */
	static final String OSM_TYPE_NODE_URL = "Elements#Node"
	static final String OSM_TYPE_WAY_URL = "Elements#Way"
	static final String OSM_TYPE_AREA_URL = "Elements#Area"
	static final String OSM_TYPE_REL_URL = "Elements#Relation"

	static final String ASTERISK_URL_ENC = "%2A"
	static final String MULTI_VALUE = "*"
	
	static final int KEY_MAX_LENGTH = 60
	static final int KEY_MAX_VALUE = 250
	static final int OSM_URI_MAX_LENGTH = 250
	static final int OSM_URI_MIN_LENGTH = 29
	
	static final String OSM_MULTI_VALUE_SEPARATOR = ' '

	/**
	 * Constructor. 
	 */
	Crawler(){
		createFolders()	
	}
	
	/**
	 * Main method of the Crawler.
	 * Extract OSM ontological info from wiki web page.
	 *  
	 * @return an OsmOntology
	 */
	static OsmOntology createOntologyFromOsmWiki(){		
		OsmOntology ontology = new OsmOntology()
		Set visitedUris = []
		ontology = visitMapFeaturesPage( ontology, visitedUris )
		// add CLUSTER pages
		Set uris = getUrlsFromUrl( OSM_MAP_FEATURES_PAGE )
		// add proposed tags page
		uris.add( OSM_MAP_PROPOSED_FEATURES_PAGE )
		// scan ALL PAGES
		log.info(">>>> scanning pages...")
		int curIdx = 0
		new File(Utils.getPageCacheFolder()).eachFile{ f->
			int _DEBUG_LIMIT = 4
			if (curIdx > _DEBUG_LIMIT){
				return
			}
			
			// validate URI
			String uri = Utils.getUriFromFileName( f.name )
			if (!uri) return
			if (!isOsmWikiUrl(uri)) return
			if (!Utils.validateUrl( uri,false )) return
			
			// process URI
			Set allUris = getUrlsFromUrl( uri ).findAll{isOsmWikiUrl(it)}
			uris.addAll( allUris )
			
			int _LOG_INTERVAL = 5000
			int i = uris.size()/_LOG_INTERVAL
			if ( i > curIdx ){
				log.info("> uris to scan="+uris.size())
				curIdx = uris.size()/_LOG_INTERVAL
			}
		
		}
		
		//OSM_WIKI_ALL_PAGES_CLUSTERS.each{ page->
		//	Set allUris = getUrlsFromUrl( page )
		//	uris.addAll( allUris )
		//	log.info("> uris to scan="+uris.size())
		//}
		log.info(">>>> ${uris.size()} valid pages found.")
		assert uris.size() > 0
		ontology = consumeAllUris( uris, visitedUris, ontology )
		//ontology = lgdService.matchOsmOntoTermsWithLgd( ontology, algo )
		ontology = workoutStatsInOntology( ontology )
		return ontology
	}

	/**
	 * 
	 */
	static void createFolders(){
		log.debug("Creating folders...")
		def d1= new File( Utils.getPageCacheFolder() )
		d1.mkdirs()
		d1= new File( Utils.getSemantiNetworkOutputFolder() )
		d1.mkdirs()
		log.debug("... done.")
	}
	
	
	/**
	 * 
	 * @param urisToExpand
	 * @param expandedUris
	 * @param ontology
	 * @return
	 */
	static private OsmOntology consumeAllUris( Set urisToExpand, Set expandedUris, OsmOntology ontology ){
		int i = 0
		int prevExpandedSize = expandedUris.size()
		while ( !urisToExpand.isEmpty() ){
			i++
			//if ( i > 120 ) break // ONLY FOR DEBUG
			if (prevExpandedSize != expandedUris.size()){
				log.info("** Page queue: \t done=${expandedUris.size()}\t todo=${urisToExpand.size()}")
			}
			prevExpandedSize = expandedUris.size()
			// consume 
			String uri = urisToExpand.toList().remove(0)
			assert uri
			assert i < 10000000,"infinite loop detected on uri=$uri"
			
			urisToExpand.remove(uri)
			if ( isValidLinkForExpansion(uri) ){
				//if (uri == "http://wiki.openstreetmap.org/wiki/Proposed_features/hardware") // DEBUG STUFF
				ontology = visitWikiUri( uri, ontology, expandedUris )
				
				expandedUris.add(uri)
	
				// produce urls
				Set links = getUrlsFromUrl( uri ).findAll{ isValidLinkForExpansion(it) }
				urisToExpand.addAll( links.findAll{ 
					if (expandedUris.contains(it)) return false
					return true
				} )
			}
		}
		log.info("Crawling complete. $i pages parsed.")
		return ontology
	}
	
	/**
	 * 
	 * @param uri
	 * @return true if uri is an English Wikipedia link.
	 */
	static boolean isWikipediaLink( String uri ){
		boolean b = (uri =~ "http://en.wikipedia.org/wiki/")
		return b
	}
	
	/**
	 * 
	 * @param root of xml tree
	 * @return collection of URI contained in the xml document.
	 * 
	 */
	static private def getUrlsFromXmlNode( def xmlRoot, String baseUrl = OSM_WIKI_BASE_URL ){
		assert xmlRoot
		def urls = []
		xmlRoot.'**'.each{
			if ( it?.@href ){
				boolean validLink = false
				String link = it?.@href?.text()?.trim()
				if (!link) return
				
				if (link =~ /^http/){
					// uri is fine
				} else {
					// relative url, make it absolute
					if (link[0] != "/" ) return
						assert link[0] == "/" 
					link = baseUrl + link
					link = link.replaceAll("/wiki//wiki/","/wiki/")
					link = link.replaceAll("/wiki//","/wiki/")
				}
				urls.add( link )
			}
		}
		return urls
	}
	
	/**
	 * 
	 * @param a uri
	 * @return all URIs contained in uri.
	 */
	static private def getUrlsFromUrl( String uri ){
		final String htmlCode = getWikiUriContent( uri )
		if (!htmlCode) return []
		
		def tree = parseHtmlPage( htmlCode )
		assert tree
		
		def urls = getUrlsFromXmlNode( tree )
		//log.info("urls found: $urls.size()")
		return urls
	}
	
	/**
	 * 
	 */
	static private def getUrisFromOsmOntoTerm( OsmOntoTerm term ){
		String allLinks = term.descriptionUris
		def uris = allLinks.split(" ")*.trim().findAll{
			it =~ /^http/
		}
		return uris
	}
	
	/**
	 * The term returned is not necessarily valid and complete.
	 * Extraction from Map feature page.
	 * 
	 * @param rowData
	 * @return term
	 */
	static private OsmOntoTerm buildOntoTermFromMapFeatTableRow( String sourceUri, Map rowData, def fields, OsmOntology ontology, boolean createPseudoUri = false, Set pseudoUris = null ){
		assert ontology
		assert sourceUri
		
		if (rowData.isEmpty()) return null
		
		String key = rowData["key"]
		String value = rowData["value"]?.replace("%",'percentage')
		if (!value) value = key
		
		if (!key || !value){
			log.debug "null key or value in rowData in wikiTable: fields=$fields rowData=$rowData Skipping..."
			return null
		}
		
		OsmOntoTerm t = new OsmOntoTerm()
		t.foundInOverviewPage = true
		
		// extract data
		t.key = key
		if ( isOsmKeyPage( rowData["keyUri"] ) ){
			t.wikiKeyUris = rowData["keyUri"]
		}
		//assert t.wikiKeyUri,"t.wikiKeyUri empty in $rowData" 
		
		// ad hoc fix for a value containing '%'_/_
		t.value = value.replaceAll(" ","_").replaceAll("_/_","/").toLowerCase()
		
		if ( isOsmTagPage( rowData["valueUri"] ) || isOsmKeyPage( rowData["valueUri"] )){
			t.uri = rowData["valueUri"]
		}
		t.photoUris = rowData["photoUri"]
		t.renderingUri = rowData["renderingUri"]
		//if (!t.photoUris) log.warn("${rowData} ${fields}")
		
		// extract all descriptions
		def descriptionFields = ["description","definition","comment","comments","desc"]
		
		assert !t.description
		//assert !t.descriptionUris
		t.description = ''
		if (!t.descriptionUris) t.descriptionUris = ''
		
		descriptionFields.each{ field->
			if (rowData[field]) t.description += ' ' + rowData[field] 
			if (rowData[field+"Uri"]) t.descriptionUris += ' ' + rowData[field+"Uri"]
		}
		t.description = t.description.trim()
		// NB: back link to page from which the element has been extracted. This might affect the results.
		//t.descriptionUris = (t.descriptionUris + ' ' + sourceUri).trim()
		
		// find types
		String el = rowData["elementUri"] + " "+ rowData["element"]
		el = el.trim().toLowerCase()

		if (el){
			//assert el, rowData
			if (el =~ "node"){ t.bNode = true }
			if (el =~ "way"){t.bWay = true }
			if (el =~ "area"){t.bArea = true }
			if (el =~ "relation"){t.bRelation = true }
		}
		
		assert t.key,"invalid term ${t}"
		assert t.value, "invalid term ${t}"
		
		if (createPseudoUri){
			assert pseudoUris != null
			if (!t.uri){
				// generate pseudo URI
				t.bPseudoUri = true
				t.uri = getUriFromKeyValue( t.key, t.value )
			}
			if (!t.wikiKeyUris){
				t.bPseudoUri = true
				t.wikiKeyUris = getUriFromKeyValue( t.key, t.key )
			}
			assert t.uri
			assert t.wikiKeyUris
			pseudoUris.add( t.uri )
		}
		
		t = fixIssuesInTerm( t, sourceUri )
		return t
	}
	
	
	static String fixLongOsmWikiUri( String uri ){
		assert isOsmWikiUrl(uri)
		assert !Utils.validUriLength( uri )
		int firstEq = uri.indexOf( '%3D' )
		assert firstEq > 0
		String cleanUri = uri[0..firstEq+2] + '*'
		assert cleanUri[-4..-1] == '%3D*'
		assert Utils.validUriLength( cleanUri )
		log.debug("long uri $uri was cut to $cleanUri")
		return cleanUri
	}
	
	/**
	 * 
	 * @param t
	 * @return
	 */
	static private OsmOntoTerm fixIssuesInTerm( OsmOntoTerm t, String sourceUri = null ){
		assert t
		
		if (!t.key || !t.value){
			String msg = "invalid key-value: '$t.key' = '$t.value'"
			log.warn msg
			t.bFailedToBuild = true
			t.sFailedToBuild = msg
			return t
		}
		
		//t.uri = Utils.fixUri(t.uri)
		
		assert t.key, "null key in t=$t"
		assert t.value, "null value in t=$t"
		
		
		final String DOTS = '\\.\\.\\.'
		
		String oldKey = t.key
		String oldValue = t.value
		
		if (t.key =~ '='){
			t.key = t.key.replaceAll('=','')
			t.key = t.key.replaceAll('\\*','')
			
		}
		t.key = t.key.replaceAll(DOTS,' ')
		t.key = t.key.replaceAll('\\*','')
		if (t.value =~ '='){
			t.value = t.value.replaceAll('=','_')	
		}
		
		t.value = t.value.replaceAll('_:',':')
		t.value = t.value.replaceAll(':_',':')
		t.value = t.value.replaceAll('\\(',' ')
		t.value = t.value.replaceAll('\\)',' ')
		t.value = t.value.replaceAll('\\<',' ')
		t.value = t.value.replaceAll('\\>',' ')
		t.value = t.value.replaceAll('_optional',' ')
		t.value = t.value.replaceAll(DOTS,' ')
		
		t.key = t.key.trim()
		t.value = t.value.trim()
		
		if (!t.key || !t.value){
			String msg = "invalid key-value: '$oldKey' = '$oldValue'"
			log.debug msg
			t.bFailedToBuild = true
			t.sFailedToBuild = msg
			return t
		}
		
		if (t.value=="user_defined" || t.value=="user-defined"){
			t.value = MULTI_VALUE
		}
		
		// check if value is 'yes' and add 'no'
		if (t.value=="yes"){
			assert t.key != t.value
			t.value = MULTI_VALUE
			t.multiValues = ["yes","no"].join( OSM_MULTI_VALUE_SEPARATOR )
			return t
		}
		t.value = t.value.replaceAll('\n','/')
		
		def values = t.value.split('/')*.trim().findAll{it}
		if (values.size()>1){
			def clearValues = []
			// multi value detected
			t.value = MULTI_VALUE
			values.each{ !(it =~ OSM_MULTI_VALUE_SEPARATOR) }
			values.each{
				if ( it.length() > 50 ){
					log.debug "fixIssuesInTerm: value suspiciously long: '$it' in $values. Skipping in term=$t "
					return
				}
				String val = it.replaceAll('_',' ').trim()
				val = val.trim()
				val = val.replaceAll(' ','_')
				clearValues.add(val)
			}
			t.multiValues = clearValues.join( OSM_MULTI_VALUE_SEPARATOR )
			return t
		}
		// check for comma separated values
		values = t.value.split(',')*.trim().findAll{it}
		if (values.size()>1){
			// comma-separated multi value detected
			def clearValues = []
			values.each{ !(it =~ OSM_MULTI_VALUE_SEPARATOR) }
			values.each{
				if ( it.length() > 50 ){
					log.warn "fixIssuesInTerm: value suspiciously long: '$it' in $values. Skipping in term=$t "
					return
				}
				String val = it.replaceAll('_',' ').trim()
				val = val.trim()
				val = val.replaceAll(' ','_')
				clearValues.add(val)
			}
			t.value = MULTI_VALUE
			t.multiValues = clearValues.join( OSM_MULTI_VALUE_SEPARATOR )
		}
		
		// check for multi value in URI
		def links = t.uri.split(' ').findAll{it}
		if (links.size()>1){
			//t = fixIssuesInTerm( t )
			//assert t.multiValues,"multiValues is null $t.multiValues (sourceUri=$sourceUri) in $t"
			assert t.wikiKeyUris
			t.uri = t.wikiKeyUris
			// too many links, move them to the description Uris
			t.descriptionUris += ' ' + links.join( ' ' )
			t.descriptionUris = t.descriptionUris.trim()
		}
		
		if ( t.key.length() >= KEY_MAX_LENGTH ){
			String msg = "fixIssuesInTerm: key too long in $t"
			t.bFailedToBuild = true
			t.sFailedToBuild = msg
			log.debug msg
		}
		
		if (t.multiValues){
			String newUri = getUriFromKeyValue( t.key, t.value )
			assert newUri
			if ( t.uri != newUri ){
				log.debug( "fixIssuesInTerm: updating URI=$t.uri to $newUri" )
				t.uri = newUri
			}
			assert t.uri
		}
		
		def newWikiKeyUris = []
		t.wikiKeyUris.split(' ')*.trim().each{
			 String fixedUri = Utils.fixUri(it)
			 newWikiKeyUris.add( fixedUri )
		}
		t.wikiKeyUris = newWikiKeyUris.join(OSM_MULTI_VALUE_SEPARATOR)
		
		if (t.bPseudoUri){
			// fix potentially invalid url
			t.uri = Utils.fixUri(t.uri)
		}
		Utils.validateUrl(t.uri)
		t.descriptionUris = Utils.fixUrisString( t.descriptionUris )
		t.wikiKeyUris = Utils.fixUrisString( t.wikiKeyUris )
		t.impliesUris = Utils.fixUrisString( t.impliesUris )
		t.combinationUris = Utils.fixUrisString( t.combinationUris )
		return t
	}  
	
	/**
	 * 
	 * @param k
	 * @param v
	 * @return a OSM wiki uri, either for TAG or for KEY
	 */
	static String getUriFromKeyValue( String k, String v ){
		k = k.trim().toLowerCase().replaceAll(' ','_')
		v = v.trim().toLowerCase().replaceAll(' ','_')
		assert k
		assert v
		String uri = ''
	
		if (k == v){
			// key
			uri = OSM_KEY_BASE_URL + k
		} else {
			// tag
			uri = OSM_TAG_BASE_URL + k + "%3D" + v
		}
		assert uri
		return uri
	}
	
	/**
	 * 
	 * @param uri
	 * @return target uri
	 */
	static private String getWikiRedirection( String uri ){
		assert uri
		assert isOsmWikiUrl( uri )
		String wikiCode = getWikiUriContent( uri + RAW_DATA_OPTION )
		if (!wikiCode) return null
		if ( wikiCode.toLowerCase() =~ "#redirect" ){
			// redirect page
			def uris = parseUrisFromWikiText( wikiCode )
			def goodUris = uris.findAll{ isOsmTagPage(it) || isOsmProposedPage(it) || isOsmKeyPage( it ) }
			if (goodUris.size() == 1)
				return goodUris[0]
			else {
				log.warn "valid uri not found in '$uris' --- wikicode: $wikiCode"
				return null
			}
		} else return null
	}
	
	/**
	 * Merge onto terms.
	 * 
	 * @param t1
	 * @param t2
	 * @return merged term
	 */
	static private OsmOntoTerm mergeOntoTerms( OsmOntoTerm t1, OsmOntoTerm t2 ){
		assert t1
		assert t2
		//log.debug("mergeOntoTerms\n\t${t1}\n\t${t2}")

		assert t1.key == t2.key
		assert t1.value == t2.value
		
		t1.key = mergeStrings( t1.key, t2.key, true )
		t1.value = mergeStrings( t1.value, t2.value, true )
		t1.multiValues = mergeStrings( t1.multiValues, t2.multiValues )
		t1.wikiKeyUris = mergeStrings( t1.wikiKeyUris, t2.wikiKeyUris, true, t1.key )
		t1.uri = mergeStrings( t1.uri, t2.uri, true )
		t1.description = mergeStrings( t1.description, t2.description )
		t1.descriptionUris = mergeStrings( t1.descriptionUris, t2.descriptionUris )
		t1.renderingUri = mergeStrings( t1.renderingUri, t2.renderingUri )
		t1.photoUris = mergeStrings( t1.photoUris, t2.photoUris )
		t1.lgdUri = mergeStrings( t1.lgdUri, t2.lgdUri )
		t1.impliesUris = mergeStrings( t1.impliesUris, t2.impliesUris )
		t1.combinationUris = mergeStrings( t1.combinationUris, t2.combinationUris )
		t1.tagCommunityStatus = mergeStrings( t1.tagCommunityStatus, t2.tagCommunityStatus )
		t1.redirectionUri = mergeStrings( t1.redirectionUri, t2.redirectionUri )

		t1.foundInOverviewPage = mergeBooleans( t1.foundInOverviewPage, t2.foundInOverviewPage )
		t1.foundInSinglePage = mergeBooleans( t1.foundInSinglePage, t2.foundInSinglePage )
		t1.foundInProposedPage = mergeBooleans( t1.foundInProposedPage, t2.foundInProposedPage )
		
		t1.bNode = mergeBooleans( t1.bNode, t2.bNode )
		t1.bWay = mergeBooleans( t1.bWay, t2.bWay )
		t1.bArea = mergeBooleans( t1.bArea, t2.bArea )
		t1.bRelation = mergeBooleans( t1.bRelation, t2.bRelation )
		
		//log.debug("merged term:\n\t${t1}")
		return t1
	}
	
	/**
	 * Merge strings.
	 * Used for merging tag descriptions.
	 * 
	 * @param a
	 * @param b
	 * @return merged string
	 */
	static private String mergeStrings( String a, String b, boolean getSingleValue = false, String textWithPriority = null){
		if ( !a && !b ) return ''
		if ( !a ) return b
		if ( !b ) return a
		
		if ( a == b ) return a
		
		if ( getSingleValue ){
			if (!(a =~ b) && !(b =~ a)){
				log.warn "mergeStrings uris are incompatible: $a $b. using textWithPriority=$textWithPriority"
				boolean priorityA = a =~ textWithPriority.trim()
				boolean priorityB = b =~ textWithPriority.trim()
				assert !(priorityA && priorityB),"can't decide priority between a and b a=$a b=$b textWithPriority=$textWithPriority"
				if (priorityA) return a
				if (priorityB) return b
				assert false,"no text was merged: a=$a b=$b textWithPriority=$textWithPriority"
			} else {
				if ( a.length() > b.length() ) return a
				else return b
			}
		}
		assert a && b
		return "${a} ${b}"
	}
	
	/**
	 * 
	 * @param a
	 * @param b
	 * @return union of a and b
	 */
	static private Boolean mergeBooleans( boolean a, boolean b ){
		return a || b
	}
	
	/**
	 * Parse page such as:
	 * 		http://wiki.openstreetmap.org/wiki/Tag:shop%3Ddeli
	 * 		http://wiki.openstreetmap.org/wiki/Key:access
	 * 
	 * @param uri a OSM WIki page.
	 * @return onto term extracted from uri 
	 */
	static private OsmOntoTerm buildTermFromWikiSinglePage( String uri ){
		assert uri
		assert isOsmTagPage(uri) || isOsmKeyPage(uri)


		OsmOntoTerm term = new OsmOntoTerm()
		term.uri = uri
		term.foundInSinglePage = true
		
		if ( isOsmTagPage(uri) ){
			String s = uri.replace( OSM_TAG_BASE_URL, '')
			s = URLDecoder.decode(s)
			assert s.length()>1
			if (s[-1]=="=") s += "*" // fix for Key:something=
			//assert s =~ "="
			def ss = null
			//if (s =~ ":") ss = s.split(':')
			if (s =~ "=") ss = s.split('=')
			if ( !ss || ss?.size() != 2 ){
				term.bFailedToBuild = true
				term.sFailedToBuild += "invalid tag '${s}'"
			} else {
				assert ss.size() == 2
				term.key = ss[0].trim().toLowerCase()
				term.value = ss[1].trim().toLowerCase()
				term.wikiKeyUris = "${OSM_KEY_BASE_URL}${term.key}"
			} 
		}
		if ( isOsmKeyPage(uri) ){
			String s = uri.replace( OSM_KEY_BASE_URL, '')
			assert s
			term.key = s.trim().toLowerCase()
			term.value = term.key
			term.wikiKeyUris = uri
		}
		
		assert term.foundInSinglePage
		assert !term.foundInProposedPage
		assert term.uri
		//assert term.key
		//assert term.value
		
		final String htmlCode = getWikiUriContent(term.uri)
		if (!htmlCode) return term
		// parse code
		def tree = parseHtmlPage( htmlCode )
		assert tree
		
		// util
		def clearString = {
			if (!it) return ''
			return it.replaceAll("\n",' ').replaceAll(",",' ').toLowerCase().replaceAll(/\s+/,' ').trim()
		}
		
		boolean inDescSection = false
		boolean startingSection = true
		String descStr = ''
		tree.'**'.each{
			String label = clearString ( it?.name() )
			if (label=="h2"){
				inDescSection = (clearString( it?.text() )=="description")
				startingSection = false
			}
			if (label=="p" && (inDescSection || startingSection)){
				descStr += it?.text() + " "
			}
		}
		descStr = descStr.trim()
		term.description += descStr
		
		def uris = getUrlsFromXmlNode( tree ).findAll{ return linkFilter( it ) }
		String linksStr = ''
		
		if (uris.size()>0){
			term.descriptionUris += " " + uris.join(' ')
			if (!term.descriptionUris){
				term.descriptionUris = ''
			}
		}
		
		// extract specific links
		term = getImpliesAndCombinationLinks( term, tree )
		
		// extract images
		term.photoUris = ''
		tree.'**'.each{
			if (it?.@class?.text() == "vcard"){
				def images = findImagesInXml( it )
				if (images.size()>0){
					term.photoUris += ' ' + images.join(' ')
				}
			}
		}
		term.photoUris = term.photoUris.trim()

		// find types
		if (htmlCode =~ OSM_TYPE_NODE_URL) term.bNode = true
		if (htmlCode =~ OSM_TYPE_WAY_URL) term.bWay = true
		if (htmlCode =~ OSM_TYPE_REL_URL) term.bRelation = true
		if (htmlCode =~ OSM_TYPE_AREA_URL) term.bArea = true
		
		//term = fixIssuesInTerm( term )
		validateOsmOntoTerm( term )
		return term
	}
	
	/**
	 * 
	 * @param xml
	 * @return array of URIs contained in an html table.
	 */
	static private def getUrisFromTableXml( String fieldName, def xml ){
		assert xml
		assert fieldName
		
		def uris = []
		// try to extract implicated uris from xml tree
		boolean inBlock = false
		xml.'**'.each{
			if (it.name().trim().toLowerCase()=='dt'){
				inBlock = (it.text().trim().toLowerCase() =~ fieldName)
				//log.debug(it.name() + "   "+ it.text())
				//log.debug("inBlockImplies=$inBlockImplies")
			}
			if (inBlock){
				//log.debug(it.name() + "   "+ it.text())
				def links = getUrlsFromXmlNode( it )
				if (links.size()>0) uris.addAll(links)
			}
		} 
		
		return uris
	}
	
	/**
	 * It's potentially very slow.
	 * 
	 * @param ontology
	 * @return
	 */
	static private OsmOntology workoutTagStatsInOntology( OsmOntology ontology ){
		assert ontology
		ontology.terms.each{ t->
			t = getTagStatsFromOsmService( t )
		}
		return ontology
	}
	
	/**
	 * 
	 * @param key
	 * @return
	 */
	static private OsmOntoTerm getTagStatsFromOsmService( OsmOntoTerm term ){
		assert term
		assert term.value,"empty value in term ${term}"
		String v = term.value
		String k = term.key
		assert v
		assert k
		
		String uri = OSM_TAG_INFO_URL+"?key=${k}&value=${v}"
		
		final String jsonCode = Utils.getWebPageByURI( uri, true ).content
		assert jsonCode
		//def json = JSON.parse( jsonCode )
		//assert json
		//term.osmTotalCount = new Long( json.all.count )
		//term.osmNodeCount = new Long( json.nodes.count )
		//term.osmWayCount = new Long( json.ways.count )
		//term.osmRelCount = new Long( json.relations.count )
		
		return term
	}
	
	/**
	 * 
	 * @param uri
	 * @return true if uri is a OSM Wiki page.
	 */
	static boolean isOsmWikiUrl( String uri ){
		if (!uri) return false
		boolean valid = uri =~ OSM_WIKI_BASE_URL
		if (!valid) valid = uri =~ OSM_WIKI_ALL_PAGES_REGEX 
		return valid
	}
	
	/**
	 * 
	 * @param uri
	 * @return true if uri is a OSM Wiki Tag page.
	 */
	static private boolean isOsmTagPage( String uri ){
		if (!isOsmWikiUrl(uri)) return false
		return uri.toLowerCase() =~ OSM_TAG_BASE_URL.toLowerCase()
	}
	
	/**
	 * 
	 * @param uri
	 * @return
	 */
	static private boolean isOsmProposedPage( String uri ){
		if (!isOsmWikiUrl(uri)) return false
		boolean b = (uri.toLowerCase() =~ OSM_PROPOSED_REL_URL.toLowerCase()) || (uri.toLowerCase() =~ OSM_PROPOSED_TAG_URL.toLowerCase())
		//log.debug("isOsmProposedPage $uri $b")
		return b
	}
	
	/**
	 * 
	 * @param uri
	 * @return
	 */
	static private boolean isOsmKeyPage( String uri ){
		if (!isOsmWikiUrl(uri)){
			return false
		}
		boolean b = uri.toLowerCase() =~ OSM_KEY_BASE_URL.toLowerCase()
		//log.debug("isOsmKeyPage $uri $b")
		return b
	}
	
	/**
	 * Compute statistics in ontology.
	 * 
	 * @param ontology
	 * @return same ontology with statistics.
	 */
	static private OsmOntology workoutStatsInOntology( OsmOntology ontology ){
		assert ontology
		
		ontology.nTermsCount = new Long( ontology?.terms?.size() )
		ontology.nTermsFailed = ontology?.terms?.findAll{ it.bFailedToBuild }?.size()
		ontology.nTermsValid = ontology?.terms?.findAll{ !it.bFailedToBuild }?.size()
		
		ontology.lgdClassFound = ontology?.terms?.findAll{ (it.lgdUri) }.size()
		ontology.termsWithWpLinks = ontology?.terms?.findAll{ "${it?.descriptionUris}".toLowerCase().contains("wikipedia") }.size()
		ontology.redirectionCount = ontology?.terms?.findAll{ isRedirectionTerm it }.size()
		//workoutTagStatsInOntology( ontology )
		
		return ontology
	}
	
	/**
	 * 
	 * @param uri
	 * @return clean uri without local link.
	 */
	static String truncateUriFromLocalLink( String uri ){
		assert uri
		if ( uri.contains('#') ){
			// truncate uri with '#' to the base uri.
			String oldUri = uri
			int firstOcc = uri.indexOf('#')
			assert firstOcc > 1
			uri = uri[0..firstOcc-1]
			//assert false,"$oldUri --> $uri"
		}
		assert uri
		return uri
	}
	
	/**
	 * Visit uri and extract ontological info.
	 * 
	 * @param uri a OSM Wiki page.
	 * @param ontology
	 * @return same ontology
	 */
	static private OsmOntology visitWikiUri( String uri, OsmOntology ontology, Set visitedUris ){
		assert uri
		assert ontology
		assert visitedUris != null
		
		uri = truncateUriFromLocalLink( uri )
		
		if (!isValidLinkForExpansion(uri)){
			return ontology 
		}
		if (visitedUris.contains(uri)){
			return ontology
		}
		
		log.debug("visitWikiUri: ${uri} ...")

		// TODO: test this properly
		Set pseudoUris = []
		ontology = getDataFromWikiTableIfPresent( uri, ontology, visitedUris, pseudoUris )
		//if (uri!="http://wiki.openstreetmap.org/wiki/Tag:shop%3Dmall") return ontology
		
		// check for redirections
		OsmOntoTerm termFromUri = buildTermFromWikiUrl( uri )
		
		// PseudoUris: these URIs are not defined in the WIKI but they contain valuable information
		// extracted from wikitables.
		if (pseudoUris.size()>0){
			termFromUri.descriptionUris += " " + pseudoUris.join(' ')
			termFromUri.descriptionUris = termFromUri.descriptionUris.trim()
		}
		assert termFromUri
		addTermToOntology( termFromUri, ontology )
		visitedUris.add( uri )
		return ontology
	}
	
	
	
	/**
	 * 
	 * @param t
	 * @return
	 */
	static public boolean isRedirectionTerm( OsmOntoTerm t ){
		if (t.redirectionUri){
			return true
		}
		return false
	}
	
	/**
	 * 
	 * @param uri
	 * @return
	 */
	static private boolean isValidLinkForExpansion( String uri ){
		return isOsmTagPage( uri ) || isOsmKeyPage( uri ) || isOsmProposedPage( uri )
	}
	
	/**
	 * 
	 * @param wikiText
	 * @param baseUrl
	 * @return
	 */
	static private def parseUrisFromWikiText( String wikiText, String baseUrl = OSM_WIKI_BASE_URL ){
		assert wikiText
		assert baseUrl
		
		def LINK_REGEX = /\[\[(.+)\]\]/
		def matcher = (wikiText =~ LINK_REGEX)
		def uris = []
		matcher.each{
			String relLink = it[1]
			assert relLink
			String uri = baseUrl + relLink
			uri = uri.replaceAll(' ','_')
			uris.add(uri)
		}
		return uris
	}
	
	/**
	 * 
	 * @param wikiText
	 * @param template
	 * @return
	 */
	private def getTemplatesFromString( String wikiText, String template ){
		assert wikiText
		assert template
		def results = []
		Pattern p = Pattern.compile("\\{\\{\\s*"+template+"(.+)\\}\\}", Pattern.DOTALL )
		Matcher m = p.matcher( wikiText )
		while (m.find()){
			assert m.group(1)
			results.add( m.group(1) )
		}
		log.debug("getTemplatesFromString '$template' sz=${results.size()}--> $results")
		return results
	}
	
	/**
	 * 
	 * @param wikiText
	 * @param template
	 * @return
	 */
	static private Map getValuesFromTemplate( String wikiText ){
		assert wikiText
		Map results = [:]
		Pattern p = Pattern.compile("\\|(.+)\\=(.*)", Pattern.DOTALL )
		Matcher m = p.matcher( wikiText )
		while (m.find()){
			String key = m.group(1)
			String value = m.group(2)
			assert key
			assert value
			results[ key ] = value
		}
		log.debug("getValuesFromTemplate sz=${results.size()} --> $results")
		
		return results
	}
	
	/**
	 * 
	 * @param wikiTerm
	 * @param term
	 * @return
	 */
	static private OsmOntoTerm parseValueDescriptionTable( String wikiText, OsmOntoTerm term ){
		assert term
		
		wikiText = '''
			 
		{{ValueDescription
		|key=highway
		|value=secondary
		|image=Image:Meyenburg-L134.jpg
		|description=A highway linking large towns.
		|onNode=no
		|onWay=yes
		|onArea=no
		|combination=
		* {{Tag|name}}
		* {{Tag|ref}}
		|implies=
		* {{Tag|motorcar||yes}}
		}}
			
		{{ValueDescription
			|key=sukandra
			|value=secondary
			|image=Image:Meyenburg-L134.jpg
			|description=A highway linking large towns.
			|onNode=no
			|onWay=yes
			|onArea=no
			|combination=
			* {{Tag|name}}
			* {{Tag|ref}}
			|implies=
			* {{Tag|motorcar||yes}}
			}}
		'''
		def descValues = getTemplatesFromString( wikiText, "ValueDescription" )
		if (descValues.isEmpty()) return term
		
		descValues.each{ desc->
			Map m = getValuesFromTemplate( desc )
			log.debug(m)
		}
		
		return term
	}
	
	/**
	 * 
	 * @param uri
	 * @return
	 */
	static OsmOntoTerm buildTermFromWikiUrl( String uri ){
		assert uri
		assert isOsmWikiUrl(uri)
		def t = null
		
		// check for redirection
		String redirectionLink = getWikiRedirection( uri )
		if ( redirectionLink ){
			// the page is only a redirection.
			OsmOntoTerm redirTerm = new OsmOntoTerm()
			redirTerm.uri = uri
			redirTerm.redirectionUri = redirectionLink
			return redirTerm
		}
		
		if (isOsmProposedPage(uri)){
			t = buildTermFromWikiProposedPage( uri )
		} else {
			t = buildTermFromWikiSinglePage( uri )
		}
		validateOsmOntoTerm(t)
		return t
	}
	
	/**
	 * 
	 * @param uri
	 * @param ontology
	 * @return
	 */
	static private OsmOntoTerm findTermInOntology( OsmOntoTerm toFind, OsmOntology ontology ){
		assert toFind.key,"empty key, $toFind"
		assert toFind.value,"empty value, $toFind"
		assert ontology
		def terms = ontology.terms.findAll{
			it.key == toFind.key && it.value == toFind.value && it.uri == toFind.uri
		}
		if (terms.size()>0){
			assert terms.size() == 1,"duplicated terms: ${terms.join('\n')}"
			assert (terms as ArrayList)[0] != null
			return (terms as ArrayList)[0]
		} else {
			return null
		}
	}
	
	/**
	 * 
	 */
	static private def parseHtmlPage( String html ){
		assert html
		return new XmlSlurper( new org.ccil.cowan.tagsoup.Parser() ).parseText( html )
	}
	
	/**
	 * Try to get data from "wikitable" table in the given uri.
	 * It should work on MapFeature page and on http://wiki.openstreetmap.org/wiki/Key:building
	 * 
	 * @param uri
	 * @param ontology
	 * @param visitedUris
	 * @return
	 */
	static private OsmOntology getDataFromWikiTableIfPresent( String uri, OsmOntology ontology, Set visitedUris, Set pseudoUris ){
		assert uri 
		assert ontology
		assert visitedUris!=null
		assert pseudoUris!=null
		String htmlCode = getWikiUriContent( uri )
		if (!htmlCode){
			// experimental code
			pseudoUris.add( uri )
			return ontology
		}
		assert htmlCode,"getDataFromWikiTableIfPresent: empty html code for uri="+uri
		
		def tree = parseHtmlPage( htmlCode )
		assert tree
		
		def tables = tree.'**'.findAll{
			if ( it?.name()?.trim()?.toLowerCase() == "table" ){
				return it?.@class?.text()?.contains("wikitable")
			}
			return false
		}
		tables.each{ table->
			def headerFields = []
			table.tr.th.each{
				String field = it.text().trim().toLowerCase()
				if (field){
					headerFields.add(field)
				}
			}
			if (headerFields.size() == 0){
				log.debug "getDataFromWikiTableIfPresent: table in <$uri> has no header: $table"
				return
			}
			
			ontology.wikiTableFound++
			
			def rows = []
			Map rowSpanCells = [:]
			
			table.tr.each{ r->
				def row = [:]
				OsmOntoTerm t = null
				// scan table row
				int nCol = r.td.size()
				//log.debug "nCol=$nCol"
				if (nCol >= 3 ){
					//assert nCol == 6,"this table row doesn't seem to contain tag info"
					
					def cells = []
					int colIdx = -1
					r.td.each{ cell ->
						colIdx++
						if (rowSpanCells[colIdx]!=null){
							// rowSpan cell found for this column.
							log.debug("using row span cells colIdx=$colIdx")
							cells.add( rowSpanCells[colIdx][0] )
							rowSpanCells[colIdx][1]--
							if (rowSpanCells[colIdx][1]<=0){
								rowSpanCells[colIdx]=null
							}
							//colIdx++
						}
						
						int rowspan = new Integer( cell.@rowspan.text() )
						if (rowspan > 1){
							log.debug("rowSpan=$rowspan")
							rowSpanCells[colIdx] = [cell,rowspan]
						}
						cells.add( cell )
					}
					//assert cells.size() == headerFields.size(),"cell is not valid. too few rows."
					// cells are good now, scan them
					colIdx = -1

					cells.each{ cell ->
						
						colIdx++
						
						String field = headerFields[colIdx]?.trim()
						
						if (!field){
							log.debug "empty table field in <$uri>: $headerFields . Skipping..."
							return
						}
						row[ field ] = cell.text().trim()
						def uris = getUrlsFromXmlNode( cell ).findAll{linkFilter(it)}
						row[ field+"Uri" ] = uris.join(' ').trim()
						
					}
					rows.add(row)
					t = buildOntoTermFromMapFeatTableRow( uri, row, headerFields, ontology, true, pseudoUris )
					
				}
				//assert t
				if (t){
					t = fixIssuesInTerm( t, uri )
					validateOsmOntoTerm( t )
					addTermToOntology( t, ontology )
					ontology.termsBuiltFromWikiTables++
				}
			}
		}
		visitedUris.add( uri )
		return ontology
	}
	
	/**
	 * 
	 * @param ontology
	 * @param visitedUris
	 * @return
	 */
	static private OsmOntology visitMapFeaturesPage( OsmOntology ontology, Set visitedUris ){
		assert ontology
		assert visitedUris != null
		log.debug("visitMapFeaturesPage $OSM_MAP_FEATURES_PAGE ...")
		Set pseudoUris = []
		ontology = getDataFromWikiTableIfPresent( OSM_MAP_FEATURES_PAGE, ontology, visitedUris, pseudoUris )
		if (pseudoUris.size() > 0 ){
			log.debug("visitMapFeaturesPage: Extracted pseudoUris=$pseudoUris")
		}
		return ontology
	}
	
	/**
	 * 
	 * @param root
	 * @return
	 */
	static private def findImagesInXml( def root ){
		assert root
		def links = getUrlsFromXmlNode( root )
		def images = links.findAll{ it.toLowerCase().contains("jpg") }
		log.debug("findImagesInXml: found ${images.size()} image(s)")
		return images
	}
	
	/**
	 * 
	 * @param uri
	 * @return
	 */
	static private Boolean isWikiPageValid( String uri ){
		if (!(uri =~ OSM_WIKI_BASE_URL)) return false
		assert uri.count("http") == 1,"invalid osm wiki uri=$uri"
		
		String content = getWikiUriContent(uri).toLowerCase()
		if (!content || content =~ "there is currently no text in this page" 
				|| content =~ "requested page title was invalid"){
			return false
		}
		return true
	}
	
	/**
	 * 
	 * @param lgdUri
	 * @return
	 */
	static private OsmOntoTerm crawlProposedTagPageForLgdClass( String lgdUri, OsmOntology ontology ){
		assert OntoUtils.isUriLgdo( lgdUri )
		assert ontology
		String name = OntoUtils.getNameFromUri( lgdUri )
		name = Utils.splitCamelCaseString( name ).trim()
		String uri = getProposedPageUrlFromTag( name )
		
		//log.debug("crawlProposedTagPageForLgdClass: <${lgdUri}> ==> $term")
		//return term
	}
	
	static private String getProposedPageUrlFromTag( String tag ){
		assert tag
		String clearTag = tag.trim().replaceAll(/\s+/,"_").trim()
		String targetUrl = OSM_PROPOSED_TAG_URL + clearTag
		return targetUrl
	}
	
	/**
	 * Parse pages such as 
	 * http://wiki.openstreetmap.org/wiki/Proposed_features/Downhill_Terminal
	 * 
	 * @param tag
	 * @return
	 */
	static private OsmOntoTerm buildTermFromWikiProposedPage( String uri ){
		assert uri
		assert isOsmProposedPage( uri )
		
		String tag = uri.toLowerCase().replaceAll( OSM_PROPOSED_TAG_URL.toLowerCase(), "") // using string to fix strange bug
		tag = tag.replaceAll( OSM_PROPOSED_REL_URL.toLowerCase(), "") // using string to fix strange bug
		tag = tag.toLowerCase().trim()
		assert !(tag =~ "http:"),"invalid tag from uri $uri"
		//log.debug("crawlProposedTagPage: '$tag' <${uri}>")
		String html = Utils.getWebPageByURI( uri, false )?.content
		
		// new term
		OsmOntoTerm term = new OsmOntoTerm()
		term.foundInProposedPage = true
		term.uri = uri
		
		if (!html){
			term.bFailedToBuild = true
			term.sFailedToBuild += "Empty page"
			return term
		}
		
		assert html,"html for '$uri' is null"
		def tree = parseHtmlPage( html )
		assert tree
		// extract info from proposed feature page
		Map data = extractTableFromProposedFeatPage( tree )

		if (data.isEmpty()){
			term.bFailedToBuild = true
			term.sFailedToBuild += "The page contains no data."
			// add all data to definition
			term.key = tag.toLowerCase().trim()
			term.value = term.key
			term.description = ''
			tree.'**'.each{
				if (it.@id.text() == "bodyContent"){
					it.'**'.each{
						term.description += it.text() + " "
					}
				}
			}
			return term
		}

		// check for rejected terms
		String status = data['status']?.toLowerCase()?.trim()
		term.tagCommunityStatus = status
		if ( status =~ "rejected" || status =~ "inactive"){
			term.bFailedToBuild = true
			term.sFailedToBuild += "tag was rejected by the community: '$status' in <$uri> $data. Skipping..."
		}
		
		String tagging = data["tagging"]
		term = parseTagFromCellStr( tagging, tag, term )
		
		//assert data["definition"] != null,"empty definition in $data for $term"
		if (data["definition"] != null){
			term.description = data["definition"]
		}
		term = getImpliesAndCombinationLinks( term, tree )
		
		def links = getUrlsFromXmlNode( tree ).findAll{linkFilter(it)}
		term.descriptionUris = links?.join(" ")
		
		String types = data["applies to"]?.toLowerCase()?.trim()
		String typesUris = data["applies toUris"]?.toLowerCase()?.trim()
		types = Utils.clearTextFromSpecialChars( types + typesUris ).trim()
		if (!types){
			term.sFailedToBuild += "empty types from data $data"
			term.bFailedToBuild = true
		}
		term.bWay = types =~ "way"
		term.bNode= types =~ "node"
		term.bArea= types =~ "area"
		term.bRelation = types =~ "relation"
		
		//term = fixIssuesInTerm( term )
		term.uri = Utils.fixUri( term.uri )
		
		validateOsmOntoTerm( term )
		return term
	}
	
	/**
	 * 
	 * @param term
	 * @param xmlTree
	 * @return
	 */
	static private OsmOntoTerm getImpliesAndCombinationLinks( OsmOntoTerm term, def xmlTree ){
		assert term
		assert xmlTree
		
		term.impliesUris = getUrisFromTableXml( "implies", xmlTree ).findAll{linkFilter(it)}.join(' ')
		term.combinationUris = getUrisFromTableXml( "combination", xmlTree ).findAll{linkFilter(it)}.join(' ')
		
		return term
	}
	
	/**
	 * 
	 */
	static private OsmOntoTerm parseTagFromCellStr( String tagging, String tag, OsmOntoTerm term ){
		if (!tagging){
			term.bFailedToBuild = true
			term.sFailedToBuild += "empty tag data"
			return term
		}
		assert term
		assert tag
		def keyValue = null
		
		// ad hoc fixes to extract valid tagging info
		tagging = tagging.replaceAll("=\\*=","=")
		tagging = tagging.replaceAll(/\s*-\s*/,"_")
		tagging = tagging.replaceAll("/","_")
		if (tagging == '=*'){
			tagging = tag + "=*"
			// TODO: FIX IT
		}
		//if (tagging=~"/"){
		//	def keyValues = tagging.split("/")*.trim()
		//	assert keyValues.size()>1
		//	keyValue = keyValues[0].split("=")*.trim()
		//} else {
		
		// split key from value
		keyValue = tagging.split("=")*.trim()
		//}
		if (keyValue.size() != 2){
			term.sFailedToBuild += "invalid key=tag data: '$tagging'. Skipping..."
			term.bFailedToBuild = true
		} else {
			// tagging should be valid here
			assert keyValue.size() == 2,"invalid key=tag data: '$tagging'"
			term.key = keyValue[0].toLowerCase()
			term.value = keyValue[1].toLowerCase()
			if (!term.key){
				term.sFailedToBuild += "empty key from '$tagging' for ${term}"
				term.bFailedToBuild = true
			}
			assert term.value,"empty value from '$tagging' for ${term}"
			if (term?.key?.length() > KEY_MAX_LENGTH){
				term.sFailedToBuild += "key too long to be correct: from '$tagging' for ${term}. skipping page..."
				term.bFailedToBuild = true
			}
			if (term?.value?.length() > 50){
				log.warn "value too long to be correct: from '$tagging' for ${term}. cutting details..."
				term.value = "*"
			}
			String keyUrl = OSM_KEY_BASE_URL + term.key
			if ( !isWikiPageValid( keyUrl )){
				term.bFailedToBuild = true
				term.sFailedToBuild += "$keyUrl is not a valid page"
			}
			term.wikiKeyUris = keyUrl
		}
		return term
	}
	
	/**
	 * 
	 * @param term
	 */
	static private void validateOsmOntoTerm( OsmOntoTerm term ){
		if (term?.bFailedToBuild){
			// skip failed terms
			return
		}
		if (isRedirectionTerm(term)){
			// Redirection term
			assert term.uri
			assert !term.key && !term.value,"invalid redirection term $term"
			return
		}
		//assert term.uri,"null uri in $term"
		assert term,"null term"
		assert term.key,"null key in $term"
		assert !(term.value =~ '='),"equal is not allowed in value. term=$term"
		assert !(term.key =~ '='),"equal is not allowed in key. term=$term"
		assert !(term.key =~ '\\*'),"asterisk is not allowed in key. term=$term"
		assert term.value,"null value in $term"
		assert term.key.length() < KEY_MAX_LENGTH,"invalid term key length $term"
		assert term.wikiKeyUris,"term.wikiKeyUri is null in $term"
		if (term.value.length() > 100){
			log.warn( "invalid term value length $term" )
			term?.bFailedToBuild = true
			return
		}
		assert !(term.uri =~ " " ),"invalid term value uri $term"
		//TODO: make sure that term.wikiKeyUris multi key is valid. 
		// assert !(term.wikiKeyUris =~ " " ),"invalid term key uri $term"
		//assert term.bWay || term.bNode || term.bArea || term.bRelation, "invalid term types $term"
		//assert term?.wikiKeyUri?.count("http") == 1,"invalid wikiKeyUri = ${term.wikiKeyUri}"
		if (term.uri){
			//assert term.uri.count("http") == 1,"invalid uri $term.uri in $term"
		}
		
		// validate URIs
		boolean validUri = Utils.validateUrl( term.uri, false )
		if (!validUri){
			term?.bFailedToBuild = true
			log.warn("Invalid uri=${term.uri}, skipping term")
			return
		}
		term.wikiKeyUris.split(' ')*.trim().each{
			if (it){
				Utils.validateUrl( it )
			}
		}
		
	}
	
	/**
	 * 
	 * @param term
	 * @param ontology
	 */
	static private void addTermToOntology( OsmOntoTerm term, OsmOntology ontology ){
		assert term
		assert ontology
		
		// merge terms if duplicates
		if ( !term.bFailedToBuild && !isRedirectionTerm( term )){
			
			def t2 = findTermInOntology( term, ontology )
			if (t2){
				// term exists, merge them
				ontology?.terms?.remove( t2 )
				term = mergeOntoTerms( term, t2 )
				ontology.mergedTerms++
			}
		}
		
		//if (term.bFailedToBuild){
		//	ontology.addToTerms( term )
			//ontology.addToFailedTerms( term )
		//} else {
		ontology.terms.add( term )
		//}
	}
	
	/**
	 * 
	 * @param xml
	 * @return
	 */
	static private Map extractTableFromProposedFeatPage( def xml ){
		Map data = [:]
		xml.'**'.grep{it.name().trim()=="tr"}.each{
			String key=''
			String value=''
			String valueUris=''
			it.children().each{ col->
				if (col.name()=="th"){
					key = col.text().replaceAll(":"," ").trim().toLowerCase()
				} else {
					if (col.name()=="td"){
						value = col.text().trim()
						def cellLinks = getUrlsFromXmlNode( col ).findAll{linkFilter(it)}
						if (cellLinks.size()>0){
							valueUris = cellLinks.join(' ')
						}
					}
				}
			}
			data[key]=value
			if (valueUris){
				data[key+"Uris"] = valueUris
			}
		}
		
		if (!data["tagging"]){
			def tags = data.findAll{ k,v -> v.toLowerCase() =~ "approved proposal" }
		}
		return data
	}
	
	/**
	 * 
	 * @param uri
	 * @return
	 */
	static boolean linkFilter( String uri ){
		assert uri
		//return isWikipediaLink( uri )
		return isValidLinkForExpansion( uri ) || isWikipediaLink( uri )
	}
	
	/**
	 * Dump all wiki website starting from startUri.
	 * This process crawls the website page by page. 
	 * @deprecated use setupWikiDump() instead
	 * @return log string.
	 */
	static String wikiDump(){
		assert false
		Set toExpand = [ OSM_MAP_FEATURES_PAGE ]
		Set expanded = []
		
		while ( toExpand.size() > 0 ){
			String uri = toExpand.toList().remove(0)
			assert uri
			String baseUri = Utils.removeAnchorFromUri( uri )
			if (expanded.contains(baseUri)){
				//toExpand.remove( uri )
				log.debug( "$uri has already been expanded in base=$baseUri" )
				toExpand.remove( uri )
				continue
			}
			uri = baseUri
			
			log.debug("wikiDump: done=${expanded.size()} todo=${toExpand.size()}. doing <$uri>...")
			String html = null
			// download html page in local file
			if  (!(new File(getDumpFileName( uri )).exists())){
				html = Utils.getWebPageByURI( uri, false )?.content
				Utils.outputFile( html, getDumpFileName( uri )+".html")
			}
			// download page wikisource
			if  (!(new File(getDumpFileName( uri + RAW_DATA_OPTION) ).exists())){
				String wikitext = Utils.getWebPageByURI( uri + RAW_DATA_OPTION, false )?.content
				
				assert !(wikitext =~ "DOCTYPE html PUBLIC"), "uri $uri is html and not wikitext"
				
				if (!wikitext) wikitext = '' // fix for a few pages
				Utils.outputFile( wikitext, getDumpFileName( uri )+".wiki")
			}
			toExpand.remove( uri )
			expanded.add( uri )
			
			if (!html)
				html = Utils.getWebPageByURI( uri, false )?.content
			
			def tree = parseHtmlPage( html )
			Set links = getUrlsFromXmlNode( tree )
			
			toExpand.addAll( links.findAll{
				if (!isValidLinkForExpansion( it )) return false
				if (expanded.contains(it)) return false
				return true
			} )
		}
		
		String msg = Utils.LOG_ROW + "<br/>wikiDump: downloaded ${expanded.size()} pages in ${getWikiDumpFolder()}" + Utils.LOG_ROW
		return msg
	}
	
	/**
	 * Read uri content from local cache, or download it form the web if it is not cached.
	 * @param uri
	 * @return content in page uri.
	 */
	static String getWikiUriContent( String uri ){
		assert uri
		
		assert isOsmWikiUrl( uri ),"getWikiUriContent invalid uri $uri"
		
		String content = null
		String fn = Utils.getDumpFileName( uri )
		assert fn
		if  (Utils.isCachedPageStillValid( fn )){
			// file ok
			log.debug( "getWikiUriContent: file found in cache for URI=$uri" )
		} else {
			//log.debug("file not found for uri=$uri: downloading it ... $fn")
			String html = Utils.getWebPageByURI( uri, false )?.content
			if (!html) html = ''
			Utils.outputFile( html, fn )
		}
		content = Utils.readFile( fn )
		return content
	}
}
