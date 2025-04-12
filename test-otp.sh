#!/bin/bash

# Test script for OTP functionality
BASE_URL="http://localhost:8080/api"
PHONE_NUMBER="+919876543210"  # Test phone number
OTP=""

echo "===== Testing OTP Functionality ====="

# Step 1: Send OTP
echo -e "\n1. Sending OTP to $PHONE_NUMBER..."
SEND_RESPONSE=$(curl -s -X POST "$BASE_URL/test/otp/mock-send" \
  -H "Content-Type: application/json" \
  -d "{\"phoneNumber\": \"$PHONE_NUMBER\"}")

echo "Response: $SEND_RESPONSE"

# Step 2: Get the OTP from the database (for testing purposes)
echo -e "\n2. Getting OTPs for $PHONE_NUMBER..."
LIST_RESPONSE=$(curl -s -X GET "$BASE_URL/test/otp/list/$PHONE_NUMBER")
echo "Response: $LIST_RESPONSE"

# Extract the OTP from the response (this is a simple extraction, might need adjustment)
OTP=$(echo $LIST_RESPONSE | grep -o '"otpValue":"[^"]*' | grep -o '[0-9]*' | head -1)

if [ -z "$OTP" ]; then
  echo "Failed to extract OTP from response. Please check the logs."
  exit 1
fi

echo "Extracted OTP: $OTP"

# Step 3: Verify the OTP
echo -e "\n3. Verifying OTP $OTP for $PHONE_NUMBER..."
VERIFY_RESPONSE=$(curl -s -X POST "$BASE_URL/test/otp/verify" \
  -H "Content-Type: application/json" \
  -d "{\"phoneNumber\": \"$PHONE_NUMBER\", \"otp\": \"$OTP\"}")

echo "Response: $VERIFY_RESPONSE"

# Step 4: Try to verify the same OTP again (should fail)
echo -e "\n4. Trying to verify the same OTP again (should fail)..."
VERIFY_AGAIN_RESPONSE=$(curl -s -X POST "$BASE_URL/test/otp/verify" \
  -H "Content-Type: application/json" \
  -d "{\"phoneNumber\": \"$PHONE_NUMBER\", \"otp\": \"$OTP\"}")

echo "Response: $VERIFY_AGAIN_RESPONSE"

echo -e "\n===== OTP Test Completed ====="
