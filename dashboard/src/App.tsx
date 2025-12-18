import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
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
      <Router>
        <Layout>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/campaigns" element={<Campaigns />} />
            <Route path="/campaigns/:id" element={<CampaignDetail />} />
            <Route path="/creatives" element={<Creatives />} />
            <Route path="/screens" element={<Screens />} />
            <Route path="/analytics" element={<Analytics />} />
            <Route path="/monitoring" element={<Monitoring />} />
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

export default App;
