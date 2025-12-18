import React, { useState, useRef, useEffect } from 'react';
import { mediaApi, campaignApi } from '../services/api';
import { Campaign } from '../types';

interface UploadedFile {
  id: string;
  url: string;
  filename: string;
  size: number;
  contentType: string;
  uploadedAt: Date;
}

const MediaUpload: React.FC = () => {
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [selectedCampaignId, setSelectedCampaignId] = useState<string>('');
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState<Map<string, number>>(new Map());
  const [uploadedFiles, setUploadedFiles] = useState<UploadedFile[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [dragActive, setDragActive] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Load campaigns on mount
  useEffect(() => {
    const loadCampaigns = async () => {
      try {
        const allCampaigns = await campaignApi.list(false);
        setCampaigns(allCampaigns || []);
        if (allCampaigns && allCampaigns.length > 0) {
          setSelectedCampaignId(allCampaigns[0].id);
        }
      } catch (err) {
        console.error('Failed to load campaigns:', err);
      }
    };
    loadCampaigns();
  }, []);

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const files = Array.from(e.dataTransfer.files);
      handleFilesSelected(files);
    }
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const files = Array.from(e.target.files);
      handleFilesSelected(files);
    }
  };

  const handleFilesSelected = (files: File[]) => {
    // Filter to only images
    const imageFiles = files.filter(file => file.type.startsWith('image/'));
    
    // Validate file sizes (10MB limit for images)
    const validFiles = imageFiles.filter(file => {
      const sizeMB = file.size / (1024 * 1024);
      return sizeMB <= 10;
    });

    if (validFiles.length !== imageFiles.length) {
      setError('Some files exceeded the 10MB size limit and were not added.');
    }

    setSelectedFiles(prev => [...prev, ...validFiles]);
    setError(null);
  };

  const removeSelectedFile = (index: number) => {
    setSelectedFiles(prev => prev.filter((_, i) => i !== index));
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };

  const uploadFile = async (file: File, index: number): Promise<UploadedFile | null> => {
    if (!selectedCampaignId) {
      throw new Error('Please select a campaign first');
    }

    try {
      const response = await mediaApi.upload(
        file,
        selectedCampaignId,
        undefined,
        (progress) => {
          setUploadProgress(prev => new Map(prev).set(`${file.name}-${index}`, progress));
        }
      );

      return {
        id: `${Date.now()}-${index}`,
        url: response.url,
        filename: file.name,
        size: file.size,
        contentType: file.type,
        uploadedAt: new Date(),
      };
    } catch (err: any) {
      throw new Error(err.response?.data || err.message || 'Upload failed');
    }
  };

  const handleUploadAll = async () => {
    if (selectedFiles.length === 0) {
      setError('Please select files to upload');
      return;
    }

    // Campaign is optional for uploads

    setUploading(true);
    setError(null);
    const uploaded: UploadedFile[] = [];
    const errors: string[] = [];

    // Upload files sequentially to avoid overwhelming the server
    for (let i = 0; i < selectedFiles.length; i++) {
      try {
        const result = await uploadFile(selectedFiles[i], i);
        if (result) {
          uploaded.push(result);
        }
      } catch (err: any) {
        errors.push(`${selectedFiles[i].name}: ${err.message}`);
      }
    }

    if (uploaded.length > 0) {
      setUploadedFiles(prev => [...uploaded, ...prev]);
      setSelectedFiles([]);
    }

    if (errors.length > 0) {
      setError(`Some uploads failed:\n${errors.join('\n')}`);
    }

    setUploadProgress(new Map());
    setUploading(false);
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text).then(() => {
      // Could show a toast notification here
    });
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Media Upload</h1>
          <p className="text-gray-600 mt-1">Upload images to the server using drag and drop</p>
        </div>
      </div>

      {/* Campaign Selection */}
      <div className="bg-white rounded-lg shadow p-6">
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Campaign (Optional)
        </label>
        <select
          value={selectedCampaignId}
          onChange={(e) => setSelectedCampaignId(e.target.value)}
          className="w-full md:w-1/3 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
        >
          <option value="">No campaign (general upload)</option>
          {campaigns.map((campaign) => (
            <option key={campaign.id} value={campaign.id}>
              {campaign.name}
            </option>
          ))}
        </select>
      </div>

      {/* Drag and Drop Zone */}
      <div
        onDragEnter={handleDrag}
        onDragLeave={handleDrag}
        onDragOver={handleDrag}
        onDrop={handleDrop}
        className={`bg-white rounded-lg shadow p-8 border-2 border-dashed transition-colors ${
          dragActive
            ? 'border-blue-500 bg-blue-50'
            : 'border-gray-300 hover:border-gray-400'
        }`}
      >
        <div className="text-center">
          <svg
            className="mx-auto h-12 w-12 text-gray-400"
            stroke="currentColor"
            fill="none"
            viewBox="0 0 48 48"
            aria-hidden="true"
          >
            <path
              d="M28 8H12a4 4 0 00-4 4v20m32-12v8m0 0v8a4 4 0 01-4 4H12a4 4 0 01-4-4v-4m32-4l-3.172-3.172a4 4 0 00-5.656 0L28 28M8 32l9.172-9.172a4 4 0 015.656 0L28 28m0 0l4 4m4-24h8m-4-4v8m-12 4h.02"
              strokeWidth={2}
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
          <div className="mt-4">
            <label
              htmlFor="file-upload"
              className="cursor-pointer"
            >
              <span className="mt-2 block text-sm font-medium text-gray-900">
                Drag and drop images here, or{' '}
                <span className="text-blue-600 hover:text-blue-500">click to browse</span>
              </span>
            </label>
            <input
              id="file-upload"
              ref={fileInputRef}
              type="file"
              accept="image/*"
              multiple
              onChange={handleFileInputChange}
              className="sr-only"
            />
          </div>
          <p className="mt-1 text-xs text-gray-500">
            PNG, JPG, GIF, WebP up to 10MB each
          </p>
        </div>
      </div>

      {/* Error Display */}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
          <pre className="whitespace-pre-wrap">{error}</pre>
        </div>
      )}

      {/* Selected Files Preview */}
      {selectedFiles.length > 0 && (
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-lg font-semibold text-gray-900">
              Selected Files ({selectedFiles.length})
            </h2>
            <button
              onClick={handleUploadAll}
              disabled={uploading}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {uploading ? 'Uploading...' : 'Upload All'}
            </button>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {selectedFiles.map((file, index) => (
              <div key={index} className="relative group">
                <div className="aspect-square bg-gray-100 rounded-lg overflow-hidden">
                  <img
                    src={URL.createObjectURL(file)}
                    alt={file.name}
                    className="w-full h-full object-cover"
                  />
                </div>
                <button
                  onClick={() => removeSelectedFile(index)}
                  className="absolute top-2 right-2 bg-red-500 text-white rounded-full p-1 opacity-0 group-hover:opacity-100 transition-opacity"
                  title="Remove"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
                <div className="mt-2">
                  <p className="text-xs text-gray-600 truncate" title={file.name}>
                    {file.name}
                  </p>
                  <p className="text-xs text-gray-500">{formatFileSize(file.size)}</p>
                  {uploadProgress.has(`${file.name}-${index}`) && (
                    <div className="mt-1">
                      <div className="w-full bg-gray-200 rounded-full h-1.5">
                        <div
                          className="bg-blue-600 h-1.5 rounded-full transition-all"
                          style={{ width: `${uploadProgress.get(`${file.name}-${index}`)}%` }}
                        />
                      </div>
                      <p className="text-xs text-gray-500 mt-1">
                        {uploadProgress.get(`${file.name}-${index}`)}%
                      </p>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Uploaded Files */}
      {uploadedFiles.length > 0 && (
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">
            Uploaded Files ({uploadedFiles.length})
          </h2>
          <div className="space-y-3">
            {uploadedFiles.map((file) => (
              <div
                key={file.id}
                className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50"
              >
                <div className="flex items-center space-x-4 flex-1">
                  <div className="w-16 h-16 bg-gray-100 rounded-lg overflow-hidden flex-shrink-0">
                    <img
                      src={file.url}
                      alt={file.filename}
                      className="w-full h-full object-cover"
                    />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-900 truncate">
                      {file.filename}
                    </p>
                    <p className="text-xs text-gray-500">
                      {formatFileSize(file.size)} • {file.contentType}
                    </p>
                    <p className="text-xs text-gray-400 mt-1">
                      {file.uploadedAt.toLocaleString()}
                    </p>
                  </div>
                </div>
                <div className="flex items-center space-x-2">
                  <input
                    type="text"
                    readOnly
                    value={file.url}
                    className="px-3 py-1 text-xs border border-gray-300 rounded bg-gray-50 w-64"
                  />
                  <button
                    onClick={() => copyToClipboard(file.url)}
                    className="px-3 py-1 text-xs bg-blue-600 text-white rounded hover:bg-blue-700"
                    title="Copy URL"
                  >
                    Copy
                  </button>
                  <a
                    href={file.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="px-3 py-1 text-xs bg-gray-600 text-white rounded hover:bg-gray-700"
                    title="Open in new tab"
                  >
                    Open
                  </a>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default MediaUpload;

