import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
    plugins: [react()],
    server: {
        // Spring 서버의 CorsConfig 가 5173 만 허용한다.
        // 5173 이 이미 쓰이고 있을 때 5174 로 밀려서 뜨면 CORS 오류가 나므로, 그냥 실패시킨다.
        port: 5173,
        strictPort: true,
    },
});
