package forwarder;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Map;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLProtocolException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import tlschannel.impl.TlsExplorer;

public class Forwarder {

  public static void main(final String[] args) throws Exception {
    //System.setProperty("javax.net.debug", "ssl:handshake");
    new Forwarder().run();

  }

  private final ByteArrayOutputStream received = new ByteArrayOutputStream();
  private SSLSocket socket;
  private ServerSocket serverSocket;
  private volatile Socket receiving;

  private void run() throws UnknownHostException, IOException, InterruptedException {
    final ServerSocketFactory ssf = ServerSocketFactory.getDefault();
    final int port = 1000;
    serverSocket = ssf.createServerSocket(port);
    final Thread handler = handle(serverSocket);
    final SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
    socket = (SSLSocket) sf.createSocket("d1.letero.com", port);
    final OutputStream os = socket.getOutputStream();
    final Thread terminator = initTermination();
    try {
      os.write("Hallo".getBytes(UTF_8));
    } catch (final SSLProtocolException e) {System.out.println(e.getMessage());}
    terminator.join();
    handler.join();
    byte[] header;
    synchronized(this) {
      received.flush();
      header = received.toByteArray();
    }
    System.out.println(header.length);
    final ByteBuffer source = ByteBuffer.wrap(header);
    final Map<Integer, SNIServerName> explore = TlsExplorer.explore(source );
    explore.forEach((i,n)->System.out.println(i+": "+n.getType()+"|"+new String(n.getEncoded())));
  }

  private Thread initTermination() {
    final Thread thread = new Thread(()->{
      try {
        Thread.sleep(1000);
        socket.close();
        serverSocket.close();
        receiving.close();
      } catch (final Exception e) {
        e.printStackTrace(System.err);
      }
    });
    thread.start();
    return thread;
  }

  private Thread handle(final ServerSocket serverSocket) {
    final Thread thread = new Thread(()->{
      try {
        receiving = serverSocket.accept();
        final InputStream is = receiving.getInputStream();
        int b = is.read();
        while(b!=-1) {
          received(b);
          b = is.read();
        }
      } catch (final IOException e) {
        e.printStackTrace(System.err);
      }
    });
    thread.start();
    return thread;
  }

  private synchronized void received(final int b) {
    received.write(b);
  }



}
