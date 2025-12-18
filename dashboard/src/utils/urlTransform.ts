/**
 * Transform URLs to replace localhost with the current host for LAN access.
 * This ensures that media URLs work when accessing the dashboard from another machine.
 */

/**
 * Replace localhost/127.0.0.1 in URLs with the current hostname.
 * This allows media URLs to work when accessing from another machine on the LAN.
 */
export const transformMediaUrl = (url: string): string => {
  if (!url) return url;
  
  // If running in browser, get current hostname
  if (typeof window !== 'undefined') {
    const currentHost = window.location.hostname;
    
    // Only transform if not already on localhost
    if (currentHost !== 'localhost' && currentHost !== '127.0.0.1') {
      // Replace localhost with current host
      let transformedUrl = url.replace(/http:\/\/localhost:/g, `http://${currentHost}:`);
      transformedUrl = transformedUrl.replace(/http:\/\/127\.0\.0\.1:/g, `http://${currentHost}:`);
      
      // Also handle URLs without protocol (relative URLs)
      if (transformedUrl.startsWith('//localhost:')) {
        transformedUrl = transformedUrl.replace('//localhost:', `//${currentHost}:`);
      }
      if (transformedUrl.startsWith('//127.0.0.1:')) {
        transformedUrl = transformedUrl.replace('//127.0.0.1:', `//${currentHost}:`);
      }
      
      return transformedUrl;
    }
  }
  
  // Return original URL if on localhost or not in browser
  return url;
};

/**
 * Check if a URL contains localhost
 */
export const isLocalhostUrl = (url: string): boolean => {
  if (!url) return false;
  return url.includes('localhost') || url.includes('127.0.0.1');
};

