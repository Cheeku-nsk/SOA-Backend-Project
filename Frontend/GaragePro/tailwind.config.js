/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        background: "#0a0a0f", // Deep dark neutral
        foreground: "#f8fafc", // slate-50
        primary: "#2563eb", // blue-600
        "primary-hover": "#1d4ed8", // blue-700
        accent: "#0ea5e9", // sky-500
        surface: "rgba(30, 41, 59, 0.5)", // slate-800 with opacity
        "surface-border": "rgba(51, 65, 85, 0.5)", // slate-700 with opacity
      },
      backgroundImage: {
        'gradient-radial': 'radial-gradient(var(--tw-gradient-stops))',
        'hero-pattern': 'linear-gradient(to right bottom, rgba(15, 23, 42, 0.9), rgba(15, 23, 42, 0.7)), url("https://images.unsplash.com/photo-1619642751034-765dfdf7c58e?auto=format&fit=crop&q=80")',
      }
    },
  },
  plugins: [],
}
