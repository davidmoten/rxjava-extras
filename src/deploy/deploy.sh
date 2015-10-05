#!/bin/bash

set -e

# Get the Java version.
# Java 1.5 will give 15.
# Java 1.6 will give 16.
# Java 1.7 will give 17.
# Java 1.8 will give 18
WHOLE_VER=`java -version 2>&1`
VER=`java -version 2>&1 | sed 's/java version "\(.*\)\.\(.*\)\..*"/\1\2/; 1q'`

# We build for several JDKs on Travis.
# Oracle 8 => deploy artifacts on a Maven repository.

# If the version is Java 1.8, then perform the following actions.
# 1. Copy Maven settings on the VM.
# 2. Upload artifacts to Sonatype.
# 3. Use -q option to only display Maven errors and warnings.
# 4. Use --settings to force the usage of our "settings.xml" file.
# 5. Enable the profile that generates the javadoc and the sources archives.

if [[ $VER == "18" ]]; then
    python src/deploy/addServer.py
    echo "MAVEN_OPTS='-Xmx512m'" > ~/.mavenrc
    mvn clean deploy --settings ~/.m2/mySettings.xml -Darguments="-Dmaven.test.skip=true"
else
	echo "No action to undertake (not OracleJava 8)."
fi
