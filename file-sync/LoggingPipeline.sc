import $file.Classes, Classes._

// Running actors single threaded for testing
// val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
// val executionContext = scala.concurrent.ExecutionContext.fromExecutor(executor)
// implicit val cc = new castor.Context.Test(executionContext)

// commented override is for context logging
implicit val cc = new castor.Context.Test() /* {
  override def reportRun(a: castor.Actor[_],
                         msg: Any,
                         token: castor.Context.Token): Unit = {
    println(s"$a <- $msg")
    super.reportRun(a, msg, token)
  }
}
*/

val diskActor = new DiskActor(os.pwd / "log.txt")
val uploadActor = new UploadActor("https://httpbin.org/post")
val base64Actor = new Base64Actor(new castor.SplitActor(diskActor, uploadActor))
val sanitizeActor = new SanitizeActor(base64Actor)
val logger = sanitizeActor