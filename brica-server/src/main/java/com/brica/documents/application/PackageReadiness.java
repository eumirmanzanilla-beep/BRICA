package com.brica.documents.application;

import com.brica.documents.application.port.DocumentConfigurationStore;
import com.brica.documents.domain.DocumentPackage;
import org.springframework.stereotype.Component;

@Component
public class PackageReadiness {
    private final DocumentConfigurationStore store;
    public PackageReadiness(DocumentConfigurationStore store) { this.store = store; }

    public String problem(DocumentPackage value, boolean lock) {
        boolean any = false;
        for (Long id : value.getTemplateIds().stream().sorted().toList()) {
            var template = store.template(id, lock);
            if (!template.isEligible()) { continue; }
            any = true;
            if (store.latest(id).isEmpty()) {
                return "La plantilla «" + template.getName() + "» necesita una versión vigente.";
            }
        }
        return any ? null : "Incluye al menos una plantilla vigente con una versión vigente.";
    }
    public void invalidateIfNecessary(DocumentPackage value) {
        if (value.isReady() && problem(value, true) != null) { value.invalidateReadiness(); }
    }
    public void templateChanged(Long id) {
        store.flush();
        for (Long packageId : store.packageIdsContaining(id)) {
            invalidateIfNecessary(store.documentPackage(packageId, true));
        }
    }
}
