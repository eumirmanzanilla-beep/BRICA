package com.brica.client.works.presentation;

import com.brica.client.clients.api.ClientDto;
import com.brica.client.works.api.WorksApiClient.Attachment;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkCreationDraftTest {
    @Test
    void retainsOnlyOpenWizardValuesAndClearsAnUploadedAttachmentWhenItsPathChanges() {
        var draft = new WorkCreationDraft();
        var contract = Path.of("C:/synthetic/contract.docx");
        var uploaded = new Attachment(4L, "CONTRACT", "contract.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 12L, "hash");

        draft.client(new ClientDto(8L, "Constructora Álamo", "Representante"));
        draft.name("Torre 3: ampliación");
        draft.description("Acceso norte");
        draft.contractAttachment(contract);
        draft.uploadedContract(uploaded);

        assertEquals("Torre 3: ampliación", draft.name());
        assertEquals(contract, draft.contractAttachment());
        assertSame(uploaded, draft.uploadedContract());

        draft.contractAttachment(Path.of("C:/synthetic/replaced-contract.docx"));

        assertNull(draft.uploadedContract());
        assertFalse(draft.created());
    }
}
