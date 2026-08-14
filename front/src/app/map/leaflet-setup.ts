// The markercluster plugin's UMD factory attaches markerClusterGroup to the free
// global `L` (globalThis.L), a different object than a bundled `import * as L from
// 'leaflet'` in optimized builds. We load the global first, then the plugin, then
// re-export the augmented global so callers use the exact object the plugin mutated.
import './leaflet-global';
import 'leaflet.markercluster';
import type * as Leaflet from 'leaflet';

export const LCluster = (globalThis as unknown as { L: typeof Leaflet }).L;
