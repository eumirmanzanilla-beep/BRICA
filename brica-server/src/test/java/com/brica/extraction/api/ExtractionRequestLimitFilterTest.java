package com.brica.extraction.api;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

class ExtractionRequestLimitFilterTest {
    @Test void rejectsOversizedChunkedJsonBeforeParsingOrCallingApplication() throws Exception {
        var request=new MockHttpServletRequest("PUT","/api/v1/extractions/00000000-0000-0000-0000-000000000001/review") {
            @Override public long getContentLengthLong() { return -1; }
        };
        request.setContent(new byte[ExtractionRequestLimitFilter.MAX_BYTES+100]);
        var response=new MockHttpServletResponse(); var chain=new MockFilterChain();
        new ExtractionRequestLimitFilter(JsonMapper.builder().build()).doFilter(request,response,chain);
        assertEquals(413,response.getStatus()); assertNull(chain.getRequest());
        assertTrue(response.getContentAsString().contains("EXTRACTION_REQUEST_TOO_LARGE"));
        assertEquals("EXTRACTION_REQUEST_TOO_LARGE",JsonMapper.builder().build().readTree(response.getContentAsString()).path("code").asText());
    }
    @Test void passesAnUnmodifiedBoundedBodyAndDoesNotInterceptAttachments() throws Exception {
        var filter=new ExtractionRequestLimitFilter(JsonMapper.builder().build());
        var request=new MockHttpServletRequest("POST","/api/v1/works/1/extractions"); request.setContent("{\"attachmentIds\":[1]}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var chain=new MockFilterChain(); filter.doFilter(request,new MockHttpServletResponse(),chain);
        assertEquals("{\"attachmentIds\":[1]}",new String(chain.getRequest().getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
        var attachment=new MockHttpServletRequest("POST","/api/v1/works/1/attachments");
        assertTrue(filter.shouldNotFilter(attachment));
    }
}
