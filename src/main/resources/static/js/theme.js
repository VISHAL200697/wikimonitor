(function () {
    const THEME_KEY = 'wikimonitor-theme';

    function getSavedTheme() {
        try {
            const value = localStorage.getItem(THEME_KEY);
            if (value === 'light' || value === 'dark') {
                return value;
            }
        } catch (e) {
        }
        return null;
    }

    function getSystemTheme() {
        return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }

    function currentTheme() {
        const theme = document.documentElement.getAttribute('data-theme');
        return theme === 'dark' ? 'dark' : 'light';
    }

    function updateToggleButtons(theme) {
        document.querySelectorAll('[data-theme-toggle]').forEach((button) => {
            const isDark = theme === 'dark';
            button.setAttribute('aria-pressed', String(isDark));
            button.setAttribute('title', isDark ? 'Switch to light mode' : 'Switch to dark mode');

            const icon = button.querySelector('i');
            if (icon) {
                icon.classList.remove('bi-moon-stars-fill', 'bi-sun-fill');
                icon.classList.add(isDark ? 'bi-sun-fill' : 'bi-moon-stars-fill');
            }

            const label = button.querySelector('[data-theme-label]');
            if (label) {
                label.textContent = isDark ? 'Light' : 'Dark';
            }
        });
    }

    function applyTheme(theme, persist) {
        const normalized = theme === 'dark' ? 'dark' : 'light';
        document.documentElement.setAttribute('data-theme', normalized);
        if (persist) {
            try {
                localStorage.setItem(THEME_KEY, normalized);
            } catch (e) {
            }
        }
        updateToggleButtons(normalized);
        document.dispatchEvent(new CustomEvent('themechange', { detail: { theme: normalized } }));
    }

    function initializeTheme() {
        const initial = getSavedTheme() || getSystemTheme();
        applyTheme(initial, false);

        document.querySelectorAll('[data-theme-toggle]').forEach((button) => {
            if (button.dataset.themeBound === '1') {
                return;
            }
            button.dataset.themeBound = '1';
            button.addEventListener('click', () => {
                const next = currentTheme() === 'dark' ? 'light' : 'dark';
                applyTheme(next, true);
            });
        });
    }

    document.addEventListener('DOMContentLoaded', initializeTheme);

    window.WikiMonitorTheme = {
        getTheme: currentTheme,
        setTheme: (theme) => applyTheme(theme, true)
    };
})();
