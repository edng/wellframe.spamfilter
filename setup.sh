#!/bin/sh
wget https://boilerpipe.googlecode.com/files/boilerpipe-1.2.0-bin.tar.gz
tar xvfz boilerpipe-1.2.0-bin.tar.gz
mvn install:install-file -Dfile=boilerpipe-1.2.0/boilerpipe-1.2.0.jar -DgroupId=de.l3s.boilerpipe -DartifactId=boilerpipe -Dversion=1.2.0 -Dpackaging=jar
rm -fr boilerpipe-1.2.0*
