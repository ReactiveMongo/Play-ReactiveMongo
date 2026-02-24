#!/usr/bin/env bash
  
set -e

S2_12="2.12"
S2_11="2.11"
S2_13="2.13"
S3="3.3"

unset REACTIVEMONGO_SHADED

TASKS=";makePom ;packageBin ;packageSrc ;packageDoc"

export RELEASE_SUFFIX=play26 PLAY_VERSION=2.6.7
sbt $SBT_OPTS ++${S2_11} "$TASKS" ++${S2_12} "$TASKS"

export RELEASE_SUFFIX=play27 PLAY_VERSION=2.7.4
sbt $SBT_OPTS ++${S2_11} "$TASKS" ++${S2_12} "$TASKS" ++${S2_13} "$TASKS"

export RELEASE_SUFFIX=play28 PLAY_VERSION=2.8.1
sbt $SBT_OPTS ++${S2_12} "$TASKS" ++${S2_13} "$TASKS"

export RELEASE_SUFFIX=play29 PLAY_VERSION=2.9.1
sbt $SBT_OPTS ++${S2_13} "$TASKS"

export RELEASE_SUFFIX=play30 PLAY_VERSION=3.0.5
sbt $SBT_OPTS ++${S2_13} "$TASKS" ++${S3} "$TASKS"
