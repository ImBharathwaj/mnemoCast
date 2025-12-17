import React, { useEffect, useState } from 'react';
import { campaignApi } from '../services/api';
import { Campaign } from '../types';
import { Link } from 'react-router-dom';
import CreateCampaignModal from '../components/CreateCampaignModal';

const Campaigns: React.FC = () => {
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [filterActive, setFilterActive] = useState(false);

  useEffect(() => {
    loadCampaigns();
  }, [filterActive]);

  const loadCampaigns = async () => {
    try {
      const data = await campaignApi.list(filterActive);
      setCampaigns(data);
    } catch (error) {
      console.error('Failed to load campaigns:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateSuccess = () => {
    setShowCreateModal(false);
    loadCampaigns();
  };

  if (loading) {
    return <div className="text-center py-8">Loading campaigns...</div>;
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Campaigns</h1>
        <div className="flex gap-4">
          <label className="flex items-center">
            <input
              type="checkbox"
              checked={filterActive}
              onChange={(e) => setFilterActive(e.target.checked)}
              className="mr-2"
            />
            Active only
          </label>
          <button
            onClick={() => setShowCreateModal(true)}
            className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700"
          >
            + Create Campaign
          </button>
        </div>
      </div>

      {campaigns.length === 0 ? (
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <p className="text-gray-500 mb-4">No campaigns found.</p>
          <button
            onClick={() => setShowCreateModal(true)}
            className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700"
          >
            Create Your First Campaign
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {campaigns.map((campaign) => (
            <CampaignCard key={campaign.id} campaign={campaign} />
          ))}
        </div>
      )}

      {showCreateModal && (
        <CreateCampaignModal
          onClose={() => setShowCreateModal(false)}
          onSuccess={handleCreateSuccess}
        />
      )}
    </div>
  );
};

interface CampaignCardProps {
  campaign: Campaign;
}

const CampaignCard: React.FC<CampaignCardProps> = ({ campaign }) => {
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString();
  };

  const statusColor = {
    active: 'bg-green-100 text-green-800',
    paused: 'bg-yellow-100 text-yellow-800',
    completed: 'bg-gray-100 text-gray-800',
  }[campaign.status] || 'bg-gray-100 text-gray-800';

  return (
    <div className="bg-white rounded-lg shadow hover:shadow-lg transition-shadow">
      <div className="p-6">
        <div className="flex justify-between items-start mb-4">
          <h3 className="text-xl font-semibold text-gray-900">{campaign.name}</h3>
          <span className={`px-2 py-1 text-xs font-semibold rounded ${statusColor}`}>
            {campaign.status}
          </span>
        </div>

        <div className="space-y-2 text-sm text-gray-600 mb-4">
          <div className="flex justify-between">
            <span>Advertiser:</span>
            <span className="font-medium">{campaign.advertiserId}</span>
          </div>
          <div className="flex justify-between">
            <span>Priority:</span>
            <span className="font-medium">{campaign.priority}</span>
          </div>
          {campaign.totalBudget && (
            <div className="flex justify-between">
              <span>Budget:</span>
              <span className="font-medium">{campaign.totalBudget.toLocaleString()} plays</span>
            </div>
          )}
          <div className="flex justify-between">
            <span>Start:</span>
            <span>{formatDate(campaign.startDate)}</span>
          </div>
          <div className="flex justify-between">
            <span>End:</span>
            <span>{formatDate(campaign.endDate)}</span>
          </div>
          <div className="flex justify-between">
            <span>Targeting Rules:</span>
            <span className="font-medium">{campaign.targetingRules.length}</span>
          </div>
        </div>

        <Link
          to={`/campaigns/${campaign.id}`}
          className="block w-full text-center bg-blue-50 text-blue-600 px-4 py-2 rounded hover:bg-blue-100 transition-colors"
        >
          View Details
        </Link>
      </div>
    </div>
  );
};

export default Campaigns;

