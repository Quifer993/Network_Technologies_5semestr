package ru.nsu.fit.zolotorevskii.networksTech.lab5.proxy;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;
import ru.nsu.fit.zolotorevskii.networksTech.lab5.util.Constants;
import ru.nsu.fit.zolotorevskii.networksTech.lab5.util.TypeWork;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.Arrays;


public class ProxySocks implements Runnable {
    private static final Logger logger = LogManager.getLogger(ProxySocks.class);
    private static final String ERROR_MES_NO_AUTH = "Auth request hasn't no_auth method, only no_auth method is supported";

    private int bufferSize = 8192;
    private int port;
    private InetAddress address;


    public ProxySocks(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    @Override
    public void run() {
        try {
            Selector selector = initSelector();
            while (true) {
                selector.select();
                var iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    handleKey(key);
                }
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private Selector initSelector() throws IOException{
        Selector selector = SelectorProvider.provider().openSelector();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.socket().bind(new InetSocketAddress(address,port));
        logger.info("Server bind --- SUCCESS");
        serverChannel.register(selector,serverChannel.validOps());
        return selector;
    }

    private void handleKey(SelectionKey key) throws IOException{
        if (key.isValid()) {
            try {
                if (key.isAcceptable()) {
                    logger.info("accepting key");
                    accept(key);
                } else if (key.isConnectable()) {
                    logger.info("connecting key");
                    connect(key);
                } else if (key.isReadable()) {
                    logger.info("reading key");
                    read(key);
                } else if (key.isWritable()) {
                    logger.info("writing key");
                    write(key);
                }
            } catch (Exception e) {
                close(key);
                logger.error(e);
            }
        }
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverSocketChannel.accept();
        logger.info(channel);
        channel.configureBlocking(false);
        channel.register(key.selector(),SelectionKey.OP_READ);
    }

    private void read(SelectionKey key) throws IOException {
        var attachment = (HolderConnection)key.attachment();
        logger.info(attachment);
        if (attachment == null) {
            attachment = new HolderConnection();
            attachment.in = ByteBuffer.allocate(bufferSize);
            attachment.type = TypeWork.AUTH_READ;
            key.attach(attachment);
        }

        if (attachment.type == TypeWork.DNS_READ ) {
            var channel = (DatagramChannel) key.channel();
            if (channel.read(attachment.in) <= 0) {
                close(key);
                logger.error("Invalid DNS reply");
                throw new IOException("Invalid DNS reply");
            } else {
                var message = new Message(attachment.in.array());
                var maybeRecord = message.getSection(Section.ANSWER).stream().findAny();
                if (maybeRecord.isPresent()) {
                    var ipAddr = InetAddress.getByName(maybeRecord.get().rdataToString());
                    registerPeer(ipAddr, attachment.port, attachment.peer);
                    key.interestOps(0);
                    key.cancel();
                    key.channel().close();
                } else {
                    close(key);
                    logger.error("Host cannot be resolved");
                    throw new RuntimeException("Host cannot be resolved");
                }
            }

        } else {
            SocketChannel channel = (SocketChannel) key.channel();

            if (channel.read(attachment.in) <= 0) {
                close(key);
            } else if (attachment.type == TypeWork.AUTH_READ) {
                readAndRequestAuthMessage(key);
            } else if (attachment.peer == null) {
                read_RequestConnectionMessage(key);
            } else {
                attachment.peer.interestOps(attachment.peer.interestOps() | SelectionKey.OP_WRITE);
                key.interestOps(key.interestOps() ^ SelectionKey.OP_READ);
                attachment.in.flip();
            }
        }
    }

    private void readAndRequestAuthMessage(SelectionKey key) throws IllegalStateException{
        HolderConnection attachment = (HolderConnection)key.attachment();
        logger.info(attachment);
        byte[] buffer = attachment.in.array();

        if (buffer[0] != Constants.VERSION) {
            return;
        }

        int length = buffer[1];
        boolean isNoAuthMethodFound = false;

        for (int i = 0; i < length; i++) {
            var method = buffer[i + 2];
            if (method == Constants.NOT_AUTH) {
                isNoAuthMethodFound = true;
                break;
            }
        }

        if (!isNoAuthMethodFound) {
            throw new RuntimeException(ERROR_MES_NO_AUTH);
        }

        attachment.out = attachment.in;
        attachment.out.clear();
        attachment.out.put(Constants.NOT_AUTH_REPLY).flip();
        attachment.type = TypeWork.AUTH_WRITE;
        key.interestOps(SelectionKey.OP_WRITE);
    }

    private void read_RequestConnectionMessage(SelectionKey key) throws IOException {
        HolderConnection attachment = (HolderConnection) key.attachment();
        int len = attachment.in.position();
        if (len < 4) { return; }

        var buffer = attachment.in.array();
        if (buffer[0] != Constants.VERSION) {
            throw new RuntimeException("Connection request has invalid version, only SOCKS5 is supported");
        }

        if (buffer[1] != Constants.CONNECTION_COMMAND) {
            throw new RuntimeException("0x%02x command is not supported, only 0x01 (connect) is supported");
        }

        if (buffer[3] == Constants.ADDR_IPV4) {
            byte[] connectAddrBytes = new byte[]{buffer[4], buffer[5], buffer[6], buffer[7]};
            InetAddress connectAddr = InetAddress.getByAddress(connectAddrBytes);
            int portPos = 8;
            int connectPort = ((buffer[portPos] & 0xFF) << 8) + (buffer[portPos + 1] & 0xFF);

            registerPeer(connectAddr, connectPort, key);
            key.interestOps(0);
        } else if (buffer[3] == Constants.ADDR_HOST) {
            var hostLen = buffer[4];
            var hostStart = 5;

            var host = new String(Arrays.copyOfRange(buffer, hostStart, hostStart + hostLen));
            var portPos = hostStart + hostLen;
            var connectPort = ((buffer[portPos] & 0xFF) << 8) + (buffer[portPos + 1] & 0xFF);

            key.interestOps(0);

            requestHostResolve(host, connectPort, key);
        }
    }

    private void registerPeer(InetAddress connectAddr, int connectPort, SelectionKey backKey) throws IOException {

        var peer = SocketChannel.open();
        peer.configureBlocking(false);
        peer.connect(new InetSocketAddress(connectAddr, connectPort));
        var peerKey = peer.register(backKey.selector(), SelectionKey.OP_CONNECT);

        ((HolderConnection) backKey.attachment()).peer = peerKey;
        HolderConnection peerAttachment = new HolderConnection();
        peerAttachment.peer = backKey;
        peerKey.attach(peerAttachment);

        ((HolderConnection) backKey.attachment()).in.clear();
    }

    private void requestHostResolve(String host, int backPort, SelectionKey backKey) throws IOException {
        var peer = DatagramChannel.open();
        peer.connect(ResolverConfig.getCurrentConfig().server());
        peer.configureBlocking(false);

        var key = peer.register(backKey.selector(), SelectionKey.OP_WRITE);

        HolderConnection attachment = new HolderConnection();
        attachment.type = TypeWork.DNS_WRITE;
        attachment.port = backPort;
        attachment.peer = backKey;
        attachment.in = ByteBuffer.allocate(bufferSize);

        var message = new Message();
        var record = Record.newRecord(Name.fromString(host + '.').canonicalize(), Type.A, DClass.IN);
        message.addRecord(record, Section.QUESTION);

        var header = message.getHeader();

        header.setFlag(Flags.AD);
        header.setFlag(Flags.RD);

        attachment.in.put(message.toWire());
        attachment.in.flip();
        attachment.out = attachment.in;

        key.attach(attachment);

    }
    private void connect(SelectionKey key) throws IOException {
        SocketChannel channel =  (SocketChannel)key.channel();
        HolderConnection attachment = ((HolderConnection) key.attachment());
        channel.finishConnect();

        attachment.in = ByteBuffer.allocate(bufferSize);
        attachment.in.put(Constants.CONNECTION_SUCCESS_REPLY).flip();
        attachment.out = ((HolderConnection) attachment.peer.attachment()).in;
        ((HolderConnection) attachment.peer.attachment()).out = attachment.in;
        attachment.peer.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        key.interestOps( 0 );
    }

    private void write(SelectionKey key) throws IOException {

        HolderConnection attachment = (HolderConnection) key.attachment();

        if (attachment.type == TypeWork.DNS_WRITE) {
            DatagramChannel channel = (DatagramChannel) key.channel();
            if (channel.write(attachment.out) == -1) {
                close(key);
            } else if (attachment.out.remaining() == 0) {

                attachment.out.clear();
                attachment.type = TypeWork.DNS_READ;
                key.interestOpsOr(SelectionKey.OP_READ);
                key.interestOpsAnd(~SelectionKey.OP_WRITE);
            }
        } else {
            SocketChannel channel = (SocketChannel) key.channel();
            ByteBuffer buffer = attachment.out;
            if (channel.write(buffer) == -1) {
                close(key);
            } else if (buffer.remaining() == 0) {
                if (attachment.type == TypeWork.AUTH_WRITE) {

                    attachment.out.clear();
                    key.interestOps(SelectionKey.OP_READ);
                    attachment.type = TypeWork.READ;
                } else if (attachment.peer == null) {
                    close(key);
                } else {
                    attachment.out.clear();
                    attachment.peer.interestOps(attachment.peer.interestOps() | SelectionKey.OP_READ);
                    key.interestOps(key.interestOps() ^ SelectionKey.OP_WRITE);
                }
            }
        }
    }

    private void close(SelectionKey key) throws IOException {
        key.cancel();
        key.channel().close();
        SelectionKey peerKey = ((HolderConnection) key.attachment()).peer;
        logger.info(peerKey);
        if (peerKey != null) {
            ((HolderConnection)peerKey.attachment()).peer=null;
            if((peerKey.interestOps()&SelectionKey.OP_WRITE) == 0 ) {
                ((HolderConnection)peerKey.attachment()).out.flip();
            }
            peerKey.interestOps(SelectionKey.OP_WRITE);
        }
    }
}
