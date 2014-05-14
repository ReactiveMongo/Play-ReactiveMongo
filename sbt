#!/bin/bash

. ./build-env

SBT_BOOT_DIR=$HOME/.sbt/boot/

if [ ! -d "$SBT_BOOT_DIR" ]; then
  mkdir -p $SBT_BOOT_DIR
fi

java -Dfile.encoding=UTF8 -Xmx512M -XX:+CMSClassUnloadingEnabled -XX:+UseCompressedOops -XX:MaxPermSize=768m \
        -Dhmrc.service=AUTH \
        $SBT_EXTRA_PARAMS \
        -Dbuild.time="`date`" \
        -Dsbt.boot.directory=$SBT_BOOT_DIR \
        -jar $SBT_FILE "$@"
