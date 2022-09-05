package org.sonatype.install4j.slf4j;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

public class Install4jSlf4JServiceProvider implements SLF4JServiceProvider {
  // to avoid constant folding by the compiler, this field must *not* be final
  public static String REQUESTED_API_VERSION = "2.0.99"; // !final

  private ILoggerFactory loggerFactory;
  private IMarkerFactory markerFactory;
  private MDCAdapter mdcAdapter;

  @Override
  public ILoggerFactory getLoggerFactory() {
    return loggerFactory;
  }

  @Override
  public IMarkerFactory getMarkerFactory() {
    return markerFactory;
  }

  @Override
  public MDCAdapter getMDCAdapter() {
    return mdcAdapter;
  }

  @Override
  public String getRequestedApiVersion() {
    return REQUESTED_API_VERSION;
  }

  @Override
  public void initialize() {
    loggerFactory = new Install4jLoggerFactory();
    markerFactory = new BasicMarkerFactory();
    mdcAdapter = new NOPMDCAdapter();
  }
}
