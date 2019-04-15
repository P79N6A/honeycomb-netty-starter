package org.honeycomb.tools.netty.core;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.honeycomb.tools.netty.request.NettyHttpServletRequest;

import javax.servlet.http.HttpServletResponse;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * User: luluful
 * Date: 4/8/19
 */
@ChannelHandler.Sharable
class RequestDispatcherHandler extends SimpleChannelInboundHandler<NettyHttpServletRequest> {
    private final Log logger = LogFactory.getLog(getClass());
    private final NettyContext context;

    RequestDispatcherHandler(NettyContext context) {
        this.context = checkNotNull(context);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, NettyHttpServletRequest request) throws Exception {
        HttpServletResponse servletResponse = (HttpServletResponse) request.getServletResponse();
        try {
            NettyRequestDispatcher dispatcher = (NettyRequestDispatcher) context.getRequestDispatcher(request.getRequestURI());
            if (dispatcher == null) {
                servletResponse.sendError(404);
                return;
            }
            dispatcher.dispatch(request, servletResponse);
        } finally {
            if (!request.isAsyncStarted()) {
                servletResponse.getOutputStream().close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Unexpected exception caught during request", cause);
        ctx.close();
    }
}
