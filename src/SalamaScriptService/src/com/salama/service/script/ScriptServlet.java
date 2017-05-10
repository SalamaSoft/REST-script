package com.salama.service.script;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicLong;

import javax.script.CompiledScript;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.log4j.Logger;

import com.salama.server.servlet.HttpServlet;
import com.salama.service.core.context.ServiceContext;
import com.salama.service.core.net.RequestWrapper;
import com.salama.service.core.net.ResponseWrapper;
import com.salama.service.core.net.http.HttpRequestWrapper;
import com.salama.service.core.net.http.HttpResponseWrapper;
import com.salama.service.core.net.http.HttpSessionWrapper;
import com.salama.service.script.config.ScriptServletConfig;
import com.salama.util.http.upload.FileUploadSupport;

public class ScriptServlet extends javax.servlet.http.HttpServlet {

    private static final long serialVersionUID = -1759081420866762147L;

    private final static Logger logger = Logger.getLogger(ScriptServlet.class);
    
    private final static AtomicLong _servletCounter = new AtomicLong();
    
    private com.salama.server.servlet.ServiceContext _servletServiceContext = null;
    private ScriptServiceContext _scriptServletContext = null;
    private ScriptServiceDispatcher _serviceDispatcher = null;
    
    private long _servletNum;
    
    @Override
    public void init() throws ServletException {
        super.init();
        
        //Init Servlet ServiceContext
        _servletServiceContext = (com.salama.server.servlet.ServiceContext) ServiceContext.getContext(
                getServletContext()).getContext(com.salama.server.servlet.ServiceContext.class);
        
        _scriptServletContext = (ScriptServiceContext) ServiceContext.getContext(
                getServletContext()).getContext(ScriptServiceContext.class
                        );
        _serviceDispatcher = _scriptServletContext.getScriptServiceDispatcher();
        
        _servletNum = _servletCounter.getAndIncrement();
        logger.info("ScriptServlet[" + _servletNum + "] inited.");
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            if(!processService(req, resp)) {
                super.doGet(req, resp);
            }
        } catch(ServletException e) {
            logger.error("doGet()", e);
            throw e;
        } catch (IOException e) {
            logger.error("doGet()", e);
            throw e;
        } catch(FileUploadException e) {
            logger.error("doGet()", e);
            throw new ServletException(e);
        } catch(RuntimeException e) {
            logger.error("doGet()", e);
            throw e;
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            if(!processService(req, resp)) {
                super.doPost(req, resp);
            }
        } catch(ServletException e) {
            logger.error("doPost()", e);
            throw e;
        } catch (IOException e) {
            logger.error("doPost()", e);
            throw e; 
        } catch(FileUploadException e) {
            logger.error("doPost()", e);
            throw new ServletException(e);
        } catch(RuntimeException e) {
            logger.error("doPost()", e);
            throw e;
        }
    }
    
    protected boolean processService(HttpServletRequest req, HttpServletResponse resp) throws FileUploadException {
        boolean isMultiPartRequest = FileUploadSupport.isMultipartContent(req);
        HttpResponseWrapper responseWrapper = new HttpResponseWrapper(resp);
        RequestWrapper requestWrapper = null;
        if(isMultiPartRequest) {
            requestWrapper = _servletServiceContext.getFileUploadSupport().parseMultipartRequest(req);
        } else {
            requestWrapper = new HttpRequestWrapper(req, new HttpSessionWrapper(req.getSession()));
        }
        
        /*
        if(logger.isDebugEnabled()) {
            Enumeration<String> enumNames = requestWrapper.getParameterNames();
            String name, value;
            while(enumNames.hasMoreElements()) {
                name = enumNames.nextElement();
                value = requestWrapper.getParameter(name);
                
                if(logger.isDebugEnabled()) {
                    logger.debug(name + ":" + value);
                }
            }
        }
        */

        //default content type
        responseWrapper.setContentType("text/plain");
        
        String result = doService(requestWrapper, responseWrapper);
        if(result != null) {
            httpOutput(resp, result);
        }
        
        return true;
    }
    
    protected String doService(RequestWrapper request, ResponseWrapper response) {
        String uri = ((HttpServletRequest) request.getRequest()).getRequestURI();
        
        //parse URI
        String app = 
        {
            
        }
        
        _scriptServletContext.getScriptServiceDispatcher().getCompiledScript(app, serviceName)
    }

    private static void httpOutput(HttpServletResponse response, String content) {
        if(content == null) {
            return;
        }
        
        try {
            PrintWriter writer = response.getWriter();
            try {
                writer.print(content);
                writer.flush();
            } finally {
                writer.close();
            }
        } catch(IOException e) {
            logger.error("httpOutput()", e);
        }
    }
}
