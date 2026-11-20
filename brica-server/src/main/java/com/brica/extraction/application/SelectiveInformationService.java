package com.brica.extraction.application;
import com.brica.server.shared.validation.domain.ExtractionRules;

import com.brica.clients.application.*;
import com.brica.clients.application.port.ClientStore;
import com.brica.documents.application.*;
import com.brica.documents.domain.*;
import com.brica.extraction.domain.*;
import com.brica.works.application.*;
import com.brica.works.application.port.WorkStore;
import com.brica.works.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@Service
public class SelectiveInformationService {
    private final WorkStore works; private final ClientStore clients; private final DocumentInformationService information;
    public SelectiveInformationService(WorkStore works,ClientStore clients,DocumentInformationService information) { this.works=works; this.clients=clients; this.information=information; }
    private static String text(Object value) { return value==null ? null : value.toString(); }
    public static boolean clientTarget(String target) { return target!=null && target.startsWith("CLIENT_"); }
    public static void writable(String target) {
        if(target!=null && target.matches("ADDITIONAL:[A-Z0-9_]{1,50}")) { return; }
        try {
            var a=ReferenceAttribute.valueOf(target);
            ExtractionRules.require(a!=ReferenceAttribute.WORK_ID && a!=ReferenceAttribute.WORK_CLIENT_ID && a!=ReferenceAttribute.CLIENT_ID,
                    "target","Los identificadores y relaciones no son destinos escribibles.");
        } catch(IllegalArgumentException | NullPointerException failure) {
            throw new DocumentException(400,"INVALID_EXTRACTION_TARGET","Selecciona un destino de negocio permitido.");
        }
    }
    private static void correction(ExtractionReview.Decision d,Object current) {
        if(!Objects.equals(text(current),d.value()) && current!=null && !text(current).isBlank()) {
            ExtractionRules.require(d.correctionOfCapture(),"correctionOfCapture","Este flujo solo corrige captura; no aplica convenios o cambios de condiciones contractuales.");
            ExtractionRules.text("justification",d.justification(),2000,true);
            ExtractionRules.require(d.justification().strip().length()>=10,"justification","Justifica la corrección (mínimo 10 caracteres).");
        }
    }
    @Transactional @PreAuthorize("@accessPolicy.hasPermission('WORKS_READ') and @accessPolicy.hasPermission('WORKS_WRITE')")
    public String work(Long id,String expected,List<ExtractionReview.Decision> decisions) {
        var work=works.findForUpdate(id).orElseThrow(()->new WorkNotFoundException(id));
        if(!WorkRevision.etag(work).equals(expected)) { throw new StaleWorkException(); }
        Map<String,String> values=new HashMap<>();
        var source=new WorkReferenceData(work);
        for(var a:ReferenceAttribute.values()) { values.put(a.name(),text(source.valueOf(a))); }
        // Status transport is the enum, not its localized label used by document presentation.
        values.put("WORK_STATUS",work.getStatus().name());
        var additional=new LinkedHashMap<>(work.getAdditionalValues());
        for(var d:decisions) {
            writable(d.target()); ExtractionRules.require(!clientTarget(d.target()),"target","Confirma el cliente por separado.");
            Object old=d.target().startsWith("ADDITIONAL:") ? additional.get(d.target().substring(11)) : values.get(d.target());
            correction(d,old);
            if (Set.of("WORK_CONTRACTED_AMOUNT","WORK_AMOUNT_BEFORE_TAX","WORK_TAX_AMOUNT").contains(d.target())) { ExtractionRules.number(d.target(), d.value()); }
            if(d.target().startsWith("ADDITIONAL:")) { additional.put(d.target().substring(11),d.value()); }
            else { values.put(d.target(),d.value()); }
        }
        var info=new DocumentInformationService.Information(values.get("WORK_CONTRACT_NUMBER"),values.get("WORK_CONTRACTOR"),
                number(values,"WORK_CONTRACTED_AMOUNT"),number(values,"WORK_AMOUNT_BEFORE_TAX"),number(values,"WORK_TAX_AMOUNT"),
                date(values,"WORK_START_DATE"),date(values,"WORK_END_DATE"),date(values,"WORK_ACTUAL_END_DATE"),
                status(values.get("WORK_STATUS")),additional);
        work=information.updateAll(id,expected,new DocumentInformationService.CompleteInformation(values.get("WORK_NAME"),values.get("WORK_LOCATION"),values.get("WORK_DESCRIPTION"),info));
        work.changeDocumentName(values.get("WORK_DOCUMENT_NAME")); works.save(work);
        return WorkRevision.etag(work);
    }
    @Transactional @PreAuthorize("@accessPolicy.hasPermission('WORKS_READ') and @accessPolicy.hasPermission('CLIENTS_READ') and @accessPolicy.hasPermission('CLIENTS_WRITE')")
    public String client(Long id,String expected,List<ExtractionReview.Decision> decisions) {
        var client=clients.findForUpdate(id).orElseThrow(()->new ClientNotFoundException(id)); ClientRevision.check(client,expected);
        String name=client.getName(),representative=client.getLegalRepresentative(),documentName=client.getDocumentName();
        for(var d:decisions) {
            writable(d.target()); ExtractionRules.require(clientTarget(d.target()),"target","El destino no pertenece al cliente.");
            Object old=switch(d.target()) { case "CLIENT_NAME"->name; case "CLIENT_LEGAL_REPRESENTATIVE"->representative; case "CLIENT_DOCUMENT_NAME"->documentName; default->throw new DocumentException(400,"INVALID_CLIENT_TARGET","Destino no permitido."); };
            correction(d,old);
            switch(d.target()) { case "CLIENT_NAME"->name=d.value(); case "CLIENT_LEGAL_REPRESENTATIVE"->representative=d.value(); case "CLIENT_DOCUMENT_NAME"->documentName=d.value(); default->throw new IllegalStateException(); }
        }
        client.updateDetails(name,representative); client.changeDocumentName(documentName); return ClientRevision.etag(clients.save(client));
    }
    private static java.math.BigDecimal number(Map<String,String> values,String key) { String value=values.get(key); return value==null||value.isBlank()?null:new java.math.BigDecimal(value); }
    private static LocalDate date(Map<String,String> values,String key) {
        String value=values.get(key); if(value==null || value.isBlank()) { return null; }
        try { return LocalDate.parse(value); } catch(java.time.DateTimeException failure) { throw new DocumentException(400,"INVALID_EXTRACTION_DATE","Confirma fechas inequívocas en formato AAAA-MM-DD."); }
    }
    private static WorkStatus status(String value) { try { return WorkStatus.valueOf(value); } catch(Exception failure) { throw new DocumentException(400,"INVALID_WORK_STATUS","Selecciona un estado válido."); } }
}
