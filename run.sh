#!/bin/bash
echo "***********************"
# NEOLIB=lib/neo4j-1.9.M03/lib
# NEOLIB=lib/neo4j-community-1.9.M05/lib
NEOLIB=lib/neo4j-community-1.9.RC1/lib
CLASSPATH=$(echo $NEOLIB/*.jar | tr ' ' ':'):$CLASSPATH
# CLASSPATH=$(echo lib/*.jar | tr ' ' ':'):

EXEC="ImportML"

T="$(date +%s)"

javac -classpath $CLASSPATH $EXEC.java && java -Xmx6g -cp $CLASSPATH $EXEC

# Time elapsed  After work
T="$(($(date +%s)-T))"

echo ""
echo "Time in Seconds: ${T}"
