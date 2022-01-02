package play.modules.reactivemongo

/*
 * Weirdly this is a workaround for Scala 2.12 compilation to avoid
 * the following error when calling `gridFS.fileToSave`:
 *
 * scala.reflect.internal.Types$TypeError: illegal cyclic inheritance involving <refinement>
 */
private[reactivemongo] object Api {
  import reactivemongo.api.SerializationPack
  import reactivemongo.api.gridfs.GridFS

  @inline def fileToSave[T <: SerializationPack with Singleton](
      gridfs: GridFS[T],
      filename: String,
      contentType: Option[String]
    ) = gridfs.fileToSave(Some(filename), contentType)
}
