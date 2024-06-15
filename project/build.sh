#!/usr/bin/env bash
  
set -e

S2_12="2.12.17"
S2_11="2.11.12"
S2_13="2.13.14"
S3="3.4.2"

unset REACTIVEMONGO_SHADED

export RELEASE_SUFFIX=play25 PLAY_VERSION=2.5.7
sbt $SBT_OPTS ++${S2_11} clean makePom packageBin packageDoc packageSrc

export RELEASE_SUFFIX=play26 PLAY_VERSION=2.6.7
sbt $SBT_OPTS ++${S2_11} makePom packageBin packageDoc packageSrc \
    ++${S2_12} makePom packageBin packageDoc packageSrc

export RELEASE_SUFFIX=play27 PLAY_VERSION=2.7.3
sbt $SBT_OPTS ++${S2_11} makePom packageBin packageDoc packageSrc \
    ++${S2_12} makePom packageBin packageDoc packageSrc \
    ++${S2_13} makePom packageBin packageDoc packageSrc

export RELEASE_SUFFIX=play28 PLAY_VERSION=2.8.1
sbt $SBT_OPTS ++${S2_12} makePom packageBin packageSrc packageDoc \
  ++${S2_13} makePom packageBin packageSrc packageDoc \

export RELEASE_SUFFIX=play29 PLAY_VERSION=2.9.1
sbt $SBT_OPTS ++${S2_13} makePom packageBin packageSrc packageDoc \
  ++${S3} makePom packageBin packageSrc packageDoc
