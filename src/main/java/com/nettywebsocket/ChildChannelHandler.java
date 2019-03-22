package com.nettywebsocket;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
public class ChildChannelHandler extends ChannelInitializer<SocketChannel>{

    //当新连接accept的时候，这个方法会调用
    @Override
    protected void initChannel(SocketChannel e) throws Exception {
// 设置30秒没有读到数据，则触发一个READER_IDLE事件。
// pipeline.addLast(new IdleStateHandler(30, 0, 0));
// HttpServerCodec：将请求和应答消息解码为HTTP消息  这里使用Netty自带的Http编解码组件HttpServerCodec对通信数据进行编解码
        e.pipeline().addLast("http-codec",new HttpServerCodec());
// HttpObjectAggregator：将HTTP消息的多个部分合成一条完整的HTTP消息
        //从上可以看出，当我们用POST方式请求服务器的时候，对应的参数信息是保存在message body中的,如果只是单纯的用HttpServerCodec是无法完全的解析Http POST请求的，因为HttpServerCodec只能获取uri中参数，所以需要加上HttpObjectAggregator.
        e.pipeline().addLast("aggregator",new HttpObjectAggregator(65536));
// ChunkedWriteHandler：向客户端发送HTML5文件
        //为了解决大文件传输过程中的内存溢出,Netty提供了ChunkedWriteHandler来解决大文件或者码流传输过程中可能发生的内存溢出问题。
        e.pipeline().addLast("http-chunked",new ChunkedWriteHandler());
// 在管道中添加我们自己的接收数据实现方法 //为当前的channel的pipeline添加自定义的处理函数
        e.pipeline().addLast("handler",new MyWebSocketServerHandler());
    }
}

