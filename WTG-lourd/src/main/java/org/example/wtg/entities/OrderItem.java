package org.example.wtg.entities;

import jakarta.persistence.*;

@Entity
public class OrderItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer quantity;
    private Integer price;

    @Column(name = "duree_mois")
    private Integer dureeMois;

    @ManyToOne
    @JoinColumn(name = "order_ref_id", nullable = false)
    private Order orderRef;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Offre offre;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public Integer getDureeMois() {
        return dureeMois;
    }

    public void setDureeMois(Integer dureeMois) {
        this.dureeMois = dureeMois;
    }

    public Order getOrderRef() {
        return orderRef;
    }

    public void setOrderRef(Order orderRef) {
        this.orderRef = orderRef;
    }

    public Offre getOffre() {
        return offre;
    }

    public void setOffre(Offre offre) {
        this.offre = offre;
    }
}