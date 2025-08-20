import axios from 'axios';
import { authService } from './authService';

// Create axios instance with base configuration
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token
api.interceptors.request.use(
  async (config) => {
    try {
      const token = await authService.getIdToken();
      if (token && token !== 'dev-token') {
        config.headers.Authorization = `Bearer ${token}`;
      }
      console.log('Request interceptor - token obtained:', token ? 'yes' : 'no');
    } catch (error) {
      // Token retrieval failed, continue without token for testing
      console.log('Request interceptor - no token available, proceeding without auth:', error.message);
    }
    return config;
  },
  (error) => {
    console.error('Request interceptor error:', error);
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Handle unauthorized - redirect to login
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

class ApiService {
  // Farm endpoints
  async getFarms() {
    try {
      console.info('Fetching farms from backend...');
      const response = await api.get('/api/farms');
      console.info('Farms fetched successfully:', response.data);
      return response.data;
    } catch (error) {
        console.error('Error fetching farms:', error.response?.data || error.message);
        throw error;
    }
  }

  async getFarm(farmId) {
    const response = await api.get(`/api/farms/${farmId}`);
    return response.data;
  }

  async createFarm(farmData) {
    try {
      console.info('Creating farm with data:', farmData);
      const response = await api.post('/api/farms', farmData);
      console.info('Farm created successfully:', response.data);
      return response.data;
    } catch (error) {
      console.error('Error creating farm:', {
        status: error.response?.status,
        statusText: error.response?.statusText,
        data: error.response?.data,
        message: error.message
      });
      console.error('Error type:', error.constructor.name);
      console.error('Error message:', error.message);
      console.error('Error status:', error.status);
      console.error('Error response:', error.response);
      if (error.response && error.response.data) {
        console.error('Server error details:', error.response.data);
      }
      throw error;
    }
  }

  async updateFarm(farmId, farmData) {
    const response = await api.put(`/api/farms/${farmId}`, farmData);
    return response.data;
  }

  async deleteFarm(farmId) {
    const response = await api.delete(`/api/farms/${farmId}`);
    return response.data;
  }

  // Room endpoints
  async getRooms(farmId) {
    try {
      console.info(`Fetching rooms for farm ${farmId}...`);
      const response = await api.get(`/api/farms/${farmId}/rooms`);
      console.info('Rooms fetched successfully:', response.data);
      return response.data;
    } catch (error) {
      console.error(`Error fetching rooms for farm ${farmId}:`, error.response?.data || error.message);
      throw error;
    }
  }

  async getRoom(roomId) {
    const response = await api.get(`/api/rooms/${roomId}`);
    return response.data;
  }

  async createRoom(farmId, roomData) {
    try {
      console.info(`Creating room for farm ${farmId} with data:`, roomData);
      const response = await api.post(`/api/farms/${farmId}/rooms`, roomData);
      console.info('Room created successfully:', response.data);
      return response.data;
    } catch (error) {
      console.error(`Error creating room for farm ${farmId}:`, {
        status: error.response?.status,
        statusText: error.response?.statusText,
        data: error.response?.data,
        message: error.message
      });
      throw error;
    }
  }

  async updateRoom(roomId, roomData) {
    const response = await api.put(`/api/rooms/${roomId}`, roomData);
    return response.data;
  }

  async deleteRoom(roomId) {
    const response = await api.delete(`/api/rooms/${roomId}`);
    return response.data;
  }

  // Device endpoints
  async getDevices(roomId) {
    const response = await api.get(`/api/rooms/${roomId}/devices`);
    return response.data;
  }

  async getDevice(deviceId) {
    const response = await api.get(`/api/devices/${deviceId}`);
    return response.data;
  }

  async createDevice(deviceData) {
    const response = await api.post('/api/devices', deviceData);
    return response.data;
  }

  async updateDevice(deviceId, deviceData) {
    const response = await api.put(`/api/devices/${deviceId}`, deviceData);
    return response.data;
  }

  async deleteDevice(deviceId) {
    const response = await api.delete(`/api/devices/${deviceId}`);
    return response.data;
  }

  // Telemetry endpoints
  async getTelemetry(roomId, params = {}) {
    const queryParams = new URLSearchParams(params).toString();
    const response = await api.get(`/api/rooms/${roomId}/telemetry?${queryParams}`);
    return response.data;
  }

  async getLatestReadings(deviceId) {
    const response = await api.get(`/api/devices/${deviceId}/latest`);
    return response.data;
  }

  // Command endpoints
  async sendCommand(deviceId, command, params = {}) {
    const response = await api.post(`/api/devices/${deviceId}/commands`, {
      command,
      params
    });
    return response.data;
  }

  async getCommands(roomId) {
    const response = await api.get(`/api/rooms/${roomId}/commands`);
    return response.data;
  }

  // Automation endpoints
  async getAutomationRules(roomId) {
    const response = await api.get(`/api/rooms/${roomId}/rules`);
    return response.data;
  }

  async createAutomationRule(roomId, ruleData) {
    const response = await api.post(`/api/rooms/${roomId}/rules`, ruleData);
    return response.data;
  }

  async updateAutomationRule(ruleId, ruleData) {
    const response = await api.put(`/api/automation/rules/${ruleId}`, ruleData);
    return response.data;
  }

  async deleteAutomationRule(ruleId) {
    const response = await api.delete(`/api/automation/rules/${ruleId}`);
    return response.data;
  }

  // Recommendations endpoints
  async getRecommendations(roomId) {
    const response = await api.get(`/api/rooms/${roomId}/recommendations`);
    return response.data;
  }

  async triggerRecommendation(roomId) {
    const response = await api.post(`/api/rooms/${roomId}/recommend`);
    return response.data;
  }

  // Notifications endpoints
  async getNotifications() {
    const response = await api.get('/api/notifications');
    return response.data;
  }

  async acknowledgeNotification(notificationId) {
    const response = await api.post(`/api/notifications/${notificationId}/ack`);
    return response.data;
  }

  // User management endpoints
  async assignUserToRoom(roomId, userId, role) {
    const response = await api.post(`/api/rooms/${roomId}/assign`, {
      user_id: userId,
      role
    });
    return response.data;
  }

  // Analytics endpoints
  async getAnalytics(farmId, params = {}) {
    const queryParams = new URLSearchParams(params).toString();
    const response = await api.get(`/api/farms/${farmId}/analytics?${queryParams}`);
    return response.data;
  }


}

export const apiService = new ApiService();