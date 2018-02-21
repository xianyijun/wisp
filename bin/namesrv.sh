#!/usr/bin/env bash

if [ -z "$WISP_HOME" ] ; then
  ## resolve links - $0 may be a link to maven's home
  PRG="$0"

  # need this for relative symlinks
  while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
      PRG="$link"
    else
      PRG="`dirname "$PRG"`/$link"
    fi
  done

  saveddir=`pwd`

  WISP_HOME=`dirname "$PRG"`/..

  # make it fully qualified
  WISP_HOME=`cd "$WISP_HOME" && pwd`

  cd "$saveddir"
fi

export WISP_HOME

sh ${WISP_HOME}/bin/runserver.sh cn.xianyijun.wisp.namesrv.NameServerStartUp $@
