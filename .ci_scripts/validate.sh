#! /bin/bash

set -e

SCRIPT_DIR=`dirname $0 | sed -e "s|^\./|$PWD/|"`

cd "$SCRIPT_DIR/.."

sbt ++$SCALA_VERSION ';scalafixAll -check ;scalafmtAll'

git diff --exit-code || (
  echo "ERROR: Scalafmt check failed, see differences above."
  echo "To fix, format your sources using ./build scalafmtAll before submitting a pull request."
  echo "Additionally, please squash your commits (eg, use git commit --amend) if you're going to update this pull request."
  false
)

TEST_OPTS=""

if [ `echo "$SCALA_VERSION" | sed -e 's/^3.*/3/'` = "3" ]; then
  TEST_OPTS="-- exclude not_scala3"
fi

sbt ++$SCALA_VERSION ";error ;test:compile ;warn ;testOnly $TEST_OPTS"
