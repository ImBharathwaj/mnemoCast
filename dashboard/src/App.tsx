import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import Campaigns from './pages/Campaigns';
import Creatives from './pages/Creatives';
import Screens from './pages/Screens';
import Analytics from './pages/Analytics';
import Playlist from './pages/Playlist';
import './App.css';

console.log('App component loading...');

function App() {
  console.log('App component rendering...');
  
  try {
    return (
      <Router>
        <Layout>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/campaigns" element={<Campaigns />} />
            <Route path="/campaigns/:id" element={<CampaignDetail />} />
            <Route path="/creatives" element={<Creatives />} />
            <Route path="/screens" element={<Screens />} />
            <Route path="/analytics" element={<Analytics />} />
            <Route path="/playlist" element={<Playlist />} />
          </Routes>
        </Layout>
      </Router>
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

// Simple Campaign Detail page
const CampaignDetail: React.FC = () => {
  return (
    <div>
      <h1 className="text-3xl font-bold text-gray-900 mb-8">Campaign Details</h1>
      <div className="bg-white rounded-lg shadow p-6">
        <p className="text-gray-500">Campaign detail view - to be implemented</p>
      </div>
    </div>
  );
};

export default App;
