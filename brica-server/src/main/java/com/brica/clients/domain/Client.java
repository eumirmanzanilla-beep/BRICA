package com.brica.clients.domain;

import jakarta.persistence.*;
import java.util.LinkedHashMap;
import com.brica.server.shared.validation.domain.InputRules;

@Entity
@Table(name = "cliente")
public class Client {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column (name = "id_cliente")
    private Long id;

    @Column (name = "nombre", nullable = false, length = 255)
    private String name;

    @Column (name = "representante_legal", nullable = false, length = 255)
    private String legalRepresentative;

    protected Client() {
    }

    public Client(String name, String legalRepresentative) {
        var errors = new LinkedHashMap<String, String>();
        InputRules.requiredText(errors, "name", name, 255);
        InputRules.requiredText(errors, "legalRepresentative", legalRepresentative, 255);
        InputRules.rejectErrors(errors);
        this.name = name;
        this.legalRepresentative = legalRepresentative;
    }

    public Long getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public String getLegalRepresentative(){
        return legalRepresentative;
    }
}
