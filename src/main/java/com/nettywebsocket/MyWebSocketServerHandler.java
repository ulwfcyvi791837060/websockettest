package com.nettywebsocket;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;

//在客户端，当 channelRead0() 方法完成时，你已经有了传入消息，并且已经处理完它了。当该方法返回时，SimpleChannelInboundHandler负责释放指向保存该消息的ByteBuf的内存引用。
public class MyWebSocketServerHandler extends SimpleChannelInboundHandler<Object> {
    private static final Logger logger = Logger.getLogger(WebSocketServerHandshaker.class.getName());
    private WebSocketServerHandshaker handshaker;
    /**
     * channel 通道 action 活跃的 当客户端主动链接服务端的链接后，这个通道就是活跃的了。也就是客户端与服务端建立了通信通道并且可以传输数据
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
// 添加
        Global.group.add(ctx.channel());
        System.out.println("客户端与服务端连接开启：" + ctx.channel().remoteAddress().toString());
    }
    /**
     * channel 通道 Inactive 不活跃的 当客户端主动断开服务端的链接后，这个通道就是不活跃的。也就是说客户端与服务端关闭了通信通道并且不可以传输数据
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
// 移除
        Global.group.remove(ctx.channel());
        System.out.println("客户端与服务端连接关闭：" + ctx.channel().remoteAddress().toString());
    }


    /**
     * 接收客户端发送的消息 channel 通道 Read 读 简而言之就是从通道中读取数据，也就是服务端接收客户端发来的数据。但是这个数据在不进行解码时它是ByteBuf类型的
     */
    @Override
    protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
// 传统的HTTP接入
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, ((FullHttpRequest) msg));
// WebSocket接入
        } else if (msg instanceof WebSocketFrame) {
            System.out.println(handshaker.uri());
            //安卓
            if("anzhuo".equals(ctx.attr(AttributeKey.valueOf("type")).get())){
                handlerWebSocketFrame(ctx, (WebSocketFrame) msg);
            }else{
                handlerWebSocketFrame2(ctx, (WebSocketFrame) msg);
            }
        }
    }


    /**
     * channel 通道 Read 读取 Complete 完成 在通道读取完成后会在这个方法里通知，对应可以做刷新操作 ctx.flush()
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    //安卓
    private void handlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
// 判断是否关闭链路的指令
        if (frame instanceof CloseWebSocketFrame) {
            System.out.println(1);
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
// 判断是否ping消息
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
// 本例程仅支持文本消息，不支持二进制消息
        if (!(frame instanceof TextWebSocketFrame)) {
            System.out.println("本例程仅支持文本消息，不支持二进制消息");
            throw new UnsupportedOperationException(
                    String.format("%s frame types not supported", frame.getClass().getName()));
        }

// 返回应答消息
        String request = ((TextWebSocketFrame) frame).text();
        System.out.println("服务端收到：" + request);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(String.format("%s received %s", ctx.channel(), request));
        }
        TextWebSocketFrame tws = new TextWebSocketFrame(new Date().toString() + ctx.channel().id() + "：" + request);
// 群发
        Global.group.writeAndFlush(tws);
// 返回【谁发的发给谁】
// ctx.channel().writeAndFlush(tws);
    }

    //非安卓
    private void handlerWebSocketFrame2(ChannelHandlerContext ctx, WebSocketFrame frame) {
// 判断是否关闭链路的指令
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
// 判断是否ping消息
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
// 本例程仅支持文本消息，不支持二进制消息
        if (!(frame instanceof TextWebSocketFrame)) {
            System.out.println("本例程仅支持文本消息，不支持二进制消息");
            throw new UnsupportedOperationException(
                    String.format("%s frame types not supported", frame.getClass().getName()));
        }
// 返回应答消息
        String request = ((TextWebSocketFrame) frame).text();
        System.out.println("服务端2收到：" + request);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(String.format("%s received %s", ctx.channel(), request));
        }
        TextWebSocketFrame tws = new TextWebSocketFrame(new Date().toString() + ctx.channel().id() + "：" + request);
// 群发
        Global.group.writeAndFlush(tws);
// 返回【谁发的发给谁】
// ctx.channel().writeAndFlush(tws);
    }

    // 传统的HTTP接入
    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
// 如果HTTP解码失败，返回HHTP异常
        if (!req.getDecoderResult().isSuccess() || (!"websocket".equals(req.headers().get("Upgrade")))) {
            sendHttpResponse(ctx, req,
                    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
//获取url后置参数
        HttpMethod method=req.getMethod();
        String uri=req.getUri();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
        Map<String, List<String>> parameters = queryStringDecoder.parameters();
        System.out.println(parameters.get("request").get(0));
        if(method==HttpMethod.GET&&"/webssss".equals(uri)){
//....处理
            ctx.attr(AttributeKey.valueOf("type")).set("anzhuo");
        }else if(method==HttpMethod.GET&&"/websocket".equals(uri)){
//...处理
            ctx.attr(AttributeKey.valueOf("type")).set("live");
        }
// 构造握手响应返回，本机测试
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                "ws://"+req.headers().get(HttpHeaders.Names.HOST)+uri, null, false);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
        }
    }


    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse res) {
// 返回应答给客户端
        if (res.getStatus().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }
// 如果是非Keep-Alive，关闭连接
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpHeaders.isKeepAlive(req) || res.getStatus().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
    /**
     * exception 异常 Caught 抓住 抓住异常，当发生异常的时候，可以做一些相应的处理，比如打印日志、关闭链接
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}


   /* netty4 连通步骤
转载：http://xw-z1985.iteye.com/blog/1973205



        服务端依次发生的步骤

        建立服务端监听套接字ServerSocketChannel，以及对应的管道pipeline；
        启动boss线程，将ServerSocketChannel注册到boss线程持有的selector中，并将注册返回的selectionKey赋值给ServerSocketChannel关联的selectionKey变量；
        在ServerSocketChannel对应的管道中触发channelRegistered事件；
        绑定IP和端口
        触发 channelActive 事件，并将ServerSocketChannel关联的selectionKey的OP_ACCEPT位置为1。
        客户端发起connect请求后，boss线程正在运行的select循环检测到了该ServerSocketChannel的ACCEPT事件就绪，则通过accept系统调用建立一个已连接套接字SocketChannel，并为其创建对应的管道；
        在服务端监听套接字对应的管道中触发channelRead事件；
        channelRead事件由ServerBootstrapAcceptor的channelRead方法响应：为已连接套接字对应的管道加入ChannelInitializer处理器；启动一个worker线程，并将已连接套接字的注册任务加入到worker线程的任务队列中；
        worker线程执行已连接套接字的注册任务：将已连接套接字注册到worker线程持有的selector中，并将注册返回的selectionKey赋值给已连接套接字关联的selectionKey变量；在已连接套接字对应的管道中触发channelRegistered事件；channelRegistered事件由ChannelInitializer的channelRegistered方法响应：将自定义的处理器（譬如EchoServerHandler）加入到已连接套接字对应的管道中；在已连接套接字对应的管道中触发channelActive事件；channelActive事件由已连接套接字对应的管道中的inbound处理器的channelActive方法响应；将已连接套接字关联的selectionKey的OP_READ位置为1；至此，worker线程关联的selector就开始监听已连接套接字的READ事件了。
        在worker线程运行的同时，Boss线程接着在服务端监听套接字对应的管道中触发channelReadComplete事件。
        客户端向服务端发送消息后，worker线程正在运行的selector循环会检测到已连接套接字的READ事件就绪。则通过read系统调用将消息从套接字的接受缓冲区中读到AdaptiveRecvByteBufAllocator（可以自适应调整分配的缓存的大小）分配的缓存中；
        在已连接套接字对应的管道中触发channelRead事件；
        channelRead事件由EchoServerHandler处理器的channelRead方法响应：执行write操作将消息存储到ChannelOutboundBuffer中；
        在已连接套接字对应的管道中触发ChannelReadComplete事件；
        ChannelReadComplete事件由EchoServerHandler处理器的channelReadComplete方法响应：执行flush操作将消息从ChannelOutboundBuffer中flush到套接字的发送缓冲区中；


        客户端依次发生的步骤

        建立套接字SocketChannel，以及对应的管道pipeline；
        启动客户端线程，将SocketChannel注册到客户端线程持有的selector中，并将注册返回的selectionKey赋值给SocketChannel关联的selectionKey变量；
        触发channelRegistered事件；
        channelRegistered事件由ChannelInitializer的channelRegistered方法响应：将客户端自定义的处理器（譬如EchoClientHandler）按顺序加入到管道中；
        向服务端发起connect请求，并将SocketChannel关联的selectionKey的OP_CONNECT位置为1；
        开始三次握手，客户端线程正在运行的select循环检测到了该SocketChannel的CONNECT事件就绪，则将关联的selectionKey的OP_CONNECT位置为0，再通过调用finishConnect完成连接的建立；
        触发channelActive事件；
        channelActive事件由EchoClientHandler的channelActive方法响应，通过调用ctx.writeAndFlush方法将消息发往服务端；
        首先将消息存储到ChannelOutboundBuffer中；（如果ChannelOutboundBuffer存储的所有未flush的消息的大小超过高水位线writeBufferHighWaterMark（默认值为64 * 1024），则会触发ChannelWritabilityChanged事件）
        然后将消息从ChannelOutboundBuffer中flush到套接字的发送缓冲区中；（如果ChannelOutboundBuffer存储的所有未flush的消息的大小小于低水位线，则会触发ChannelWritabilityChanged事件）*/