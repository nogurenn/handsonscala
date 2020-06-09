import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration.Inf
import $file.FetchLinks, FetchLinks._
import $file.FetchLinksAsync, FetchLinksAsync._

def fetchAllLinks(startTitle: String, depth: Int): Set[String] = {
  var seen = Set(startTitle)
  var current = Set(startTitle)
  for (i <- Range(0, depth)) {
    val nextTitleLists = for (title <- current) yield fetchLinks(title)
    current = nextTitleLists.flatten.filter(!seen.contains(_))
    seen = seen ++ current
  }
  seen
}

def fetchAllLinksParallel(startTitle: String, depth: Int): Set[String] = {
  var seen = Set(startTitle)
  var current = Set(startTitle)
  for (i <- Range(0, depth)) {
    val futures = for (title <- current) yield Future{ fetchLinks(title) }
    val nextTitleLists = futures.map(Await.result(_, Inf))
    current = nextTitleLists.flatten.filter(!seen.contains(_))
    seen = seen ++ current
  }
  seen
}

def fetchAllLinksRec(startTitle: String, depth: Int): Set[String] = {
  def rec(current: Set[String], seen: Set[String], recDepth: Int): Set[String] = {
    if (recDepth >= depth) seen
    else {
      val futures = for (title <- current) yield Future{ fetchLinks(title) }
      val nextTitleLists = futures.map(Await.result(_, Inf))
      val nextTitles = nextTitleLists.flatten
      rec(nextTitles.filter(!seen.contains(_)), seen ++ nextTitles, recDepth + 1)
    }
  }
  rec(Set(startTitle), Set(startTitle), 0)
}

def fetchAllLinksAsync(startTitle: String, depth: Int): Future[Set[String]] = {
  def rec(current: Set[String], seen: Set[String], recDepth: Int): Future[Set[String]] = {
    if (recDepth >= depth) Future.successful(seen)
    else {
      val futures = for (title <- current) yield fetchLinksAsync(title)
      Future.sequence(futures).map{nextTitleLists =>
        val nextTitles = nextTitleLists.flatten
        rec(nextTitles.filter(!seen.contains(_)), seen ++ nextTitles, recDepth + 1)
      }.flatten
    }
  }
  rec(Set(startTitle), Set(startTitle), 0)
}