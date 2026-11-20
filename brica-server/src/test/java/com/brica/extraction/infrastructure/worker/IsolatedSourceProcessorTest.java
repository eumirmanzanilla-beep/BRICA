package com.brica.extraction.infrastructure.worker;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class IsolatedSourceProcessorTest {
    @Test void missingDelegatedCgroupFailsBeforeStartingAnyReaderOrInterpreter() {
        var processor=new IsolatedSourceProcessor(JsonMapper.builder().build(),"/must-not-execute/python","/must-not-read/script.py","");
        var error=assertThrows(java.io.IOException.class,()->processor.process(List.of(),()->true));
        assertEquals("ISOLATION_NOT_CONFIGURED",error.getMessage());
    }
}
