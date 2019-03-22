package com.nettywebsocket;
import javax.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;


@Service
public class NettyServer {
    public static void main(String[] args) {
        new NettyServer().run();
    }

    //被@PostConstruct修饰的方法会在服务器加载Servlet的时候运行，并且只会被服务器调用一次，类似于Serclet的inti()方法。被@PostConstruct修饰的方法会在构造函数之后，init()方法之前运行。
    @PostConstruct
    public void initNetty(){
        new Thread(){
            @Override
            public void run() {
                new NettyServer().run();
            }
        }.start();
    }
    public void run(){
        System.out.println("===========================Netty端口启动========");
// Boss线程：由这个线程池提供的线程是boss种类的，用于创建、连接、绑定socket， （有点像门卫）然后把这些socket传给worker线程池。
// 在服务器端每个监听的socket都有一个boss线程来处理。在客户端，只有一个boss线程来处理所有的socket。
        //NioEventLoopGroup可以理解为一个线程池，内部维护了一组线程，每个线程负责处理多个Channel上的事件，而一个Channel只对应于一个线程，这样可以回避多线程下的数据同步问题。
        EventLoopGroup bossGroup = new NioEventLoopGroup();
// Worker线程：Worker线程执行所有的异步I/O，即处理操作
        EventLoopGroup workGroup = new NioEventLoopGroup();
        try {
// ServerBootstrap 启动NIO服务的辅助启动类,负责初始话netty服务器，并且开始监听端口的socket请求
            ServerBootstrap b = new ServerBootstrap();
            //父 子
            b.group(bossGroup, workGroup);
// 设置非阻塞,用它来建立新accept的连接,用于构造serversocketchannel的工厂类
            //(4)、 指定通道channel的类型，由于是服务端，故而是NioServerSocketChannel；
            b.channel(NioServerSocketChannel.class);
// ChildChannelHandler 对出入的数据进行的业务操作,其继承ChannelInitializer
            b.childHandler(new ChildChannelHandler());

            System.out.println("服务端开启等待客户端连接 ... ...");
            Channel ch = b.bind(7397).sync().channel();

            //但是我看这行代码一直没有执行。请问这是怎么回事呢？
            //不是没执行，是主线程到这里就 wait 子线程退出了，子线程才是真正监听和接受请求的。
            //绑定端口后,后面future.channel().closeFuture().sync();其实是有执行的,不过线程变为wait状态了
            ch.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }
}