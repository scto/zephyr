package io.sunshower.common.io;

import io.sunshower.kernel.io.ChannelTransferListener;
import io.sunshower.kernel.io.ObservableChannelTransferListener;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.util.concurrent.*;
import lombok.val;

public class MonitorableFileTransfer extends ObservableChannelTransferListener
    implements Callable<File>, ChannelTransferListener {

  private final transient long expectedSize;
  private final transient File destination;
  private final transient ReadableByteChannel channel;

  public MonitorableFileTransfer(File destination, long expectedSize, ReadableByteChannel channel) {
    this.channel = channel;
    this.destination = destination;
    this.expectedSize = expectedSize;
  }

  @Override
  public File call() throws Exception {
    val source = new MonitorableByteChannel(channel, this, expectedSize);
    try (val outputstream = Files.newOutputStream(destination.toPath());
        val outputChannel = Channels.newChannel(outputstream)) {
      copy(source, outputChannel);
    } catch (IOException ex) {
      onError(source, ex);
      throw ex;
    } finally {
      onComplete(source);
    }
    return destination;
  }

  public static void copy(ReadableByteChannel in, WritableByteChannel out) throws IOException {
    ByteBuffer buffer = ByteBuffer.allocateDirect(32 * 1024);
    while (in.read(buffer) != -1 || buffer.position() > 0) {
      buffer.flip();

      out.write(buffer);
      buffer.compact();
    }
  }
}
