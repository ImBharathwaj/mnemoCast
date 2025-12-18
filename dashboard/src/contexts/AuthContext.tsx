import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { User, LoginRequest, SignupRequest } from '../types/auth';
import { authService } from '../services/authService';

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (request: LoginRequest) => Promise<void>;
  signup: (request: SignupRequest) => Promise<void>;
  logout: () => Promise<void>;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    const initAuth = async () => {
      // Try to get user from localStorage first for faster initial render
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        try {
          setUser(JSON.parse(storedUser));
        } catch (e) {
          // Invalid stored user, clear it
          localStorage.removeItem('user');
        }
      }
      
      // Verify token is still valid by calling the API
      try {
        const currentUser = await authService.getCurrentUser();
        setUser(currentUser);
      } catch (error) {
        // Token invalid or expired, clear everything
        await authService.logout();
        setUser(null);
      } finally {
        setLoading(false);
      }
    };
    
    initAuth();
  }, []);
  
  const login = async (request: LoginRequest) => {
    const response = await authService.login(request);
    setUser(response.user);
  };
  
  const signup = async (request: SignupRequest) => {
    const response = await authService.signup(request);
    setUser(response.user);
  };
  
  const logout = async () => {
    await authService.logout();
    setUser(null);
  };
  
  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        login,
        signup,
        logout,
        isAuthenticated: !!user,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

