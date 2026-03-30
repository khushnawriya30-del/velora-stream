/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        background: '#0B0515',
        surface: '#130D22',
        'surface-light': '#1C1230',
        border: '#2D1F4E',
        gold: '#D4AF37',
        'gold-light': '#E8D48B',
        'gold-dark': '#A8892C',
        'text-primary': '#FFFFFF',
        'text-secondary': '#B0A3C4',
        'text-muted': '#6B5E80',
        error: '#E74C3C',
        success: '#2ECC71',
        warning: '#F39C12',
        info: '#3498DB',
        purple: '#9B59B6',
        'purple-light': '#BB86FC',
        'purple-dark': '#6C3483',
      },
      fontFamily: {
        sans: ['DM Sans', 'system-ui', 'sans-serif'],
        display: ['Playfair Display', 'serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
    },
  },
  plugins: [],
};
