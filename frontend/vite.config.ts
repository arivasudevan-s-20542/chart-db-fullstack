import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { visualizer } from 'rollup-plugin-visualizer';
import path from 'path';
import UnpluginInjectPreload from 'unplugin-inject-preload/vite';
import { execSync } from 'child_process';

// Get git commit hash for versioning
const getGitCommit = () => {
    try {
        return execSync('git rev-parse HEAD').toString().trim();
    } catch {
        return 'unknown';
    }
};

// https://vitejs.dev/config/
export default defineConfig({
    define: {
        'import.meta.env.VITE_BUILD_VERSION': JSON.stringify(
            process.env.npm_package_version || '1.0.0'
        ),
        'import.meta.env.VITE_BUILD_COMMIT': JSON.stringify(getGitCommit()),
        'import.meta.env.VITE_BUILD_TIME': JSON.stringify(
            new Date().toISOString()
        ),
    },
    plugins: [
        react(),
        visualizer({
            filename: './stats/stats.html',
            open: false,
        }),
        UnpluginInjectPreload({
            files: [
                {
                    entryMatch: /logo-light.png$/,
                    outputMatch: /logo-light-.*.png$/,
                },
                {
                    entryMatch: /logo-dark.png$/,
                    outputMatch: /logo-dark-.*.png$/,
                },
            ],
        }),
    ],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src'),
        },
    },
    server: {
        allowedHosts: ['arivasudevans-m78uhg34-5173.zcodecorp.in'],
        host: true,
        strictPort: true,
        proxy: {
            // Proxy API requests to backend
            '/api': {
                target: 'http://192.168.31.243:8080',
                changeOrigin: true,
                secure: false,
            },
            // Proxy WebSocket requests
            '/ws': {
                target: 'http://192.168.31.243:8080',
                changeOrigin: true,
                ws: true,
            },
        },
    },
    build: {
        rollupOptions: {
            external: (id) => /__test__/.test(id),
            output: {
                assetFileNames: (assetInfo) => {
                    if (
                        assetInfo.names &&
                        assetInfo.originalFileNames.some((name) =>
                            name.startsWith('src/assets/templates/')
                        )
                    ) {
                        return 'assets/[name][extname]';
                    }
                    return 'assets/[name]-[hash][extname]';
                },
            },
        },
    },
});
