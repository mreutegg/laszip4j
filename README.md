# laszip4j

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.mreutegg/laszip4j/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.mreutegg/laszip4j/) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=mreutegg_laszip4j&metric=alert_status)](https://sonarcloud.io/dashboard?id=mreutegg_laszip4j) [![Build Status](https://mreutegg.visualstudio.com/laszip4j/_apis/build/status/mreutegg.laszip4j)](https://mreutegg.visualstudio.com/laszip4j/_build/latest?definitionId=1) 

The LASzip library ported to Java

This is a Java port of the [LASzip](https://github.com/LASzip/LASzip) and 
[LAStools](https://github.com/LAStools/LAStools) libraries by Martin Isenburg ([RIP](https://lidarmag.com/2021/10/30/in-memoriam-martin-isenburg-1972-2021/)).

The port to Java is *not* complete. Many classes are stubs only and have not
been ported. The main driver was to have a Java implementation of the laszip 
utility and be able to unpack LAZ files for 
[Canton of Zurich](http://geolion.zh.ch/geodatensatz/2618)
in Switzerland, which works fine.

Usage is the same as with the native laszip utility, but invoked as a runnable
jar:

    java -jar laszip4j-0.18.jar -oparse xyzc -keep_class 3 4 5 6 10 -i 7015_2640.laz -o 7015_2640.xyzk
    
On top of LASzip and LAStools this library also provides convenience classes
for reading LAS points. You can read LAS points in your code like this:

        LASReader reader = new LASReader(new File("data.laz"));
        for (LASPoint p : reader.getPoints()) {
            // read something from point
            p.getClassification();
        }

See also [LASReaderTest](src/test/java/com/github/mreutegg/laszip4j/LASReaderTest.java)
on how to use the LASReader class.

As of version 0.13, laszip4j also supports writing las files. In the simplest
case the executable jar file can be used to read compressed data from a laz
file and write it to a corresponding las file.

    java -jar laszip4j-0.18.jar -i 7015_2640.laz -o 7015_2640.las

[!["Buy Me A Coffee"](https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png)](https://www.buymeacoffee.com/mreutegg)
