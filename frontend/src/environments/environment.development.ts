export const environment = {
  production: false,
  API_URL: `${import.meta.env.NG_API_URL || 'http://localhost:8080'}`,
  UI_URL: `${import.meta.env.NG_UI_URL || 'http://localhost:4200'}`,
  INFINITY_LOGIN: `${import.meta.env.NG_INFINITY_LOGIN || false}`,
};
