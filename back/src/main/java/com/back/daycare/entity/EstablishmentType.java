package com.back.daycare.entity;

/**
 * Type d'établissement d'accueil du jeune enfant / de la petite enfance.
 * <p>
 * EAJE, ALSH, RPE et LAEP proviennent des données monenfant.fr.
 * MECS, CENTRE_MATERNEL, VILLAGE_ENFANTS, PMI et CENTRE_HOSPITALIER proviennent du référentiel FINESS.
 * AUTRE est utilisé par défaut pour les données issues d'OpenStreetMap dont le type n'est pas qualifié.
 */
public enum EstablishmentType {
    /** Établissement d'Accueil du Jeune Enfant (crèche). */
    EAJE,
    /** Accueil de Loisirs Sans Hébergement. */
    ALSH,
    /** Relais Petite Enfance. */
    RPE,
    /** Lieu d'Accueil Enfants Parents. */
    LAEP,
    /** Maison d'Enfants à Caractère Social. */
    MECS,
    /** Centre maternel / parental. */
    CENTRE_MATERNEL,
    /** Village d'enfants (SOS Villages d'Enfants...). */
    VILLAGE_ENFANTS,
    /** Protection Maternelle et Infantile. */
    PMI,
    /** Centre hospitalier. */
    CENTRE_HOSPITALIER,
    /** Type inconnu / non qualifié. */
    AUTRE
}

