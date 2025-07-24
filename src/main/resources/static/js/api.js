// api.js (또는 main.js, App.js 초기 구동 시)
import axios from 'axios';

axios.defaults.baseURL = 'http://localhost:8080';

// 요청마다 Authorization 헤더 자동 주입
axios.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
}, error => {
    return Promise.reject(error);
});