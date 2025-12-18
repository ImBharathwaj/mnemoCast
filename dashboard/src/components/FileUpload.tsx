import React, { useState, useRef } from 'react';
import { mediaApi } from '../services/api';

interface FileUploadProps {
  onFileUploaded: (url: string, duration?: number) => void;
  campaignId?: string; // Made optional
  creativeId?: string;
  acceptedTypes?: string; // e.g., "image/*,video/*"
  maxSizeMB?: number;
  onError?: (error: string) => void;
}

const FileUpload: React.FC<FileUploadProps> = ({
  onFileUploaded,
  campaignId,
  creativeId,
  acceptedTypes = 'image/*,video/*',
  maxSizeMB = 500,
  onError,
}) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileSelect = async (file: File) => {
    // Validate file type
    const isValidType = acceptedTypes.split(',').some(type => {
      const pattern = type.trim().replace('*', '.*');
      return new RegExp(pattern).test(file.type);
    });

    if (!isValidType) {
      const errorMsg = `Invalid file type. Accepted types: ${acceptedTypes}`;
      setError(errorMsg);
      onError?.(errorMsg);
      return;
    }

    // Validate file size
    const fileSizeMB = file.size / (1024 * 1024);
    if (fileSizeMB > maxSizeMB) {
      const errorMsg = `File size (${fileSizeMB.toFixed(2)} MB) exceeds maximum allowed size (${maxSizeMB} MB)`;
      setError(errorMsg);
      onError?.(errorMsg);
      return;
    }

    setError(null);
    setSelectedFile(file);

    // Create preview for images and videos
    if (file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setPreview(reader.result as string);
      };
      reader.readAsDataURL(file);
    } else if (file.type.startsWith('video/')) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setPreview(reader.result as string);
      };
      reader.readAsDataURL(file);
    } else {
      setPreview(null);
    }

    // Auto-upload the file when selected
    // This makes the workflow smoother - just select file and it uploads automatically
    try {
      setUploading(true);
      setError(null);
      setUploadProgress(0);

      const response = await mediaApi.upload(
        file,
        campaignId,
        creativeId,
        (progress) => setUploadProgress(progress)
      );

      setUploadProgress(100);
      onFileUploaded(response.url, response.duration);
      
      // Reset state after successful upload (keep preview)
      setTimeout(() => {
        setUploadProgress(null);
        if (fileInputRef.current) {
          fileInputRef.current.value = '';
        }
      }, 2000);
    } catch (err: any) {
      const errorMsg = err.response?.data || err.message || 'Failed to upload file';
      setError(errorMsg);
      onError?.(errorMsg);
      // Don't clear selectedFile on error, so user can retry
    } finally {
      setUploading(false);
    }
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      handleFileSelect(file);
    }
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    const file = e.dataTransfer.files[0];
    if (file) {
      handleFileSelect(file);
    }
  };

  const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setError('Please select a file');
      return;
    }

    setUploading(true);
    setError(null);
    setUploadProgress(0);

    try {
      const response = await mediaApi.upload(
        selectedFile,
        campaignId,
        creativeId,
        (progress) => setUploadProgress(progress)
      );

      setUploadProgress(100);
      onFileUploaded(response.url, response.duration);
      
      // Reset state after successful upload
      setTimeout(() => {
        setSelectedFile(null);
        setPreview(null);
        setUploadProgress(null);
        if (fileInputRef.current) {
          fileInputRef.current.value = '';
        }
      }, 1000);
    } catch (err: any) {
      const errorMsg = err.response?.data || err.message || 'Failed to upload file';
      setError(errorMsg);
      onError?.(errorMsg);
    } finally {
      setUploading(false);
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };

  return (
    <div className="space-y-4">
      {/* File Drop Zone */}
      <div
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        className={`border-2 border-dashed rounded-lg p-6 text-center transition-colors ${
          selectedFile
            ? 'border-blue-500 bg-blue-50'
            : 'border-gray-300 hover:border-gray-400 bg-gray-50'
        }`}
      >
        {preview ? (
          <div className="space-y-2">
            {selectedFile?.type.startsWith('image/') ? (
              <img
                src={preview}
                alt="Preview"
                className="max-h-48 mx-auto rounded-lg"
              />
            ) : selectedFile?.type.startsWith('video/') ? (
              <video
                src={preview}
                controls
                className="max-h-48 mx-auto rounded-lg"
              />
            ) : null}
            <div className="text-sm text-gray-600">
              <p className="font-medium">{selectedFile?.name}</p>
              <p className="text-gray-500">{selectedFile && formatFileSize(selectedFile.size)}</p>
            </div>
          </div>
        ) : (
          <div>
            <p className="text-gray-600 mb-2">
              Drag & drop a file here, or click to select
            </p>
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
            >
              Select File
            </button>
            <input
              ref={fileInputRef}
              type="file"
              accept={acceptedTypes}
              onChange={handleFileInputChange}
              className="hidden"
            />
          </div>
        )}
      </div>

      {/* Error Display */}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
          {error}
        </div>
      )}

      {/* Upload Progress */}
      {uploadProgress !== null && (
        <div>
          <div className="flex justify-between text-sm text-gray-600 mb-1">
            <span>Uploading...</span>
            <span>{uploadProgress}%</span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-2">
            <div
              className="bg-blue-600 h-2 rounded-full transition-all duration-300"
              style={{ width: `${uploadProgress}%` }}
            />
          </div>
        </div>
      )}

      {/* Upload Button - Only show if upload failed and file is still selected */}
      {selectedFile && !uploading && uploadProgress === null && error && (
        <button
          type="button"
          onClick={handleUpload}
          className="w-full px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          Retry Upload
        </button>
      )}

      {/* Upload Complete */}
      {uploadProgress === 100 && !error && (
        <div className="text-green-600 text-sm text-center font-medium">
          ✓ File uploaded successfully! URL has been set automatically.
        </div>
      )}

      {/* Uploading Status */}
      {uploading && (
        <div className="text-blue-600 text-sm text-center">
          Uploading to MinIO...
        </div>
      )}
    </div>
  );
};

export default FileUpload;

