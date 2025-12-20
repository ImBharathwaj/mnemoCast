/**
 * Transform URLs to replace localhost with the current host for LAN access.
 * This ensures that media URLs work when accessing the dashboard from another machine.
 */

/**
 * Replace localhost/127.0.0.1 in URLs with the current hostname.
 * This allows media URLs to work when accessing from another machine on the LAN.
 * Also ensures port 9000 is added to URLs if missing (for MinIO and localhost).
 */
export const transformMediaUrl = (url: string): string => {
  if (!url) return url;
  
  let transformedUrl = url;
  
  // First, ensure URLs have port 9000 if they're missing a port
  // This handles:
  // 1. localhost URLs without port
  // 2. MinIO URLs (containing /mnemocast-creatives/) without port
  // 3. IP addresses without port that contain MinIO paths
  
  // Match http://<host>/mnemocast-creatives/... (MinIO pattern without port)
  // Check if URL contains mnemocast-creatives and is missing a port
  if (transformedUrl.includes('/mnemocast-creatives/') && !transformedUrl.match(/^http:\/\/[^:]+:\d+\//)) {
    // Extract the host part and add port 9000, preserving the rest of the path
    transformedUrl = transformedUrl.replace(/^http:\/\/([^\/]+)\//, 'http://$1:9000/');
  }
  
  // Match http://localhost/ or http://127.0.0.1/ (without port)
  if (transformedUrl.match(/^http:\/\/(localhost|127\.0\.0\.1)\//)) {
    transformedUrl = transformedUrl.replace(/^http:\/\/(localhost|127\.0\.0\.1)\//, 'http://$1:9000/');
  }
  
  // Match http://<ip>/api/v1/media/... (API media URLs without port)
  if (transformedUrl.match(/^http:\/\/[^\/]+\/api\/v1\/media\//)) {
    // Check if it's missing a port (doesn't have :port before the first /)
    if (!transformedUrl.match(/^http:\/\/[^:]+:\d+\//)) {
      transformedUrl = transformedUrl.replace(/^http:\/\/([^\/]+)\/(api\/v1\/media\/)/, 'http://$1:9000/$2');
    }
  }
  
  // If running in browser, get current hostname
  if (typeof window !== 'undefined') {
    const currentHost = window.location.hostname;
    
    // Only transform if not already on localhost
    if (currentHost !== 'localhost' && currentHost !== '127.0.0.1') {
      // Replace localhost:port with currentHost:port (preserve port)
      transformedUrl = transformedUrl.replace(/http:\/\/localhost:(\d+)/g, `http://${currentHost}:$1`);
      transformedUrl = transformedUrl.replace(/http:\/\/127\.0\.0\.1:(\d+)/g, `http://${currentHost}:$1`);
      
      // Replace localhost without port (shouldn't happen after above fix, but just in case)
      transformedUrl = transformedUrl.replace(/http:\/\/localhost\//g, `http://${currentHost}:9000/`);
      transformedUrl = transformedUrl.replace(/http:\/\/127\.0\.0\.1\//g, `http://${currentHost}:9000/`);
      
      // Also handle URLs without protocol (relative URLs)
      if (transformedUrl.startsWith('//localhost:')) {
        transformedUrl = transformedUrl.replace(/\/\/localhost:(\d+)/g, `//${currentHost}:$1`);
      }
      if (transformedUrl.startsWith('//127.0.0.1:')) {
        transformedUrl = transformedUrl.replace(/\/\/127\.0\.0\.1:(\d+)/g, `//${currentHost}:$1`);
      }
    }
  }
  
  return transformedUrl;
};

/**
 * Check if a URL contains localhost
 */
export const isLocalhostUrl = (url: string): boolean => {
  if (!url) return false;
  return url.includes('localhost') || url.includes('127.0.0.1');
};

