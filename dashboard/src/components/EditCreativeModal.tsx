import React, { useState, useEffect } from 'react';
import { creativeApi, campaignApi } from '../services/api';
import { Creative, Campaign } from '../types';

interface EditCreativeModalProps {
  creative: Creative;
  campaigns?: Campaign[];
  onClose: () => void;
  onSuccess: () => void;
}

const EditCreativeModal: React.FC<EditCreativeModalProps> = ({ creative, campaigns: propCampaigns, onClose, onSuccess }) => {
  const [campaigns, setCampaigns] = useState<Campaign[]>(propCampaigns || []);
  const [formData, setFormData] = useState({
    name: creative.name,
    creativeType: creative.creativeType,
    creativeUrl: creative.creativeUrl,
    targetUrl: creative.targetUrl || '',
    durationSeconds: creative.durationSeconds.toString(),
    status: creative.status,
    shareOfVoice: creative.shareOfVoice?.toString() || '',
    frequencyCapPerScreen: creative.frequencyCapPerScreen?.toString() || '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!propCampaigns || propCampaigns.length === 0) {
      campaignApi.list(false).then(setCampaigns).catch(console.error);
    }
  }, [propCampaigns]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const creativeData = {
        name: formData.name,
        creativeType: formData.creativeType,
        creativeUrl: formData.creativeUrl,
        targetUrl: formData.targetUrl || undefined,
        durationSeconds: parseInt(formData.durationSeconds),
        status: formData.status,
        shareOfVoice: formData.shareOfVoice ? parseFloat(formData.shareOfVoice) : undefined,
        frequencyCapPerScreen: formData.frequencyCapPerScreen ? parseInt(formData.frequencyCapPerScreen) : undefined,
        metadata: creative.metadata, // Keep existing metadata
      };

      await creativeApi.update(creative.id, creativeData);
      onSuccess();
    } catch (err: any) {
      setError(err.response?.data || err.message || 'Failed to update creative');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl max-w-lg w-full mx-4 max-h-[90vh] overflow-y-auto">
        <div className="p-6">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-2xl font-bold text-gray-900">Edit Creative</h2>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600 text-2xl"
            >
              ×
            </button>
          </div>

          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Campaign ID (read-only)
              </label>
              <input
                type="text"
                value={creative.campaignId}
                disabled
                className="w-full px-3 py-2 border border-gray-300 rounded-lg bg-gray-50 text-gray-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Creative Name *
              </label>
              <input
                type="text"
                required
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Type *
                </label>
                <select
                  value={formData.creativeType}
                  onChange={(e) => setFormData({ ...formData, creativeType: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                >
                  <option value="video">Video</option>
                  <option value="image">Image</option>
                  <option value="html">HTML</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Duration (seconds) *
                </label>
                <input
                  type="number"
                  required
                  min="1"
                  value={formData.durationSeconds}
                  onChange={(e) => setFormData({ ...formData, durationSeconds: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Creative URL *
              </label>
              <input
                type="url"
                required
                value={formData.creativeUrl}
                onChange={(e) => setFormData({ ...formData, creativeUrl: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Target URL (Optional)
              </label>
              <input
                type="url"
                value={formData.targetUrl}
                onChange={(e) => setFormData({ ...formData, targetUrl: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Status
                </label>
                <select
                  value={formData.status}
                  onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                >
                  <option value="active">Active</option>
                  <option value="paused">Paused</option>
                  <option value="deleted">Deleted</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Share of Voice (0-1)
                </label>
                <input
                  type="number"
                  step="0.1"
                  min="0"
                  max="1"
                  value={formData.shareOfVoice}
                  onChange={(e) => setFormData({ ...formData, shareOfVoice: e.target.value })}
                  placeholder="Optional"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>

            <div className="flex justify-end gap-4 pt-4">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={loading}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
              >
                {loading ? 'Updating...' : 'Update Creative'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default EditCreativeModal;

