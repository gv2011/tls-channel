package forwarder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;


public class NonBlockingForwarder {

  public static void main(final String[] args) throws Exception {
    new NonBlockingForwarder().run();
  }

  private void run() throws Exception {
    final ServerSocketChannel ssChannel = ServerSocketChannel.open();
    ssChannel.configureBlocking(false);
    final InetAddress hostIPAddress = InetAddress.getLocalHost();
    final int port = 1000;
    final Selector selector = Selector.open();
    ssChannel.socket().bind(new InetSocketAddress(hostIPAddress, port));
    ssChannel.register(selector, SelectionKey.OP_ACCEPT);
    while (true) {
      if (selector.select() > 0) {
        processReadySet(selector.selectedKeys());
      }
    }
  }

  public void processReadySet(final Set<SelectionKey> readySet) throws Exception {
    final Iterator<SelectionKey> iterator = readySet.iterator();
    while (iterator.hasNext()) {
      final SelectionKey key = iterator.next();
      iterator.remove();
      if (key.isAcceptable()) {
        createNewConnection(key.selector(), (ServerSocketChannel) key.channel());
        final ServerSocketChannel ssChannel = (ServerSocketChannel) key.channel();
        final SocketChannel sChannel = ssChannel.accept();
        sChannel.configureBlocking(false);
        sChannel.register(key.selector(), SelectionKey.OP_READ);
      }
      else {
        ((Connection)key.attachment()).update(key);
      }
    }
  }

  private void createNewConnection(final Selector selector, final ServerSocketChannel serverSocketChannel) throws IOException {
    final SocketChannel sChannel = serverSocketChannel.accept();
    sChannel.configureBlocking(false);
    final Connection cn = new Connection(null, sChannel);
    sChannel.register(selector, SelectionKey.OP_READ, cn);
  }

  public ByteBuffer processRead(final SelectionKey key) throws Exception {
    final SocketChannel sChannel = (SocketChannel) key.channel();
    final ByteBuffer buffer = ByteBuffer.allocate(1024);
    final int bytesCount = sChannel.read(buffer);
    return buffer;
  }
}
