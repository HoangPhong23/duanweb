import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import { keycloak } from './keycloak';
import './index.css';

// Ignore browser extension errors
window.addEventListener('error', (e) => {
    if (e.message?.includes('Could not establish connection')) {
        e.stopImmediatePropagation();
        return;
    }
});

async function bootstrap() {
    try {
        // Clear any corrupted OAuth state
        const urlParams = new URLSearchParams(window.location.search);
        if (urlParams.has('error')) {
            window.history.replaceState({}, document.title, window.location.pathname);
            localStorage.removeItem('kc-callback');
        }

        // Tự động lấy URL hiện tại (Local sẽ ra localhost:5173, Vercel sẽ ra domain vercel)
        const currentUrl = window.location.origin.replace(/\/$/, '');

        // init Keycloak
        const authenticated = await keycloak.init({
            onLoad: 'login-required',
            pkceMethod: 'S256',
            checkLoginIframe: false,
            redirectUri: currentUrl,
        });

        if (!authenticated || !keycloak.token) {
            await keycloak.login({ redirectUri: currentUrl });
            return;
        }

        (window as any).token = keycloak.token;

        const { useUserProfile } = await import('./stores/userProfile');
        await useUserProfile.getState().fetchMe();

        ReactDOM.createRoot(document.getElementById('root')!).render(
            <React.StrictMode>
                <App />
            </React.StrictMode>,
        );
    } catch (e) {
        console.error('Keycloak init error:', e);
    }
}

bootstrap();