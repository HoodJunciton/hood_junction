# Firebase Authentication Integration Guide

This guide explains how to use Firebase Authentication in The Hood Junction application, which has replaced the previous MSG91 OTP system.

## Authentication Flow

### 1. Email/Google Authentication

1. **Client-Side**: User authenticates with Firebase using email/password or Google
2. **Client-Side**: After successful authentication, get the Firebase ID token
3. **Server-Side**: Send the ID token to `/api/auth/firebase/authenticate`
4. **Server-Side**: Server verifies the token and returns a JWT token for subsequent API calls

### 2. Phone Authentication

1. **Client-Side**: User enters phone number in your application
2. **Client-Side**: Firebase SDK sends SMS with verification code
3. **Client-Side**: User enters verification code
4. **Client-Side**: After successful verification, get the Firebase ID token
5. **Server-Side**: Send the ID token to `/api/auth/phone/verify`
6. **Server-Side**: Server verifies the token and returns a JWT token for subsequent API calls

## API Endpoints

### Firebase Authentication (Email/Google)

```
POST /api/auth/firebase/authenticate
```

Request body:
```json
{
  "idToken": "firebase-id-token-from-client",
  "authProvider": "google" // or "email"
}
```

### Firebase Phone Authentication

```
POST /api/auth/phone/verify
```

Request body:
```json
{
  "idToken": "firebase-id-token-from-client",
  "authProvider": "phone"
}
```

### Response Format (Both Endpoints)

```json
{
  "token": "jwt-token-for-api-calls",
  "username": "user's-username",
  "email": "user's-email",
  "fullName": "user's-full-name"
}
```

## Client-Side Integration Examples

### Web Application (JavaScript)

```javascript
// Initialize Firebase
const firebaseConfig = {
  apiKey: "your-api-key",
  authDomain: "your-project.firebaseapp.com",
  projectId: "your-project-id",
  storageBucket: "your-project.appspot.com",
  messagingSenderId: "your-messaging-sender-id",
  appId: "your-app-id"
};

firebase.initializeApp(firebaseConfig);

// Google Sign-in
async function signInWithGoogle() {
  const provider = new firebase.auth.GoogleAuthProvider();
  try {
    const result = await firebase.auth().signInWithPopup(provider);
    const idToken = await result.user.getIdToken();
    
    // Send token to backend
    const response = await fetch('/api/auth/firebase/authenticate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        idToken: idToken,
        authProvider: 'google'
      })
    });
    
    const data = await response.json();
    // Store JWT token for API calls
    localStorage.setItem('jwt', data.token);
    
  } catch (error) {
    console.error("Error signing in with Google:", error);
  }
}

// Phone Authentication
async function signInWithPhone() {
  // Set up reCAPTCHA verifier
  const recaptchaVerifier = new firebase.auth.RecaptchaVerifier('recaptcha-container');
  
  // Request verification code
  const phoneNumber = '+1234567890'; // Get from user input
  try {
    const confirmationResult = await firebase.auth().signInWithPhoneNumber(phoneNumber, recaptchaVerifier);
    
    // Store confirmation result to use later
    window.confirmationResult = confirmationResult;
    
    // Show verification code input field to user
    // ...
  } catch (error) {
    console.error("Error sending verification code:", error);
  }
}

// Verify code entered by user
async function verifyPhoneCode() {
  const code = '123456'; // Get from user input
  try {
    const result = await window.confirmationResult.confirm(code);
    const idToken = await result.user.getIdToken();
    
    // Send token to backend
    const response = await fetch('/api/auth/phone/verify', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        idToken: idToken,
        authProvider: 'phone'
      })
    });
    
    const data = await response.json();
    // Store JWT token for API calls
    localStorage.setItem('jwt', data.token);
    
  } catch (error) {
    console.error("Error verifying code:", error);
  }
}
```

### Android Application (Kotlin)

```kotlin
// Google Sign-in
private fun signInWithGoogle() {
    val signInIntent = googleSignInClient.signInIntent
    startActivityForResult(signInIntent, RC_SIGN_IN)
}

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == RC_SIGN_IN) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.w(TAG, "Google sign in failed", e)
        }
    }
}

private fun firebaseAuthWithGoogle(idToken: String) {
    val credential = GoogleAuthProvider.getCredential(idToken, null)
    auth.signInWithCredential(credential)
        .addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                user?.getIdToken(false)?.addOnSuccessListener { result ->
                    val firebaseToken = result.token
                    // Send token to your backend
                    sendTokenToBackend(firebaseToken, "google")
                }
            } else {
                Log.w(TAG, "signInWithCredential:failure", task.exception)
            }
        }
}

// Phone Authentication
private fun startPhoneAuth(phoneNumber: String) {
    val options = PhoneAuthOptions.newBuilder(auth)
        .setPhoneNumber(phoneNumber)
        .setTimeout(60L, TimeUnit.SECONDS)
        .setActivity(this)
        .setCallbacks(callbacks)
        .build()
    PhoneAuthProvider.verifyPhoneNumber(options)
}

private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
        signInWithPhoneAuthCredential(credential)
    }

    override fun onVerificationFailed(e: FirebaseException) {
        Log.w(TAG, "onVerificationFailed", e)
    }

    override fun onCodeSent(
        verificationId: String,
        token: PhoneAuthProvider.ForceResendingToken
    ) {
        storedVerificationId = verificationId
        resendToken = token
    }
}

private fun verifyPhoneNumberWithCode(verificationId: String, code: String) {
    val credential = PhoneAuthProvider.getCredential(verificationId, code)
    signInWithPhoneAuthCredential(credential)
}

private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
    auth.signInWithCredential(credential)
        .addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                user?.getIdToken(false)?.addOnSuccessListener { result ->
                    val firebaseToken = result.token
                    // Send token to your backend
                    sendTokenToBackend(firebaseToken, "phone")
                }
            } else {
                Log.w(TAG, "signInWithCredential:failure", task.exception)
            }
        }
}

private fun sendTokenToBackend(token: String, provider: String) {
    val url = if (provider == "phone") "/api/auth/phone/verify" else "/api/auth/firebase/authenticate"
    
    // Use Retrofit or your preferred HTTP client to send the token to your backend
    // Example with OkHttp:
    val client = OkHttpClient()
    val json = """
        {
            "idToken": "$token",
            "authProvider": "$provider"
        }
    """.trimIndent()
    
    val requestBody = json.toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
        .url("https://your-backend.com$url")
        .post(requestBody)
        .build()
    
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e(TAG, "Failed to send token to backend", e)
        }
        
        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                // Parse JWT token and store it
                // ...
            } else {
                Log.e(TAG, "Backend authentication failed: ${response.code}")
            }
        }
    })
}
```

## Security Best Practices

1. **Token Verification**: Always verify Firebase ID tokens on the server-side
2. **Token Expiration**: Set appropriate expiration times for JWT tokens
3. **HTTPS**: Use HTTPS for all API communications
4. **Rate Limiting**: Implement rate limiting for authentication endpoints
5. **Logging**: Log authentication attempts for security monitoring
6. **Error Handling**: Provide generic error messages to users to avoid information leakage

## Migrating Existing Users

If you have existing users who were using the MSG91 OTP system:

1. When they attempt to authenticate with their phone number via Firebase, check if a user with that phone number exists
2. If found, authenticate them with their existing account
3. If not found, create a new account

The FirebaseAuthService has been implemented to handle this migration automatically.

## Testing

For testing Firebase Authentication:

1. Use Firebase Authentication Emulator for local development
2. Add test phone numbers in the Firebase Console for phone authentication testing
3. Create test users for email/password authentication
