export interface AuthRequest {
    email: string
    password: string,
    clientId: string
}

export interface AuthResponse {
    accessToken: string,
    refreshToken: string
}