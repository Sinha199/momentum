import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// In dev, the browser calls /logs on the Vite server (localhost:5173) and Vite
// forwards it to Spring Boot on :8080. Same origin from the browser's view, so no CORS needed.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/logs": "http://localhost:8080",
    },
  },
});
