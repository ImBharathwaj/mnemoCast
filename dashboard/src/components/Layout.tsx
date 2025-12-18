import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import Logo from './Logo';

interface LayoutProps {
  children: React.ReactNode;
}

interface NavItem {
  path: string;
  label: string;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const isActive = (path: string) => location.pathname === path;
  
  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const navItems: NavItem[] = [
    { path: '/', label: 'Dashboard' },
    { path: '/campaigns', label: 'Campaigns' },
    { path: '/creatives', label: 'Creatives' },
    { path: '/screens', label: 'Screens' },
    { path: '/analytics', label: 'Analytics' },
    { path: '/monitoring', label: 'Monitoring' },
    { path: '/playlist', label: 'Playlist' },
  ];

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Sidebar */}
      <div className="fixed inset-y-0 left-0 w-64 bg-white shadow-lg">
        <div className="flex flex-col h-full">
          {/* Logo */}
          <div className="flex items-center justify-center h-16 bg-blue-600 text-white border-b border-blue-700 px-4">
            <Logo className="w-8 h-8" showText={true} />
          </div>

          {/* Navigation */}
          <nav className="flex-1 px-4 py-6 space-y-2">
            {navItems.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center px-4 py-3 text-sm font-medium rounded-lg transition-colors ${
                  isActive(item.path)
                    ? 'bg-blue-100 text-blue-700'
                    : 'text-gray-700 hover:bg-gray-100'
                }`}
              >
                {item.label}
              </Link>
            ))}
          </nav>
          
          {/* User info and logout */}
          <div className="px-4 py-4 border-t border-gray-200">
            {user && (
              <div className="mb-3">
                <div className="text-sm font-medium text-gray-900 truncate" title={user.email}>
                  {user.fullName || user.username}
                </div>
                <div className="text-xs text-gray-500 truncate" title={user.email}>
                  {user.email}
                </div>
              </div>
            )}
            <button
              onClick={handleLogout}
              className="w-full px-4 py-2 text-sm font-medium text-red-600 bg-red-50 rounded-lg hover:bg-red-100 transition-colors"
            >
              Logout
            </button>
          </div>
        </div>
      </div>

      {/* Main content */}
      <div className="pl-64">
        <main className="p-8">
          {children}
        </main>
      </div>
    </div>
  );
};

export default Layout;

