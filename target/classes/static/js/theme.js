// SLP: UI/UX Design → "Light mode", "Dark mode"
//
// The *initial* theme (based on a saved preference, or the OS setting)
// is applied by a tiny inline script in fragments/layout.html's <head>

document.addEventListener('DOMContentLoaded', function () {
    var toggle = document.getElementById('theme-toggle');
    if (!toggle) {
        return;
    }
    toggle.addEventListener('click', function () {
        var root = document.documentElement;
        var current = root.getAttribute('data-theme') === 'dark' ? 'dark' : 'light';
        var next = current === 'dark' ? 'light' : 'dark';
        root.setAttribute('data-theme', next);
        try {
            localStorage.setItem('medifind-theme', next);
        } catch (e) {
            // Private browsing / storage disabled — theme just won't persist across visits.
        }
    });
});
