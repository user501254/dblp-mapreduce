DBLP MapReduce — Processing dblp dataset using Apache Hadoop
============================================================

This project is a collection of Map reduce jobs process DBLP dataset to produce stats on various fields.
Processing can happen over a single field or a set of fields, all defined by the user at runtime.
All outputs are tab separated values (TSV) an can be used for visualization of output.

Extra care has been taken for seamless processing of data on AWS S3 and GCS.
Alternatively, https://github.com/user501254/BD_STTP_2016.git can be used for semi-automated 
local Hadoop setup or for setup on AWS Amazon Elastic Compute Cloud (EC2), Google Compute Engine (GCE).

-----
INDEX
-----

1. [About the DBLP dataset](#1-about-the-dblp-dataset)
2. [Some Important Files](#2-some-important-files)
3. [Application Design](#3-application-design)
4. [Setup Instructions](#4-setup-instructions)
5. [Usage Details](#5-usage-details)
6. [Visualization and Demo](#6-visualization-and-demo)



1\. About the DBLP dataset
--------------------------

Extracting data from dblp F.A.Q:

Q. How can I download the whole dblp dataset?  
A:    https://dblp.org/faq/1474679.html  

Q. What do I find in dblp.xml?  
A:    https://dblp.org/faq/16154937.html

Q. How are data annotations used in dblp.xml?  
A:    https://dblp.org/faq/17334559.html
    
Q. How to parse dblp.xml?  
A:    https://dblp.org/faq/1474681.html

The dblp.xml is a simple, plain ASCII XML file, using the named entities as given in the accompanying dblp.dtd file.
A daily updated (but unversioned) XML dump can be found on the dblp web server: 
    https://dblp.org/xml/
```text
dblp.xml.gz		                   compressed (gzip) version of an XML file with all bibliographic records
dblp.dtd 		                   the Document Type Definition (DTD) required to validate the XML file
```

Furthermore, each month, a persistent snapshot release is archived:
    https://dblp.org/xml/release/

More information on the XML structure of the dblp records and several design decisions can be found in this paper:
    https://dblp.uni-trier.de/xml/docu/dblpxml.pdf



2\. Some Important Files
------------------------

```text
hw2/src/main/resources/bin
    dtd2xsd.pl                              perl script to convert DTD to XML Schema Definition (XSD)
    inspectdblp.sh                          bash script to extract counts for bibliographic records

hw2/src/main/java/com/ashessin/cs441/hw2/dblp/utils
    ExtractLocalGzipFile.java               extracts a gzip file
    PublicationsSequenceFileWriter.java     writes the publication records SequenceFile on HDFS by parsing the XML
    PublicationsSequenceFileReader.java     reads the publication records SequenceFile from the HDFS
    PublicationWritable.java                to serialize and deserialize publication record fields

hw2/src/main/java/com/ashessin/cs441/hw2/dblp/mr/
    SingleFieldCount.java                   counts the number of occurrences of a field value
    JoinedFieldsCount.java                  counts the number of occurrences of a field value set
    PrimaryFieldCount.java                  counts the number of occurrences of the primary (ie. fist) field value
    SwapSortKeyValuePairs.java              swaps key value pairs and then sorts (descending)
```



3\. Application Design
----------------------

First, the input dblp dataset is parsed using StAX API to process bibliographic child record elements into
`com.ashessin.cs441.hw2.dblp.utils.PublicationWritable` objects having key, publrecord, publtype fields present.
Other relevant object fields are empty at this point.

The bibliographic child record elements may be any of the below:

    article, inproceedings, proceedings, book, incollection, phdthesis, mastersthesis, www, data

To fill the remaining object fields, after any of the above are encountered as a `javax.xml.stream.events.StartElement`,
we process subsequent events to look for its child elements (or grandchild of XML root, dblp).

The fields in use are:  

    authors, editors, year, journal, urls, ees, cites, crossref, schools

For simplicity, we only use `String` or `List<String>` datatype for these fields, with the exception of the field year,
which is an `int`. The year is set to -1 if it's not present for the bibliographic records, rest all are set to empty
when missing.

This simplification was done based on the inspection of data through `hw2/src/main/resources/bin/inspectdblp.sh`

    Inspecting ../dblp-2019-10-01.xml for tags.
    Found 7151497 publication elements.

    Counting each child of the 'dblp' root element...
    'article': 2115563
    'inproceedings': 2473542
    'proceedings': 42142
    'book': 17792
    'incollection': 59913
    'phdthesis': 73841
    'mastersthesis': 12
    'www': 2368692
    'person': 0
    'data': 0

    Counting each grandchild of the 'dblp' root element...
    'author': 16613178
    'editor': 104297
    'title': 7151148
    'booktitle': 2576947
    'pages': 4240700
    'year': 4782819
    'address': 3
    'journal': 2115340
    'volume': 2137824
    'number': 1608299
    'month': 10943
    'url': 4868873
    'ee': 5617166
    'cdrom': 12962
    'cite': 172746
    'publisher': 64563
    'crossref': 2533467
    'isbn': 65813
    'series': 27663
    'school': 76369
    'chapter': 2
    'publnr': 0

    Counting identical sibling(s) that are grandchildren of the 'dblp' root element...
    '</author>...<author>': 11006018
    '</editor>...<editor>': 86064
    '</title>...<title>': 0
    '</booktitle>...<booktitle>': 0
    '</pages>...<pages>': 16
    '</year>...<year>': 0
    '</address>...<address>': 0
    '</journal>...<journal>': 0
    '</volume>...<volume>': 0
    '</number>...<number>': 0
    '</month>...<month>': 0
    '</url>...<url>': 121338
    '</ee>...<ee>': 1882196
    '</cdrom>...<cdrom>': 850
    '</cite>...<cite>': 97280
    '</publisher>...<publisher>': 2
    '</crossref>...<crossref>': 2
    '</isbn>...<isbn>': 11794
    '</series>...<series>': 0
    '</school>...<school>': 1388
    '</chapter>...<chapter>': 0
    '</publnr>...<publnr>': 0


Once a single bibliographic record (with its child elements) has been processed and made into an object, it is
serialized to the HDFS as sequence files. Further, block compression is used with default codec to append each new
object record. This sequence file acts as input for all subsequent Map Reduce jobs.

As a sample, if the included `hw2/src/main/resources/dblp-tiny.xml` file is passed, the objects  are written to HDFS and
their `toString()` representation gives:
```log
....
INFO  PublicationsSequenceFileReader - tr/meltdown/s18	key="tr/meltdown/s18", publrecord="article", publtype="informal", authors=[Paul Kocher, Daniel Genkin, Daniel Gruss, Werner Haas, Mike Hamburg, Moritz Lipp, Stefan Mangard, Thomas Prescher 0002, Michael Schwarz 0001, Yuval Yarom]", editors=[], year=2018, journal="meltdownattack.com", urls=[], ees=[https://spectreattack.com/spectre.pdf], cites=[], crossref="", schools=[]
INFO  PublicationsSequenceFileReader - tr/meltdown/m18	key="tr/meltdown/m18", publrecord="article", publtype="informal", authors=[Moritz Lipp, Michael Schwarz 0001, Daniel Gruss, Thomas Prescher 0002, Werner Haas, Stefan Mangard, Paul Kocher, Daniel Genkin, Yuval Yarom, Mike Hamburg]", editors=[], year=2018, journal="meltdownattack.com", urls=[], ees=[https://meltdownattack.com/meltdown.pdf], cites=[], crossref="", schools=[]
INFO  PublicationsSequenceFileReader - tr/acm/CS2013	key="tr/acm/CS2013", publrecord="book", publtype="", authors=[]", editors=[], year=2013, journal="", urls=[], ees=[https://doi.org/10.1145/2534860], cites=[], crossref="", schools=[]
INFO  PublicationsSequenceFileReader - phd/Phipps92	key="phd/Phipps92", publrecord="phdthesis", publtype="", authors=[Geoffrey Phipps]", editors=[], year=1992, journal="", urls=[], ees=[], cites=[], crossref="", schools=[Stanford University, Department of Computer Science]
INFO  PublicationsSequenceFileReader - phd/Breuer2000	key="phd/Breuer2000", publrecord="phdthesis", publtype="", authors=[Lothar Breuer]", editors=[], year=2000, journal="", urls=[], ees=[http://ubt.opus.hbz-nrw.de/volltexte/2004/119/], cites=[], crossref="", schools=[Univ. Trier, FB 4, Informatik]
INFO  PublicationsSequenceFileReader - phd/Hofmann1999	key="phd/Hofmann1999", publrecord="phdthesis", publtype="", authors=[Jens Hofmann 0001]", editors=[], year=1999, journal="", urls=[], ees=[http://ubt.opus.hbz-nrw.de/volltexte/2004/148/, http://d-nb.info/957663463], cites=[], crossref="", schools=[University of Trier, FB 4 Informatik, Germany]
INFO  PublicationsSequenceFileReader - phd/Ordille93	key="phd/Ordille93", publrecord="phdthesis", publtype="", authors=[Joann J. Ordille]", editors=[], year=1993, journal="", urls=[], ees=[], cites=[], crossref="", schools=[Univ. of Wisconsin-Madison]
INFO  PublicationsSequenceFileReader - phd/Beferull-Lozano02	key="phd/Beferull-Lozano02", publrecord="phdthesis", publtype="", authors=[Baltasar Beferull-Lozano]", editors=[], year=2002, journal="", urls=[], ees=[http://www.uv.es/%7Ebalbelo/thesis_BBL.pdf], cites=[], crossref="", schools=[University of Southern California]
INFO  PublicationsSequenceFileReader - phd/Smolka89	key="phd/Smolka89", publrecord="phdthesis", publtype="", authors=[Gert Smolka]", editors=[], year=1989, journal="", urls=[], ees=[http://d-nb.info/891234179], cites=[], crossref="", schools=[Kaiserslautern University of Technology, Germany]
INFO  PublicationsSequenceFileReader - phd/Dobry87	key="phd/Dobry87", publrecord="phdthesis", publtype="", authors=[Tep P. Dobry]", editors=[], year=1987, journal="", urls=[], ees=[], cites=[], crossref="", schools=[University of California at Berkeley]
INFO  PublicationsSequenceFileReader - phd/Ghemawat95	key="phd/Ghemawat95", publrecord="phdthesis", publtype="", authors=[Sanjay Ghemawat]", editors=[], year=1995, journal="", urls=[], ees=[http://hdl.handle.net/1721.1/37012], cites=[], crossref="", schools=[MIT Laboratory for Computer Science, Cambridge, MA, USA]
INFO  PublicationsSequenceFileReader - phd/Rothkugel2002	key="phd/Rothkugel2002", publrecord="phdthesis", publtype="", authors=[Steffen Rothkugel]", editors=[], year=2002, journal="", urls=[], ees=[http://ubt.opus.hbz-nrw.de/volltexte/2004/210/, http://nbn-resolving.de/urn:nbn:de:hbz:385-2109, http://d-nb.info/971737843], cites=[], crossref="", schools=[Univ. Trier, FB 4, Informatik]
INFO  PublicationsSequenceFileReader - phd/sk/Frisch2009	key="phd/sk/Frisch2009", publrecord="phdthesis", publtype="", authors=[Guido Frisch]", editors=[], year=2009, journal="", urls=[], ees=[http://d-nb.info/996716548], cites=[], crossref="", schools=[Bratislava, Univ.]
....
```

All Map Reduce jobs extends a simple count program to compute numbers across mutiple fields values and field value sets.

    `SingleFieldCount` computes counts over a single field value across serialized dblp records.
    
    For example on the field 'year' (ie. Number of publications per year):
        Text IntWritable
        2018 78
        2019 76
        1999 23
        ...
    
    For field 'authors' (ie. Number of publications per author):
        Text                IntWritable
        Paul Kocher         2
        Daniel Genkin       2
        ...
    
    `JoinedFieldsCount` computes counts over values of a field set (up till 3) across serialized dblp records.
    The order of fields doesn't really matter for our use cases if we later sort the result based on count.
    However
    Also, we can give any combination of  `String`, `List<String>`, `int` (for year) type.
    A Cartesian Product is caluclated over individual fields and then they are joined using '\t'. 
    This gives us multiple `Text` type key which conatains all the fields values.
    
    eg. fieldset 'year,publrecord'             (ie. histogram of publication record per year):
    eg. fieldset 'authors,authors'             (ie. most prolific author pairs):
    eg. fieldset 'authors,authors,year'        (ie. most prolific author pairs each year):
    eg. fieldset 'authors,authors,authors'     (ie. most prolific author triplets):
    
    `PrimaryFieldCount` works on the output of `JoinedFieldsCount` to aggregate horixonatlly acosst fied value in a set.
    This can be used to compute number of co-authors per author etc.
    The workflow in that case would be:
        (sequnce_file, 'authors,authors') ->  JoinedFieldsCount -> (Text(author1\tauthor2), IntWritable(Count)) ->
            PrimaryFieldCount -> number of co-authors per author

Intermediate results are stored in the HDFS as compressed sequence files and the final output can be extracted to the
local filesystem through  CopyHdfsFileToLocal class, which takes HDFS or the defualt file system path as input.
(All project files have Javadoc comments and loggers, please feel free to see them in case of doubt.)



4\. Setup Instructions
----------------------

Something to note, absolute paths are almost always preferred. Make sure to use correct file system URI.
For example, if the file 'hw2/src/main/resources/dblp.xml' is on a ordinary filesystem, within the users home directory,
use: `file://$HOME/hw2/src/main/resources/dblp.xml`

Similarly, for file on HDFS use, `hdfs://$HOSTNAME:PORT/some-path`

For S3 bucket use, `s3://bucket-name/some-path`

3. Setup Hadoop using bootstrap script and start all services

        git clone "https://github.com/user501254/BD_STTP_2016.git"; cd BD_STTP_2016; chmod +x *.sh; InstallHadoop.sh
        start-all.sh

1. Clone this repository

        git clone https://asing80@bitbucket.org/asing80/hw2.git

2. Download and extract dataset

        wget https://dblp.uni-trier.de/xml/dblp-2019-10-01.xml.gz -O hw2/src/main/resources/dblp.xml.gz
        gunzip dblp.xml.gz

4. Run jar file on hadoop with the downloaded dataset

        hadoop jar hw2/target/scala-2.12/hw2-assembly-0.1.jar PublicationsSequenceFileWriter \
            hw2/src/main/resources/dblp.xml \
            hw2/src/main/resources/publications.sequnce.deflate

Output part-r-* files will be saved to the same folder within subdirectories.
For AWS EMR, please follow these steps:
	https://docs.aws.amazon.com/emr/latest/ReleaseGuide/emr-launch-custom-jar-cli.html
Both input and output S3 bucket locations should be passed to the JAR file and must be accessible.



5\. Usage Details
-----------------

```text
java com.ashessin.cs441.hw2.dblp.Start <option> [absolute_input_path[,absolute_output_path]] [arg1[,arg2,arg3]]

<option>:
	[-w, PublicationsSequenceFileWriter | 
	 -r, PublicationsSequenceFileReader | 
	 -c, CopyHdfsFileToLocal | 
	 -e, ExtractLocalGzipFile | 
	     SingleFieldCount | 
	     JoinedFieldsCount | 
	     PrimaryFieldCount | 
	     SwapSortKeyValuePairs
	 --configFile]


[absolute_input_path[,absolute_output_path]]:
    fully qualifed file and/or directory URI.

    example paths:
        # unzipped dblp.xml file
        file:///absolute-path-to-dblp.xml
        # unzipped dblp.xml file on AWS S3
        s3://bucket-name/absolute-path-to-dblp.xml
        # unzipped dblp.xml file on Google Storage
        gs://bucket-name/absolute-path-to-dblp.xml
        # compressed sequece file on hdfs
        hdfs://localhost:9000/absolute-path-to-dblp.sequnce.deflate
        # compressed sequece file on AWS S3
        s3://bucket-name/absolute-path-to-dblp.sequnce.deflate
        # unzipped dblp.xml file on Google Storage
        gs://bucket-name/absolute-path-to-dblp.sequnce.deflate
        # output directory for part-r-* files
        hdfs://localhost:9000/absolute-path-to-output-directory/
        s3://bucket-name/absolute-path-to-output-directory/
        gs://bucket-name/absolute-path-to-output-directory/


[arg1[,arg2,arg3]]
    any combination of:
        key publrecord publtype authors editors year journal urls ees cites crossref schools


For execution through custom config file, please see src/main/resources/reference.conf for fields.

examples:

	java com.ashessin.cs441.hw2.dblp.Start --configFile \
    	   file:///absolute-path-to-config-file.conf

	java com.ashessin.cs441.hw2.dblp.Start \
    	-w file:///absolute-path-to-dblp.xml \
	       hdfs://localhost:9000/absolute-path-to-dblp.sequnce.deflate

	java com.ashessin.cs441.hw2.dblp.Start \
	    -r hdfs://localhost:9000/absolute-path-to-dblp.sequnce.deflate

	java com.ashessin.cs441.hw2.dblp.Start SingleFieldCount \
	    hdfs://localhost:9000/absolute-path-to-dblp.sequnce.deflate \
	    hdfs://localhost:9000/absolute-path-to-output-directory-1/ \
	    authors

	java com.ashessin.cs441.hw2.dblp.Start JoinedFieldCount \
	    hdfs://localhost:9000/absolute-path-to-dblp.sequnce.deflate \
	    hdfs://localhost:9000/absolute-path-to-output-directory-2/ \
	    authors,year

	java com.ashessin.cs441.hw2.dblp.Start JoinedFieldCount \
	    hdfs://localhost:9000/absolute-path-to-dblp.sequnce.deflate \
	    hdfs://localhost:9000/absolute-path-to-output-directory-3/ \
	    authors,authors

	java com.ashessin.cs441.hw2.dblp.Start JoinedFieldCount \
	    hdfs://localhost:9000/absolute-path-to-dblp.sequnce.deflate \
	    hdfs://localhost:9000/absolute-path-to-output-directory-4/ \
	    authors,authors,year

	java com.ashessin.cs441.hw2.dblp.Start PrimaryFieldCount \
	    hdfs://localhost:9000/absolute-path-to-output-directory-3/
	    hdfs://localhost:9000/absolute-path-to-output-directory-5/

	java com.ashessin.cs441.hw2.dblp.Start SwapSortKeyValuePairs \
	    hdfs://localhost:9000/absolute-path-to-output-directory-5/
	    hdfs://localhost:9000/absolute-path-to-output-directory/
```



6\. Visualization and Demo
-------------------------

The program in its default configuration produces a number of TSV files, which can used for visualization.
Some samples along with demo for Amaon EMR and Google Compute Engine deployment are available at:  
	https://asing80.people.uic.edu/cs441/hw2/ 
