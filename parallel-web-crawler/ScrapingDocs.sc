import $ivy.`org.jsoup:jsoup:1.12.1`, org.jsoup._
import collection.JavaConverters._

val indexDoc = Jsoup.connect("https://developer.mozilla.org/en-US/docs/Web/API").get()
val links = indexDoc.select("h2#Interfaces").nextAll.select("div.index a").asScala
val linkData = links.map(link => (link.attr("href"), link.attr("title"), link.text))
val articles = for ((url, tooltip, name) <- linkData) yield {
    println("Scraping " + name)
    val doc = Jsoup.connect("https://developer.mozilla.org" + url).get()
    val summary = doc.select("article#wikiArticle > p").asScala.headOption match {
        case Some(n) => n.text; case None => ""
    }
    val methodsAndProperties = doc
        .select("article#wikiArticle dl dt")
        .asScala
        .map(el => (el.text, el.nextElementSibling match {case null => ""; case x => x.text}))
    (url, tooltip, name, summary, methodsAndProperties)
}

// Scrape API status
val annotationsList =
  for(span <- indexDoc.select("span.indexListRow").asScala)
  yield (
    span.text,
    span.select("span.indexListBadges .icon-only-inline").asScala.map(_.attr("title"))
  )

val annotationsMap = annotationsList.toMap

assert(
  annotationsMap("BatteryManager") ==
  Seq("This is an obsolete API and is no longer guaranteed to work.")
)
assert(
  annotationsMap("BluetoothAdvertisingData") ==
  Seq(
    "This API has not been standardized.",
    "This is an obsolete API and is no longer guaranteed to work."
  )
)

os.write(os.pwd / "annotations.json", upickle.default.write(annotationsMap))



// Scrape external links
val start = "https://www.lihaoyi.com/"
val seen = collection.mutable.Set(start)
val queue = collection.mutable.ArrayDeque(start)

while(queue.nonEmpty){
  val current = queue.removeHead()
  println("Crawling " + current)
  val docOpt =
    try Some(Jsoup.connect(current).get())
    catch{case e: org.jsoup.HttpStatusException => None}

  docOpt match{
    case None =>
    case Some(doc) =>
      val allLinks = doc.select("a").asScala.map(_.attr("href"))
      for(link <- allLinks if !link.startsWith("#")){
        // ignore hash query fragment in URL
        val newUri = new java.net.URI(current).resolve(link.takeWhile(_ != '#')).normalize()
        val normalizedLink = newUri.toString
        if (normalizedLink.startsWith(start) &&
            !seen.contains(normalizedLink) &&
            link.endsWith(".html")){
          queue.append(normalizedLink)
        }
        seen.add(normalizedLink)
      }
  }
}

pprint.log(seen)
pprint.log(seen.size)
