import { defineConfig, loadEnv } from 'vite'
import { resolve } from 'path'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd());

  return {
    plugins: [],
    define: {
      __APP_ENV__: JSON.stringify(env.VITE_APP_ENV),
    },
    server: {
      proxy: {
        '/api': {
          target: env.VITE_API_BASE,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, '')
        },
        '/file/sample': {
          target: `${env.VITE_API_BASE}:${env.VITE_API_PORT}`,
          changeOrigin: true
        }
      }
    },
    base: '/',
    build: {
      outDir: 'dist', // 输出目录
      assetsDir: 'static', // 静态资源目录名
      sourcemap: false, // 是否生成 source map
      minify: 'esbuild', // 或 'terser'
      rollupOptions: {
        input: {
          main: resolve(__dirname, 'index.html'),
          login: resolve(__dirname, 'login.html'),
          video_play: resolve(__dirname, 'page/videoPlay.html'),
          image_preview: resolve(__dirname, 'page/imagePreview.html'),
        },
        output: {
          // 控制输出文件名格式
          entryFileNames: 'assets/[name].[hash].js',
          chunkFileNames: 'assets/[name].[hash].js',
          assetFileNames: 'assets/[name].[hash].[ext]'
        }
      }
    }
  };
});
