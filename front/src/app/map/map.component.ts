import { HeaderComponent } from '../header/header.component';
import { AfterViewInit, Component, ElementRef, ViewChild, OnDestroy, signal } from '@angular/core';
import * as L from 'leaflet';

interface Ping {
    id: number;
    name: string;
    description: string;
    lat: number;
    lng: number;
}

const AIX_EN_PROVENCE: L.LatLngTuple = [43.52974, 5.44742];

const PINGS: Ping[] = [
    { id: 1, name: 'Aix-en-Provence', description: 'Centre-ville et Cours Mirabeau', lat: 43.52974, lng: 5.44742 },
    { id: 2, name: 'Marseille', description: 'Vieux-Port et Notre-Dame de la Garde', lat: 43.29695, lng: 5.38107 },
    { id: 3, name: 'Aubagne', description: 'Ville natale de Marcel Pagnol', lat: 43.29296, lng: 5.57019 },
    { id: 4, name: 'Salon-de-Provence', description: "Château de l'Empéri", lat: 43.64051, lng: 5.09679 },
    { id: 5, name: 'Pertuis', description: 'Porte du Luberon', lat: 43.69469, lng: 5.50329 },
];

@Component({
    selector: 'app-map',
    imports: [HeaderComponent],
    templateUrl: './map.component.html',
    styleUrl: './map.component.scss',
})
export class MapComponent implements AfterViewInit, OnDestroy {
    @ViewChild('mapContainer') mapContainer!: ElementRef;
    private map: L.Map | undefined;

    readonly selectedPing = signal<Ping | null>(null);

    ngAfterViewInit(): void {
        this.initMap();
    }

    ngOnDestroy(): void {
        if (this.map) {
            this.map.remove();
        }
    }

    private initMap(): void {
        this.map = L.map(this.mapContainer.nativeElement).setView(AIX_EN_PROVENCE, 8);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '© OpenStreetMap contributors'
        }).addTo(this.map);

        // loaded from CDN so the default marker images resolve correctly under the Angular build
        const defaultIcon = L.icon({
            iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
            iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
            shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
            iconSize: [25, 41],
            iconAnchor: [12, 41],
            popupAnchor: [1, -34],
            shadowSize: [41, 41],
        });

        PINGS.forEach((ping) => {
            L.marker([ping.lat, ping.lng], { icon: defaultIcon })
                .addTo(this.map!)
                .on('click', () => this.selectPing(ping));
        });
    }

    private selectPing(ping: Ping): void {
        this.selectedPing.set(ping);
        this.map?.flyTo([ping.lat, ping.lng], 9);
    }
}
