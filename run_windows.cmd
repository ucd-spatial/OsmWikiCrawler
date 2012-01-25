REM	  This script runs OSM Wiki Crawler on Microsoft Windows XP/Vista/7.

REM	  This file is part of the OSM Wiki Crawler.
REM   The OSM Wiki Crawler is free software: you can redistribute it and/or modify
REM   it under the terms of the GNU General Public License as published by
REM   the Free Software Foundation, either version 3 of the License, or
REM   (at your option) any later version.
REM
REM   The OSM Wiki Crawler is distributed in the hope that it will be useful,
REM   but WITHOUT ANY WARRANTY; without even the implied warranty of
REM   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
REM   GNU General Public License for more details.
REM
REM   You should have received a copy of the GNU General Public License
REM   along with the OSM Wiki Crawler.  If not, see <http://www.gnu.org/licenses/>.
REM  
REM   Author: Andrea Ballatore
REM   Project home page: http://gforge.ucd.ie/projects/osm-similarity/

java -cp "jar/osmwikicrawler-0.1.jar;lib/arq-2.8.8.jar;lib/jakarta-oro-2.0.8.jar;lib/commons-validator-1.4-SNAPSHOT.jar;lib/jena-2.6.4.jar;lib/log4j-1.2.14.jar;lib/commons-lang-2.4.jar;lib/slf4j-api-1.5.8.jar;lib/slf4j-log4j12-1.5.8.jar;lib/junit-4.10.jar;lib/tagsoup-1.2.jar;lib/wstx-asl-3.2.9.jar;lib/iri-0.8.jar;lib/xercesImpl-2.7.1.jar;lib/xom.jar;lib/icu4j-3.4.4.jar;lib/groovy-all-1.7.8.jar" org.ucd.osmwikicrawler.RunOsmWikiCrawler