% Aeron For The Working Programmer
% Mark Raynsford
% 2018-03-28T10:39:05+0000

# Aeron For The Working Programmer

[Aeron](https://github.com/real-logic/Aeron) is an ultra-efficient
message transport library for Java and C++. It is designed to work
over unreliable media protocols such as UDP and Infiniband, and offers
ordered messaging and optional reliability (by retransmission of
messages in the case of dropped packets). The design and implementation
has an extreme emphasis on low-latency communication, making the
library ideal for use in applications with realtime requirements such
as fast-paced networked multiplayer games, high frequency financial
trading, VOIP, video streaming, etc.

This guide is an attempt to describe how to put together a working
server that can serve a number of clients concurrently. It is
somewhat biased towards the perspective of a developer using Aeron
as the networking component of a client/server multiplayer game
engine. Specifically, the intention is that the described server
configuration will serve a relatively small number of clients
(typically less than a hundred) concurrently as opposed to serving
the tens of thousands of clients concurrently that might be expected
of a high-performance web server.

# Concepts

## Publications And Subscriptions

The Aeron library works with unidirectional streams of messages known
as _publications_ and _subscriptions_. Intuitively, a _publication_
is a stream of messages to which you can _write_, and a _subscription_
is a stream of messages from which you can _read_. This requires a
slight adjustment in the way one would usually think about programming
within the context of, say, UDP sockets. UDP sockets are bidirectional;
the programmer will usually create a UDP socket, bind it to a local
address, and then read from that socket to receive datagrams,
and write datagrams to that same socket to send messages to peers. Aeron,
in contrast, requires the programmer to create separate pairs of
publications and subscriptions to write and read messages to and from
peers.

## Media Driver

Aeron defines a protocol on top of which the user is expected
to implement their own application-specific message protocol. The Aeron
protocol handles the details of message transmission, retransmission,
ordering, fragmentation, etc, leaving the application free to send
whatever messages it needs without having to worry about ensuring
those messages actually arrive and arrive in the right order.

The actual low level handling of the transmission medium (such as
UDP sockets) is handled by a module of code known as the _media
driver_. The media driver can be either run standalone as a separate
process, or can be embedded within the application. For the examples
presented in this guide, we assume that the media driver will be
embedded within the application and, therefore, the application is
responsible for configuring, starting up, and shutting down the media
driver correctly.

# A Client And Server (Take 1)

As a first step, we'll write a trivial `echo` server and client:
Clients can connect to the server and send UTF-8 strings. Any string
received by the server will be sent back to the sending client
unmodified.  For this example, we'll simplify things even further:
The client will simply send random integers as strings, once per
second, as opposed to allowing the user to type in arbitrary strings.
We'll step through the simplest possible implementation that can work,
and then critically assess it with an eye to producing a second,
better implementation.

In order to write a server, the following steps are required:

  1. Start up the media driver.
  2. Create a subscription that will be used to read messages from
     clients.
  3. Go into a loop, creating new publications/subscriptions for clients
     as they connect, and reading/writing messages from/to existing
     clients.

In order to write a client, the following steps are required:

  1. Start up the media driver.
  2. Create a publication that will be used to send messages to
     the server.
  3. Go into a loop, sending messages to the server and reading
     back the responses.

For the sake of simplicity, we'll write the client first. All of
the code presented here is available as a Maven project on
[GitHub](https://github.com/io7m/aeron-guide), but excerpts will
be printed here for explanatory purposes.

## Echo Client

We start by defining an `EchoClient` class with a static `create`
method that initializes a media driver and an instance of the Aeron
library to go with it. The media driver requires a directory on the
filesystem within which it creates various temporary files that
are memory-mapped to allow efficient thread-safe communication
between the separate components of the library. For best results,
this directory should reside on a memory-backed filesystem (such as
`/dev/shm` on Linux), but this is not actually required.

The `create` method we define simply creates and launches Aeron and
the media driver. It also takes the local address that the client
will use for communication, and the address of the server to which
the client will connect. It passes these addresses on to the
constructor for later use.

[EchoClient.create()](EchoClient.java)
```{include=out/echo_client_create.txt}
```

The various `Context` types contain a wealth of configuration options.
For the sake of this example, we're only interested in setting the
directory locations and will otherwise use the default settings.

Because the `EchoClient` class is the one responsible for starting
up the media driver, it's also responsible for shutting down the
media driver and Aeron when necessary. It implements the standard
`java.io.Closeable` interface and does the necessary cleanup in the
`close` method.

[EchoClient.close()](EchoClient.java)
```{include=out/echo_client_close.txt}
```

Now we need to define a simple (blocking) `run` method that attempts to
connect to the server and then goes into an infinite loop sending
and receiving messages. For the sake of producing readable output, we'll
limit to polling for messages once per second. In real applications
with low-latency requirements, this would obviously be completely
counterproductive and a more sensible delay would be used. The
method is constructed from several parts, so we'll define each of
those as their own methods and then connect them at the end.

Firstly, we need to create a _subscription_ that will be used to receive messages from the
remote server. This is somewhat analogous, for those familiar with
UDP programming, to opening a UDP socket and binding it to a local
address and port. We create the subscription by constructing a
[channel URI](https://github.com/real-logic/aeron/wiki/Channel-Configuration)
based on the local address given in the `create` method. We use
the convenient `ChannelUriStringBuilder` class to create a URI
string specifying the details of the subscription. We state that
we want to use `udp` as the underlying transport, and that we want
the channel to be _reliable_ (meaning that lost messages will be
retransmitted, and messages will be delivered to the remote side
in the order that they were sent). We also specify a
[stream ID](https://github.com/real-logic/aeron/wiki/Protocol-Specification#stream-setup)
when creating the subscription. Aeron is capable of multiplexing
several independent _streams_ of messages into a single connection.
It's therefore necessary for the client and server to agree on
a stream ID that will be used for communcation. In this case we
simply pick an arbitrary value of `0x2044f002`, but any non-zero
32-bit unsigned value can be used; the choice is entirely up to
the application.

[EchoClient.setupSubscription()](EchoClient.java)
```{include=out/echo_client_setup_sub.txt}
```

If, for this example, we assume a client at `10.10.1.100` using a
local port `8000`, the resulting channel URI will look something like:

```
aeron:udp?endpoint=10.10.1.100:8000|reliable=true
```

We then create a _publication_ that will be used to send messages
to the server. The procedure for creating the publication is very
similar to that of the _subscription_, so the explanation won't be
repeated here.

[EchoClient.setupPublication()](EchoClient.java)
```{include=out/echo_client_setup_pub.txt}
```

If, for this example, we assume a server at `10.10.1.200` using a
local port `9000`, the resulting channel URI will look something like:

```
aeron:udp?endpoint=10.10.1.200:9000|reliable=true
```

We now define a `runLoop` method that takes a created publication
and subscription and simply loops forever, sending and receiving
messages.

[EchoClient.runLoop()](EchoClient.java)
```{include=out/echo_client_run_loop.txt}
```

The method first creates a buffer that will be used to store incoming
and outgoing data. Some of the ways in which Aeron achieves a very
high degree of performance include using native memory, allocating
memory up-front in order to avoid producing garbage during steady-state
execution, and eliminating buffer copying as much as is possible
on the JVM. In order to assist with this, we allocate a `2KiB` direct
byte buffer (with 16 byte alignment) to store messages, and use
the `UnsafeBuffer` class from Aeron's associated data structure
package [Agrona](https://github.com/real-logic/Agrona) to get very
high-performance (unsafe) memory operations on the given
buffer.

The method then loops forever, polling the subscription for new
messages, and sending a random integer string to the publication,
waiting for a second at each iteration.

The `sendMessage` method is a more-or-less uninteresting utility
method that simply packs the given string into the buffer we allocated
at the start of the method. It does not do any particular error handling:
Message sending can fail for the reasons given in the documentation
for the `Publication.offer` method, and real applications should do the
appropriate error handling.

[EchoClient.sendMessage()](EchoClient.java)
```{include=out/echo_client_send_message.txt}
```

The `poll` method, defined on the `Subscription` type, takes a function
of type `FragmentHandler` as an argument. This function is responsible
for parsing received messages. In our case, we don't bother to handle
fragmentation as we are extremely unlikely to ever send or receive a message that
could have exceeded the [MTU](https://en.wikipedia.org/wiki/Maximum_transmission_unit)
of the underlying UDP transport. Our `FragmentHandler` function, `onParseMessage`,
simply reads the given number of bytes from the buffer, decodes the
bytes as if they were a UTF-8 string, and then prints the result
to the standard output. Real applications should expect to have to
handle fragmentation unless they can otherwise guarantee that they
will not exceed the transport's MTU.

[EchoClient.onParseMessage()](EchoClient.java)
```{include=out/echo_client_on_parse_message.txt}
```

Now that we have all of the required pieces, the `run` method is
trivial:

[EchoClient.run()](EchoClient.java)
```{include=out/echo_client_run.txt}
```

This is actually all that is required to have a working client. For
ease of testing, a simple `main` method can be defined that takes
command line arguments:

[EchoClient.main()](EchoClient.java)
```{include=out/echo_client_main.txt}
```

## Echo Server

