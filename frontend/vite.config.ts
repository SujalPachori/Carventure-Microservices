import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");

  // Every /backend/<service> prefix now points at the API Gateway instead of
  // a different hardcoded port per service - the gateway does the real
  // routing based on path (see api-gateway/src/main/resources/application.yml).
  // Existing frontend code (src/api/client.ts) doesn't need to change at all -
  // it still calls /backend/car, /backend/payment, etc. exactly as before.
  const gatewayTarget = env.VITE_GATEWAY_URL || "http://localhost:8090";
//   const services = ["auth", "car", "payment", "scheduling", "governance", "reporting", "user"];
  const services = ["auth", "car", "payment", "scheduling", "governance",
                    "reporting", "user", "offer", "communication", "engagement"];

  return {
    plugins: [react()],

    server: {
      port: 5173,

      proxy: Object.fromEntries(
        services.map((key) => [
          `/backend/${key}`,
          {
            target: gatewayTarget,
            changeOrigin: true,
            secure: false,
            rewrite: (path: string) => path.replace(`/backend/${key}`, ""),
          },
        ])
      ),
    },
  };
});