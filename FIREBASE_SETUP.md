# Firebase Authentication Setup Guide

This guide explains how to set up Firebase Authentication for The Hood Junction application.

## Prerequisites

1. A Google account
2. Firebase project (create one at [Firebase Console](https://console.firebase.google.com/))

## Setup Steps

### 1. Create a Firebase Project

1. Go to the [Firebase Console](https://console.firebase.google.com/)
2. Click "Add Project" and follow the setup wizard
3. Enable Google Analytics if desired

### 2. Set Up Authentication Methods

1. In your Firebase project, navigate to "Authentication" in the left sidebar
2. Click on "Sign-in method" tab
3. Enable the following authentication methods:
   - **Email/Password**
   - **Google** (configure OAuth consent screen if needed)
   - **Phone** (add test phone numbers in development)

### 3. Create a Service Account

1. In your Firebase project, go to "Project settings" (gear icon)
2. Navigate to the "Service accounts" tab
3. Click "Generate new private key"
4. Save the downloaded JSON file securely

### 4. Configure the Application

1. Rename the downloaded service account JSON file to `firebase-service-account.json`
2. Place this file in `src/main/resources/` directory
3. Make sure this file is in your `.gitignore` to avoid committing sensitive credentials

```
# Add to .gitignore
src/main/resources/firebase-service-account.json
```

### 5. Update Application Properties

The application is already configured to use Firebase in `application.yml`. Make sure the path to your service account file is correct:

```yaml
firebase:
  config:
    path: firebase-service-account.json
  auth:
    token-expiry-minutes: 60
```

## Client-Side Integration

For client applications (web, mobile), you'll need to:

1. Add Firebase SDK to your client application
2. Initialize Firebase with your web/mobile app configuration
3. Use Firebase Authentication UI or custom UI for sign-in
4. After successful authentication, send the ID token to the backend endpoint:
   - `POST /api/auth/firebase/authenticate` with the request body:
   ```json
   {
     "idToken": "firebase-id-token-from-client",
     "authProvider": "google" // or "phone"
   }
   ```

## Security Considerations

- Never expose your Firebase service account credentials
- Use proper CORS configuration for your API endpoints
- Implement rate limiting for authentication endpoints
- Monitor authentication activity in Firebase Console

## Testing

- For phone authentication testing, use the phone numbers provided in Firebase Console
- For Google authentication, you can use test accounts during development

## Troubleshooting

- Check Firebase Console logs for authentication issues
- Verify that your service account has the correct permissions
- Ensure your Firebase project is on the appropriate billing plan for your usage needs
