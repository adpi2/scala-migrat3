package fix.explicitResultTypes

object PartialFunction {
  def empty[A, B]: PartialFunction[A,B] = scala.PartialFunction.empty[A, B]
}
