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
trading, VOIP, video streaming, etc. In particular, the Java implementation
is designed such that it will produce no garbage during steady state
execution, reducing memory pressure and work for the collector.

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

The actual low-level handling of the transmission medium (such as
UDP sockets) is handled by a module of code known as the _media
driver_. The media driver can be either run standalone as a separate
process, or can be embedded within the application. For the examples
presented in this guide, we assume that the media driver will be
embedded within the application and, therefore, the application is
responsible for configuring, starting up, and shutting down the media
driver correctly.

# A Client And Server (Take 1) {#client_server_take_1}

As a first step, we'll write a trivial `echo` server and client:
Clients can connect to the server and send UTF-8 strings, and the
server will send them back. More formally, the client will:

[client_protocol]:

  1. Send an initial string `HELLO <port>`, where `<port>` is
     an unsigned decimal integer indicating the port to which
     responses from the server should be sent.

  2. If the initial `HELLO` string has been sent, the client will
     send an infinite series of arbitrary unsigned 32-bit decimal
     integers encoded as UTF-8 strings, one string per second.

The server will:

[server_protocol]:

  1. Wait for a client to connect and then read the initial `HELLO <port>`
     message.

  2. For each connected client `c`, the server will read a string `s`
     from `c` and then send back the exact same string `s` to the
     source address of `c` and the port `p` that `c` specified in its
     initial `HELLO` string.

We'll step through the simplest possible implementation that can work,
and then critically assess it with an eye to producing a second, better
implementation. No attempt will be made to produce efficient code:
We are, after all, sending UTF-8 strings as messages; There _will_
be allocations! For this example code, if any choice must be made
between efficiency, correctness, or simplicity, simplicity will be
chosen every time.

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
counterproductive and a more sensible delay would be used [^poll]. The
method is constructed from several parts, so we'll define each of
those as their own methods and then compose them at the end.

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
byte buffer (with `16` byte alignment) to store messages, and use
the `UnsafeBuffer` class from Aeron's associated data structure
package [Agrona](https://github.com/real-logic/Agrona) to get very
high-performance (unsafe) memory operations on the given
buffer.

The method then sends a string `HELLO <port>` where `<port>` is
replaced with the port number used to create the _subscription_
earlier. The server is required to address responses to this port
and so those will be made available on the _subscription_ that the
client opened.

The method then loops forever, polling the subscription for new
messages, and sending a random integer string to the publication,
waiting for a second at each iteration.

The `sendMessage` method is a more-or-less uninteresting utility
method that simply packs the given string into the buffer we allocated
at the start of the method. It does not do any particular error handling:
Message sending can fail for the reasons given in the documentation
for the `Publication.offer()` method, and real applications should do the
appropriate error handling [^overflow]. Our implementation simply tries to write
a message, retrying up to five times in total, before giving up.
The method returns `true` if the message was sent, and logs an
error message and returns `false` otherwise. Better approaches for
real applications are [discussed later](#message_sending_not_robust).

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

This is the bare minimum that is required to have a working client. For
ease of testing, a simple `main` method can be defined that takes
command line arguments:

[EchoClient.main()](EchoClient.java)
```{include=out/echo_client_main.txt}
```

## Echo Server

The design of the server is not radically different to that of the
client. The main differences are in when and where publications
and subscriptions are created. The client simply opens a
publication/subscription pair to the only peer with which it will
communicate (the server) and maintains them until the user has had
enough. The server, however, needs to create publications and/or
subscriptions in response to clients connecting, and needs to be able
to address clients individually in order to send responses.

We structure the `EchoServer` server similarly to the `EchoClient`
including the static `create` method that sets up Aeron and the
media driver. The only difference is that the server does not have
a _remote address_ as part of its configuration information; it only
specifies a local address to which clients will connect.

The `run` method, however, is different in several aspects.

[EchoServer.setupSubscription()](EchoServer.java)
```{include=out/echo_server_setup_sub.txt}
```

The _subscription_ configured by the server is augmented with
a pair of _image_ handlers. An _image_, in Aeron terminology, is
the replication of a _publication_ stream on the _subscription_ side.
In other words, when a client creates a _publication_ to talk to
the server, the server obtains an _image_ of the client's _publication_
that contains a _subscription_ from which the server can read. When
the client writes a message to its _publication_, the server can
read a message from the _subscription_ in the _image_.

When an _image_ becomes available, this is our indication that a
client has connected. When an _image_ becomes unavailable, this is
our indication that a client has disconnected.

We provide the subscription with pair of method references,
`this::onClientConnected` and `this::onClientDisconnected`, that
will be called when an _image_ becomes available and unavailable,
respectively.

[EchoServer.onClientConnected()](EchoServer.java)
```{include=out/echo_server_on_client_connected.txt}
```

When an _image_ becomes available, we take note of the _session ID_
of the _image_. This can be effectively used to uniquely identify a
client with respect to that particular _subscription_. We create a
new instance of a `ServerClient` class used to store per-client state
on the server, and store it in map associating _session IDs_ with
`ServerClient` instances. The details of the `ServerClient` class
will be discussed shortly.

Similarly, in the `onClientDisconnected` method, we find the
client that appears to be disconnecting using the _session ID_ of
the _image_, call the `close` method on the corresponding
`ServerClient` instance, assuming that one exists, and remove
`ServerClient` instance from the table of clients.

[EchoServer.onClientDisconnected()](EchoServer.java)
```{include=out/echo_server_on_client_disconnected.txt}
```

The server does not create a _publication_ in the `run` method
as the client did: It defers the creation of _publications_
until clients have connected for reasons that will be discussed
shortly.

The complete `run` method, therefore, looks like this:

[EchoServer.run()](EchoServer.java)
```{include=out/echo_server_run.txt}
```

The `runLoop` method on the server is simplified when compared
to the analogous method on the client. The method simply polls
the main subscription repeatedly:

[EchoServer.runLoop()](EchoServer.java)
```{include=out/echo_server_run_loop.txt}
```

The main difference is the work that now takes place in the `onParseMessage`
method:

[EchoServer.onParseMessage()](EchoServer.java)
```{include=out/echo_server_on_parse_message.txt}
```

We first take the _session ID_ provided to us by the `Header` value
passed to us by Aeron. The _session ID_ is used to look up a client
in the table of clients populated by the `onClientConnected` method.
Assuming that a client actually exists with a matching _session ID_,
a UTF-8 string is decoded from the buffer as it is in the `EchoClient`
implementation, but the decoded string is then given to the corresponding
`ServerClient` instance to be processed via its `onReceiveMessage`
method.

Due to the small size of the `ServerClient` class (it effectively
only contains a single method that does interesting work), the code
is published here in its entirety:

[EchoServer.ServerClient](EchoServer.java)
```{include=out/echo_server_server_client.txt}
```

The `ServerClient` class maintains a `State` field which may either be
`CONNECTED` or `INITIAL`. The client begins in the `INITIAL` state and
then transitions to the `CONNECTED` state after successfully processing
the `HELLO` string that is [expected to be sent by connecting clients
as their first message](#client_server_take_1). The `onReceiveMessage`
method checks to see if the client is in the `INITIAL` state or
the `CONNECTED` state. If the client is in the `INITIAL` state,
the message is passed to the `onReceiveMessageInitial` method.
This method parses what it assumes will be a `HELLO` string and
constructs a new _publication_ that will be used to send messages back
to the client. Aeron provides us with both the source address of the
client and the ephemeral port the client used to send the message we
just received via the `Image.sourceIdentity()` method. However, we
cannot send messages back to the ephemeral port the client used: We
need to send messages to the port the client specified in the `HELLO`
message so that they are readable via the _subscription_ the client
created in `EchoClient.setupSubscription()` for that purpose.

When using UDP as a transport, the result of the `sourceIdentity()`
call will be a string of the form `ip-address:port`. For a client at
`10.10.1.200` using an arbitrary high-numbered ephemeral UDP port, the
string may look something like `10.10.1.200:53618`. The simplest way
to parse a string of this form is to simply delegate parsing to the standard
`java.net.URI` class. We do this by constructing a URI containing the
original address and port, extracting the IP address from the resulting
`URI` value, substituting the port specified by the client in the
`HELLO` string, and then opening a new _publication_ in a way that
should now be familiar.

Assuming that all of this proceeds without issue, the client is moved
to the `CONNECTED` state and the method returns. From that point on,
any message received by that particular client instance will be sent
back to the client via the newly created _publication_.

At this point, we appear to have a working client and server. An
additional `main` method is added to the server to help with testing
from the command line:

[EchoServer.main()](EchoServer.java)
```{include=out/echo_server_main.txt}
```

Executing the server and a client from the command line produces the expected output:

```
server$ java -classpath target/com.io7m.aeron-guide-0.0.1.jar com.io7m.aeron_guide.take1.EchoServer /tmp/aeron-server 10.10.1.100 9000
20:26:47.070 [main] DEBUG com.io7m.aeron_guide.take1.EchoServer - subscription URI: aeron:udp?endpoint=10.10.1.100:9000|reliable=true
20:28:09.981 [aeron-client-conductor] DEBUG com.io7m.aeron_guide.take1.EchoServer - onClientConnected: 10.10.1.100:44501
20:28:10.988 [main] DEBUG com.io7m.aeron_guide.take1.EchoServer - receive [0x896291375]: HELLO 8000
20:28:11.049 [main] DEBUG com.io7m.aeron_guide.take1.EchoServer - receive [0x896291375]: 2745822766
20:28:11.050 [main] DEBUG com.io7m.aeron_guide.take1.EchoServer - send: [session 0x562238613] 2745822766
20:28:11.953 [main] DEBUG com.io7m.aeron_guide.take1.EchoServer - receive [0x896291375]: 1016181810
20:28:11.953 [main] DEBUG com.io7m.aeron_guide.take1.EchoServer - send: [session 0x562238613] 1016181810
20:28:12.955 [main] DEBUG com.io7m.aeron_guide.take1.EchoServer - receive [0x896291375]: 296510575
20:28:12.955 [main] DEBUG com.io7m.aeron_guide.take1.EchoServer - send: [session 0x562238613] 296510575
20:28:13.957 [main] DEBUG com.io7m.aeron_guide.take1.EchoServer - receive [0x896291375]: 3276793170
20:28:13.957 [main] DEBUG com.io7m.aeron_guide.take1.EchoServer - send: [session 0x562238613] 3276793170

client$ java -classpath target/com.io7m.aeron-guide-0.0.1.jar com.io7m.aeron_guide.take1.EchoClient /tmp/aeron-client0 10.10.1.100 8000 10.10.1.100 9000
20:28:09.826 [main] DEBUG com.io7m.aeron_guide.take1.EchoClient - subscription URI: aeron:udp?endpoint=10.10.1.100:8000|reliable=true
20:28:09.846 [main] DEBUG com.io7m.aeron_guide.take1.EchoClient - publication URI: aeron:udp?endpoint=10.10.1.100:9000|reliable=true
20:28:10.926 [main] DEBUG com.io7m.aeron_guide.take1.EchoClient - send: HELLO 8000
20:28:10.927 [main] DEBUG com.io7m.aeron_guide.take1.EchoClient - send: 2745822766
20:28:11.928 [main] DEBUG com.io7m.aeron_guide.take1.EchoClient - send: 1016181810
20:28:11.933 [main] DEBUG com.io7m.aeron_guide.take1.EchoClient - response: 2745822766
20:28:12.934 [main] DEBUG com.io7m.aeron_guide.take1.EchoClient - send: 296510575
20:28:12.934 [main] DEBUG com.io7m.aeron_guide.take1.EchoClient - response: 1016181810
20:28:13.935 [main] DEBUG com.io7m.aeron_guide.take1.EchoClient - send: 3276793170
20:28:13.935 [main] DEBUG com.io7m.aeron_guide.take1.EchoClient - response: 296510575
```

The implementation works, but suffers from a number of weaknesses ranging from
benign to potentially fatal.

## Implementation Weaknesses

### EchoClient Server Disconnections

The `EchoClient` implementation does not handle the case of the server
either disconnecting or not existing in the first place. It will simply
try to send messages forever and will ignore the fact that it never
gets a response. Handling this was deliberately left out of the implementation
for the sake of keeping the code as simple as possible. The way to
fix this is twofold.

Firstly, the client can simply give up trying to send the initial
`HELLO` message if no response has been received after a reasonable
amount of time has elapsed. This solution can work well for protocols
where the first message in the conversation is expected to be sent
by the client. Aeron also indicates when a `Publication` is no longer
connected by returning `NOT_CONNECTED` or `CLOSED` when calling `offer()`
on the `Publication`. Real applications can react accordingly rather than
just logging a failure message (like the `EchoClient` and `EchoServer`) and
continuing.

Secondly, the client can specify _image handlers_ on the _subscription_
it creates in the same manner as the server. When an _image_ becomes
available, that means that the server has sent a message and is therefore
presumably alive and willing to talk to the client. When an _image_
becomes unavailable, the server is no longer willing or able to
talk to the client. This can work well for protocols where the first
message in the conversation is expected to be sent by the server.

### MTU Handling Is Implicit

Sending messages over the open internet imposes an upper bound on
the [MTU](https://en.wikipedia.org/wiki/Maximum_transmission_unit)
that an application can use for individual messages.

TODO: Note when Aeron will fragment messages.

As a general rule of thumb, the `MTU` for UDP packets sent over
the open internet should be `<= 1200` bytes. Aeron adds the further
restriction that `MTU` values must be a multiple of `32`.

The `EchoClient` and `EchoServer` implementations use UTF-8 strings
that are not expected to be longer than about `16` bytes, and so
assume that fragmentation will never occur and make no attempt to
handle it.

### Clients Sending Bad Messages Are Not Killed

The `EchoServer` implementation has a rather serious failing in
that if the first message received by a client is not parseable
as a simple `HELLO <port>` string, the offending client will
never be told about this and the server will continue to process
every subsequent message from the client as if it was an unparseable
`HELLO` string.

This is partly a result of an underspecification of the protocol:
The server has no way to tell a client that a fatal error has occurred
and that the client should go away (or at least retry the `HELLO`
string). This is also an implementation issue: The server has no
means to forcibly disconnect a client (and as discussed previouly,
the client would not notice that it had been disconnected anyway).

### Message Sending Is Not Robust {#message_sending_not_robust}

The way that messages are sent is insufficient in the sense that a
failure to send a message is not a hard error. Real applications must
be prepared to queue and retry messages as necessary, and should raise
exceptions if messages absolutely cannot be sent after a reasonable
number of attempts.

### Work Takes Place On Aeron Threads

Currently, all work takes place on threads controlled by Aeron. As
per the documentation, `Publication` values are thread-safe, and
`Subscription` values are not. Real applications should expect to
take messages from a `Subscription` and place them into a queue
for processing by one or more application threads [^disruptor] to
avoid blocking the Aeron conductor threads. See [Thread Utilisation](https://github.com/real-logic/aeron/wiki/Thread-Utilisation)
for details.

### EchoClient Cannot Be Behind NAT

This is the most serious issue with the implementation described
so far (and astute programmers familiar with UDP networking will
already have noticed): The implementation is fundamentally incompatible
with [Network Address Translation](https://en.wikipedia.org/wiki/Network_address_translation).

The server must be able to open connections directly to clients, and
this is something that is not possible without clients enabling port
forwarding on the NAT routers that they are inevitably sitting behind.
The same is true of connections opened to the server by the clients,
but this is less of an issue in that server operators are used to
routinely enabling port forwarding on their routers to allow clients
to connect in. For a multiplayer game with non-technical players
running clients on the open internet, requring each client to enable
port forwarding just to be able to connect to a server would be
unacceptable.

Additionally, having clients specify port information in an
application-level protocol is distasteful. Anyone familiar with
UDP programming can open a socket, bind it, and then read and write
datagrams without even thinking about NAT: Routers will statefully
match inbound datagrams to previously sent outbound datagrams and
allow them to pass through unheeded. It seems unpleasant that Aeron
would require us to give up this essentially OS-level functionality.
Thankfully, Aeron includes a somewhat sparsely documented feature
known as [multi-destination cast](https://github.com/real-logic/aeron/wiki/Protocol-Specification#multi-destination-cast-mode-of-operation)
that can be used to traverse NAT systems reliably, removing the
requirement for servers to connect directly back to clients.

# A Client And Server (Take 2) {#client_server_take_2}

[^poll]: The `Subscription` interface also contains many different
         variations on `poll` that provide for different semantics
         depending on the requirements of the application.

[^disruptor]: A pattern recommended by and for the [Disruptor](https://lmax-exchange.github.io/disruptor/).

[^overflow]: The method also makes no attempt to check if the
             contents of the string would overflow the buffer. This
             could have spectacular consequences given the nature
             of `Unsafe` buffers.
