# ReactiveMongo for Play Framework

[![Travis branch](https://img.shields.io/travis/ReactiveMongo/Play-ReactiveMongo/master.svg)](https://travis-ci.org/ReactiveMongo/Play-ReactiveMongo)
[![Maven Central](https://img.shields.io/maven-central/v/org.reactivemongo/play2-reactivemongo_2.11.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.reactivemongo%22%20AND%20a%3A%22play2-reactivemongo_2.11%22)

This is a plugin for [Play Framework](https://www.playframework.com) 2.4 and 2.5, enabling support for [ReactiveMongo](http://reactivemongo.org) – a reactive, asynchronous and non-blocking Scala driver for MongoDB.


## Usage

In your `project/Build.scala` or `build.sbt`:

```scala
// only for Play 2.5.x
libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.11"
)

// only for Play 2.4.x
libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.11-play24"
)
```


## Build manually

ReactiveMongo for Play 2 can be built from this source repository.

    sbt publish-local

To run the tests, use:

    sbt test

> As of Play 2.4, a JDK 1.8+ is required to build this plugin.


## Learn more

- [Complete documentation and tutorials](http://reactivemongo.org/releases/0.11/documentation/tutorial/play2.html)
- [Search or create issues](https://github.com/ReactiveMongo/Play-ReactiveMongo/issues) on GitHub
- Get help in the [ReactiveMongo Google Group](https://groups.google.com/forum/?fromgroups#!forum/reactivemongo) or [Stack Overflow](http://stackoverflow.com/questions/tagged/play-reactivemongo)
- [Contribute](https://github.com/ReactiveMongo/ReactiveMongo/blob/master/CONTRIBUTING.md#reactivemongo-developer--contributor-guidelines)
