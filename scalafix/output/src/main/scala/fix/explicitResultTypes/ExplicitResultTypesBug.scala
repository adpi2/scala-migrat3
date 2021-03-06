package fix.explicitResultTypes

import scala.reflect.runtime.universe._
import scala.collection.{mutable => mut}

object ExplicitResultTypesBug {

  class MyMirror(owner: ClassMirror) {
    val symbol: reflect.runtime.universe.MethodSymbol =
      owner.symbol.info.decl(TermName("")).asMethod
  }

  val map: scala.collection.mutable.Map[Int,Int] = mut.Map.empty[Int, Int]

  object Ignored {
    import java.{util => ju}
    val hasImport: java.util.List[Int] = ju.Collections.emptyList[Int]()
  }
  val missingImport: java.util.List[Int] = java.util.Collections.emptyList[Int]()

  def o3: rsc.tests.testpkg.O3 = new rsc.tests.testpkg.O3()

  def overload(a: Int): Int = a
  def overload(a: String): String = a

  abstract class ParserInput {
    def apply(index: Int): Char
  }
  case class IndexedParserInput(data: String) extends ParserInput {
    override def apply(index: Int): Char = data.charAt(index)
  }
  case class Foo(a: Int) {
    def apply(x: Int): Int = x
  }
  abstract class Opt[T] {
    def get(e: T): T
  }
  class IntOpt extends Opt[Int] {
    def get(e: Int): Int = e
  }

  val o4: List[rsc.tests.testpkg.O4] = null.asInstanceOf[List[rsc.tests.testpkg.O4]]

}