import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development, API calls to /api are forwarded to the Spring Boot backend.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: { '/api': 'http://localhost:8081' }
  }
});
