# The Hood Junction

A comprehensive Spring Boot backend project with support for PostgreSQL, MongoDB, Kafka, RabbitMQ, WebSockets, Redis, and MSG91 OTP verification.

## Features

- User authentication with JWT
- PostgreSQL and MongoDB integration
- Kafka messaging
- RabbitMQ integration
- WebSocket support
- Redis caching
- MSG91 OTP verification

## MSG91 OTP Verification

The project includes integration with MSG91 for OTP (One-Time Password) verification. This allows you to implement phone number verification in your application.

### Configuration

Configure MSG91 in the `application.yml` file:

```yaml
msg91:
  auth-key: YOUR_MSG91_AUTH_KEY  # Replace with your MSG91 auth key
  sender-id: HOODJU              # Replace with your sender ID (6 characters)
  route: 4                       # Transactional route
  otp-template-id: YOUR_TEMPLATE_ID  # Replace with your MSG91 template ID
  otp-length: 6                  # Length of OTP
  otp-expiry-minutes: 10         # OTP expiry time in minutes
```

### OTP API Endpoints

The following endpoints are available for OTP operations:

1. **Send OTP**
   - URL: `/api/otp/send`
   - Method: POST
   - Request Body:
     ```json
     {
       "phoneNumber": "+919876543210"  // Phone number in E.164 format
     }
     ```
   - Response:
     ```json
     {
       "message": "OTP sent successfully",
       "success": true,
       "phoneNumber": "+919876543210",
       "expiresInSeconds": 600
     }
     ```

2. **Verify OTP**
   - URL: `/api/otp/verify`
   - Method: POST
   - Request Body:
     ```json
     {
       "phoneNumber": "+919876543210",  // Phone number in E.164 format
       "otp": "123456"                  // OTP received by the user
     }
     ```
   - Response:
     ```json
     {
       "message": "OTP verified successfully",
       "success": true,
       "phoneNumber": "+919876543210",
       "expiresInSeconds": 0
     }
     ```

3. **Resend OTP**
   - URL: `/api/otp/resend`
   - Method: POST
   - Request Body:
     ```json
     {
       "phoneNumber": "+919876543210"  // Phone number in E.164 format
     }
     ```
   - Response:
     ```json
     {
       "message": "OTP resent successfully",
       "success": true,
       "phoneNumber": "+919876543210",
       "expiresInSeconds": 600
     }
     ```

### Integration Steps

1. Sign up for a MSG91 account at [msg91.com](https://msg91.com/)
2. Get your AUTH KEY from the MSG91 dashboard
3. Create an OTP template in the MSG91 dashboard and note the template ID
4. Update the `application.yml` file with your MSG91 credentials
5. Use the OTP API endpoints in your application

## Getting Started

1. Clone the repository
2. Configure the database connections in `application.yml`
3. Configure MSG91 credentials in `application.yml`
4. Run the application using Maven:
   ```
   mvn spring-boot:run
   ```

## API Documentation

API documentation is available at `/api/swagger-ui.html` when the application is running.
