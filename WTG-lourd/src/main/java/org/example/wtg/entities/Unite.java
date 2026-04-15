package org.example.wtg.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Unite {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String numero;
    private String etat;
    private String nom;
    private String type;
    private String couleur;

    @JsonIgnore // Évite la boucle : Unite → Baie → Liste<Unite> → Baie → ...
    @ManyToOne
    @JoinColumn(nullable = false)
    private Baie baie;

    @JsonIgnore // Évite la boucle : Unite → User → List<Unite> → User → ...
    @ManyToOne
    @JoinColumn(name = "locataire_id")
    private User locataire;

    @Column(name = "date_fin_location")
    private LocalDateTime dateFinLocation;

    @JsonIgnore
    @ManyToMany(mappedBy = "unites")
    private List<Intervention> interventions = new ArrayList<>();

    public LocalDateTime getDateFinLocation() {
        return dateFinLocation;
    }

    public void setDateFinLocation(LocalDateTime dateFinLocation) {
        this.dateFinLocation = dateFinLocation;
    }

    public List<Intervention> getInterventions() {
        return interventions;
    }

    public void setInterventions(List<Intervention> interventions) {
        this.interventions = interventions;
    }

    public User getLocataire() {
        return locataire;
    }

    public void setLocataire(User locataire) {
        this.locataire = locataire;
    }

    public String getCouleur() {
        return couleur;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getEtat() {
        return etat;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Baie getBaie() {
        return baie;
    }

    public void setBaie(Baie baie) {
        this.baie = baie;
    }
}