// SLP: Medicine Search & Pharmacy Discovery → "Integrate mapping/
// geolocation service"
//
// Leaflet + OpenStreetMap tiles are used instead of Google Maps because
// they need no API key — a real constraint for a student project where
// nobody wants to set up billing just to show a map. Pharmacy pins come
// from /api/patient/pharmacies/map (see ApiController), not from Leaflet
// itself.
document.addEventListener('DOMContentLoaded', function () {
    var mapEl = document.getElementById('pharmacy-map');
    if (!mapEl || typeof L === 'undefined') {
        return;
    }


    var params = new URLSearchParams(window.location.search);
    var userLat = parseFloat(params.get('lat'));
    var userLng = parseFloat(params.get('lng'));
    var hasUserLocation = !isNaN(userLat) && !isNaN(userLng);


    var map = hasUserLocation
        ? L.map('pharmacy-map').setView([userLat, userLng], 13)
        : L.map('pharmacy-map').setView([23.7808, 90.4144], 12);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(map);

    var bounds = [];
    if (hasUserLocation) {
        L.circleMarker([userLat, userLng], {
            radius: 9, color: '#c1622e', fillColor: '#c1622e', fillOpacity: 0.85, weight: 2
        }).addTo(map).bindPopup('<strong>You are here</strong>');
        bounds.push([userLat, userLng]);
        var intro = document.getElementById('mapIntro');
        if (intro) {
            intro.textContent = 'Showing every verified pharmacy relative to your current location — the orange dot is you.';
        }
    }

    fetch('/api/patient/pharmacies/map')
        .then(function (res) { return res.json(); })
        .then(function (pharmacies) {
            pharmacies.forEach(function (p) {
                var marker = L.marker([p.latitude, p.longitude]).addTo(map);
                var badge = p.verified ? ' <span style="color:#0e7c66;">&#10003; Verified</span>' : '';
                marker.bindPopup(
                    '<strong>' + escapeHtml(p.name) + '</strong>' + badge +
                    '<br>' + escapeHtml(p.address) +
                    (p.openingHours ? '<br><small>' + escapeHtml(p.openingHours) + '</small>' : '')
                );
                bounds.push([p.latitude, p.longitude]);
            });
            if (bounds.length > 1) {
                map.fitBounds(bounds, { padding: [30, 30] });
            }
        })
        .catch(function () { /* map still renders with no pins on a fetch failure */ });

    function escapeHtml(text) {
        var div = document.createElement('div');
        div.textContent = text == null ? '' : text;
        return div.innerHTML;
    }
});
