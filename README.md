# ReactiveMongo Support to Play! Framework 2.4

This is a plugin for Play 2.4, enabling support for [ReactiveMongo](http://reactivemongo.org) - reactive, asynchronous and non-blocking Scala driver for MongoDB.

## Usage

In your `project/Build.scala`:

```scala
// only for Play 2.4.x
libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.3.play24")
```

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.reactivemongo/play2-reactivemongo_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.reactivemongo/play2-reactivemongo_2.11/)

## Build manually

ReactiveMongo for Play2 can be built from this source repository.

    sbt publish-local

To run the tests, use:

    sbt test

[Travis](https://travis-ci.org/ReactiveMongo/Play-ReactiveMongo): ![Travis build status](https://travis-ci.org/ReactiveMongo/Play-ReactiveMongo.png?branch=master)

> As for [Play Framework](http://playframework.com/) 2.4, a JDK 1.8+ is required to build this plugin.

### Learn More

- [Complete documentation and tutorials](http://reactivemongo.org/releases/0.11/documentation/tutorial/play2.html)
- [Search or create issues](https://github.com/ReactiveMongo/Play-ReactiveMongo/issues)
- [Get help](https://groups.google.com/forum/?fromgroups#!forum/reactivemongo)
- [Contribute](https://github.com/ReactiveMongo/ReactiveMongo/blob/master/CONTRIBUTING.md#reactivemongo-developer--contributor-guidelines)
