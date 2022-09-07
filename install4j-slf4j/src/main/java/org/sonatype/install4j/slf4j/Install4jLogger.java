/*
 * Copyright (c) 2012-present Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package org.sonatype.install4j.slf4j;

import com.install4j.api.Util;
import com.install4j.api.actions.Action;
import com.install4j.api.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.spi.LocationAwareLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;

// Based in slf4j SimpleLogger

/**
 * install4j-slf4j bridge {@link Logger}.
 *
 * @since 1.0
 */
public class Install4jLogger
    extends AbstractLogger
{
  private static final long serialVersionUID = 1;

  private static final String CONFIGURATION_FILE = "install4jlogger.properties";

  private static final String CONFIGURATION_PREFIX = Install4jLogger.class.getPackage().getName() + ".";

  public static final int LEVEL_TRACE = LocationAwareLogger.TRACE_INT;

  public static final int LEVEL_DEBUG = LocationAwareLogger.DEBUG_INT;

  public static final int LEVEL_INFO = LocationAwareLogger.INFO_INT;

  public static final int LEVEL_WARN = LocationAwareLogger.WARN_INT;

  public static final int LEVEL_ERROR = LocationAwareLogger.ERROR_INT;

  public static final int LEVEL_ALL = LEVEL_TRACE - 10;

  public static final int LEVEL_OFF = LEVEL_ERROR + 10;

  private static final Properties configuration = new Properties();

  private static boolean showLogName = true;

  private static boolean showLevel = false;

  private static String getStringProperty(final String name) {
    String prop = null;
    try {
      prop = System.getProperty(name);
    }
    catch (SecurityException e) {
      // Ignore
    }
    return prop == null ? configuration.getProperty(name) : prop;
  }

  private static boolean getBooleanProperty(final String name, final boolean defaultValue) {
    try {
      return Boolean.getBoolean(name);
    }
    catch (SecurityException e) {
      // Ignore
    }
    return defaultValue;
  }

  static {
    URL url = AccessController.doPrivileged(
      (PrivilegedAction<URL>) () -> {
        ClassLoader threadCL = Thread.currentThread().getContextClassLoader();
        if (threadCL != null) {
          return threadCL.getResource(CONFIGURATION_FILE);
        }
        else {
          return ClassLoader.getSystemResource(CONFIGURATION_FILE);
        }
      });

    if (url != null) {
      try {
        Util.logInfo(null, "Reading " + CONFIGURATION_FILE + ": " + url);
        try (InputStream input = url.openStream()) {
          configuration.load(input);
          Util.logInfo(null, "Configuration: " + configuration);
        }
      }
      catch (IOException e) {
        Util.logError(null, "Failed to load: " + CONFIGURATION_FILE);
        Util.log(e);
      }
    }
    else {
      Util.logError(null, "Missing: " + CONFIGURATION_FILE);
    }

    showLogName = getBooleanProperty(CONFIGURATION_PREFIX + "showLogName", showLogName);
    Util.logInfo(null, "Show log-name: " + showLogName);

    showLevel = getBooleanProperty(CONFIGURATION_PREFIX + "showLevel", showLevel);
    Util.logInfo(null, "Show level: " + showLevel);
  }

  protected int currentLogLevel = LEVEL_INFO;

  private final Class<?> type;

  private boolean screenOrAction = false;

  Install4jLogger(String name) {
    this.name = name;

    // install4j's Util.log* helpers need a type (or object) to determine name, so try and load the type here
    this.type = loadType();
    if (type != null) {
      screenOrAction = Screen.class.isAssignableFrom(type) || Action.class.isAssignableFrom(type);
    }

    // Set log level from properties
    String level = getStringProperty(CONFIGURATION_PREFIX + "logger." + name);
    int i = name.lastIndexOf(".");
    while (null == level && i > -1) {
      name = name.substring(0, i);
      level = getStringProperty(CONFIGURATION_PREFIX + "logger." + name);
      i = name.lastIndexOf(".");
    }

    if (null == level) {
      level = getStringProperty(CONFIGURATION_PREFIX + "level");
    }

    if ("all".equalsIgnoreCase(level)) {
      this.currentLogLevel = LEVEL_ALL;
    }
    else if ("trace".equalsIgnoreCase(level)) {
      this.currentLogLevel = LEVEL_TRACE;
    }
    else if ("debug".equalsIgnoreCase(level)) {
      this.currentLogLevel = LEVEL_DEBUG;
    }
    else if ("info".equalsIgnoreCase(level)) {
      this.currentLogLevel = LEVEL_INFO;
    }
    else if ("warn".equalsIgnoreCase(level)) {
      this.currentLogLevel = LEVEL_WARN;
    }
    else if ("error".equalsIgnoreCase(level)) {
      this.currentLogLevel = LEVEL_ERROR;
    }
    else if ("off".equalsIgnoreCase(level)) {
      this.currentLogLevel = LEVEL_OFF;
    }
  }

  private Class<?> loadType() {
    try {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      return cl.loadClass(name);
    }
    catch (Exception e) {
      return null;
    }
  }

  private void log(final int level, final String message, final Throwable t) {
    if (!isLevelEnabled(level)) {
      return;
    }

    StringBuilder buff = new StringBuilder(32);

    if (showLevel) {
      switch (level) {
        case LEVEL_TRACE:
          buff.append("TRACE");
          break;
        case LEVEL_DEBUG:
          buff.append("DEBUG");
          break;
        case LEVEL_INFO:
          buff.append("INFO");
          break;
        case LEVEL_WARN:
          buff.append("WARN");
          break;
        case LEVEL_ERROR:
          buff.append("ERROR");
          break;
      }
      buff.append(' ');
    }

    // Only append name if no type associated
    if (type == null && showLogName) {
      buff.append(name).append(" - ");
    }

    buff.append(message);

    // If the logger is for a screen or action, null the source to use the current (which should be the screen or action)
    Object source = type;
    if (screenOrAction) {
      source = null;
    }

    // Hand over the details to install4j's Util helper
    if (level == LEVEL_ERROR) {
      Util.logError(source, buff.toString());
    }
    else {
      Util.logInfo(source, buff.toString());
    }
    if (t != null) {
      Util.log(t);
    }
  }

  private void formatAndLog(int level, String format, Object arg1, Object arg2) {
    if (!isLevelEnabled(level)) {
      return;
    }
    FormattingTuple tp = MessageFormatter.format(format, arg1, arg2);
    log(level, tp.getMessage(), tp.getThrowable());
  }

  private void formatAndLog(int level, String format, Object[] argArray) {
    if (!isLevelEnabled(level)) {
      return;
    }
    FormattingTuple tp = MessageFormatter.arrayFormat(format, argArray);
    log(level, tp.getMessage(), tp.getThrowable());
  }

  @Override
  protected void handleNormalizedLoggingCall(Level level, Marker marker, String msg, Object[] arguments, Throwable throwable) {
    if (!isLevelEnabled(level.toInt())) {
      return;
    }
    FormattingTuple tp = MessageFormatter.arrayFormat(msg, arguments, throwable);
    log(level.toInt(), tp.getMessage(), tp.getThrowable());
  }

  protected boolean isLevelEnabled(int logLevel) {
    return logLevel >= currentLogLevel;
  }

  @Override
  public boolean isTraceEnabled() {
    return isLevelEnabled(LEVEL_TRACE);
  }

  @Override
  public boolean isTraceEnabled(Marker marker) {
    return isTraceEnabled();
  }

  @Override
  public void trace(String msg) {
    log(LEVEL_TRACE, msg, null);
  }

  @Override
  public void trace(String format, Object param1) {
    formatAndLog(LEVEL_TRACE, format, param1, null);
  }

  @Override
  public void trace(String format, Object param1, Object param2) {
    formatAndLog(LEVEL_TRACE, format, param1, param2);
  }

  @Override
  public void trace(String format, Object... argArray) {
    formatAndLog(LEVEL_TRACE, format, argArray);
  }

  @Override
  public void trace(String msg, Throwable t) {
    log(LEVEL_TRACE, msg, t);
  }

  @Override
  public boolean isDebugEnabled() {
    return isLevelEnabled(LEVEL_DEBUG);
  }

  @Override
  public boolean isDebugEnabled(Marker marker) {
    return isDebugEnabled();
  }

  @Override
  public void debug(String msg) {
    log(LEVEL_DEBUG, msg, null);
  }

  @Override
  public void debug(String format, Object param1) {
    formatAndLog(LEVEL_DEBUG, format, param1, null);
  }

  @Override
  public void debug(String format, Object param1, Object param2) {
    formatAndLog(LEVEL_DEBUG, format, param1, param2);
  }

  @Override
  public void debug(String format, Object... argArray) {
    formatAndLog(LEVEL_DEBUG, format, argArray);
  }

  @Override
  public void debug(String msg, Throwable t) {
    log(LEVEL_DEBUG, msg, t);
  }

  @Override
  public boolean isInfoEnabled() {
    return isLevelEnabled(LEVEL_INFO);
  }

  @Override
  public boolean isInfoEnabled(Marker marker) {
    return isInfoEnabled();
  }

  @Override
  public void info(String msg) {
    log(LEVEL_INFO, msg, null);
  }

  @Override
  public void info(String format, Object arg) {
    formatAndLog(LEVEL_INFO, format, arg, null);
  }

  @Override
  public void info(String format, Object arg1, Object arg2) {
    formatAndLog(LEVEL_INFO, format, arg1, arg2);
  }

  @Override
  public void info(String format, Object... argArray) {
    formatAndLog(LEVEL_INFO, format, argArray);
  }

  @Override
  public void info(String msg, Throwable t) {
    log(LEVEL_INFO, msg, t);
  }

  @Override
  public boolean isWarnEnabled() {
    return isLevelEnabled(LEVEL_WARN);
  }

  @Override
  public boolean isWarnEnabled(Marker marker) {
    return isWarnEnabled();
  }

  @Override
  public void warn(String msg) {
    log(LEVEL_WARN, msg, null);
  }

  @Override
  public void warn(String format, Object arg) {
    formatAndLog(LEVEL_WARN, format, arg, null);
  }

  @Override
  public void warn(String format, Object arg1, Object arg2) {
    formatAndLog(LEVEL_WARN, format, arg1, arg2);
  }

  @Override
  public void warn(String format, Object... argArray) {
    formatAndLog(LEVEL_WARN, format, argArray);
  }

  @Override
  public void warn(String msg, Throwable t) {
    log(LEVEL_WARN, msg, t);
  }

  @Override
  public boolean isErrorEnabled() {
    return isLevelEnabled(LEVEL_ERROR);
  }

  @Override
  public boolean isErrorEnabled(Marker marker) {
    return isErrorEnabled();
  }

  @Override
  public void error(String msg) {
    log(LEVEL_ERROR, msg, null);
  }

  @Override
  public void error(String format, Object arg) {
    formatAndLog(LEVEL_ERROR, format, arg, null);
  }

  @Override
  public void error(String format, Object arg1, Object arg2) {
    formatAndLog(LEVEL_ERROR, format, arg1, arg2);
  }

  @Override
  public void error(String format, Object... argArray) {
    formatAndLog(LEVEL_ERROR, format, argArray);
  }

  @Override
  public void error(String msg, Throwable t) {
    log(LEVEL_ERROR, msg, t);
  }

  @Override
  protected String getFullyQualifiedCallerName() {
    return null;
  }
}
