#! /bin/sh

SCRIPT_DIR=`dirname $0`

# TRAVIS_BRANCH
if [ ! "x$TRAVIS_TAG" = "x" -o "$SONATYPE_USER" = "" -o "$SONATYPE_PASS" = "" ]; then
    echo "skip the snapshot publication"
    exit 0
fi

cd "$SCRIPT_DIR/.."

export PUBLISH_REPO_NAME="Sonatype Nexus Repository Manager"
export PUBLISH_REPO_ID="oss.sonatype.org"
export PUBLISH_REPO_URL=https://oss.sonatype.org/content/repositories/snapshots/
export PUBLISH_USER="$SONATYPE_USER"
export PUBLISH_PASS="$SONATYPE_PASS"

sbt '+publish'
