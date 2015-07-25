#! /usr/bin/env sh

set -e

SCRIPT_DIR=`dirname $0 | sed -e "s|^\./|$PWD/|"`

cd "$SCRIPT_DIR/.."

sbt ++$TRAVIS_SCALA_VERSION scalariformFormat test:scalariformFormat
git diff --exit-code || (
  echo "ERROR: Scalariform check failed, see differences above."
  echo "To fix, format your sources using ./build scalariformFormat test:scalariformFormat before submitting a pull request."
  echo "Additionally, please squash your commits (eg, use git commit --amend) if you're going to update this pull request."
  false
)


# Sonatype staging (avoid Central sync delay)
sed -e 's|libraryDependencies |resolvers += "Sonatype Staging" at "https://oss.sonatype.org/content/repositories/staging/", libraryDependencies |' < "$SCRIPT_DIR/../project/Play2-ReactiveMongo.scala" > /tmp/Play2-ReactiveMongo.scala && mv /tmp/Play2-ReactiveMongo.scala "$SCRIPT_DIR/../project/Play2-ReactiveMongo.scala"

sbt ++$TRAVIS_SCALA_VERSION "testOnly *"
