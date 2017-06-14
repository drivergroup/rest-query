package xyz.driver.pdsuicommon.db

import java.io.Closeable
import java.time._
import java.util.concurrent.Executors
import javax.sql.DataSource

import com.typesafe.config.Config
import io.getquill._
import xyz.driver.pdsuicommon.concurrent.MdcExecutionContext
import xyz.driver.pdsuicommon.db.SqlContext.Settings
import xyz.driver.pdsuicommon.logging.{PhiLogging, Unsafe}

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

object SqlContext extends PhiLogging {

  case class DbCredentials(user: String,
                           password: String,
                           host: String,
                           port: Int,
                           dbName: String,
                           dbCreateFlag: Boolean,
                           dbContext: String,
                           connectionParams: String,
                           url: String)

  case class Settings(credentials: DbCredentials,
                      connection: Config,
                      connectionAttemptsOnStartup: Int,
                      threadPoolSize: Int)

  def apply(settings: Settings): SqlContext = {
    // Prevent leaking credentials to a log
    Try(JdbcContextConfig(settings.connection).dataSource) match {
      case Success(dataSource) => new SqlContext(dataSource, settings)
      case Failure(NonFatal(e)) =>
        logger.error(phi"Can not load dataSource, error: ${Unsafe(e.getClass.getName)}")
        throw new IllegalArgumentException("Can not load dataSource from config. Check your database and config")
    }
  }

}

class SqlContext(dataSource: DataSource with Closeable, settings: Settings)
    extends MysqlJdbcContext[MysqlEscape](dataSource) with EntityExtractorDerivation[Literal] {

  private val tpe = Executors.newFixedThreadPool(settings.threadPoolSize)

  implicit val executionContext: ExecutionContext = {
    val orig = ExecutionContext.fromExecutor(tpe)
    MdcExecutionContext.from(orig)
  }

  override def close(): Unit = {
    super.close()
    tpe.shutdownNow()
  }

  final implicit class LocalDateTimeDbOps(val left: LocalDateTime) {

    // scalastyle:off
    def <=(right: LocalDateTime): Quoted[Boolean] = quote(infix"$left <= $right".as[Boolean])
  }
}
