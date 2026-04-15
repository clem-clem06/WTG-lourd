package org.example.wtg.entities;

import jakarta.persistence.*;

@Entity
public class Offre {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String nom;

    @Column(name = "nombre_unites")
    private Integer nombreUnites;

    @Column(name = "prix_mensuel")
    private Integer prixMensuel;

    @Column(name = "prix_annuel")
    private Integer prixAnnuel;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Integer getNombreUnites() {
        return nombreUnites;
    }

    public void setNombreUnites(Integer nombreUnites) {
        this.nombreUnites = nombreUnites;
    }

    public Integer getPrixMensuel() {
        return prixMensuel;
    }

    public void setPrixMensuel(Integer prixMensuel) {
        this.prixMensuel = prixMensuel;
    }

    public Integer getPrixAnnuel() {
        return prixAnnuel;
    }

    public void setPrixAnnuel(Integer prixAnnuel) {
        this.prixAnnuel = prixAnnuel;
    }
}