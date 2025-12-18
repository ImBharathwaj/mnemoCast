import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { campaignApi, creativeApi } from '../services/api';
import { Campaign, Creative } from '../types';
import { transformMediaUrl } from '../utils/urlTransform';

const CampaignDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [campaign, setCampaign] = useState<Campaign | null>(null);
  const [creatives, setCreatives] = useState<Creative[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (id) {
      loadCampaignDetails();
      loadCreatives();
    }
  }, [id]);

  const loadCampaignDetails = async () => {
    if (!id) return;
    
    try {
      setLoading(true);
      const data = await campaignApi.get(id);
      setCampaign(data);
      setError(null);
    } catch (err: any) {
      console.error('Failed to load campaign:', err);
      setError(err.response?.data || err.message || 'Failed to load campaign');
    } finally {
      setLoading(false);
    }
  };

  const loadCreatives = async () => {
    if (!id) return;
    
    try {
      const data = await creativeApi.listByCampaign(id);
      setCreatives(data || []);
    } catch (err) {
      console.error('Failed to load creatives:', err);
      // Don't set error here, just log it
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  const statusColor = campaign ? {
    active: 'bg-green-100 text-green-800',
    paused: 'bg-yellow-100 text-yellow-800',
    completed: 'bg-gray-100 text-gray-800',
  }[campaign.status] || 'bg-gray-100 text-gray-800' : 'bg-gray-100 text-gray-800';

  if (loading) {
    return (
      <div className="text-center py-8">
        <p>Loading campaign details...</p>
      </div>
    );
  }

  if (error || !campaign) {
    return (
      <div>
        <div className="mb-4">
          <button
            onClick={() => navigate('/campaigns')}
            className="text-blue-600 hover:text-blue-800 flex items-center gap-2"
          >
            ← Back to Campaigns
          </button>
        </div>
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
          {error || 'Campaign not found'}
        </div>
      </div>
    );
  }

  return (
    <div>
      <div className="mb-6">
        <button
          onClick={() => navigate('/campaigns')}
          className="text-blue-600 hover:text-blue-800 flex items-center gap-2 mb-4"
        >
          ← Back to Campaigns
        </button>
      </div>

      <div className="flex justify-between items-center mb-8">
        <div className="flex-1 min-w-0 mr-4">
          <h1 className="text-3xl font-bold text-gray-900 truncate" title={campaign.name}>{campaign.name}</h1>
          <p className="text-gray-500 mt-1 truncate" title={`Campaign ID: ${campaign.id}`}>Campaign ID: {campaign.id}</p>
        </div>
        <span className={`px-4 py-2 text-sm font-semibold rounded ${statusColor}`}>
          {campaign.status.toUpperCase()}
        </span>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Details */}
        <div className="lg:col-span-2 space-y-6">
          {/* Basic Information */}
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Basic Information</h2>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-sm font-medium text-gray-500">Advertiser ID</label>
                <p className="text-gray-900 font-medium mt-1 truncate" title={campaign.advertiserId}>{campaign.advertiserId}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-gray-500">Priority</label>
                <p className="text-gray-900 font-medium mt-1">{campaign.priority}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-gray-500">Start Date</label>
                <p className="text-gray-900 mt-1">{formatDate(campaign.startDate)}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-gray-500">End Date</label>
                <p className="text-gray-900 mt-1">{formatDate(campaign.endDate)}</p>
              </div>
            </div>
          </div>

          {/* Budget & Goals */}
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Budget & Goals</h2>
            <div className="grid grid-cols-2 gap-4">
              {campaign.totalBudget !== null && campaign.totalBudget !== undefined ? (
                <div>
                  <label className="text-sm font-medium text-gray-500">Total Budget</label>
                  <p className="text-gray-900 font-medium mt-1">{campaign.totalBudget.toLocaleString()} plays</p>
                </div>
              ) : (
                <div>
                  <label className="text-sm font-medium text-gray-500">Total Budget</label>
                  <p className="text-gray-500 mt-1">Not set</p>
                </div>
              )}
              {campaign.targetPlayouts !== null && campaign.targetPlayouts !== undefined ? (
                <div>
                  <label className="text-sm font-medium text-gray-500">Target Playouts</label>
                  <p className="text-gray-900 font-medium mt-1">{campaign.targetPlayouts.toLocaleString()}</p>
                </div>
              ) : (
                <div>
                  <label className="text-sm font-medium text-gray-500">Target Playouts</label>
                  <p className="text-gray-500 mt-1">Not set</p>
                </div>
              )}
            </div>
          </div>

          {/* Targeting Rules */}
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">
              Targeting Rules ({campaign.targetingRules.length})
            </h2>
            {campaign.targetingRules.length > 0 ? (
              <div className="space-y-3">
                {campaign.targetingRules.map((rule, index) => (
                  <div key={index} className="border border-gray-200 rounded-lg p-4">
                    <div className="grid grid-cols-3 gap-4">
                      <div className="min-w-0">
                        <label className="text-xs font-medium text-gray-500">Key</label>
                        <p className="text-gray-900 font-medium mt-1 truncate" title={rule.key}>{rule.key}</p>
                      </div>
                      <div className="min-w-0">
                        <label className="text-xs font-medium text-gray-500">Operator</label>
                        <p className="text-gray-900 font-medium mt-1 truncate" title={rule.operator}>{rule.operator}</p>
                      </div>
                      <div className="min-w-0">
                        <label className="text-xs font-medium text-gray-500">Value</label>
                        <p className="text-gray-900 font-medium mt-1 truncate" title={rule.value}>{rule.value}</p>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-gray-500">No targeting rules configured</p>
            )}
          </div>

          {/* Creatives */}
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">
              Creatives ({creatives.length})
            </h2>
            {creatives.length > 0 ? (
              <div className="space-y-3">
                {creatives.map((creative) => (
                  <div key={creative.id} className="border border-gray-200 rounded-lg p-4">
                    <div className="flex justify-between items-start mb-2">
                      <div className="flex-1 min-w-0 mr-2">
                        <h3 className="font-semibold text-gray-900 truncate" title={creative.name}>{creative.name}</h3>
                        <p className="text-sm text-gray-500 truncate" title={creative.id}>ID: {creative.id}</p>
                      </div>
                      <span className={`px-2 py-1 text-xs font-semibold rounded ${
                        creative.status === 'active' ? 'bg-green-100 text-green-800' : 
                        creative.status === 'paused' ? 'bg-yellow-100 text-yellow-800' : 
                        'bg-gray-100 text-gray-800'
                      }`}>
                        {creative.status}
                      </span>
                    </div>
                    <div className="grid grid-cols-2 gap-4 mt-3 text-sm">
                      <div>
                        <span className="text-gray-500">Type:</span>
                        <span className="ml-2 font-medium">{creative.creativeType}</span>
                      </div>
                      <div>
                        <span className="text-gray-500">Duration:</span>
                        <span className="ml-2 font-medium">{creative.durationSeconds}s</span>
                      </div>
                      {creative.shareOfVoice && (
                        <div>
                          <span className="text-gray-500">Share of Voice:</span>
                          <span className="ml-2 font-medium">{(creative.shareOfVoice * 100).toFixed(1)}%</span>
                        </div>
                      )}
                      {creative.frequencyCapPerScreen && (
                        <div>
                          <span className="text-gray-500">Frequency Cap:</span>
                          <span className="ml-2 font-medium">{creative.frequencyCapPerScreen} per screen</span>
                        </div>
                      )}
                    </div>
                    {creative.creativeUrl && (
                      <div className="mt-2">
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
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-gray-500">No creatives found for this campaign</p>
            )}
          </div>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Campaign Metadata */}
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Metadata</h2>
            <div className="space-y-3 text-sm">
              <div>
                <label className="text-gray-500">Created At</label>
                <p className="text-gray-900 mt-1">{formatDate(campaign.createdAt)}</p>
              </div>
              <div>
                <label className="text-gray-500">Updated At</label>
                <p className="text-gray-900 mt-1">{formatDate(campaign.updatedAt)}</p>
              </div>
            </div>
          </div>

          {/* Actions */}
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Actions</h2>
            <div className="space-y-2">
              <Link
                to={`/creatives?campaignId=${campaign.id}`}
                className="block w-full text-center bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors"
              >
                View All Creatives
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CampaignDetail;

