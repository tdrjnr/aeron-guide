package com.io7m.aeron_guide.take3;

import io.aeron.Publication;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Convenience functions to send messages.
 */

public final class MessagesHelper
{
  private static final Logger LOG = LoggerFactory.getLogger(MessagesHelper.class);

  private MessagesHelper()
  {

  }

  /**
   * Send the given message to the given publication. If the publication fails
   * to accept the message, the method will retry {@code 5} times, waiting
   * {@code 100} milliseconds each time, before throwing an exception.
   *
   * @param pub    The publication
   * @param buffer A buffer that will hold the message for sending
   * @param text   The message
   *
   * @return The new publication stream position
   *
   * @throws IOException If the message cannot be sent
   */

  public static long sendMessage(
    final Publication pub,
    final UnsafeBuffer buffer,
    final String text)
    throws IOException
  {
    Objects.requireNonNull(pub, "publication");
    Objects.requireNonNull(buffer, "buffer");
    Objects.requireNonNull(text, "text");

    LOG.trace("[{}] send: {}", Integer.toString(pub.sessionId()), text);

    final byte[] value = text.getBytes(UTF_8);
    buffer.putBytes(0, value);

    long result = 0L;
    for (int index = 0; index < 5; ++index) {
      result = pub.offer(buffer, 0, value.length);
      if (result < 0L) {
        try {
          Thread.sleep(100L);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        continue;
      }
      return result;
    }

    throw new IOException(
      "Could not send message: Error code: " + errorCodeName(result));
  }

  private static String errorCodeName(final long result)
  {
    if (result == Publication.NOT_CONNECTED) {
      return "Not connected";
    }
    if (result == Publication.ADMIN_ACTION) {
      return "Administrative action";
    }
    if (result == Publication.BACK_PRESSURED) {
      return "Back pressured";
    }
    if (result == Publication.CLOSED) {
      return "Publication is closed";
    }
    if (result == Publication.MAX_POSITION_EXCEEDED) {
      return "Maximum term position exceeded";
    }
    throw new IllegalStateException();
  }

  /**
   * Extract a UTF-8 encoded string from the given buffer.
   *
   * @param buffer The buffer
   * @param offset The offset from the start of the buffer
   * @param length The number of bytes to extract
   *
   * @return A string
   */

  public static String parseMessageUTF8(
    final DirectBuffer buffer,
    final int offset,
    final int length)
  {
    Objects.requireNonNull(buffer, "buffer");
    final byte[] data = new byte[length];
    buffer.getBytes(offset, data);
    return new String(data, UTF_8);
  }
}
