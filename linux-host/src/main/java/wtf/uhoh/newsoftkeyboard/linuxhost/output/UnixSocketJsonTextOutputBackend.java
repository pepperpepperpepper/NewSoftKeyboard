package wtf.uhoh.newsoftkeyboard.linuxhost.output;

import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import wtf.uhoh.newsoftkeyboard.keyboard.core.actions.SemanticAction;
import wtf.uhoh.newsoftkeyboard.keyboard.core.adapters.TextOutputBackend;

public final class UnixSocketJsonTextOutputBackend implements TextOutputBackend {
  private final Path socketPath;

  public UnixSocketJsonTextOutputBackend(Path socketPath) {
    this.socketPath = Objects.requireNonNull(socketPath);
  }

  @Override
  public void apply(List<SemanticAction> actions) {
    Objects.requireNonNull(actions);
    if (actions.isEmpty()) return;

    UnixDomainSocketAddress address = UnixDomainSocketAddress.of(socketPath);
    try (SocketChannel channel = SocketChannel.open(address)) {
      for (SemanticAction action : actions) {
        writeUtf8Line(channel, SemanticActionJsonLine.toJsonLine(action));
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed writing to unix socket: " + socketPath, e);
    }
  }

  private static void writeUtf8Line(SocketChannel channel, String line) throws IOException {
    byte[] bytes = (line + "\n").getBytes(StandardCharsets.UTF_8);
    ByteBuffer buffer = ByteBuffer.wrap(bytes);
    while (buffer.hasRemaining()) {
      channel.write(buffer);
    }
  }
}
