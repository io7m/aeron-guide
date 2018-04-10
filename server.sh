#!/bin/sh
exec java -cp target/com.io7m.aeron-guide-0.0.1.jar com.io7m.aeron_guide.take2.EchoServer "$@"
