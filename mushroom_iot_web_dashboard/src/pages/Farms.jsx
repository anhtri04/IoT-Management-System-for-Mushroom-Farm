import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { apiService } from '../services/apiService';
import LoadingSpinner from '../components/LoadingSpinner';
import {
  PlusIcon,
  BuildingOfficeIcon,
  MapPinIcon,
  CalendarIcon,
  CubeIcon,
  EllipsisVerticalIcon
} from '@heroicons/react/24/outline';

// Component logging utility
const log = {
  info: (message, data = null) => {
    console.log(`[Farms Component] ${message}`, data || '');
  },
  error: (message, error = null) => {
    console.error(`[Farms Component ERROR] ${message}`, error || '');
  }
};

const Farms = () => {
  const [loading, setLoading] = useState(true);
  const [farms, setFarms] = useState([]);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    location: '',
    description: ''
  });
  const [formErrors, setFormErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    log.info('Farms component mounted, loading farms...');
    loadFarms();
  }, []);

  // Log farms state changes
  useEffect(() => {
    log.info('Farms state changed:', {
      count: farms?.length || 0,
      farms: farms
    });
  }, [farms]);

  const loadFarms = async () => {
    try {
      log.info('Starting to load farms...');
      setLoading(true);
      const data = await apiService.getFarms();
      log.info('Farms loaded from API:', data);
      setFarms(data);
      log.info('Farms state updated, current farms count:', data?.length || 0);
    } catch (error) {
      log.error('Error loading farms:', error);
    } finally {
      setLoading(false);
      log.info('Load farms process completed');
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
      errors.name = 'Farm name is required';
    }
    
    if (!formData.location.trim()) {
      errors.location = 'Location is required';
    }
    
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleCreateFarm = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    try {
      log.info('Starting farm creation process with data:', formData);
      setSubmitting(true);
      
      // Use real API service
      const createdFarm = await apiService.createFarm({
        name: formData.name,
        location: formData.location,
        description: formData.description
      });
      
      log.info('Farm created successfully:', createdFarm);
      setShowCreateModal(false);
      setFormData({ name: '', location: '', description: '' });
      
      // Reload farms to get updated list from backend
      log.info('Reloading farms after creation...');
      await loadFarms();
      log.info('Farm creation process completed successfully');
    } catch (error) {
      log.error('Error creating farm:', error);
    } finally {
      setSubmitting(false);
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
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
          <h1 className="text-2xl font-bold text-gray-900">Farms</h1>
          <p className="text-gray-600">Manage your mushroom farms</p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="btn btn-primary flex items-center space-x-2"
        >
          <PlusIcon className="w-5 h-5" />
          <span>Add Farm</span>
        </button>
      </div>

      {/* Farms Grid */}
      {farms.length === 0 ? (
        <div className="text-center py-12">
          <BuildingOfficeIcon className="mx-auto h-12 w-12 text-gray-400" />
          <h3 className="mt-2 text-sm font-medium text-gray-900">No farms</h3>
          <p className="mt-1 text-sm text-gray-500">Get started by creating a new farm.</p>
          <div className="mt-6">
            <button
              onClick={() => setShowCreateModal(true)}
              className="btn btn-primary"
            >
              <PlusIcon className="w-5 h-5 mr-2" />
              Add Farm
            </button>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {farms.map((farm) => (
            <div key={farm.farm_id} className="card hover:shadow-lg transition-shadow duration-200">
              <div className="flex items-center justify-between mb-4">
                <div className="p-3 rounded-lg bg-green-100">
                  <BuildingOfficeIcon className="w-6 h-6 text-green-600" />
                </div>
                <button className="p-1 rounded-md text-gray-400 hover:text-gray-600">
                  <EllipsisVerticalIcon className="w-5 h-5" />
                </button>
              </div>
              
              <h3 className="text-lg font-semibold text-gray-900 mb-2">{farm.name}</h3>
              
              <div className="space-y-2 mb-4">
                <div className="flex items-center text-sm text-gray-600">
                  <MapPinIcon className="w-4 h-4 mr-2" />
                  <span>{farm.location}</span>
                </div>
                <div className="flex items-center text-sm text-gray-600">
                  <CalendarIcon className="w-4 h-4 mr-2" />
                  <span>Created {formatDate(farm.created_at)}</span>
                </div>
                <div className="flex items-center text-sm text-gray-600">
                  <CubeIcon className="w-4 h-4 mr-2" />
                  <span>{farm.rooms_count || 0} rooms</span>
                </div>
              </div>
              
              <div className="flex space-x-2">
                <Link
                  to={`/farms/${farm.farm_id}/rooms`}
                  className="btn btn-primary flex-1 text-center"
                >
                  View Rooms
                </Link>
                <button className="btn btn-secondary">
                  Settings
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create Farm Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full">
            <div className="p-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Create New Farm</h3>
              
              <form onSubmit={handleCreateFarm} className="space-y-4">
                <div>
                  <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
                    Farm Name *
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
                    placeholder="Enter farm name"
                  />
                  {formErrors.name && (
                    <p className="mt-1 text-sm text-red-600">{formErrors.name}</p>
                  )}
                </div>

                <div>
                  <label htmlFor="location" className="block text-sm font-medium text-gray-700 mb-1">
                    Location *
                  </label>
                  <input
                    type="text"
                    id="location"
                    name="location"
                    value={formData.location}
                    onChange={handleInputChange}
                    className={`w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-green-500 ${
                      formErrors.location ? 'border-red-300' : 'border-gray-300'
                    }`}
                    placeholder="Enter farm location"
                  />
                  {formErrors.location && (
                    <p className="mt-1 text-sm text-red-600">{formErrors.location}</p>
                  )}
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
                    placeholder="Enter farm description (optional)"
                  />
                </div>

                <div className="flex space-x-3 pt-4">
                  <button
                    type="button"
                    onClick={() => {
                      setShowCreateModal(false);
                      setFormData({ name: '', location: '', description: '' });
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
                      'Create Farm'
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

export default Farms;