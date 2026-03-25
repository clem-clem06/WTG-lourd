package org.example.wtg.entities;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Baie {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String reference;

    @OneToMany(mappedBy = "baie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Unite> unites = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public List<Unite> getUnites() {
        return unites;
    }

    public void setUnites(List<Unite> unites) {
        this.unites = unites;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}