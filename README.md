jsay
===

[![Build Status](https://img.shields.io/travis/io7m/jsay.svg?style=flat-square)](https://travis-ci.org/io7m/jsay)
[![Maven Central](https://img.shields.io/maven-central/v/com.io7m.jsay/com.io7m.jsay.svg?style=flat-square)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.io7m.jsay%22)
[![Maven Central (snapshot)](https://img.shields.io/nexus/s/https/oss.sonatype.org/com.io7m.jsay/com.io7m.jsay.svg?style=flat-square)](https://oss.sonatype.org/content/repositories/snapshots/com/io7m/jsay/)
[![Codacy Badge](https://img.shields.io/codacy/grade/8114c45dcba04a85a7564661c489eb42.svg?style=flat-square)](https://www.codacy.com/app/github_79/jsay?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=io7m/jsay&amp;utm_campaign=Badge_Grade)

A tiny JMS text message sender.

![jsay](./src/site/resources/jsay.jpg?raw=true)

# Usage

```
INFO [main] com.io7m.jsay.Main: Usage: jsay [options]
  Options:
  * --address
      The message address
  * --broker-uri
      The message broker URI
    --expires
      The message expiry time
    --file
      The message file (if not specified, data is read from stdin)
      Default: /dev/stdin
    --password
      The message broker password
    --user
      The message broker user
    --verbose
      The level of logging verbosity
      Default: INFO
      Possible Values: [TRACE, DEBUG, INFO, WARN, ERROR]
```

# Example

To send the contents of `file.txt` to the `someQueue` queue on the message broker at `messaging.example.com` using
port `7000` and requiring TLS:

```
$ java -jar jsay.jar --address someQueue --broker-uri tcp://messaging.example.com:7000?sslEnabled=true --file hello.txt
```

