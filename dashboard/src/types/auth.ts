export interface User {
  id: string;
  email: string;
  username: string;
  fullName?: string;
  role: string;
}

export interface AuthResponse {
  token: string;
  refreshToken?: string;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignupRequest {
  email: string;
  username: string;
  password: string;
  fullName?: string;
}

