import React, { useEffect, useState } from 'react';
import { screenApi } from '../services/api';
import { Screen } from '../types';
import CreateScreenModal from '../components/CreateScreenModal';

const Screens: React.FC = () => {
  const [screens, setScreens] = useState<Screen[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);

  useEffect(() => {
    loadScreens();
  }, []);

  const loadScreens = async () => {
    try {
      const data = await screenApi.list();
      setScreens(data);
    } catch (error) {
      console.error('Failed to load screens:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateSuccess = () => {
    setShowCreateModal(false);
    loadScreens();
  };

  if (loading) {
    return <div className="text-center py-8">Loading screens...</div>;
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Screens</h1>
        <button
          onClick={() => setShowCreateModal(true)}
          className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700"
        >
          + Register Screen
        </button>
      </div>

      {screens.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <p className="text-gray-500 mb-4">No screens registered.</p>
          <button
            onClick={() => setShowCreateModal(true)}
            className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700"
          >
            Register Your First Screen
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {screens.map((screen) => (
            <ScreenCard key={screen.id} screen={screen} onUpdate={loadScreens} />
          ))}
        </div>
      )}

      {showCreateModal && (
        <CreateScreenModal
          onClose={() => setShowCreateModal(false)}
          onSuccess={handleCreateSuccess}
        />
      )}
    </div>
  );
};

interface ScreenCardProps {
  screen: Screen;
  onUpdate: () => void;
}

const ScreenCard: React.FC<ScreenCardProps> = ({ screen, onUpdate }) => {
  const handleHeartbeat = async () => {
    try {
      await screenApi.heartbeat(screen.id);
      onUpdate();
    } catch (error) {
      console.error('Failed to update heartbeat:', error);
    }
  };

  return (
    <div className="bg-white rounded-lg shadow hover:shadow-lg transition-shadow">
      <div className="p-6">
        <div className="flex justify-between items-start mb-4">
          <h3 className="text-xl font-semibold text-gray-900">{screen.name}</h3>
          <div className="flex items-center gap-2">
            <span className={`w-3 h-3 rounded-full ${screen.isOnline ? 'bg-green-500' : 'bg-gray-400'}`} />
            <span className="text-sm text-gray-600">{screen.isOnline ? 'Online' : 'Offline'}</span>
          </div>
        </div>

        <div className="space-y-2 text-sm text-gray-600 mb-4">
          <div className="flex justify-between">
            <span>ID:</span>
            <span className="font-mono text-xs">{screen.id}</span>
          </div>
          {screen.location.city && (
            <div className="flex justify-between">
              <span>City:</span>
              <span className="font-medium">{screen.location.city}</span>
            </div>
          )}
          {screen.location.area && (
            <div className="flex justify-between">
              <span>Area:</span>
              <span className="font-medium">{screen.location.area}</span>
            </div>
          )}
          {screen.location.venueType && (
            <div className="flex justify-between">
              <span>Venue:</span>
              <span className="font-medium capitalize">{screen.location.venueType}</span>
            </div>
          )}
          {screen.tags.length > 0 && (
            <div>
              <span className="text-gray-600">Tags: </span>
              <div className="flex flex-wrap gap-1 mt-1">
                {screen.tags.map((tag, idx) => (
                  <span key={idx} className="px-2 py-1 bg-blue-100 text-blue-700 text-xs rounded">
                    {tag}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>

        <button
          onClick={handleHeartbeat}
          className="w-full bg-gray-100 text-gray-700 px-4 py-2 rounded hover:bg-gray-200 transition-colors text-sm"
        >
          Update Heartbeat
        </button>
      </div>
    </div>
  );
};

export default Screens;

