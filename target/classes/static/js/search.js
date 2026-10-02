// SLP: Medicine Search & Pharmacy Discovery → "Filter results by
// distance and availability", "Integrate mapping/geolocation service"

document.addEventListener('DOMContentLoaded', function () {
    var button = document.getElementById('useLocationBtn');
    var status = document.getElementById('locationStatus');
    if (!button) {
        return;
    }
    button.addEventListener('click', function () {
        if (!('geolocation' in navigator)) {
            status.textContent = 'Your browser does not support location. Enter a max distance manually instead.';
            return;
        }
        status.textContent = 'Requesting your location…';
        navigator.geolocation.getCurrentPosition(
            function (position) {
                var lat = position.coords.latitude;
                var lng = position.coords.longitude;
                document.getElementById('lat').value = lat;
                document.getElementById('lng').value = lng;
                // Remember it for this tab only, so the "view on map" link
                // below still has it even after the form resubmit reloads
                // the page and re-runs this script from scratch.
                sessionStorage.setItem('medifind_last_location', JSON.stringify({ lat: lat, lng: lng }));
                status.textContent = 'Location set — showing distances from where you are.';
                document.getElementById('searchForm').submit();
            },
            function () {
                status.textContent = 'Could not get your location. You can still search without it.';
            }
        );
    });

    showMapLinkIfLocationKnown();

    function showMapLinkIfLocationKnown() {
        var saved = sessionStorage.getItem('medifind_last_location');
        if (!saved) {
            return;
        }
        try {
            var loc = JSON.parse(saved);
            var link = document.createElement('a');
            link.href = '/patient/pharmacy-map?lat=' + encodeURIComponent(loc.lat) + '&lng=' + encodeURIComponent(loc.lng);
            link.textContent = '📍 View nearby pharmacies on the map';
            link.style.marginLeft = '10px';
            status.appendChild(document.createTextNode(' '));
            status.appendChild(link);
        } catch (e) {
            /* malformed/old sessionStorage value — ignore, nothing to show */
        }
    }
});
