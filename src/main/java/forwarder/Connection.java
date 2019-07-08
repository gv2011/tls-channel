package forwarder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;

import javax.net.ssl.SNIServerName;

import tlschannel.impl.TlsExplorer;

public class Connection {

  private static enum State{WAIT_FOR_SERVER_NAME, FORWARDING, CLOSED}

  private State state = State.WAIT_FOR_SERVER_NAME;
  private final SocketChannel clientChannel;
  private SocketChannel serverChannel;
  private ByteBuffer toServer = ByteBuffer.allocate(1024);
  private final ByteBuffer toClient = ByteBuffer.allocate(1024);
  private boolean toServerReading = true;
  private String server = "";
  private int requiredSize = -1;
  private final ServerMapping serverMapping;
  private boolean toServerEnd;
  private boolean toClientReading;
  private boolean toClientEnd;

  public Connection(final ServerMapping serverMapping, final SocketChannel clientChannel) {
    this.serverMapping = serverMapping;
    this.clientChannel = clientChannel;
  }

  public void update(final SelectionKey key) throws IOException {
    if(state==State.WAIT_FOR_SERVER_NAME) {
      assert(key.isReadable());
      assert key.channel().equals(clientChannel);
      assert server.isEmpty();
      if(requiredSize!=-1) assert toServer.position()<requiredSize;
      clientChannel.read(toServer);
      if(toServer.position()>=TlsExplorer.RECORD_HEADER_SIZE) {
        toServer.flip();
        if(requiredSize==-1) {
          requiredSize = TlsExplorer.getRequiredSize(toServer);
          if(requiredSize>toServer.capacity()) {
            final ByteBuffer newBuffer = ByteBuffer.allocate(requiredSize);
            toServer.position(0);
            newBuffer.put(toServer);
            newBuffer.flip();
            toServer = newBuffer;
          }
          else toServer.position(0);
        }
        if(toServer.limit()>=requiredSize) {
          server = extractHostName(TlsExplorer.explore(toServer));
          serverChannel = SocketChannel.open(serverMapping.getAddress(server));
          serverChannel.configureBlocking(false);
          serverChannel.register(key.selector(), SelectionKey.OP_READ + SelectionKey.OP_WRITE, this);
          state = State.FORWARDING;
          toServer.position(0);
          toServerReading = false;
          toClientReading = true;
        }else {
          toServer.position(toServer.limit());
          toServer.limit(toServer.capacity());
        }
      }
    }else if(state==State.FORWARDING) {
      if(key.channel().equals(serverChannel)) {
        if(key.isWritable() && !toServerReading && !toServerEnd) {
          serverChannel.write(toServer);
          if(!toServer.hasRemaining()) {
            toServer.position(0);
            toServer.limit(toServer.capacity());
            toServerReading = true;
          }
        }
        if(key.isReadable() && toClientReading) {
          final int count = serverChannel.read(toClient);
          if(count==-1) {
            toClient.flip();
            assert !toClient.hasRemaining();
            toClientEnd = true;
            toClientReading=false;
            clientChannel.shutdownOutput();
          }
          else if(toClient.position()>0) {
            toClient.flip();
            toClientReading=false;
          }
        }
      }else if(key.channel().equals(clientChannel)) {
        if(key.isReadable() && toServerReading) {
          assert toServer.position()==0;
          assert toServer.limit()==toServer.capacity();
          final int count = clientChannel.read(toServer);
          if(count==-1) {
            toServer.flip();
            assert !toServer.hasRemaining();
            toServerEnd = true;
            toServerReading=false;
            serverChannel.shutdownOutput();
          }
          else if(toServer.position()>0) {
            toServer.flip();
            toServerReading=false;
          }
        }
      }
      if(key.isWritable() && !toClientReading && !toClientEnd) {
        clientChannel.write(toClient);
        if(!toClient.hasRemaining()) {
          toClient.position(0);
          toClient.limit(toClient.capacity());
          toClientReading = true;
        }
      }
    }
    else assert false;
    if(toServerEnd && toClientEnd) {
      clientChannel.close();
      serverChannel.close();
    }
  }

  private String extractHostName(final Map<Integer, SNIServerName> explore) {
    // TODO Auto-generated method stub
    return "test";
  }

}
