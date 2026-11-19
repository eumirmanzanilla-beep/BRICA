package com.brica.extraction.application;

import com.brica.budgets.application.BudgetService;
import com.brica.budgets.application.port.BudgetStore;
import com.brica.extraction.application.port.ExtractionStore;
import com.brica.server.security.application.*;
import com.brica.server.security.application.port.*;
import com.brica.works.application.port.WorkStore;
import com.brica.documents.application.port.ReferenceFieldStore;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExtractionPermissionsTest {
    @Configuration @EnableMethodSecurity static class Config {
        @Bean AccountStore accounts() { return mock(AccountStore.class); }
        @Bean CurrentActor actor() { return ()->Optional.of("reviewer"); }
        @Bean AccessPolicy accessPolicy(AccountStore accounts,CurrentActor actor) { return new AccessPolicy(accounts,actor); }
        @Bean ExtractionStore extractions() { return mock(ExtractionStore.class); }
        @Bean WorkStore works() { return mock(WorkStore.class); }
        @Bean BudgetStore budgetStore() { return mock(BudgetStore.class); }
        @Bean BudgetService budgets(BudgetStore budgets,WorkStore works) { return new BudgetService(budgets,works); }
        @Bean ExtractionService extraction(ExtractionStore store,WorkStore works,AccessPolicy access,BudgetService budgets) {
            return new ExtractionService(store,works,mock(SourceEvidenceService.class),access,JsonMapper.builder().build(),mock(SelectiveInformationService.class),budgets,mock(ReferenceFieldStore.class));
        }
        @Bean MappingProfileService profiles(ExtractionStore store,WorkStore works,AccessPolicy access) { return new MappingProfileService(store,works,access); }
    }
    private AnnotationConfigApplicationContext context;
    private ExtractionService extraction;
    @BeforeEach void prepare() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("reviewer","unused","ROLE_TEST"));
        context=new AnnotationConfigApplicationContext(Config.class); extraction=context.getBean(ExtractionService.class);
    }
    @AfterEach void close() { context.close(); SecurityContextHolder.clearContext(); }
    private void permissions(String... permissions) {
        when(context.getBean(AccountStore.class).findAccess("reviewer")).thenReturn(Optional.of(new AccountAccess(1L,"reviewer","Reviewer",1L,"Test",true,Set.of(permissions))));
    }
    @Test void readOnlyUserCannotCreateSaveCancelOrConfirm() {
        permissions("WORKS_READ"); var id=UUID.randomUUID();
        assertThrows(AccessDeniedException.class,()->extraction.create(1L,null));
        assertThrows(AccessDeniedException.class,()->extraction.save(id,"old",null));
        assertThrows(AccessDeniedException.class,()->extraction.cancel(id,"old"));
        assertThrows(AccessDeniedException.class,()->extraction.confirm(id,"old",new ExtractionService.Confirm(UUID.randomUUID(),"WORK",null)));
        verifyNoInteractions(context.getBean(ExtractionStore.class));
    }
    @Test void workWriteDoesNotGrantSharedClientWrite() {
        permissions("WORKS_READ","WORKS_WRITE","CLIENTS_READ");
        assertThrows(AccessDeniedException.class,()->extraction.confirm(UUID.randomUUID(),"old",new ExtractionService.Confirm(UUID.randomUUID(),"CLIENT",null)));
        verifyNoInteractions(context.getBean(ExtractionStore.class));
    }
    @Test void profilesAndPrincipalSelectionRequireWorkWriteAndReadCannotBeInferredFromOtherPermissions() {
        permissions("WORKS_READ");
        assertThrows(AccessDeniedException.class,()->context.getBean(MappingProfileService.class).save(1L,null));
        assertThrows(AccessDeniedException.class,()->context.getBean(BudgetService.class).principal(1L,UUID.randomUUID(),"old"));
        permissions("CLIENTS_WRITE","CLIENTS_READ");
        assertThrows(AccessDeniedException.class,()->extraction.get(UUID.randomUUID()));
        verifyNoInteractions(context.getBean(ExtractionStore.class),context.getBean(BudgetStore.class));
    }
}
