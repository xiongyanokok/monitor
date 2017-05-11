package com.xy.monitor.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class MonitorFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		
	}

	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
		final HttpServletResponse httpResponse = (HttpServletResponse) response;
		try {
			Future<HttpServletResponse> future = new FutureTask<HttpServletResponse>(new Callable<HttpServletResponse>() {
				
				@Override
				public HttpServletResponse call() throws Exception {
					chain.doFilter(request, httpResponse);
					return httpResponse;
				}
			});
			
			future.get(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			returnJson(httpResponse, "请求超时");
		}
		
		/*chain.doFilter(request, httpResponse);
		ServletOutputStream s = httpResponse.getOutputStream();
		System.out.println(s.toString());*/
	}

	private static void returnJson(HttpServletResponse response, String json) {
        PrintWriter writer = null;
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=utf-8");
        try {
            writer = response.getWriter();
            writer.print(json);
        } catch (Exception e) {
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
	
	@Override
	public void destroy() {
		
	}

}
