import React, { useState, useEffect } from 'react';
import { apiService } from '../services/apiService';
import LoadingSpinner from '../components/LoadingSpinner';
import {
  CalendarIcon,
  ChartBarIcon,
  ArrowTrendingUpIcon,
  ArrowTrendingDownIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon
} from '@heroicons/react/24/outline';
import {
  LineChart,
  Line,
  AreaChart,
  Area,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Legend
} from 'recharts';

const Analytics = () => {
  const [loading, setLoading] = useState(true);
  const [farms, setFarms] = useState([]);
  const [selectedFarm, setSelectedFarm] = useState('');
  const [selectedRoom, setSelectedRoom] = useState('');
  const [dateRange, setDateRange] = useState('7d');
  const [analyticsData, setAnalyticsData] = useState({
    overview: {},
    trends: [],
    distribution: [],
    alerts: []
  });



  useEffect(() => {
    loadAnalyticsData();
  }, [selectedFarm, selectedRoom, dateRange]);

  const loadAnalyticsData = async () => {
    try {
      setLoading(true);
      
      // Load farms data from Flask backend
      const farmsData = await apiService.getFarms();
      setFarms(farmsData);
      
      // Load analytics data based on selected farm, room and date range
      const params = {
        from: getDateFromRange(dateRange),
        to: new Date().toISOString()
      };
      
      // Get analytics data from backend
      const analyticsResponse = await apiService.getAnalytics(
        selectedFarm || (farmsData.length > 0 ? farmsData[0].farm_id : null),
        selectedRoom,
        params
      );
      
      // Use real data from Flask backend
      if (analyticsResponse && analyticsResponse.data) {
        setAnalyticsData(analyticsResponse.data);
      } else {
        // Initialize with empty data structure if no data available
        setAnalyticsData({
          overview: {
            totalDevices: 0,
            activeDevices: 0,
            avgTemperature: 0,
            avgHumidity: 0,
            avgCO2: 0,
            totalYield: 0,
            efficiency: 0,
            alerts: 0
          },
          trends: [],
          distribution: [],
          alerts: []
        });
      }
    } catch (error) {
      console.error('Error loading analytics data:', error);
      // Initialize with empty data structure on error
      setAnalyticsData({
        overview: {
          totalDevices: 0,
          activeDevices: 0,
          avgTemperature: 0,
          avgHumidity: 0,
          avgCO2: 0,
          totalYield: 0,
          efficiency: 0,
          alerts: 0
        },
        trends: [],
        distribution: [],
        alerts: []
      });
    } finally {
      setLoading(false);
    }
  };
  
  // Helper function to get date from range string
  const getDateFromRange = (range) => {
    const now = new Date();
    switch (range) {
      case '24h':
        return new Date(now.getTime() - 24 * 60 * 60 * 1000).toISOString();
      case '7d':
        return new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString();
      case '30d':
        return new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000).toISOString();
      case '90d':
        return new Date(now.getTime() - 90 * 24 * 60 * 60 * 1000).toISOString();
      default:
        return new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString();
    }
  };

  const getAlertIcon = (type) => {
    switch (type) {
      case 'critical':
        return <ExclamationTriangleIcon className="w-5 h-5 text-red-500" />;
      case 'warning':
        return <ExclamationTriangleIcon className="w-5 h-5 text-yellow-500" />;
      case 'info':
        return <CheckCircleIcon className="w-5 h-5 text-blue-500" />;
      default:
        return <CheckCircleIcon className="w-5 h-5 text-gray-500" />;
    }
  };

  const getAlertColor = (type) => {
    switch (type) {
      case 'critical':
        return 'bg-red-50 border-red-200';
      case 'warning':
        return 'bg-yellow-50 border-yellow-200';
      case 'info':
        return 'bg-blue-50 border-blue-200';
      default:
        return 'bg-gray-50 border-gray-200';
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric'
    });
  };

  const formatTimestamp = (timestamp) => {
    return new Date(timestamp).toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
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
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Analytics</h1>
          <p className="text-gray-600">Monitor performance and insights across your farms</p>
        </div>
      </div>

      {/* Filters */}
      <div className="card">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Farm
            </label>
            <select
              value={selectedFarm}
              onChange={(e) => setSelectedFarm(e.target.value)}
              className="w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500"
            >
              <option value="">All Farms</option>
              {farms.map(farm => (
                <option key={farm.farm_id} value={farm.farm_id}>{farm.name}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Room
            </label>
            <select
              value={selectedRoom}
              onChange={(e) => setSelectedRoom(e.target.value)}
              className="w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500"
            >
              <option value="">All Rooms</option>
              <option value="room1">Room A</option>
              <option value="room2">Room B</option>
              <option value="room3">Room C</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Time Range
            </label>
            <select
              value={dateRange}
              onChange={(e) => setDateRange(e.target.value)}
              className="w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500"
            >
              <option value="1d">Last 24 Hours</option>
              <option value="7d">Last 7 Days</option>
              <option value="30d">Last 30 Days</option>
              <option value="90d">Last 90 Days</option>
            </select>
          </div>
        </div>
      </div>

      {/* Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="card">
          <div className="flex items-center">
            <div className="p-3 rounded-lg bg-blue-100">
              <ChartBarIcon className="w-6 h-6 text-blue-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Active Devices</p>
              <p className="text-2xl font-bold text-gray-900">
                {analyticsData.overview.activeDevices}/{analyticsData.overview.totalDevices}
              </p>
              <p className="text-xs text-green-600 flex items-center">
                <ArrowTrendingUpIcon className="w-3 h-3 mr-1" />
                {analyticsData.overview.efficiency}% efficiency
              </p>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="flex items-center">
            <div className="p-3 rounded-lg bg-red-100">
              <div className="w-6 h-6 text-red-600 font-bold text-lg">°C</div>
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Avg Temperature</p>
              <p className="text-2xl font-bold text-gray-900">
                {analyticsData.overview.avgTemperature}°C
              </p>
              <p className="text-xs text-green-600 flex items-center">
                <ArrowTrendingUpIcon className="w-3 h-3 mr-1" />
                Optimal range
              </p>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="flex items-center">
            <div className="p-3 rounded-lg bg-blue-100">
              <div className="w-6 h-6 text-blue-600 font-bold text-lg">%</div>
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Avg Humidity</p>
              <p className="text-2xl font-bold text-gray-900">
                {analyticsData.overview.avgHumidity}%
              </p>
              <p className="text-xs text-green-600 flex items-center">
                <ArrowTrendingUpIcon className="w-3 h-3 mr-1" />
                Within target
              </p>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="flex items-center">
            <div className="p-3 rounded-lg bg-yellow-100">
              <div className="w-6 h-6 text-yellow-600 font-bold text-sm">kg</div>
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Total Yield</p>
              <p className="text-2xl font-bold text-gray-900">
                {analyticsData.overview.totalYield} kg
              </p>
              <p className="text-xs text-green-600 flex items-center">
                <ArrowTrendingUpIcon className="w-3 h-3 mr-1" />
                +12% vs last week
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Environmental Trends */}
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Environmental Trends</h3>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={analyticsData.trends}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis 
                dataKey="date" 
                tickFormatter={formatDate}
              />
              <YAxis yAxisId="temp" orientation="left" domain={[20, 26]} />
              <YAxis yAxisId="humidity" orientation="right" domain={[75, 90]} />
              <Tooltip 
                labelFormatter={(value) => formatDate(value)}
                formatter={(value, name) => [
                  `${value}${name === 'temperature' ? '°C' : name === 'humidity' ? '%' : ' ppm'}`,
                  name.charAt(0).toUpperCase() + name.slice(1)
                ]}
              />
              <Line 
                yAxisId="temp"
                type="monotone" 
                dataKey="temperature" 
                stroke="#EF4444" 
                strokeWidth={2}
                name="temperature"
              />
              <Line 
                yAxisId="humidity"
                type="monotone" 
                dataKey="humidity" 
                stroke="#3B82F6" 
                strokeWidth={2}
                name="humidity"
              />
              <Line 
                yAxisId="temp"
                type="monotone" 
                dataKey="co2" 
                stroke="#10B981" 
                strokeWidth={2}
                name="co2"
              />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* Yield Performance */}
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Yield Performance</h3>
          <ResponsiveContainer width="100%" height={300}>
            <AreaChart data={analyticsData.trends}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis 
                dataKey="date" 
                tickFormatter={formatDate}
              />
              <YAxis domain={[10, 16]} />
              <Tooltip 
                labelFormatter={(value) => formatDate(value)}
                formatter={(value) => [`${value} kg`, 'Daily Yield']}
              />
              <Area 
                type="monotone" 
                dataKey="yield" 
                stroke="#10B981" 
                fill="#10B981"
                fillOpacity={0.3}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Status Distribution and Alerts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Status Distribution */}
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">System Status Distribution</h3>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={analyticsData.distribution}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                outerRadius={80}
                fill="#8884d8"
                dataKey="value"
              >
                {analyticsData.distribution.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.color} />
                ))}
              </Pie>
              <Tooltip formatter={(value) => [`${value}%`, 'Percentage']} />
            </PieChart>
          </ResponsiveContainer>
        </div>

        {/* Recent Alerts */}
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Recent Alerts</h3>
          <div className="space-y-3 max-h-80 overflow-y-auto">
            {analyticsData.alerts.map((alert) => (
              <div 
                key={alert.id} 
                className={`p-3 rounded-lg border ${getAlertColor(alert.type)}`}
              >
                <div className="flex items-start space-x-3">
                  {getAlertIcon(alert.type)}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-900">
                      {alert.message}
                    </p>
                    <div className="mt-1 flex items-center space-x-2 text-xs text-gray-500">
                      <span>{alert.room}</span>
                      <span>•</span>
                      <span>{formatTimestamp(alert.timestamp)}</span>
                      {alert.value && (
                        <>
                          <span>•</span>
                          <span className="font-medium">{alert.value}</span>
                        </>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Performance Metrics */}
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Performance Metrics</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="text-center">
            <div className="text-3xl font-bold text-green-600 mb-2">
              {analyticsData.overview.efficiency}%
            </div>
            <div className="text-sm text-gray-600">System Efficiency</div>
            <div className="mt-2 text-xs text-gray-500">
              Based on optimal conditions vs actual
            </div>
          </div>
          
          <div className="text-center">
            <div className="text-3xl font-bold text-blue-600 mb-2">
              {((analyticsData.overview.activeDevices / analyticsData.overview.totalDevices) * 100).toFixed(1)}%
            </div>
            <div className="text-sm text-gray-600">Device Uptime</div>
            <div className="mt-2 text-xs text-gray-500">
              {analyticsData.overview.activeDevices} of {analyticsData.overview.totalDevices} devices online
            </div>
          </div>
          
          <div className="text-center">
            <div className="text-3xl font-bold text-yellow-600 mb-2">
              {analyticsData.overview.alerts}
            </div>
            <div className="text-sm text-gray-600">Active Alerts</div>
            <div className="mt-2 text-xs text-gray-500">
              Requiring immediate attention
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Analytics;