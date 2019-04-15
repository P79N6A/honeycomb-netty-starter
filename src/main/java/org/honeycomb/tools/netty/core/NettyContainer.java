package org.honeycomb.tools.netty.core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerDomainSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.DomainSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;

import java.net.InetSocketAddress;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * User: luluful
 * Date: 4/8/19
 */
public class NettyContainer implements WebServer {
    private final Log log = LogFactory.getLog(getClass());

    private final DomainSocketAddress address; //监听socket地址
    private final NettyContext servletContext; //Context
    private final InetSocketAddress ipAddress; //监听端口地址

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private DefaultEventExecutorGroup servletExecutor;

    public NettyContainer(DomainSocketAddress address, InetSocketAddress ipAddress, NettyContext servletContext) {
        this.address = address;
        this.servletContext = servletContext;
        this.ipAddress = ipAddress;
    }


    @Override
    public void start() throws WebServerException {
        servletContext.setInitialised(false);

        ServerBootstrap bootstrap = new ServerBootstrap();
        EventLoopGroup bossGroup;
        EventLoopGroup workerGroup;
        Class channel;
        String os = System.getProperty("os.name").toLowerCase();

        Boolean isPort = ipAddress != null ? true : false;

        if (isPort) {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            channel = NioServerSocketChannel.class;
        } else {
            if (os.indexOf("mac") >= 0) {
                bossGroup = new KQueueEventLoopGroup();
                workerGroup = new KQueueEventLoopGroup();
                channel = KQueueServerDomainSocketChannel.class;
            } else {
                bossGroup = new EpollEventLoopGroup();
                workerGroup = new EpollEventLoopGroup();
                channel = EpollServerDomainSocketChannel.class;
            }
        }

        bootstrap.option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_BACKLOG, 100);

        bootstrap.channel(channel).group(bossGroup, workerGroup);
        log.info("Bootstrap configuration: " + bootstrap.toString());

        servletExecutor = new DefaultEventExecutorGroup(50);
        ChannelFuture future = null;
        if (isPort) {
            addChildHandlerForPort(bootstrap);
            servletContext.setInitialised(true);
            future = bootstrap.bind(ipAddress).awaitUninterruptibly();
        } else {
            addChildHandlerForDomainSocket(bootstrap);
            servletContext.setInitialised(true);
            future = bootstrap.bind(address).awaitUninterruptibly();
        }

        Throwable cause = future.cause();
        if (null != cause) {
            throw new WebServerException("Could not start Netty server", cause);
        }
        log.info(servletContext.getServerInfo() + " started ");
    }

    private void addChildHandlerForDomainSocket(ServerBootstrap bootstrap) {

        bootstrap.childHandler(new ChannelInitializer<DomainSocketChannel>() {
            @Override
            protected void initChannel(DomainSocketChannel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();
                p.addLast("codec", new HttpServerCodec(4096, 8192, 8192, false)); //HTTP编码解码Handler
                p.addLast("servletInput", new ServletContentHandler(servletContext)); //处理请求，读入数据，生成Request和Response对象
                p.addLast(checkNotNull(servletExecutor), "filterChain", new RequestDispatcherHandler(servletContext)); //获取请求分发器，让对应的Servlet处理请求，同时处理404情况
            }
        });
    }

    private void addChildHandlerForPort(ServerBootstrap bootstrap) {

        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();
                p.addLast("codec", new HttpServerCodec(4096, 8192, 8192, false)); //HTTP编码解码Handler
                p.addLast("servletInput", new ServletContentHandler(servletContext)); //处理请求，读入数据，生成Request和Response对象
                p.addLast(checkNotNull(servletExecutor), "filterChain", new RequestDispatcherHandler(servletContext)); //获取请求分发器，让对应的Servlet处理请求，同时处理404情况
            }
        });
    }

    /**
     * 优雅地关闭各种资源
     *
     * @throws WebServerException
     */
    @Override
    public void stop() throws WebServerException {
        log.info("Embedded Netty Servlet Container is now shuting down.");
        try {
            if (null != bossGroup) {
                bossGroup.shutdownGracefully().await();
            }
            if (null != workerGroup) {
                workerGroup.shutdownGracefully().await();
            }
            if (null != servletExecutor) {
                servletExecutor.shutdownGracefully().await();
            }
        } catch (InterruptedException e) {
            throw new WebServerException("Container stop interrupted", e);
        }
    }

    @Override
    public int getPort() {
        return 0;
    }
}
