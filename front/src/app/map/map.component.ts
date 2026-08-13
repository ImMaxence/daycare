import { HeaderComponent } from '../header/header.component';
import { AfterViewInit, Component, ElementRef, ViewChild, OnDestroy, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import * as L from 'leaflet';
import { DaycareService } from '../../service/api/api/daycare.service';
import { MapDaycareResponse } from '../../service/api/model/mapDaycareResponse';
import { DaycareDetailResponse } from '../../service/api/model/daycareDetailResponse';
import { StatusUpdateRequest } from '../../service/api/model/statusUpdateRequest';

type StatusEnum = MapDaycareResponse.StatusEnum;

const FRANCE_CENTER: L.LatLngTuple = [46.6034, 1.8883];

const STATUS_META: Record<StatusEnum, { label: string; color: string }> = {
    [MapDaycareResponse.StatusEnum.AContacter]: { label: 'À contacter', color: '#9CA3AF' },
    [MapDaycareResponse.StatusEnum.Contacte]: { label: 'Contacté', color: '#3B82F6' },
    [MapDaycareResponse.StatusEnum.Entretien]: { label: 'Entretien', color: '#F59E0B' },
    [MapDaycareResponse.StatusEnum.Accepte]: { label: 'Accepté', color: '#10B981' },
    [MapDaycareResponse.StatusEnum.Refuse]: { label: 'Refusé', color: '#EF4444' },
};

@Component({
    selector: 'app-map',
    imports: [HeaderComponent, FormsModule],
    templateUrl: './map.component.html',
    styleUrl: './map.component.scss',
})
export class MapComponent implements AfterViewInit, OnDestroy {
    @ViewChild('mapContainer') mapContainer!: ElementRef;
    private map: L.Map | undefined;
    private readonly markers = new Map<string, L.Marker>();

    readonly selectedDaycare = signal<DaycareDetailResponse | null>(null);
    readonly legendEntries = Object.entries(STATUS_META).map(([status, meta]) => ({
        status: status as StatusEnum,
        ...meta,
    }));

    constructor(private readonly daycareService: DaycareService) { }

    ngAfterViewInit(): void {
        this.initMap();
        this.loadPins();
    }

    ngOnDestroy(): void {
        if (this.map) {
            this.map.remove();
        }
    }

    private initMap(): void {
        this.map = L.map(this.mapContainer.nativeElement).setView(FRANCE_CENTER, 6);

        // CARTO Voyager: clean, Google Maps-like basemap, no API key required
        L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
            maxZoom: 20,
            subdomains: 'abcd',
            attribution: '© OpenStreetMap contributors © CARTO',
        }).addTo(this.map);
    }

    private loadPins(): void {
        this.daycareService.getDaycaresForMap().subscribe((daycares) => {
            daycares.forEach((daycare) => this.addPin(daycare));
        });
    }

    private addPin(daycare: MapDaycareResponse): void {
        if (!daycare.id || daycare.latitude === undefined || daycare.longitude === undefined) {
            return;
        }

        const marker = L.marker([daycare.latitude, daycare.longitude], {
            icon: this.buildIcon(daycare.status),
        })
            .addTo(this.map!)
            .on('click', () => this.onPinClick(daycare.id!));

        this.markers.set(daycare.id, marker);
    }

    private buildIcon(status: StatusEnum | undefined): L.DivIcon {
        const color = status ? STATUS_META[status].color : STATUS_META[MapDaycareResponse.StatusEnum.AContacter].color;

        return L.divIcon({
            className: 'daycare-pin',
            html: `<span style="display:block; width:32px; height:32px; mask-image:url('/icon/custom/map.svg'); -webkit-mask-image:url('/icon/custom/map.svg'); mask-size:contain; -webkit-mask-size:contain; mask-repeat:no-repeat; -webkit-mask-repeat:no-repeat; background-color:${color};"></span>`,
            iconSize: [32, 32],
            iconAnchor: [16, 32],
        });
    }

    private onPinClick(id: string): void {
        this.daycareService.getDaycareById(id).subscribe((daycare) => {
            this.selectedDaycare.set(daycare);
            if (daycare.latitude !== undefined && daycare.longitude !== undefined) {
                this.map?.flyTo([daycare.latitude, daycare.longitude], Math.max(this.map.getZoom(), 12));
            }
        });
    }

    googleMapsUrl(daycare: DaycareDetailResponse): string {
        return `https://www.google.com/maps/search/?api=1&query=${daycare.latitude},${daycare.longitude}`;
    }

    onStatusChange(status: StatusEnum): void {
        const daycare = this.selectedDaycare();
        if (!daycare?.id) {
            return;
        }

        const request: StatusUpdateRequest = { status: status as unknown as StatusUpdateRequest.StatusEnum };

        this.daycareService.updateDaycareStatus(daycare.id, request).subscribe(() => {
            this.selectedDaycare.set({ ...daycare, status });
            this.markers.get(daycare.id!)?.setIcon(this.buildIcon(status));
        });
    }

    closePanel(): void {
        this.selectedDaycare.set(null);
    }
}
