import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { apiService } from '../services/apiService';
import LoadingSpinner from '../components/LoadingSpinner';
import {
  BuildingOfficeIcon,
  CubeIcon,
  CpuChipIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
  ClockIcon
} from '@heroicons/react/24/outline';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  BarChart,
  Bar
} from 'recharts';

const Dashboard = () => {
  const [loading, setLoading] = useState(true);
  const [farms, setFarms] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [stats, setStats] = useState({
    totalFarms: 0,
    totalRooms: 0,
    totalDevices: 0,
    activeAlerts: 0
  });

  const [temperatureData, setTemperatureData] = useState([]);
  const [humidityData, setHumidityData] = useState([]);
  const [roomStatusData, setRoomStatusData] = useState([]);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      
      // Load real data from Flask backend
      const farmsData = await apiService.getFarms();
      const roomsData = await apiService.getRooms();
      
      setFarms(farmsData);
      setRooms(roomsData);
      
      // Calculate stats
      const devicePromises = roomsData.map(room => apiService.getDevices(room.room_id));
      const allDevices = await Promise.all(devicePromises);
      const totalDevices = allDevices.reduce((sum, devices) => sum + devices.length, 0);
      
      setStats({
        totalFarms: farmsData.length,
        totalRooms: roomsData.length,
        totalDevices,
        activeAlerts: roomsData.filter(room => room.status === 'warning').length
      });
      
      // Load telemetry data for charts from the first room if available
      if (roomsData.length > 0) {
        const firstRoom = roomsData[0];
        const telemetryResponse = await apiService.getTelemetry(firstRoom.room_id, {
          from: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(), // Last 24 hours
          to: new Date().toISOString(),
          agg: 'hour'
        });
        
        if (telemetryResponse && telemetryResponse.data) {
          // Transform telemetry data for charts
          const telemetryData = telemetryResponse.data;
          
          const tempData = telemetryData.map(item => ({
            time: new Date(item.recorded_at).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
            value: item.temperature_c || 0
          }));
          
          const humData = telemetryData.map(item => ({
            time: new Date(item.recorded_at).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
            value: item.humidity_pct || 0
          }));
          
          setTemperatureData(tempData);
          setHumidityData(humData);
        }
      }
      
      // Calculate room status distribution
      const statusCounts = roomsData.reduce((acc, room) => {
        const status = room.status || 'unknown';
        acc[status] = (acc[status] || 0) + 1;
        return acc;
      }, {});
      
      const statusData = [
        { name: 'Optimal', count: statusCounts.optimal || 0, color: '#10B981' },
        { name: 'Good', count: statusCounts.good || 0, color: '#F59E0B' },
        { name: 'Warning', count: statusCounts.warning || 0, color: '#EF4444' },
      ].filter(item => item.count > 0);
      
      setRoomStatusData(statusData);
      
    } catch (error) {
      console.error('Error loading dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'optimal':
        return <CheckCircleIcon className="w-5 h-5 text-green-500" />;
      case 'good':
        return <ClockIcon className="w-5 h-5 text-yellow-500" />;
      case 'warning':
        return <ExclamationTriangleIcon className="w-5 h-5 text-red-500" />;
      default:
        return <ClockIcon className="w-5 h-5 text-gray-500" />;
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'optimal':
        return 'bg-green-100 text-green-800';
      case 'good':
        return 'bg-yellow-100 text-yellow-800';
      case 'warning':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <LoadingSpinner size="large" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p className="text-gray-600">Overview of your mushroom farm operations</p>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="card">
          <div className="flex items-center">
            <div className="p-3 rounded-lg bg-blue-100">
              <BuildingOfficeIcon className="w-6 h-6 text-blue-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Total Farms</p>
              <p className="text-2xl font-bold text-gray-900">{stats.totalFarms}</p>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="flex items-center">
            <div className="p-3 rounded-lg bg-green-100">
              <CubeIcon className="w-6 h-6 text-green-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Total Rooms</p>
              <p className="text-2xl font-bold text-gray-900">{stats.totalRooms}</p>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="flex items-center">
            <div className="p-3 rounded-lg bg-purple-100">
              <CpuChipIcon className="w-6 h-6 text-purple-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Total Devices</p>
              <p className="text-2xl font-bold text-gray-900">{stats.totalDevices}</p>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="flex items-center">
            <div className="p-3 rounded-lg bg-red-100">
              <ExclamationTriangleIcon className="w-6 h-6 text-red-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Active Alerts</p>
              <p className="text-2xl font-bold text-gray-900">{stats.activeAlerts}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Temperature Chart */}
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Temperature Trends</h3>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={temperatureData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="time" />
              <YAxis domain={['dataMin - 1', 'dataMax + 1']} />
              <Tooltip formatter={(value) => [`${value}°C`, 'Temperature']} />
              <Line 
                type="monotone" 
                dataKey="value" 
                stroke="#3B82F6" 
                strokeWidth={2}
                dot={{ fill: '#3B82F6', strokeWidth: 2, r: 4 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* Humidity Chart */}
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Humidity Levels</h3>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={humidityData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="time" />
              <YAxis domain={[75, 90]} />
              <Tooltip formatter={(value) => [`${value}%`, 'Humidity']} />
              <Line 
                type="monotone" 
                dataKey="value" 
                stroke="#10B981" 
                strokeWidth={2}
                dot={{ fill: '#10B981', strokeWidth: 2, r: 4 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Room Status and Recent Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Room Status Chart */}
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Room Status</h3>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={roomStatusData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="count" fill={(entry) => entry.color} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Room Cards */}
        <div className="lg:col-span-2">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Room Overview</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {rooms.map((room) => (
              <Link
                key={room.room_id}
                to={`/rooms/${room.room_id}`}
                className="card hover:shadow-lg transition-shadow duration-200"
              >
                <div className="flex items-center justify-between mb-3">
                  <h4 className="font-semibold text-gray-900">{room.name}</h4>
                  {getStatusIcon(room.status)}
                </div>
                <p className="text-sm text-gray-600 mb-2">{room.description}</p>
                <div className="flex items-center justify-between">
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(room.status)}`}>
                    {room.status.charAt(0).toUpperCase() + room.status.slice(1)}
                  </span>
                  <div className="text-sm text-gray-500">
                    {room.current_temp}°C | {room.current_humidity}%
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;