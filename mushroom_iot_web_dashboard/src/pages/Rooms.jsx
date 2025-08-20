import React, { useState, useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { apiService } from '../services/apiService';
import LoadingSpinner from '../components/LoadingSpinner';
import {
  PlusIcon,
  CubeIcon,
  FireIcon,
  BeakerIcon,
  LightBulbIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
  ClockIcon,
  ArrowLeftIcon
} from '@heroicons/react/24/outline';

// Component logging utility
const log = {
  info: (message, data = null) => {
    console.log(`[Rooms Component] ${message}`, data || '');
  },
  error: (message, error = null) => {
    console.error(`[Rooms Component ERROR] ${message}`, error || '');
  }
};

const Rooms = () => {
  const { farmId } = useParams();
  const [loading, setLoading] = useState(true);
  const [rooms, setRooms] = useState([]);
  const [farm, setFarm] = useState(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    mushroom_type: '',
    stage: 'incubation'
  });
  const [formErrors, setFormErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  const mushroomTypes = [
    'Shiitake',
    'Oyster',
    'Enoki',
    'Lion\'s Mane',
    'Reishi',
    'Maitake',
    'Cremini',
    'Portobello'
  ];

  const stages = [
    { value: 'incubation', label: 'Incubation' },
    { value: 'fruiting', label: 'Fruiting' },
    { value: 'maintenance', label: 'Maintenance' },
    { value: 'harvest', label: 'Harvest Ready' }
  ];

  useEffect(() => {
    log.info(`Rooms component mounted for farm ${farmId}, loading rooms data...`);
    loadRoomsData();
  }, [farmId]);

  // Log rooms state changes
  useEffect(() => {
    log.info('Rooms state changed:', {
      farmId,
      roomsCount: rooms?.length || 0,
      rooms: rooms
    });
  }, [rooms, farmId]);

  const loadRoomsData = async () => {
    try {
      log.info(`Starting to load rooms data for farm ${farmId}...`);
      setLoading(true);
      
      // Load farm info and rooms
      const farmsData = await apiService.getFarms();
      log.info('Farms data loaded:', farmsData);
      
      const farmData = farmsData.find(f => f.farm_id === farmId);
      log.info('Current farm found:', farmData);
      
      const roomsData = await apiService.getRooms(farmId);
      log.info('Rooms data loaded:', roomsData);
      
      setFarm(farmData);
      setRooms(roomsData);
      log.info('Rooms state updated, current rooms count:', roomsData?.length || 0);
    } catch (error) {
      log.error('Error loading rooms data:', error);
    } finally {
      setLoading(false);
      log.info('Load rooms data process completed');
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    // Clear error when user starts typing
    if (formErrors[name]) {
      setFormErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
  };

  const validateForm = () => {
    const errors = {};
    
    if (!formData.name.trim()) {
      errors.name = 'Room name is required';
    }
    
    if (!formData.mushroom_type) {
      errors.mushroom_type = 'Mushroom type is required';
    }
    
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleCreateRoom = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      log.info('Form validation failed');
      return;
    }

    try {
      log.info(`Starting room creation process for farm ${farmId} with data:`, formData);
      setSubmitting(true);
      
      // Use real API service
      const createdRoom = await apiService.createRoom(farmId, {
        name: formData.name,
        description: formData.description,
        mushroom_type: formData.mushroom_type,
        stage: formData.stage
      });
      
      log.info('Room created successfully:', createdRoom);
      setShowCreateModal(false);
      setFormData({ name: '', description: '', mushroom_type: '', stage: 'incubation' });
      
      // Reload rooms to get updated list from backend
      log.info('Reloading rooms after creation...');
      await loadRoomsData();
      log.info('Room creation process completed successfully');
    } catch (error) {
      log.error('Error creating room:', error);
    } finally {
      setSubmitting(false);
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

  const getStageColor = (stage) => {
    switch (stage) {
      case 'incubation':
        return 'bg-blue-100 text-blue-800';
      case 'fruiting':
        return 'bg-green-100 text-green-800';
      case 'maintenance':
        return 'bg-yellow-100 text-yellow-800';
      case 'harvest':
        return 'bg-purple-100 text-purple-800';
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
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <Link
            to="/farms"
            className="p-2 rounded-md text-gray-400 hover:text-gray-600 hover:bg-gray-100"
          >
            <ArrowLeftIcon className="w-5 h-5" />
          </Link>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">
              {farm?.name || 'Farm'} - Rooms
            </h1>
            <p className="text-gray-600">Manage rooms in this farm</p>
          </div>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="btn btn-primary flex items-center space-x-2"
        >
          <PlusIcon className="w-5 h-5" />
          <span>Add Room</span>
        </button>
      </div>

      {/* Rooms Grid */}
      {rooms.length === 0 ? (
        <div className="text-center py-12">
          <CubeIcon className="mx-auto h-12 w-12 text-gray-400" />
          <h3 className="mt-2 text-sm font-medium text-gray-900">No rooms</h3>
          <p className="mt-1 text-sm text-gray-500">Get started by creating a new room.</p>
          <div className="mt-6">
            <button
              onClick={() => setShowCreateModal(true)}
              className="btn btn-primary"
            >
              <PlusIcon className="w-5 h-5 mr-2" />
              Add Room
            </button>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {rooms.map((room) => (
            <Link
              key={room.room_id}
              to={`/rooms/${room.room_id}`}
              className="card hover:shadow-lg transition-shadow duration-200"
            >
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center space-x-3">
                  <div className="p-3 rounded-lg bg-blue-100">
                    <CubeIcon className="w-6 h-6 text-blue-600" />
                  </div>
                  <div>
                    <h3 className="text-lg font-semibold text-gray-900">{room.name}</h3>
                    <p className="text-sm text-gray-600">{room.mushroom_type}</p>
                  </div>
                </div>
                {getStatusIcon(room.status)}
              </div>
              
              <div className="space-y-3 mb-4">
                <div className="flex items-center justify-between">
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStageColor(room.stage)}`}>
                    {room.stage.charAt(0).toUpperCase() + room.stage.slice(1)}
                  </span>
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(room.status)}`}>
                    {room.status.charAt(0).toUpperCase() + room.status.slice(1)}
                  </span>
                </div>
                
                <div className="grid grid-cols-2 gap-4">
                  <div className="flex items-center space-x-2">
                    <FireIcon className="w-4 h-4 text-red-500" />
                    <span className="text-sm text-gray-600">{room.current_temp}°C</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <BeakerIcon className="w-4 h-4 text-blue-500" />
                    <span className="text-sm text-gray-600">{room.current_humidity}%</span>
                  </div>
                </div>
              </div>
              
              <p className="text-sm text-gray-600">{room.description}</p>
            </Link>
          ))}
        </div>
      )}

      {/* Create Room Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full max-h-[90vh] overflow-y-auto">
            <div className="p-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Create New Room</h3>
              
              <form onSubmit={handleCreateRoom} className="space-y-4">
                <div>
                  <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
                    Room Name *
                  </label>
                  <input
                    type="text"
                    id="name"
                    name="name"
                    value={formData.name}
                    onChange={handleInputChange}
                    className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 ${
                      formErrors.name ? 'border-red-300' : 'border-gray-300'
                    }`}
                    placeholder="Enter room name (e.g., Block A)"
                  />
                  {formErrors.name && (
                    <p className="mt-1 text-sm text-red-600">{formErrors.name}</p>
                  )}
                </div>

                <div>
                  <label htmlFor="mushroom_type" className="block text-sm font-medium text-gray-700 mb-1">
                    Mushroom Type *
                  </label>
                  <select
                    id="mushroom_type"
                    name="mushroom_type"
                    value={formData.mushroom_type}
                    onChange={handleInputChange}
                    className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 ${
                      formErrors.mushroom_type ? 'border-red-300' : 'border-gray-300'
                    }`}
                  >
                    <option value="">Select mushroom type</option>
                    {mushroomTypes.map((type) => (
                      <option key={type} value={type}>{type}</option>
                    ))}
                  </select>
                  {formErrors.mushroom_type && (
                    <p className="mt-1 text-sm text-red-600">{formErrors.mushroom_type}</p>
                  )}
                </div>

                <div>
                  <label htmlFor="stage" className="block text-sm font-medium text-gray-700 mb-1">
                    Growth Stage
                  </label>
                  <select
                    id="stage"
                    name="stage"
                    value={formData.stage}
                    onChange={handleInputChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500"
                  >
                    {stages.map((stage) => (
                      <option key={stage.value} value={stage.value}>{stage.label}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
                    Description
                  </label>
                  <textarea
                    id="description"
                    name="description"
                    rows={3}
                    value={formData.description}
                    onChange={handleInputChange}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500"
                    placeholder="Enter room description (optional)"
                  />
                </div>

                <div className="flex space-x-3 pt-4">
                  <button
                    type="button"
                    onClick={() => {
                      setShowCreateModal(false);
                      setFormData({ name: '', description: '', mushroom_type: '', stage: 'incubation' });
                      setFormErrors({});
                    }}
                    className="btn btn-secondary flex-1"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={submitting}
                    className="btn btn-primary flex-1"
                  >
                    {submitting ? (
                      <LoadingSpinner size="small" />
                    ) : (
                      'Create Room'
                    )}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Rooms;