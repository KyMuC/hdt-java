#!/bin/bash

# Warning load this file using:
# source `dirname $0`/javaenv.sh

BASE=$(dirname "$0")/../

export CP
case $(uname) in
  CYGWIN* | MINGW*)
    CP=$( echo "$BASE"/lib/*.jar | sed 's/ /;/g')
    ;;
  *)
    CP=$( echo "$BASE"/lib/*.jar | sed 's/ /:/g')
esac
#echo $CP

# Find Java
if [ "$JAVA_HOME" = "" ] ; then
    export JAVA="java"
else
    export JAVA="$JAVA_HOME/bin/java"
fi

# Set HDT Color options, set to true to allow color
if [ "$RDFHDT_COLOR" = "" ] ; then
    export RDFHDT_COLOR="false"
fi

# Set Java options
if [ "$JAVA_OPTIONS" = "" ] ; then
    export JAVA_OPTIONS="-Xmx1g"
fi
