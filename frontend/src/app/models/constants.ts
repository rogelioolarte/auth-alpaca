import { environment } from '../../environments/environment';

export const OAUTH2_AUTHORIZE_URI = '/oauth2/authorize';

export const CLIENT_ID = 'clientId';
export const ACCESS_TOKEN = 'accessToken';
export const REFRESH_TOKEN = 'refreshToken';
export const ACCESS_TOKEN_HEADER_KEY = 'Authorization';
export const CLIENT_ID_HEADER_KEY = 'X-Client-Id';
export const REFRESH_TOKEN_HEADER_KEY = 'X-Refresh-Token';

// separate redirect URIs are necessary to distinguish between the different OAuth2 providers and display on login
const GOOGLE_OAUTH2_REDIRECT_URI = `${environment.UI_URL}/oauth2/google/redirect`;
export const GOOGLE_AUTH_URL = `${environment.API_URL}${OAUTH2_AUTHORIZE_URI}/google?redirect_uri=${GOOGLE_OAUTH2_REDIRECT_URI}`;

const GITHUB_OAUTH2_REDIRECT_URI = `${environment.UI_URL}/oauth2/github/redirect`;
export const GITHUB_AUTH_URL = `${environment.API_URL}${OAUTH2_AUTHORIZE_URI}/github?redirect_uri=${GITHUB_OAUTH2_REDIRECT_URI}`;
