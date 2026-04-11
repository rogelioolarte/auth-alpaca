export interface AuthRequest {
    email: string
    password: string
}

export interface AuthResponse {
    accessToken: string,
    refreshToken: string
}