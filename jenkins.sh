#!/bin/bash

SBT_BOOT_DIR=${HOME}/.sbt/boot/

VERSION=`grep "pluginVersion =" project/Play-ReactiveMongo.scala | awk '{print $4}' | sed -e 's/^"//'  -e 's/"$//'`

if [[ $VERSION == *SNAPSHOT* ]]
then
  SBT_TARGETS="clean test publishSigned"
else
  SBT_TARGETS="clean test publishSigned sonatypeRelease"
fi

SBT_STANDARD_OPTS="-Dsbt.log.noformat=true -Xmx1024M -XX:+CMSClassUnloadingEnabled -XX:+UseCompressedOops -XX:MaxPermSize=768m  -Dfile.encoding=UTF8"

if [ -f "/etc/buildrc" ]; then
  . /etc/buildrc
else
  . ./build-env
fi

scriptname=$(basename $0)
echo "[${scriptname}] Using ivy directory ${HOME}/.ivy2"
echo "[${scriptname}] java  ${SBT_STANDARD_OPTS} ${SBT_EXTRA_PARAMS} -jar ${SBT_FILE} ${SBT_TARGETS}"

if [ ! -d "${SBT_BOOT_DIR}" ]; then
  mkdir -p ${SBT_BOOT_DIR}
fi

java    ${SBT_STANDARD_OPTS} ${SBT_EXTRA_PARAMS} \
        -Dbuild.time="`date`" -Dsbt.boot.directory=${SBT_BOOT_DIR} \
        -jar ${SBT_FILE} ${SBT_TARGETS}

