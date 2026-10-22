package com.indra.retail.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;


@Embeddable
public class Address {

    @Column(length = 150)
    private String street;

    @Column(length = 60)
    private String city;

    @Column(length = 60)
    private String country;

    protected Address() {

    }

    public Address(String street, String city, String country) {
        this.street = street;
        this.city = city;
        this.country = country;
    }

    public String getStreet() {
        return street;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }
}
