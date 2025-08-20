import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { apiService } from '../services/apiService';
import LoadingSpinner from '../components/LoadingSpinner';
import {
  ArrowLeftIcon,
  FireIcon,
  BeakerIcon,
  LightBulbIcon,
  CogIcon,
  CpuChipIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
  ClockIcon,
  PowerIcon,
  AdjustmentsHorizontalIcon
} from '@heroicons/react/24/outline';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  AreaChart,
  Area
} from 'recharts';

const RoomDetail = () => {
  const { roomId } = useParams();
  const [loading, setLoading] = useState(true);
  const [room, setRoom] = useState(null);
  const [devices, setDevices] = useState([]);
  const [telemetryData, setTelemetryData] = useState([]);
  const [activeTab, setActiveTab] = useState('overview');
  const [controlsLoading, setControlsLoading] = useState({});



  useEffect(() => {
    loadRoomData();
  }, [roomId]);

  const loadRoomData = async () => {
    try {
      setLoading(true);
      
      // Load room info and devices from Flask backend
      const roomData = await apiService.getRoom(roomId);
      const devicesData = await apiService.getDevices(roomId);
      
      // Load telemetry data for the room
      const telemetryResponse = await apiService.getTelemetry(roomId, {
        from: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(), // Last 24 hours
        to: new Date().toISOString(),
        agg: 'hour'
      });
      
      setRoom(roomData);
      setDevices(devicesData);
      
      // Format telemetry data for charts if available
      if (telemetryResponse && telemetryResponse.data && telemetryResponse.data.length > 0) {
        const formattedData = telemetryResponse.data.map(item => ({
          time: new Date(item.recorded_at).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
          temperature: item.temperature_c || 0,
          humidity: item.humidity_pct || 0,
          co2: item.co2_ppm || 0,
          light: item.light_lux || 0
        }));
        setTelemetryData(formattedData);
      } else {
        // Initialize with empty array if no data
        setTelemetryData([]);
      }
    } catch (error) {
      console.error('Error loading room data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDeviceControl = async (deviceId, command, params = {}) => {
    try {
      setControlsLoading(prev => ({ ...prev, [deviceId]: true }));
      
      // Send command to Flask backend API
      await apiService.sendCommand(deviceId, command, params);
      
      // Update device state locally
      setDevices(prev => prev.map(device => {
        if (device.device_id === deviceId) {
          if (command === 'toggle') {
            return { ...device, state: !device.state };
          }
          return { ...device, ...params };
        }
        return device;
      }));
    } catch (error) {
      console.error('Error controlling device:', error);
    } finally {
      setControlsLoading(prev => ({ ...prev, [deviceId]: false }));
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

  const getDeviceIcon = (category) => {
    switch (category) {
      case 'temperature':
        return <FireIcon className="w-5 h-5" />;
      case 'humidity':
        return <BeakerIcon className="w-5 h-5" />;
      case 'light':
        return <LightBulbIcon className="w-5 h-5" />;
      case 'fan':
        return <CogIcon className="w-5 h-5" />;
      default:
        return <CpuChipIcon className="w-5 h-5" />;
    }
  };

  const getDeviceColor = (category) => {
    switch (category) {
      case 'temperature':
        return 'bg-red-100 text-red-600';
      case 'humidity':
        return 'bg-blue-100 text-blue-600';
      case 'light':
        return 'bg-yellow-100 text-yellow-600';
      case 'fan':
        return 'bg-green-100 text-green-600';
      default:
        return 'bg-gray-100 text-gray-600';
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <LoadingSpinner size="large" />
      </div>
    );
  }

  if (!room) {
    return (
      <div className="text-center py-12">
        <h3 className="text-lg font-medium text-gray-900">Room not found</h3>
        <p className="text-gray-500">The requested room could not be found.</p>
        <Link to="/farms" className="btn btn-primary mt-4">
          Back to Farms
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <Link
            to={`/farms/${room.farm_id}/rooms`}
            className="p-2 rounded-md text-gray-400 hover:text-gray-600 hover:bg-gray-100"
          >
            <ArrowLeftIcon className="w-5 h-5" />
          </Link>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{room.name}</h1>
            <p className="text-gray-600">{room.mushroom_type} - {room.stage}</p>
          </div>
        </div>
        <div className="flex items-center space-x-3">
          {getStatusIcon(room.status)}
          <span className={`px-3 py-1 rounded-full text-sm font-medium ${getStatusColor(room.status)}`}>
            {room.status.charAt(0).toUpperCase() + room.status.slice(1)}
          </span>
        </div>
      </div>

      {/* Tabs */}
      <div className="border-b border-gray-200">
        <nav className="-mb-px flex space-x-8">
          {[
            { id: 'overview', name: 'Overview' },
            { id: 'devices', name: 'Devices' },
            { id: 'analytics', name: 'Analytics' },
            { id: 'automation', name: 'Automation' }
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`py-2 px-1 border-b-2 font-medium text-sm ${
                activeTab === tab.id
                  ? 'border-green-500 text-green-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              {tab.name}
            </button>
          ))}
        </nav>
      </div>

      {/* Tab Content */}
      {activeTab === 'overview' && (
        <div className="space-y-6">
          {/* Current Conditions */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            <div className="card">
              <div className="flex items-center">
                <div className="p-3 rounded-lg bg-red-100">
                  <FireIcon className="w-6 h-6 text-red-600" />
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">Temperature</p>
                  <p className="text-2xl font-bold text-gray-900">{room.current_temp}°C</p>
                  <p className="text-xs text-gray-500">Target: 22-24°C</p>
                </div>
              </div>
            </div>

            <div className="card">
              <div className="flex items-center">
                <div className="p-3 rounded-lg bg-blue-100">
                  <BeakerIcon className="w-6 h-6 text-blue-600" />
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">Humidity</p>
                  <p className="text-2xl font-bold text-gray-900">{room.current_humidity}%</p>
                  <p className="text-xs text-gray-500">Target: 80-90%</p>
                </div>
              </div>
            </div>

            <div className="card">
              <div className="flex items-center">
                <div className="p-3 rounded-lg bg-green-100">
                  <BeakerIcon className="w-6 h-6 text-green-600" />
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">CO2</p>
                  <p className="text-2xl font-bold text-gray-900">380 ppm</p>
                  <p className="text-xs text-gray-500">Target: 350-400 ppm</p>
                </div>
              </div>
            </div>

            <div className="card">
              <div className="flex items-center">
                <div className="p-3 rounded-lg bg-yellow-100">
                  <LightBulbIcon className="w-6 h-6 text-yellow-600" />
                </div>
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">Light</p>
                  <p className="text-2xl font-bold text-gray-900">800 lux</p>
                  <p className="text-xs text-gray-500">Target: 500-1000 lux</p>
                </div>
              </div>
            </div>
          </div>

          {/* Charts */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="card">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Temperature & Humidity</h3>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={telemetryData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="time" />
                  <YAxis yAxisId="temp" orientation="left" domain={[20, 26]} />
                  <YAxis yAxisId="humidity" orientation="right" domain={[75, 90]} />
                  <Tooltip />
                  <Line 
                    yAxisId="temp"
                    type="monotone" 
                    dataKey="temperature" 
                    stroke="#EF4444" 
                    strokeWidth={2}
                    name="Temperature (°C)"
                  />
                  <Line 
                    yAxisId="humidity"
                    type="monotone" 
                    dataKey="humidity" 
                    stroke="#3B82F6" 
                    strokeWidth={2}
                    name="Humidity (%)"
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>

            <div className="card">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">CO2 & Light Levels</h3>
              <ResponsiveContainer width="100%" height={300}>
                <AreaChart data={telemetryData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="time" />
                  <YAxis yAxisId="co2" orientation="left" domain={[300, 500]} />
                  <YAxis yAxisId="light" orientation="right" domain={[0, 1500]} />
                  <Tooltip />
                  <Area 
                    yAxisId="co2"
                    type="monotone" 
                    dataKey="co2" 
                    stackId="1"
                    stroke="#10B981" 
                    fill="#10B981"
                    fillOpacity={0.3}
                    name="CO2 (ppm)"
                  />
                  <Area 
                    yAxisId="light"
                    type="monotone" 
                    dataKey="light" 
                    stackId="2"
                    stroke="#F59E0B" 
                    fill="#F59E0B"
                    fillOpacity={0.3}
                    name="Light (lux)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      )}

      {activeTab === 'devices' && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {devices.map((device) => (
              <div key={device.device_id} className="card">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center space-x-3">
                    <div className={`p-2 rounded-lg ${getDeviceColor(device.category)}`}>
                      {getDeviceIcon(device.category)}
                    </div>
                    <div>
                      <h3 className="font-semibold text-gray-900">{device.name}</h3>
                      <p className="text-sm text-gray-600">{device.category}</p>
                    </div>
                  </div>
                  <div className={`w-3 h-3 rounded-full ${
                    device.status === 'online' ? 'bg-green-500' : 'bg-red-500'
                  }`} />
                </div>

                {device.device_type === 'sensor' && (
                  <div className="mb-4">
                    <p className="text-2xl font-bold text-gray-900">
                      {device.last_reading}
                      {device.category === 'temperature' ? '°C' : 
                       device.category === 'humidity' ? '%' : 
                       device.category === 'co2' ? ' ppm' : 
                       device.category === 'light' ? ' lux' : ''}
                    </p>
                    <p className="text-sm text-gray-500">Current reading</p>
                  </div>
                )}

                {device.device_type === 'actuator' && (
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="text-sm font-medium text-gray-700">Status</span>
                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                        device.state ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                      }`}>
                        {device.state ? 'ON' : 'OFF'}
                      </span>
                    </div>
                    <button
                      onClick={() => handleDeviceControl(device.device_id, 'toggle')}
                      disabled={controlsLoading[device.device_id]}
                      className={`w-full btn ${
                        device.state ? 'btn-secondary' : 'btn-primary'
                      } flex items-center justify-center space-x-2`}
                    >
                      {controlsLoading[device.device_id] ? (
                        <LoadingSpinner size="small" />
                      ) : (
                        <>
                          <PowerIcon className="w-4 h-4" />
                          <span>{device.state ? 'Turn Off' : 'Turn On'}</span>
                        </>
                      )}
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {activeTab === 'analytics' && (
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Analytics Dashboard</h3>
          <p className="text-gray-600">Advanced analytics and insights coming soon...</p>
        </div>
      )}

      {activeTab === 'automation' && (
        <div className="card">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Automation Rules</h3>
          <p className="text-gray-600">Automation rules and AI recommendations coming soon...</p>
        </div>
      )}
    </div>
  );
};

export default RoomDetail;