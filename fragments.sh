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

#------------------------------------------------------------------------

sed -n -e '/^  public static EchoServer create/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take2/EchoServer.java \
  > out/echo_server2_create.txt

sed -n -e '/^public final class EchoServerExecutor/,/^}/ p' \
  < src/main/java/com/io7m/aeron_guide/take2/EchoServerExecutor.java \
  > out/echo_server2_executor.txt

sed -n -e '/^public interface EchoServerExecutorService/,/^}/ p' \
  < src/main/java/com/io7m/aeron_guide/take2/EchoServerExecutorService.java \
  > out/echo_server2_executor_service.txt

sed -n -e '/^  private void onInitialClientConnected/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take2/EchoServer.java \
  > out/echo_server2_on_initial_client_connected.txt

sed -n -e '/^  private void onInitialClientDisconnected/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take2/EchoServer.java \
  > out/echo_server2_on_initial_client_disconnected.txt

sed -n -e '/^  private void onInitialClientMessage/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take2/EchoServer.java \
  > out/echo_server2_on_initial_client_message.txt

sed -n -e '/^  public void run/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take2/EchoServer.java \
  > out/echo_server2_run.txt

sed -n -e '/^    void onInitialClientMessageProcess/,/^    }/ p' \
  < src/main/java/com/io7m/aeron_guide/take2/EchoServer.java \
  > out/echo_server2_on_initial_client_message_process.txt

sed -n -e '/^  private Publication setupAllClientsPublication/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take2/EchoServer.java \
  > out/echo_server2_setupAllClientsPublication.txt

sed -n -e '/^  private Subscription setupAllClientsSubscription/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take2/EchoServer.java \
  > out/echo_server2_setupAllClientsSubscription.txt

sed -n -e '/^    public void poll/,/^    }/ p' \
  < src/main/java/com/io7m/aeron_guide/take2/EchoServer.java \
  > out/echo_server2_poll.txt

sed -n -e '/^    private EchoServerDuologue allocateNewDuologue/,/^    }/ p' \
  < src/main/java/com/io7m/aeron_guide/take2/EchoServer.java \
  > out/echo_server2_allocateNewDuologue.txt

sed -n -e '/^    void onInitialClientDisconnected/,/^    }/ p' \
  < src/main/java/com/io7m/aeron_guide/take2/EchoServer.java \
  > out/echo_server2_onInitialClientDisconnected.txt

sed -n -e '/^    void onInitialClientConnected/,/^    }/ p' \
  < src/main/java/com/io7m/aeron_guide/take2/EchoServer.java \
  > out/echo_server2_onInitialClientConnected.txt

sed -n -e '/^public final class EchoServerPortAllocator/,/^}/ p' \
  < src/main/java/com/io7m/aeron_guide/take2/EchoServerPortAllocator.java \
  > out/echo_server2_EchoServerPortAllocator.txt

sed -n -e '/^public final class EchoServerSessionAllocator/,/^}/ p' \
  < src/main/java/com/io7m/aeron_guide/take2/EchoServerSessionAllocator.java \
  > out/echo_server2_EchoServerSessionAllocator.txt

sed -n -e '/^public final class EchoServerDuologue/,/^}/ p' \
  < src/main/java/com/io7m/aeron_guide/take2/EchoServerDuologue.java \
  > out/echo_server2_EchoServerDuologue.txt

sed -n -e '/^public final class EchoChannels/,/^}/ p' \
  < src/main/java/com/io7m/aeron_guide/take2/EchoChannels.java \
  > out/echo_server2_EchoChannels.txt

sed -n -e '/^public final class EchoMessages/,/^}/ p' \
  < src/main/java/com/io7m/aeron_guide/take2/EchoMessages.java \
  > out/echo_server2_EchoMessages.txt

sed -n -e '/^public interface EchoServerConfiguration/,/^}/ p' \
  < src/main/java/com/io7m/aeron_guide/take2/EchoServerConfiguration.java \
  > out/echo_server2_EchoServerConfiguration.txt

#------------------------------------------------------------------------

sed -n -e '/^  public static EchoClient create/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take2/EchoClient.java \
  > out/echo_client2_create.txt

sed -n -e '/^  public void run/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take2/EchoClient.java \
  > out/echo_client2_run.txt

sed -n -e '/^  private void runEchoLoop/,/^  }/ p' \
  < src/main/java/com/io7m/aeron_guide/take2/EchoClient.java \
  > out/echo_client2_runEchoLoop.txt

