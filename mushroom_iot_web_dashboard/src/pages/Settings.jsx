import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import LoadingSpinner from '../components/LoadingSpinner';
import {
  UserIcon,
  BellIcon,
  CogIcon,
  ShieldCheckIcon,
  KeyIcon,
  DevicePhoneMobileIcon,
  EnvelopeIcon,
  CheckCircleIcon,
  ExclamationTriangleIcon
} from '@heroicons/react/24/outline';

const Settings = () => {
  const { user } = useAuth();
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('profile');
  const [saveStatus, setSaveStatus] = useState('');
  
  // Profile settings
  const [profileData, setProfileData] = useState({
    fullName: user?.name || '',
    email: user?.email || '',
    phone: '',
    timezone: 'UTC',
    language: 'en'
  });
  
  // Notification settings
  const [notificationSettings, setNotificationSettings] = useState({
    emailNotifications: true,
    pushNotifications: true,
    smsNotifications: false,
    criticalAlerts: true,
    warningAlerts: true,
    infoAlerts: false,
    maintenanceReminders: true,
    weeklyReports: true,
    monthlyReports: false
  });
  
  // System settings
  const [systemSettings, setSystemSettings] = useState({
    temperatureUnit: 'celsius',
    dateFormat: 'MM/DD/YYYY',
    timeFormat: '12h',
    autoRefresh: true,
    refreshInterval: 30,
    darkMode: false,
    compactView: false
  });
  
  // Security settings
  const [securitySettings, setSecuritySettings] = useState({
    twoFactorEnabled: false,
    sessionTimeout: 60,
    loginNotifications: true,
    deviceTracking: true
  });

  const handleProfileChange = (e) => {
    const { name, value } = e.target;
    setProfileData(prev => ({ ...prev, [name]: value }));
  };

  const handleNotificationChange = (e) => {
    const { name, checked } = e.target;
    setNotificationSettings(prev => ({ ...prev, [name]: checked }));
  };

  const handleSystemChange = (e) => {
    const { name, value, type, checked } = e.target;
    setSystemSettings(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleSecurityChange = (e) => {
    const { name, value, type, checked } = e.target;
    setSecuritySettings(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleSave = async (section) => {
    try {
      setLoading(true);
      setSaveStatus('');
      
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      setSaveStatus('success');
      setTimeout(() => setSaveStatus(''), 3000);
    } catch (error) {
      console.error('Error saving settings:', error);
      setSaveStatus('error');
      setTimeout(() => setSaveStatus(''), 3000);
    } finally {
      setLoading(false);
    }
  };

  const handleChangePassword = () => {
    // This would typically redirect to Cognito's change password flow
    alert('Password change functionality would redirect to AWS Cognito hosted UI');
  };

  const handleEnable2FA = () => {
    // This would typically start the 2FA setup process
    alert('2FA setup would be handled through AWS Cognito MFA configuration');
  };

  const tabs = [
    { id: 'profile', name: 'Profile', icon: UserIcon },
    { id: 'notifications', name: 'Notifications', icon: BellIcon },
    { id: 'system', name: 'System', icon: CogIcon },
    { id: 'security', name: 'Security', icon: ShieldCheckIcon }
  ];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Settings</h1>
        <p className="text-gray-600">Manage your account and application preferences</p>
      </div>

      {/* Save Status */}
      {saveStatus && (
        <div className={`p-4 rounded-md flex items-center space-x-2 ${
          saveStatus === 'success' 
            ? 'bg-green-50 text-green-800 border border-green-200'
            : 'bg-red-50 text-red-800 border border-red-200'
        }`}>
          {saveStatus === 'success' ? (
            <CheckCircleIcon className="w-5 h-5" />
          ) : (
            <ExclamationTriangleIcon className="w-5 h-5" />
          )}
          <span>
            {saveStatus === 'success' 
              ? 'Settings saved successfully!' 
              : 'Error saving settings. Please try again.'}
          </span>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Sidebar */}
        <div className="lg:col-span-1">
          <nav className="space-y-1">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`w-full flex items-center space-x-3 px-3 py-2 text-left rounded-md transition-colors ${
                    activeTab === tab.id
                      ? 'bg-green-100 text-green-700 border-r-2 border-green-500'
                      : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                  }`}
                >
                  <Icon className="w-5 h-5" />
                  <span className="font-medium">{tab.name}</span>
                </button>
              );
            })}
          </nav>
        </div>

        {/* Content */}
        <div className="lg:col-span-3">
          <div className="card">
            {/* Profile Tab */}
            {activeTab === 'profile' && (
              <div className="space-y-6">
                <div className="border-b border-gray-200 pb-4">
                  <h3 className="text-lg font-semibold text-gray-900">Profile Information</h3>
                  <p className="text-sm text-gray-600">Update your personal information and preferences.</p>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Full Name
                    </label>
                    <input
                      type="text"
                      name="fullName"
                      value={profileData.fullName}
                      onChange={handleProfileChange}
                      className="w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Email Address
                    </label>
                    <input
                      type="email"
                      name="email"
                      value={profileData.email}
                      onChange={handleProfileChange}
                      className="w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500"
                      disabled
                    />
                    <p className="text-xs text-gray-500 mt-1">Email changes must be done through AWS Cognito</p>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Phone Number
                    </label>
                    <input
                      type="tel"
                      name="phone"
                      value={profileData.phone}
                      onChange={handleProfileChange}
                      className="w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500"
                      placeholder="+1 (555) 123-4567"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Timezone
                    </label>
                    <select
                      name="timezone"
                      value={profileData.timezone}
                      onChange={handleProfileChange}
                      className="w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500"
                    >
                      <option value="UTC">UTC</option>
                      <option value="America/New_York">Eastern Time</option>
                      <option value="America/Chicago">Central Time</option>
                      <option value="America/Denver">Mountain Time</option>
                      <option value="America/Los_Angeles">Pacific Time</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Language
                    </label>
                    <select
                      name="language"
                      value={profileData.language}
                      onChange={handleProfileChange}
                      className="w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500"
                    >
                      <option value="en">English</option>
                      <option value="es">Spanish</option>
                      <option value="fr">French</option>
                      <option value="de">German</option>
                    </select>
                  </div>
                </div>

                <div className="flex justify-end">
                  <button
                    onClick={() => handleSave('profile')}
                    disabled={loading}
                    className="btn btn-primary flex items-center space-x-2"
                  >
                    {loading ? <LoadingSpinner size="small" /> : <span>Save Changes</span>}
                  </button>
                </div>
              </div>
            )}

            {/* Notifications Tab */}
            {activeTab === 'notifications' && (
              <div className="space-y-6">
                <div className="border-b border-gray-200 pb-4">
                  <h3 className="text-lg font-semibold text-gray-900">Notification Preferences</h3>
                  <p className="text-sm text-gray-600">Choose how you want to be notified about important events.</p>
                </div>

                <div className="space-y-6">
                  {/* Delivery Methods */}
                  <div>
                    <h4 className="text-md font-medium text-gray-900 mb-3">Delivery Methods</h4>
                    <div className="space-y-3">
                      <label className="flex items-center">
                        <input
                          type="checkbox"
                          name="emailNotifications"
                          checked={notificationSettings.emailNotifications}
                          onChange={handleNotificationChange}
                          className="rounded border-gray-300 text-green-600 shadow-sm focus:border-green-500 focus:ring-green-500"
                        />
                        <div className="ml-3">
                          <span className="text-sm font-medium text-gray-700">Email Notifications</span>
                          <p className="text-xs text-gray-500">Receive notifications via email</p>
                        </div>
                      </label>

                      <label className="flex items-center">
                        <input
                          type="checkbox"
                          name="pushNotifications"
                          checked={notificationSettings.pushNotifications}
                          onChange={handleNotificationChange}
                          className="rounded border-gray-300 text-green-600 shadow-sm focus:border-green-500 focus:ring-green-500"
                        />
                        <div className="ml-3">
                          <span className="text-sm font-medium text-gray-700">Push Notifications</span>
                          <p className="text-xs text-gray-500">Receive browser push notifications</p>
                        </div>
                      </label>

                      <label className="flex items-center">
                        <input
                          type="checkbox"
                          name="smsNotifications"
                          checked={notificationSettings.smsNotifications}
                          onChange={handleNotificationChange}
                          className="rounded border-gray-300 text-green-600 shadow-sm focus:border-green-500 focus:ring-green-500"
                        />
                        <div className="ml-3">
                          <span className="text-sm font-medium text-gray-700">SMS Notifications</span>
                          <p className="text-xs text-gray-500">Receive notifications via text message</p>
                        </div>
                      </label>
                    </div>
                  </div>

                  {/* Alert Types */}
                  <div>
                    <h4 className="text-md font-medium text-gray-900 mb-3">Alert Types</h4>
                    <div className="space-y-3">
                      <label className="flex items-center">
                        <input
                          type="checkbox"
                          name="criticalAlerts"
                          checked={notificationSettings.criticalAlerts}
                          onChange={handleNotificationChange}
                          className="rounded border-gray-300 text-green-600 shadow-sm focus:border-green-500 focus:ring-green-500"
                        />
                        <div className="ml-3">
                          <span className="text-sm font-medium text-gray-700">Critical Alerts</span>
                          <p className="text-xs text-gray-500">System failures, security issues</p>
                        </div>
                      </label>

                      <label className="flex items-center">
                        <input
                          type="checkbox"
                          name="warningAlerts"
                          checked={notificationSettings.warningAlerts}
                          onChange={handleNotificationChange}
                          className="rounded border-gray-300 text-green-600 shadow-sm focus:border-green-500 focus:ring-green-500"
                        />
                        <div className="ml-3">
                          <span className="text-sm font-medium text-gray-700">Warning Alerts</span>
                          <p className="text-xs text-gray-500">Environmental conditions outside optimal range</p>
                        </div>
                      </label>

                      <label className="flex items-center">
                        <input
                          type="checkbox"
                          name="infoAlerts"
                          checked={notificationSettings.infoAlerts}
                          onChange={handleNotificationChange}
                          className="rounded border-gray-300 text-green-600 shadow-sm focus:border-green-500 focus:ring-green-500"
                        />
                        <div className="ml-3">
                          <span className="text-sm font-medium text-gray-700">Info Alerts</span>
                          <p className="text-xs text-gray-500">General system updates and information</p>
                        </div>
                      </label>

                      <label className="flex items-center">
                        <input
                          type="checkbox"
                          name="maintenanceReminders"
                          checked={notificationSettings.maintenanceReminders}
                          onChange={handleNotificationChange}
                          className="rounded border-gray-300 text-green-600 shadow-sm focus:border-green-500 focus:ring-green-500"
                        />
                        <div className="ml-3">
                          <span className="text-sm font-medium text-gray-700">Maintenance Reminders</span>
                          <p className="text-xs text-gray-500">Device calibration and maintenance schedules</p>
                        </div>
                      </label>
                    </div>
                  </div>

                  {/* Reports */}
                  <div>
                    <h4 className="text-md font-medium text-gray-900 mb-3">Reports</h4>
                    <div className="space-y-3">
                      <label className="flex items-center">
                        <input
                          type="checkbox"
                          name="weeklyReports"
                          checked={notificationSettings.weeklyReports}
                          onChange={handleNotificationChange}
                          className="rounded border-gray-300 text-green-600 shadow-sm focus:border-green-500 focus:ring-green-500"
                        />
                        <div className="ml-3">
                          <span className="text-sm font-medium text-gray-700">Weekly Reports</span>
                          <p className="text-xs text-gray-500">Weekly summary of farm performance</p>
                        </div>
                      </label>

                      <label className="flex items-center">
                        <input
                          type="checkbox"
                          name="monthlyReports"
                          checked={notificationSettings.monthlyReports}
                          onChange={handleNotificationChange}
                          className="rounded border-gray-300 text-green-600 shadow-sm focus:border-green-500 focus:ring-green-500"
                        />
                        <div className="ml-3">
                          <span className="text-sm font-medium text-gray-700">Monthly Reports</span>
                          <p className="text-xs text-gray-500">Monthly analytics and insights</p>
                        </div>
                      </label>
                    </div>
                  </div>
                </div>

                <div className="flex justify-end">
                  <button
                    onClick={() => handleSave('notifications')}
                    disabled={loading}
                    className="btn btn-primary flex items-center space-x-2"
                  >
                    {loading ? <LoadingSpinner size="small" /> : <span>Save Changes</span>}
                  </button>
                </div>
              </div>
            )}

            {/* System Tab */}
            {activeTab === 'system' && (
              <div className="space-y-6">
                <div className="border-b border-gray-200 pb-4">
                  <h3 className="text-lg font-semibold text-gray-900">System Preferences</h3>
                  <p className="text-sm text-gray-600">Customize how the application looks and behaves.</p>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Temperature Unit
                    </label>
                    <select
                      name="temperatureUnit"
                      value={systemSettings.temperatureUnit}
                      onChange={handleSystemChange}
                      className="w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500"
                    >
                      <option value="celsius">Celsius (°C)</option>
                      <option value="fahrenheit">Fahrenheit (°F)</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Date Format
                    </label>
                    <select
                      name="dateFormat"
                      value={systemSettings.dateFormat}
                      onChange={handleSystemChange}
                      className="w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500"
                    >
                      <option value="MM/DD/YYYY">MM/DD/YYYY</option>
                      <option value="DD/MM/YYYY">DD/MM/YYYY</option>
                      <option value="YYYY-MM-DD">YYYY-MM-DD</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Time Format
                    </label>
                    <select
                      name="timeFormat"
                      value={systemSettings.timeFormat}
                      onChange={handleSystemChange}
                      className="w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500"
                    >
                      <option value="12h">12 Hour (AM/PM)</option>
                      <option value="24h">24 Hour</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Auto Refresh Interval (seconds)
                    </label>
                    <select
                      name="refreshInterval"
                      value={systemSettings.refreshInterval}
                      onChange={handleSystemChange}
                      className="w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500"
                      disabled={!systemSettings.autoRefresh}
                    >
                      <option value={15}>15 seconds</option>
                      <option value={30}>30 seconds</option>
                      <option value={60}>1 minute</option>
                      <option value={300}>5 minutes</option>
                    </select>
                  </div>
                </div>

                <div className="space-y-4">
                  <label className="flex items-center">
                    <input
                      type="checkbox"
                      name="autoRefresh"
                      checked={systemSettings.autoRefresh}
                      onChange={handleSystemChange}
                      className="rounded border-gray-300 text-green-600 shadow-sm focus:border-green-500 focus:ring-green-500"
                    />
                    <div className="ml-3">
                      <span className="text-sm font-medium text-gray-700">Auto Refresh Data</span>
                      <p className="text-xs text-gray-500">Automatically refresh dashboard data</p>
                    </div>
                  </label>

                  <label className="flex items-center">
                    <input
                      type="checkbox"
                      name="darkMode"
                      checked={systemSettings.darkMode}
                      onChange={handleSystemChange}
                      className="rounded border-gray-300 text-green-600 shadow-sm focus:border-green-500 focus:ring-green-500"
                    />
                    <div className="ml-3">
                      <span className="text-sm font-medium text-gray-700">Dark Mode</span>
                      <p className="text-xs text-gray-500">Use dark theme (coming soon)</p>
                    </div>
                  </label>

                  <label className="flex items-center">
                    <input
                      type="checkbox"
                      name="compactView"
                      checked={systemSettings.compactView}
                      onChange={handleSystemChange}
                      className="rounded border-gray-300 text-green-600 shadow-sm focus:border-green-500 focus:ring-green-500"
                    />
                    <div className="ml-3">
                      <span className="text-sm font-medium text-gray-700">Compact View</span>
                      <p className="text-xs text-gray-500">Show more information in less space</p>
                    </div>
                  </label>
                </div>

                <div className="flex justify-end">
                  <button
                    onClick={() => handleSave('system')}
                    disabled={loading}
                    className="btn btn-primary flex items-center space-x-2"
                  >
                    {loading ? <LoadingSpinner size="small" /> : <span>Save Changes</span>}
                  </button>
                </div>
              </div>
            )}

            {/* Security Tab */}
            {activeTab === 'security' && (
              <div className="space-y-6">
                <div className="border-b border-gray-200 pb-4">
                  <h3 className="text-lg font-semibold text-gray-900">Security Settings</h3>
                  <p className="text-sm text-gray-600">Manage your account security and privacy settings.</p>
                </div>

                <div className="space-y-6">
                  {/* Password */}
                  <div className="bg-gray-50 p-4 rounded-lg">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-3">
                        <KeyIcon className="w-5 h-5 text-gray-600" />
                        <div>
                          <h4 className="text-sm font-medium text-gray-900">Password</h4>
                          <p className="text-xs text-gray-500">Last changed 30 days ago</p>
                        </div>
                      </div>
                      <button
                        onClick={handleChangePassword}
                        className="btn btn-secondary"
                      >
                        Change Password
                      </button>
                    </div>
                  </div>

                  {/* Two-Factor Authentication */}
                  <div className="bg-gray-50 p-4 rounded-lg">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-3">
                        <DevicePhoneMobileIcon className="w-5 h-5 text-gray-600" />
                        <div>
                          <h4 className="text-sm font-medium text-gray-900">Two-Factor Authentication</h4>
                          <p className="text-xs text-gray-500">
                            {securitySettings.twoFactorEnabled ? 'Enabled' : 'Not enabled'}
                          </p>
                        </div>
                      </div>
                      <button
                        onClick={handleEnable2FA}
                        className={`btn ${
                          securitySettings.twoFactorEnabled ? 'btn-secondary' : 'btn-primary'
                        }`}
                      >
                        {securitySettings.twoFactorEnabled ? 'Manage' : 'Enable'}
                      </button>
                    </div>
                  </div>

                  {/* Session Settings */}
                  <div>
                    <h4 className="text-md font-medium text-gray-900 mb-3">Session Settings</h4>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                          Session Timeout (minutes)
                        </label>
                        <select
                          name="sessionTimeout"
                          value={securitySettings.sessionTimeout}
                          onChange={handleSecurityChange}
                          className="w-full rounded-md border-gray-300 shadow-sm focus:border-green-500 focus:ring-green-500"
                        >
                          <option value={15}>15 minutes</option>
                          <option value={30}>30 minutes</option>
                          <option value={60}>1 hour</option>
                          <option value={120}>2 hours</option>
                          <option value={480}>8 hours</option>
                        </select>
                      </div>
                    </div>
                  </div>

                  {/* Privacy Settings */}
                  <div>
                    <h4 className="text-md font-medium text-gray-900 mb-3">Privacy Settings</h4>
                    <div className="space-y-3">
                      <label className="flex items-center">
                        <input
                          type="checkbox"
                          name="loginNotifications"
                          checked={securitySettings.loginNotifications}
                          onChange={handleSecurityChange}
                          className="rounded border-gray-300 text-green-600 shadow-sm focus:border-green-500 focus:ring-green-500"
                        />
                        <div className="ml-3">
                          <span className="text-sm font-medium text-gray-700">Login Notifications</span>
                          <p className="text-xs text-gray-500">Get notified when someone logs into your account</p>
                        </div>
                      </label>

                      <label className="flex items-center">
                        <input
                          type="checkbox"
                          name="deviceTracking"
                          checked={securitySettings.deviceTracking}
                          onChange={handleSecurityChange}
                          className="rounded border-gray-300 text-green-600 shadow-sm focus:border-green-500 focus:ring-green-500"
                        />
                        <div className="ml-3">
                          <span className="text-sm font-medium text-gray-700">Device Tracking</span>
                          <p className="text-xs text-gray-500">Track devices used to access your account</p>
                        </div>
                      </label>
                    </div>
                  </div>
                </div>

                <div className="flex justify-end">
                  <button
                    onClick={() => handleSave('security')}
                    disabled={loading}
                    className="btn btn-primary flex items-center space-x-2"
                  >
                    {loading ? <LoadingSpinner size="small" /> : <span>Save Changes</span>}
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Settings;