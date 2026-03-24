import { fileURLToPath, URL } from "node:url";

import vue from "@vitejs/plugin-vue";
import viteAutoImport from "unplugin-auto-import/vite";
import IconsResolver from "unplugin-icons/resolver";
import { ElementPlusResolver } from "unplugin-vue-components/resolvers";
import viteComponents from "unplugin-vue-components/vite";
import { defineConfig } from "vite";
import viteCompression from "vite-plugin-compression";
import vueDevTools from "vite-plugin-vue-devtools";

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    viteAutoImport({
      imports: ["vue", "vue-router", "pinia"],
      resolvers: [ElementPlusResolver(), IconsResolver({ prefix: "Icon" })],
      dts: "src/types/auto-imports.d.ts",
    }),
    viteComponents({
      dts: "src/types/components.d.ts",
      resolvers: [
        IconsResolver({
          enabledCollections: ["ep"],
        }),
        ElementPlusResolver(),
      ],
    }),
    viteCompression({
      ext: ".gz",
      deleteOriginFile: false,
    }),
  ],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  build: {
    outDir: "dist",
    chunkSizeWarningLimit: 800,
    rollupOptions: {
      output: {
        chunkFileNames: "static/js/[name]-[hash].js",
        entryFileNames: "static/js/[name]-[hash].js",
        assetFileNames: "static/[ext]/[name]-[hash].[ext]",
        manualChunks: {
          vendor: ["vue", "vue-router", "pinia", "axios"],
          echarts: ["echarts"],
          "element-plus": ["element-plus"],
          "element-plus-icons": ["@element-plus/icons-vue"],
        },
      },
    },
  },
  server: {
    open: false,
    proxy: {
      "/api": {
        target: "http://localhost:8081",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ""),
      },
      // 代理 Strapi 上传的文件
      "/uploads": {
        target: "http://localhost:1337",
        changeOrigin: true,
      },
    },
  },
});
