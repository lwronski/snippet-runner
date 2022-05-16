//> using lib "io.get-coursier:coursier_2.13:2.1.0-M5-24-g678b31710"
//> using lib "com.lihaoyi::os-lib:0.8.0"
//> using scala "3.1.2"

import coursier.Versions
import coursier.cache.FileCache
import coursier.core.{Version, Versions => CoreVersions}
import coursier.util.{Artifact, Task}
import coursier.util.StringInterpolators.safeModule
import coursier.core.Module

import scala.concurrent.duration.DurationInt
import scala.language.experimental.macros
import coursier.core.Organization
import coursier.core.ModuleName

object NightlyChecker {
  def main(args: Array[String]): Unit = {
    val cache: FileCache[Task] = FileCache()
    val scala3Library = Module.apply(Organization("org.scala-lang"),ModuleName("scala3-library_3"), Map.empty)

    val scala3nigthlies: List[String] = cache.withTtl(0.seconds).logger.use {
      Versions(cache)
        .withModule(scala3Library)
        .result()
        .unsafeRun()(cache.ec)
    }.versions.available.filter(_.endsWith("-NIGHTLY")).sorted.reverse

    val code = args(0)
    for { nightly <- scala3nigthlies } {
        println(s"Testing code with following scala nightly version $nightly")
        val res = os.proc("scala-cli", "-S", nightly, "-q", "-")
              .call(cwd = os.pwd, stdin = code, check = false)
        val output = res.out.text().trim  
        if(res.exitCode == 0) {
          println(s"Found the latest nightly version working with passed code $nightly")
          sys.exit(0)
        }
      }
  }
}
