package com.flightreservation.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "airports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Airport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** IATA 3-letter code e.g. JFK, LHR */
    @Column(nullable = false, unique = true, length = 3)
    private String iataCode;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(length = 50)
    private String timezone;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
