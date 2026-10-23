package com.brica.clientes.domain;

import jakarta.persistence.*;

@Entity 
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    @Column (name = "id_cliente")
    private Long idCliente;

    @Column (name = "nombre", nullable = false, length = 255)
    private String nombre;

    @Column (name = "representante_legal", nullable = false, length = 255)
    private String representanteLegal;

    protected Cliente() {
    }

    public Cliente(String nombre, String representanteLegal) {
        this.nombre = nombre;
        this.representanteLegal = representanteLegal;
    }
    
    public Long getIdCliente(){
        return idCliente;
    }

    public String getNombre(){
        return nombre;
    }

    public String getRepresentanteLegal(){
        return representanteLegal;
    }
}   