import React, { useEffect, useState } from 'react';
import { creativeApi, campaignApi } from '../services/api';
import { Creative, Campaign } from '../types';
import CreateCreativeModal from '../components/CreateCreativeModal';
import EditCreativeModal from '../components/EditCreativeModal';
import { transformMediaUrl } from '../utils/urlTransform';

const Creatives: React.FC = () => {
  const [creatives, setCreatives] = useState<Creative[]>([]);
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingCreative, setEditingCreative] = useState<Creative | null>(null);
  const [selectedCampaign, setSelectedCampaign] = useState<string>('');

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedCampaign]);

  const loadData = async () => {
    try {
      const [allCreatives, allCampaigns] = await Promise.all([
        creativeApi.list(),
        campaignApi.list(false),
      ]);
      
      setCampaigns(allCampaigns);
      
      if (selectedCampaign) {
        try {
          const filtered = await creativeApi.listByCampaign(selectedCampaign);
          setCreatives(Array.isArray(filtered) ? filtered : []);
        } catch (err) {
          console.error('Failed to load creatives by campaign:', err);
          setCreatives([]);
        }
      } else {
        setCreatives(Array.isArray(allCreatives) ? allCreatives : []);
      }
    } catch (error) {
      console.error('Failed to load creatives:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateSuccess = () => {
    setShowCreateModal(false);
    loadData();
  };

  const handleEditSuccess = () => {
    setEditingCreative(null);
    loadData();
  };

  const handleDelete = async (creative: Creative) => {
    if (window.confirm(`Are you sure you want to delete "${creative.name}"? This action cannot be undone.`)) {
      try {
        await creativeApi.delete(creative.id);
        loadData();
      } catch (error) {
        console.error('Failed to delete creative:', error);
        alert('Failed to delete creative. Please try again.');
      }
    }
  };

  if (loading) {
    return <div className="text-center py-8">Loading creatives...</div>;
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Creatives</h1>
        <button
          onClick={() => setShowCreateModal(true)}
          className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700"
        >
          + Create Creative
        </button>
      </div>

      <div className="mb-4">
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Filter by Campaign
        </label>
        <select
          value={selectedCampaign}
          onChange={(e) => setSelectedCampaign(e.target.value)}
          className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
        >
          <option value="">All Campaigns</option>
          {campaigns.map((campaign) => (
            <option key={campaign.id} value={campaign.id}>
              {campaign.name}
            </option>
          ))}
        </select>
      </div>

      {creatives.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <p className="text-gray-500 mb-4">No creatives found.</p>
          <button
            onClick={() => setShowCreateModal(true)}
            className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700"
          >
            Create Your First Creative
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {creatives.map((creative) => (
            <CreativeCard 
              key={creative.id} 
              creative={creative}
              onEdit={() => setEditingCreative(creative)}
              onDelete={() => handleDelete(creative)}
            />
          ))}
        </div>
      )}

      {showCreateModal && (
        <CreateCreativeModal
          campaigns={campaigns}
          onClose={() => setShowCreateModal(false)}
          onSuccess={handleCreateSuccess}
        />
      )}

      {editingCreative && (
        <EditCreativeModal
          creative={editingCreative}
          campaigns={campaigns}
          onClose={() => setEditingCreative(null)}
          onSuccess={handleEditSuccess}
        />
      )}
    </div>
  );
};

interface CreativeCardProps {
  creative: Creative;
  onEdit: () => void;
  onDelete: () => void;
}

const CreativeCard: React.FC<CreativeCardProps> = ({ creative, onEdit, onDelete }) => {
  const statusColor = {
    active: 'bg-green-100 text-green-800',
    paused: 'bg-yellow-100 text-yellow-800',
    deleted: 'bg-red-100 text-red-800',
  }[creative.status] || 'bg-gray-100 text-gray-800';

  return (
    <div className="bg-white rounded-lg shadow hover:shadow-lg transition-shadow">
      <div className="p-6">
        <div className="flex justify-between items-start mb-4">
          <h3 className="text-xl font-semibold text-gray-900">{creative.name}</h3>
          <span className={`px-2 py-1 text-xs font-semibold rounded ${statusColor}`}>
            {creative.status}
          </span>
        </div>

        <div className="space-y-2 text-sm text-gray-600 mb-4">
          <div className="flex justify-between">
            <span>Type:</span>
            <span className="font-medium capitalize">{creative.creativeType}</span>
          </div>
          <div className="flex justify-between">
            <span>Duration:</span>
            <span className="font-medium">{creative.durationSeconds}s</span>
          </div>
          <div className="flex justify-between">
            <span>Campaign ID:</span>
            <span className="font-mono text-xs">{creative.campaignId}</span>
          </div>
          {creative.shareOfVoice && (
            <div className="flex justify-between">
              <span>Share of Voice:</span>
              <span className="font-medium">{(creative.shareOfVoice * 100).toFixed(1)}%</span>
            </div>
          )}
        </div>

        {creative.creativeUrl && (
          <div className="mb-4">
            <a
              href={transformMediaUrl(creative.creativeUrl)}
              target="_blank"
              rel="noopener noreferrer"
              className="text-blue-600 hover:text-blue-800 text-sm"
            >
              View Creative →
            </a>
          </div>
        )}

        <div className="flex gap-2">
          <button
            onClick={onEdit}
            className="flex-1 px-4 py-2 bg-yellow-50 text-yellow-700 rounded hover:bg-yellow-100 transition-colors text-sm"
          >
            Edit
          </button>
          <button
            onClick={onDelete}
            className="flex-1 px-4 py-2 bg-red-50 text-red-700 rounded hover:bg-red-100 transition-colors text-sm"
          >
            Delete
          </button>
        </div>
      </div>
    </div>
  );
};

export default Creatives;

