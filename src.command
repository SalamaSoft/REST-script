#!/usr/bin/env bash

# get directory path ---------------------
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

/Applications/Eclipse.app/Contents/MacOS/eclipse -data $PRGDIR/src -vmargs -Xms256m -Xmx1024m &
