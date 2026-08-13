package com.back.daycare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "daycares")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Daycare {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "osm_id")
    private Long osmId;

    @Column(nullable = false)
    private String name;

    private Double latitude;

    private Double longitude;

    @Column(name = "house_number")
    private String houseNumber;

    private String street;

    private String postcode;

    private String city;

    private String phone;

    private String operator;

    private String siret;

    @Column(columnDefinition = "TEXT")
    private String note;

    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DaycareStatus status;
}


