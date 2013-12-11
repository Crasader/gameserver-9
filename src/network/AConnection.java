package network;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import network.options.Assertion;

/**
 * Class that represent Connection with server socket. Connection is created by
 * <code>ConnectionFactory</code> and attached to
 * <code>SelectionKey</code> key. Selection key is registered to one of
 * Dispatchers
 * <code>Selector</code> to handle io read and write.
 *
 * @author -Nemesiss-
 */
public abstract class AConnection {

    /**
     * SocketChannel representing this connection
     */
    private boolean isNeedProcessTencentHead = false;
    private final SocketChannel socketChannel;
    /**
     * Dispatcher [AcceptReadWriteDispatcherImpl] to witch this connection
     * SelectionKey is registered.
     */
    private final Dispatcher dispatcher;
    /**
     * SelectionKey representing this connection.
     */
    private SelectionKey key;
    /**
     * True if this connection should be closed after sending last server
     * packet.
     */
    protected boolean pendingClose;
    /**
     * True if OnDisconnect() method should be called immediately after this
     * connection was closed.
     */
    protected boolean isForcedClosing;
    /**
     * True if this connection is already closed.
     */
    protected boolean closed;
    /**
     * Object on witch some methods are synchronized
     */
    protected final Object guard = new Object();
    /**
     * ByteBuffer for io write.
     */
    public final ByteBuffer writeBuffer;
    /**
     * ByteBuffer for io read.
     */
    public final ByteBuffer readBuffer;
    /**
     * Caching ip address to make sure that {@link #getIP()} method works even
     * after disconnection
     */
    private final String ip;
    /**
     * Used only for PacketProcessor synchronization purpose
     */
    private boolean locked = false;

    public boolean getIsNeedProcessTencentHead() {
        return isNeedProcessTencentHead && CommonConfig.isTencentPlatform;
    }

    protected void setIsNeedProcessTencentHead(boolean isNeedProcessTencentHead) {
        this.isNeedProcessTencentHead = isNeedProcessTencentHead;
    }

    /**
     * Constructor
     *
     * @param sc
     * @param d
     * @throws IOException
     */
    public AConnection(SocketChannel sc, Dispatcher d) throws IOException {
        socketChannel = sc;
        dispatcher = d;
        writeBuffer = ByteBuffer.allocate(8192 * 2);
        writeBuffer.flip();
        writeBuffer.order(ByteOrder.LITTLE_ENDIAN);
        readBuffer = ByteBuffer.allocate(8192 * 2);
        readBuffer.order(ByteOrder.LITTLE_ENDIAN);

        dispatcher.register(socketChannel, SelectionKey.OP_READ, this);

        InetAddress address = socketChannel.socket().getInetAddress();
        //腾讯平台上有大量的短连接，在代码运行到这里之前连接已经断开，导致这里得到的address为空
        //最后抛出NullPointerException
        this.ip = null != address ? address.getHostAddress() : null;

    }

    /**
     * Set selection key - result of registration this AConnection socketChannel
     * to one of dispatchers.
     *
     * @param key
     */
    final void setKey(SelectionKey key) {
        this.key = key;
    }

    /**
     * Notify Dispatcher Selector that we want write some data here.
     */
    protected final void enableWriteInterest() {
        if (key.isValid()) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            key.selector().wakeup();
        }
    }

    /**
     * @return Dispatcher to witch this connection is registered.
     */
    final Dispatcher getDispatcher() {
        return dispatcher;
    }

    /**
     * @return SocketChannel representing this connection.
     */
    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    /**
     * Connection will be closed at some time [by Dispatcher Thread], after that
     * onDisconnect() method will be called to clear all other things.
     *
     * @param forced is just hint that getDisconnectionDelay() should return 0
     * so OnDisconnect() method will be called without any delay.
     */
    public final void close(boolean forced) {
        synchronized (guard) {
            if (isWriteDisabled()) {
                return;
            }

            isForcedClosing = forced;
            getDispatcher().closeConnection(this);
        }
    }

    /**
     * This will only close the connection without taking care of the rest. May
     * be called only by Dispatcher Thread. Returns true if connection was not
     * closed before.
     *
     * @return true if connection was not closed before.
     */
    final boolean onlyClose() {
        /**
         * Test if this build should use assertion. If NetworkAssertion == false
         * javac will remove this code block
         */
        if (Assertion.NetworkAssertion) {
            assert Thread.currentThread() == dispatcher;
        }

        synchronized (guard) {
            if (closed) {
                return false;
            }
            try {
                if (socketChannel.isOpen()) {
                    socketChannel.close();
                    key.attach(null);
                    key.cancel();
                }
                closed = true;
            } catch (IOException ignored) {
            }
        }
        return true;
    }

    /**
     * @return True if this connection is pendingClose and not closed yet.
     */
    final boolean isPendingClose() {
        return pendingClose && !closed;
    }

    /**
     * @return True if write to this connection is possible.
     */
    protected final boolean isWriteDisabled() {
        return pendingClose || closed;
    }

    /**
     * @return IP address of this Connection.
     */
    public final String getIP() {
        return ip;
    }

    /**
     * Used only for PacketProcessor synchronization purpose. Return true if
     * locked successful - if wasn't locked before.
     *
     * @return locked
     */
    boolean tryLockConnection() {
        if (locked) {
            return false;
        }
        return locked = true;
    }

    /**
     * Used only for PacketProcessor synchronization purpose. Unlock this
     * connection.
     */
    void unlockConnection() {
        locked = false;
    }

    /**
     * @param data
     * @return True if data was processed correctly, False if some error
     * occurred and connection should be closed NOW.
     */
    abstract protected boolean processData(ByteBuffer data);

    /**
     * This method will be called by Dispatcher, and will be repeated till
     * return false.
     *
     * @param data
     * @return True if data was written to buffer, False indicating that there
     * are not any more data to write.
     */
    abstract protected boolean writeData(ByteBuffer data);

    /**
     * This method is called by Dispatcher when connection is ready to be
     * closed.
     *
     * @return time in ms after witch onDisconnect() method will be called.
     */
    abstract protected long getDisconnectionDelay();

    /**
     * This method is called by Dispatcher to inform that this connection was
     * closed and should be cleared. This method is called only once.
     */
    abstract protected void onDisconnect();

    /**
     * This method is called by NioServer to inform that NioServer is shouting
     * down. This method is called only once.
     */
    abstract protected void onServerClose();
}
