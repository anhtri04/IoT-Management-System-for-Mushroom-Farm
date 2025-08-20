# IoT Smart Farm System API Testing Guide

## Overview

This document provides instructions for using the Postman collection to test the CRUD (Create, Read, Update, Delete) operations of the Mushroom Farm IoT system API. The collection includes requests for all major endpoints in the system, including farms, rooms, devices, telemetry, commands, automation rules, and more.

## Prerequisites

- [Postman](https://www.postman.com/downloads/) installed on your machine
- The Flask backend server running (default: http://localhost:5000)
- Basic understanding of REST APIs and JSON

## Getting Started

1. Import the `postman_collection.json` file into Postman:
   - Open Postman
   - Click on "Import" button
   - Select the `postman_collection.json` file
   - Click "Import"

2. Set up environment variables (optional but recommended):
   - Create a new environment in Postman
   - Add the following variables:
     - `base_url`: http://localhost:5000 (or your server URL)
     - `auth_token`: (leave empty, will be populated after login)
     - `farm_id`: (leave empty, will be populated after farm creation)
     - `room_id`: (leave empty, will be populated after room creation)
     - `device_id`: (leave empty, will be populated after device creation)
     - `user_id`: (your user ID if known, otherwise leave empty)

## Authentication

Before testing protected endpoints, you need to authenticate:

1. Use the "Register User" request if you don't have an account
2. Use the "Login User" request to obtain an authentication token
   - The token will be automatically stored in the `auth_token` variable

## Testing Flow

The collection is organized to follow a logical testing flow:

1. **Authentication**: Register and login
2. **Farms**: Create a farm, then get/update/delete it
3. **Rooms**: Create rooms within a farm, then get/update/delete them
4. **Devices**: Register devices in rooms, then get/update/delete them
5. **Telemetry & Commands**: Query sensor data and send device commands
6. **Automation Rules**: Create and manage automation rules
7. **AI Recommendations**: Trigger and retrieve AI recommendations
8. **Notifications**: Get and acknowledge system notifications
9. **Analytics**: Retrieve farm analytics data

## Variable Chaining

The collection uses Postman's test scripts to automatically capture and store IDs:

- When you create a farm, the `farm_id` is automatically stored
- When you create a room, the `room_id` is automatically stored
- When you create a device, the `device_id` is automatically stored

This allows you to run requests in sequence without manually copying IDs.

## Testing CRUD Operations

### Farms

1. **Create**: POST to `/api/farms` with farm details
2. **Read**: GET from `/api/farms` or `/api/farms/{farm_id}`
3. **Update**: PUT to `/api/farms/{farm_id}` with updated details
4. **Delete**: DELETE to `/api/farms/{farm_id}`

### Rooms

1. **Create**: POST to `/api/farms/{farm_id}/rooms` with room details
2. **Read**: GET from `/api/farms/{farm_id}/rooms` or `/api/rooms/{room_id}`
3. **Update**: PUT to `/api/rooms/{room_id}` with updated details
4. **Delete**: DELETE to `/api/rooms/{room_id}`

### Devices

1. **Create**: POST to `/api/devices` with device details
2. **Read**: GET from `/api/rooms/{room_id}/devices` or `/api/devices/{device_id}`
3. **Update**: PUT to `/api/devices/{device_id}` with updated details
4. **Delete**: DELETE to `/api/devices/{device_id}`

## Internal Testing

The collection includes an "Internal Endpoints" folder for testing backend-to-backend communication:

- **Internal Ingest**: Simulates telemetry data being sent from AWS IoT Core to the backend

## Troubleshooting

- If authentication fails, check that the backend server is running and Cognito is properly configured
- If requests return 404, verify the API paths and that the server is running
- If requests return 403, check that your authentication token is valid and has the required permissions
- For 400 errors, check the request body format against the API specifications

## Extending the Collection

You can extend this collection by:

1. Duplicating existing requests and modifying them
2. Adding environment variables for different deployment environments (dev, staging, prod)
3. Adding more test scripts to validate response data
4. Creating request folders for new API endpoints as they are developed

## Security Notes

- The collection includes an internal token for testing. In production, use proper security measures.
- Never commit real authentication tokens or sensitive data to version control.
- For production testing, ensure proper HTTPS and authentication are configured.