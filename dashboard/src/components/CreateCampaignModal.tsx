import React, { useState } from 'react';
import { campaignApi } from '../services/api';
import { TargetingRule } from '../types';

interface CreateCampaignModalProps {
  onClose: () => void;
  onSuccess: () => void;
}

const CreateCampaignModal: React.FC<CreateCampaignModalProps> = ({ onClose, onSuccess }) => {
  const [formData, setFormData] = useState({
    name: '',
    advertiserId: '',
    status: 'active',
    startDate: new Date().toISOString().split('T')[0],
    endDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    totalBudget: '',
    targetPlayouts: '',
    priority: '1',
  });
  const [targetingRules, setTargetingRules] = useState<TargetingRule[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const campaignData = {
        name: formData.name,
        advertiserId: formData.advertiserId,
        status: formData.status,
        startDate: new Date(formData.startDate).toISOString(),
        endDate: new Date(formData.endDate).toISOString(),
        totalBudget: formData.totalBudget ? parseInt(formData.totalBudget) : undefined,
        targetPlayouts: formData.targetPlayouts ? parseInt(formData.targetPlayouts) : undefined,
        priority: parseInt(formData.priority),
        targetingRules,
      };

      await campaignApi.create(campaignData);
      onSuccess();
    } catch (err: any) {
      setError(err.response?.data || err.message || 'Failed to create campaign');
    } finally {
      setLoading(false);
    }
  };

  const addTargetingRule = () => {
    setTargetingRules([...targetingRules, { key: '', operator: 'eq', value: '' }]);
  };

  const updateTargetingRule = (index: number, field: keyof TargetingRule, value: string) => {
    const updated = [...targetingRules];
    updated[index] = { ...updated[index], [field]: value };
    setTargetingRules(updated);
  };

  const removeTargetingRule = (index: number) => {
    setTargetingRules(targetingRules.filter((_, i) => i !== index));
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
        <div className="p-6">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-2xl font-bold text-gray-900">Create Campaign</h2>
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
                Campaign Name *
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
                  Advertiser ID *
                </label>
                <input
                  type="text"
                  required
                  value={formData.advertiserId}
                  onChange={(e) => setFormData({ ...formData, advertiserId: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                />
              </div>

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
                  <option value="completed">Completed</option>
                </select>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Start Date *
                </label>
                <input
                  type="date"
                  required
                  value={formData.startDate}
                  onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  End Date *
                </label>
                <input
                  type="date"
                  required
                  value={formData.endDate}
                  onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>

            <div className="grid grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Priority
                </label>
                <input
                  type="number"
                  min="1"
                  max="10"
                  value={formData.priority}
                  onChange={(e) => setFormData({ ...formData, priority: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Total Budget
                </label>
                <input
                  type="number"
                  value={formData.totalBudget}
                  onChange={(e) => setFormData({ ...formData, totalBudget: e.target.value })}
                  placeholder="Optional"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Target Playouts
                </label>
                <input
                  type="number"
                  value={formData.targetPlayouts}
                  onChange={(e) => setFormData({ ...formData, targetPlayouts: e.target.value })}
                  placeholder="Optional"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>

            <div>
              <div className="flex justify-between items-center mb-2">
                <label className="block text-sm font-medium text-gray-700">
                  Targeting Rules
                </label>
                <button
                  type="button"
                  onClick={addTargetingRule}
                  className="text-sm text-blue-600 hover:text-blue-800"
                >
                  + Add Rule
                </button>
              </div>
              <div className="space-y-2">
                {targetingRules.map((rule, index) => (
                  <div key={index} className="flex gap-2 items-center">
                    <input
                      type="text"
                      placeholder="Key (e.g., city)"
                      value={rule.key}
                      onChange={(e) => updateTargetingRule(index, 'key', e.target.value)}
                      className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm"
                    />
                    <select
                      value={rule.operator}
                      onChange={(e) => updateTargetingRule(index, 'operator', e.target.value)}
                      className="px-3 py-2 border border-gray-300 rounded-lg text-sm"
                    >
                      <option value="eq">equals</option>
                      <option value="in">in</option>
                      <option value="daypart">daypart</option>
                    </select>
                    <input
                      type="text"
                      placeholder="Value"
                      value={rule.value}
                      onChange={(e) => updateTargetingRule(index, 'value', e.target.value)}
                      className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm"
                    />
                    <button
                      type="button"
                      onClick={() => removeTargetingRule(index)}
                      className="text-red-600 hover:text-red-800"
                    >
                      ×
                    </button>
                  </div>
                ))}
                {targetingRules.length === 0 && (
                  <p className="text-sm text-gray-500">No targeting rules. Campaign will match all requests.</p>
                )}
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
                {loading ? 'Creating...' : 'Create Campaign'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default CreateCampaignModal;

