# mini project - daycare
## angular v22 spring boot 4 java 26 postgres 18.4s

overpass turbo openstreetmap

map daycares from BOUCHE DU RHONE and around... (GARD vaucluse var alpes de haute Provence). 

ng generate component login. 
npm run generate-api. 

[out:json][timeout:120];

// On cherche les zones par leur nom exact (avec les bons accents et tirets)
area["name"~"^(Bouches-du-Rhône|Gard|Vaucluse|Var|Alpes-de-Haute-Provence)$"]->.searchArea;

// On récupère les crèches
(
  node["amenity"~"kindergarten|childcare"](area.searchArea);
  way["amenity"~"kindergarten|childcare"](area.searchArea);
);

// On sort le tout (avec geom pour garder les coordonnées des polygones)
out geom;