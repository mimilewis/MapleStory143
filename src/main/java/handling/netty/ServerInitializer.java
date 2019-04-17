package handling.netty;

import handling.MapleServerHandler;
import handling.ServerType;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

public class ServerInitializer extends ChannelInitializer<SocketChannel> {

    private final int world;
    private final int channels;
    private final ServerType type;

    public ServerInitializer(int world, int channels, ServerType type) {
        this.world = world;
        this.channels = channels;
        this.type = type;
    }

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        ChannelPipeline pipe = channel.pipeline();
        pipe.addLast("idleStateHandler", new IdleStateHandler(25, 25, 0));
        pipe.addLast("decoder", new MaplePacketDecoder()); // decodes the packet
        pipe.addLast("encoder", new MaplePacketEncoder()); //encodes the packet
        pipe.addLast("handler", new MapleServerHandler(world, channels, type));
    }

}
