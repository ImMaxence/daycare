import { HeaderComponent } from '../header/header.component';
import { AfterViewInit, Component, ElementRef, ViewChild, OnDestroy, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import * as L from 'leaflet';
import 'leaflet.markercluster';
import { DaycareService } from '../../service/api/api/daycare.service';
import { MapDaycareResponse } from '../../service/api/model/mapDaycareResponse';
import { DaycareDetailResponse } from '../../service/api/model/daycareDetailResponse';
import { StatusUpdateRequest } from '../../service/api/model/statusUpdateRequest';

type StatusEnum = MapDaycareResponse.StatusEnum;
type TypeEnum = MapDaycareResponse.TypeEnum;

const FRANCE_CENTER: L.LatLngTuple = [46.6034, 1.8883];

const STATUS_META: Record<StatusEnum, { label: string; color: string }> = {
    [MapDaycareResponse.StatusEnum.AContacter]: { label: 'À contacter', color: '#9CA3AF' },
    [MapDaycareResponse.StatusEnum.Contacte]: { label: 'Contacté', color: '#3B82F6' },
    [MapDaycareResponse.StatusEnum.Entretien]: { label: 'Entretien', color: '#F59E0B' },
    [MapDaycareResponse.StatusEnum.Accepte]: { label: 'Accepté', color: '#10B981' },
    [MapDaycareResponse.StatusEnum.Refuse]: { label: 'Refusé', color: '#EF4444' },
};

const TYPE_META: Record<TypeEnum, { label: string }> = {
    [MapDaycareResponse.TypeEnum.Eaje]: { label: 'EAJE (Établissement d\'Accueil du Jeune Enfant)' },
    [MapDaycareResponse.TypeEnum.Alsh]: { label: 'ALSH (Accueil de Loisirs Sans Hébergement)' },
    [MapDaycareResponse.TypeEnum.Rpe]: { label: 'RPE (Relais Petite Enfance)' },
    [MapDaycareResponse.TypeEnum.Laep]: { label: 'LAEP (Lieu d\'Accueil Enfants Parents)' },
    [MapDaycareResponse.TypeEnum.Mecs]: { label: 'MECS (Maison d\'Enfants à Caractère Social)' },
    [MapDaycareResponse.TypeEnum.CentreMaternel]: { label: 'Centre maternel (Centre maternel / parental)' },
    [MapDaycareResponse.TypeEnum.VillageEnfants]: { label: 'Village d\'enfants (SOS Villages d\'Enfants...)' },
    [MapDaycareResponse.TypeEnum.Pmi]: { label: 'PMI (Protection Maternelle et Infantile)' },
    [MapDaycareResponse.TypeEnum.CentreHospitalier]: { label: 'Centre hospitalier' },
    [MapDaycareResponse.TypeEnum.Autre]: { label: 'Autre (Type inconnu / non qualifié)' },
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
    private clusterGroup: L.MarkerClusterGroup | undefined;
    private readonly markers = new Map<string, L.Marker>();

    readonly selectedDaycare = signal<DaycareDetailResponse | null>(null);
    readonly legendEntries = Object.entries(STATUS_META).map(([status, meta]) => ({
        status: status as StatusEnum,
        ...meta,
    }));
    readonly typeEntries = Object.entries(TYPE_META).map(([type, meta]) => ({
        type: type as TypeEnum,
        ...meta,
    }));

    readonly searchName = signal('');
    readonly searchType = signal<TypeEnum | ''>('');
    readonly searchStatus = signal<StatusEnum | ''>('');
    readonly searchResults = signal<DaycareDetailResponse[]>([]);
    readonly searchLoading = signal(false);

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

        // groups nearby pins into a single bubble so ~2000 points stay navigable
        this.clusterGroup = L.markerClusterGroup({
            maxClusterRadius: 60,
            spiderfyOnMaxZoom: true,
            showCoverageOnHover: false,
        });
        this.map.addLayer(this.clusterGroup);
    }

    private loadPins(): void {
        this.daycareService.getDaycaresForMap().subscribe((daycares) => {
            const newMarkers = daycares
                .map((daycare) => this.createMarker(daycare))
                .filter((marker): marker is L.Marker => marker !== null);
            this.clusterGroup?.addLayers(newMarkers);
        });
    }

    private createMarker(daycare: MapDaycareResponse): L.Marker | null {
        if (!daycare.id || daycare.latitude === undefined || daycare.longitude === undefined) {
            return null;
        }

        const marker = L.marker([daycare.latitude, daycare.longitude], {
            icon: this.buildIcon(daycare.status),
        }).on('click', () => this.onPinClick(daycare.id!));

        this.markers.set(daycare.id, marker);
        return marker;
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

    typeLabel(type: TypeEnum | undefined): string {
        return type ? TYPE_META[type].label : 'Non renseigné';
    }

    statusColor(status: StatusEnum | undefined): string {
        return status ? STATUS_META[status].color : STATUS_META[MapDaycareResponse.StatusEnum.AContacter].color;
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

    search(): void {
        this.searchLoading.set(true);
        this.daycareService
            .searchDaycares(this.searchType() || undefined, this.searchStatus() || undefined, this.searchName() || undefined)
            .subscribe({
                next: (results) => {
                    this.searchResults.set(results);
                    this.searchLoading.set(false);
                    this.applySearchHighlight(results);
                },
                error: () => this.searchLoading.set(false),
            });
    }

    resetSearch(): void {
        this.searchName.set('');
        this.searchType.set('');
        this.searchStatus.set('');
        this.searchResults.set([]);
        this.highlightMarkers(new Set());
    }

    selectSearchResult(daycare: DaycareDetailResponse): void {
        this.selectedDaycare.set(daycare);
        if (daycare.latitude !== undefined && daycare.longitude !== undefined) {
            this.map?.flyTo([daycare.latitude, daycare.longitude], Math.max(this.map.getZoom(), 12));
        }
    }

    private applySearchHighlight(results: DaycareDetailResponse[]): void {
        const ids = new Set(results.map((result) => result.id).filter((id): id is string => !!id));
        this.highlightMarkers(ids);

        const points = results
            .filter((result) => result.latitude !== undefined && result.longitude !== undefined)
            .map((result) => [result.latitude!, result.longitude!] as L.LatLngTuple);
        if (points.length && this.map) {
            this.map.fitBounds(L.latLngBounds(points), { padding: [40, 40], maxZoom: 13 });
        }
    }

    private highlightMarkers(ids: Set<string>): void {
        if (!this.clusterGroup) {
            return;
        }

        const markersToShow = ids.size === 0
            ? Array.from(this.markers.values())
            : Array.from(this.markers.entries())
                .filter(([id]) => ids.has(id))
                .map(([, marker]) => marker);

        this.clusterGroup.clearLayers();
        this.clusterGroup.addLayers(markersToShow);
    }
}
