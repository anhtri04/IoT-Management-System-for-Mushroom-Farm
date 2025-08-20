import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { apiService } from '../services/apiService';
import LoadingSpinner from '../components/LoadingSpinner';
import {
  PlusIcon,
  MagnifyingGlassIcon,
  FunnelIcon,
  FireIcon,
  BeakerIcon,
  LightBulbIcon,
  ArrowPathIcon,
  CpuChipIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
  XMarkIcon,
  PowerIcon
} from '@heroicons/react/24/outline';

const Devices = () => {
  const [loading, setLoading] = useState(true);
  const [devices, setDevices] = useState([]);
  const [farms, setFarms] = useState([]);
  const [rooms, setRooms] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedFarm, setSelectedFarm] = useState('');
  const [selectedRoom, setSelectedRoom] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');
  const [selectedType, setSelectedType] = useState('');
  const [showAddModal, setShowAddModal] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    room_id: '',
    device_type: 'sensor',
    category: 'temperature',
    description: ''
  });
  const [formErrors, setFormErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [controlsLoading, setControlsLoading] = useState({});

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      // Get real data from Flask backend
      const farmsData = await apiService.getFarms();
      
      // Load all devices and rooms for all farms
      const allRooms = [];
      const allDevices = [];
      
      for (const farm of farmsData) {
        const farmRooms = await apiService.getRooms(farm.farm_id);
        allRooms.push(...farmRooms.map(room => ({ ...room, farm_name: farm.name })));
        
        // Get devices for each room
        for (const room of farmRooms) {
          const roomDevices = await apiService.getDevices(room.room_id);
          allDevices.push(...roomDevices);
        }
      }
      
      setDevices(allDevices);
      setFarms(farmsData);
      setRooms(allRooms);
    } catch (error) {
      console.error('Error loading data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (formErrors[name]) {
      setFormErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const validateForm = () => {
    const errors = {};
    if (!formData.name.trim()) errors.name = 'Device name is required';
    if (!formData.room_id) errors.room_id = 'Room selection is required';
    return errors;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errors = validateForm();
    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    try {
      setSubmitting(true);
      
      // Find the selected room to get farm info
      const selectedRoomData = rooms.find(r => r.room_id === formData.room_id);
      
      const newDevice = {
        device_id: `device_${Date.now()}`,
        ...formData,
        farm_id: selectedRoomData?.farm_id,
        status: 'offline',
        last_seen: null,
        state: false,
        last_reading: device_type === 'sensor' ? '0' : null,
        created_at: new Date().toISOString()
      };
      
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      setDevices(prev => [newDevice, ...prev]);
      setShowAddModal(false);
      setFormData({
        name: '',
        room_id: '',
        device_type: 'sensor',
        category: 'temperature',
        description: ''
      });
    } catch (error) {
      console.error('Error creating device:', error);
    } finally {
      setSubmitting(false);
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

  const getDeviceIcon = (category) => {
    switch (category) {
      case 'temperature':
        return <FireIcon className="w-5 h-5" />;
      case 'humidity':
        return <BeakerIcon className="w-5 h-5" />;
      case 'light':
        return <LightBulbIcon className="w-5 h-5" />;
      case 'fan':
        return <ArrowPathIcon className="w-5 h-5" />;
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

  const getStatusIcon = (status) => {
    switch (status) {
      case 'online':
        return <CheckCircleIcon className="w-5 h-5 text-green-500" />;
      case 'offline':
        return <ExclamationTriangleIcon className="w-5 h-5 text-red-500" />;
      default:
        return <ExclamationTriangleIcon className="w-5 h-5 text-gray-500" />;
    }
  };

  // Filter devices based on search and filters
  const filteredDevices = devices.filter(device => {
    const room = rooms.find(r => r.room_id === device.room_id);
    const matchesSearch = device.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         device.category.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesFarm = !selectedFarm || room?.farm_id === selectedFarm;
    const matchesRoom = !selectedRoom || device.room_id === selectedRoom;
    const matchesStatus = !selectedStatus || device.status === selectedStatus;
    const matchesType = !selectedType || device.device_type === selectedType;
    
    return matchesSearch && matchesFarm && matchesRoom && matchesStatus && matchesType;
  });

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
          <h1 className="text-2xl font-bold text-gray-900">Devices</h1>
          <p className="text-gray-600">Manage and monitor all your IoT devices</p>
        </div>
        <button
          onClick={() => setShowAddModal(true)}
          className="btn btn-primary flex items-center space-x-2"
        >
          <PlusIcon className="w-5 h-5" />
          <span>Add Device</span>
        </button>
      </div>

      {/* Search and Filters */}
      <div className="card">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-6 gap-4">
          {/* Search */}
          <div className="lg:col-span-2">
            <div className="relative">
              <MagnifyingGlassIcon className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="text"
                placeholder="Search devices..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10 w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500"
              />
            </div>
          </div>

          {/* Farm Filter */}
          <div>
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

          {/* Room Filter */}
          <div>
            <select
              value={selectedRoom}
              onChange={(e) => setSelectedRoom(e.target.value)}
              className="w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500"
            >
              <option value="">All Rooms</option>
              {rooms
                .filter(room => !selectedFarm || room.farm_id === selectedFarm)
                .map(room => (
                  <option key={room.room_id} value={room.room_id}>
                    {room.farm_name} - {room.name}
                  </option>
                ))
              }
            </select>
          </div>

          {/* Status Filter */}
          <div>
            <select
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value)}
              className="w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500"
            >
              <option value="">All Status</option>
              <option value="online">Online</option>
              <option value="offline">Offline</option>
            </select>
          </div>

          {/* Type Filter */}
          <div>
            <select
              value={selectedType}
              onChange={(e) => setSelectedType(e.target.value)}
              className="w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500"
            >
              <option value="">All Types</option>
              <option value="sensor">Sensors</option>
              <option value="actuator">Actuators</option>
            </select>
          </div>
        </div>
      </div>

      {/* Devices Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        {filteredDevices.map((device) => {
          const room = rooms.find(r => r.room_id === device.room_id);
          return (
            <div key={device.device_id} className="card hover:shadow-lg transition-shadow">
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
                {getStatusIcon(device.status)}
              </div>

              {/* Location */}
              <div className="mb-4">
                <p className="text-sm text-gray-600">
                  <span className="font-medium">{room?.farm_name}</span>
                  {room && <span> • {room.name}</span>}
                </p>
              </div>

              {/* Device Info */}
              <div className="space-y-2 mb-4">
                <div className="flex justify-between text-sm">
                  <span className="text-gray-600">Type:</span>
                  <span className="font-medium capitalize">{device.device_type}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-600">Status:</span>
                  <span className={`font-medium ${
                    device.status === 'online' ? 'text-green-600' : 'text-red-600'
                  }`}>
                    {device.status.charAt(0).toUpperCase() + device.status.slice(1)}
                  </span>
                </div>
                {device.device_type === 'sensor' && device.last_reading && (
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-600">Reading:</span>
                    <span className="font-medium">
                      {device.last_reading}
                      {device.category === 'temperature' ? '°C' : 
                       device.category === 'humidity' ? '%' : 
                       device.category === 'co2' ? ' ppm' : 
                       device.category === 'light' ? ' lux' : ''}
                    </span>
                  </div>
                )}
                {device.device_type === 'actuator' && (
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-600">State:</span>
                    <span className={`font-medium ${
                      device.state ? 'text-green-600' : 'text-gray-600'
                    }`}>
                      {device.state ? 'ON' : 'OFF'}
                    </span>
                  </div>
                )}
              </div>

              {/* Actions */}
              <div className="flex space-x-2">
                <Link
                  to={`/rooms/${device.room_id}`}
                  className="flex-1 btn btn-secondary text-center"
                >
                  View Room
                </Link>
                {device.device_type === 'actuator' && device.status === 'online' && (
                  <button
                    onClick={() => handleDeviceControl(device.device_id, 'toggle')}
                    disabled={controlsLoading[device.device_id]}
                    className={`btn ${device.state ? 'btn-secondary' : 'btn-primary'} flex items-center justify-center`}
                    style={{ minWidth: '44px' }}
                  >
                    {controlsLoading[device.device_id] ? (
                      <LoadingSpinner size="small" />
                    ) : (
                      <PowerIcon className="w-4 h-4" />
                    )}
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {filteredDevices.length === 0 && (
        <div className="text-center py-12">
          <CpuChipIcon className="mx-auto h-12 w-12 text-gray-400" />
          <h3 className="mt-2 text-sm font-medium text-gray-900">No devices found</h3>
          <p className="mt-1 text-sm text-gray-500">
            {searchTerm || selectedFarm || selectedRoom || selectedStatus || selectedType
              ? 'Try adjusting your search or filters.'
              : 'Get started by adding your first device.'}
          </p>
        </div>
      )}

      {/* Add Device Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
          <div className="relative top-20 mx-auto p-5 border w-96 shadow-lg rounded-md bg-white">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-900">Add New Device</h3>
              <button
                onClick={() => setShowAddModal(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <XMarkIcon className="w-6 h-6" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Device Name *
                </label>
                <input
                  type="text"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  className={`w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500 ${
                    formErrors.name ? 'border-red-300' : ''
                  }`}
                  placeholder="Enter device name"
                />
                {formErrors.name && (
                  <p className="mt-1 text-sm text-red-600">{formErrors.name}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Room *
                </label>
                <select
                  name="room_id"
                  value={formData.room_id}
                  onChange={handleInputChange}
                  className={`w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500 ${
                    formErrors.room_id ? 'border-red-300' : ''
                  }`}
                >
                  <option value="">Select a room</option>
                  {rooms.map(room => (
                    <option key={room.room_id} value={room.room_id}>
                      {room.farm_name} - {room.name}
                    </option>
                  ))}
                </select>
                {formErrors.room_id && (
                  <p className="mt-1 text-sm text-red-600">{formErrors.room_id}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Device Type
                </label>
                <select
                  name="device_type"
                  value={formData.device_type}
                  onChange={handleInputChange}
                  className="w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500"
                >
                  <option value="sensor">Sensor</option>
                  <option value="actuator">Actuator</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Category
                </label>
                <select
                  name="category"
                  value={formData.category}
                  onChange={handleInputChange}
                  className="w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500"
                >
                  <option value="temperature">Temperature</option>
                  <option value="humidity">Humidity</option>
                  <option value="co2">CO2</option>
                  <option value="light">Light</option>
                  <option value="fan">Fan</option>
                  <option value="humidifier">Humidifier</option>
                  <option value="heater">Heater</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Description
                </label>
                <textarea
                  name="description"
                  value={formData.description}
                  onChange={handleInputChange}
                  rows={3}
                  className="w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500"
                  placeholder="Optional description"
                />
              </div>

              <div className="flex space-x-3 pt-4">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="flex-1 btn btn-secondary"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="flex-1 btn btn-primary flex items-center justify-center space-x-2"
                >
                  {submitting ? (
                    <LoadingSpinner size="small" />
                  ) : (
                    <span>Add Device</span>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Devices;