package com.io7m.aeron_guide.take2;

import io.aeron.Aeron;
import io.aeron.AvailableImageHandler;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.ConcurrentPublication;
import io.aeron.Subscription;
import io.aeron.UnavailableImageHandler;

import java.net.InetAddress;
import java.util.Objects;

import static java.lang.Boolean.TRUE;

/**
 * Convenience functions to construct publications and subscriptions.
 */

public final class EchoChannels
{
  private EchoChannels()
  {

  }

  /**
   * Create a publication at the given address and port, using the given
   * stream ID.
   *
   * @param aeron     The Aeron instance
   * @param address   The address
   * @param port      The port
   * @param stream_id The stream ID
   *
   * @return A new publication
   */

  public static ConcurrentPublication createPublication(
    final Aeron aeron,
    final InetAddress address,
    final int port,
    final int stream_id)
  {
    Objects.requireNonNull(aeron, "aeron");
    Objects.requireNonNull(address, "address");

    final String addr_string =
      address.toString().replaceFirst("^/", "");

    final String pub_uri =
      new ChannelUriStringBuilder()
        .reliable(TRUE)
        .media("udp")
        .endpoint(
          new StringBuilder(64)
            .append(addr_string)
            .append(":")
            .append(Integer.toUnsignedString(port))
            .toString())
        .build();

    return aeron.addPublication(pub_uri, stream_id);
  }

  /**
   * Create a subscription with a control port (for dynamic MDC) at the given
   * address and port, using the given stream ID.
   *
   * @param aeron     The Aeron instance
   * @param address   The address
   * @param port      The port
   * @param stream_id The stream ID
   *
   * @return A new publication
   */

  public static Subscription createSubscriptionDynamicMDC(
    final Aeron aeron,
    final InetAddress address,
    final int port,
    final int stream_id)
  {
    Objects.requireNonNull(aeron, "aeron");
    Objects.requireNonNull(address, "address");

    final String addr_string =
      address.toString().replaceFirst("^/", "");

    final String sub_uri =
      new ChannelUriStringBuilder()
        .reliable(TRUE)
        .media("udp")
        .controlEndpoint(
          new StringBuilder(64)
            .append(addr_string)
            .append(":")
            .append(Integer.toUnsignedString(port))
            .toString())
        .controlMode("dynamic")
        .build();

    return aeron.addSubscription(sub_uri, stream_id);
  }

  /**
   * Create a publication with a control port (for dynamic MDC) at the given
   * address and port, using the given stream ID.
   *
   * @param aeron     The Aeron instance
   * @param address   The address
   * @param port      The port
   * @param stream_id The stream ID
   *
   * @return A new publication
   */

  public static ConcurrentPublication createPublicationDynamicMDC(
    final Aeron aeron,
    final InetAddress address,
    final int port,
    final int stream_id)
  {
    Objects.requireNonNull(aeron, "aeron");
    Objects.requireNonNull(address, "address");

    final String addr_string =
      address.toString().replaceFirst("^/", "");

    final String pub_uri =
      new ChannelUriStringBuilder()
        .reliable(TRUE)
        .media("udp")
        .controlEndpoint(
          new StringBuilder(32)
            .append(addr_string)
            .append(":")
            .append(Integer.toUnsignedString(port))
            .toString())
        .controlMode("dynamic")
        .build();

    return aeron.addPublication(pub_uri, stream_id);
  }

  /**
   * Create a subscription at the given address and port, using the given
   * stream ID and image handlers.
   *
   * @param aeron                The Aeron instance
   * @param address              The address
   * @param port                 The port
   * @param stream_id            The stream ID
   * @param on_image_available   Called when an image becomes available
   * @param on_image_unavailable Called when an image becomes unavailable
   *
   * @return A new publication
   */

  public static Subscription createSubscriptionWithHandlers(
    final Aeron aeron,
    final InetAddress address,
    final int port,
    final int stream_id,
    final AvailableImageHandler on_image_available,
    final UnavailableImageHandler on_image_unavailable)
  {
    Objects.requireNonNull(aeron, "aeron");
    Objects.requireNonNull(address, "address");
    Objects.requireNonNull(on_image_available, "on_image_available");
    Objects.requireNonNull(on_image_unavailable, "on_image_unavailable");

    final String addr_string =
      address.toString().replaceFirst("^/", "");

    final String sub_uri =
      new ChannelUriStringBuilder()
        .reliable(TRUE)
        .media("udp")
        .endpoint(
          new StringBuilder(32)
            .append(addr_string)
            .append(":")
            .append(Integer.toUnsignedString(port))
            .toString())
        .build();

    return aeron.addSubscription(
      sub_uri,
      stream_id,
      on_image_available,
      on_image_unavailable);
  }

  /**
   * Create a publication with a control port (for dynamic MDC) at the given
   * address and port, using the given stream ID and session ID.
   *
   * @param aeron     The Aeron instance
   * @param address   The address
   * @param port      The port
   * @param stream_id The stream ID
   * @param session   The session ID
   *
   * @return A new publication
   */

  public static ConcurrentPublication createPublicationDynamicMDCWithSession(
    final Aeron aeron,
    final InetAddress address,
    final int port,
    final int stream_id,
    final int session)
  {
    Objects.requireNonNull(aeron, "aeron");
    Objects.requireNonNull(address, "address");

    final String addr_string =
      address.toString().replaceFirst("^/", "");

    final String pub_uri =
      new ChannelUriStringBuilder()
        .reliable(TRUE)
        .media("udp")
        .controlEndpoint(
          new StringBuilder(32)
            .append(addr_string)
            .append(":")
            .append(Integer.toUnsignedString(port))
            .toString())
        .controlMode("dynamic")
        .sessionId(Integer.valueOf(session))
        .build();

    return aeron.addPublication(pub_uri, stream_id);
  }

  /**
   * Create a subscription at the given address and port, using the given
   * stream ID, session ID, and image handlers.
   *
   * @param aeron                The Aeron instance
   * @param address              The address
   * @param port                 The port
   * @param stream_id            The stream ID
   * @param on_image_available   Called when an image becomes available
   * @param on_image_unavailable Called when an image becomes unavailable
   * @param session              The session ID
   *
   * @return A new publication
   */

  public static Subscription createSubscriptionWithHandlersAndSession(
    final Aeron aeron,
    final InetAddress address,
    final int port,
    final int stream_id,
    final AvailableImageHandler on_image_available,
    final UnavailableImageHandler on_image_unavailable,
    final int session)
  {
    Objects.requireNonNull(aeron, "aeron");
    Objects.requireNonNull(address, "address");
    Objects.requireNonNull(on_image_available, "on_image_available");
    Objects.requireNonNull(on_image_unavailable, "on_image_unavailable");

    final String addr_string =
      address.toString().replaceFirst("^/", "");

    final String sub_uri =
      new ChannelUriStringBuilder()
        .reliable(TRUE)
        .media("udp")
        .endpoint(
          new StringBuilder(32)
            .append(addr_string)
            .append(":")
            .append(Integer.toUnsignedString(port))
            .toString())
        .sessionId(Integer.valueOf(session))
        .build();

    return aeron.addSubscription(
      sub_uri,
      stream_id,
      on_image_available,
      on_image_unavailable);
  }

  /**
   * Create a subscription with a control port (for dynamic MDC) at the given
   * address and port, using the given stream ID, and session ID.
   *
   * @param aeron     The Aeron instance
   * @param address   The address
   * @param port      The port
   * @param stream_id The stream ID
   * @param session   The session ID
   *
   * @return A new publication
   */

  public static Subscription createSubscriptionDynamicMDCWithSession(
    final Aeron aeron,
    final InetAddress address,
    final int port,
    final int session,
    final int stream_id)
  {
    Objects.requireNonNull(aeron, "aeron");
    Objects.requireNonNull(address, "address");

    final String addr_string =
      address.toString().replaceFirst("^/", "");

    final String sub_uri =
      new ChannelUriStringBuilder()
        .reliable(TRUE)
        .media("udp")
        .controlEndpoint(
          new StringBuilder(64)
            .append(addr_string)
            .append(":")
            .append(Integer.toUnsignedString(port))
            .toString())
        .controlMode("dynamic")
        .sessionId(Integer.valueOf(session))
        .build();

    return aeron.addSubscription(sub_uri, stream_id);
  }

  /**
   * Create a publication at the given address and port, using the given
   * stream ID and session ID.
   *
   * @param aeron     The Aeron instance
   * @param address   The address
   * @param port      The port
   * @param stream_id The stream ID
   * @param session   The session ID
   *
   * @return A new publication
   */

  public static ConcurrentPublication createPublicationWithSession(
    final Aeron aeron,
    final InetAddress address,
    final int port,
    final int session,
    final int stream_id)
  {
    Objects.requireNonNull(aeron, "aeron");
    Objects.requireNonNull(address, "address");

    final String addr_string =
      address.toString().replaceFirst("^/", "");

    final String pub_uri =
      new ChannelUriStringBuilder()
        .reliable(TRUE)
        .media("udp")
        .endpoint(
          new StringBuilder(64)
            .append(addr_string)
            .append(":")
            .append(Integer.toUnsignedString(port))
            .toString())
        .sessionId(Integer.valueOf(session))
        .build();

    return aeron.addPublication(pub_uri, stream_id);
  }
}
