package org.honeycomb.tools.netty.utils;

import javax.servlet.Servlet;

/**
 * User: luluful
 * Date: 4/8/19
 */
public class MappingData {

    Servlet servlet = null;
    String servletName;
    String redirectPath ;

    public void recycle() {
        servlet = null;
        servletName = null;
        redirectPath = null;
    }

}
