export interface TokenDecode {
    // Common JWT properties
    iss: string,
    sub: string, // Subject (whom the token refers to)
    iat: number, // Issued at (seconds since Unix epoch)
    nbf: number, // Not valid before (seconds since Unix epoch)
    exp: number, // Expiration time (seconds since Unix epoch)
    // Access Token JWT properties
    authorities?: string,
    userId: string,
    profileId?: string,
    advertiserId?: string,
    // Refresh Token JWT properties
    familyId?: string,
    jti?: string,
    clientId?: string,
}

export interface JWTTokens {
    access: string,
    refresh: string
}

export interface DecodeTokens {
    access: TokenDecode,
    refresh: TokenDecode
}