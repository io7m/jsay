/*
 * Copyright © 2019 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.jsay;

import ch.qos.logback.classic.Level;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Destination;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import static javax.jms.Session.AUTO_ACKNOWLEDGE;

/**
 * The main <tt>jsay</tt> program.
 */

public final class Main
{
  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  private Main()
  {

  }

  private enum LogLevel
  {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR
  }

  private static final class CommandLineOptions
  {
    CommandLineOptions()
    {

    }

    @Parameter(
      required = false,
      names = "--verbose",
      description = "The level of logging verbosity")
    private LogLevel verbose = LogLevel.INFO;

    @Parameter(
      required = false,
      names = "--user",
      description = "The message broker user")
    private String brokerUser;

    @Parameter(
      required = false,
      names = "--password",
      description = "The message broker password")
    private String brokerPassword;

    @Parameter(
      required = true,
      names = "--broker-uri",
      description = "The message broker URI")
    private URI brokerURI;

    @Parameter(
      required = false,
      names = "--expires",
      description = "The message expiry time")
    private String expiry;

    @Parameter(
      required = true,
      names = "--address",
      description = "The message address")
    private String address;

    @Parameter(
      required = false,
      names = "--topic",
      arity = 1,
      description = "The destination is a topic, not a queue.")
    private boolean topic;

    @Parameter(
      required = false,
      names = "--file",
      description = "The message file (if not specified, data is read from stdin)")
    private Path file = Paths.get("/dev/stdin");
  }

  /**
   * The main entry point.
   *
   * @param args Command-line arguments
   *
   * @throws Exception On errors
   */

  public static void main(
    final String[] args)
    throws Exception
  {
    new Main().run(args);
  }

  /**
   * Run the message send.
   *
   * @param args Arguments
   *
   * @throws Exception On errors
   */

  public void run(
    final String... args)
    throws Exception
  {
    final var options =
      new CommandLineOptions();

    final var console = new StringConsole();
    final var commander =
      JCommander.newBuilder()
        .addObject(options)
        .programName("jsay")
        .console(console)
        .build();

    try {
      commander.parse(args);
    } catch (final ParameterException e) {
      LOG.error("parameter error: {}", e.getMessage());
      commander.usage();
      LOG.info("{}", packageVersion());
      LOG.info("{}", console.builder());
      throw e;
    }

    {
      final var root =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
      switch (options.verbose) {

        case TRACE:
          root.setLevel(Level.TRACE);
          break;
        case DEBUG:
          root.setLevel(Level.DEBUG);
          break;
        case INFO:
          root.setLevel(Level.INFO);
          break;
        case ERROR:
          root.setLevel(Level.ERROR);
          break;
        case WARN:
          root.setLevel(Level.WARN);
          break;
      }
    }

    LOG.debug("creating connection factory");
    try (var connectionFactory =
           ActiveMQJMSClient.createConnectionFactory(
             options.brokerURI.toString(),
             "jsay")) {

      LOG.debug("creating connection");
      try (var connection = connectionFactory.createConnection(
        options.brokerUser, options.brokerPassword)) {

        LOG.debug("creating session");
        try (var session =
               connection.createSession(false, AUTO_ACKNOWLEDGE)) {

          LOG.debug("creating queue");
          final Destination destination;
          if (options.topic) {
            destination = session.createTopic(options.address);
          } else {
            destination = session.createQueue(options.address);
          }

          LOG.debug("creating producer");
          try (var producer = session.createProducer(destination)) {
            connection.start();

            final var text = readText(options);
            final var message = session.createBytesMessage();
            message.writeBytes(text.getBytes(StandardCharsets.UTF_8));

            if (options.expiry != null) {
              final var time =
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(
                  options.expiry.trim());
              message.setJMSExpiration(Instant.from(time).toEpochMilli());
            }

            LOG.debug("sending message");
            producer.send(message);
          }
        }
      }
    }
  }

  private static String packageVersion()
  {
    final var pack = Main.class.getPackage();
    final var name = pack.getImplementationTitle();
    final var vers = pack.getImplementationVersion();
    if (name != null && vers != null) {
      return name + " " + vers;
    }
    return "jsay 0.0.0";
  }

  private static String readText(
    final CommandLineOptions options)
    throws IOException
  {
    return Files.readString(options.file, StandardCharsets.UTF_8);
  }
}
