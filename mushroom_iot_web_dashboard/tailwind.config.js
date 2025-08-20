/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'farm-green': '#10b981',
        'farm-dark': '#059669',
        'farm-light': '#d1fae5',
      },
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
  ],
}