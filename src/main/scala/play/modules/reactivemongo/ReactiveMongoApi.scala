package play.modules.reactivemongo

import scala.concurrent.Future

import play.api.inject.ApplicationLifecycle
import play.api.{
  ApplicationLoader,
  BuiltInComponentsFromContext,
  Configuration
}

import reactivemongo.api.{ DefaultDB, MongoConnection, MongoDriver }
import reactivemongo.api.gridfs.GridFS

import reactivemongo.play.json.JSONSerializationPack

/**
 * MongoDB API
 */
trait ReactiveMongoApi {
  def driver: MongoDriver
  def connection: MongoConnection
  def database: Future[DefaultDB]
  def asyncGridFS: Future[GridFS[JSONSerializationPack.type]]

  /** See [[database]] */
  @deprecated("Use [[database]]", "0.11.12")
  def db: DefaultDB

  /** See [[asyncGridFS]] */
  def gridFS: GridFS[JSONSerializationPack.type]
}

trait ReactiveMongoApiComponents {
  /** The instance name (default: `default`) */
  def name: String

  /** The configuration */
  def configuration: Configuration

  /** The connection URI */
  def parsedUri: MongoConnection.ParsedURI

  /** The name of the database */
  def dbName: String

  /** Indicated whether the [[parsedUri]] must be strictly interpreted */
  def strictMode: Boolean = false

  /** The application lifecycle */
  def applicationLifecycle: ApplicationLifecycle

  /** The API initialized according the current configuration */
  lazy val reactiveMongoApi: ReactiveMongoApi = new DefaultReactiveMongoApi(
    name, parsedUri, dbName, strictMode, configuration, applicationLifecycle
  )
}

/**
 * Can be used for a custom application loader.
 *
 * {{{
 * import play.api.ApplicationLoader
 *
 * class MyApplicationLoader extends ApplicationLoader {
 *   def load(context: ApplicationLoader.Context) =
 *     new MyComponents(context).application
 * }
 *
 * class MyComponents(context: ApplicationLoader.Context)
 *     extends ReactiveMongoApiFromContext(context) {
 *   lazy val router = play.api.routing.Router.empty
 * }
 * }}}
 */
abstract class ReactiveMongoApiFromContext(
  context: ApplicationLoader.Context,
  val name: String
) extends BuiltInComponentsFromContext(context)
    with ReactiveMongoApiComponents {

  def this(context: ApplicationLoader.Context) = this(context, "default")

  private lazy val parsed =
    DefaultReactiveMongoApi.parseConfiguration(configuration).collectFirst {
      case (n, info) if (n == name) => info
    }

  lazy val parsedUri = parsed.map(_.uri).
    getOrElse(throw configuration.globalError(
      s"Missing ReactiveMongo configuration for '$name'"
    ))

  lazy val dbName = parsed.flatMap(_.uri.db).
    getOrElse(throw configuration.globalError(
      s"Missing ReactiveMongo configuration for '$name'"
    ))

  override lazy val strictMode = parsed.map(_.strict).getOrElse(false)
}
