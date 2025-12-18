import React, { useEffect, useState } from 'react';
import { playlistApi, screenApi } from '../services/api';
import { PlaylistResponse, Screen } from '../types';

const Playlist: React.FC = () => {
  const [screens, setScreens] = useState<Screen[]>([]);
  const [selectedScreen, setSelectedScreen] = useState('');
  const [durationMinutes, setDurationMinutes] = useState(3);
  const [playlist, setPlaylist] = useState<PlaylistResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadScreens();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadScreens = async () => {
    try {
      const data = await screenApi.list();
      setScreens(data);
      if (data.length > 0 && !selectedScreen) {
        setSelectedScreen(data[0].id);
      }
    } catch (error) {
      console.error('Failed to load screens:', error);
    }
  };

  const generatePlaylist = async () => {
    if (!selectedScreen) {
      setError('Please select a screen');
      return;
    }

    setLoading(true);
    setError(null);
    setPlaylist(null);

    try {
      const data = await playlistApi.generate(selectedScreen, durationMinutes);
      setPlaylist(data);
    } catch (err: any) {
      if (err.response?.status === 204) {
        setError('No playlist available for this screen. Ensure there are active campaigns matching the screen.');
      } else {
        setError(err.response?.data || err.message || 'Failed to generate playlist');
      }
    } finally {
      setLoading(false);
    }
  };

  const formatDuration = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <div>
      <h1 className="text-3xl font-bold text-gray-900 mb-8">Playlist Generator</h1>

      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Select Screen
            </label>
            <select
              value={selectedScreen}
              onChange={(e) => setSelectedScreen(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
            >
              <option value="">Select a screen</option>
              {screens.map((screen) => (
                <option key={screen.id} value={screen.id}>
                  {screen.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Duration (minutes)
            </label>
            <input
              type="number"
              min="1"
              max="60"
              value={durationMinutes}
              onChange={(e) => setDurationMinutes(parseInt(e.target.value) || 3)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="flex items-end">
            <button
              onClick={generatePlaylist}
              disabled={loading || !selectedScreen}
              className="w-full bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? 'Generating...' : 'Generate Playlist'}
            </button>
          </div>
        </div>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-6">
          {error}
        </div>
      )}

      {playlist && (
        <div className="bg-white rounded-lg shadow">
          <div className="p-6 border-b">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-2xl font-semibold text-gray-900">Generated Playlist</h2>
              <div className="flex items-center gap-4">
                <div className="text-sm text-gray-600">
                  Request ID: <span className="font-mono">{playlist.requestId}</span>
                </div>
                <button
                  onClick={() => setPlaylist(null)}
                  className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors text-sm"
                >
                  Clear
                </button>
              </div>
            </div>
            <div className="grid grid-cols-3 gap-4 text-sm">
              <div>
                <span className="text-gray-600">Screen ID:</span>
                <span className="ml-2 font-medium">{playlist.screenId}</span>
              </div>
              <div>
                <span className="text-gray-600">Total Duration:</span>
                <span className="ml-2 font-medium">{formatDuration(playlist.totalDurationSeconds)}</span>
              </div>
              <div>
                <span className="text-gray-600">Valid For:</span>
                <span className="ml-2 font-medium">{formatDuration(playlist.validForSeconds)}</span>
              </div>
            </div>
          </div>

          <div className="p-6">
            <h3 className="text-lg font-semibold mb-4">Playlist Items ({playlist.items.length})</h3>
            <div className="space-y-4">
              {playlist.items.map((item, index) => (
                <div key={index} className="border rounded-lg p-4 hover:bg-gray-50">
                  <div className="flex justify-between items-start mb-2">
                    <div>
                      <div className="font-semibold text-gray-900">
                        Position #{item.position + 1} - {item.adId}
                      </div>
                      <div className="text-sm text-gray-600 mt-1">
                        Duration: {formatDuration(item.durationSeconds)}
                      </div>
                    </div>
                    <span className="px-2 py-1 bg-blue-100 text-blue-700 text-xs rounded">
                      {item.position + 1}/{playlist.items.length}
                    </span>
                  </div>
                  <div className="mt-2">
                    <a
                      href={item.creativeUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-blue-600 hover:text-blue-800 text-sm"
                    >
                      View Creative →
                    </a>
                    {item.targetUrl && (
                      <span className="mx-2 text-gray-400">•</span>
                    )}
                    {item.targetUrl && (
                      <a
                        href={item.targetUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-blue-600 hover:text-blue-800 text-sm"
                      >
                        Target URL →
                      </a>
                    )}
                  </div>
                  {item.impressionTrackingUrl && (
                    <div className="mt-2">
                      <span className="text-xs text-gray-500">Tracking: </span>
                      <span className="text-xs font-mono text-gray-600">{item.impressionTrackingUrl}</span>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Playlist;

