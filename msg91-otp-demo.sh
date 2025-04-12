#!/bin/bash

# MSG91 OTP Verification Demo
# This script simulates the OTP verification process using MSG91

# Configuration
AUTH_KEY="445979AHHWnwUg4JXz67f2c9f4P1"
SENDER_ID="HoodJunciton"
TEMPLATE_ID="67f2cac9d6fc05563f58e9b4"
ROUTE=1 # Transactional route
OTP_LENGTH=6
OTP_EXPIRY_MINUTES=10

# Function to generate a random OTP
generate_otp() {
  # Generate a random 6-digit OTP
  printf "%06d\n" $((RANDOM % 1000000))
}

# Function to simulate sending OTP via MSG91
send_otp() {
  local phone_number=$1
  local otp=$2
  
  echo "Simulating MSG91 API call with the following parameters:"
  echo "- URL: https://api.msg91.com/api/v5/otp"
  echo "- template_id: $TEMPLATE_ID"
  echo "- mobile: $phone_number"
  echo "- authkey: $AUTH_KEY"
  echo "- otp: $otp"
  
  # In a real implementation, we would make an HTTP request to MSG91 API
  # curl -X GET "https://api.msg91.com/api/v5/otp?template_id=$TEMPLATE_ID&mobile=$phone_number&authkey=$AUTH_KEY&otp=$otp"
  
  echo "OTP sent successfully to $phone_number"
  echo "OTP: $otp (This would be sent to the user's phone in a real application)"
  echo "Expires in $OTP_EXPIRY_MINUTES minutes"
}

# Function to simulate verifying OTP
verify_otp() {
  local phone_number=$1
  local user_otp=$2
  local stored_otp=$3
  
  if [ "$user_otp" == "$stored_otp" ]; then
    echo "OTP verified successfully!"
    return 0
  else
    echo "OTP verification failed. Invalid OTP."
    return 1
  fi
}

# Main demo script
echo "===== MSG91 OTP Verification Demo ====="
echo ""
echo "This demo simulates the OTP verification process using MSG91."
echo "In a real application, the OTP would be sent to the user's phone."
echo ""

# Ask for phone number
read -p "Enter phone number (with country code, e.g., +919876543210): " phone_number

# Generate and send OTP
otp=$(generate_otp)
echo ""
echo "Generating and sending OTP..."
send_otp "$phone_number" "$otp"

# Ask user to enter the OTP
echo ""
read -p "Enter the OTP you received: " user_otp

# Verify OTP
echo ""
echo "Verifying OTP..."
verify_otp "$phone_number" "$user_otp" "$otp"

echo ""
echo "===== Demo Completed ====="

# Explain the integration
echo ""
echo "In the actual Spring Boot application, the MSG91 integration works as follows:"
echo ""
echo "1. The application uses the MSG91 API to send OTPs to users' phones."
echo "2. The OTP is stored in the database with an expiration time."
echo "3. When the user enters the OTP, it is verified against the stored OTP."
echo "4. If the OTP is valid and not expired, the verification is successful."
echo "5. The OTP is marked as used to prevent reuse."
echo ""
echo "The integration is implemented in the following classes:"
echo "- Msg91Service: Handles sending and verifying OTPs"
echo "- OtpController: Provides REST endpoints for OTP operations"
echo "- Otp: Entity class for storing OTP information"
echo ""
echo "To use the actual MSG91 service, you need to:"
echo "1. Sign up for a MSG91 account at msg91.com"
echo "2. Get your AUTH KEY from the MSG91 dashboard"
echo "3. Create an OTP template in the MSG91 dashboard"
echo "4. Update the application.yml file with your MSG91 credentials"
