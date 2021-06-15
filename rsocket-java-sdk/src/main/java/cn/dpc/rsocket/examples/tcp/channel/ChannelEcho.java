package cn.dpc.rsocket.examples.tcp.channel;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.RSocketServer;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Duration;

public class ChannelEcho {
    static final Logger logger = LoggerFactory.getLogger(ChannelEcho.class);

    public static void main(String[] args) {
        SocketAcceptor acceptor = SocketAcceptor
                .forRequestChannel(payloads ->
                        Flux.from(payloads)
                                .map(Payload::getDataUtf8)
                                .map(s -> "echo " + s)
                                .map(DefaultPayload::create)
                );

        RSocketServer.create()
                .acceptor(acceptor)
                .bindNow(TcpServerTransport.create(7000));

        RSocket socket = RSocketConnector.create()
                .connect(TcpClientTransport.create(7000)).block();

        socket
                .requestChannel(
                        Flux.interval(Duration.ofMillis(1000)).map(i -> DefaultPayload.create("Hello")))
                .map(Payload::getDataUtf8)
                .take(10)
                .doOnNext(logger::debug)
                .doFinally(signalType -> socket.dispose())
                .then()
                .block();
    }
}
