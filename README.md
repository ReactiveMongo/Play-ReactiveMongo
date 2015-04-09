# ReactiveMongo Support to Play! Framework

[![Build Status](https://travis-ci.org/hmrc/Play-ReactiveMongo.svg?branch=master)](https://travis-ci.org/hmrc/Play-ReactiveMongo) [ ![Download](https://api.bintray.com/packages/hmrc/releases/play-reactivemongo/images/download.svg) ](https://bintray.com/hmrc/releases/play-reactivemongo/_latestVersion)

A plugin for Play 2.x, enabling support for [ReactiveMongo](http://reactivemongo.org) - reactive, asynchronous and non-blocking Scala driver for MongoDB.

This provides the configuration and mongo connectivity to functionality in [simple-reactivemongo](https://github.com/hmrc/simple-reactivemongo)

## Main features

### Add ReactiveMongo to your dependencies

In your project/Build.scala:

```scala
libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play-reactivemongo" % "x.x.x"
)
```

### Configure your application to use ReactiveMongo plugin

#### add to your conf/play.plugins

``` 
400:play.modules.reactivemongo.ReactiveMongoPlugin
```


### Configure your database access within `application.conf`

This plugin reads connection properties from the `application.conf` and gives you an easy access to the connected database.

#### Add this to your conf/application.conf

The plugin will look for the mongo config under a root of the `Application#mode`. If no configuration is found for the current application mode then the fallback is `Mode.Dev`

```
Dev {
    mongodb {
        uri = "mongodb://username:password@localhost:27017/your_db_name"
        channels = 5
        failoverStrategy = {
            initialDelayMsecs = 100
            retries = 10
            delay = {
                function = fibonacci
                factor = 1
            }
        }
    }
}
```

