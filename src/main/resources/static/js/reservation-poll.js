// SLP: Pharmacy Reservation Fulfilment → "View incoming reservations in
// real time"

document.addEventListener('DOMContentLoaded', function () {
    var countBadge = document.getElementById('queueCount');
    if (!countBadge) {
        return;
    }
    var lastKnownCount = parseInt(countBadge.textContent, 10) || 0;

    setInterval(function () {
        fetch('/api/pharmacist/reservations/live')
            .then(function (res) { return res.json(); })
            .then(function (queue) {
                if (queue.length !== lastKnownCount) {
                    countBadge.textContent = queue.length + ' waiting — refresh to update the list';
                    countBadge.classList.add('badge-warning');
                    lastKnownCount = queue.length;
                }
            })
            .catch(function () { /* a missed poll is not worth bothering the pharmacist about */ });
    }, 20000);
});
