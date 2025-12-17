import React, { useState, useEffect } from 'react';
import { creativeApi, campaignApi } from '../services/api';
import { Campaign } from '../types';
import FileUpload from './FileUpload';

interface CreateCreativeModalProps {
  campaigns?: Campaign[]; // Make it optional since we'll load it ourselves
  onClose: () => void;
  onSuccess: () => void;
}

const CreateCreativeModal: React.FC<CreateCreativeModalProps> = ({ campaigns: propCampaigns, onClose, onSuccess }) => {
  const [campaigns, setCampaigns] = useState<Campaign[]>(propCampaigns || []);
  const [loadingCampaigns, setLoadingCampaigns] = useState(!propCampaigns || propCampaigns.length === 0);
  
  const [formData, setFormData] = useState({
    campaignId: '',
    name: '',
    creativeType: 'video',
    creativeUrl: '',
    targetUrl: '',
    durationSeconds: '30',
    status: 'active',
    shareOfVoice: '',
    frequencyCapPerScreen: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Load campaigns if not provided or empty
  useEffect(() => {
    const loadCampaigns = async () => {
      if (propCampaigns && propCampaigns.length > 0) {
        // Use provided campaigns
        setCampaigns(propCampaigns);
        setLoadingCampaigns(false);
        // Set default campaign if not already set
        if (propCampaigns.length > 0) {
          setFormData(prev => {
            if (!prev.campaignId) {
              return { ...prev, campaignId: propCampaigns[0].id };
            }
            return prev;
          });
        }
      } else {
        // Load campaigns from API
        try {
          setLoadingCampaigns(true);
          const allCampaigns = await campaignApi.list(false);
          setCampaigns(allCampaigns || []);
          // Set default campaign if available
          if (allCampaigns && allCampaigns.length > 0) {
            setFormData(prev => {
              if (!prev.campaignId) {
                return { ...prev, campaignId: allCampaigns[0].id };
              }
              return prev;
            });
          }
        } catch (err) {
          console.error('Failed to load campaigns:', err);
          setError('Failed to load campaigns. Please try again.');
        } finally {
          setLoadingCampaigns(false);
        }
      }
    };
    
    loadCampaigns();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [propCampaigns]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const creativeData = {
        campaignId: formData.campaignId,
        name: formData.name,
        creativeType: formData.creativeType,
        creativeUrl: formData.creativeUrl,
        targetUrl: formData.targetUrl || undefined,
        durationSeconds: parseInt(formData.durationSeconds),
        status: formData.status,
        shareOfVoice: formData.shareOfVoice ? parseFloat(formData.shareOfVoice) : undefined,
        frequencyCapPerScreen: formData.frequencyCapPerScreen ? parseInt(formData.frequencyCapPerScreen) : undefined,
      };

      await creativeApi.create(formData.campaignId, creativeData);
      onSuccess();
    } catch (err: any) {
      setError(err.response?.data || err.message || 'Failed to create creative');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl max-w-lg w-full mx-4">
        <div className="p-6">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-2xl font-bold text-gray-900">Create Creative</h2>
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
                Campaign *
              </label>
              {loadingCampaigns ? (
                <div className="w-full px-3 py-2 border border-gray-300 rounded-lg bg-gray-50 text-gray-500">
                  Loading campaigns...
                </div>
              ) : campaigns.length === 0 ? (
                <div className="w-full px-3 py-2 border border-red-300 rounded-lg bg-red-50 text-red-700">
                  No campaigns available. Please create a campaign first.
                </div>
              ) : (
                <select
                  required
                  value={formData.campaignId}
                  onChange={(e) => setFormData({ ...formData, campaignId: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">Select a campaign</option>
                  {campaigns.map((campaign) => (
                    <option key={campaign.id} value={campaign.id}>
                      {campaign.name}
                    </option>
                  ))}
                </select>
              )}
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
                Creative URL * (Upload file or enter URL)
              </label>
              
              {/* File Upload Component */}
              {formData.campaignId && (
                <div className="mb-3">
                  <FileUpload
                    campaignId={formData.campaignId}
                    onFileUploaded={(url, duration) => {
                      setFormData(prev => ({
                        ...prev,
                        creativeUrl: url,
                        ...(duration && { durationSeconds: duration.toString() }),
                      }));
                    }}
                    onError={(error) => setError(error)}
                    acceptedTypes={
                      formData.creativeType === 'video'
                        ? 'video/mp4,video/webm'
                        : formData.creativeType === 'image'
                        ? 'image/jpeg,image/png,image/gif,image/webp'
                        : 'image/*,video/*'
                    }
                    maxSizeMB={formData.creativeType === 'video' ? 500 : 10}
                  />
                </div>
              )}
              {!formData.campaignId && (
                <p className="text-sm text-gray-500 mb-2">
                  Select a campaign to enable file upload
                </p>
              )}
              
              <input
                type="url"
                required
                value={formData.creativeUrl}
                onChange={(e) => setFormData({ ...formData, creativeUrl: e.target.value })}
                placeholder="https://example.com/ad.mp4 or upload file above"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
              />
              <p className="mt-1 text-xs text-gray-500">
                Upload a file using the uploader above, or enter a URL manually.
              </p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Target URL (Optional)
              </label>
              <input
                type="url"
                value={formData.targetUrl}
                onChange={(e) => setFormData({ ...formData, targetUrl: e.target.value })}
                placeholder="https://example.com/landing"
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
                disabled={loading || loadingCampaigns || campaigns.length === 0 || !formData.campaignId}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {loading ? 'Creating...' : 'Create Creative'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default CreateCreativeModal;

