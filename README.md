# ReactiveMongo Support to Play! Framework 2.0

This is a plugin for Play 2.1, enabling support for [ReactiveMongo](http://reactivemongo.org) - reactive, asynchronous and non-blocking Scala driver for MongoDB.

## Main features

### JSON <-> BSON conversion

With Play2-ReactiveMongo, you can use directly the embedded JSON library in Play >= 2.1.

The JSON lib has been completely refactored and is now the most powerful one in the Scala world. Thanks to it, you can now fetch documents from MongoDB in the JSON format, transform them by removing and/or adding some properties, and send them to the client. Even better, when a client sends a JSON document, you can validate it and transform it before saving it into a MongoDB collection.

Another advantage to use this plugin is to be capable of using JSON documents for querying MongoDB.

### Configure your database access within `application.conf`

This plugin reads connection properties from the `application.conf` and gives you an easy access to the connected database.

### Helpers for GridFS

Play2-ReactiveMongo makes it easy to serve and store files in a complete non-blocking manner. It provides a body parser for handling file uploads, and a method to serve files from a GridFS store.