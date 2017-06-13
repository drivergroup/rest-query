package xyz.driver.common.utils

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object FutureUtils {

  def executeSynchronously[T](f: ExecutionContext => Future[T]): Try[T] = {
    val future = f {
      new ExecutionContext {
        override def reportFailure(cause: Throwable): Unit = cause.printStackTrace()

        override def execute(runnable: Runnable): Unit = runnable.run()
      }
    }
    future.value.get
  }

}
