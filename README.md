# ReactiveMongo for Play Framework

This is a module for the [Play Framework](https://www.playframework.com) 2.5, 2.6, 2.7, 2.8, 2.9 and 3.0, enabling support for [ReactiveMongo](http://reactivemongo.org) – a reactive, asynchronous and non-blocking Scala driver for MongoDB.

[![Maven](https://img.shields.io/maven-central/v/org.reactivemongo/play2-reactivemongo_2.12.svg)](http://search.maven.org/#search%7Cga%7C1%7Cplay2-reactivemongo) [![Javadocs](https://javadoc.io/badge/org.reactivemongo/play2-reactivemongo_2.12.svg)](https://javadoc.io/doc/org.reactivemongo/play2-reactivemongo_2.12)

**Learn more:**

The documentation is available online.

- [Complete documentation and tutorials](http://reactivemongo.org/releases/0.1x/documentation/tutorial/play.html)
- [Search or create issues](https://github.com/ReactiveMongo/Play-ReactiveMongo/issues) on GitHub
- Get help in the [ReactiveMongo Google Group](https://groups.google.com/forum/?fromgroups#!forum/reactivemongo) or [Stack Overflow](http://stackoverflow.com/questions/tagged/play-reactivemongo)
- [Contribute](https://github.com/ReactiveMongo/ReactiveMongo/blob/master/CONTRIBUTING.md#reactivemongo-developer--contributor-guidelines)

## Build manually

ReactiveMongo for Play can be built from this source repository.

    sbt publishLocal

To run the tests, use:

    sbt test

> As of Play 2.4, a JDK 1.8+ is required to build this plugin.

[![CircleCI](https://circleci.com/gh/ReactiveMongo/Play-ReactiveMongo.svg?style=svg)](https://circleci.com/gh/ReactiveMongo/Play-ReactiveMongo)