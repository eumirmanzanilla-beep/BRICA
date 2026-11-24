package com.brica.client.works.presentation;

import com.brica.client.clients.api.ClientDto;
import com.brica.client.works.api.WorkDto;
import com.brica.client.works.api.WorksApiClient.Attachment;
import com.brica.client.works.api.WorksApiClient.VersionedInformation;
import java.nio.file.Path;
import java.util.Objects;

/** Temporary UI state for one open work-creation wizard; it is never persisted locally. */
public final class WorkCreationDraft {
    private ClientDto client;
    private String name = "";
    private String description = "";
    private Path contractAttachment;
    private Path baseBudgetAttachment;
    private Attachment uploadedContract;
    private Attachment uploadedBaseBudget;
    private WorkDto work;
    private VersionedInformation information;

    public ClientDto client() { return client; }
    public void client(ClientDto value) { client = value; }
    public String name() { return name; }
    public void name(String value) { name = value == null ? "" : value; }
    public String description() { return description; }
    public void description(String value) { description = value == null ? "" : value; }
    public Path contractAttachment() { return contractAttachment; }
    public void contractAttachment(Path value) {
        if (!Objects.equals(contractAttachment, value)) { uploadedContract = null; }
        contractAttachment = value;
    }
    public Path baseBudgetAttachment() { return baseBudgetAttachment; }
    public void baseBudgetAttachment(Path value) {
        if (!Objects.equals(baseBudgetAttachment, value)) { uploadedBaseBudget = null; }
        baseBudgetAttachment = value;
    }
    public Attachment uploadedContract() { return uploadedContract; }
    public void uploadedContract(Attachment value) { uploadedContract = value; }
    public Attachment uploadedBaseBudget() { return uploadedBaseBudget; }
    public void uploadedBaseBudget(Attachment value) { uploadedBaseBudget = value; }
    public WorkDto work() { return work; }
    public void work(WorkDto value) { work = value; }
    public VersionedInformation information() { return information; }
    public void information(VersionedInformation value) { information = value; }
    public boolean created() { return work != null && information != null; }
}
