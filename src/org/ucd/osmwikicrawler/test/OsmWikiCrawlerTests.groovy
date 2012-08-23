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
package org.ucd.osmwikicrawler.test

import junit.framework.TestResult;

import org.apache.commons.validator.UrlValidator
import org.ucd.osmwikicrawler.crawler.Crawler
import org.ucd.osmwikicrawler.exceptions.RemoteServiceException
import org.ucd.osmwikicrawler.ontology.Lgd;
import org.ucd.osmwikicrawler.ontology.OsmOntoTerm;
import org.ucd.osmwikicrawler.ontology.OsmOntology
import org.ucd.osmwikicrawler.rdf.WikiRdf;
import org.ucd.osmwikicrawler.utils.Utils
import groovy.util.GroovyTestCase

/**
 * Tests for OsmWikiCrawler
 * 
 * @author Andrea Ballatore
 *
 */
class OsmWikiCrawlerTests extends GroovyTestCase {
	
	static def problematicUris = [ "http://wiki.openstreetmap.org/wiki/Proposed_features/hardware",
		"http://wiki.openstreetmap.org/wiki/Tag:shop%3Dhardware",
	   "http://wiki.openstreetmap.org/wiki/Tag:highway%3Dsecondary",
	   "http://wiki.openstreetmap.org/wiki/Proposed_features/hardware",
	   "http://wiki.openstreetmap.org/wiki/Key:addr#Using_Address_Interpolation_for_partial_surveys",
	   "http://wiki.openstreetmap.org/wiki/Proposed_features/Industrial_Plant",
	   "http://wiki.openstreetmap.org/wiki/Tag:bridge%3Dyes",
	   "http://wiki.openstreetmap.org/wiki/Tag:supervised%3Dyes",
	   "http://wiki.openstreetmap.org/wiki/Tag:amenity%3Duniversity",
	   "http://wiki.openstreetmap.org/wiki/Proposed_Features/Importance",
	   "http://wiki.openstreetmap.org/wiki/Tag:shelter_type%3D",
	   "http://wiki.openstreetmap.org/wiki/Tag:amenity%3Dshelter",
	   "http://wiki.openstreetmap.org/wiki/Tag:amenity%3Drestaurant",
	   "http://wiki.openstreetmap.org/wiki/Proposed_features/Shop_(rather_than_amenity%3Dshoptype_above)" ]
	
	void testRedirection(){
		def uris = [ "http://wiki.openstreetmap.org/wiki/Proposed_features/hardware",
			"http://wiki.openstreetmap.org/wiki/Proposed_features/access_restrictions_1.5" ]
		//"http://en.wikipedia.org/wiki/ireland"]
		String res = ''
		uris.each{
			String uri = Crawler.getWikiRedirection( it )
			String uri2 = Utils.getLinkRedirection( it )
			res +=  "<b>$it</b><br/>==> (wikitext) $uri<br/>==> (http) $uri2<br/><br/>"
		}
		println res
	}
	
	void testWikitext(){
		def uris = [ "http://wiki.openstreetmap.org/wiki/Proposed_features/hardware",
					 "http://wiki.openstreetmap.org/wiki/Tag:shop%3Dhardware",
					"http://wiki.openstreetmap.org/wiki/Tag:highway%3Dsecondary" ]
				//"http://en.wikipedia.org/wiki/ireland"]
		String str = ''
		uris.each{ uri->
			def t = Crawler.buildTermFromWikiUrl( uri )
			str += "$uri <br/>==> $t <br/><br/>"
		}
		println str
	}
	
	void testTruncateUriFromActionRaw(){
		println "testTruncateUriFromActionRaw"
		def uris = ["http://wiki.openstreetmap.org/wiki/Key:abandoned?&action=raw", 
			"http://wiki.openstreetmap.org/wiki/Key:drink:*"]
		uris.each{uri->
			println uri
			def uriFixed = Crawler.truncateUriFromActionRaw( uri )
			println uriFixed
		}
	}
	
	
	void testBuildTermFromWikiPage(){
		
		String str = ''
		problematicUris.each{ uri->
			println ">>>>>> testBuildTermFromWikiPage URI = $uri"
			def t = Crawler.buildTermFromWikiUrl( uri )
			str += "<b>$uri</b> <br/>==> $t <br/><br/>"
		}
		println str
	}
	
	void testFixUris(){
		def invalidUris = [ 
		   "http://wiki.openstreetmap.org/wiki/Tag:ref:<qualifier>%3Doperator's_or_network's_reference",
		   "http://wiki.openstreetmap.org/wiki/Tag:waterway%3D[river,_stream,_canal,_drain,_ditch]",
		   "http://wiki.openstreetmap.org/wiki/Tag:name%3D<vehicle_type>_<reference_number>:_<initial_stop>_=>_<terminal_stop>",
		   "http://wiki.openstreetmap.org/wiki/Tag:region_type%3Dregion_category_value,_possible_values_for_region_type,administrativestate/county/township/city/village/province/prefecture,_legal2judicial_county/judicial_districtcadastral_(see_2)katastralgemeinde/grundstücktelephone3county_code_zone/area_code_zone/district_code_zone/postalzone_a_(first_digit)/_zone_b_(2nd_and_third_digit)*/_etc.political_(electoral)4riding/constituency/district/county/countrymaritime_(maritime_boundaries)5ocean/seemountain_rangemountain_range/massive/mountaindiscussvalleydiscussreligious6parish/deanery/diocese_/province/ecclesiastical_province_climatic_see_for_example_turkish_regions_wikipedia:regions_of_turkey",
		   "http://wiki.openstreetmap.org/wiki/Tag:name_(or_ref)%3D(..)" ]
		invalidUris.each{
			String fixed = Utils.fixUri(it)
			assertTrue( Utils.validateUrl(fixed) )
		}
	}
	
	void testGenerateRDF(){
		println("Generate test RDF statements...")
		
		def uris = [ "http://wiki.openstreetmap.org/wiki/Proposed_features/hardware",
			"http://wiki.openstreetmap.org/wiki/Key:amenity",
			"http://wiki.openstreetmap.org/wiki/Tag:sports%3Dathletics",
			"http://wiki.openstreetmap.org/wiki/Key:drink",
			"http://wiki.openstreetmap.org/wiki/Key:operator",
			//"http://wiki.openstreetmap.org/wiki/Key:",
			"http://wiki.openstreetmap.org/wiki/Tag:sport%3Dathletics",
			"http://wiki.openstreetmap.org/wiki/Proposed_features/wilderness_mountain_buildings",
			"http://wiki.openstreetmap.org/wiki/Tag:amenity%3Duniversity",
			"http://wiki.openstreetmap.org/wiki/Operator",
			//"http://wiki.openstreetmap.org/wiki/Map_features",
			"http://wiki.openstreetmap.org/wiki/Key:shelter_type",
			"http://wiki.openstreetmap.org/wiki/Tag:sport%3Dskating",
			"http://wiki.openstreetmap.org/wiki/Tag:landuse%3Dstreet",
			"http://wiki.openstreetmap.org/wiki/Key:landuse",
			"http://wiki.openstreetmap.org/wiki/Tag:area%3Dyes" ]
		uris.addAll(problematicUris)
		
		OsmOntology ontology = new OsmOntology()
		
		uris.each{ uri->
			OsmOntoTerm termFromUri = Crawler.buildTermFromWikiUrl(uri)
			Crawler.addTermToOntology( termFromUri, ontology )
		}
		
		
		ontology = Lgd.matchOsmOntoTermsWithLgd( ontology )
		WikiRdf.genRdfFiles( ontology )
		
		//def m = WikiRdf.buildJenaModelFromOntology( ontology )
		
	}
	
	public static void main(String[] args) {
		// TODO: fix test logic.
		OsmWikiCrawlerTests test = new OsmWikiCrawlerTests()
		/*test.testBuildTermFromWikiPage()
		test.testRedirection()
		test.testWikitext()
		test.testFixUris()
		test.testTruncateUriFromActionRaw()
		*/
		test.testGenerateRDF()
	}
}