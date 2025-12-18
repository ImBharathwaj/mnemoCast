# Dashboard Troubleshooting Guide

## Blank White Screen

If you see a blank white screen, follow these steps:

### 1. Check Browser Console

Open browser developer tools (F12) and check the Console tab for errors.

Common errors:
- **CORS errors**: Backend not running or CORS not configured
- **Network errors**: Backend API not accessible
- **JavaScript errors**: Check the error message

### 2. Check if Backend is Running

```bash
curl http://localhost:8080/api/v1/campaigns
```

Should return JSON (even if empty array `[]`). If you get connection refused, start the backend:

```bash
cd backend
sbt "project engineApi" run
```

### 3. Check Network Tab

In browser DevTools → Network tab:
- Check if API requests are being made
- Check response status codes
- Check for CORS errors (Status: (failed) or CORS policy errors)

### 4. Clear Browser Cache

- Hard refresh: Ctrl+Shift+R (or Cmd+Shift+R on Mac)
- Or clear cache in browser settings

### 5. Check Terminal Output

Look at the terminal where `npm start` is running for:
- Compilation errors
- Warnings
- Any error messages

### 6. Test Simple Version

Temporarily replace `App.tsx` content with a simple test:

```tsx
import React from 'react';

function App() {
  return <div style={{ padding: '20px' }}><h1>React is working!</h1></div>;
}

export default App;
```

If this shows, React is working and the issue is in the components.

### 7. Check Dependencies

```bash
cd dashboard
rm -rf node_modules package-lock.json
npm install
npm start
```

### 8. Common Issues

#### Issue: "Cannot find module"
**Solution**: Run `npm install` again

#### Issue: CORS errors in console
**Solution**: Ensure backend is running and has CORS enabled

#### Issue: "Network Error" or connection refused
**Solution**: 
1. Check backend is running on port 8080
2. Check API URL in `.env` file
3. Try: `curl http://localhost:8080/api/v1/campaigns`

#### Issue: "undefined is not an object" or similar
**Solution**: Check browser console for full error stack trace

### 9. Enable Debug Mode

The dashboard now has:
- Error boundaries (should show error message instead of blank screen)
- Console logging for all API calls
- Better error handling

Check browser console for:
- `API Request: GET ...` messages
- `API Response: 200 ...` messages
- Error messages

### 10. Quick Test

Run this in browser console (F12) when dashboard is open:

```javascript
fetch('http://localhost:8080/api/v1/campaigns')
  .then(r => r.json())
  .then(console.log)
  .catch(console.error);
```

Should log the campaigns array or an error message.

