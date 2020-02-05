package play.modules.reactivemongo

import scala.concurrent.{ ExecutionContext, Future }

import play.api.inject.ApplicationLifecycle
import play.api.{
  ApplicationLoader,
  BuiltInComponentsFromContext,
  Configuration
}

import reactivemongo.api.{ AsyncDriver, DefaultDB, MongoConnection }

import reactivemongo.api.gridfs.GridFS

import reactivemongo.play.json.JSONSerializationPack

/**
 * MongoDB API
 */
trait ReactiveMongoApi {
  /** Provisionned ReactiveMongo driver */
  def asyncDriver: AsyncDriver

  /** Default ReactiveMongo pool for the Play application */
  def connection: MongoConnection

  def database: Future[DefaultDB]
  def asyncGridFS: Future[GridFS[JSONSerializationPack.type]]
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

  /** The execution context */
  protected def ec: ExecutionContext

  /** The API initialized according the current configuration */
  lazy val reactiveMongoApi: ReactiveMongoApi = new DefaultReactiveMongoApi(
    parsedUri, dbName, strictMode, configuration, applicationLifecycle)(ec)
}

/**
 * Can be used for a custom application loader.
 *
 * {{{
 * import play.api.ApplicationLoader
 * import play.modules.reactivemongo.ReactiveMongoApiFromContext
 *
 * class MyApplicationLoader extends ApplicationLoader {
 *   def load(context: ApplicationLoader.Context) =
 *     new MyComponents(context).application
 * }
 *
 * class MyComponents(context: ApplicationLoader.Context)
 *     extends ReactiveMongoApiFromContext(context) {
 *   lazy val router = play.api.routing.Router.empty
 *   override lazy val httpFilters = Seq.empty[play.api.mvc.EssentialFilter]
 * }
 * }}}
 */
abstract class ReactiveMongoApiFromContext(
    context: ApplicationLoader.Context,
    val name: String) extends BuiltInComponentsFromContext(context)
  with ReactiveMongoApiComponents {

  def this(context: ApplicationLoader.Context) = this(context, "default")

  private lazy val parsed =
    DefaultReactiveMongoApi.parseConfiguration(configuration)(ec).collectFirst {
      case (n, info) if (n == name) => info
    }

  lazy val parsedUri = parsed.map(_.uri).
    getOrElse(throw configuration.globalError(
      s"Missing ReactiveMongo configuration for '$name'"))

  lazy val dbName = parsed.flatMap(_.uri.db).
    getOrElse(throw configuration.globalError(
      s"Missing ReactiveMongo configuration for '$name'"))

  override lazy val strictMode = parsed.map(_.strict).getOrElse(false)

  final protected def ec: ExecutionContext = materializer.executionContext
}
