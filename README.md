# ReactiveMongo Support to Play! Framework

[ ![Download](https://api.bintray.com/packages/hmrc/releases/play-reactivemongo/images/download.svg) ](https://bintray.com/hmrc/releases/play-reactivemongo/_latestVersion)

## Main features

A plugin for Play 2.x, enabling support for [ReactiveMongo](http://reactivemongo.org) - reactive, asynchronous and non-blocking Scala driver for MongoDB.

This provides the configuration and mongo connectivity to functionality in [simple-reactivemongo](https://github.com/hmrc/simple-reactivemongo)

### Configure your application to use ReactiveMongo plugin

#### add to your conf/application.conf

``` 
play.modules.enabled += "play.modules.reactivemongo.ReactiveMongoHmrcModule"
```

### Configure your database access within `application.conf`

This plugin reads connection properties from the `application.conf` and gives you an easy access to the connected database.

#### Add this to your conf/application.conf

The plugin will look for the mongo config under:
 - the config root, then if not found
 - the root of the `Application#mode`, then if not found
 - the root of the `Application#Dev`

```

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

```

### Installing

Include the following dependency in your SBT build

```scala
resolvers += Resolver.bintrayRepo("hmrc", "releases")

libraryDependencies += "uk.gov.hmrc" %% "play-reactivemongo" % "[INSERT_VERSION]"
```

*For Java 7 and Play 2.3.x use versions <=4.3.0*

## Upgrading from 5.x.x to 6.x.x?

Due to the upgrade of the underlying reactivmongo, there are several documented breaking changes. Details and fixes for these  are documented in the upgrade details for (simple-reactivmongo)[https://github.com/hmrc/simple-reactivemongo/blob/master/README.md].

## License ##
 
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
