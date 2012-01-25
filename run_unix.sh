#!/bin/bash

# 	This script runs the OSM Wiki Crawler on Linux/Unix/Mac OS X.

#	This file is part of the OSM Wiki Crawler.
#   The OSM Wiki Crawler is free software: you can redistribute it and/or modify
#   it under the terms of the GNU General Public License as published by
#   the Free Software Foundation, either version 3 of the License, or
#   (at your option) any later version.
#
#   The OSM Wiki Crawler is distributed in the hope that it will be useful,
#   but WITHOUT ANY WARRANTY: without even the implied warranty of
#   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#   GNU General Public License for more details.
#
#   You should have received a copy of the GNU General Public License
#   along with the OSM Wiki Crawler.  If not, see <http://www.gnu.org/licenses/>.
#  
#   Author: Andrea Ballatore
#   Project home page: http://gforge.ucd.ie/projects/osm-similarity/

java -cp "jar/osmwikicrawler-0.1.jar:lib/arq-2.8.8.jar:lib/jakarta-oro-2.0.8.jar:lib/commons-validator-1.4-SNAPSHOT.jar:lib/jena-2.6.4.jar:lib/log4j-1.2.14.jar:lib/slf4j-api-1.5.8.jar:lib/commons-lang-2.4.jar:lib/slf4j-log4j12-1.5.8.jar:lib/junit-4.10.jar:lib/tagsoup-1.2.jar:lib/wstx-asl-3.2.9.jar:lib/iri-0.8.jar:lib/xercesImpl-2.7.1.jar:lib/xom.jar:lib/icu4j-3.4.4.jar:lib/groovy-all-1.7.8.jar"  org.ucd.osmwikicrawler.RunOsmWikiCrawler
