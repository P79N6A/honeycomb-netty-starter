package org.honeycomb.tools.netty.bootstrap;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.unix.DomainSocketAddress;
import org.honeycomb.tools.netty.core.NettyContainer;
import org.honeycomb.tools.netty.core.NettyContext;
import org.honeycomb.tools.netty.utils.GsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import javax.servlet.ServletException;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;


/**
 * User: luluful
 * Date: 4/8/19
 */
public class NettyFactory extends AbstractServletWebServerFactory implements ResourceLoaderAware {

    @Value("${server.port.enable}")
    private String portEnable;

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String SERVER_INFO = "Netty@SpringBoot";
    private ResourceLoader resourceLoader;

    @Override
    public WebServer getWebServer(ServletContextInitializer... initializers) {
        ClassLoader parentClassLoader = resourceLoader != null ? resourceLoader.getClassLoader() : ClassUtils.getDefaultClassLoader();
        //Netty启动环境相关信息
        Package nettyPackage = Bootstrap.class.getPackage();
        String title = nettyPackage.getImplementationTitle();
        String version = nettyPackage.getImplementationVersion();
        log.info("Running with " + title + " " + version);
        //是否支持默认Servlet
        if (isRegisterDefaultServlet()) {
            log.warn("This container does not support a default servlet");
        }
        //上下文
        NettyContext context = new NettyContext(getContextPath(), new URLClassLoader(new URL[]{}, parentClassLoader), SERVER_INFO);
        for (ServletContextInitializer initializer : initializers) {
            try {
                initializer.onStartup(context);
            } catch (ServletException e) {
                throw new RuntimeException(e);
            }
        }
        String address = getSockAddress();
        int port = getPort();
        InetSocketAddress ipAddress = null;
        if (port > 0 && !StringUtils.isEmpty(portEnable) && Boolean.valueOf(portEnable)) {
            ipAddress = new InetSocketAddress(port);
            log.info("Server initialized with portEnable:{} port:{} ",portEnable,port);
        }else{
            log.info("Server initialized with address: " + address);
        }
        return new NettyContainer(new DomainSocketAddress(address), ipAddress, context);
    }


    private String getSockAddress() {
        Map<String, String> env = System.getenv();
        String envHcConfig = env.get("HC_APP_CONFIG");
        log.info("Netty Http Server HC_APP_CONFIG:" + envHcConfig);

        if (envHcConfig == null) {
            String cwd = new File("").getAbsoluteFile().toString();
            envHcConfig = "{\"targetSock\": \"/tmp/sockets.sock\"}";
        }
        Map<String, Object> jsonObject = GsonUtil.fromJson(envHcConfig, Map.class);
        return jsonObject.get("targetSock").toString();
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }


}
