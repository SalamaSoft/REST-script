package com.salama.service.script;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSON;
import com.salama.service.core.auth.MethodAccessNoAuthorityException;
import com.salama.service.core.context.ServiceContext;
import com.salama.service.core.net.RequestWrapper;
import com.salama.service.core.net.ResponseWrapper;
import com.salama.service.core.net.http.HttpRequestWrapper;
import com.salama.service.core.net.http.HttpResponseWrapper;
import com.salama.service.core.net.http.HttpSessionWrapper;
import com.salama.service.script.core.IScriptServiceDispatcher;
import com.salama.service.script.util.ResponseConverter;
import com.salama.util.http.upload.FileUploadSupport;

public class ScriptServiceServlet extends javax.servlet.http.HttpServlet {
    private static final long serialVersionUID = -1759081420866762147L;
    
    private final static Log logger = LogFactory.getLog(ScriptServiceServlet.class);
    
    
//    private static final String ReturnValue_Xml_MethodAccessNoAuthority = 
//            "<Error><type>MethodAccessNoAuthorityException</type></Error>";
    public static class Error {
        private String type;
        
        public Error() {
        }
        
        public Error(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
        
    }
    
    /**
     * "xml" | "json" | "xml.jsonp=$varName" | "json.jsonp=$varName"
     * Default "xml" when empty 
     */
    public final static String HTTP_HEADER_RESPONSE_TYPE = "Response-Type";
    public final static String HTTP_HEADER_RESPONSE_PRETTIFY = "Response-Prettify";
    
    private final static AtomicLong _servletCounter = new AtomicLong();
    
    private ScriptServiceServletContext _scriptSericeContext = null;
    private FileUploadSupport _fileUploadSupport = null;
    private IScriptServiceDispatcher _serviceDispatcher = null;
    
    private long _servletNum;
    
    @Override
    public void init() throws ServletException {
        super.init();
        
        _scriptSericeContext = (ScriptServiceServletContext) ServiceContext.getContext(
                getServletContext()).getContext(ScriptServiceServletContext.class
                        );
        _fileUploadSupport = _scriptSericeContext.getFileUploadSupport();
        _serviceDispatcher = _scriptSericeContext.getServiceDispatcher();
        
        if(_serviceDispatcher.getScriptSourceProvider().getClassLoader() != null) {
            Thread.currentThread().setContextClassLoader(
                    _serviceDispatcher.getScriptSourceProvider().getClassLoader()
                    );
        }
        
        _servletNum = _servletCounter.getAndIncrement();
        logger.info("ScriptServlet[" + _servletNum + "] --> inited.");
    }
    
    @Override
    public void destroy() {
        super.destroy();
        
        _scriptSericeContext = null;
        _fileUploadSupport = null;
        _serviceDispatcher = null;
        
        logger.info("ScriptServlet[" + _servletNum + "] --> destroyed.");
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
            requestWrapper = _fileUploadSupport.parseMultipartRequest(req);
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
        try {
            Object retVal;
            try {
                retVal = _serviceDispatcher.dispatch(request, response);
            } catch (Throwable error) {
                if(error.getClass() == MethodAccessNoAuthorityException.class) {
                    retVal = new Error(error.getClass().getSimpleName());
                } else {
                    throw error;
                }
            }
            
            if(retVal == null) {
                return null;
            }  
            
            final String responseType = ((HttpServletRequest) request.getRequest()).getHeader(HTTP_HEADER_RESPONSE_TYPE);
            
            //Default prettify
            boolean bResponsePrettify = true;
            final String responsePrettify = ((HttpServletRequest) request.getRequest()).getHeader(HTTP_HEADER_RESPONSE_PRETTIFY);
            if(responsePrettify != null && responsePrettify.length() > 0 && responsePrettify.equalsIgnoreCase("false")) {
                bResponsePrettify = false;
            }
            
            final String responseText = ResponseConverter.convertResponse(
                    responseType, bResponsePrettify,
                    retVal, 
                    request.getCharacterEncoding()
                    );
            if(logger.isDebugEnabled()) {
                logger.debug("Script service finish -> "
                        + " URI:" + ((HttpServletRequest) request.getRequest()).getRequestURI()
                        + " responseText:\n" + responseText
                        );
            }
            
            return responseText;
        } catch (Throwable e) {
            logger.error("URI:" + ((HttpServletRequest) request.getRequest()).getRequestURI(), e);
            return null;
        }
        
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
