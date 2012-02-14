=====================================================================
	OpenStreetMap Wiki Crawler
=====================================================================

The OpenStreetMap Wiki Crawler is an open source tool that extracts
the OpenStreetMap Semantic Network from the OpenStreetMap Wiki website 
(http://wiki.openstreetmap.org).
Pre-extracted RDF graphs are available at
http://github.com/ucd-spatial/OsmSemanticNetwork


---------------------------------------------------------------------
	How to get Java
---------------------------------------------------------------------
To run the OpenStreetMap Wiki Crawler, Java 1.6+ has to be installed. 
The Java Runtime Enviroment can be downloaded at 
http://www.java.com/en/download

---------------------------------------------------------------------
	How to launch the precompiled jar
---------------------------------------------------------------------
* Linux / Unix / Mac OS X: open a shell, go to the file directory and type
	"./run_unix.sh".

* Microsoft Windows: open a command terminal, go to the file directory
	and run "./run_windows.sh".

---------------------------------------------------------------------
	How to get the Source
---------------------------------------------------------------------

The source code is available on GitHub: http://github.com/ucd-spatial

---------------------------------------------------------------------
	How to compile the source code
---------------------------------------------------------------------

The project was developed in the Eclipse IDE
* http://www.eclipse.org
With the Groovy plugin:
* http://groovy.codehaus.org/Eclipse+Plugin

The project can be imported into Eclipse through the 
"import existing projects" function.

The file build.xml contains Apache ANT commands to compile and run 
the crawler: http://ant.apache.org 

---------------------------------------------------------------------
	License
---------------------------------------------------------------------

See LICENSE.txt.

---------------------------------------------------------------------
	Author and Contact
---------------------------------------------------------------------
Author: Andrea Ballatore (School of Computer Science and Informatics,
	University College Dublin)
Project home page: http://github.com/ucd-spatial

---------------------------------------------------------------------
	External Libraries
---------------------------------------------------------------------
The OSM Wiki Crawler is dependent upon the following open source 
libraries (see 'lib' folder for the licenses):

groovy-all-1.7.8
arq-2.8.8
commons-lang-2.4
commons-validator-1.4
icu4j-3.4.4
iri-0.8
jakarta-oro-2.0.8
jena-2.6.4
junit-4.10
log4j-1.2.14
slf4j-api-1.5.8
slf4j-log4j12-1.5.8
tagsoup-1.2
wstx-asl-3.2.9
xercesImpl-2.7.1
xom
---------------------------------------------------------------------