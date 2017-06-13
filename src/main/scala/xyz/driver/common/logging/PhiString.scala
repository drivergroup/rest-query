package xyz.driver.common.logging

class PhiString(private[logging] val text: String) {
  // scalastyle:off
  @inline def +(that: PhiString) = new PhiString(this.text + that.text)
}
