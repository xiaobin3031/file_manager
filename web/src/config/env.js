const MODE = import.meta.env.MODE

const ENV_MAP = {
    dev: {
        BASE_URL: `${import.meta.env.VITE_API_BASE}:${import.meta.env.VITE_API_PORT}`,
        DEBUG: true
    },
    prod: {
        BASE_URL: `${import.meta.env.VITE_API_BASE}:${import.meta.env.VITE_API_PORT}`,
        DEBUG: false
    }
}

const current = ENV_MAP[MODE] || ENV_MAP.dev

export const ENV = {
    MODE,
    ...current
}