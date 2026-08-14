import * as L from 'leaflet';

// leaflet.markercluster's UMD bundle expects a global `L` (like a <script> tag setup),
// which doesn't exist in an ESM/bundled build, so we expose it manually.
(globalThis as unknown as { L: typeof L }).L = L;
