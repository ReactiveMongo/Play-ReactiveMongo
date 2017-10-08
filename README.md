# ReactiveMongo for Play Framework

This is a plugin for [Play Framework](https://www.playframework.com) 2.4 and 2.5, enabling support for [ReactiveMongo](http://reactivemongo.org) – a reactive, asynchronous and non-blocking Scala driver for MongoDB.

## Usage

In your `project/Build.scala` or `build.sbt`:

```ocaml
val reactiveMongoVer = "0.11.14"

// only for Play 2.5.x
libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % reactiveMongoVer
)

// only for Play 2.4.x
libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % s"$reactiveMongoVer-play24"
)
```

[![Maven](https://img.shields.io/maven-central/v/org.reactivemongo/play2-reactivemongo_2.12.svg)](http://search.maven.org/#search%7Cga%7C1%7Cplay2-reactivemongo) [![Javadocs](https://javadoc.io/badge/org.reactivemongo/play2-reactivemongo_2.12.svg)](https://javadoc.io/doc/org.reactivemongo/play2-reactivemongo_2.12)

**Learn more:**


The documentation is available online.

- [Complete documentation and tutorials](http://reactivemongo.org/releases/0.11/documentation/tutorial/play2.html)
- [Search or create issues](https://github.com/ReactiveMongo/Play-ReactiveMongo/issues) on GitHub
- Get help in the [ReactiveMongo Google Group](https://groups.google.com/forum/?fromgroups#!forum/reactivemongo) or [Stack Overflow](http://stackoverflow.com/questions/tagged/play-reactivemongo)
- [Contribute](https://github.com/ReactiveMongo/ReactiveMongo/blob/master/CONTRIBUTING.md#reactivemongo-developer--contributor-guidelines)

## Build manually

ReactiveMongo for Play 2 can be built from this source repository.

    sbt publish-local

To run the tests, use:

    sbt test

> As of Play 2.4, a JDK 1.8+ is required to build this plugin.