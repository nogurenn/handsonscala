package sync
import upickle.default.{ReadWriter, macroRW, readwriter, Reader, Writer}

sealed trait Rpc
object Rpc{
  implicit val subPathRw = readwriter[String].bimap[os.SubPath](_.toString, os.SubPath(_))
  
  case class StatPath(path: os.SubPath) extends Rpc
  implicit val statPathRw: ReadWriter[StatPath] = macroRW
  
  case class WriteOver(src: Array[Byte], path: os.SubPath) extends Rpc
  implicit val writeOverRw: ReadWriter[WriteOver] = macroRW
  
  case class Delete(path: os.SubPath) extends Rpc
  implicit val deleteRw: ReadWriter[Delete] = macroRW

  case class StatInfo(p: os.SubPath, fileHash: Option[Int])
  implicit val statInfoRw: ReadWriter[StatInfo] = macroRW
  
  implicit val msgRw: ReadWriter[Rpc] = macroRW
}

object Shared{
  def send[T: Writer](out: java.io.DataOutputStream, msg: T): Unit = {
    val bytes = upickle.default.writeBinary(msg)
    out.writeInt(bytes.length)
    out.write(bytes)
    out.flush()
  }
  def receive[T: Reader](in: java.io.DataInputStream) = {
    val buf = new Array[Byte](in.readInt())
    in.readFully(buf)
    upickle.default.readBinary[T](buf)
  }
  def hashPath(p: os.Path) = {
    if (!os.isFile(p)) None
    else Some(java.util.Arrays.hashCode(os.read.bytes(p)))
  }
}