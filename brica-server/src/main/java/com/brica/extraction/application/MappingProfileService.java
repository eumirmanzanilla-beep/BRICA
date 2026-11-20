package com.brica.extraction.application;
import com.brica.extraction.application.port.ExtractionStore;
import com.brica.extraction.domain.MappingProfile;
import com.brica.server.security.application.AccessPolicy;
import com.brica.works.application.WorkNotFoundException;
import com.brica.works.application.port.WorkStore;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @Transactional(readOnly=true) @PreAuthorize("@accessPolicy.hasPermission('WORKS_READ')")
public class MappingProfileService {
    private final ExtractionStore store; private final WorkStore works; private final AccessPolicy access;
    public MappingProfileService(ExtractionStore store,WorkStore works,AccessPolicy access) { this.store=store; this.works=works; this.access=access; }
    public record View(UUID id,String name,int version,MappingProfile.Mapping mapping) { static View from(MappingProfile p) { return new View(p.getId(),p.getName(),p.getVersion(),p.getMapping()); } }
    public record Save(String name,MappingProfile.Mapping mapping) { }
    public List<View> list(Long work,int page,int size) { com.brica.budgets.application.BudgetService.pagination(page,size); works.findById(work).filter(w->w.getClient().getDeletedAt()==null).orElseThrow(()->new WorkNotFoundException(work)); return store.profiles(work,page,size).stream().map(View::from).toList(); }
    @Transactional @PreAuthorize("@accessPolicy.hasPermission('WORKS_READ') and @accessPolicy.hasPermission('WORKS_WRITE')")
    public View save(Long work,Save request) {
        works.findForUpdate(work).filter(w->w.getClient().getDeletedAt()==null).orElseThrow(()->new WorkNotFoundException(work));
        com.brica.server.shared.validation.domain.ExtractionRules.require(request!=null && request.mapping()!=null,"mapping","Indica un mapeo declarativo.");
        var p=new MappingProfile(work,request.name(),store.profileVersion(work,request.name())+1,request.mapping(),access.current().username()); store.profile(p); return View.from(p);
    }
}
