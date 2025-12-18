import React from 'react';

// Simple test component to check if React is working
const SimpleApp: React.FC = () => {
  return (
    <div style={{ padding: '20px', fontFamily: 'Arial, sans-serif' }}>
      <h1 style={{ color: '#2563eb' }}>Mnemocast Dashboard</h1>
      <p>If you see this, React is working!</p>
      <p>Check browser console for errors.</p>
    </div>
  );
};

export default SimpleApp;

