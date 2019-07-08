package tlschannel.example;

import tlschannel.ClientTlsChannel;
import tlschannel.TlsChannel;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

/**
 * Client example. Connects to a public TLS reporting service.
 */
public class TestClient {

    private static final Charset utf8 = StandardCharsets.UTF_8;

    public static final String domain = "localhost";
    public static final String httpLine =
            "GET https://www.howsmyssl.com/a/check HTTP/1.0\nHost: www.howsmyssl.com\n\n";

    public static void main(final String[] args) throws IOException, GeneralSecurityException {

        // initialize the SSLContext, a configuration holder, reusable object
        final SSLContext sslContext = SimpleBlockingServer.authenticatedContext("TLSv1.2");

        // connect raw socket channel normally
        try (SocketChannel rawChannel = SocketChannel.open()) {
            rawChannel.connect(new InetSocketAddress(domain, 10000));

            // create TlsChannel builder, combining the raw channel and the SSLEngine, using minimal options
            final ClientTlsChannel.Builder builder = ClientTlsChannel.newBuilder(rawChannel, sslContext);

            // instantiate TlsChannel
            try (TlsChannel tlsChannel = builder.build()) {

                // do HTTP interaction and print result
                tlsChannel.write(ByteBuffer.wrap(httpLine.getBytes()));
                final ByteBuffer res = ByteBuffer.allocate(10000);
                // being HTTP 1.0, the server will just close the connection at the end
                while (tlsChannel.read(res) != -1)
                    ;
                res.flip();
                System.out.println(utf8.decode(res).toString());

            }
        }
    }

}
