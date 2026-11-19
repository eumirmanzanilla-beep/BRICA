package com.brica.extraction.api;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/** Bound JSON before Jackson materializes a review; multipart attachments retain their own limit. */
@Component @Order(Ordered.LOWEST_PRECEDENCE-10)
public class ExtractionRequestLimitFilter extends OncePerRequestFilter {
    public static final int MAX_BYTES=1_048_576;
    private final JsonMapper json;
    public ExtractionRequestLimitFilter(JsonMapper json) { this.json=json; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path=request.getRequestURI().substring(request.getContextPath().length());
        boolean scoped=path.startsWith("/api/v1/extractions/") || path.matches("/api/v1/works/[0-9]+/(extractions|extraction-profiles)");
        return !scoped || !(request.getMethod().equals("POST") || request.getMethod().equals("PUT"));
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws IOException,ServletException {
        byte[] bytes=request.getContentLengthLong()>MAX_BYTES ? null:request.getInputStream().readNBytes(MAX_BYTES+1);
        if(bytes==null || bytes.length>MAX_BYTES) {
            var problem=com.brica.server.shared.error.api.ApiProblems.problem(HttpStatus.PAYLOAD_TOO_LARGE,
                    "EXTRACTION_REQUEST_TOO_LARGE","Borrador demasiado grande","La solicitud supera 1 MiB.","Reduce el bloque de revisión; no se guardó ningún cambio.");
            response.setStatus(413); response.setContentType("application/problem+json"); response.setCharacterEncoding("UTF-8");
            var body=new java.util.LinkedHashMap<String,Object>();
            body.put("type",problem.getType()==null?"about:blank":problem.getType().toString()); body.put("title",problem.getTitle()); body.put("status",problem.getStatus()); body.put("detail",problem.getDetail());
            if(problem.getProperties()!=null) { body.putAll(problem.getProperties()); }
            response.getOutputStream().write(json.writeValueAsBytes(body)); return;
        }
        var cached=new HttpServletRequestWrapper(request) {
            @Override public ServletInputStream getInputStream() {
                var input=new ByteArrayInputStream(bytes);
                return new ServletInputStream() {
                    public int read() { return input.read(); }
                    public int read(byte[] target,int offset,int length) { return input.read(target,offset,length); }
                    public boolean isFinished() { return input.available()==0; }
                    public boolean isReady() { return true; }
                    public void setReadListener(ReadListener listener) { throw new IllegalStateException("Synchronous bounded JSON input"); }
                };
            }
            @Override public BufferedReader getReader() { return new BufferedReader(new InputStreamReader(getInputStream(),StandardCharsets.UTF_8)); }
        };
        chain.doFilter(cached,response);
    }
}
