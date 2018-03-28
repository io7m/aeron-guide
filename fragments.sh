#!/bin/sh

rm -rfv out
mkdir -p out

sed -n -e '/^  public static EchoClient create/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take1/EchoClient.java \
  > out/echo_client_create.txt

sed -n -e '/^  public void close/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take1/EchoClient.java \
  > out/echo_client_close.txt

sed -n -e '/^  public void run/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take1/EchoClient.java \
  > out/echo_client_run.txt

sed -n -e '/^  private Publication setupPublication/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take1/EchoClient.java \
  > out/echo_client_setup_pub.txt

sed -n -e '/^  private Subscription setupSubscription/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take1/EchoClient.java \
  > out/echo_client_setup_sub.txt

sed -n -e '/^  private void runLoop/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take1/EchoClient.java \
  > out/echo_client_run_loop.txt

sed -n -e '/^  private static boolean sendMessage/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take1/EchoClient.java \
  > out/echo_client_send_message.txt

sed -n -e '/^  private static void onParseMessage/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take1/EchoClient.java \
  > out/echo_client_on_parse_message.txt

sed -n -e '/^  public static void main/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take1/EchoClient.java \
  > out/echo_client_main.txt

#------------------------------------------------------------------------

sed -n -e '/^  public static EchoServer create/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take1/EchoServer.java \
  > out/echo_server_create.txt

sed -n -e '/^  public void close/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take1/EchoServer.java \
  > out/echo_server_close.txt

sed -n -e '/^  public void run/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take1/EchoServer.java \
  > out/echo_server_run.txt

sed -n -e '/^  private Publication setupPublication/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take1/EchoServer.java \
  > out/echo_server_setup_pub.txt

sed -n -e '/^  private Subscription setupSubscription/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take1/EchoServer.java \
  > out/echo_server_setup_sub.txt

sed -n -e '/^  private void runLoop/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take1/EchoServer.java \
  > out/echo_server_run_loop.txt

sed -n -e '/^  private static boolean sendMessage/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take1/EchoServer.java \
  > out/echo_server_send_message.txt

sed -n -e '/^  private void onParseMessage/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take1/EchoServer.java \
  > out/echo_server_on_parse_message.txt

sed -n -e '/^  public static void main/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take1/EchoServer.java \
  > out/echo_server_main.txt

sed -n -e '/^  private void onClientConnected/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take1/EchoServer.java \
  > out/echo_server_on_client_connected.txt

sed -n -e '/^  private void onClientDisconnected/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take1/EchoServer.java \
  > out/echo_server_on_client_disconnected.txt

sed -n -e '/^  private static final class ServerClient/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take1/EchoServer.java \
  > out/echo_server_server_client.txt

