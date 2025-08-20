import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import {
  HomeIcon,
  BuildingOfficeIcon,
  CubeIcon,
  CpuChipIcon,
  ChartBarIcon,
  Cog6ToothIcon,
  XMarkIcon
} from '@heroicons/react/24/outline';

const Sidebar = ({ isOpen, onClose }) => {
  const location = useLocation();

  const navigation = [
    { name: 'Dashboard', href: '/dashboard', icon: HomeIcon },
    { name: 'Farms', href: '/farms', icon: BuildingOfficeIcon },
    { name: 'Devices', href: '/devices', icon: CpuChipIcon },
    { name: 'Analytics', href: '/analytics', icon: ChartBarIcon },
    { name: 'Settings', href: '/settings', icon: Cog6ToothIcon },
  ];

  const isActive = (href) => {
    if (href === '/dashboard') {
      return location.pathname === '/' || location.pathname === '/dashboard';
    }
    return location.pathname.startsWith(href);
  };

  return (
    <>
      {/* Desktop sidebar */}
      <div className={`fixed inset-y-0 left-0 z-50 w-64 bg-white shadow-lg transform transition-transform duration-300 ease-in-out lg:translate-x-0 ${
        isOpen ? 'translate-x-0' : '-translate-x-full lg:w-16'
      }`}>
        <div className="flex items-center justify-between h-16 px-4 border-b border-gray-200">
          <div className={`flex items-center space-x-3 ${!isOpen && 'lg:justify-center'}`}>
            <div className="w-10 h-10 bg-green-600 rounded-lg flex items-center justify-center">
              <CubeIcon className="w-6 h-6 text-white stroke-2" />
            </div>
            {(isOpen || window.innerWidth < 1024) && (
              <span className="text-xl font-bold text-gray-900">MushroomIoT</span>
            )}
          </div>
          {isOpen && (
            <button
              onClick={onClose}
              className="lg:hidden p-1 rounded-md text-gray-400 hover:text-gray-600"
            >
              <XMarkIcon className="w-6 h-6" />
            </button>
          )}
        </div>

        <nav className="mt-8 px-4">
          <ul className="space-y-2">
            {navigation.map((item) => {
              const Icon = item.icon;
              const active = isActive(item.href);
              
              return (
                <li key={item.name}>
                  <Link
                    to={item.href}
                    onClick={() => window.innerWidth < 1024 && onClose()}
                    className={`nav-item group relative ${
                      active ? 'nav-item-active' : 'nav-item-inactive'
                    } ${!isOpen && 'lg:justify-center lg:px-3'}`}
                    title={!isOpen ? item.name : ''}
                  >
                    <Icon className={`w-8 h-8 ${!isOpen && 'lg:w-9 lg:h-9'} stroke-[2.5]`} />
                    {(isOpen || window.innerWidth < 1024) && (
                      <span className="ml-3">{item.name}</span>
                    )}
                    {/* Tooltip for collapsed sidebar */}
                    {!isOpen && (
                      <div className="absolute left-full ml-2 px-2 py-1 bg-gray-900 text-white text-sm rounded opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none whitespace-nowrap z-50 hidden lg:block">
                        {item.name}
                      </div>
                    )}
                  </Link>
                </li>
              );
            })}
          </ul>
        </nav>

        {/* User info section */}
        {(isOpen || window.innerWidth < 1024) && (
          <div className="absolute bottom-0 left-0 right-0 p-4 border-t border-gray-200">
            <div className="flex items-center space-x-3">
              <div className="w-8 h-8 bg-gray-300 rounded-full flex items-center justify-center">
                <span className="text-sm font-medium text-gray-700">U</span>
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 truncate">User</p>
                <p className="text-xs text-gray-500 truncate">Online</p>
              </div>
            </div>
          </div>
        )}
      </div>
    </>
  );
};

export default Sidebar;