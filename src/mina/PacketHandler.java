package mina;

import mina.message.ClientPacket;
import mina.message.PacketKind;
import mina.message.PacketManagement;
import mina.message.ServerPacket;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

public class PacketHandler extends IoHandlerAdapter {

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        super.exceptionCaught(session, cause);
    }

    /**
     * 对接受到的消息进行处理
     * 
     * @param session
     * @param object
     * @throws Exception 
     */
    @Override
    public void messageReceived(IoSession session, Object object) throws Exception {
        ClientPacket clientPacket = (ClientPacket)object;
        if (clientPacket.canPerform()) {
            clientPacket.perform();
        }
        super.messageReceived(session, clientPacket);
    }

    /**
     * 发送消息给client
     * 
     * @param session
     * @param serverMessage
     * @throws Exception 
     */
    @Override
    public void messageSent(IoSession session, Object object) throws Exception {
        ServerPacket serverPacket = (ServerPacket)object;
        if (serverPacket.canPerform()) {
            serverPacket.perform();
        }
        super.messageSent(session, serverPacket);
    }

    /**
     * 当回话断开时，调用（注意添加离线任务）
     * 
     * @param session
     * @throws Exception 
     */
    @Override
    public void sessionClosed(IoSession session) throws Exception {
        super.sessionClosed(session);
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        super.sessionCreated(session);
    }

    /**
     * 回话空闲超时调用
     * 
     * @param session
     * @param status
     * @throws Exception 
     */
    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        session.write(PacketManagement.getPacketByOpcode(PacketKind.IDEL_PACKET.getOpcode()));
        super.sessionIdle(session, status);
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        super.sessionOpened(session);
    }
}