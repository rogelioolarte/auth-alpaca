export const API_BASE_URL = 'http://localhost:8080';
export const OAUTH2_AUTHORIZE_URI = '/oauth2/authorize';
export const UI_BASE_URL = 'http://localhost:4200';

export const ACCESS_TOKEN = 'accessToken';
export const ACCESS_TOKEN_HEADER_KEY = 'Authorization';


// separate redirect URIs are necessary to distinguish between the different OAuth2 providers and display on login

const GOOGLE_OAUTH2_REDIRECT_URI = `${UI_BASE_URL}/oauth2/google/redirect`;
export const GOOGLE_AUTH_URL = `${API_BASE_URL}${OAUTH2_AUTHORIZE_URI}/google?redirect_uri=${GOOGLE_OAUTH2_REDIRECT_URI}`;

const GITHUB_OAUTH2_REDIRECT_URI = `${UI_BASE_URL}/oauth2/github/redirect`;
export const GITHUB_AUTH_URL = `${API_BASE_URL}${OAUTH2_AUTHORIZE_URI}/github?redirect_uri=${GITHUB_OAUTH2_REDIRECT_URI}`;
