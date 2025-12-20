import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import Layout from './components/Layout';
import Login from './pages/Login';
import Signup from './pages/Signup';
import Dashboard from './pages/Dashboard';
import Campaigns from './pages/Campaigns';
import CampaignDetail from './pages/CampaignDetail';
import Creatives from './pages/Creatives';
import Screens from './pages/Screens';
import Analytics from './pages/Analytics';
import Monitoring from './pages/Monitoring';
import Playlist from './pages/Playlist';
import './App.css';

console.log('App component loading...');

function App() {
  console.log('App component rendering...');
  
  try {
    return (
      <AuthProvider>
        <Router>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/signup" element={<Signup />} />
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <Layout>
                    <Dashboard />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/campaigns"
              element={
                <ProtectedRoute>
                  <Layout>
                    <Campaigns />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/campaigns/:id"
              element={
                <ProtectedRoute>
                  <Layout>
                    <CampaignDetail />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/creatives"
              element={
                <ProtectedRoute>
                  <Layout>
                    <Creatives />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/screens"
              element={
                <ProtectedRoute>
                  <Layout>
                    <Screens />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/analytics"
              element={
                <ProtectedRoute>
                  <Layout>
                    <Analytics />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/monitoring"
              element={
                <ProtectedRoute>
                  <Layout>
                    <Monitoring />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/playlist"
              element={
                <ProtectedRoute>
                  <Layout>
                    <Playlist />
                  </Layout>
                </ProtectedRoute>
              }
            />
          </Routes>
        </Router>
      </AuthProvider>
    );
  } catch (error) {
    console.error('Error in App component:', error);
    return (
      <div style={{ padding: '40px', fontFamily: 'Arial, sans-serif' }}>
        <h1 style={{ color: '#dc2626' }}>Error loading app</h1>
        <pre style={{ background: '#f3f4f6', padding: '20px', borderRadius: '8px' }}>
          {String(error)}
        </pre>
      </div>
    );
  }
}

export default App;
