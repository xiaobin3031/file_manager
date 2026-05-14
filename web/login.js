import { $, $$ } from '/src/util/dom.js'
import { request } from '/src/util/request.js'

// 初始化
init();

function init() {
    bindEvents();
    resize();
    window.addEventListener('resize', resize);
}

function bindEvents() {
    $('#login-btn').addEventListener('click', login);
}

function resize() {
    const root = $('#login-root');
    if (!root) return;

    root.style.height = `${window.innerHeight}px`;
    root.style.width = `${window.innerWidth}px`;
}

async function login() {
    const username = $('#username').value.trim();
    const password = $('#password').value.trim();

    if (!username || !password) {
        alert('请输入账号和密码');
        return;
    }

    try {
        const res = await request('/login', {
            method: "POST",
            body: { username, password }
        });

        // 建议：存 token
        if (res?.token) {
            localStorage.setItem('token', res.token);
        }

        // 跳转
        window.location.replace('/index.html');

    } catch (e) {
        console.error(e);
        alert('登录失败');
    }
}