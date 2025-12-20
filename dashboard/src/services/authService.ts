import { API_BASE_URL } from '../config/api';
import { AuthResponse, LoginRequest, SignupRequest, User } from '../types/auth';

class AuthService {
  private getAuthToken(): string | null {
    return localStorage.getItem('authToken');
  }
  
  private setAuthToken(token: string): void {
    localStorage.setItem('authToken', token);
  }
  
  private removeAuthToken(): void {
    localStorage.removeItem('authToken');
  }
  
  async login(request: LoginRequest): Promise<AuthResponse> {
    const response = await fetch(`${API_BASE_URL}/api/v1/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Login failed');
    }
    
    const data: AuthResponse = await response.json();
    this.setAuthToken(data.token);
    if (data.refreshToken) {
      localStorage.setItem('refreshToken', data.refreshToken);
    }
    localStorage.setItem('user', JSON.stringify(data.user));
    return data;
  }
  
  async signup(request: SignupRequest): Promise<AuthResponse> {
    const response = await fetch(`${API_BASE_URL}/api/v1/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Signup failed');
    }
    
    const data: AuthResponse = await response.json();
    this.setAuthToken(data.token);
    if (data.refreshToken) {
      localStorage.setItem('refreshToken', data.refreshToken);
    }
    localStorage.setItem('user', JSON.stringify(data.user));
    return data;
  }
  
  async logout(): Promise<void> {
    this.removeAuthToken();
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
  }
  
  async getCurrentUser(): Promise<User | null> {
    const token = this.getAuthToken();
    if (!token) return null;
    
    try {
      const response = await fetch(`${API_BASE_URL}/api/v1/auth/me`, {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });
      
      if (!response.ok) {
        this.logout();
        return null;
      }
      
      const user: User = await response.json();
      localStorage.setItem('user', JSON.stringify(user));
      return user;
    } catch (error) {
      this.logout();
      return null;
    }
  }
  
  isAuthenticated(): boolean {
    return !!this.getAuthToken();
  }
  
  getAuthHeaders(): Record<string, string> {
    const token = this.getAuthToken();
    return token ? { 'Authorization': `Bearer ${token}` } : {};
  }
}

export const authService = new AuthService();

